package system.wgt.orientation.infrastructure.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class SQLiteDiscoveryDatabase {
    private static final int SCHEMA_VERSION = 1;
    private static final String MIGRATION_RESOURCE = "/db/migration/V001__discovery_collections.sql";

    private final String jdbcUrl;

    SQLiteDiscoveryDatabase(Path databasePath) {
        if (databasePath == null) {
            throw new IllegalArgumentException("databasePath is required.");
        }
        Path absolute = databasePath.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to create Orientation database directory", exception);
            }
        }
        this.jdbcUrl = "jdbc:sqlite:" + absolute;
        migrate();
    }

    Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void migrate() {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("""
                            CREATE TABLE IF NOT EXISTS orientation_schema_migrations (
                                version INTEGER PRIMARY KEY,
                                applied_at TEXT NOT NULL
                            )
                            """);
                }

                int currentVersion = currentVersion(connection);
                if (currentVersion > SCHEMA_VERSION) {
                    throw new IllegalStateException("Orientation database schema is newer than this runtime supports: " + currentVersion);
                }
                if (currentVersion < 1) {
                    applySql(connection, readMigration(MIGRATION_RESOURCE));
                    try (var statement = connection.prepareStatement(
                            "INSERT INTO orientation_schema_migrations(version, applied_at) VALUES (?, ?)")) {
                        statement.setInt(1, 1);
                        statement.setString(2, OffsetDateTime.now(ZoneOffset.UTC).toString());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("Unable to migrate Orientation discovery database", exception);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM orientation_schema_migrations")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void applySql(Connection connection, String sql) throws SQLException {
        for (String fragment : sql.split(";")) {
            String statementSql = fragment.trim();
            if (!statementSql.isEmpty()) {
                try (var statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    private String readMigration(String resource) {
        try (InputStream input = SQLiteDiscoveryDatabase.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing migration resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read migration resource " + resource, exception);
        }
    }
}
