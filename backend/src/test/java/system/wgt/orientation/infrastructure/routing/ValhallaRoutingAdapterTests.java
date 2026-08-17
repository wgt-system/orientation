package system.wgt.orientation.infrastructure.routing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import system.wgt.orientation.application.routing.RoutingFailureKind;
import system.wgt.orientation.application.routing.RoutingProviderException;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.routing.RouteRequest;
import system.wgt.orientation.domain.routing.TravelProfile;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValhallaRoutingAdapterTests {
    private static final String SHAPE = "_zlceB_vv`R_pR_pR_pR_pR";

    private HttpServer server;
    private AtomicReference<Response> response;
    private AtomicReference<String> requestBody;
    private ValhallaRoutingAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        response = new AtomicReference<>(new Response(200, successFixture(), 0));
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/route", this::respond);
        server.start();
        adapter = adapter(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsProviderRouteAndKeepsValhallaTermsBehindThePort() throws Exception {
        var request = new RouteRequest(new Coordinate(9.99, 53.55), new Coordinate(10.01, 53.57),
                TravelProfile.DRIVING);

        var route = adapter.route(request);

        assertEquals(request.origin(), route.origin());
        assertEquals(request.destination(), route.destination());
        assertEquals(TravelProfile.DRIVING, route.profile());
        assertEquals(1250d, route.distanceMeters());
        assertEquals(180.5d, route.durationSeconds());
        assertEquals(3, route.geometry().coordinates().size());
        assertEquals(new Coordinate(9.99, 53.55), route.geometry().coordinates().get(0));
        assertEquals(new Coordinate(10.01, 53.57), route.geometry().coordinates().get(2));

        JsonNode upstreamRequest = objectMapper.readTree(requestBody.get());
        assertEquals("auto", upstreamRequest.path("costing").asText());
        assertEquals("kilometers", upstreamRequest.path("units").asText());
        assertEquals("none", upstreamRequest.path("directions_type").asText());
        assertEquals(53.55, upstreamRequest.path("locations").get(0).path("lat").doubleValue());
        assertEquals(9.99, upstreamRequest.path("locations").get(0).path("lon").doubleValue());
    }

    @Test
    void mapsAllOrientationTravelProfilesToValhallaCosting() throws Exception {
        assertCosting(TravelProfile.DRIVING, "auto");
        assertCosting(TravelProfile.CYCLING, "bicycle");
        assertCosting(TravelProfile.WALKING, "pedestrian");
    }

    @Test
    void mapsDocumentedNoRouteErrors() {
        for (int errorCode : new int[]{170, 171, 441, 442}) {
            response.set(new Response(400,
                    "{\"error_code\":" + errorCode + ",\"error\":\"No path\",\"status_code\":400}", 0));
            assertEquals(RoutingFailureKind.NO_ROUTE_FOUND, failure().kind());
        }
    }

    @Test
    void mapsRateLimitTimeoutAndProviderAvailability() {
        response.set(new Response(429, "{}", 0));
        assertEquals(RoutingFailureKind.RATE_LIMITED, failure().kind());

        response.set(new Response(504, "{}", 0));
        assertEquals(RoutingFailureKind.TIMEOUT, failure().kind());

        response.set(new Response(503, "{}", 0));
        assertEquals(RoutingFailureKind.PROVIDER_UNAVAILABLE, failure().kind());
    }

    @Test
    void treatsOtherProviderClientErrorsAsInvalidProviderBehavior() {
        response.set(new Response(400, "{\"error_code\":125,\"error\":\"No costing method found\"}", 0));

        assertEquals(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());
    }

    @Test
    void rejectsMalformedSuccessfulResponsesAndPolylineGeometry() {
        response.set(new Response(200, "{\"trip\":{\"status\":0}}", 0));
        assertEquals(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());

        response.set(new Response(200, successFixture().replace(SHAPE, "?"), 0));
        assertEquals(RoutingFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());
    }

    @Test
    void mapsReadTimeout() {
        response.set(new Response(200, successFixture(), 250));
        ValhallaRoutingAdapter shortTimeoutAdapter = adapter(Duration.ofMillis(25));

        assertEquals(RoutingFailureKind.TIMEOUT, assertThrows(RoutingProviderException.class,
                () -> shortTimeoutAdapter.route(request(TravelProfile.DRIVING))).kind());
    }

    @Test
    void provesResponseReaderConsumesAtMostMaxPlusOneBytes() throws IOException {
        CountingInputStream atLimit = new CountingInputStream(new byte[ValhallaRoutingAdapter.MAX_PROVIDER_RESPONSE_BYTES]);
        ValhallaRoutingAdapter.readResponseBody(responseWithBody(atLimit, -1));
        assertEquals(ValhallaRoutingAdapter.MAX_PROVIDER_RESPONSE_BYTES, atLimit.bytesRead);

        CountingInputStream overLimit = new CountingInputStream(
                new byte[ValhallaRoutingAdapter.MAX_PROVIDER_RESPONSE_BYTES + 2]);
        assertEquals(RoutingFailureKind.INVALID_PROVIDER_RESPONSE,
                assertThrows(RoutingProviderException.class,
                        () -> ValhallaRoutingAdapter.readResponseBody(responseWithBody(overLimit, -1))).kind());
        assertEquals(ValhallaRoutingAdapter.MAX_PROVIDER_RESPONSE_BYTES + 1, overLimit.bytesRead);
    }

    private void assertCosting(TravelProfile profile, String expected) throws Exception {
        adapter.route(request(profile));
        JsonNode upstreamRequest = objectMapper.readTree(requestBody.get());
        assertEquals(expected, upstreamRequest.path("costing").asText());
    }

    private RoutingProviderException failure() {
        return assertThrows(RoutingProviderException.class,
                () -> adapter.route(request(TravelProfile.DRIVING)));
    }

    private RouteRequest request(TravelProfile profile) {
        return new RouteRequest(new Coordinate(9.99, 53.55), new Coordinate(10.01, 53.57), profile);
    }

    private ValhallaRoutingAdapter adapter(Duration readTimeout) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return new ValhallaRoutingAdapter(RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(factory)
                .build(), objectMapper);
    }

    private void respond(HttpExchange exchange) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        Response current = response.get();
        if (current.delayMillis() > 0) {
            try {
                Thread.sleep(current.delayMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = current.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(current.status(), bytes.length);
        try {
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

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

    private String successFixture() {
        return "{\"trip\":{\"status\":0,\"units\":\"kilometers\","
                + "\"summary\":{\"length\":1.25,\"time\":180.5},"
                + "\"legs\":[{\"shape\":\"" + SHAPE + "\"}]}}";
    }

    private record Response(int status, String body, long delayMillis) {
    }

    private static final class CountingInputStream extends InputStream {
        private final byte[] content;
        private int position;
        private int bytesRead;

        private CountingInputStream(byte[] content) {
            this.content = content;
        }

        @Override
        public int read() {
            if (position >= content.length) {
                return -1;
            }
            bytesRead++;
            return content[position++];
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position >= content.length) {
                return -1;
            }
            int count = Math.min(length, content.length - position);
            System.arraycopy(content, position, buffer, offset, count);
            position += count;
            bytesRead += count;
            return count;
        }
    }
}
