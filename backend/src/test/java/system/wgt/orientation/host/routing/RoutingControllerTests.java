package system.wgt.orientation.host.routing;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import system.wgt.orientation.application.routing.RoutingFailureKind;
import system.wgt.orientation.application.routing.RoutingProviderException;
import system.wgt.orientation.application.routing.RoutingService;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteGeometry;
import system.wgt.orientation.domain.routing.TravelProfile;
import system.wgt.orientation.host.place.PlaceApiExceptionHandler;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RoutingControllerTests {
    private final Route route = new Route(new Coordinate(10, 50), new Coordinate(11, 51), TravelProfile.DRIVING,
            new RouteGeometry(List.of(new Coordinate(10, 50), new Coordinate(10.5, 50.5), new Coordinate(11, 51))), 1234.5, 98.25);

    @Test
    void returnsOnlyOrientationRouteFields() throws Exception {
        MockMvc mvc = mvc(request -> route);
        mvc.perform(post("/api/v1/routes").contentType("application/json").content("""
                        {"origin":{"longitude":10,"latitude":50},"destination":{"longitude":11,"latitude":51},"profile":"DRIVING"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"distanceMeters\":1234.5")))
                .andExpect(content().string(containsString("\"durationSeconds\":98.25")))
                .andExpect(content().string(containsString("\"geometry\":[{")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Valhalla"))));
    }

    @Test
    void rejectsInvalidCoordinatesProfilesAndMalformedRequests() throws Exception {
        MockMvc mvc = mvc(request -> route);
        mvc.perform(post("/api/v1/routes").contentType("application/json").content("""
                        {"origin":{"longitude":181,"latitude":50},"destination":{"longitude":11,"latitude":51},"profile":"DRIVING"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/routes").contentType("application/json").content("""
                        {"origin":{"longitude":10,"latitude":50},"destination":{"longitude":11,"latitude":51},"profile":"auto"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/routes").contentType("application/json").content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsNoRouteAndProviderFailuresWithoutInternals() throws Exception {
        for (RoutingFailureKind kind : RoutingFailureKind.values()) {
            MockMvc mvc = mvc(request -> { throw new RoutingProviderException(kind, "provider internals"); });
            var result = mvc.perform(post("/api/v1/routes").contentType("application/json").content("""
                            {"origin":{"longitude":10,"latitude":50},"destination":{"longitude":11,"latitude":51},"profile":"WALKING"}
                            """));
            int expected = switch (kind) {
                case NO_ROUTE_FOUND -> 404;
                case RATE_LIMITED -> 429;
                case INVALID_PROVIDER_RESPONSE -> 502;
                case TIMEOUT, PROVIDER_UNAVAILABLE -> 503;
            };
            result.andExpect(status().is(expected)).andExpect(content().string(org.hamcrest.Matchers.not(containsString("provider internals"))));
        }
    }

    private MockMvc mvc(system.wgt.orientation.application.routing.RoutingPort port) {
        return standaloneSetup(new RoutingController(new RoutingService(port)))
                .setControllerAdvice(new PlaceApiExceptionHandler()).build();
    }
}
