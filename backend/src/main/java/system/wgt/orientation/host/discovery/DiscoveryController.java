package system.wgt.orientation.host.discovery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.wgt.orientation.application.discovery.DiscoveryImportReport;
import system.wgt.orientation.application.discovery.DiscoveryImportService;
import system.wgt.orientation.application.discovery.DiscoveryRepository;
import system.wgt.orientation.domain.discovery.DiscoveryCollection;
import system.wgt.orientation.domain.discovery.DiscoveryCollectionSummary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final DiscoveryImportService importService;
    private final DiscoveryRepository repository;
    private final ObjectMapper objectMapper;

    public DiscoveryController(DiscoveryImportService importService,
                               DiscoveryRepository repository,
                               ObjectMapper objectMapper) {
        this.importService = importService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/imports")
    public ResponseEntity<ImportResponse> importBundle(@RequestBody JsonNode bundle) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(bundle);
        } catch (Exception exception) {
            throw new IllegalArgumentException("bundle could not be serialized");
        }
        DiscoveryImportReport report = importService.importBundle(json);
        ImportResponse response = ImportResponse.from(report);
        return report.status() == DiscoveryImportReport.Status.REJECTED
                ? ResponseEntity.badRequest().body(response)
                : ResponseEntity.ok(response);
    }

    @GetMapping("/collections")
    public List<SummaryResponse> collections() {
        return repository.listCollections().stream().map(SummaryResponse::from).toList();
    }

    @GetMapping("/collections/{collectionId}")
    public ResponseEntity<DetailResponse> collection(@PathVariable String collectionId) {
        return repository.findById(collectionId)
                .map(value -> ResponseEntity.ok(DetailResponse.from(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ImportResponse(String status, String collectionId, int candidateCount, int sourceCount, List<String> errors) {
        static ImportResponse from(DiscoveryImportReport report) {
            return new ImportResponse(report.status().name(), report.collectionId().orElse(null),
                    report.candidateCount(), report.sourceCount(), report.errors());
        }
    }

    public record SummaryResponse(
            String collectionId,
            String researchedAt,
            String questionRef,
            String questionText,
            String centerLabel,
            int radiusMeters,
            int candidateCount) {
        static SummaryResponse from(DiscoveryCollectionSummary summary) {
            return new SummaryResponse(summary.collectionId(), summary.researchedAt().toString(), summary.questionRef(),
                    summary.questionText(), summary.centerLabel(), summary.radiusMeters(), summary.candidateCount());
        }
    }

    public record DetailResponse(
            String collectionId,
            String researchedAt,
            QuestionResponse question,
            List<CriterionResponse> criteria,
            List<SourceResponse> sources,
            List<CandidateResponse> candidates) {
        static DetailResponse from(DiscoveryCollection collection) {
            var question = new QuestionResponse(
                    collection.questionRef(),
                    collection.questionText(),
                    collection.centerLabel(),
                    collection.centerCoordinate().map(value -> new CoordinateResponse(value.longitude(), value.latitude())).orElse(null),
                    collection.radiusMeters());
            return new DetailResponse(
                    collection.collectionId(),
                    collection.researchedAt().toString(),
                    question,
                    collection.criteria().stream().map(CriterionResponse::from).toList(),
                    collection.sources().stream().map(SourceResponse::from).toList(),
                    collection.candidates().stream().map(CandidateResponse::from).toList());
        }
    }

    public record QuestionResponse(
            String questionRef,
            String text,
            String centerLabel,
            CoordinateResponse centerCoordinate,
            int radiusMeters) {
    }

    public record CoordinateResponse(double longitude, double latitude) {
    }

    public record CriterionResponse(String criterionRef, String description, String evaluationMode) {
        static CriterionResponse from(DiscoveryCollection.Criterion criterion) {
            return new CriterionResponse(criterion.criterionRef(), criterion.description(), criterion.evaluationMode().name());
        }
    }

    public record SourceResponse(String sourceRef, String url, String title, String retrievedAt) {
        static SourceResponse from(DiscoveryCollection.Source source) {
            return new SourceResponse(source.sourceRef(), source.url(), source.title().orElse(null), source.retrievedAt().toString());
        }
    }

    public record CandidateResponse(
            String candidateRef,
            String displayName,
            IdentityResponse identity,
            LocationResponse researchedLocation,
            List<ClaimResponse> claims) {
        static CandidateResponse from(DiscoveryCollection.Candidate candidate) {
            return new CandidateResponse(
                    candidate.candidateRef(),
                    candidate.displayName(),
                    candidate.identity().map(IdentityResponse::from).orElse(null),
                    LocationResponse.from(candidate.researchedLocation()),
                    candidate.claims().stream().map(ClaimResponse::from).toList());
        }
    }

    public record IdentityResponse(String canonicalUri, List<ExternalIdResponse> externalIds) {
        static IdentityResponse from(DiscoveryCollection.Identity identity) {
            return new IdentityResponse(identity.canonicalUri().orElse(null),
                    identity.externalIds().stream().map(ExternalIdResponse::from).toList());
        }
    }

    public record ExternalIdResponse(String provider, String id) {
        static ExternalIdResponse from(DiscoveryCollection.ExternalId externalId) {
            return new ExternalIdResponse(externalId.provider(), externalId.externalId());
        }
    }

    public record LocationResponse(String label, CoordinateResponse coordinate, List<String> sourceRefs) {
        static LocationResponse from(DiscoveryCollection.ResearchedLocation location) {
            return new LocationResponse(location.label(),
                    location.coordinate().map(value -> new CoordinateResponse(value.longitude(), value.latitude())).orElse(null),
                    location.sourceRefs());
        }
    }

    public record ClaimResponse(
            String criterionRef,
            String status,
            String basis,
            ClaimValueResponse observedValue,
            List<String> sourceRefs,
            String note) {
        static ClaimResponse from(DiscoveryCollection.Claim claim) {
            return new ClaimResponse(claim.criterionRef(), claim.status().name(), claim.basis().name(),
                    claim.observedValue().map(ClaimValueResponse::from).orElse(null), claim.sourceRefs(), claim.note().orElse(null));
        }
    }

    public record ClaimValueResponse(String kind, String value) {
        static ClaimValueResponse from(DiscoveryCollection.ClaimValue value) {
            return new ClaimValueResponse(value.kind().name(), value.value());
        }
    }
}
