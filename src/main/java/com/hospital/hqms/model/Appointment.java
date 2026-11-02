package com.hospital.hqms.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

public class Appointment {

    /**
     * Order in which patients are called: emergencies first, then senior citizens,
     * then everyone else - and within the same priority, by token number.
     */
    public static final Comparator<Appointment> QUEUE_ORDER =
            Comparator.comparingInt((Appointment a) -> a.getPriority().getRank())
                      .thenComparingInt(Appointment::getTokenNo);

    private int id;
    private int patientId;
    private int doctorId;
    private LocalDate date;
    private LocalTime timeSlot;
    private int tokenNo;
    private Priority priority = Priority.NORMAL;
    private AppointmentStatus status = AppointmentStatus.WAITING;
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private String symptoms;

    // Read-only details joined from the patients / doctors tables for display.
    private String patientName;
    private String patientPhone;
    private String doctorName;
    private String specialization;
    private String deptCode;
    private String room;
    private double fee;

    /** Token shown to the patient, e.g. "CAR-004". */
    public String getTokenLabel() {
        return String.format("%s-%03d", deptCode == null ? "TKN" : deptCode, tokenNo);
    }

    public String getDisplayId() {
        return String.format("A%05d", id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(LocalTime timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(int tokenNo) {
        this.tokenNo = tokenNo;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    @Override
    public String toString() {
        return getTokenLabel() + "  |  " + date + " " + timeSlot + "  |  " + doctorName
                + "  |  ₹" + (int) fee + " (" + paymentStatus + ")";
    }
}
