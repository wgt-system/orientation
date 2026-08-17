package system.wgt.orientation.host.journey;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import system.wgt.orientation.application.journey.JourneyFailureKind;
import system.wgt.orientation.application.journey.JourneyPort;
import system.wgt.orientation.application.journey.JourneyProviderException;
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
import system.wgt.orientation.host.place.PlaceApiExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class JourneyControllerTests {
    private static final OffsetDateTime TIME = OffsetDateTime.parse("2026-08-17T22:00:00+02:00");
    private static final Coordinate ORIGIN = new Coordinate(10.0, 53.5);
    private static final Coordinate DESTINATION = new Coordinate(10.2, 53.6);

    @Test
    void acceptsExplicitDepartAtAndArriveByIntent() throws Exception {
        AtomicReference<JourneyRequest> captured = new AtomicReference<>();
        MockMvc mvc = mvc(request -> {
            captured.set(request);
            return plan();
        });

        mvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("DEPART_AT", "2026-08-17T22:00:00+02:00")))
                .andExpect(status().isOk());
        assertEquals(JourneyTimeMode.DEPART_AT, captured.get().timeMode());
        assertEquals(TIME, captured.get().time());

        mvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("ARRIVE_BY", "2026-08-17T23:00:00+02:00")))
                .andExpect(status().isOk());
        assertEquals(JourneyTimeMode.ARRIVE_BY, captured.get().timeMode());
        assertEquals(OffsetDateTime.parse("2026-08-17T23:00:00+02:00"), captured.get().time());
    }

    @Test
    void returnsOnlyProviderNeutralJourneyFields() throws Exception {
        MockMvc mvc = mvc(request -> plan());
        mvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("DEPART_AT", "2026-08-17T22:00:00+02:00")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"mode\":\"SUBWAY\"")))
                .andExpect(content().string(containsString("\"label\":\"U1\"")))
                .andExpect(content().string(containsString("\"scheduledTime\":")))
                .andExpect(content().string(containsString("\"realtimeTime\":")))
                .andExpect(content().string(not(containsString("MOTIS"))))
                .andExpect(content().string(not(containsString("Valhalla"))));
    }

    @Test
    void rejectsMalformedIntentAndMapsStableJourneyFailures() throws Exception {
        MockMvc validMvc = mvc(request -> plan());
        validMvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("TRANSIT", "2026-08-17T22:00:00+02:00")))
                .andExpect(status().isBadRequest());
        validMvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("DEPART_AT", "2026-08-17T22:00:00")))
                .andExpect(status().isBadRequest());
        validMvc.perform(post("/api/v1/journeys").contentType("application/json").content("not-json"))
                .andExpect(status().isBadRequest());

        for (JourneyFailureKind kind : JourneyFailureKind.values()) {
            MockMvc mvc = mvc(request -> { throw new JourneyProviderException(kind, "provider internals"); });
            int expected = switch (kind) {
                case NO_JOURNEY_FOUND -> 404;
                case RATE_LIMITED -> 429;
                case INVALID_PROVIDER_RESPONSE -> 502;
                case TIMEOUT, PROVIDER_UNAVAILABLE -> 503;
            };
            mvc.perform(post("/api/v1/journeys").contentType("application/json").content(requestJson("DEPART_AT", "2026-08-17T22:00:00+02:00")))
                    .andExpect(status().is(expected))
                    .andExpect(content().string(not(containsString("provider internals"))));
        }
    }

    private MockMvc mvc(JourneyPort port) {
        return standaloneSetup(new JourneyController(port))
                .setControllerAdvice(new PlaceApiExceptionHandler()).build();
    }

    private JourneyPlan plan() {
        JourneyStop origin = new JourneyStop("Origin", ORIGIN);
        JourneyStop destination = new JourneyStop("Destination", DESTINATION);
        JourneyLeg leg = new JourneyLeg(
                JourneyLegMode.SUBWAY,
                origin,
                destination,
                new JourneyEventTime(TIME, null),
                new JourneyEventTime(TIME.plusMinutes(20), TIME.plusMinutes(22)),
                new TransitService("U1", "Destination"),
                null,
                List.of());
        return new JourneyPlan(List.of(new Journey(List.of(leg), 0)));
    }

    private String requestJson(String timeMode, String time) {
        return """
                {"origin":{"longitude":10.0,"latitude":53.5},"destination":{"longitude":10.2,"latitude":53.6},"timeMode":"%s","time":"%s"}
                """.formatted(timeMode, time);
    }
}
