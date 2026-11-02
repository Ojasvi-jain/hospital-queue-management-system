package com.hospital.hqms.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life-cycle of an appointment. Each status knows which statuses it may move to,
 * so invalid transitions (e.g. COMPLETED -> WAITING) are rejected centrally.
 */
public enum AppointmentStatus {
    WAITING("Waiting"),
    IN_CONSULTATION("In Consultation"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            case WAITING -> EnumSet.of(IN_CONSULTATION, CANCELLED, NO_SHOW);
            case IN_CONSULTATION -> EnumSet.of(COMPLETED, WAITING);
            case NO_SHOW -> EnumSet.of(WAITING, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }

    public boolean canMoveTo(AppointmentStatus next) {
        return allowedNext().contains(next);
    }

    /** Active appointments still occupy a slot and a place in the queue. */
    public boolean isActive() {
        return this == WAITING || this == IN_CONSULTATION;
    }

    @Override
    public String toString() {
        return label;
    }
}
