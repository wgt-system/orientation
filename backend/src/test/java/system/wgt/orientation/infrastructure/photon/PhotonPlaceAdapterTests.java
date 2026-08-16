package system.wgt.orientation.infrastructure.photon;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhotonPlaceAdapterTests {
    private HttpServer server;
    private AtomicReference<Response> response;
    private PhotonPlaceAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        response = new AtomicReference<>(new Response(200, fixture(), 0));
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::respond);
        server.start();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(1));
        adapter = new PhotonPlaceAdapter(RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(factory).build(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsSearchFeaturesAndEncodesQueryParameters() {
        var query = new PlaceSearchQuery("Hamburg Hauptbahnhof", 5, Optional.of("de"),
                Optional.of(new PlaceSearchQuery.LocationBias(new Coordinate(10, 50), 12)));

        var places = adapter.search(query);

        assertEquals(1, places.size());
        assertEquals("N:123", places.get(0).providerReference());
        assertEquals("Hamburg Hauptbahnhof", places.get(0).displayLabel());
        assertEquals(9.99, places.get(0).coordinate().longitude());
        assertEquals(53.55, places.get(0).coordinate().latitude());
        assertEquals(Optional.empty(), places.get(0).kind());
        assertEquals("Hamburg Hauptbahnhof", lastQuery.get().get("q"));
        assertEquals("5", lastQuery.get().get("limit"));
        assertEquals("de", lastQuery.get().get("lang"));
        assertEquals("50.0", lastQuery.get().get("lat"));
        assertEquals("10.0", lastQuery.get().get("lon"));
    }

    @Test
    void mapsReverseAndPreservesLongitudeLatitudeOrder() {
        var place = adapter.reverse(new ReverseGeocodeQuery(new Coordinate(9.99, 53.55), Optional.empty()));

        assertEquals("N:123", place.orElseThrow().providerReference());
        assertEquals("53.55", lastQuery.get().get("lat"));
        assertEquals("9.99", lastQuery.get().get("lon"));
        assertEquals("1", lastQuery.get().get("limit"));
    }

    @Test
    void acceptsEmptyFeatureCollectionAsNoResults() {
        response.set(new Response(200, "{\"type\":\"FeatureCollection\",\"features\":[]}", 0));

        assertEquals(List.of(), adapter.search(new PlaceSearchQuery("unknown", 5, Optional.empty(), Optional.empty())));
        assertEquals(Optional.empty(), adapter.reverse(new ReverseGeocodeQuery(new Coordinate(0, 0), Optional.empty())));
    }

    @Test
    void mapsRateLimitAndInvalidResponses() {
        response.set(new Response(429, "", 0));
        assertEquals(ProviderFailureKind.RATE_LIMITED, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());

        response.set(new Response(200, "not-json", 0));
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    @Test
    void rejectsResponsesOverTheProviderBound() {
        response.set(new Response(200, "{" + "\"type\":\"FeatureCollection\",\"features\":[" + "x".repeat(PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES) + "]}", 0));

        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    @Test
    void provesTheReaderConsumesAtMostMaxPlusOneBytes() throws IOException {
        CountingInputStream atLimit = new CountingInputStream(new byte[PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES]);
        ClientHttpResponse atLimitResponse = responseWithBody(atLimit, -1);
        PhotonPlaceAdapter.readResponseBody(atLimitResponse);
        assertEquals(PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES, atLimit.bytesRead);

        CountingInputStream overLimit = new CountingInputStream(new byte[PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES + 2]);
        ClientHttpResponse overLimitResponse = responseWithBody(overLimit, -1);
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> PhotonPlaceAdapter.readResponseBody(overLimitResponse)).kind());
        assertEquals(PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES + 1, overLimit.bytesRead);

        CountingInputStream contentLengthRejected = new CountingInputStream(new byte[PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES + 2]);
        ClientHttpResponse contentLengthResponse = responseWithBody(contentLengthRejected, PhotonPlaceAdapter.MAX_PROVIDER_RESPONSE_BYTES + 1L);
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> PhotonPlaceAdapter.readResponseBody(contentLengthResponse)).kind());
        assertEquals(0, contentLengthRejected.bytesRead);
    }

    @Test
    void rejectsMalformedGeometryAndProviderCoordinates() {
        response.set(new Response(200, "{\"type\":\"FeatureCollection\",\"features\":[{\"geometry\":{\"type\":\"LineString\"}}]}", 0));
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());

        response.set(new Response(200, fixture().replace("9.99,53.55", "181,53.55"), 0));
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    @Test
    void mapsUpstreamServerFailure() {
        response.set(new Response(503, "", 0));
        assertEquals(ProviderFailureKind.UNAVAILABLE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    @Test
    void mapsReadTimeout() {
        response.set(new Response(200, fixture(), 250));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofMillis(25));
        PhotonPlaceAdapter shortTimeoutAdapter = new PhotonPlaceAdapter(RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort()).requestFactory(factory).build(), new ObjectMapper());

        assertEquals(ProviderFailureKind.TIMEOUT, assertThrows(PlaceProviderException.class,
                () -> shortTimeoutAdapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    private final AtomicReference<java.util.Map<String, String>> lastQuery = new AtomicReference<>();

    private ClientHttpResponse responseWithBody(InputStream body, long contentLength) {
        HttpHeaders headers = new HttpHeaders();
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getHeaders()).thenReturn(headers);
        try {
            when(response.getBody()).thenReturn(body);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        return response;
    }

    private static final class CountingInputStream extends InputStream {
        private final byte[] content;
        private int position;
        private int bytesRead;

        private CountingInputStream(byte[] content) {
            this.content = content;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position == content.length) {
                return -1;
            }
            int amount = Math.min(length, content.length - position);
            System.arraycopy(content, position, buffer, offset, amount);
            position += amount;
            bytesRead += amount;
            return amount;
        }

        @Override
        public int read() {
            if (position == content.length) {
                return -1;
            }
            bytesRead++;
            return content[position++];
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        lastQuery.set(java.util.Arrays.stream(exchange.getRequestURI().getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(parts -> java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8))));
        Response current = response.get();
        if (current.delayMillis() > 0) {
            try {
                Thread.sleep(current.delayMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = current.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(current.status(), bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String fixture() {
        return "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"id\":\"fallback\",\"properties\":{\"osm_type\":\"N\",\"osm_id\":123,\"name\":\"Hamburg Hauptbahnhof\",\"street\":\"Hauptbahnhof\",\"housenumber\":\"1\",\"postcode\":\"20095\",\"city\":\"Hamburg\",\"country\":\"Germany\",\"countrycode\":\"DE\",\"type\":\"house\",\"extent\":[9.98,53.56,10.0,53.54]},\"geometry\":{\"type\":\"Point\",\"coordinates\":[9.99,53.55]}}]}";
    }

    private record Response(int status, String body, long delayMillis) {
    }
}
