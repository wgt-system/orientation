package system.wgt.orientation.domain.journey;

import system.wgt.orientation.domain.place.Coordinate;

public record JourneyStop(String name, Coordinate coordinate) {
    public static final int MAX_NAME_LENGTH = 200;

    public JourneyStop {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Journey stop name is required.");
        }
        name = name.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Journey stop name exceeds the length limit.");
        }
        if (coordinate == null) {
            throw new IllegalArgumentException("Journey stop coordinate is required.");
        }
    }
}
