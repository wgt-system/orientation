package system.wgt.orientation.domain.research;

import java.util.Objects;
import java.util.regex.Pattern;

public record SpatialResearchCriterion(
        String criterionRef,
        String description,
        ResearchEvaluationMode evaluationMode) {

    private static final Pattern LOCAL_REF = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}");

    public SpatialResearchCriterion {
        criterionRef = requireRef(criterionRef);
        description = requireText(description, "description", 1000);
        evaluationMode = Objects.requireNonNull(evaluationMode, "evaluationMode");
    }

    private static String requireRef(String value) {
        if (value == null || !LOCAL_REF.matcher(value).matches()) {
            throw new IllegalArgumentException("criterionRef must be a valid local reference.");
        }
        return value;
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " is too long.");
        }
        return trimmed;
    }
}
