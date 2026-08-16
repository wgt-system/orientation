package system.wgt.orientation.application.routing;

import org.springframework.stereotype.Service;
import system.wgt.orientation.domain.routing.Route;
import system.wgt.orientation.domain.routing.RouteRequest;

@Service
public class RoutingService {
    private final RoutingPort port;

    public RoutingService(RoutingPort port) {
        if (port == null) {
            throw new IllegalArgumentException("Routing port is required.");
        }
        this.port = port;
    }

    public Route route(RouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Route request is required.");
        }
        return port.route(request);
    }
}
