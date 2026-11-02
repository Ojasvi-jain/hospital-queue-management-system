package com.hospital.hqms.model;

/**
 * Queue priority. A lower {@code rank} is served first.
 * Enums can carry fields and behaviour just like classes.
 */
public enum Priority {
    EMERGENCY(1, "Emergency"),
    SENIOR(2, "Senior Citizen"),
    NORMAL(3, "Normal");

    private final int rank;
    private final String label;

    Priority(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return label;
    }
}
