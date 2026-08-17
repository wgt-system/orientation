package system.wgt.orientation.application.discovery;

import system.wgt.orientation.domain.discovery.DiscoveryCollection;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.research.ResearchEvaluationMode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class SpatialResearchBundleTranslator {
    private final ObjectMapper objectMapper;

    SpatialResearchBundleTranslator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    DiscoveryCollection translate(JsonNode root) {
        JsonNode question = root.get("question");
        JsonNode area = question.get("area");
        JsonNode center = area.get("center");

        List<DiscoveryCollection.Criterion> criteria = new ArrayList<>();
        for (JsonNode criterion : question.get("criteria")) {
            criteria.add(new DiscoveryCollection.Criterion(
                    criterion.get("criterionRef").asText(),
                    criterion.get("description").asText(),
                    ResearchEvaluationMode.valueOf(criterion.get("evaluationMode").asText())));
        }

        List<DiscoveryCollection.Source> sources = new ArrayList<>();
        for (JsonNode source : root.get("sources")) {
            sources.add(new DiscoveryCollection.Source(
                    source.get("sourceRef").asText(),
                    source.get("url").asText(),
                    optionalText(source.get("title")),
                    OffsetDateTime.parse(source.get("retrievedAt").asText())));
        }

        List<DiscoveryCollection.Candidate> candidates = new ArrayList<>();
        for (JsonNode candidate : root.get("candidates")) {
            candidates.add(candidate(candidate));
        }

        return new DiscoveryCollection(
                UUID.randomUUID().toString(),
                fingerprint(root),
                OffsetDateTime.parse(root.get("researchedAt").asText()),
                question.get("questionRef").asText(),
                question.get("text").asText(),
                center.get("label").asText(),
                optionalCoordinate(center.get("coordinate")),
                area.get("radiusMeters").intValue(),
                criteria,
                sources,
                candidates);
    }

    private DiscoveryCollection.Candidate candidate(JsonNode node) {
        List<DiscoveryCollection.Claim> claims = new ArrayList<>();
        for (JsonNode claim : node.get("claims")) {
            claims.add(new DiscoveryCollection.Claim(
                    claim.get("criterionRef").asText(),
                    DiscoveryCollection.ClaimStatus.valueOf(claim.get("status").asText()),
                    DiscoveryCollection.ClaimBasis.valueOf(claim.get("basis").asText()),
                    claimValue(claim.get("observedValue")),
                    refs(claim.get("sourceRefs")),
                    optionalText(claim.get("note"))));
        }

        JsonNode researchedLocation = node.get("researchedLocation");
        var location = new DiscoveryCollection.ResearchedLocation(
                researchedLocation.get("label").asText(),
                optionalCoordinate(researchedLocation.get("coordinate")),
                refs(researchedLocation.get("sourceRefs")));

        return new DiscoveryCollection.Candidate(
                node.get("candidateRef").asText(),
                node.get("displayName").asText(),
                identity(node.get("identity")),
                location,
                claims);
    }

    private Optional<DiscoveryCollection.Identity> identity(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        List<DiscoveryCollection.ExternalId> externalIds = new ArrayList<>();
        JsonNode ids = node.get("externalIds");
        if (ids != null && ids.isArray()) {
            for (JsonNode id : ids) {
                externalIds.add(new DiscoveryCollection.ExternalId(
                        id.get("provider").asText(), id.get("id").asText()));
            }
        }
        return Optional.of(new DiscoveryCollection.Identity(optionalText(node.get("canonicalUri")), externalIds));
    }

    private Optional<DiscoveryCollection.ClaimValue> claimValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isTextual()) {
            return Optional.of(new DiscoveryCollection.ClaimValue(DiscoveryCollection.ValueKind.TEXT, node.asText()));
        }
        if (node.isNumber()) {
            return Optional.of(new DiscoveryCollection.ClaimValue(DiscoveryCollection.ValueKind.NUMBER, node.toString()));
        }
        if (node.isBoolean()) {
            return Optional.of(new DiscoveryCollection.ClaimValue(DiscoveryCollection.ValueKind.BOOLEAN, node.asText()));
        }
        throw new IllegalArgumentException("validated observedValue has unsupported type");
    }

    private Optional<Coordinate> optionalCoordinate(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(new Coordinate(node.get("longitude").doubleValue(), node.get("latitude").doubleValue()));
    }

    private Optional<String> optionalText(JsonNode node) {
        return node == null || node.isNull() ? Optional.empty() : Optional.of(node.asText());
    }

    private List<String> refs(JsonNode array) {
        List<String> refs = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode ref : array) {
                refs.add(ref.asText());
            }
        }
        return refs;
    }

    private String fingerprint(JsonNode root) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical(root).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonical(JsonNode node) {
        if (node.isObject()) {
            var properties = new ArrayList<>(node.properties());
            properties.sort(Comparator.comparing(java.util.Map.Entry::getKey));
            var builder = new StringBuilder("{");
            for (int index = 0; index < properties.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                var entry = properties.get(index);
                builder.append(quoted(entry.getKey())).append(':').append(canonical(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (node.isArray()) {
            var builder = new StringBuilder("[");
            int index = 0;
            for (JsonNode child : node) {
                if (index++ > 0) {
                    builder.append(',');
                }
                builder.append(canonical(child));
            }
            return builder.append(']').toString();
        }
        return node.toString();
    }

    private String quoted(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to canonicalize JSON property name", exception);
        }
    }
}
