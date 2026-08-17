package system.wgt.orientation.application.discovery;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SpatialResearchBundleShapeValidator {
    List<String> validate(JsonNode root) {
        List<String> errors = new ArrayList<>();
        if (root == null || !root.isObject()) {
            return errors;
        }

        allowed(root, "$", Set.of("contract", "version", "researchedAt", "question", "sources", "candidates"), errors);
        JsonNode question = root.get("question");
        if (object(question)) {
            allowed(question, "question", Set.of("questionRef", "text", "area", "criteria"), errors);
            JsonNode area = question.get("area");
            if (object(area)) {
                allowed(area, "question.area", Set.of("center", "radiusMeters"), errors);
                JsonNode center = area.get("center");
                if (object(center)) {
                    allowed(center, "question.area.center", Set.of("label", "coordinate"), errors);
                    coordinate(center.get("coordinate"), "question.area.center.coordinate", errors);
                }
            }
            JsonNode criteria = question.get("criteria");
            if (array(criteria)) {
                int index = 0;
                for (JsonNode criterion : criteria) {
                    if (object(criterion)) {
                        allowed(criterion, "question.criteria[" + index + "]",
                                Set.of("criterionRef", "description", "evaluationMode"), errors);
                    }
                    index++;
                }
            }
        }

        JsonNode sources = root.get("sources");
        if (array(sources)) {
            int index = 0;
            for (JsonNode source : sources) {
                if (object(source)) {
                    allowed(source, "sources[" + index + "]", Set.of("sourceRef", "url", "title", "retrievedAt"), errors);
                }
                index++;
            }
        }

        JsonNode candidates = root.get("candidates");
        if (array(candidates)) {
            int index = 0;
            for (JsonNode candidate : candidates) {
                String path = "candidates[" + index + "]";
                if (object(candidate)) {
                    allowed(candidate, path, Set.of("candidateRef", "displayName", "identity", "researchedLocation", "claims"), errors);
                    identity(candidate.get("identity"), path + ".identity", errors);
                    location(candidate.get("researchedLocation"), path + ".researchedLocation", errors);
                    claims(candidate.get("claims"), path + ".claims", errors);
                }
                index++;
            }
        }
        return errors;
    }

    private void identity(JsonNode identity, String path, List<String> errors) {
        if (!object(identity)) {
            return;
        }
        allowed(identity, path, Set.of("canonicalUri", "externalIds"), errors);
        JsonNode externalIds = identity.get("externalIds");
        if (array(externalIds)) {
            int index = 0;
            for (JsonNode externalId : externalIds) {
                if (object(externalId)) {
                    allowed(externalId, path + ".externalIds[" + index + "]", Set.of("provider", "id"), errors);
                }
                index++;
            }
        }
    }

    private void location(JsonNode location, String path, List<String> errors) {
        if (!object(location)) {
            return;
        }
        allowed(location, path, Set.of("label", "coordinate", "sourceRefs"), errors);
        coordinate(location.get("coordinate"), path + ".coordinate", errors);
    }

    private void claims(JsonNode claims, String path, List<String> errors) {
        if (!array(claims)) {
            return;
        }
        int index = 0;
        for (JsonNode claim : claims) {
            if (object(claim)) {
                allowed(claim, path + "[" + index + "]",
                        Set.of("criterionRef", "status", "basis", "observedValue", "sourceRefs", "note"), errors);
            }
            index++;
        }
    }

    private void coordinate(JsonNode coordinate, String path, List<String> errors) {
        if (object(coordinate)) {
            allowed(coordinate, path, Set.of("longitude", "latitude"), errors);
        }
    }

    private void allowed(JsonNode node, String path, Set<String> allowed, List<String> errors) {
        for (var property : node.properties()) {
            if (!allowed.contains(property.getKey())) {
                errors.add(path + " contains unsupported property " + property.getKey());
            }
        }
    }

    private boolean object(JsonNode node) {
        return node != null && !node.isNull() && node.isObject();
    }

    private boolean array(JsonNode node) {
        return node != null && !node.isNull() && node.isArray();
    }
}
