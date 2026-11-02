package com.hospital.hqms.service;

import com.hospital.hqms.dao.AppointmentDao;
import com.hospital.hqms.dao.PaymentDao;
import com.hospital.hqms.exception.PaymentException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Payment;
import com.hospital.hqms.model.PaymentStatus;
import com.hospital.hqms.service.payment.PaymentProcessor;

import java.util.List;

/**
 * Coordinates a payment. It does not care <em>how</em> the patient pays:
 * any {@link PaymentProcessor} implementation can be passed in (polymorphism).
 */
public class PaymentService {

    private final PaymentDao paymentDao = new PaymentDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    /**
     * Runs validation, the simulated gateway call, and records the result.
     * Slow (simulated network delay) - call it from a background thread.
     */
    public Payment pay(Appointment appointment, PaymentProcessor processor) throws PaymentException {
        Appointment fresh = appointmentDao.findById(appointment.getId())
                .orElseThrow(() -> new PaymentException("Appointment no longer exists."));
        if (fresh.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Appointment " + fresh.getTokenLabel() + " is already paid.");
        }
        if (fresh.getStatus() == AppointmentStatus.CANCELLED) {
            throw new PaymentException("Cannot pay for a cancelled appointment.");
        }

        processor.validate();
        String txnRef = processor.authorize(fresh.getFee());
        Payment payment = paymentDao.recordSuccessfulPayment(fresh.getId(), fresh.getFee(), processor.method(), txnRef);
        appointment.setPaymentStatus(PaymentStatus.PAID);
        return payment;
    }

    public List<Payment> findAll() {
        return paymentDao.findAll();
    }
}
