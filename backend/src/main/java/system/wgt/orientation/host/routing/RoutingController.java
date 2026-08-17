package system.wgt.orientation.host.routing;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.wgt.orientation.application.routing.RoutingService;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteRequest;
import system.wgt.orientation.domain.routing.TravelProfile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RoutingController {
    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping
    public RouteResponse route(@RequestBody RouteRequestDto request) {
        if (request == null || request.origin() == null || request.destination() == null) {
            throw new IllegalArgumentException("Origin and destination are required.");
        }
        TravelProfile profile = parseProfile(request.profile());
        Route route = routingService.route(new RouteRequest(request.origin().toCoordinate(),
                request.destination().toCoordinate(), profile));
        return RouteResponse.from(route);
    }

    private TravelProfile parseProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            throw new IllegalArgumentException("Travel profile is required.");
        }
        try {
            return TravelProfile.valueOf(profile.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Travel profile is invalid.");
        }
    }

    public record RouteRequestDto(CoordinateDto origin, CoordinateDto destination, String profile) {
    }

    public record CoordinateDto(double longitude, double latitude) {
        static CoordinateDto from(Coordinate coordinate) {
            return new CoordinateDto(coordinate.longitude(), coordinate.latitude());
        }

        Coordinate toCoordinate() {
            return new Coordinate(longitude, latitude);
        }
    }

    public record RouteResponse(RouteDto route) {
        static RouteResponse from(Route route) {
            return new RouteResponse(RouteDto.from(route));
        }
    }

    public record RouteDto(CoordinateDto origin, CoordinateDto destination, TravelProfile profile,
                           double distanceMeters, double durationSeconds, List<CoordinateDto> geometry) {
        static RouteDto from(Route route) {
            return new RouteDto(CoordinateDto.from(route.origin()), CoordinateDto.from(route.destination()),
                    route.profile(), route.distanceMeters(), route.durationSeconds(),
                    route.geometry().coordinates().stream().map(CoordinateDto::from).toList());
        }
    }

}
