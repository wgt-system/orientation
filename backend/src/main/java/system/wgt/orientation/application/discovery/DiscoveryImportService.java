package system.wgt.orientation.application.discovery;

import system.wgt.orientation.application.research.SpatialResearchBundleValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DiscoveryImportService {
    private final SpatialResearchBundleValidator validator;
    private final SpatialResearchBundleShapeValidator shapeValidator = new SpatialResearchBundleShapeValidator();
    private final SpatialResearchBundleTranslator translator;
    private final DiscoveryRepository repository;
    private final ObjectMapper objectMapper;

    public DiscoveryImportService(ObjectMapper objectMapper, DiscoveryRepository repository) {
        this.objectMapper = objectMapper;
        this.validator = new SpatialResearchBundleValidator(objectMapper);
        this.translator = new SpatialResearchBundleTranslator(objectMapper);
        this.repository = repository;
    }

    public DiscoveryImportReport importBundle(String json) {
        var validation = validator.validate(json);
        if (!validation.valid()) {
            return DiscoveryImportReport.rejected(validation.errors());
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception exception) {
            return DiscoveryImportReport.rejected(java.util.List.of("bundle is not valid JSON"));
        }

        var shapeErrors = shapeValidator.validate(root);
        if (!shapeErrors.isEmpty()) {
            return DiscoveryImportReport.rejected(shapeErrors);
        }

        var collection = translator.translate(root);
        var stored = repository.storeIfAbsent(collection);
        var effective = stored.collection();
        return DiscoveryImportReport.stored(
                stored.created(),
                effective.collectionId(),
                effective.candidates().size(),
                effective.sources().size());
    }
}
