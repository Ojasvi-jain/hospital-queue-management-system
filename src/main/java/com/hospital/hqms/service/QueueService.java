package com.hospital.hqms.service;

import com.hospital.hqms.dao.AppointmentDao;
import com.hospital.hqms.dao.DoctorDao;
import com.hospital.hqms.dao.PatientDao;
import com.hospital.hqms.dao.PaymentDao;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.DashboardStats;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.QueueStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Manages the live OPD queue for each doctor.
 *
 * The waiting line is modelled with a {@link PriorityQueue} ordered by
 * {@link Appointment#QUEUE_ORDER}: emergencies first, then senior citizens,
 * then normal patients by token number.
 */
public class QueueService {

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final PatientDao patientDao = new PatientDao();
    private final PaymentDao paymentDao = new PaymentDao();

    /** Builds the priority queue of waiting patients for a doctor on a given day. */
    private PriorityQueue<Appointment> buildQueue(int doctorId, LocalDate date) {
        PriorityQueue<Appointment> queue = new PriorityQueue<>(Appointment.QUEUE_ORDER);
        for (Appointment a : appointmentDao.findByDoctorAndDate(doctorId, date)) {
            if (a.getStatus() == AppointmentStatus.WAITING) {
                queue.offer(a);
            }
        }
        return queue;
    }

    /** Waiting patients in the exact order they will be called. */
    public List<Appointment> getWaitingList(int doctorId, LocalDate date) {
        PriorityQueue<Appointment> queue = buildQueue(doctorId, date);
        List<Appointment> ordered = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    public Optional<Appointment> getNowServing(int doctorId, LocalDate date) {
        return appointmentDao.findByDoctorAndDate(doctorId, date).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.IN_CONSULTATION)
                .findFirst();
    }

    /**
     * Finishes the current consultation (if any) and calls the next patient
     * from the head of the priority queue.
     *
     * @return the patient now with the doctor, or empty if nobody is waiting
     */
    public Optional<Appointment> callNext(int doctorId, LocalDate date) {
        getNowServing(doctorId, date).ifPresent(current ->
                appointmentDao.updateStatus(current.getId(), AppointmentStatus.COMPLETED));

        Appointment next = buildQueue(doctorId, date).poll();
        if (next == null) {
            return Optional.empty();
        }
        appointmentDao.updateStatus(next.getId(), AppointmentStatus.IN_CONSULTATION);
        next.setStatus(AppointmentStatus.IN_CONSULTATION);
        return Optional.of(next);
    }

    /** Where a particular appointment currently stands in its doctor's queue. */
    public QueueStatus getQueueStatus(Appointment appointment) {
        Appointment fresh = appointmentDao.findById(appointment.getId()).orElse(appointment);
        String nowServing = getNowServing(fresh.getDoctorId(), fresh.getDate())
                .map(Appointment::getTokenLabel).orElse(null);

        int position = 0;
        int waitMins = 0;
        if (fresh.getStatus() == AppointmentStatus.WAITING) {
            List<Appointment> waiting = getWaitingList(fresh.getDoctorId(), fresh.getDate());
            for (int i = 0; i < waiting.size(); i++) {
                if (waiting.get(i).getId() == fresh.getId()) {
                    position = i + 1;
                    break;
                }
            }
            int slotMinutes = doctorDao.findById(fresh.getDoctorId()).map(Doctor::getSlotMinutes).orElse(15);
            waitMins = (position - 1 + (nowServing != null ? 1 : 0)) * slotMinutes;
        }
        return new QueueStatus(fresh, position, nowServing, waitMins);
    }

    /** Number of waiting patients per doctor - uses streams and groupingBy. */
    public Map<String, Long> waitingCountByDoctor(LocalDate date) {
        return appointmentDao.findByDate(date).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.WAITING)
                .collect(Collectors.groupingBy(Appointment::getDoctorName, TreeMap::new, Collectors.counting()));
    }

    public DashboardStats getDashboardStats(LocalDate date) {
        return new DashboardStats(
                patientDao.count(),
                appointmentDao.countByDate(date),
                appointmentDao.countByDateAndStatus(date, AppointmentStatus.WAITING),
                appointmentDao.countByDateAndStatus(date, AppointmentStatus.COMPLETED),
                paymentDao.totalCollectedOn(date));
    }

    public List<Appointment> getAppointmentsFor(int doctorId, LocalDate date) {
        return appointmentDao.findByDoctorAndDate(doctorId, date);
    }
}
