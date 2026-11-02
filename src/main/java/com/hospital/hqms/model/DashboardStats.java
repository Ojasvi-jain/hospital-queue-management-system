package com.hospital.hqms.model;

/** Numbers shown on the receptionist dashboard summary cards. */
public record DashboardStats(int totalPatients,
                             int todaysAppointments,
                             int waiting,
                             int completed,
                             double revenueToday) {
}
