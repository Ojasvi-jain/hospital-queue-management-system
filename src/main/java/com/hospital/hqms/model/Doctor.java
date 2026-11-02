package com.hospital.hqms.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Doctor extends Person {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private String specialization;
    private String deptCode;
    private String qualification;
    private String room;
    private double consultationFee;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotMinutes;
    private boolean available;

    public Doctor() {
    }

    public Doctor(int id, String name, String specialization, String deptCode, String qualification,
                  String room, double consultationFee, LocalTime startTime, LocalTime endTime,
                  int slotMinutes, boolean available) {
        super(id, name, null);
        this.specialization = specialization;
        this.deptCode = deptCode;
        this.qualification = qualification;
        this.room = room;
        this.consultationFee = consultationFee;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
        this.available = available;
    }

    @Override
    public String getDisplayId() {
        return String.format("D%04d", getId());
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    /** Every bookable time slot between the doctor's start and end time. */
    public List<LocalTime> generateSlots() {
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = startTime; t.plusMinutes(slotMinutes).compareTo(endTime) <= 0; t = t.plusMinutes(slotMinutes)) {
            slots.add(t);
        }
        return slots;
    }

    public String getTimingLabel() {
        return startTime.format(TIME_FMT) + " - " + endTime.format(TIME_FMT);
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public String getQualification() {
        return qualification;
    }

    public String getRoom() {
        return room;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public boolean isAvailable() {
        return available;
    }

    /** Used by ComboBoxes to render the doctor. */
    @Override
    public String toString() {
        return getName() + "  |  " + specialization + "  |  ₹" + (int) consultationFee;
    }
}
