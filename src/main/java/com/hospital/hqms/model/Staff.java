package com.hospital.hqms.model;

/** Logged-in hospital staff member (the receptionist). */
public record Staff(int id, String username, String fullName, String role) {
}
