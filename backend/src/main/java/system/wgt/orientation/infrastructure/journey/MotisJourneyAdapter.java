package system.wgt.orientation.infrastructure.journey;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import system.wgt.orientation.application.journey.JourneyFailureKind;
import system.wgt.orientation.application.journey.JourneyPort;
import system.wgt.orientation.application.journey.JourneyProviderException;
import system.wgt.orientation.domain.journey.Journey;
import system.wgt.orientation.domain.journey.JourneyEventTime;
import system.wgt.orientation.domain.journey.JourneyLeg;
import system.wgt.orientation.domain.journey.JourneyLegGeometry;
import system.wgt.orientation.domain.journey.JourneyLegMode;
import system.wgt.orientation.domain.journey.JourneyPlan;
import system.wgt.orientation.domain.journey.JourneyRequest;
import system.wgt.orientation.domain.journey.JourneyStop;
import system.wgt.orientation.domain.journey.JourneyTimeMode;
import system.wgt.orientation.domain.journey.TransitService;
import system.wgt.orientation.domain.place.Coordinate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MotisJourneyAdapter implements JourneyPort {
    static final int MAX_PROVIDER_RESPONSE_BYTES = 4_194_304;
    private static final int EXPECTED_POLYLINE_PRECISION = 6;
    private static final String TRANSIT_MODES = String.join(",",
            "TRAM", "SUBWAY", "FERRY", "BUS", "COACH", "HIGHSPEED_RAIL", "LONG_DISTANCE",
            "NIGHT_RAIL", "REGIONAL_RAIL", "SUBURBAN", "FUNICULAR", "AERIAL_LIFT");

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public MotisJourneyAdapter(RestClient client, ObjectMapper objectMapper) {
        if (client == null || objectMapper == null) {
            throw new IllegalArgumentException("MOTIS client and object mapper are required.");
        }
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public JourneyPlan plan(JourneyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Journey request is required.");
        }
        try {
            JsonNode response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v6/plan")
                            .queryParam("fromPlace", place(request.origin()))
                            .queryParam("toPlace", place(request.destination()))
                            .queryParam("time", request.time().toString())
                            .queryParam("arriveBy", request.timeMode() == JourneyTimeMode.ARRIVE_BY)
                            .queryParam("transitModes", TRANSIT_MODES)
                            .queryParam("directModes", "")
                            .queryParam("preTransitModes", "WALK")
                            .queryParam("postTransitModes", "WALK")
                            .queryParam("detailedLegs", true)
                            .queryParam("detailedTransfers", true)
                            .queryParam("maxItineraries", JourneyPlan.MAX_JOURNEYS)
                            .queryParam("realtimeMode", "REALTIME")
                            .build())
                    .exchange((httpRequest, httpResponse) -> handleResponse(httpResponse));
            return parsePlan(response);
        } catch (JourneyProviderException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            JourneyFailureKind kind = hasCause(exception, SocketTimeoutException.class)
                    || hasCause(exception, HttpTimeoutException.class)
                    ? JourneyFailureKind.TIMEOUT
                    : JourneyFailureKind.PROVIDER_UNAVAILABLE;
            throw new JourneyProviderException(kind, "Journey provider request failed.", exception);
        } catch (RestClientException exception) {
            throw new JourneyProviderException(JourneyFailureKind.PROVIDER_UNAVAILABLE,
                    "Journey provider request failed.", exception);
        }
    }

    private String place(Coordinate coordinate) {
        return Double.toString(coordinate.latitude()) + "," + Double.toString(coordinate.longitude());
    }

    private JsonNode handleResponse(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = readResponseBody(response);
        if (status == 429) {
            throw failure(JourneyFailureKind.RATE_LIMITED, "Journey provider rate limit reached.");
        }
        if (status == 408 || status == 504) {
            throw failure(JourneyFailureKind.TIMEOUT, "Journey provider timed out.");
        }
        if (status == 400 || status == 404 || status == 422) {
            throw failure(JourneyFailureKind.INVALID_PROVIDER_RESPONSE,
                    "Journey provider rejected a valid Orientation request.");
        }
        if (status < 200 || status >= 300) {
            throw failure(JourneyFailureKind.PROVIDER_UNAVAILABLE, "Journey provider is unavailable.");
        }
        try {
            return objectMapper.readTree(body);
        } catch (RuntimeException exception) {
            throw invalid("Journey provider response is not valid JSON.", exception);
        }
    }

    private JourneyPlan parsePlan(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw invalid("Journey provider response must be an object.");
        }
        JsonNode itineraries = root.path("itineraries");
        if (!itineraries.isArray()) {
            throw invalid("Journey provider response does not contain itineraries.");
        }
        if (itineraries.isEmpty()) {
            throw failure(JourneyFailureKind.NO_JOURNEY_FOUND, "Journey provider found no public-transit journey.");
        }

        List<Journey> journeys = new ArrayList<>();
        int count = Math.min(itineraries.size(), JourneyPlan.MAX_JOURNEYS);
        for (int index = 0; index < count; index++) {
            journeys.add(parseJourney(itineraries.get(index)));
        }
        try {
            return new JourneyPlan(journeys);
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned an invalid Journey plan.", exception);
        }
    }

    private Journey parseJourney(JsonNode itinerary) {
        if (itinerary == null || !itinerary.isObject()) {
            throw invalid("Journey provider itinerary must be an object.");
        }
        JsonNode transfersNode = itinerary.get("transfers");
        JsonNode legsNode = itinerary.get("legs");
        if (transfersNode == null || !transfersNode.isIntegralNumber() || transfersNode.intValue() < 0
                || legsNode == null || !legsNode.isArray() || legsNode.isEmpty()) {
            throw invalid("Journey provider itinerary is missing valid transfers or legs.");
        }
        if (legsNode.size() > Journey.MAX_LEGS) {
            throw invalid("Journey provider itinerary exceeds the Orientation leg limit.");
        }

        List<JourneyLeg> legs = new ArrayList<>(legsNode.size());
        for (JsonNode legNode : legsNode) {
            legs.add(parseLeg(legNode));
        }
        try {
            return new Journey(legs, transfersNode.intValue());
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned an invalid Journey.", exception);
        }
    }

    private JourneyLeg parseLeg(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw invalid("Journey provider leg must be an object.");
        }
        JsonNode realTimeNode = node.get("realTime");
        if (realTimeNode == null || !realTimeNode.isBoolean()) {
            throw invalid("Journey provider leg is missing realtime state.");
        }

        JourneyLegMode mode = mapMode(requiredText(node, "mode"));
        JourneyStop origin = parseStop(node.get("from"));
        JourneyStop destination = parseStop(node.get("to"));
        OffsetDateTime scheduledStart = requiredTime(node, "scheduledStartTime");
        OffsetDateTime scheduledEnd = requiredTime(node, "scheduledEndTime");
        OffsetDateTime effectiveStart = requiredTime(node, "startTime");
        OffsetDateTime effectiveEnd = requiredTime(node, "endTime");
        boolean realTime = realTimeNode.booleanValue();

        JourneyEventTime departure = new JourneyEventTime(scheduledStart, realTime ? effectiveStart : null);
        JourneyEventTime arrival = new JourneyEventTime(scheduledEnd, realTime ? effectiveEnd : null);
        TransitService service = mode.isTransit() ? parseService(node, mode) : null;
        JourneyLegGeometry geometry = parseGeometry(node.get("legGeometry"));
        List<JourneyStop> intermediateStops = parseIntermediateStops(node.get("intermediateStops"), mode);

        try {
            return new JourneyLeg(mode, origin, destination, departure, arrival, service, geometry, intermediateStops);
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned an invalid leg.", exception);
        }
    }

    private JourneyStop parseStop(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw invalid("Journey provider leg stop must be an object.");
        }
        String name = requiredText(node, "name");
        JsonNode lat = node.get("lat");
        JsonNode lon = node.get("lon");
        if (lat == null || !lat.isNumber() || lon == null || !lon.isNumber()) {
            throw invalid("Journey provider leg stop requires numeric coordinates.");
        }
        try {
            return new JourneyStop(name, new Coordinate(lon.doubleValue(), lat.doubleValue()));
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned an invalid leg stop.", exception);
        }
    }

    private TransitService parseService(JsonNode node, JourneyLegMode mode) {
        String label = optionalText(node, "displayName");
        if (label == null) {
            label = optionalText(node, "routeShortName");
        }
        if (label == null) {
            label = fallbackServiceLabel(mode);
        }
        String headsign = optionalText(node, "headsign");
        try {
            return new TransitService(label, headsign);
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned invalid transit service information.", exception);
        }
    }

    private JourneyLegGeometry parseGeometry(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw invalid("Journey provider leg geometry must be an object.");
        }
        JsonNode points = node.get("points");
        if (points == null || !points.isTextual() || points.asText().isBlank()) {
            return null;
        }
        JsonNode precision = node.get("precision");
        if (precision == null || !precision.isIntegralNumber() || precision.intValue() != EXPECTED_POLYLINE_PRECISION) {
            throw invalid("Journey provider leg geometry uses an unexpected polyline precision.");
        }
        try {
            return new JourneyLegGeometry(decodePolyline(points.asText(), EXPECTED_POLYLINE_PRECISION));
        } catch (IllegalArgumentException exception) {
            throw invalid("Journey provider returned invalid leg geometry.", exception);
        }
    }

    private List<JourneyStop> parseIntermediateStops(JsonNode node, JourneyLegMode mode) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw invalid("Journey provider intermediate stops must be an array.");
        }
        if (!mode.isTransit() && !node.isEmpty()) {
            throw invalid("Journey provider returned intermediate transit stops for a walking leg.");
        }
        if (node.size() > JourneyLeg.MAX_INTERMEDIATE_STOPS) {
            throw invalid("Journey provider leg exceeds the Orientation intermediate-stop limit.");
        }
        List<JourneyStop> stops = new ArrayList<>(node.size());
        for (JsonNode stop : node) {
            stops.add(parseStop(stop));
        }
        return List.copyOf(stops);
    }

    private JourneyLegMode mapMode(String mode) {
        return switch (mode.toUpperCase(Locale.ROOT)) {
            case "WALK" -> JourneyLegMode.WALK;
            case "TRAM" -> JourneyLegMode.TRAM;
            case "SUBWAY" -> JourneyLegMode.SUBWAY;
            case "FERRY" -> JourneyLegMode.FERRY;
            case "BUS" -> JourneyLegMode.BUS;
            case "COACH" -> JourneyLegMode.COACH;
            case "SUBURBAN", "METRO" -> JourneyLegMode.SUBURBAN_RAIL;
            case "RAIL", "HIGHSPEED_RAIL", "LONG_DISTANCE", "NIGHT_RAIL", "REGIONAL_FAST_RAIL", "REGIONAL_RAIL" -> JourneyLegMode.RAIL;
            case "FUNICULAR", "AERIAL_LIFT", "AREAL_LIFT" -> JourneyLegMode.OTHER_TRANSIT;
            default -> throw invalid("Journey provider returned an unsupported leg mode.");
        };
    }

    private String fallbackServiceLabel(JourneyLegMode mode) {
        return switch (mode) {
            case RAIL -> "Rail";
            case SUBURBAN_RAIL -> "Suburban rail";
            case SUBWAY -> "Subway";
            case TRAM -> "Tram";
            case BUS -> "Bus";
            case COACH -> "Coach";
            case FERRY -> "Ferry";
            case OTHER_TRANSIT -> "Transit";
            case WALK -> throw new IllegalArgumentException("Walking legs have no transit service label.");
        };
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw invalid("Journey provider response is missing required text field: " + field + ".");
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalid("Journey provider response contains a non-text field: " + field + ".");
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private OffsetDateTime requiredTime(JsonNode node, String field) {
        String value = requiredText(node, field);
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid("Journey provider returned an invalid offset-aware time.", exception);
        }
    }

    static List<Coordinate> decodePolyline(String encoded, int precision) {
        if (encoded == null || encoded.isBlank() || precision < 0 || precision > 7) {
            throw new IllegalArgumentException("Encoded polyline and supported precision are required.");
        }
        double factor = Math.pow(10d, precision);
        List<Coordinate> coordinates = new ArrayList<>();
        long latitude = 0;
        long longitude = 0;
        int index = 0;
        while (index < encoded.length()) {
            DecodedDelta latitudeDelta = decodeDelta(encoded, index);
            index = latitudeDelta.nextIndex();
            DecodedDelta longitudeDelta = decodeDelta(encoded, index);
            index = longitudeDelta.nextIndex();
            latitude += latitudeDelta.value();
            longitude += longitudeDelta.value();
            coordinates.add(new Coordinate(longitude / factor, latitude / factor));
            if (coordinates.size() > JourneyLegGeometry.MAX_COORDINATES) {
                throw new IllegalArgumentException("Decoded polyline exceeds the coordinate limit.");
            }
        }
        if (coordinates.size() < 2) {
            throw new IllegalArgumentException("Decoded polyline requires at least two coordinates.");
        }
        return List.copyOf(coordinates);
    }

    private static DecodedDelta decodeDelta(String encoded, int startIndex) {
        long value = 0;
        int shift = 0;
        int index = startIndex;
        int current;
        do {
            if (index >= encoded.length() || shift > 60) {
                throw new IllegalArgumentException("Encoded polyline is malformed.");
            }
            current = encoded.charAt(index++) - 63;
            if (current < 0 || current > 63) {
                throw new IllegalArgumentException("Encoded polyline is malformed.");
            }
            value |= (long) (current & 0x1f) << shift;
            shift += 5;
        } while (current >= 0x20);
        long delta = (value & 1L) == 0 ? value >> 1 : ~(value >> 1);
        return new DecodedDelta(delta, index);
    }

    static String readResponseBody(ClientHttpResponse response) {
        long contentLength = response.getHeaders().getContentLength();
        if (contentLength > MAX_PROVIDER_RESPONSE_BYTES) {
            throw invalidStatic("Journey provider response is too large.");
        }
        InputStream body;
        try {
            body = response.getBody();
        } catch (IOException exception) {
            throw new RestClientException("Could not read Journey provider response.", exception);
        }
        if (body == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(
                contentLength > 0 ? (int) contentLength : 8192,
                MAX_PROVIDER_RESPONSE_BYTES));
        int total = 0;
        try {
            while (total <= MAX_PROVIDER_RESPONSE_BYTES) {
                int remaining = MAX_PROVIDER_RESPONSE_BYTES + 1 - total;
                byte[] buffer = new byte[Math.min(8192, remaining)];
                int read = body.read(buffer);
                if (read == -1) {
                    break;
                }
                total += read;
                if (total > MAX_PROVIDER_RESPONSE_BYTES) {
                    throw invalidStatic("Journey provider response is too large.");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RestClientException("Could not read Journey provider response.", exception);
        }
    }

    private JourneyProviderException invalid(String message) {
        return new JourneyProviderException(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, message);
    }

    private JourneyProviderException invalid(String message, Throwable cause) {
        return new JourneyProviderException(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, message, cause);
    }

    private JourneyProviderException failure(JourneyFailureKind kind, String message) {
        return new JourneyProviderException(kind, message);
    }

    private static JourneyProviderException invalidStatic(String message) {
        return new JourneyProviderException(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, message);
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private record DecodedDelta(long value, int nextIndex) {
    }
}
