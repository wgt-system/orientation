package system.wgt.orientation.host.research;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.wgt.orientation.application.research.GeneratedSpatialResearchPrompt;
import system.wgt.orientation.application.research.SpatialResearchPromptService;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.research.ResearchEvaluationMode;
import system.wgt.orientation.domain.research.SpatialResearchCriterion;
import system.wgt.orientation.domain.research.SpatialResearchQuestion;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/research")
public class ResearchPromptController {
    private final SpatialResearchPromptService promptService;

    public ResearchPromptController(SpatialResearchPromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping("/prompts")
    public PromptResponse generate(@RequestBody PromptRequest request) {
        if (request == null || request.area() == null || request.criteria() == null) {
            throw new IllegalArgumentException("question, area and criteria are required.");
        }
        var center = request.area().center();
        if (center == null) {
            throw new IllegalArgumentException("area.center is required.");
        }

        Optional<Coordinate> coordinate = center.coordinate() == null
                ? Optional.empty()
                : Optional.of(new Coordinate(center.coordinate().longitude(), center.coordinate().latitude()));

        List<SpatialResearchCriterion> criteria = request.criteria().stream()
                .map(criterion -> new SpatialResearchCriterion(
                        criterion.criterionRef(),
                        criterion.description(),
                        evaluationMode(criterion.evaluationMode())))
                .toList();

        var question = new SpatialResearchQuestion(
                request.questionRef(),
                request.text(),
                center.label(),
                coordinate,
                request.area().radiusMeters(),
                criteria);

        return PromptResponse.from(promptService.generate(question));
    }

    private ResearchEvaluationMode evaluationMode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("evaluationMode is required.");
        }
        try {
            return ResearchEvaluationMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("evaluationMode must be EVIDENCE_REQUIRED or HEURISTIC.");
        }
    }

    public record PromptRequest(
            String questionRef,
            String text,
            AreaDto area,
            List<CriterionDto> criteria) {
    }

    public record AreaDto(CenterDto center, int radiusMeters) {
    }

    public record CenterDto(String label, CoordinateDto coordinate) {
    }

    public record CoordinateDto(double longitude, double latitude) {
    }

    public record CriterionDto(String criterionRef, String description, String evaluationMode) {
    }

    public record PromptResponse(String contract, String version, String schemaId, String prompt) {
        static PromptResponse from(GeneratedSpatialResearchPrompt generated) {
            return new PromptResponse(generated.contract(), generated.version(), generated.schemaId(), generated.prompt());
        }
    }
}
