package system.wgt.orientation.application.routing;

import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteRequest;

public interface RoutingPort {
    Route route(RouteRequest request);
}
