package system.wgt.orientation.domain.journey;

import java.util.List;

public record JourneyLeg(
        JourneyLegMode mode,
        JourneyStop origin,
        JourneyStop destination,
        JourneyEventTime departure,
        JourneyEventTime arrival,
        TransitService transitService,
        JourneyLegGeometry geometry,
        List<JourneyStop> intermediateStops) {
    public static final int MAX_INTERMEDIATE_STOPS = 128;

    public JourneyLeg {
        if (mode == null || origin == null || destination == null || departure == null || arrival == null) {
            throw new IllegalArgumentException("Journey leg mode, stops and times are required.");
        }
        if (arrival.effectiveTime().isBefore(departure.effectiveTime())) {
            throw new IllegalArgumentException("Journey leg arrival cannot be before departure.");
        }
        if (mode.isTransit() && transitService == null) {
            throw new IllegalArgumentException("Transit journey legs require service information.");
        }
        if (!mode.isTransit() && transitService != null) {
            throw new IllegalArgumentException("Walking journey legs cannot contain transit service information.");
        }
        if (intermediateStops == null) {
            intermediateStops = List.of();
        }
        if (intermediateStops.size() > MAX_INTERMEDIATE_STOPS) {
            throw new IllegalArgumentException("Journey leg exceeds the intermediate-stop limit.");
        }
        if (intermediateStops.stream().anyMatch(stop -> stop == null)) {
            throw new IllegalArgumentException("Journey intermediate stops are required when present.");
        }
        if (!mode.isTransit() && !intermediateStops.isEmpty()) {
            throw new IllegalArgumentException("Walking journey legs cannot contain intermediate transit stops.");
        }
        intermediateStops = List.copyOf(intermediateStops);
    }

    public long durationSeconds() {
        return java.time.Duration.between(departure.effectiveTime(), arrival.effectiveTime()).toSeconds();
    }
}
