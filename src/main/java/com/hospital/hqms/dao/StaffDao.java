package com.hospital.hqms.dao;

import com.hospital.hqms.model.Staff;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StaffDao extends BaseDao<Staff> {

    @Override
    protected Staff mapRow(ResultSet rs) throws SQLException {
        return new Staff(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"));
    }

    @Override
    public Optional<Staff> findById(int id) {
        return queryOne("SELECT * FROM staff WHERE id = ?", id);
    }

    @Override
    public List<Staff> findAll() {
        return query("SELECT * FROM staff");
    }

    public Optional<Staff> findByCredentials(String username, String passwordHash) {
        return queryOne("SELECT * FROM staff WHERE username = ? AND password_hash = ?", username, passwordHash);
    }
}
