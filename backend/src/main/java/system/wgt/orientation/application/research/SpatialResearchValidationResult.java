package system.wgt.orientation.application.research;

import java.util.List;

public record SpatialResearchValidationResult(List<String> errors) {
    public SpatialResearchValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
