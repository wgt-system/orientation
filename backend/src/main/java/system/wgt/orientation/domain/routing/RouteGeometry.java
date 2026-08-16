package system.wgt.orientation.domain.routing;

import system.wgt.orientation.domain.place.Coordinate;

import java.util.List;

public record RouteGeometry(List<Coordinate> coordinates) {
    /** Initial defensive bound for one decoded route geometry. */
    public static final int MAX_COORDINATES = 10_000;

    public RouteGeometry {
        if (coordinates == null || coordinates.size() < 2) {
            throw new IllegalArgumentException("Route geometry requires at least two coordinates.");
        }
        if (coordinates.size() > MAX_COORDINATES) {
            throw new IllegalArgumentException("Route geometry exceeds the coordinate limit.");
        }
        if (coordinates.stream().anyMatch(coordinate -> coordinate == null)) {
            throw new IllegalArgumentException("Route geometry coordinates are required.");
        }
        coordinates = List.copyOf(coordinates);
    }
}
