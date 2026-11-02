package com.hospital.hqms.model;

/** One labelled number from a GROUP BY query, e.g. ("UPI", 12400.0). Used for charts. */
public record LabelValue(String label, double value) {
}
