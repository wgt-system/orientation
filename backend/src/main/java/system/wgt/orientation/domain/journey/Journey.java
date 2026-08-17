package system.wgt.orientation.domain.journey;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

public record Journey(List<JourneyLeg> legs, int transfers) {
    public static final int MAX_LEGS = 64;

    public Journey {
        if (legs == null || legs.isEmpty()) {
            throw new IllegalArgumentException("Journey requires at least one leg.");
        }
        if (legs.size() > MAX_LEGS) {
            throw new IllegalArgumentException("Journey exceeds the leg limit.");
        }
        if (legs.stream().anyMatch(leg -> leg == null)) {
            throw new IllegalArgumentException("Journey legs are required.");
        }
        long transitLegs = legs.stream().filter(leg -> leg.mode().isTransit()).count();
        if (transitLegs == 0) {
            throw new IllegalArgumentException("Public-transit Journey requires at least one transit leg.");
        }
        if (transfers < 0 || transfers > transitLegs - 1) {
            throw new IllegalArgumentException("Journey transfer count is invalid.");
        }
        for (int index = 1; index < legs.size(); index++) {
            JourneyLeg previous = legs.get(index - 1);
            JourneyLeg current = legs.get(index);
            if (current.departure().effectiveTime().isBefore(previous.arrival().effectiveTime())) {
                throw new IllegalArgumentException("Journey legs must be chronologically ordered.");
            }
        }
        legs = List.copyOf(legs);
    }

    public OffsetDateTime departureTime() {
        return legs.getFirst().departure().effectiveTime();
    }

    public OffsetDateTime arrivalTime() {
        return legs.getLast().arrival().effectiveTime();
    }

    public long durationSeconds() {
        return Duration.between(departureTime(), arrivalTime()).toSeconds();
    }
}
