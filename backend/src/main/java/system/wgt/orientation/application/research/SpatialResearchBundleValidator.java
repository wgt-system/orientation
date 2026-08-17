package system.wgt.orientation.application.research;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SpatialResearchBundleValidator {
    public static final String CONTRACT = "orientation.spatial-research-bundle";
    public static final String VERSION = "1.0";

    private static final Pattern LOCAL_REF = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}");
    private static final Set<String> EVALUATION_MODES = Set.of("EVIDENCE_REQUIRED", "HEURISTIC");
    private static final Set<String> CLAIM_STATUSES = Set.of("MATCH", "NO_MATCH", "UNCERTAIN", "UNKNOWN");
    private static final Set<String> CLAIM_BASES = Set.of("DIRECT_EVIDENCE", "HEURISTIC");

    private final ObjectMapper objectMapper;

    public SpatialResearchBundleValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SpatialResearchValidationResult validate(String json) {
        var errors = new ArrayList<String>();
        if (json == null || json.isBlank()) {
            return new SpatialResearchValidationResult(List.of("bundle must be non-blank JSON"));
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception exception) {
            return new SpatialResearchValidationResult(List.of("bundle is not valid JSON"));
        }

        if (root == null || !root.isObject()) {
            return new SpatialResearchValidationResult(List.of("bundle root must be an object"));
        }

        exactText(root, "contract", CONTRACT, "contract", errors);
        exactText(root, "version", VERSION, "version", errors);
        timestamp(root, "researchedAt", "researchedAt", errors);

        var criteria = validateQuestion(requiredObject(root, "question", "question", errors), errors);
        var sources = validateSources(requiredArray(root, "sources", "sources", errors), errors);
        validateCandidates(requiredArray(root, "candidates", "candidates", errors), criteria, sources, errors);

        return new SpatialResearchValidationResult(errors);
    }

    private Map<String, String> validateQuestion(JsonNode question, List<String> errors) {
        var criteria = new LinkedHashMap<String, String>();
        if (question == null) {
            return criteria;
        }

        localRef(question, "questionRef", "question.questionRef", errors);
        boundedText(question, "text", "question.text", 2000, errors);

        var area = requiredObject(question, "area", "question.area", errors);
        if (area != null) {
            var center = requiredObject(area, "center", "question.area.center", errors);
            if (center != null) {
                boundedText(center, "label", "question.area.center.label", 500, errors);
                var coordinate = center.get("coordinate");
                if (coordinate != null && !coordinate.isNull()) {
                    coordinate(coordinate, "question.area.center.coordinate", errors);
                }
            }
            var radius = area.get("radiusMeters");
            if (radius == null || !radius.isIntegralNumber() || radius.intValue() < 1 || radius.intValue() > 500_000) {
                errors.add("question.area.radiusMeters must be an integer between 1 and 500000");
            }
        }

        var criteriaNode = requiredArray(question, "criteria", "question.criteria", errors);
        if (criteriaNode == null) {
            return criteria;
        }
        if (criteriaNode.isEmpty() || criteriaNode.size() > 20) {
            errors.add("question.criteria must contain between 1 and 20 criteria");
        }

        int index = 0;
        for (JsonNode criterion : criteriaNode) {
            var path = "question.criteria[" + index + "]";
            if (!criterion.isObject()) {
                errors.add(path + " must be an object");
                index++;
                continue;
            }
            var ref = localRef(criterion, "criterionRef", path + ".criterionRef", errors);
            boundedText(criterion, "description", path + ".description", 1000, errors);
            var mode = enumText(criterion, "evaluationMode", path + ".evaluationMode", EVALUATION_MODES, errors);
            if (ref != null && mode != null && criteria.putIfAbsent(ref, mode) != null) {
                errors.add(path + ".criterionRef must be unique within the question");
            }
            index++;
        }
        return criteria;
    }

    private Set<String> validateSources(JsonNode sourcesNode, List<String> errors) {
        var sources = new LinkedHashSet<String>();
        if (sourcesNode == null) {
            return sources;
        }
        if (sourcesNode.size() > 100) {
            errors.add("sources must contain at most 100 entries");
        }

        int index = 0;
        for (JsonNode source : sourcesNode) {
            var path = "sources[" + index + "]";
            if (!source.isObject()) {
                errors.add(path + " must be an object");
                index++;
                continue;
            }
            var ref = localRef(source, "sourceRef", path + ".sourceRef", errors);
            httpsUri(source, "url", path + ".url", errors);
            timestamp(source, "retrievedAt", path + ".retrievedAt", errors);
            var title = source.get("title");
            if (title != null && !title.isNull()) {
                boundedText(source, "title", path + ".title", 500, errors);
            }
            if (ref != null && !sources.add(ref)) {
                errors.add(path + ".sourceRef must be unique within the bundle");
            }
            index++;
        }
        return sources;
    }

    private void validateCandidates(JsonNode candidatesNode, Map<String, String> criteria,
                                    Set<String> sources, List<String> errors) {
        if (candidatesNode == null) {
            return;
        }
        if (candidatesNode.size() > 500) {
            errors.add("candidates must contain at most 500 entries");
        }

        var candidateRefs = new LinkedHashSet<String>();
        int index = 0;
        for (JsonNode candidate : candidatesNode) {
            var path = "candidates[" + index + "]";
            if (!candidate.isObject()) {
                errors.add(path + " must be an object");
                index++;
                continue;
            }

            var candidateRef = localRef(candidate, "candidateRef", path + ".candidateRef", errors);
            if (candidateRef != null && !candidateRefs.add(candidateRef)) {
                errors.add(path + ".candidateRef must be unique within the bundle");
            }
            boundedText(candidate, "displayName", path + ".displayName", 500, errors);
            validateIdentity(candidate.get("identity"), path + ".identity", errors);
            validateResearchedLocation(requiredObject(candidate, "researchedLocation", path + ".researchedLocation", errors),
                    path + ".researchedLocation", sources, errors);
            validateClaims(requiredArray(candidate, "claims", path + ".claims", errors), path + ".claims",
                    criteria, sources, errors);
            index++;
        }
    }

    private void validateIdentity(JsonNode identity, String path, List<String> errors) {
        if (identity == null || identity.isNull()) {
            return;
        }
        if (!identity.isObject()) {
            errors.add(path + " must be an object when present");
            return;
        }

        boolean hasCanonicalUri = identity.get("canonicalUri") != null && !identity.get("canonicalUri").isNull();
        boolean hasExternalIds = identity.get("externalIds") != null && !identity.get("externalIds").isNull();
        if (!hasCanonicalUri && !hasExternalIds) {
            errors.add(path + " must contain canonicalUri or externalIds");
        }
        if (hasCanonicalUri) {
            httpsUri(identity, "canonicalUri", path + ".canonicalUri", errors);
        }
        if (hasExternalIds) {
            var ids = identity.get("externalIds");
            if (!ids.isArray() || ids.isEmpty() || ids.size() > 10) {
                errors.add(path + ".externalIds must contain between 1 and 10 entries");
            } else {
                int index = 0;
                for (JsonNode id : ids) {
                    var idPath = path + ".externalIds[" + index + "]";
                    if (!id.isObject()) {
                        errors.add(idPath + " must be an object");
                    } else {
                        boundedText(id, "provider", idPath + ".provider", 100, errors);
                        boundedText(id, "id", idPath + ".id", 300, errors);
                    }
                    index++;
                }
            }
        }
    }

    private void validateResearchedLocation(JsonNode location, String path, Set<String> sources, List<String> errors) {
        if (location == null) {
            return;
        }
        boundedText(location, "label", path + ".label", 500, errors);
        var coordinate = location.get("coordinate");
        if (coordinate != null && !coordinate.isNull()) {
            coordinate(coordinate, path + ".coordinate", errors);
        }
        validateSourceRefs(requiredArray(location, "sourceRefs", path + ".sourceRefs", errors), path + ".sourceRefs",
                sources, false, errors);
    }

    private void validateClaims(JsonNode claimsNode, String path, Map<String, String> criteria,
                                Set<String> sources, List<String> errors) {
        if (claimsNode == null) {
            return;
        }
        if (claimsNode.isEmpty() || claimsNode.size() > 20) {
            errors.add(path + " must contain between 1 and 20 claims");
        }

        var seenCriteria = new LinkedHashSet<String>();
        int index = 0;
        for (JsonNode claim : claimsNode) {
            var claimPath = path + "[" + index + "]";
            if (!claim.isObject()) {
                errors.add(claimPath + " must be an object");
                index++;
                continue;
            }

            var criterionRef = localRef(claim, "criterionRef", claimPath + ".criterionRef", errors);
            var status = enumText(claim, "status", claimPath + ".status", CLAIM_STATUSES, errors);
            var basis = enumText(claim, "basis", claimPath + ".basis", CLAIM_BASES, errors);
            if (criterionRef != null) {
                if (!criteria.containsKey(criterionRef)) {
                    errors.add(claimPath + ".criterionRef must reference a criterion from question.criteria");
                } else if (!seenCriteria.add(criterionRef)) {
                    errors.add(claimPath + ".criterionRef must occur at most once per candidate");
                }
            }

            var observedValue = claim.get("observedValue");
            if (observedValue != null && !observedValue.isNull()
                    && !observedValue.isTextual() && !observedValue.isNumber() && !observedValue.isBoolean()) {
                errors.add(claimPath + ".observedValue must be a string, number, boolean or null");
            }
            var note = claim.get("note");
            if (note != null && !note.isNull()) {
                boundedText(claim, "note", claimPath + ".note", 2000, errors);
            }

            boolean evidenceRequired = "MATCH".equals(status) || "NO_MATCH".equals(status);
            validateSourceRefs(requiredArray(claim, "sourceRefs", claimPath + ".sourceRefs", errors),
                    claimPath + ".sourceRefs", sources, evidenceRequired, errors);

            if (criterionRef != null && basis != null) {
                var evaluationMode = criteria.get(criterionRef);
                if ("HEURISTIC".equals(evaluationMode) && !"HEURISTIC".equals(basis)) {
                    errors.add(claimPath + " for a HEURISTIC criterion must use basis HEURISTIC");
                }
                if ("EVIDENCE_REQUIRED".equals(evaluationMode) && evidenceRequired && !"DIRECT_EVIDENCE".equals(basis)) {
                    errors.add(claimPath + " MATCH/NO_MATCH for an EVIDENCE_REQUIRED criterion must use DIRECT_EVIDENCE");
                }
            }
            index++;
        }

        for (String criterionRef : criteria.keySet()) {
            if (!seenCriteria.contains(criterionRef)) {
                errors.add(path + " must contain one claim for criterion " + criterionRef);
            }
        }
    }

    private void validateSourceRefs(JsonNode sourceRefs, String path, Set<String> knownSources,
                                    boolean requireEvidence, List<String> errors) {
        if (sourceRefs == null) {
            return;
        }
        if (sourceRefs.size() > 20) {
            errors.add(path + " must contain at most 20 source refs");
        }
        if (requireEvidence && sourceRefs.isEmpty()) {
            errors.add(path + " must not be empty for MATCH or NO_MATCH claims");
        }
        for (JsonNode refNode : sourceRefs) {
            if (!refNode.isTextual() || !LOCAL_REF.matcher(refNode.asText()).matches()) {
                errors.add(path + " must contain only valid local refs");
            } else if (!knownSources.contains(refNode.asText())) {
                errors.add(path + " references unknown source " + refNode.asText());
            }
        }
    }

    private void coordinate(JsonNode coordinate, String path, List<String> errors) {
        if (!coordinate.isObject()) {
            errors.add(path + " must be an object");
            return;
        }
        var longitude = coordinate.get("longitude");
        var latitude = coordinate.get("latitude");
        if (longitude == null || !longitude.isNumber() || !Double.isFinite(longitude.doubleValue())
                || longitude.doubleValue() < -180 || longitude.doubleValue() > 180) {
            errors.add(path + ".longitude must be a finite number in [-180, 180]");
        }
        if (latitude == null || !latitude.isNumber() || !Double.isFinite(latitude.doubleValue())
                || latitude.doubleValue() < -90 || latitude.doubleValue() > 90) {
            errors.add(path + ".latitude must be a finite number in [-90, 90]");
        }
    }

    private String localRef(JsonNode parent, String field, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual() || !LOCAL_REF.matcher(value.asText()).matches()) {
            errors.add(path + " must match " + LOCAL_REF.pattern());
            return null;
        }
        return value.asText();
    }

    private String boundedText(JsonNode parent, String field, String path, int maxLength, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank() || value.asText().length() > maxLength) {
            errors.add(path + " must be non-blank text up to " + maxLength + " characters");
            return null;
        }
        return value.asText();
    }

    private String enumText(JsonNode parent, String field, String path, Set<String> accepted, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual() || !accepted.contains(value.asText())) {
            errors.add(path + " must be one of " + accepted);
            return null;
        }
        return value.asText();
    }

    private void exactText(JsonNode parent, String field, String expected, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual() || !expected.equals(value.asText())) {
            errors.add(path + " must be exactly " + expected);
        }
    }

    private void timestamp(JsonNode parent, String field, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual()) {
            errors.add(path + " must be an ISO-8601 timestamp with offset");
            return;
        }
        try {
            OffsetDateTime.parse(value.asText());
        } catch (DateTimeParseException exception) {
            errors.add(path + " must be an ISO-8601 timestamp with offset");
        }
    }

    private void httpsUri(JsonNode parent, String field, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isTextual()) {
            errors.add(path + " must be an absolute HTTPS URI");
            return;
        }
        try {
            var uri = URI.create(value.asText());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
                errors.add(path + " must be an absolute HTTPS URI");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(path + " must be an absolute HTTPS URI");
        }
    }

    private JsonNode requiredObject(JsonNode parent, String field, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isObject()) {
            errors.add(path + " must be an object");
            return null;
        }
        return value;
    }

    private JsonNode requiredArray(JsonNode parent, String field, String path, List<String> errors) {
        var value = parent.get(field);
        if (value == null || !value.isArray()) {
            errors.add(path + " must be an array");
            return null;
        }
        return value;
    }
}
