package system.wgt.orientation.application.routing;

public class RoutingProviderException extends RuntimeException {
    private final RoutingFailureKind kind;

    public RoutingProviderException(RoutingFailureKind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public RoutingProviderException(RoutingFailureKind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public RoutingFailureKind kind() {
        return kind;
    }
}
