package system.wgt.orientation.infrastructure.discovery;

import system.wgt.orientation.application.discovery.DiscoveryRepository;
import system.wgt.orientation.domain.discovery.DiscoveryCollection;
import system.wgt.orientation.domain.discovery.DiscoveryCollectionSummary;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.research.ResearchEvaluationMode;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SQLiteDiscoveryRepository implements DiscoveryRepository {
    private final SQLiteDiscoveryDatabase database;

    public SQLiteDiscoveryRepository(Path databasePath) {
        this.database = new SQLiteDiscoveryDatabase(databasePath);
    }

    @Override
    public StoreResult storeIfAbsent(DiscoveryCollection collection) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                int inserted = insertCollection(connection, collection);
                if (inserted == 0) {
                    DiscoveryCollection existing = findByFingerprint(connection, collection.importFingerprint())
                            .orElseThrow(() -> new IllegalStateException("fingerprint conflict without existing collection"));
                    connection.commit();
                    return new StoreResult(false, existing);
                }

                insertCriteria(connection, collection);
                insertSources(connection, collection);
                insertCandidates(connection, collection);
                connection.commit();
                return new StoreResult(true, collection);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("Unable to persist Orientation discovery collection", exception);
        }
    }

    @Override
    public List<DiscoveryCollectionSummary> listCollections() {
        String sql = """
                SELECT c.collection_id, c.researched_at, c.question_ref, c.question_text,
                       c.center_label, c.radius_meters, COUNT(d.candidate_ref) AS candidate_count
                FROM discovery_collections c
                LEFT JOIN discovery_candidates d ON d.collection_id = c.collection_id
                GROUP BY c.collection_id
                ORDER BY c.researched_at DESC, c.collection_id ASC
                """;
        try (Connection connection = database.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            List<DiscoveryCollectionSummary> summaries = new ArrayList<>();
            while (result.next()) {
                summaries.add(new DiscoveryCollectionSummary(
                        result.getString("collection_id"),
                        OffsetDateTime.parse(result.getString("researched_at")),
                        result.getString("question_ref"),
                        result.getString("question_text"),
                        result.getString("center_label"),
                        result.getInt("radius_meters"),
                        result.getInt("candidate_count")));
            }
            return summaries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to list Orientation discovery collections", exception);
        }
    }

    @Override
    public Optional<DiscoveryCollection> findById(String collectionId) {
        if (collectionId == null || collectionId.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = database.open()) {
            return findById(connection, collectionId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read Orientation discovery collection", exception);
        }
    }

    @Override
    public long countCollections() {
        try (Connection connection = database.open();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM discovery_collections");
             var result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to count Orientation discovery collections", exception);
        }
    }

    private int insertCollection(Connection connection, DiscoveryCollection collection) throws SQLException {
        String sql = """
                INSERT INTO discovery_collections(
                    collection_id, import_fingerprint, researched_at, question_ref, question_text,
                    center_label, center_longitude, center_latitude, radius_meters, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(import_fingerprint) DO NOTHING
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collection.collectionId());
            statement.setString(2, collection.importFingerprint());
            statement.setString(3, collection.researchedAt().toString());
            statement.setString(4, collection.questionRef());
            statement.setString(5, collection.questionText());
            statement.setString(6, collection.centerLabel());
            setCoordinate(statement, 7, 8, collection.centerCoordinate());
            statement.setInt(9, collection.radiusMeters());
            statement.setString(10, OffsetDateTime.now(ZoneOffset.UTC).toString());
            return statement.executeUpdate();
        }
    }

    private void insertCriteria(Connection connection, DiscoveryCollection collection) throws SQLException {
        String sql = """
                INSERT INTO discovery_criteria(collection_id, ordinal, criterion_ref, description, evaluation_mode)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (var criterion : collection.criteria()) {
                statement.setString(1, collection.collectionId());
                statement.setInt(2, ordinal++);
                statement.setString(3, criterion.criterionRef());
                statement.setString(4, criterion.description());
                statement.setString(5, criterion.evaluationMode().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertSources(Connection connection, DiscoveryCollection collection) throws SQLException {
        String sql = """
                INSERT INTO research_sources(collection_id, ordinal, source_ref, url, title, retrieved_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (var source : collection.sources()) {
                statement.setString(1, collection.collectionId());
                statement.setInt(2, ordinal++);
                statement.setString(3, source.sourceRef());
                statement.setString(4, source.url());
                setNullableString(statement, 5, source.title());
                statement.setString(6, source.retrievedAt().toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertCandidates(Connection connection, DiscoveryCollection collection) throws SQLException {
        String candidateSql = """
                INSERT INTO discovery_candidates(
                    collection_id, ordinal, candidate_ref, display_name, canonical_uri,
                    researched_location_label, researched_longitude, researched_latitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var candidateStatement = connection.prepareStatement(candidateSql)) {
            int ordinal = 0;
            for (var candidate : collection.candidates()) {
                candidateStatement.setString(1, collection.collectionId());
                candidateStatement.setInt(2, ordinal++);
                candidateStatement.setString(3, candidate.candidateRef());
                candidateStatement.setString(4, candidate.displayName());
                setNullableString(candidateStatement, 5, candidate.identity().flatMap(DiscoveryCollection.Identity::canonicalUri));
                candidateStatement.setString(6, candidate.researchedLocation().label());
                setCoordinate(candidateStatement, 7, 8, candidate.researchedLocation().coordinate());
                candidateStatement.executeUpdate();

                insertExternalIds(connection, collection.collectionId(), candidate);
                insertLocationSources(connection, collection.collectionId(), candidate);
                insertClaims(connection, collection.collectionId(), candidate);
            }
        }
    }

    private void insertExternalIds(Connection connection, String collectionId,
                                   DiscoveryCollection.Candidate candidate) throws SQLException {
        if (candidate.identity().isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO candidate_external_ids(collection_id, candidate_ref, ordinal, provider, external_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (var externalId : candidate.identity().orElseThrow().externalIds()) {
                statement.setString(1, collectionId);
                statement.setString(2, candidate.candidateRef());
                statement.setInt(3, ordinal++);
                statement.setString(4, externalId.provider());
                statement.setString(5, externalId.externalId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertLocationSources(Connection connection, String collectionId,
                                       DiscoveryCollection.Candidate candidate) throws SQLException {
        String sql = """
                INSERT INTO candidate_location_sources(collection_id, candidate_ref, ordinal, source_ref)
                VALUES (?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (String sourceRef : candidate.researchedLocation().sourceRefs()) {
                statement.setString(1, collectionId);
                statement.setString(2, candidate.candidateRef());
                statement.setInt(3, ordinal++);
                statement.setString(4, sourceRef);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertClaims(Connection connection, String collectionId,
                              DiscoveryCollection.Candidate candidate) throws SQLException {
        String sql = """
                INSERT INTO candidate_claims(
                    collection_id, candidate_ref, ordinal, criterion_ref, status, basis,
                    observed_value_kind, observed_value, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (var claim : candidate.claims()) {
                statement.setString(1, collectionId);
                statement.setString(2, candidate.candidateRef());
                statement.setInt(3, ordinal++);
                statement.setString(4, claim.criterionRef());
                statement.setString(5, claim.status().name());
                statement.setString(6, claim.basis().name());
                if (claim.observedValue().isPresent()) {
                    statement.setString(7, claim.observedValue().orElseThrow().kind().name());
                    statement.setString(8, claim.observedValue().orElseThrow().value());
                } else {
                    statement.setObject(7, null);
                    statement.setObject(8, null);
                }
                setNullableString(statement, 9, claim.note());
                statement.executeUpdate();
                insertClaimSources(connection, collectionId, candidate.candidateRef(), claim);
            }
        }
    }

    private void insertClaimSources(Connection connection, String collectionId, String candidateRef,
                                    DiscoveryCollection.Claim claim) throws SQLException {
        String sql = """
                INSERT INTO claim_sources(collection_id, candidate_ref, criterion_ref, ordinal, source_ref)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int ordinal = 0;
            for (String sourceRef : claim.sourceRefs()) {
                statement.setString(1, collectionId);
                statement.setString(2, candidateRef);
                statement.setString(3, claim.criterionRef());
                statement.setInt(4, ordinal++);
                statement.setString(5, sourceRef);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Optional<DiscoveryCollection> findByFingerprint(Connection connection, String fingerprint) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT collection_id FROM discovery_collections WHERE import_fingerprint = ?")) {
            statement.setString(1, fingerprint);
            try (var result = statement.executeQuery()) {
                return result.next() ? findById(connection, result.getString(1)) : Optional.empty();
            }
        }
    }

    private Optional<DiscoveryCollection> findById(Connection connection, String collectionId) throws SQLException {
        String sql = """
                SELECT collection_id, import_fingerprint, researched_at, question_ref, question_text,
                       center_label, center_longitude, center_latitude, radius_meters
                FROM discovery_collections WHERE collection_id = ?
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new DiscoveryCollection(
                        result.getString("collection_id"),
                        result.getString("import_fingerprint"),
                        OffsetDateTime.parse(result.getString("researched_at")),
                        result.getString("question_ref"),
                        result.getString("question_text"),
                        result.getString("center_label"),
                        coordinate(result, "center_longitude", "center_latitude"),
                        result.getInt("radius_meters"),
                        criteria(connection, collectionId),
                        sources(connection, collectionId),
                        candidates(connection, collectionId))));
            }
        }
    }

    private List<DiscoveryCollection.Criterion> criteria(Connection connection, String collectionId) throws SQLException {
        String sql = """
                SELECT criterion_ref, description, evaluation_mode
                FROM discovery_criteria WHERE collection_id = ? ORDER BY ordinal
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            try (var result = statement.executeQuery()) {
                List<DiscoveryCollection.Criterion> values = new ArrayList<>();
                while (result.next()) {
                    values.add(new DiscoveryCollection.Criterion(
                            result.getString("criterion_ref"),
                            result.getString("description"),
                            ResearchEvaluationMode.valueOf(result.getString("evaluation_mode"))));
                }
                return values;
            }
        }
    }

    private List<DiscoveryCollection.Source> sources(Connection connection, String collectionId) throws SQLException {
        String sql = """
                SELECT source_ref, url, title, retrieved_at
                FROM research_sources WHERE collection_id = ? ORDER BY ordinal
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            try (var result = statement.executeQuery()) {
                List<DiscoveryCollection.Source> values = new ArrayList<>();
                while (result.next()) {
                    values.add(new DiscoveryCollection.Source(
                            result.getString("source_ref"),
                            result.getString("url"),
                            Optional.ofNullable(result.getString("title")),
                            OffsetDateTime.parse(result.getString("retrieved_at"))));
                }
                return values;
            }
        }
    }

    private List<DiscoveryCollection.Candidate> candidates(Connection connection, String collectionId) throws SQLException {
        String sql = """
                SELECT candidate_ref, display_name, canonical_uri, researched_location_label,
                       researched_longitude, researched_latitude
                FROM discovery_candidates WHERE collection_id = ? ORDER BY ordinal
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            try (var result = statement.executeQuery()) {
                List<DiscoveryCollection.Candidate> values = new ArrayList<>();
                while (result.next()) {
                    String candidateRef = result.getString("candidate_ref");
                    var externalIds = externalIds(connection, collectionId, candidateRef);
                    Optional<String> canonicalUri = Optional.ofNullable(result.getString("canonical_uri"));
                    Optional<DiscoveryCollection.Identity> identity = canonicalUri.isPresent() || !externalIds.isEmpty()
                            ? Optional.of(new DiscoveryCollection.Identity(canonicalUri, externalIds))
                            : Optional.empty();
                    var location = new DiscoveryCollection.ResearchedLocation(
                            result.getString("researched_location_label"),
                            coordinate(result, "researched_longitude", "researched_latitude"),
                            locationSources(connection, collectionId, candidateRef));
                    values.add(new DiscoveryCollection.Candidate(
                            candidateRef,
                            result.getString("display_name"),
                            identity,
                            location,
                            claims(connection, collectionId, candidateRef)));
                }
                return values;
            }
        }
    }

    private List<DiscoveryCollection.ExternalId> externalIds(Connection connection, String collectionId,
                                                             String candidateRef) throws SQLException {
        String sql = """
                SELECT provider, external_id FROM candidate_external_ids
                WHERE collection_id = ? AND candidate_ref = ? ORDER BY ordinal
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            statement.setString(2, candidateRef);
            try (var result = statement.executeQuery()) {
                List<DiscoveryCollection.ExternalId> values = new ArrayList<>();
                while (result.next()) {
                    values.add(new DiscoveryCollection.ExternalId(result.getString("provider"), result.getString("external_id")));
                }
                return values;
            }
        }
    }

    private List<String> locationSources(Connection connection, String collectionId, String candidateRef) throws SQLException {
        String sql = """
                SELECT source_ref FROM candidate_location_sources
                WHERE collection_id = ? AND candidate_ref = ? ORDER BY ordinal
                """;
        return sourceRefs(connection, sql, collectionId, candidateRef, null);
    }

    private List<DiscoveryCollection.Claim> claims(Connection connection, String collectionId,
                                                   String candidateRef) throws SQLException {
        String sql = """
                SELECT criterion_ref, status, basis, observed_value_kind, observed_value, note
                FROM candidate_claims
                WHERE collection_id = ? AND candidate_ref = ?
                ORDER BY ordinal
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            statement.setString(2, candidateRef);
            try (var result = statement.executeQuery()) {
                List<DiscoveryCollection.Claim> values = new ArrayList<>();
                while (result.next()) {
                    String criterionRef = result.getString("criterion_ref");
                    String valueKind = result.getString("observed_value_kind");
                    Optional<DiscoveryCollection.ClaimValue> observedValue = valueKind == null
                            ? Optional.empty()
                            : Optional.of(new DiscoveryCollection.ClaimValue(
                                    DiscoveryCollection.ValueKind.valueOf(valueKind), result.getString("observed_value")));
                    values.add(new DiscoveryCollection.Claim(
                            criterionRef,
                            DiscoveryCollection.ClaimStatus.valueOf(result.getString("status")),
                            DiscoveryCollection.ClaimBasis.valueOf(result.getString("basis")),
                            observedValue,
                            claimSources(connection, collectionId, candidateRef, criterionRef),
                            Optional.ofNullable(result.getString("note"))));
                }
                return values;
            }
        }
    }

    private List<String> claimSources(Connection connection, String collectionId, String candidateRef,
                                      String criterionRef) throws SQLException {
        String sql = """
                SELECT source_ref FROM claim_sources
                WHERE collection_id = ? AND candidate_ref = ? AND criterion_ref = ? ORDER BY ordinal
                """;
        return sourceRefs(connection, sql, collectionId, candidateRef, criterionRef);
    }

    private List<String> sourceRefs(Connection connection, String sql, String collectionId,
                                    String candidateRef, String criterionRef) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, collectionId);
            statement.setString(2, candidateRef);
            if (criterionRef != null) {
                statement.setString(3, criterionRef);
            }
            try (var result = statement.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (result.next()) {
                    values.add(result.getString("source_ref"));
                }
                return values;
            }
        }
    }

    private Optional<Coordinate> coordinate(ResultSet result, String longitudeColumn, String latitudeColumn) throws SQLException {
        Object longitude = result.getObject(longitudeColumn);
        Object latitude = result.getObject(latitudeColumn);
        if (longitude == null || latitude == null) {
            return Optional.empty();
        }
        return Optional.of(new Coordinate(((Number) longitude).doubleValue(), ((Number) latitude).doubleValue()));
    }

    private void setCoordinate(java.sql.PreparedStatement statement, int longitudeIndex, int latitudeIndex,
                               Optional<Coordinate> coordinate) throws SQLException {
        if (coordinate.isPresent()) {
            statement.setDouble(longitudeIndex, coordinate.orElseThrow().longitude());
            statement.setDouble(latitudeIndex, coordinate.orElseThrow().latitude());
        } else {
            statement.setObject(longitudeIndex, null);
            statement.setObject(latitudeIndex, null);
        }
    }

    private void setNullableString(java.sql.PreparedStatement statement, int index,
                                   Optional<String> value) throws SQLException {
        if (value.isPresent()) {
            statement.setString(index, value.orElseThrow());
        } else {
            statement.setObject(index, null);
        }
    }
}
