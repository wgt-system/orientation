package system.wgt.orientation.application.routing;

import org.junit.jupiter.api.Test;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteGeometry;
import system.wgt.orientation.domain.routing.RouteRequest;
import system.wgt.orientation.domain.routing.TravelProfile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingServiceTests {
    private static final RouteRequest REQUEST = new RouteRequest(new Coordinate(10, 50), new Coordinate(11, 51), TravelProfile.DRIVING);
    private static final Route ROUTE = new Route(REQUEST.origin(), REQUEST.destination(), REQUEST.profile(),
            new RouteGeometry(List.of(REQUEST.origin(), REQUEST.destination())), 1000, 120);

    @Test
    void returnsTheProviderNeutralRouteFromThePort() {
        RoutingService service = new RoutingService(request -> ROUTE);
        assertEquals(ROUTE, service.route(REQUEST));
    }

    @Test
    void validatesRequestBeforeCallingThePort() {
        RoutingService service = new RoutingService(request -> { throw new AssertionError("port must not be called"); });
        assertThrows(IllegalArgumentException.class, () -> service.route(null));
    }

    @Test
    void preservesStableRoutingFailureKinds() {
        for (RoutingFailureKind kind : RoutingFailureKind.values()) {
            RoutingService service = new RoutingService(request -> { throw new RoutingProviderException(kind, "stub"); });
            RoutingProviderException exception = assertThrows(RoutingProviderException.class, () -> service.route(REQUEST));
            assertEquals(kind, exception.kind());
        }
    }
}
