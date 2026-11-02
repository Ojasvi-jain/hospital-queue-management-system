package com.hospital.hqms.model;

public enum PaymentStatus {
    PENDING("Pending"),
    PAID("Paid"),
    REFUNDED("Refunded");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
