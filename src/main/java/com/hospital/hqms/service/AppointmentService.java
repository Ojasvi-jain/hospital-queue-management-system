package com.hospital.hqms.service;

import com.hospital.hqms.dao.AppointmentDao;
import com.hospital.hqms.dao.DoctorDao;
import com.hospital.hqms.exception.SlotUnavailableException;
import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.PaymentStatus;
import com.hospital.hqms.model.Priority;
import com.hospital.hqms.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class AppointmentService {

    public static final int MAX_DAYS_AHEAD = 30;

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final DoctorDao doctorDao = new DoctorDao();

    public List<Doctor> getAvailableDoctors() {
        return doctorDao.findAvailable();
    }

    /** Every doctor, including those currently on leave (for the doctor directory). */
    public List<Doctor> getAllDoctors() {
        return doctorDao.findAll();
    }

    public void setDoctorAvailable(Doctor doctor, boolean available) {
        doctorDao.setAvailable(doctor.getId(), available);
    }

    /** All of the doctor's slots on that day minus the ones already booked. */
    public List<LocalTime> getFreeSlots(Doctor doctor, LocalDate date) {
        Set<LocalTime> booked = appointmentDao.findBookedSlots(doctor.getId(), date);
        return doctor.generateSlots().stream()
                .filter(slot -> !booked.contains(slot))
                .toList();
    }

    public Appointment book(Patient patient, Doctor doctor, LocalDate date, LocalTime slot,
                            Priority priority, String symptoms) throws ValidationException, SlotUnavailableException {
        if (patient == null) {
            throw new ValidationException("Find or register the patient first.");
        }
        doctor = Validator.requireSelected(doctor, "a doctor");
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new ValidationException("Appointment date cannot be in the past.");
        }
        if (date.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new ValidationException("Appointments can be booked at most " + MAX_DAYS_AHEAD + " days ahead.");
        }
        if (slot == null) {
            throw new ValidationException("Please select a time slot.");
        }

        final int doctorId = doctor.getId();
        boolean duplicate = appointmentDao.findByPatient(patient.getId()).stream()
                .anyMatch(a -> a.getDoctorId() == doctorId && a.getDate().equals(date) && a.getStatus().isActive());
        if (duplicate) {
            throw new ValidationException(patient.getName() + " already has an active appointment with "
                    + doctor.getName() + " on " + date + ".");
        }

        // Senior citizens are automatically moved up unless it is already an emergency.
        Priority effective = (priority == null) ? Priority.NORMAL : priority;
        if (effective == Priority.NORMAL && patient.isSenior()) {
            effective = Priority.SENIOR;
        }

        Appointment a = new Appointment();
        a.setPatientId(patient.getId());
        a.setDoctorId(doctor.getId());
        a.setDate(date);
        a.setTimeSlot(slot);
        a.setPriority(effective);
        a.setSymptoms(symptoms == null || symptoms.isBlank() ? null : symptoms.trim());
        return appointmentDao.book(a);
    }

    /** Applies a status change after checking it is a legal transition. */
    public void changeStatus(Appointment a, AppointmentStatus next) throws ValidationException {
        if (a.getStatus() == next) {
            return;
        }
        if (!a.getStatus().canMoveTo(next)) {
            throw new ValidationException("Cannot change " + a.getTokenLabel() + " from "
                    + a.getStatus() + " to " + next + ".");
        }
        appointmentDao.updateStatus(a.getId(), next);
        if (next == AppointmentStatus.CANCELLED && a.getPaymentStatus() == PaymentStatus.PAID) {
            appointmentDao.updatePaymentStatus(a.getId(), PaymentStatus.REFUNDED);
            a.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        a.setStatus(next);
    }

    public void changePriority(Appointment a, Priority priority) throws ValidationException {
        if (a.getStatus() != AppointmentStatus.WAITING) {
            throw new ValidationException("Priority can only be changed while the patient is waiting.");
        }
        appointmentDao.updatePriority(a.getId(), priority);
        a.setPriority(priority);
    }

    public List<Appointment> findByPatient(Patient p) {
        return appointmentDao.findByPatient(p.getId());
    }

    public List<Appointment> findAll() {
        return appointmentDao.findAll();
    }
}
