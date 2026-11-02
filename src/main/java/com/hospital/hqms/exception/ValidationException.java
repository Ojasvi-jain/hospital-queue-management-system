package com.hospital.hqms.exception;

/** Thrown when user input or a requested state change is not valid. */
public class ValidationException extends HospitalException {

    public ValidationException(String message) {
        super(message);
    }
}
