package system.wgt.orientation.domain.journey;

import system.wgt.orientation.domain.place.Coordinate;

import java.time.OffsetDateTime;

public record JourneyRequest(
        Coordinate origin,
        Coordinate destination,
        JourneyTimeMode timeMode,
        OffsetDateTime time) {

    public JourneyRequest {
        if (origin == null) {
            throw new IllegalArgumentException("Journey origin is required.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Journey destination is required.");
        }
        if (timeMode == null) {
            throw new IllegalArgumentException("Journey time mode is required.");
        }
        if (time == null) {
            throw new IllegalArgumentException("Journey time is required.");
        }
    }
}
