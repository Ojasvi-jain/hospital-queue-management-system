package com.hospital.hqms.ui.reception;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/** Searchable, filterable list of every appointment, with status updates and CSV export. */
class AppointmentsPage extends VBox {

    private static final String ALL = "All statuses";

    private final AppointmentService appointmentService = new AppointmentService();
    private final ReceptionPortalView portal;

    private final ObservableList<Appointment> all = FXCollections.observableArrayList();
    private final FilteredList<Appointment> filtered = new FilteredList<>(all);
    private final TableView<Appointment> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<Object> statusFilter = new ComboBox<>();
    private final ComboBox<Object> doctorFilter = new ComboBox<>();
    private final DatePicker dateFilter = new DatePicker(LocalDate.now());
    private final CheckBox allDates = new CheckBox("All dates");
    private final Label resultCount = new Label();

    AppointmentsPage(ReceptionPortalView portal) {
        super(20);
        this.portal = portal;

        search.setPromptText("Search patient, phone, token or doctor");
        search.setPrefWidth(260);
        StackPane searchBox = new StackPane(search, Icons.of(Icons.SEARCH, 16));
        searchBox.getStyleClass().add("search-box");
        StackPane.setAlignment(searchBox.getChildren().get(1), javafx.geometry.Pos.CENTER_LEFT);

        statusFilter.getItems().add(ALL);
        statusFilter.getItems().addAll(AppointmentStatus.values());
        statusFilter.setValue(ALL);
        statusFilter.setPrefWidth(165);
        doctorFilter.getItems().add("All doctors");
        doctorFilter.getItems().addAll(appointmentService.getAllDoctors());
        doctorFilter.setValue("All doctors");
        doctorFilter.setPrefWidth(230);
        dateFilter.setPrefWidth(150);
        doctorFilter.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item instanceof Doctor d ? d.getName() : "All doctors");
            }
        });

        search.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        doctorFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        dateFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        allDates.selectedProperty().addListener((obs, o, n) -> {
            dateFilter.setDisable(n);
            applyFilter();
        });

        Button export = Fx.iconButton("Export CSV", Icons.DOWNLOAD, "btn-secondary");
        export.setOnAction(e -> exportCsv());
        HBox filters = Fx.row(10, searchBox, statusFilter, doctorFilter, dateFilter, allDates);
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        VBox filterCard = Fx.card(Fx.cardHeader("Filters", "Narrow down by patient, status, doctor or date", export), filters);

        table.getColumns().add(Fx.column("ID", Appointment::getDisplayId, 80));
        table.getColumns().add(Fx.column("Token", Appointment::getTokenLabel, 85));
        table.getColumns().add(Fx.column("Date", Appointment::getDate, 95));
        table.getColumns().add(Fx.column("Time", Appointment::getTimeSlot, 60));
        table.getColumns().add(Fx.column("Patient", Appointment::getPatientName, 150));
        table.getColumns().add(Fx.column("Doctor", Appointment::getDoctorName, 160));
        table.getColumns().add(Fx.badgeColumn("Priority", Appointment::getPriority, 120));
        table.getColumns().add(Fx.badgeColumn("Status", Appointment::getStatus, 130));
        table.getColumns().add(Fx.badgeColumn("Payment", Appointment::getPaymentStatus, 95));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        SortedList<Appointment> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPrefHeight(520);
        table.setPlaceholder(Fx.emptyState(Icons.SEARCH, "No matching appointments", "Try clearing a filter or ticking \"All dates\"."));

        ComboBox<AppointmentStatus> newStatus = new ComboBox<>();
        newStatus.setPromptText("Select a row");
        newStatus.setPrefWidth(190);
        Button update = Fx.primary("Update Status");
        update.setOnAction(e -> {
            Appointment a = table.getSelectionModel().getSelectedItem();
            try {
                if (a == null) {
                    throw new ValidationException("Select an appointment in the table first.");
                }
                if (newStatus.getValue() == null) {
                    throw new ValidationException("Choose the new status first.");
                }
                appointmentService.changeStatus(a, newStatus.getValue());
                Fx.toast(this, a.getTokenLabel() + " updated to " + a.getStatus(), true);
                reload();
                portal.refreshBadges();
            } catch (ValidationException ex) {
                Fx.toast(this, ex.getMessage(), false);
            }
        });
        Button collect = Fx.iconButton("Collect Fee", Icons.CARD, "btn-secondary");
        collect.setOnAction(e -> {
            Appointment a = table.getSelectionModel().getSelectedItem();
            if (a != null) {
                portal.openPayment(a);
            }
        });
        // Only the statuses the selected appointment may legally move to are offered.
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, a) -> {
            newStatus.getItems().setAll(a == null ? List.of() : a.getStatus().allowedNext());
            newStatus.setPromptText(a == null ? "Select a row" : newStatus.getItems().isEmpty() ? "Final status" : "New status");
        });

        resultCount.getStyleClass().add("count-pill");
        VBox tableCard = Fx.card(
                Fx.cardHeader("Appointments", "Click a column header to sort", resultCount),
                table,
                Fx.row(10, Fx.muted("Selected appointment:"), newStatus, update, Fx.spacer(), collect));
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(filterCard, tableCard);
        reload();
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private void reload() {
        all.setAll(appointmentService.findAll());
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        Object status = statusFilter.getValue();
        Object doctor = doctorFilter.getValue();
        LocalDate date = allDates.isSelected() ? null : dateFilter.getValue();
        filtered.setPredicate(a ->
                (q.isEmpty()
                        || a.getPatientName().toLowerCase().contains(q)
                        || a.getPatientPhone().contains(q)
                        || a.getTokenLabel().toLowerCase().contains(q)
                        || a.getDoctorName().toLowerCase().contains(q))
                && (!(status instanceof AppointmentStatus s) || a.getStatus() == s)
                && (!(doctor instanceof Doctor d) || a.getDoctorId() == d.getId())
                && (date == null || a.getDate().equals(date)));
        resultCount.setText(filtered.size() + " of " + all.size());
    }

    /** Writes the currently visible rows to a CSV file chosen by the user. */
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export appointments");
        chooser.setInitialFileName("appointments-" + LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("ID,Token,Date,Time,Patient,Phone,Doctor,Department,Priority,Status,Payment,Fee");
            w.newLine();
            for (Appointment a : table.getItems()) {
                w.write(String.join(",", a.getDisplayId(), a.getTokenLabel(), a.getDate().toString(),
                        a.getTimeSlot().toString(), csv(a.getPatientName()), a.getPatientPhone(), csv(a.getDoctorName()),
                        csv(a.getSpecialization()), a.getPriority().name(), a.getStatus().name(),
                        a.getPaymentStatus().name(), String.valueOf((int) a.getFee())));
                w.newLine();
            }
            Fx.toast(this, "Exported " + table.getItems().size() + " rows to " + file.getName(), true);
        } catch (IOException ex) {
            Fx.error("Export failed", ex.getMessage());
        }
    }

    private static String csv(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
