package system.wgt.orientation.domain.discovery;

import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.research.ResearchEvaluationMode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DiscoveryCollection(
        String collectionId,
        String importFingerprint,
        OffsetDateTime researchedAt,
        String questionRef,
        String questionText,
        String centerLabel,
        Optional<Coordinate> centerCoordinate,
        int radiusMeters,
        List<Criterion> criteria,
        List<Source> sources,
        List<Candidate> candidates) {

    public DiscoveryCollection {
        collectionId = requireText(collectionId, "collectionId");
        importFingerprint = requireText(importFingerprint, "importFingerprint");
        researchedAt = Objects.requireNonNull(researchedAt, "researchedAt");
        questionRef = requireText(questionRef, "questionRef");
        questionText = requireText(questionText, "questionText");
        centerLabel = requireText(centerLabel, "centerLabel");
        centerCoordinate = centerCoordinate == null ? Optional.empty() : centerCoordinate;
        if (radiusMeters < 1) {
            throw new IllegalArgumentException("radiusMeters must be positive.");
        }
        criteria = List.copyOf(criteria);
        sources = List.copyOf(sources);
        candidates = List.copyOf(candidates);
    }

    public record Criterion(String criterionRef, String description, ResearchEvaluationMode evaluationMode) {
        public Criterion {
            criterionRef = requireText(criterionRef, "criterionRef");
            description = requireText(description, "description");
            evaluationMode = Objects.requireNonNull(evaluationMode, "evaluationMode");
        }
    }

    public record Source(String sourceRef, String url, Optional<String> title, OffsetDateTime retrievedAt) {
        public Source {
            sourceRef = requireText(sourceRef, "sourceRef");
            url = requireText(url, "url");
            title = title == null ? Optional.empty() : title;
            retrievedAt = Objects.requireNonNull(retrievedAt, "retrievedAt");
        }
    }

    public record Candidate(
            String candidateRef,
            String displayName,
            Optional<Identity> identity,
            ResearchedLocation researchedLocation,
            List<Claim> claims) {
        public Candidate {
            candidateRef = requireText(candidateRef, "candidateRef");
            displayName = requireText(displayName, "displayName");
            identity = identity == null ? Optional.empty() : identity;
            researchedLocation = Objects.requireNonNull(researchedLocation, "researchedLocation");
            claims = List.copyOf(claims);
        }
    }

    public record Identity(Optional<String> canonicalUri, List<ExternalId> externalIds) {
        public Identity {
            canonicalUri = canonicalUri == null ? Optional.empty() : canonicalUri;
            externalIds = List.copyOf(externalIds);
        }
    }

    public record ExternalId(String provider, String externalId) {
        public ExternalId {
            provider = requireText(provider, "provider");
            externalId = requireText(externalId, "externalId");
        }
    }

    public record ResearchedLocation(String label, Optional<Coordinate> coordinate, List<String> sourceRefs) {
        public ResearchedLocation {
            label = requireText(label, "label");
            coordinate = coordinate == null ? Optional.empty() : coordinate;
            sourceRefs = List.copyOf(sourceRefs);
        }
    }

    public record Claim(
            String criterionRef,
            ClaimStatus status,
            ClaimBasis basis,
            Optional<ClaimValue> observedValue,
            List<String> sourceRefs,
            Optional<String> note) {
        public Claim {
            criterionRef = requireText(criterionRef, "criterionRef");
            status = Objects.requireNonNull(status, "status");
            basis = Objects.requireNonNull(basis, "basis");
            observedValue = observedValue == null ? Optional.empty() : observedValue;
            sourceRefs = List.copyOf(sourceRefs);
            note = note == null ? Optional.empty() : note;
        }
    }

    public record ClaimValue(ValueKind kind, String value) {
        public ClaimValue {
            kind = Objects.requireNonNull(kind, "kind");
            value = Objects.requireNonNull(value, "value");
        }
    }

    public enum ClaimStatus {
        MATCH,
        NO_MATCH,
        UNCERTAIN,
        UNKNOWN
    }

    public enum ClaimBasis {
        DIRECT_EVIDENCE,
        HEURISTIC
    }

    public enum ValueKind {
        TEXT,
        NUMBER,
        BOOLEAN
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
