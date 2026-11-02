package com.hospital.hqms.model;

/**
 * How busy a doctor or department is on a given day.
 *
 * @param key       doctor id or department name
 * @param total     appointments booked (excluding cancellations)
 * @param waiting   patients still waiting
 * @param completed consultations finished
 */
public record LoadSummary(String key, int total, int waiting, int completed) {

    public static final LoadSummary EMPTY = new LoadSummary("", 0, 0, 0);

    /** Share of the day's patients already seen, from 0.0 to 1.0. */
    public double progress() {
        return total == 0 ? 0 : (double) completed / total;
    }
}
