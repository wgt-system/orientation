package system.wgt.orientation.domain.journey;

import org.junit.jupiter.api.Test;
import system.wgt.orientation.domain.place.Coordinate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JourneyDomainTests {
    private static final Coordinate A = new Coordinate(10.0, 53.5);
    private static final Coordinate B = new Coordinate(10.1, 53.55);
    private static final Coordinate C = new Coordinate(10.2, 53.6);
    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-08-17T22:00:00+02:00");

    @Test
    void journeyRequestRequiresCoordinatesTimeModeAndOffsetAwareTime() {
        assertEquals(2, JourneyTimeMode.values().length);
        assertThrows(IllegalArgumentException.class, () -> new JourneyRequest(null, B, JourneyTimeMode.DEPART_AT, T0));
        assertThrows(IllegalArgumentException.class, () -> new JourneyRequest(A, null, JourneyTimeMode.DEPART_AT, T0));
        assertThrows(IllegalArgumentException.class, () -> new JourneyRequest(A, B, null, T0));
        assertThrows(IllegalArgumentException.class, () -> new JourneyRequest(A, B, JourneyTimeMode.ARRIVE_BY, null));
    }

    @Test
    void scheduledAndRealtimeTimesRemainDistinct() {
        JourneyEventTime scheduled = new JourneyEventTime(T0, null);
        JourneyEventTime updated = new JourneyEventTime(T0, T0.plusMinutes(4));

        assertFalse(scheduled.hasRealtimeUpdate());
        assertEquals(T0, scheduled.effectiveTime());
        assertTrue(updated.hasRealtimeUpdate());
        assertEquals(T0.plusMinutes(4), updated.effectiveTime());
    }

    @Test
    void journeyLegsAndAlternativesAreBoundedCopySafeAndProviderNeutral() {
        JourneyStop a = new JourneyStop("A", A);
        JourneyStop b = new JourneyStop("B", B);
        JourneyStop c = new JourneyStop("C", C);
        JourneyLeg walk = new JourneyLeg(JourneyLegMode.WALK, a, b,
                new JourneyEventTime(T0, null), new JourneyEventTime(T0.plusMinutes(5), null),
                null, new JourneyLegGeometry(List.of(A, B)), List.of());
        JourneyLeg transit = new JourneyLeg(JourneyLegMode.SUBWAY, b, c,
                new JourneyEventTime(T0.plusMinutes(7), null), new JourneyEventTime(T0.plusMinutes(20), T0.plusMinutes(22)),
                new TransitService("U1", "Downtown"), new JourneyLegGeometry(List.of(B, C)), List.of());

        List<JourneyLeg> source = new ArrayList<>(List.of(walk, transit));
        Journey journey = new Journey(source, 0);
        source.clear();

        assertEquals(2, journey.legs().size());
        assertEquals(T0, journey.departureTime());
        assertEquals(T0.plusMinutes(22), journey.arrivalTime());
        assertThrows(UnsupportedOperationException.class, () -> journey.legs().add(walk));
        assertThrows(IllegalArgumentException.class, () -> new JourneyLeg(JourneyLegMode.WALK, a, b,
                new JourneyEventTime(T0, null), new JourneyEventTime(T0.plusMinutes(1), null),
                new TransitService("X", null), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Journey(List.of(walk), 0));
        assertThrows(IllegalArgumentException.class, () -> new JourneyLegGeometry(Stream.generate(() -> A)
                .limit(JourneyLegGeometry.MAX_COORDINATES + 1L).toList()));

        JourneyPlan plan = new JourneyPlan(List.of(journey));
        assertEquals(1, plan.journeys().size());
        assertThrows(IllegalArgumentException.class, () -> new JourneyPlan(Stream.generate(() -> journey)
                .limit(JourneyPlan.MAX_JOURNEYS + 1L).toList()));
    }

    @Test
    void journeyRejectsChronologicalAndTransferInconsistency() {
        JourneyStop a = new JourneyStop("A", A);
        JourneyStop b = new JourneyStop("B", B);
        JourneyStop c = new JourneyStop("C", C);
        JourneyLeg first = transit(a, b, T0, T0.plusMinutes(10));
        JourneyLeg overlapping = transit(b, c, T0.plusMinutes(9), T0.plusMinutes(20));

        assertThrows(IllegalArgumentException.class, () -> new Journey(List.of(first, overlapping), 2));
        assertThrows(IllegalArgumentException.class, () -> new Journey(List.of(first, overlapping), 1));
    }

    private JourneyLeg transit(JourneyStop origin, JourneyStop destination, OffsetDateTime departure, OffsetDateTime arrival) {
        return new JourneyLeg(JourneyLegMode.BUS, origin, destination,
                new JourneyEventTime(departure, null), new JourneyEventTime(arrival, null),
                new TransitService("5", null), null, List.of());
    }
}
