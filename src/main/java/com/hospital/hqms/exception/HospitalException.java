package com.hospital.hqms.exception;

/**
 * Base checked exception for all business-rule failures in the application.
 * Sub-classes describe the specific failure so the UI can react appropriately.
 */
public class HospitalException extends Exception {

    public HospitalException(String message) {
        super(message);
    }

    public HospitalException(String message, Throwable cause) {
        super(message, cause);
    }
}
