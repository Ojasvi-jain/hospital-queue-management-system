package com.hospital.hqms.exception;

/** Thrown when a (simulated) payment is rejected or cannot be processed. */
public class PaymentException extends HospitalException {

    public PaymentException(String message) {
        super(message);
    }
}
