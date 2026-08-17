package system.wgt.orientation.infrastructure.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import system.wgt.orientation.application.discovery.DiscoveryImportReport;
import system.wgt.orientation.application.discovery.DiscoveryImportService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteDiscoveryRepositoryTests {
    @TempDir
    Path tempDirectory;

    @Test
    void importsRoundTripsAndSurvivesRepositoryRestart() throws IOException {
        Path database = tempDirectory.resolve("orientation.db");
        ObjectMapper mapper = new ObjectMapper();
        var repository = new SQLiteDiscoveryRepository(database);
        var importer = new DiscoveryImportService(mapper, repository);

        DiscoveryImportReport report = importer.importBundle(fixture("spatial-research-v1.valid.json"));

        assertEquals(DiscoveryImportReport.Status.CREATED, report.status());
        assertEquals(1, repository.countCollections());
        String collectionId = report.collectionId().orElseThrow();
        var collection = repository.findById(collectionId).orElseThrow();
        assertEquals("hamburg-restaurant-example", collection.questionRef());
        assertEquals(2, collection.criteria().size());
        assertEquals(2, collection.sources().size());
        assertEquals(1, collection.candidates().size());
        assertEquals("source-official", collection.candidates().getFirst().researchedLocation().sourceRefs().getFirst());
        assertEquals("HEURISTIC", collection.candidates().getFirst().claims().getFirst().basis().name());
        assertEquals("Li Wei", collection.candidates().getFirst().claims().getFirst().observedValue().orElseThrow().value());

        var restarted = new SQLiteDiscoveryRepository(database);
        var afterRestart = restarted.findById(collectionId).orElseThrow();
        assertEquals(collection.questionText(), afterRestart.questionText());
        assertEquals(collection.sources(), afterRestart.sources());
        assertEquals(collection.candidates(), afterRestart.candidates());
        assertEquals(1, restarted.listCollections().getFirst().candidateCount());
    }

    @Test
    void semanticReimportIsUnchangedAndRejectedInputDoesNotMutate() throws IOException {
        Path database = tempDirectory.resolve("orientation.db");
        ObjectMapper mapper = new ObjectMapper();
        var repository = new SQLiteDiscoveryRepository(database);
        var importer = new DiscoveryImportService(mapper, repository);
        String canonical = fixture("spatial-research-v1.valid.json");

        DiscoveryImportReport first = importer.importBundle(canonical);
        String compact = mapper.writeValueAsString(mapper.readTree(canonical));
        DiscoveryImportReport second = importer.importBundle(compact);
        DiscoveryImportReport invalidSemantics = importer.importBundle(fixture("spatial-research-v1.invalid-heuristic-basis.json"));
        DiscoveryImportReport unsupportedProperty = importer.importBundle(
                canonical.replaceFirst("\\{", "{\"unsupported\":true,"));

        assertEquals(DiscoveryImportReport.Status.CREATED, first.status());
        assertEquals(DiscoveryImportReport.Status.UNCHANGED, second.status());
        assertEquals(first.collectionId(), second.collectionId());
        assertEquals(DiscoveryImportReport.Status.REJECTED, invalidSemantics.status());
        assertFalse(invalidSemantics.errors().isEmpty());
        assertEquals(DiscoveryImportReport.Status.REJECTED, unsupportedProperty.status());
        assertTrue(unsupportedProperty.errors().stream().anyMatch(error -> error.contains("unsupported property unsupported")));
        assertEquals(1, repository.countCollections());
    }

    @Test
    void canonicalFingerprintIgnoresObjectPropertyOrder() throws IOException {
        Path database = tempDirectory.resolve("orientation.db");
        ObjectMapper mapper = new ObjectMapper();
        var repository = new SQLiteDiscoveryRepository(database);
        var importer = new DiscoveryImportService(mapper, repository);
        String canonical = fixture("spatial-research-v1.valid.json");
        var root = mapper.readTree(canonical);
        String reorderedTopLevel = "{" +
                "\"version\":" + root.get("version") + "," +
                "\"contract\":" + root.get("contract") + "," +
                "\"candidates\":" + root.get("candidates") + "," +
                "\"sources\":" + root.get("sources") + "," +
                "\"question\":" + root.get("question") + "," +
                "\"researchedAt\":" + root.get("researchedAt") + "}";

        var first = importer.importBundle(canonical);
        var second = importer.importBundle(reorderedTopLevel);

        assertTrue(first.collectionId().isPresent());
        assertEquals(DiscoveryImportReport.Status.UNCHANGED, second.status());
        assertEquals(first.collectionId(), second.collectionId());
        assertEquals(1, repository.countCollections());
    }

    private String fixture(String name) throws IOException {
        return Files.readString(Path.of("..", "contracts", "examples", name));
    }
}
