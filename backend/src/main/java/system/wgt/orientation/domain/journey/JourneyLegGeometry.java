package system.wgt.orientation.domain.journey;

import system.wgt.orientation.domain.place.Coordinate;

import java.util.List;

public record JourneyLegGeometry(List<Coordinate> coordinates) {
    public static final int MAX_COORDINATES = 10_000;

    public JourneyLegGeometry {
        if (coordinates == null || coordinates.size() < 2) {
            throw new IllegalArgumentException("Journey leg geometry requires at least two coordinates.");
        }
        if (coordinates.size() > MAX_COORDINATES) {
            throw new IllegalArgumentException("Journey leg geometry exceeds the coordinate limit.");
        }
        if (coordinates.stream().anyMatch(coordinate -> coordinate == null)) {
            throw new IllegalArgumentException("Journey leg geometry coordinates are required.");
        }
        coordinates = List.copyOf(coordinates);
    }
}
