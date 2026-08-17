package system.wgt.orientation.domain.routing;

import org.junit.jupiter.api.Test;
import system.wgt.orientation.domain.place.Coordinate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingDomainTests {
    private static final Coordinate ORIGIN = new Coordinate(10, 50);
    private static final Coordinate DESTINATION = new Coordinate(11, 51);

    @Test
    void acceptsTheThreeGenericTravelProfiles() {
        assertEquals(3, TravelProfile.values().length);
        assertEquals(TravelProfile.DRIVING, TravelProfile.valueOf("DRIVING"));
        assertEquals(TravelProfile.CYCLING, TravelProfile.valueOf("CYCLING"));
        assertEquals(TravelProfile.WALKING, TravelProfile.valueOf("WALKING"));
    }

    @Test
    void routeRequestRequiresCoordinatesAndProfile() {
        assertThrows(IllegalArgumentException.class, () -> new RouteRequest(null, DESTINATION, TravelProfile.DRIVING));
        assertThrows(IllegalArgumentException.class, () -> new RouteRequest(ORIGIN, null, TravelProfile.DRIVING));
        assertThrows(IllegalArgumentException.class, () -> new RouteRequest(ORIGIN, DESTINATION, null));
        assertThrows(IllegalArgumentException.class, () -> new RouteRequest(new Coordinate(181, 50), DESTINATION, TravelProfile.DRIVING));
    }

    @Test
    void routeGeometryIsBoundedAndCopySafe() {
        List<Coordinate> source = new ArrayList<>(List.of(ORIGIN, DESTINATION));
        RouteGeometry geometry = new RouteGeometry(source);
        source.clear();
        assertEquals(2, geometry.coordinates().size());
        assertThrows(UnsupportedOperationException.class, () -> geometry.coordinates().add(ORIGIN));
        assertThrows(IllegalArgumentException.class, () -> new RouteGeometry(List.of(ORIGIN)));
        assertThrows(IllegalArgumentException.class, () -> new RouteGeometry(java.util.stream.Stream
                .generate(() -> ORIGIN).limit(RouteGeometry.MAX_COORDINATES + 1).toList()));
        assertThrows(IllegalArgumentException.class, () -> new RouteGeometry(java.util.Arrays.asList(ORIGIN, null)));
    }

    @Test
    void routeRequiresFiniteNonNegativeMeasures() {
        RouteGeometry geometry = new RouteGeometry(List.of(ORIGIN, DESTINATION));
        assertThrows(IllegalArgumentException.class, () -> new Route(ORIGIN, DESTINATION, TravelProfile.DRIVING, geometry, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Route(ORIGIN, DESTINATION, TravelProfile.DRIVING, geometry, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new Route(ORIGIN, DESTINATION, TravelProfile.DRIVING, geometry, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new Route(ORIGIN, DESTINATION, TravelProfile.DRIVING, geometry, 1, Double.POSITIVE_INFINITY));
    }
}
