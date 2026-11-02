package com.hospital.hqms.model;

/**
 * Snapshot of where a patient currently stands in their doctor's queue.
 *
 * @param appointment        the patient's appointment
 * @param position           1-based place in the waiting line (0 if not waiting)
 * @param nowServingToken    token currently with the doctor, or null
 * @param estimatedWaitMins  rough estimate based on the doctor's slot length
 */
public record QueueStatus(Appointment appointment,
                          int position,
                          String nowServingToken,
                          int estimatedWaitMins) {

    public int peopleAhead() {
        return Math.max(0, position - 1);
    }
}
