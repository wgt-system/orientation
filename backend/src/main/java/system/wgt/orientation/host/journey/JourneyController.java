package system.wgt.orientation.host.journey;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.wgt.orientation.application.journey.JourneyPort;
import system.wgt.orientation.application.journey.JourneyService;
import system.wgt.orientation.domain.journey.Journey;
import system.wgt.orientation.domain.journey.JourneyEventTime;
import system.wgt.orientation.domain.journey.JourneyLeg;
import system.wgt.orientation.domain.journey.JourneyPlan;
import system.wgt.orientation.domain.journey.JourneyRequest;
import system.wgt.orientation.domain.journey.JourneyStop;
import system.wgt.orientation.domain.journey.JourneyTimeMode;
import system.wgt.orientation.domain.journey.TransitService;
import system.wgt.orientation.domain.place.Coordinate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/journeys")
public class JourneyController {
    private final JourneyService journeyService;

    public JourneyController(JourneyPort port) {
        this.journeyService = new JourneyService(port);
    }

    @PostMapping
    public JourneyPlanResponse plan(@RequestBody JourneyRequestDto request) {
        if (request == null || request.origin() == null || request.destination() == null) {
            throw new IllegalArgumentException("Journey origin and destination are required.");
        }
        JourneyRequest journeyRequest = new JourneyRequest(
                request.origin().toCoordinate(),
                request.destination().toCoordinate(),
                parseTimeMode(request.timeMode()),
                parseTime(request.time()));
        return JourneyPlanResponse.from(journeyService.plan(journeyRequest));
    }

    private JourneyTimeMode parseTimeMode(String timeMode) {
        if (timeMode == null || timeMode.isBlank()) {
            throw new IllegalArgumentException("Journey time mode is required.");
        }
        try {
            return JourneyTimeMode.valueOf(timeMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Journey time mode is invalid.");
        }
    }

    private OffsetDateTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            throw new IllegalArgumentException("Journey time is required.");
        }
        try {
            return OffsetDateTime.parse(time.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Journey time must be an offset-aware ISO-8601 timestamp.");
        }
    }

    public record JourneyRequestDto(CoordinateDto origin, CoordinateDto destination, String timeMode, String time) {
    }

    public record CoordinateDto(double longitude, double latitude) {
        static CoordinateDto from(Coordinate coordinate) {
            return new CoordinateDto(coordinate.longitude(), coordinate.latitude());
        }

        Coordinate toCoordinate() {
            return new Coordinate(longitude, latitude);
        }
    }

    public record JourneyPlanResponse(List<JourneyDto> journeys) {
        static JourneyPlanResponse from(JourneyPlan plan) {
            return new JourneyPlanResponse(plan.journeys().stream().map(JourneyDto::from).toList());
        }
    }

    public record JourneyDto(
            OffsetDateTime departureTime,
            OffsetDateTime arrivalTime,
            long durationSeconds,
            int transfers,
            List<JourneyLegDto> legs) {
        static JourneyDto from(Journey journey) {
            return new JourneyDto(journey.departureTime(), journey.arrivalTime(), journey.durationSeconds,
                    journey.transfers(), journey.legs().stream().map(JourneyLegDto::from).toList());
        }
    }

    public record JourneyLegDto(
            system.wgt.orientation.domain.journey.JourneyLegMode mode,
            JourneyStopDto origin,
            JourneyStopDto destination,
            JourneyEventTimeDto departure,
            JourneyEventTimeDto arrival,
            long durationSeconds,
            TransitServiceDto transitService,
            List<CoordinateDto> geometry,
            List<JourneyStopDto> intermediateStops) {
        static JourneyLegDto from(JourneyLeg leg) {
            return new JourneyLegDto(
                    leg.mode(),
                    JourneyStopDto.from(leg.origin()),
                    JourneyStopDto.from(leg.destination()),
                    JourneyEventTimeDto.from(leg.departure()),
                    JourneyEventTimeDto.from(leg.arrival()),
                    leg.durationSeconds(),
                    TransitServiceDto.from(leg.transitService()),
                    leg.geometry() == null ? List.of() : leg.geometry().coordinates().stream().map(CoordinateDto::from).toList(),
                    leg.intermediateStops().stream().map(JourneyStopDto::from).toList());
        }
    }

    public record JourneyStopDto(String name, CoordinateDto coordinate) {
        static JourneyStopDto from(JourneyStop stop) {
            return new JourneyStopDto(stop.name(), CoordinateDto.from(stop.coordinate()));
        }
    }

    public record JourneyEventTimeDto(OffsetDateTime scheduledTime, OffsetDateTime realtimeTime) {
        static JourneyEventTimeDto from(JourneyEventTime time) {
            return new JourneyEventTimeDto(time.scheduledTime(), time.realtimeTime());
        }
    }

    public record TransitServiceDto(String label, String headsign) {
        static TransitServiceDto from(TransitService service) {
            return service == null ? null : new TransitServiceDto(service.label(), service.headsign());
        }
    }
}
