package com.hospital.hqms.model;

public enum PaymentMethod {
    CARD("Credit / Debit Card"),
    UPI("UPI"),
    CASH("Cash at Counter");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
