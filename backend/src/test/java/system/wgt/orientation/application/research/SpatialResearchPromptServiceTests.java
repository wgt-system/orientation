package system.wgt.orientation.application.research;

import org.junit.jupiter.api.Test;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.research.ResearchEvaluationMode;
import system.wgt.orientation.domain.research.SpatialResearchCriterion;
import system.wgt.orientation.domain.research.SpatialResearchQuestion;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialResearchPromptServiceTests {
    private final SpatialResearchPromptService service = new SpatialResearchPromptService();

    @Test
    void generatesDeterministicContractBoundPrompt() {
        var question = new SpatialResearchQuestion(
                "restaurants-nearby",
                "Find suitable restaurants nearby.",
                "Hamburg Hauptbahnhof",
                Optional.of(new Coordinate(10.0067, 53.5526)),
                5000,
                List.of(
                        new SpatialResearchCriterion("operator-name-pattern", "Match the explicit user-defined name pattern.", ResearchEvaluationMode.HEURISTIC),
                        new SpatialResearchCriterion("vegetarian-options", "Document vegetarian options from a source.", ResearchEvaluationMode.EVIDENCE_REQUIRED)));

        var first = service.generate(question);
        var second = service.generate(question);

        assertEquals(first, second);
        assertEquals(SpatialResearchBundleValidator.CONTRACT, first.contract());
        assertEquals(SpatialResearchBundleValidator.VERSION, first.version());
        assertEquals(SpatialResearchPromptService.SCHEMA_ID, first.schemaId());
        assertTrue(first.prompt().contains("questionRef: restaurants-nearby"));
        assertTrue(first.prompt().contains("operator-name-pattern [HEURISTIC]"));
        assertTrue(first.prompt().contains("vegetarian-options [EVIDENCE_REQUIRED]"));
        assertTrue(first.prompt().contains("do not convert names, language, appearance or other proxies into claims about ethnicity"));
        assertTrue(first.prompt().contains("Return JSON only"));
    }
}
