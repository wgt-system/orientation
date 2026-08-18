package system.wgt.orientation.domain.journey;

import java.util.List;

public record JourneyPlan(List<Journey> journeys) {
    public static final int MAX_JOURNEYS = 8;

    public JourneyPlan {
        if (journeys == null || journeys.isEmpty()) {
            throw new IllegalArgumentException("Journey plan requires at least one journey.");
        }
        if (journeys.size() > MAX_JOURNEYS) {
            throw new IllegalArgumentException("Journey plan exceeds the alternative limit.");
        }
        if (journeys.stream().anyMatch(journey -> journey == null)) {
            throw new IllegalArgumentException("Journey alternatives are required.");
        }
        journeys = List.copyOf(journeys);
    }
}
