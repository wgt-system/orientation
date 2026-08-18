package system.wgt.orientation.infrastructure.journey;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.PlaceSearchPort;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.application.place.ReverseGeocodingPort;
import system.wgt.orientation.domain.place.AddressComponents;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MotisPlaceAdapter implements PlaceSearchPort, ReverseGeocodingPort {
    static final int MAX_PROVIDER_RESPONSE_BYTES = 1_048_576;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public MotisPlaceAdapter(RestClient client, ObjectMapper objectMapper) {
        if (client == null || objectMapper == null) {
            throw new IllegalArgumentException("MOTIS client and object mapper are required.");
        }
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Place> search(PlaceSearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Place search query is required.");
        }
        JsonNode response = get("/api/v1/geocode", uri -> {
            uri.queryParam("text", query.text());
            uri.queryParam("numResults", query.limit());
            query.language().ifPresent(language -> uri.queryParam("language", language));
            query.locationBias().ifPresent(bias -> uri.queryParam("place",
                    bias.coordinate().latitude() + "," + bias.coordinate().longitude()));
        });
        return parseMatches(response);
    }

    @Override
    public Optional<Place> reverse(ReverseGeocodeQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Reverse geocode query is required.");
        }
        JsonNode response = get("/api/v1/reverse-geocode", uri -> {
            uri.queryParam("place", query.coordinate().latitude() + "," + query.coordinate().longitude());
            uri.queryParam("numResults", 1);
        });
        return parseMatches(response).stream().findFirst();
    }

    private JsonNode get(String path, java.util.function.Consumer<org.springframework.web.util.UriBuilder> parameters) {
        try {
            return client.get()
                    .uri(uri -> {
                        uri.path(path);
                        parameters.accept(uri);
                        return uri.build();
                    })
                    .exchange((request, response) -> handleResponse(response));
        } catch (PlaceProviderException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            ProviderFailureKind kind = hasCause(exception, SocketTimeoutException.class)
                    || hasCause(exception, HttpTimeoutException.class)
                    ? ProviderFailureKind.TIMEOUT
                    : ProviderFailureKind.UNAVAILABLE;
            throw new PlaceProviderException(kind, "Place provider request failed.", exception);
        } catch (RestClientException exception) {
            throw new PlaceProviderException(ProviderFailureKind.UNAVAILABLE, "Place provider request failed.", exception);
        }
    }

    private JsonNode handleResponse(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = readResponseBody(response);
        if (status == 429) {
            throw new PlaceProviderException(ProviderFailureKind.RATE_LIMITED, "Place provider rate limit reached.");
        }
        if (status == 408 || status == 504) {
            throw new PlaceProviderException(ProviderFailureKind.TIMEOUT, "Place provider timed out.");
        }
        if (status < 200 || status >= 300) {
            throw new PlaceProviderException(ProviderFailureKind.UNAVAILABLE, "Place provider is unavailable.");
        }
        try {
            return objectMapper.readTree(body);
        } catch (RuntimeException exception) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE,
                    "Place provider response is invalid.", exception);
        }
    }

    static String readResponseBody(ClientHttpResponse response) {
        long contentLength = response.getHeaders().getContentLength();
        if (contentLength > MAX_PROVIDER_RESPONSE_BYTES) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE,
                    "Place provider response is too large.");
        }
        InputStream body;
        try {
            body = response.getBody();
        } catch (IOException exception) {
            throw new RestClientException("Could not read place provider response.", exception);
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
                    throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE,
                            "Place provider response is too large.");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RestClientException("Could not read place provider response.", exception);
        }
    }

    private List<Place> parseMatches(JsonNode root) {
        if (root == null || !root.isArray()) {
            throw invalid("MOTIS geocoding response must be an array.");
        }
        List<Place> places = new ArrayList<>();
        for (JsonNode match : root) {
            places.add(parseMatch(match));
        }
        return List.copyOf(places);
    }

    private Place parseMatch(JsonNode match) {
        if (!match.isObject()) {
            throw invalid("MOTIS geocoding match must be an object.");
        }
        String id = requiredText(match, "id");
        String name = requiredText(match, "name");
        double latitude = requiredNumber(match, "lat");
        double longitude = requiredNumber(match, "lon");
        Coordinate coordinate;
        try {
            coordinate = new Coordinate(longitude, latitude);
        } catch (IllegalArgumentException exception) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE,
                    "MOTIS geocoding returned out-of-range coordinates.", exception);
        }

        Optional<String> type = text(match, "type");
        AddressComponents address = new AddressComponents(
                Optional.of(name),
                text(match, "street"),
                text(match, "houseNumber"),
                text(match, "zip"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                text(match, "country"));

        return new Place("motis:" + id, name, coordinate, Optional.empty(), type, address);
    }

    private String requiredText(JsonNode node, String field) {
        return text(node, field).orElseThrow(() -> invalid("MOTIS geocoding response is missing " + field + "."));
    }

    private double requiredNumber(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw invalid("MOTIS geocoding response is missing numeric " + field + ".");
        }
        return value.doubleValue();
    }

    private Optional<String> text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isValueNode() && !value.asText().isBlank()
                ? Optional.of(value.asText().trim())
                : Optional.empty();
    }

    private PlaceProviderException invalid(String message) {
        return new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, message);
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
