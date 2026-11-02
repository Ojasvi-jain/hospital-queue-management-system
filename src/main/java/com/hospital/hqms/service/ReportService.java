package com.hospital.hqms.service;

import com.hospital.hqms.dao.PaymentDao;
import com.hospital.hqms.dao.ReportDao;
import com.hospital.hqms.model.LabelValue;
import com.hospital.hqms.model.LoadSummary;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns raw aggregate rows into chart-ready series for the dashboard. */
public class ReportService {

    private final ReportDao reportDao = new ReportDao();
    private final PaymentDao paymentDao = new PaymentDao();

    /** Appointments per day for the last {@code days} days, including days with zero. */
    public Map<LocalDate, Double> appointmentsTrend(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        return fillDays(from, to, reportDao.appointmentsPerDay(from, to));
    }

    public Map<LocalDate, Double> revenueTrend(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        return fillDays(from, to, reportDao.revenuePerDay(from, to));
    }

    /** Ordered map with one entry per day, so the chart has no gaps (LinkedHashMap keeps insertion order). */
    private static Map<LocalDate, Double> fillDays(LocalDate from, LocalDate to, List<LabelValue> rows) {
        Map<String, Double> byDay = rows.stream().collect(Collectors.toMap(LabelValue::label, LabelValue::value));
        Map<LocalDate, Double> result = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.put(d, byDay.getOrDefault(d.toString(), 0.0));
        }
        return result;
    }

    public List<LabelValue> statusBreakdown(LocalDate date) {
        return reportDao.statusBreakdown(date);
    }

    public List<LabelValue> revenueByMethod(int days) {
        return reportDao.revenueByMethod(LocalDate.now().minusDays(days - 1L), LocalDate.now());
    }

    public List<LabelValue> revenueByDepartment(int days) {
        return reportDao.revenueByDepartment(LocalDate.now().minusDays(days - 1L), LocalDate.now());
    }

    public List<LoadSummary> loadByDepartment(LocalDate date) {
        return reportDao.loadByDepartment(date);
    }

    /** Today's load per doctor id, for the doctor directory and the dashboard. */
    public Map<Integer, LoadSummary> loadByDoctor(LocalDate date) {
        return reportDao.loadByDoctor(date).stream()
                .collect(Collectors.toMap(l -> Integer.parseInt(l.key()), Function.identity()));
    }

    public int newPatientsThisWeek() {
        return reportDao.newPatientsSince(LocalDate.now().minusDays(6));
    }

    public int appointmentsOn(LocalDate date) {
        return reportDao.countBetween(date, date);
    }

    public double revenueOn(LocalDate date) {
        return paymentDao.totalCollectedOn(date);
    }

    /** Share of last week's appointments where the patient never turned up. */
    public double noShowRate(int days) {
        LocalDate to = LocalDate.now().minusDays(1);
        LocalDate from = to.minusDays(days - 1L);
        int total = reportDao.countBetween(from, to);
        return total == 0 ? 0 : (double) reportDao.countWithStatusBetween("NO_SHOW", from, to) / total;
    }
}
