package system.wgt.orientation.application.journey;

import system.wgt.orientation.domain.journey.JourneyPlan;
import system.wgt.orientation.domain.journey.JourneyRequest;

public interface JourneyPort {
    JourneyPlan plan(JourneyRequest request);
}
