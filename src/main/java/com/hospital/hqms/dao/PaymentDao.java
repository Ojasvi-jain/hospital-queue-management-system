package com.hospital.hqms.dao;

import com.hospital.hqms.exception.DataAccessException;
import com.hospital.hqms.model.Payment;
import com.hospital.hqms.model.PaymentMethod;
import com.hospital.hqms.model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PaymentDao extends BaseDao<Payment> {

    private static final DateTimeFormatter SQL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected Payment mapRow(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getInt("id"),
                rs.getInt("appointment_id"),
                rs.getDouble("amount"),
                PaymentMethod.valueOf(rs.getString("method")),
                rs.getString("transaction_ref"),
                rs.getString("status"),
                LocalDateTime.parse(rs.getString("paid_at"), SQL_DATETIME));
    }

    @Override
    public Optional<Payment> findById(int id) {
        return queryOne("SELECT * FROM payments WHERE id = ?", id);
    }

    @Override
    public List<Payment> findAll() {
        return query("SELECT * FROM payments ORDER BY paid_at DESC, id DESC");
    }

    public double totalCollectedOn(LocalDate date) {
        return queryDouble("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS' AND date(paid_at) = ?", date);
    }

    /**
     * Stores the payment and marks the appointment as paid atomically,
     * so the two tables can never disagree.
     */
    public Payment recordSuccessfulPayment(int appointmentId, double amount, PaymentMethod method, String txnRef) {
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO payments (appointment_id, amount, method, transaction_ref, status, paid_at) VALUES (?, ?, ?, ?, 'SUCCESS', ?)");
                 PreparedStatement upd = conn.prepareStatement(
                         "UPDATE appointments SET payment_status = ? WHERE id = ?")) {
                LocalDateTime now = LocalDateTime.now().withNano(0);
                bind(ins, appointmentId, amount, method, txnRef, now.format(SQL_DATETIME));
                ins.executeUpdate();
                bind(upd, PaymentStatus.PAID, appointmentId);
                upd.executeUpdate();
                conn.commit();
                return new Payment(0, appointmentId, amount, method, txnRef, "SUCCESS", now);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not record payment: " + e.getMessage(), e);
        }
    }
}
