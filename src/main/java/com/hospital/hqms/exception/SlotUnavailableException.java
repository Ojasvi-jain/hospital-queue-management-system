package com.hospital.hqms.exception;

/** Thrown when the selected time slot has already been booked by someone else. */
public class SlotUnavailableException extends HospitalException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
