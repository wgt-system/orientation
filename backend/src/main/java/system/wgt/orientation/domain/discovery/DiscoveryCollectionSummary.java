package system.wgt.orientation.domain.discovery;

import java.time.OffsetDateTime;

public record DiscoveryCollectionSummary(
        String collectionId,
        OffsetDateTime researchedAt,
        String questionRef,
        String questionText,
        String centerLabel,
        int radiusMeters,
        int candidateCount) {
}
