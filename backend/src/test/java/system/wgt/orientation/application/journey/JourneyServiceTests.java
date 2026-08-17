package system.wgt.orientation.application.journey;

import org.junit.jupiter.api.Test;
import system.wgt.orientation.domain.journey.Journey;
import system.wgt.orientation.domain.journey.JourneyEventTime;
import system.wgt.orientation.domain.journey.JourneyLeg;
import system.wgt.orientation.domain.journey.JourneyLegMode;
import system.wgt.orientation.domain.journey.JourneyPlan;
import system.wgt.orientation.domain.journey.JourneyRequest;
import system.wgt.orientation.domain.journey.JourneyStop;
import system.wgt.orientation.domain.journey.JourneyTimeMode;
import system.wgt.orientation.domain.journey.TransitService;
import system.wgt.orientation.domain.place.Coordinate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JourneyServiceTests {
    private static final OffsetDateTime TIME = OffsetDateTime.parse("2026-08-17T22:00:00+02:00");
    private static final JourneyRequest REQUEST = new JourneyRequest(
            new Coordinate(10.0, 53.5), new Coordinate(10.2, 53.6), JourneyTimeMode.DEPART_AT, TIME);
    private static final JourneyPlan PLAN = new JourneyPlan(List.of(new Journey(List.of(new JourneyLeg(
            JourneyLegMode.BUS,
            new JourneyStop("Origin", REQUEST.origin()),
            new JourneyStop("Destination", REQUEST.destination()),
            new JourneyEventTime(TIME, null),
            new JourneyEventTime(TIME.plusMinutes(20), null),
            new TransitService("5", "Destination"),
            null,
            List.of())), 0)));

    @Test
    void returnsProviderNeutralJourneyPlanFromPort() {
        JourneyService service = new JourneyService(request -> PLAN);
        assertEquals(PLAN, service.plan(REQUEST));
    }

    @Test
    void validatesRequestBeforeCallingPort() {
        JourneyService service = new JourneyService(request -> { throw new AssertionError("port must not be called"); });
        assertThrows(IllegalArgumentException.class, () -> service.plan(null));
    }

    @Test
    void preservesStableJourneyFailureKinds() {
        for (JourneyFailureKind kind : JourneyFailureKind.values()) {
            JourneyService service = new JourneyService(request -> { throw new JourneyProviderException(kind, "stub"); });
            JourneyProviderException exception = assertThrows(JourneyProviderException.class, () -> service.plan(REQUEST));
            assertEquals(kind, exception.kind());
        }
    }
}
