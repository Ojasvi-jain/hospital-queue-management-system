package com.hospital.hqms.ui.reception;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.DashboardStats;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.LabelValue;
import com.hospital.hqms.model.LoadSummary;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.service.ReportService;
import com.hospital.hqms.ui.components.Charts;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Landing page of the reception portal: KPIs, charts and a live summary of every queue. */
class DashboardPage extends VBox {

    private final QueueService queueService = new QueueService();
    private final ReportService reportService = new ReportService();
    private final AppointmentService appointmentService = new AppointmentService();

    DashboardPage(ReceptionPortalView portal) {
        super(20);
        LocalDate today = LocalDate.now();
        DashboardStats s = queueService.getDashboardStats(today);
        int yesterdayAppts = reportService.appointmentsOn(today.minusDays(1));
        double yesterdayRevenue = reportService.revenueOn(today.minusDays(1));
        int activeDoctors = appointmentService.getAvailableDoctors().size();

        HBox kpis = new HBox(16,
                Fx.kpi("Registered patients", new Label(String.valueOf(s.totalPatients())), Icons.PEOPLE, "accent-blue",
                        trend("+" + reportService.newPatientsThisWeek() + " new this week", true)),
                Fx.kpi("Appointments today", new Label(String.valueOf(s.todaysAppointments())), Icons.CALENDAR, "accent-purple",
                        compare(s.todaysAppointments(), yesterdayAppts)),
                Fx.kpi("Waiting now", new Label(String.valueOf(s.waiting())), Icons.CLOCK, "accent-amber",
                        neutral("across " + activeDoctors + " doctors on duty")),
                Fx.kpi("Consultations done", new Label(String.valueOf(s.completed())), Icons.CHECK, "accent-green",
                        neutral(percent(s.todaysAppointments() == 0 ? 0 : (double) s.completed() / s.todaysAppointments()) + " of today's load")),
                Fx.kpi("Collected today", new Label(Charts.rupees(s.revenueToday())), Icons.RUPEE, "accent-teal",
                        compare(s.revenueToday(), yesterdayRevenue)));

        // ---- charts row
        VBox trendCard = Fx.card(
                Fx.cardHeader("Patient footfall", "Appointments per day over the last 14 days"),
                Charts.areaTrend(reportService.appointmentsTrend(14), "Appointments", false));
        List<LabelValue> statuses = reportService.statusBreakdown(today);
        VBox statusCard = Fx.card(
                Fx.cardHeader("Today's appointments", "By current status"),
                Charts.donut(statuses.stream()
                        .map(lv -> new LabelValue(AppointmentStatus.valueOf(lv.label()).toString(), lv.value()))
                        .toList(), "total", false));
        GridPane row2 = Charts.grid(new double[]{64, 36}, trendCard, statusCard);

        // ---- department load + revenue
        VBox deptList = new VBox(14);
        List<LoadSummary> loads = reportService.loadByDepartment(today);
        for (LoadSummary l : loads) {
            deptList.getChildren().add(loadRow(l));
        }
        if (loads.isEmpty()) {
            deptList.getChildren().add(Fx.emptyState(Icons.CALENDAR, "No appointments today", "Bookings will appear here."));
        }
        VBox deptCard = Fx.card(Fx.cardHeader("Department load", "Progress through today's patients"), deptList);
        VBox revenueCard = Fx.card(
                Fx.cardHeader("Revenue", "Consultation fees collected, last 7 days"),
                Charts.barTrend(reportService.revenueTrend(7), true));
        GridPane row3 = Charts.grid(new double[]{50, 50}, deptCard, revenueCard);

        // ---- live queues summary
        Map<Integer, LoadSummary> byDoctor = reportService.loadByDoctor(today);
        GridPane queues = new GridPane();
        queues.getStyleClass().add("summary-grid");
        queues.setHgap(18);
        queues.setVgap(10);
        String[] headers = {"Doctor", "Room", "Now serving", "Waiting", "Done", "Progress", ""};
        for (int c = 0; c < headers.length; c++) {
            Label h = new Label(headers[c].toUpperCase());
            h.getStyleClass().add("grid-header");
            queues.add(h, c, 0);
        }
        int r = 1;
        for (Doctor d : appointmentService.getAvailableDoctors()) {
            LoadSummary l = byDoctor.getOrDefault(d.getId(), LoadSummary.EMPTY);
            String serving = queueService.getNowServing(d.getId(), today).map(Appointment::getTokenLabel).orElse("—");
            Label name = new Label(d.getName());
            name.getStyleClass().add("cell-strong");
            Label spec = Fx.muted(d.getSpecialization());
            HBox who = new HBox(10, Fx.avatar(d.getName(), 32), new VBox(0, name, spec));
            who.setAlignment(Pos.CENTER_LEFT);
            Label token = new Label(serving);
            token.getStyleClass().add("token-chip");
            ProgressBar pb = new ProgressBar(l.progress());
            pb.setPrefWidth(140);
            Button manage = Fx.iconButton("Manage", Icons.ARROW_RIGHT, "btn-link");
            manage.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            manage.setOnAction(e -> portal.openQueue(d.getId()));
            queues.addRow(r++, who, Fx.muted(d.getRoom()), token, new Label(String.valueOf(l.waiting())),
                    new Label(String.valueOf(l.completed())), pb, manage);
        }
        Button openQueue = Fx.iconButton("Open Live Queue", Icons.QUEUE, "btn-secondary");
        openQueue.setOnAction(e -> portal.navigate("queue"));
        VBox queueCard = Fx.card(Fx.cardHeader("Live queues", "Every doctor on duty today", openQueue), queues);

        getChildren().addAll(kpis, row2, row3, queueCard);
    }

    private static HBox loadRow(LoadSummary l) {
        Label dept = new Label(l.key());
        dept.getStyleClass().add("cell-strong");
        Label nums = Fx.muted(l.completed() + " done  •  " + l.waiting() + " waiting  •  " + l.total() + " total");
        ProgressBar pb = new ProgressBar(l.progress());
        pb.setMaxWidth(Double.MAX_VALUE);
        VBox text = new VBox(6, Fx.row(8, dept, Fx.spacer(), nums), pb);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
        return new HBox(text);
    }

    private static Label trend(String text, boolean positive) {
        Label l = new Label(text);
        l.getStyleClass().add(positive ? "trend-up" : "trend-down");
        return l;
    }

    private static Label neutral(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("trend-neutral");
        return l;
    }

    private static Label compare(double today, double yesterday) {
        if (yesterday == 0) {
            return neutral("no data for yesterday");
        }
        double change = (today - yesterday) / yesterday;
        String arrow = change >= 0 ? "+" : "-";
        return trend(arrow + percent(Math.abs(change)) + " vs yesterday", change >= 0);
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }
}
