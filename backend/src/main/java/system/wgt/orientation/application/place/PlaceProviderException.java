package system.wgt.orientation.application.place;

public class PlaceProviderException extends RuntimeException {
    private final ProviderFailureKind kind;

    public PlaceProviderException(ProviderFailureKind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public PlaceProviderException(ProviderFailureKind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public ProviderFailureKind kind() {
        return kind;
    }
}
