package system.wgt.orientation.domain.journey;

public record TransitService(String label, String headsign) {
    public static final int MAX_LABEL_LENGTH = 100;
    public static final int MAX_HEADSIGN_LENGTH = 200;

    public TransitService {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Transit service label is required.");
        }
        label = label.trim();
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("Transit service label exceeds the length limit.");
        }
        if (headsign != null) {
            headsign = headsign.trim();
            if (headsign.isEmpty()) {
                headsign = null;
            } else if (headsign.length() > MAX_HEADSIGN_LENGTH) {
                throw new IllegalArgumentException("Transit service headsign exceeds the length limit.");
            }
        }
    }
}
