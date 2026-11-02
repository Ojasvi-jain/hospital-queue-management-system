package com.hospital.hqms.dao;

import com.hospital.hqms.exception.DataAccessException;
import com.hospital.hqms.exception.SlotUnavailableException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.PaymentStatus;
import com.hospital.hqms.model.Priority;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AppointmentDao extends BaseDao<Appointment> {

    /** Appointment joined with the patient and doctor details needed on screen. */
    private static final String SELECT = """
            SELECT a.*, p.name AS patient_name, p.phone AS patient_phone,
                   d.name AS doctor_name, d.specialization, d.dept_code, d.room, d.consultation_fee
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN doctors  d ON d.id = a.doctor_id
            """;

    @Override
    protected Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setDate(LocalDate.parse(rs.getString("appt_date")));
        a.setTimeSlot(LocalTime.parse(rs.getString("time_slot")));
        a.setTokenNo(rs.getInt("token_no"));
        a.setPriority(Priority.valueOf(rs.getString("priority")));
        a.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        a.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        a.setSymptoms(rs.getString("symptoms"));
        a.setPatientName(rs.getString("patient_name"));
        a.setPatientPhone(rs.getString("patient_phone"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setSpecialization(rs.getString("specialization"));
        a.setDeptCode(rs.getString("dept_code"));
        a.setRoom(rs.getString("room"));
        a.setFee(rs.getDouble("consultation_fee"));
        return a;
    }

    @Override
    public Optional<Appointment> findById(int id) {
        return queryOne(SELECT + " WHERE a.id = ?", id);
    }

    @Override
    public List<Appointment> findAll() {
        return query(SELECT + " ORDER BY a.appt_date DESC, a.doctor_id, a.token_no");
    }

    public List<Appointment> findByDate(LocalDate date) {
        return query(SELECT + " WHERE a.appt_date = ? ORDER BY a.doctor_id, a.token_no", date);
    }

    public List<Appointment> findByDoctorAndDate(int doctorId, LocalDate date) {
        return query(SELECT + " WHERE a.doctor_id = ? AND a.appt_date = ? ORDER BY a.token_no", doctorId, date);
    }

    public List<Appointment> findByPatient(int patientId) {
        return query(SELECT + " WHERE a.patient_id = ? ORDER BY a.appt_date DESC, a.time_slot DESC", patientId);
    }

    /** Slots already taken by active (non-cancelled) appointments. */
    public Set<LocalTime> findBookedSlots(int doctorId, LocalDate date) {
        return new HashSet<>(query(
                "SELECT time_slot FROM appointments WHERE doctor_id = ? AND appt_date = ? "
                        + "AND status NOT IN ('CANCELLED', 'NO_SHOW')",
                rs -> LocalTime.parse(rs.getString(1)), doctorId, date));
    }

    /**
     * Books an appointment inside a single transaction: checks the slot is still free,
     * generates the next token number for that doctor and day, and inserts the row.
     */
    public Appointment book(Appointment a) throws SlotUnavailableException {
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appt_date = ? AND time_slot = ? "
                                + "AND status NOT IN ('CANCELLED', 'NO_SHOW')")) {
                    bind(check, a.getDoctorId(), a.getDate(), a.getTimeSlot());
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SlotUnavailableException("The " + a.getTimeSlot() + " slot was just booked. Please pick another.");
                        }
                    }
                }

                int token;
                try (PreparedStatement next = conn.prepareStatement(
                        "SELECT COALESCE(MAX(token_no), 0) + 1 FROM appointments WHERE doctor_id = ? AND appt_date = ?")) {
                    bind(next, a.getDoctorId(), a.getDate());
                    try (ResultSet rs = next.executeQuery()) {
                        rs.next();
                        token = rs.getInt(1);
                    }
                }

                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO appointments (patient_id, doctor_id, appt_date, time_slot, token_no, priority, status, payment_status, symptoms) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bind(ins, a.getPatientId(), a.getDoctorId(), a.getDate(), a.getTimeSlot(), token,
                            a.getPriority(), AppointmentStatus.WAITING, PaymentStatus.PENDING, a.getSymptoms());
                    ins.executeUpdate();
                }

                int id;
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    rs.next();
                    id = rs.getInt(1);
                }
                conn.commit();
                return findById(id).orElseThrow();
            } catch (SQLException | SlotUnavailableException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Booking failed: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int id, AppointmentStatus status) {
        update("UPDATE appointments SET status = ? WHERE id = ?", status, id);
    }

    public void updatePaymentStatus(int id, PaymentStatus status) {
        update("UPDATE appointments SET payment_status = ? WHERE id = ?", status, id);
    }

    public void updatePriority(int id, Priority priority) {
        update("UPDATE appointments SET priority = ? WHERE id = ?", priority, id);
    }

    public int countByDate(LocalDate date) {
        return (int) queryDouble("SELECT COUNT(*) FROM appointments WHERE appt_date = ?", date);
    }

    public int countByDateAndStatus(LocalDate date, AppointmentStatus status) {
        return (int) queryDouble("SELECT COUNT(*) FROM appointments WHERE appt_date = ? AND status = ?", date, status);
    }
}
