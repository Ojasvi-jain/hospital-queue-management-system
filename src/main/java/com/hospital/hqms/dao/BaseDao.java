package com.hospital.hqms.dao;

import com.hospital.hqms.db.DatabaseManager;
import com.hospital.hqms.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing for all DAOs. Sub-classes only describe
 * how a {@link ResultSet} row is turned into an object ({@link #mapRow}).
 */
public abstract class BaseDao<T> implements Dao<T> {

    /** Functional interface that lets lambdas throw SQLException. */
    @FunctionalInterface
    protected interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    protected final DatabaseManager db = DatabaseManager.getInstance();

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected Connection connection() throws SQLException {
        return db.getConnection();
    }

    protected List<T> query(String sql, Object... params) {
        return query(sql, this::mapRow, params);
    }

    protected <R> List<R> query(String sql, RowMapper<R> mapper, Object... params) {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<R> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + e.getMessage(), e);
        }
    }

    protected Optional<T> queryOne(String sql, Object... params) {
        return query(sql, params).stream().findFirst();
    }

    protected double queryDouble(String sql, Object... params) {
        return query(sql, rs -> rs.getDouble(1), params).stream().findFirst().orElse(0.0);
    }

    protected int update(String sql, Object... params) {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + e.getMessage(), e);
        }
    }

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            // Dates, times and enums are stored as text (enums by constant name, not display label).
            if (p instanceof Enum<?> e) {
                ps.setString(i + 1, e.name());
            } else if (p == null || p instanceof Number || p instanceof String) {
                ps.setObject(i + 1, p);
            } else {
                ps.setString(i + 1, p.toString());
            }
        }
    }
}
