package system.wgt.orientation.domain.journey;

import java.time.OffsetDateTime;

public record JourneyEventTime(OffsetDateTime scheduledTime, OffsetDateTime realtimeTime) {

    public JourneyEventTime {
        if (scheduledTime == null) {
            throw new IllegalArgumentException("Scheduled journey time is required.");
        }
    }

    public boolean hasRealtimeUpdate() {
        return realtimeTime != null;
    }

    public OffsetDateTime effectiveTime() {
        return realtimeTime == null ? scheduledTime : realtimeTime;
    }
}
