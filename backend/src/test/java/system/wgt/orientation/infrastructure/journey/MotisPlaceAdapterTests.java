package system.wgt.orientation.infrastructure.journey;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotisPlaceAdapterTests {
    private HttpServer server;
    private AtomicReference<Response> response;
    private AtomicReference<java.util.Map<String, String>> lastQuery;
    private MotisPlaceAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        response = new AtomicReference<>(new Response(200, fixture()));
        lastQuery = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::respond);
        server.start();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(1));
        adapter = new MotisPlaceAdapter(RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(factory)
                .build(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsSearchThroughMotisGeocode() {
        var query = new PlaceSearchQuery("Hamburg Hauptbahnhof", 5, Optional.of("de"),
                Optional.of(new PlaceSearchQuery.LocationBias(new Coordinate(10.0, 53.55), 12)));

        var places = adapter.search(query);

        assertEquals(1, places.size());
        assertEquals("motis:de:stop:123", places.get(0).providerReference());
        assertEquals("Hamburg Hauptbahnhof", places.get(0).displayLabel());
        assertEquals(new Coordinate(10.0067, 53.5526), places.get(0).coordinate());
        assertEquals(Optional.of("STOP"), places.get(0).kind());
        assertEquals("Hamburg Hauptbahnhof", lastQuery.get().get("text"));
        assertEquals("5", lastQuery.get().get("numResults"));
        assertEquals("de", lastQuery.get().get("language"));
        assertEquals("53.55,10.0", lastQuery.get().get("place"));
    }

    @Test
    void mapsReverseThroughMotisWithoutInventingMissingAddressFields() {
        var place = adapter.reverse(new ReverseGeocodeQuery(new Coordinate(10.0067, 53.5526), Optional.of("de")));

        assertEquals("motis:de:stop:123", place.orElseThrow().providerReference());
        assertEquals("53.5526,10.0067", lastQuery.get().get("place"));
        assertEquals("1", lastQuery.get().get("numResults"));
        assertEquals(Optional.empty(), place.orElseThrow().address().city());
        assertEquals(Optional.of("DE"), place.orElseThrow().address().countryCode());
    }

    @Test
    void acceptsNoMatchesAndMapsProviderFailures() {
        response.set(new Response(200, "[]"));
        assertEquals(List.of(), adapter.search(new PlaceSearchQuery("unknown", 5, Optional.empty(), Optional.empty())));

        response.set(new Response(503, "{}"));
        assertEquals(ProviderFailureKind.UNAVAILABLE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    @Test
    void rejectsMalformedMotisResponses() {
        response.set(new Response(200, "[{\"id\":\"x\",\"name\":\"Bad\",\"lat\":95,\"lon\":10}]"));
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());

        response.set(new Response(200, "{}"));
        assertEquals(ProviderFailureKind.INVALID_RESPONSE, assertThrows(PlaceProviderException.class,
                () -> adapter.search(new PlaceSearchQuery("x", 5, Optional.empty(), Optional.empty()))).kind());
    }

    private void respond(HttpExchange exchange) throws IOException {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        lastQuery.set(rawQuery == null || rawQuery.isBlank() ? java.util.Map.of() : Arrays.stream(rawQuery.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> java.net.URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8))));
        Response current = response.get();
        byte[] bytes = current.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(current.status(), bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String fixture() {
        return "[{\"type\":\"STOP\",\"name\":\"Hamburg Hauptbahnhof\",\"id\":\"de:stop:123\","
                + "\"lat\":53.5526,\"lon\":10.0067,\"street\":\"Hachmannplatz\",\"houseNumber\":\"16\","
                + "\"zip\":\"20099\",\"country\":\"DE\",\"tokens\":[],\"areas\":[],\"score\":1.0}]";
    }

    private record Response(int status, String body) {
    }
}
