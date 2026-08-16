package system.wgt.orientation.application.routing;

public enum RoutingFailureKind {
    PROVIDER_UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    INVALID_PROVIDER_RESPONSE,
    NO_ROUTE_FOUND
}
