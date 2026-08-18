package system.wgt.orientation.application.journey;

import system.wgt.orientation.domain.journey.JourneyPlan;
import system.wgt.orientation.domain.journey.JourneyRequest;

public class JourneyService {
    private final JourneyPort port;

    public JourneyService(JourneyPort port) {
        if (port == null) {
            throw new IllegalArgumentException("Journey port is required.");
        }
        this.port = port;
    }

    public JourneyPlan plan(JourneyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Journey request is required.");
        }
        return port.plan(request);
    }
}
