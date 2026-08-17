package system.wgt.orientation.domain.routing;

import system.wgt.orientation.domain.place.Coordinate;

public record Route(
        Coordinate origin,
        Coordinate destination,
        TravelProfile profile,
        RouteGeometry geometry,
        double distanceMeters,
        double durationSeconds) {

    public Route {
        if (origin == null || destination == null || profile == null || geometry == null) {
            throw new IllegalArgumentException("Route origin, destination, profile and geometry are required.");
        }
        if (!Double.isFinite(distanceMeters) || distanceMeters < 0) {
            throw new IllegalArgumentException("Route distance must be finite and non-negative.");
        }
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0) {
            throw new IllegalArgumentException("Route duration must be finite and non-negative.");
        }
    }
}
