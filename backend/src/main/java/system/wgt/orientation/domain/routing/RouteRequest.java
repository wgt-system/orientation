package system.wgt.orientation.domain.routing;

import system.wgt.orientation.domain.place.Coordinate;

public record RouteRequest(Coordinate origin, Coordinate destination, TravelProfile profile) {

    public RouteRequest {
        if (origin == null) {
            throw new IllegalArgumentException("Origin is required.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination is required.");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Travel profile is required.");
        }
    }
}
