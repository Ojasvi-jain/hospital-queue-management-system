package com.hospital.hqms.db;

import com.hospital.hqms.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central place for JDBC connections (Singleton pattern).
 *
 * The application uses SQLite: the whole database is a single file
 * ({@code data/hospital.db}) created automatically on first launch,
 * so no database server has to be installed.
 */
public final class DatabaseManager {

    private static final Path DB_FILE = Path.of("data", "hospital.db");
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;

    private DatabaseManager() {
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Opens a new connection. Callers close it with try-with-resources.
     * Foreign-key checks are off by default in SQLite, so we switch them on per connection.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /** Creates the tables if needed and loads the sample data on the very first run. */
    public void initialize() {
        try {
            Files.createDirectories(DB_FILE.getParent());
            runScript("/db/schema.sql");
            if (isEmpty()) {
                runScript("/db/data.sql");
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not create the data directory", e);
        }
    }

    /** Drops every table and recreates the database with fresh sample data. */
    public void resetDemoData() {
        List<String> tables = List.of("payments", "appointments", "patients", "doctors", "staff");
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = OFF");
            for (String table : tables) {
                st.execute("DROP TABLE IF EXISTS " + table);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not reset the database", e);
        }
        runScript("/db/schema.sql");
        runScript("/db/data.sql");
    }

    public String getDatabasePath() {
        return DB_FILE.toAbsolutePath().toString();
    }

    private boolean isEmpty() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM doctors")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not inspect the database", e);
        }
    }

    /** Executes a .sql file from the classpath statement by statement inside one transaction. */
    private void runScript(String resource) {
        String sql = readResource(resource);
        // Remove full-line comments, then split into individual statements.
        String cleaned = Arrays.stream(sql.split("\\R"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                for (String statement : cleaned.split(";")) {
                    if (!statement.isBlank()) {
                        st.execute(statement.trim());
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run " + resource + ": " + e.getMessage(), e);
        }
    }

    private String readResource(String resource) {
        try (InputStream in = DatabaseManager.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + resource, e);
        }
    }
}
