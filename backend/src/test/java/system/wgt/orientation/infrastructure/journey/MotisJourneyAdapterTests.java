package system.wgt.orientation.infrastructure.journey;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import system.wgt.orientation.application.journey.JourneyFailureKind;
import system.wgt.orientation.application.journey.JourneyProviderException;
import system.wgt.orientation.domain.journey.JourneyLegMode;
import system.wgt.orientation.domain.journey.JourneyRequest;
import system.wgt.orientation.domain.journey.JourneyTimeMode;
import system.wgt.orientation.domain.place.Coordinate;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MotisJourneyAdapterTests {
    private static final String SHAPE = "_zlceB_vv`R_pR_pR_pR_pR";
    private static final OffsetDateTime REQUEST_TIME = OffsetDateTime.parse("2026-08-17T22:00:00+02:00");

    private HttpServer server;
    private AtomicReference<Response> response;
    private AtomicReference<String> rawQuery;
    private AtomicReference<String> userAgent;
    private MotisJourneyAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        response = new AtomicReference<>(new Response(200, scheduledFixture(), 0));
        rawQuery = new AtomicReference<>();
        userAgent = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v6/plan", this::respond);
        server.start();
        adapter = adapter(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsDepartAtRequestAndScheduledJourneyWithoutSharingModes() {
        var plan = adapter.plan(request(JourneyTimeMode.DEPART_AT));

        assertEquals(1, plan.journeys().size());
        var journey = plan.journeys().getFirst();
        assertEquals(0, journey.transfers());
        assertEquals(2, journey.legs().size());
        assertEquals(JourneyLegMode.WALK, journey.legs().get(0).mode());
        assertEquals(JourneyLegMode.SUBWAY, journey.legs().get(1).mode());
        assertEquals("U1", journey.legs().get(1).transitService().label());
        assertFalse(journey.legs().get(1).departure().hasRealtimeUpdate());
        assertEquals(3, journey.legs().get(1).geometry().coordinates().size());

        Map<String, String> query = query();
        assertEquals("53.55,9.99", query.get("fromPlace"));
        assertEquals("53.57,10.01", query.get("toPlace"));
        assertEquals(REQUEST_TIME.toString(), query.get("time"));
        assertEquals("false", query.get("arriveBy"));
        assertEquals("", query.get("directModes"));
        assertEquals("WALK", query.get("preTransitModes"));
        assertEquals("WALK", query.get("postTransitModes"));
        assertEquals("true", query.get("detailedLegs"));
        assertEquals("true", query.get("detailedTransfers"));
        assertEquals("8", query.get("maxItineraries"));
        assertEquals("REALTIME", query.get("realtimeMode"));
        assertFalse(query.get("transitModes").contains("RIDE_SHARING"));
        assertFalse(query.get("transitModes").contains("ODM"));
        assertFalse(query.get("transitModes").contains("RENTAL"));
        assertEquals("orientation-test/1.0", userAgent.get());
    }

    @Test
    void mapsArriveByAndKeepsScheduledTimesBesideRealtimeUpdates() {
        response.set(new Response(200, realtimeFixture(), 0));

        var journey = adapter.plan(request(JourneyTimeMode.ARRIVE_BY)).journeys().getFirst();

        assertEquals("true", query().get("arriveBy"));
        assertTrue(journey.legs().get(1).departure().hasRealtimeUpdate());
        assertEquals(OffsetDateTime.parse("2026-08-17T22:07:00+02:00"),
                journey.legs().get(1).departure().scheduledTime());
        assertEquals(OffsetDateTime.parse("2026-08-17T22:10:00+02:00"),
                journey.legs().get(1).departure().realtimeTime());
        assertEquals(OffsetDateTime.parse("2026-08-17T22:30:00+02:00"), journey.arrivalTime());
    }

    @Test
    void boundsDocumentedExtraItinerariesAndRejectsUnsupportedProviderSemantics() {
        response.set(new Response(200, manyItinerariesFixture(10), 0));
        assertEquals(8, adapter.plan(request(JourneyTimeMode.DEPART_AT)).journeys().size());

        response.set(new Response(200, scheduledFixture().replace("\"SUBWAY\"", "\"RIDE_SHARING\""), 0));
        assertEquals(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());

        response.set(new Response(200, scheduledFixture().replace("\"precision\":6", "\"precision\":5"), 0));
        assertEquals(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());
    }

    @Test
    void mapsEmptyResultsAndProviderFailuresToStableJourneyOutcomes() {
        response.set(new Response(200, "{\"itineraries\":[]}", 0));
        assertEquals(JourneyFailureKind.NO_JOURNEY_FOUND, failure().kind());

        response.set(new Response(429, "{}", 0));
        assertEquals(JourneyFailureKind.RATE_LIMITED, failure().kind());

        response.set(new Response(504, "{}", 0));
        assertEquals(JourneyFailureKind.TIMEOUT, failure().kind());

        response.set(new Response(422, "{}", 0));
        assertEquals(JourneyFailureKind.INVALID_PROVIDER_RESPONSE, failure().kind());

        response.set(new Response(503, "{}", 0));
        assertEquals(JourneyFailureKind.PROVIDER_UNAVAILABLE, failure().kind());
    }

    @Test
    void mapsReadTimeoutAndBoundsProviderResponseConsumption() throws IOException {
        response.set(new Response(200, scheduledFixture(), 250));
        MotisJourneyAdapter shortTimeoutAdapter = adapter(Duration.ofMillis(25));
        assertEquals(JourneyFailureKind.TIMEOUT,
                assertThrows(JourneyProviderException.class,
                        () -> shortTimeoutAdapter.plan(request(JourneyTimeMode.DEPART_AT))).kind());

        CountingInputStream atLimit = new CountingInputStream(new byte[MotisJourneyAdapter.MAX_PROVIDER_RESPONSE_BYTES]);
        MotisJourneyAdapter.readResponseBody(responseWithBody(atLimit, -1));
        assertEquals(MotisJourneyAdapter.MAX_PROVIDER_RESPONSE_BYTES, atLimit.bytesRead);

        CountingInputStream overLimit = new CountingInputStream(
                new byte[MotisJourneyAdapter.MAX_PROVIDER_RESPONSE_BYTES + 2]);
        assertEquals(JourneyFailureKind.INVALID_PROVIDER_RESPONSE,
                assertThrows(JourneyProviderException.class,
                        () -> MotisJourneyAdapter.readResponseBody(responseWithBody(overLimit, -1))).kind());
        assertEquals(MotisJourneyAdapter.MAX_PROVIDER_RESPONSE_BYTES + 1, overLimit.bytesRead);
    }

    private JourneyRequest request(JourneyTimeMode mode) {
        return new JourneyRequest(new Coordinate(9.99, 53.55), new Coordinate(10.01, 53.57), mode, REQUEST_TIME);
    }

    private JourneyProviderException failure() {
        return assertThrows(JourneyProviderException.class,
                () -> adapter.plan(request(JourneyTimeMode.DEPART_AT)));
    }

    private MotisJourneyAdapter adapter(Duration readTimeout) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return new MotisJourneyAdapter(RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(factory)
                .defaultHeader("User-Agent", "orientation-test/1.0")
                .build(), new ObjectMapper());
    }

    private Map<String, String> query() {
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(rawQuery.get().split("&", -1)).forEach(part -> {
            String[] pair = part.split("=", 2);
            values.put(decode(pair[0]), pair.length == 1 ? "" : decode(pair[1]));
        });
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange) throws IOException {
        rawQuery.set(exchange.getRequestURI().getRawQuery());
        userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
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

    private String scheduledFixture() {
        return fixture(false);
    }

    private String realtimeFixture() {
        return fixture(true);
    }

    private String fixture(boolean realtime) {
        String transitStart = realtime ? "2026-08-17T22:10:00+02:00" : "2026-08-17T22:07:00+02:00";
        String transitEnd = realtime ? "2026-08-17T22:30:00+02:00" : "2026-08-17T22:27:00+02:00";
        return "{\"itineraries\":[{" +
                "\"duration\":1800,\"startTime\":\"2026-08-17T22:00:00+02:00\",\"endTime\":\"" + transitEnd + "\",\"transfers\":0," +
                "\"legs\":[" +
                "{\"mode\":\"WALK\",\"startTime\":\"2026-08-17T22:00:00+02:00\",\"endTime\":\"2026-08-17T22:05:00+02:00\","
                + "\"scheduledStartTime\":\"2026-08-17T22:00:00+02:00\",\"scheduledEndTime\":\"2026-08-17T22:05:00+02:00\","
                + "\"realTime\":false,\"scheduled\":true,\"duration\":300,\"from\":" + place("Origin", 53.55, 9.99)
                + ",\"to\":" + place("Station A", 53.555, 9.995) + ",\"intermediateStops\":null,\"legGeometry\":" + geometry() + "},"
                + "{\"mode\":\"SUBWAY\",\"startTime\":\"" + transitStart + "\",\"endTime\":\"" + transitEnd + "\","
                + "\"scheduledStartTime\":\"2026-08-17T22:07:00+02:00\",\"scheduledEndTime\":\"2026-08-17T22:27:00+02:00\","
                + "\"realTime\":" + realtime + ",\"scheduled\":true,\"duration\":1200,\"from\":" + place("Station A", 53.555, 9.995)
                + ",\"to\":" + place("Destination", 53.57, 10.01) + ",\"displayName\":\"U1\",\"routeShortName\":\"U1\","
                + "\"headsign\":\"Norderstedt Mitte\",\"intermediateStops\":[" + place("Middle", 53.56, 10.0) + "],\"legGeometry\":" + geometry() + "}]}]}";
    }

    private String manyItinerariesFixture(int count) {
        String itinerary = scheduledFixture().substring("{\"itineraries\":[".length(), scheduledFixture().length() - 2);
        return "{\"itineraries\":[" + String.join(",", java.util.Collections.nCopies(count, itinerary)) + "]}";
    }

    private String place(String name, double lat, double lon) {
        return "{\"name\":\"" + name + "\",\"id\":\"stub\",\"lat\":" + lat + ",\"lon\":" + lon + "}";
    }

    private String geometry() {
        return "{\"points\":\"" + SHAPE + "\",\"precision\":6,\"length\":1000}";
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
