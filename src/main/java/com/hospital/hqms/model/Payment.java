package com.hospital.hqms.model;

import java.time.LocalDateTime;

/** A completed (or failed) simulated payment, stored as an immutable record. */
public record Payment(int id,
                      int appointmentId,
                      double amount,
                      PaymentMethod method,
                      String transactionRef,
                      String status,
                      LocalDateTime paidAt) {
}
