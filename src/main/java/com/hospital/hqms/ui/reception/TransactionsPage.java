package com.hospital.hqms.ui.reception;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.LabelValue;
import com.hospital.hqms.model.Payment;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PaymentService;
import com.hospital.hqms.service.ReportService;
import com.hospital.hqms.ui.components.Charts;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Revenue analytics and the full list of payments received. */
class TransactionsPage extends VBox {

    private static final DateTimeFormatter PAID_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    TransactionsPage() {
        super(20);
        ReportService reports = new ReportService();
        List<Payment> payments = new PaymentService().findAll();
        Map<Integer, Appointment> appointments = new AppointmentService().findAll().stream()
                .collect(Collectors.toMap(Appointment::getId, Function.identity()));

        LocalDate today = LocalDate.now();
        double week = reports.revenueTrend(7).values().stream().mapToDouble(Double::doubleValue).sum();
        long todayCount = payments.stream().filter(p -> p.paidAt().toLocalDate().equals(today)).count();
        double avg = payments.stream().mapToDouble(Payment::amount).average().orElse(0);

        HBox kpis = new HBox(16,
                Fx.kpi("Collected today", new Label(Charts.rupees(reports.revenueOn(today))), Icons.RUPEE, "accent-teal", null),
                Fx.kpi("Last 7 days", new Label(Charts.rupees(week)), Icons.CALENDAR, "accent-blue", null),
                Fx.kpi("Transactions today", new Label(String.valueOf(todayCount)), Icons.RECEIPT, "accent-purple", null),
                Fx.kpi("Average ticket", new Label(Charts.rupees(avg)), Icons.CARD, "accent-amber", null));

        List<LabelValue> byMethod = reports.revenueByMethod(7).stream()
                .map(lv -> new LabelValue(lv.label().equals("CARD") ? "Card" : lv.label().equals("CASH") ? "Cash" : "UPI", lv.value()))
                .toList();
        VBox methodCard = Fx.card(Fx.cardHeader("Payment methods", "Share of revenue, last 7 days"),
                Charts.donut(byMethod, "collected", true));
        VBox deptCard = Fx.card(Fx.cardHeader("Revenue by department", "Last 7 days"),
                Charts.bars(reports.revenueByDepartment(7), true));
        GridPane charts = Charts.grid(new double[]{36, 64}, methodCard, deptCard);

        // ---- table
        TableView<Payment> table = new TableView<>();
        table.getColumns().add(Fx.column("Transaction ID", Payment::transactionRef, 190));
        table.getColumns().add(Fx.column("Paid on", p -> p.paidAt().format(PAID_FMT), 185));
        table.getColumns().add(Fx.column("Patient", p -> name(appointments.get(p.appointmentId()), Appointment::getPatientName), 160));
        table.getColumns().add(Fx.column("Doctor", p -> name(appointments.get(p.appointmentId()), Appointment::getDoctorName), 160));
        table.getColumns().add(Fx.column("Token", p -> name(appointments.get(p.appointmentId()), Appointment::getTokenLabel), 95));
        table.getColumns().add(Fx.column("Method", Payment::method, 140));
        table.getColumns().add(Fx.column("Amount", p -> Charts.rupees(p.amount()), 90));
        table.getColumns().add(Fx.badgeColumn("Status", p -> p.status().charAt(0) + p.status().substring(1).toLowerCase(), 105));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        FilteredList<Payment> filtered = new FilteredList<>(FXCollections.observableArrayList(payments));
        SortedList<Payment> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPrefHeight(460);

        Label count = new Label(payments.size() + " payments");
        count.getStyleClass().add("count-pill");
        TextField search = new TextField();
        search.setPromptText("Search transaction, patient or doctor");
        search.setPrefWidth(320);
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.trim().toLowerCase();
            filtered.setPredicate(p -> {
                Appointment a = appointments.get(p.appointmentId());
                return q.isEmpty() || p.transactionRef().toLowerCase().contains(q)
                        || (a != null && (a.getPatientName().toLowerCase().contains(q) || a.getDoctorName().toLowerCase().contains(q)));
            });
            count.setText(filtered.size() + " payments");
        });
        StackPane searchBox = new StackPane(search, Icons.of(Icons.SEARCH, 16));
        searchBox.getStyleClass().add("search-box");
        StackPane.setAlignment(searchBox.getChildren().get(1), Pos.CENTER_LEFT);

        VBox tableCard = Fx.card(Fx.cardHeader("All transactions", "Simulated gateway payments and cash collected at the desk",
                searchBox, count), table);

        getChildren().addAll(kpis, charts, tableCard);
    }

    private static String name(Appointment a, Function<Appointment, String> getter) {
        return a == null ? "-" : getter.apply(a);
    }
}
