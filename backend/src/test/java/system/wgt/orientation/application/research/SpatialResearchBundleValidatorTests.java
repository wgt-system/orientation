package system.wgt.orientation.application.research;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialResearchBundleValidatorTests {
    private final SpatialResearchBundleValidator validator = new SpatialResearchBundleValidator(new ObjectMapper());

    @Test
    void acceptsCanonicalBundle() throws IOException {
        var result = validator.validate(fixture("spatial-research-v1.valid.json"));

        assertTrue(result.valid(), () -> String.join("\n", result.errors()));
    }

    @Test
    void rejectsHeuristicCriterionPresentedAsDirectEvidence() throws IOException {
        var result = validator.validate(fixture("spatial-research-v1.invalid-heuristic-basis.json"));

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("HEURISTIC criterion must use basis HEURISTIC")));
    }

    @Test
    void rejectsUnknownSourceReference() throws IOException {
        var invalid = fixture("spatial-research-v1.valid.json")
                .replace("[\"source-menu\"]", "[\"source-missing\"]");

        var result = validator.validate(invalid);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("references unknown source source-missing")));
    }

    @Test
    void rejectsIncompatibleContractVersion() throws IOException {
        var invalid = fixture("spatial-research-v1.valid.json")
                .replace("\"version\": \"1.0\"", "\"version\": \"9.9\"");

        var result = validator.validate(invalid);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("version must be exactly 1.0")));
    }

    @Test
    void requiresOneClaimPerCriterion() throws IOException {
        var result = validator.validate(fixture("spatial-research-v1.invalid-missing-claim.json"));

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("must contain one claim for criterion criterion-b")));
    }

    private String fixture(String name) throws IOException {
        return Files.readString(Path.of("..", "contracts", "examples", name));
    }
}
