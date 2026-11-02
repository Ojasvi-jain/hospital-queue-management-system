package com.hospital.hqms.service.payment;

import com.hospital.hqms.exception.PaymentException;
import com.hospital.hqms.model.PaymentMethod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Strategy interface for the different (simulated) ways a patient can pay.
 * Each implementation validates its own details and "talks" to a fake gateway.
 */
public interface PaymentProcessor {

    PaymentMethod method();

    /** Checks the entered payment details before contacting the gateway. */
    void validate() throws PaymentException;

    /**
     * Simulates authorising the amount with a payment gateway.
     *
     * @return the transaction reference issued by the gateway
     */
    String authorize(double amount) throws PaymentException;

    /** Shared helper: builds a unique-looking transaction reference. */
    default String newTransactionRef() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "TXN-" + method().name() + "-" + stamp + "-" + random;
    }

    /** Pretends to wait for a network round-trip to the bank. */
    default void simulateNetworkDelay() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
