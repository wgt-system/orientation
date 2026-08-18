package system.wgt.orientation.application.journey;

public class JourneyProviderException extends RuntimeException {
    private final JourneyFailureKind kind;

    public JourneyProviderException(JourneyFailureKind kind, String message) {
        super(message);
        if (kind == null) {
            throw new IllegalArgumentException("Journey failure kind is required.");
        }
        this.kind = kind;
    }

    public JourneyProviderException(JourneyFailureKind kind, String message, Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Journey failure kind is required.");
        }
        this.kind = kind;
    }

    public JourneyFailureKind kind() {
        return kind;
    }
}
