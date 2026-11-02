package com.hospital.hqms.dao;

import com.hospital.hqms.model.LabelValue;
import com.hospital.hqms.model.LoadSummary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Read-only aggregate (GROUP BY) queries that feed the dashboard charts. */
public class ReportDao extends BaseDao<LabelValue> {

    private static final String LOAD_COLUMNS = """
            COUNT(*) AS total,
            SUM(CASE WHEN a.status = 'WAITING'   THEN 1 ELSE 0 END) AS waiting,
            SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
            """;

    @Override
    protected LabelValue mapRow(ResultSet rs) throws SQLException {
        return new LabelValue(rs.getString(1), rs.getDouble(2));
    }

    private static LoadSummary mapLoad(ResultSet rs) throws SQLException {
        return new LoadSummary(rs.getString(1), rs.getInt("total"), rs.getInt("waiting"), rs.getInt("completed"));
    }

    @Override
    public Optional<LabelValue> findById(int id) {
        return Optional.empty();
    }

    @Override
    public List<LabelValue> findAll() {
        return List.of();
    }

    /** Non-cancelled appointments per day in the range. Days with none are simply absent. */
    public List<LabelValue> appointmentsPerDay(LocalDate from, LocalDate to) {
        return query("SELECT appt_date, COUNT(*) FROM appointments WHERE appt_date BETWEEN ? AND ? "
                + "AND status <> 'CANCELLED' GROUP BY appt_date ORDER BY appt_date", from, to);
    }

    public List<LabelValue> revenuePerDay(LocalDate from, LocalDate to) {
        return query("SELECT date(paid_at), SUM(amount) FROM payments WHERE status = 'SUCCESS' "
                + "AND date(paid_at) BETWEEN ? AND ? GROUP BY date(paid_at) ORDER BY 1", from, to);
    }

    public List<LabelValue> statusBreakdown(LocalDate date) {
        return query("SELECT status, COUNT(*) FROM appointments WHERE appt_date = ? GROUP BY status ORDER BY 2 DESC", date);
    }

    public List<LabelValue> revenueByMethod(LocalDate from, LocalDate to) {
        return query("SELECT method, SUM(amount) FROM payments WHERE status = 'SUCCESS' "
                + "AND date(paid_at) BETWEEN ? AND ? GROUP BY method ORDER BY 2 DESC", from, to);
    }

    public List<LabelValue> revenueByDepartment(LocalDate from, LocalDate to) {
        return query("""
                SELECT d.specialization, SUM(p.amount) FROM payments p
                JOIN appointments a ON a.id = p.appointment_id
                JOIN doctors d ON d.id = a.doctor_id
                WHERE p.status = 'SUCCESS' AND date(p.paid_at) BETWEEN ? AND ?
                GROUP BY d.specialization ORDER BY 2 DESC""", from, to);
    }

    public List<LoadSummary> loadByDepartment(LocalDate date) {
        return query("SELECT d.specialization, " + LOAD_COLUMNS
                + " FROM appointments a JOIN doctors d ON d.id = a.doctor_id "
                + "WHERE a.appt_date = ? AND a.status <> 'CANCELLED' GROUP BY d.specialization ORDER BY total DESC",
                ReportDao::mapLoad, date);
    }

    public List<LoadSummary> loadByDoctor(LocalDate date) {
        return query("SELECT CAST(a.doctor_id AS TEXT), " + LOAD_COLUMNS
                + " FROM appointments a WHERE a.appt_date = ? AND a.status <> 'CANCELLED' GROUP BY a.doctor_id",
                ReportDao::mapLoad, date);
    }

    public int newPatientsSince(LocalDate date) {
        return (int) queryDouble("SELECT COUNT(*) FROM patients WHERE date(created_at) >= ?", date);
    }

    public int countWithStatusBetween(String status, LocalDate from, LocalDate to) {
        return (int) queryDouble("SELECT COUNT(*) FROM appointments WHERE status = ? AND appt_date BETWEEN ? AND ?",
                status, from, to);
    }

    public int countBetween(LocalDate from, LocalDate to) {
        return (int) queryDouble("SELECT COUNT(*) FROM appointments WHERE status <> 'CANCELLED' AND appt_date BETWEEN ? AND ?",
                from, to);
    }
}
