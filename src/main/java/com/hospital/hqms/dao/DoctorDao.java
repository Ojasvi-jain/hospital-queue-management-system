package com.hospital.hqms.dao;

import com.hospital.hqms.model.Doctor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class DoctorDao extends BaseDao<Doctor> {

    @Override
    protected Doctor mapRow(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("specialization"),
                rs.getString("dept_code"),
                rs.getString("qualification"),
                rs.getString("room"),
                rs.getDouble("consultation_fee"),
                LocalTime.parse(rs.getString("start_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getInt("slot_minutes"),
                rs.getInt("available") == 1);
    }

    @Override
    public Optional<Doctor> findById(int id) {
        return queryOne("SELECT * FROM doctors WHERE id = ?", id);
    }

    @Override
    public List<Doctor> findAll() {
        return query("SELECT * FROM doctors ORDER BY specialization");
    }

    public void setAvailable(int id, boolean available) {
        update("UPDATE doctors SET available = ? WHERE id = ?", available ? 1 : 0, id);
    }

    public List<Doctor> findAvailable() {
        return query("SELECT * FROM doctors WHERE available = 1 ORDER BY specialization");
    }
}
