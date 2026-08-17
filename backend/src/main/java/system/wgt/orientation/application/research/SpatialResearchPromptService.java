package system.wgt.orientation.application.research;

import system.wgt.orientation.domain.research.SpatialResearchQuestion;

public final class SpatialResearchPromptService {
    public static final String SCHEMA_ID = "https://schemas.wgt-system.org/orientation/spatial-research/1.0/schema.json";

    public GeneratedSpatialResearchPrompt generate(SpatialResearchQuestion question) {
        if (question == null) {
            throw new IllegalArgumentException("question is required.");
        }

        var prompt = new StringBuilder();
        prompt.append("You are producing structured external spatial research for Orientation.\n\n")
                .append("Return JSON only. Do not wrap the result in Markdown.\n")
                .append("Contract: ").append(SpatialResearchBundleValidator.CONTRACT).append('\n')
                .append("Version: ").append(SpatialResearchBundleValidator.VERSION).append('\n')
                .append("Schema ID: ").append(SCHEMA_ID).append("\n\n")
                .append("RESEARCH QUESTION\n")
                .append("questionRef: ").append(question.questionRef()).append('\n')
                .append("text: ").append(question.text()).append('\n')
                .append("area center: ").append(question.centerLabel()).append('\n');

        question.centerCoordinate().ifPresent(coordinate -> prompt
                .append("area center coordinate: longitude=").append(coordinate.longitude())
                .append(", latitude=").append(coordinate.latitude()).append('\n'));

        prompt.append("radiusMeters: ").append(question.radiusMeters()).append("\n\n")
                .append("CRITERIA\n");

        int index = 1;
        for (var criterion : question.criteria()) {
            prompt.append(index++).append(". ")
                    .append(criterion.criterionRef()).append(" [")
                    .append(criterion.evaluationMode()).append("] — ")
                    .append(criterion.description()).append('\n');
        }

        prompt.append("\nRESEARCH AND OUTPUT RULES\n")
                .append("1. Research only candidates plausibly inside the explicit radial area.\n")
                .append("2. Use absolute HTTPS sources and record retrieval timestamps. Prefer direct/primary sources where practical.\n")
                .append("3. Every candidate must contain exactly one claim for every criterion. Use only MATCH, NO_MATCH, UNCERTAIN or UNKNOWN.\n")
                .append("4. For EVIDENCE_REQUIRED criteria, MATCH or NO_MATCH must use DIRECT_EVIDENCE and cite at least one sourceRef.\n")
                .append("5. For HEURISTIC criteria, always use basis HEURISTIC. A heuristic MATCH means only that the user-defined heuristic matched; do not convert names, language, appearance or other proxies into claims about ethnicity, nationality, religion or another sensitive trait.\n")
                .append("6. Preserve uncertainty. If evidence is missing or conflicting, use UNCERTAIN or UNKNOWN instead of guessing.\n")
                .append("7. candidateRef and sourceRef are bundle-local references. Do not invent a cross-import identity from similar names or addresses.\n")
                .append("8. identity is optional. Supply canonicalUri only for a canonical absolute HTTPS page and externalIds only when a real provider/external identifier is known.\n")
                .append("9. researchedLocation is external research evidence, not an Orientation provider-backed Place. Include sourceRefs for the location evidence; coordinate is optional if not reliably supported.\n")
                .append("10. Do not add fields outside the schema, do not include Vocation/Illumination/WGT data, and do not include commentary outside the JSON.\n\n")
                .append("REQUIRED TOP-LEVEL SHAPE\n")
                .append("{\n")
                .append("  \"contract\": \"").append(SpatialResearchBundleValidator.CONTRACT).append("\",\n")
                .append("  \"version\": \"").append(SpatialResearchBundleValidator.VERSION).append("\",\n")
                .append("  \"researchedAt\": \"<ISO-8601 timestamp with offset>\",\n")
                .append("  \"question\": {\n")
                .append("    \"questionRef\": \"").append(question.questionRef()).append("\",\n")
                .append("    \"text\": \"<the exact supplied research question>\",\n")
                .append("    \"area\": {\"center\": {\"label\": \"<center>\"}, \"radiusMeters\": <integer>},\n")
                .append("    \"criteria\": [{\"criterionRef\": \"<ref>\", \"description\": \"<description>\", \"evaluationMode\": \"EVIDENCE_REQUIRED|HEURISTIC\"}]\n")
                .append("  },\n")
                .append("  \"sources\": [{\"sourceRef\": \"source-1\", \"url\": \"https://...\", \"retrievedAt\": \"<timestamp>\"}],\n")
                .append("  \"candidates\": [{\n")
                .append("    \"candidateRef\": \"candidate-1\",\n")
                .append("    \"displayName\": \"<name>\",\n")
                .append("    \"researchedLocation\": {\"label\": \"<location>\", \"sourceRefs\": [\"source-1\"]},\n")
                .append("    \"claims\": [{\"criterionRef\": \"<criterion-ref>\", \"status\": \"MATCH|NO_MATCH|UNCERTAIN|UNKNOWN\", \"basis\": \"DIRECT_EVIDENCE|HEURISTIC\", \"sourceRefs\": [\"source-1\"]}]\n")
                .append("  }]\n")
                .append("}\n");

        return new GeneratedSpatialResearchPrompt(
                SpatialResearchBundleValidator.CONTRACT,
                SpatialResearchBundleValidator.VERSION,
                SCHEMA_ID,
                prompt.toString());
    }
}
