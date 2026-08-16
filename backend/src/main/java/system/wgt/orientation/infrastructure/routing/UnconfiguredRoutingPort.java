package system.wgt.orientation.infrastructure.routing;

import org.springframework.stereotype.Component;
import system.wgt.orientation.application.routing.RoutingFailureKind;
import system.wgt.orientation.application.routing.RoutingPort;
import system.wgt.orientation.application.routing.RoutingProviderException;
import system.wgt.orientation.domain.routing.RouteRequest;

/**
 * Keeps the HTTP boundary explicit before the Valhalla adapter is introduced
 * in issue #10. It performs no network request.
 */
@Component
public class UnconfiguredRoutingPort implements RoutingPort {
    @Override
    public system.wgt.orientation.domain.routing.Route route(RouteRequest request) {
        throw new RoutingProviderException(RoutingFailureKind.PROVIDER_UNAVAILABLE,
                "No routing provider is configured.");
    }
}
