package system.wgt.orientation.infrastructure.routing;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import system.wgt.orientation.application.routing.RoutingFailureKind;
import system.wgt.orientation.application.routing.RoutingPort;
import system.wgt.orientation.application.routing.RoutingProviderException;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteGeometry;
import system.wgt.orientation.domain.routing.RouteRequest;
import system.wgt.orientation.domain.routing.TravelProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValhallaRoutingAdapter implements RoutingPort {
    static final int MAX_PROVIDER_RESPONSE_BYTES = 2_097_152;
    private static final double POLYLINE_PRECISION = 1_000_000d;
    private static final Set<Integer> NO_ROUTE_ERROR_CODES = Set.of(170, 171, 441, 442);

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public ValhallaRoutingAdapter(RestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public Route route(RouteRequest request) {
        String requestBody = requestBody(request);
        try {
            JsonNode response = client.post()
                    .uri("/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .exchange((httpRequest, httpResponse) -> handleResponse(httpResponse));
            return parseRoute(request, response);
        } catch (RoutingProviderException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            RoutingFailureKind kind = hasCause(exception, SocketTimeoutException.class)
                    || hasCause(exception, HttpTimeoutException.class)
                    ? RoutingFailureKind.TIMEOUT
                    : RoutingFailureKind.PROVIDER_UNAVAILABLE;
            throw new RoutingProviderException(kind, "Routing provider request failed.", exception);
        } catch (RestClientException exception) {
            throw new RoutingProviderException(RoutingFailureKind.PROVIDER_UNAVAILABLE,
                    "Routing provider request failed.", exception);
        }
    }

    private String requestBody(RouteRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode locations = root.putArray("locations");
        addLocation(locations, request.origin());
        addLocation(locations, request.destination());
        root.put("costing", costing(request.profile()));
        root.put("units", "kilometers");
        root.put("directions_type", "none");
        return root.toString();
    }

    private void addLocation(ArrayNode locations, Coordinate coordinate) {
        ObjectNode location = locations.addObject();
        location.put("lat", coordinate.latitude());
        location.put("lon", coordinate.longitude());
    }

    private String costing(TravelProfile profile) {
        return switch (profile) {
            case DRIVING -> "auto";
            case CYCLING -> "bicycle";
            case WALKING -> "pedestrian";
        };
    }

    private JsonNode handleResponse(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = readResponseBody(response);
        if (status == 429) {
            throw new RoutingProviderException(RoutingFailureKind.RATE_LIMITED,
                    "Routing provider rate limit reached.");
        }
        if (status == 408 || status == 504) {
            throw new RoutingProviderException(RoutingFailureKind.TIMEOUT,
                    "Routing provider timed out.");
        }
        if (status == 400 && isNoRouteResponse(body)) {
            throw new RoutingProviderException(RoutingFailureKind.NO_ROUTE_FOUND,
                    "Routing provider found no route.");
        }
        if (status >= 400 && status < 500) {
            throw new RoutingProviderException(RoutingFailureKind.INVALID_PROVIDER_RESPONSE,
                    "Routing provider rejected a valid Orientation request.");
        }
        if (status < 200 || status >= 300) {
            throw new RoutingProviderException(RoutingFailureKind.PROVIDER_UNAVAILABLE,
                    "Routing provider is unavailable.");
        }
        try {
            return objectMapper.readTree(body);
        } catch (RuntimeException exception) {
            throw invalid("Routing provider response is not valid JSON.", exception);
        }
    }

    private boolean isNoRouteResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode code = root == null ? null : root.get("error_code");
            return code != null && code.isIntegralNumber() && NO_ROUTE_ERROR_CODES.contains(code.intValue());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Route parseRoute(RouteRequest request, JsonNode root) {
        if (root == null || !root.isObject()) {
            throw invalid("Routing provider response must be an object.");
        }
        JsonNode trip = root.path("trip");
        if (!trip.isObject() || !trip.path("status").isIntegralNumber() || trip.path("status").intValue() != 0) {
            throw invalid("Routing provider response does not contain a successful trip.");
        }
        if (!"kilometers".equals(trip.path("units").asText())) {
            throw invalid("Routing provider response uses unexpected distance units.");
        }

        JsonNode summary = trip.path("summary");
        if (!summary.isObject() || !summary.path("length").isNumber() || !summary.path("time").isNumber()) {
            throw invalid("Routing provider response does not contain a valid trip summary.");
        }
        double distanceMeters = summary.path("length").doubleValue() * 1000d;
        double durationSeconds = summary.path("time").doubleValue();
        if (!Double.isFinite(distanceMeters) || distanceMeters < 0
                || !Double.isFinite(durationSeconds) || durationSeconds < 0) {
            throw invalid("Routing provider returned invalid distance or duration.");
        }

        JsonNode legs = trip.path("legs");
        if (!legs.isArray() || legs.isEmpty()) {
            throw invalid("Routing provider response does not contain route legs.");
        }
        List<Coordinate> geometry = new ArrayList<>();
        for (JsonNode leg : legs) {
            if (!leg.isObject() || !leg.path("shape").isTextual()) {
                throw invalid("Routing provider route leg does not contain a polyline6 shape.");
            }
            appendGeometry(geometry, decodePolyline6(leg.path("shape").asText()));
            if (geometry.size() > RouteGeometry.MAX_COORDINATES) {
                throw invalid("Routing provider route geometry exceeds the Orientation coordinate limit.");
            }
        }

        try {
            return new Route(request.origin(), request.destination(), request.profile(),
                    new RouteGeometry(geometry), distanceMeters, durationSeconds);
        } catch (IllegalArgumentException exception) {
            throw invalid("Routing provider returned an invalid route.", exception);
        }
    }

    private void appendGeometry(List<Coordinate> target, List<Coordinate> leg) {
        int start = !target.isEmpty() && !leg.isEmpty() && target.get(target.size() - 1).equals(leg.get(0)) ? 1 : 0;
        for (int index = start; index < leg.size(); index++) {
            target.add(leg.get(index));
        }
    }

    static List<Coordinate> decodePolyline6(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw invalidStatic("Routing provider returned an empty route shape.");
        }
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
            try {
                coordinates.add(new Coordinate(longitude / POLYLINE_PRECISION, latitude / POLYLINE_PRECISION));
            } catch (IllegalArgumentException exception) {
                throw invalidStatic("Routing provider returned out-of-range route coordinates.", exception);
            }
            if (coordinates.size() > RouteGeometry.MAX_COORDINATES) {
                throw invalidStatic("Routing provider route geometry exceeds the Orientation coordinate limit.");
            }
        }
        if (coordinates.size() < 2) {
            throw invalidStatic("Routing provider route geometry requires at least two coordinates.");
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
                throw invalidStatic("Routing provider returned malformed polyline6 geometry.");
            }
            current = encoded.charAt(index++) - 63;
            if (current < 0 || current > 63) {
                throw invalidStatic("Routing provider returned malformed polyline6 geometry.");
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
            throw invalidStatic("Routing provider response is too large.");
        }
        InputStream body;
        try {
            body = response.getBody();
        } catch (IOException exception) {
            throw new RestClientException("Could not read routing provider response.", exception);
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
                    throw invalidStatic("Routing provider response is too large.");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RestClientException("Could not read routing provider response.", exception);
        }
    }

    private RoutingProviderException invalid(String message) {
        return new RoutingProviderException(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, message);
    }

    private RoutingProviderException invalid(String message, Throwable cause) {
        return new RoutingProviderException(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, message, cause);
    }

    private static RoutingProviderException invalidStatic(String message) {
        return new RoutingProviderException(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, message);
    }

    private static RoutingProviderException invalidStatic(String message, Throwable cause) {
        return new RoutingProviderException(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, message, cause);
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
