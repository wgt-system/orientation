package system.wgt.orientation.infrastructure.photon;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.PlaceSearchPort;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.application.place.ReverseGeocodingPort;
import system.wgt.orientation.domain.place.AddressComponents;
import system.wgt.orientation.domain.place.BoundingBox;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PhotonPlaceAdapter implements PlaceSearchPort, ReverseGeocodingPort {
    static final int MAX_PROVIDER_RESPONSE_BYTES = 1_048_576;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public PhotonPlaceAdapter(RestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Place> search(PlaceSearchQuery query) {
        JsonNode response = get("/api", uri -> {
            uri.queryParam("q", query.text());
            uri.queryParam("limit", query.limit());
            query.language().ifPresent(language -> uri.queryParam("lang", language));
            query.locationBias().ifPresent(bias -> {
                uri.queryParam("lat", bias.coordinate().latitude());
                uri.queryParam("lon", bias.coordinate().longitude());
                uri.queryParam("zoom", bias.zoom());
            });
        });
        return parsePlaces(response);
    }

    @Override
    public Optional<Place> reverse(ReverseGeocodeQuery query) {
        JsonNode response = get("/reverse", uri -> {
            uri.queryParam("lat", query.coordinate().latitude());
            uri.queryParam("lon", query.coordinate().longitude());
            uri.queryParam("limit", 1);
            query.language().ifPresent(language -> uri.queryParam("lang", language));
        });
        return parsePlaces(response).stream().findFirst();
    }

    private JsonNode get(String path, java.util.function.Consumer<org.springframework.web.util.UriBuilder> parameters) {
        try {
            return client.get()
                    .uri(uri -> {
                        uri.path(path);
                        parameters.accept(uri);
                        return uri.build();
                    })
                    .exchange((request, response) -> {
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
                            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider response is invalid.", exception);
                        }
                    });
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

    private String readResponseBody(ClientHttpResponse response) {
        long contentLength = response.getHeaders().getContentLength();
        if (contentLength > MAX_PROVIDER_RESPONSE_BYTES) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider response is too large.");
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
            int read;
            while (total <= MAX_PROVIDER_RESPONSE_BYTES) {
                int remaining = MAX_PROVIDER_RESPONSE_BYTES + 1 - total;
                byte[] buffer = new byte[Math.min(8192, remaining)];
                read = body.read(buffer);
                if (read == -1) {
                    break;
                }
                total += read;
                if (total > MAX_PROVIDER_RESPONSE_BYTES) {
                    throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider response is too large.");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RestClientException("Could not read place provider response.", exception);
        }
    }

    private List<Place> parsePlaces(JsonNode root) {
        if (root == null || !root.isObject() || !"FeatureCollection".equals(root.path("type").asText()) || !root.path("features").isArray()) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider response is not a FeatureCollection.");
        }
        List<Place> places = new ArrayList<>();
        for (JsonNode feature : root.path("features")) {
            places.add(parsePlace(feature));
        }
        return List.copyOf(places);
    }

    private Place parsePlace(JsonNode feature) {
        if (!feature.isObject() || !"Point".equals(feature.path("geometry").path("type").asText())) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider returned an unsupported geometry.");
        }
        JsonNode coordinates = feature.path("geometry").path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2 || !coordinates.get(0).isNumber() || !coordinates.get(1).isNumber()) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider returned invalid coordinates.");
        }
        Coordinate coordinate;
        try {
            coordinate = new Coordinate(coordinates.get(0).doubleValue(), coordinates.get(1).doubleValue());
        } catch (IllegalArgumentException exception) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider returned out-of-range coordinates.", exception);
        }

        JsonNode properties = feature.path("properties");
        String providerReference = providerReference(feature, properties);
        AddressComponents address = address(properties);
        String label = firstText(properties, "name")
                .or(() -> address.street().map(street -> address.houseNumber().map(number -> street + " " + number).orElse(street)))
                .or(() -> address.city())
                .or(() -> address.country())
                .orElseThrow(() -> invalid("Place provider returned a place without a display label."));

        return new Place(providerReference, label, coordinate, extent(properties), Optional.empty(), address);
    }

    private String providerReference(JsonNode feature, JsonNode properties) {
        String osmType = text(properties, "osm_type").orElse("");
        String osmId = text(properties, "osm_id").orElse("");
        if (!osmType.isBlank() && !osmId.isBlank()) {
            return osmType + ":" + osmId;
        }
        return text(feature, "id").orElseThrow(() -> invalid("Place provider returned a place without an opaque reference."));
    }

    private AddressComponents address(JsonNode properties) {
        return new AddressComponents(
                firstText(properties, "name"), firstText(properties, "street"), firstText(properties, "housenumber"),
                firstText(properties, "postcode"), firstText(properties, "city"), firstText(properties, "county"),
                firstText(properties, "state"), firstText(properties, "country"), firstText(properties, "countrycode"));
    }

    private Optional<BoundingBox> extent(JsonNode properties) {
        JsonNode value = properties.path("extent");
        if (value.isMissingNode() || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isArray() || value.size() != 4
                || !value.get(0).isNumber() || !value.get(1).isNumber()
                || !value.get(2).isNumber() || !value.get(3).isNumber()) {
            throw invalid("Place provider returned an invalid extent.");
        }
        try {
            // Photon encodes extents as [west, north, east, south].
            return Optional.of(new BoundingBox(value.get(0).doubleValue(), value.get(3).doubleValue(),
                    value.get(2).doubleValue(), value.get(1).doubleValue()));
        } catch (IllegalArgumentException exception) {
            throw new PlaceProviderException(ProviderFailureKind.INVALID_RESPONSE, "Place provider returned an invalid extent.", exception);
        }
    }

    private Optional<String> firstText(JsonNode node, String field) {
        return text(node, field);
    }

    private Optional<String> text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
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
