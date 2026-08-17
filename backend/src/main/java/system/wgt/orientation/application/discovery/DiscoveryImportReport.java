package system.wgt.orientation.application.discovery;

import java.util.List;
import java.util.Optional;

public record DiscoveryImportReport(
        Status status,
        Optional<String> collectionId,
        int candidateCount,
        int sourceCount,
        List<String> errors) {

    public DiscoveryImportReport {
        collectionId = collectionId == null ? Optional.empty() : collectionId;
        errors = List.copyOf(errors);
    }

    public static DiscoveryImportReport rejected(List<String> errors) {
        return new DiscoveryImportReport(Status.REJECTED, Optional.empty(), 0, 0, errors);
    }

    public static DiscoveryImportReport stored(boolean created, String collectionId, int candidateCount, int sourceCount) {
        return new DiscoveryImportReport(created ? Status.CREATED : Status.UNCHANGED,
                Optional.of(collectionId), candidateCount, sourceCount, List.of());
    }

    public enum Status {
        CREATED,
        UNCHANGED,
        REJECTED
    }
}
