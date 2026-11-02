package com.hospital.hqms.dao;

import com.hospital.hqms.exception.DataAccessException;
import com.hospital.hqms.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class PatientDao extends BaseDao<Patient> {

    @Override
    protected Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("blood_group"),
                rs.getString("address"));
    }

    @Override
    public Optional<Patient> findById(int id) {
        return queryOne("SELECT * FROM patients WHERE id = ?", id);
    }

    @Override
    public List<Patient> findAll() {
        return query("SELECT * FROM patients ORDER BY id DESC");
    }

    public Optional<Patient> findByPhone(String phone) {
        return queryOne("SELECT * FROM patients WHERE phone = ?", phone);
    }

    public int count() {
        return (int) queryDouble("SELECT COUNT(*) FROM patients");
    }

    /** Inserts the patient and returns it with the generated id filled in. */
    public Patient insert(Patient p) {
        String sql = "INSERT INTO patients (name, age, gender, phone, email, blood_group, address) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, p.getName(), p.getAge(), p.getGender(), p.getPhone(),
                    p.getEmail(), p.getBloodGroup(), p.getAddress());
            ps.executeUpdate();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                p.setId(rs.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save patient: " + e.getMessage(), e);
        }
    }
}
