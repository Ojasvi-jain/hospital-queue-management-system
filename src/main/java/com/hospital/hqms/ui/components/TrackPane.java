package com.hospital.hqms.ui.components;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.QueueStatus;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PatientService;
import com.hospital.hqms.service.QueueService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Lets a patient look up their token and see how many people are ahead of them. */
public class TrackPane extends HBox {

    private final PatientService patientService = new PatientService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final QueueService queueService = new QueueService();

    private final TextField phone = new TextField();
    private final Label message = new Label();
    private final TableView<Appointment> table = new TableView<>();
    private final VBox statusPanel = new VBox(16);

    public TrackPane() {
        setSpacing(20);

        phone.setPromptText("Registered mobile number");
        Button find = Fx.primary("Show my appointments");
        find.setOnAction(e -> load());
        phone.setOnAction(e -> load());
        HBox.setHgrow(phone, Priority.ALWAYS);

        table.getColumns().add(Fx.column("Token", Appointment::getTokenLabel, 90));
        table.getColumns().add(Fx.column("Date", Appointment::getDate, 95));
        table.getColumns().add(Fx.column("Time", Appointment::getTimeSlot, 60));
        table.getColumns().add(Fx.column("Doctor", Appointment::getDoctorName, 140));
        table.getColumns().add(Fx.badgeColumn("Status", Appointment::getStatus, 120));
        table.getColumns().add(Fx.badgeColumn("Payment", Appointment::getPaymentStatus, 90));
        table.setPlaceholder(Fx.muted("Enter your mobile number to see your appointments."));
        table.setPrefHeight(330);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> showStatus(n));
        message.setWrapText(true);

        VBox left = Fx.card(
                Fx.sectionTitle("Track Your Token"),
                Fx.muted("Select an appointment to see the live queue position."),
                Fx.row(8, phone, find), message, table);
        left.setPrefWidth(620);
        left.setMinWidth(500);

        statusPanel.setPrefWidth(340);
        statusPanel.setMinWidth(300);
        statusPanel.getChildren().add(Fx.card(Fx.sectionTitle("Queue Status"), Fx.muted("No appointment selected.")));
        getChildren().addAll(left, statusPanel);
    }

    private void load() {
        try {
            Patient p = patientService.findByPhone(phone.getText());
            List<Appointment> list = appointmentService.findByPatient(p);
            table.getItems().setAll(list);
            message.setText(list.isEmpty() ? "No appointments found for " + p.getName() + "." : "Hello, " + p.getName() + "!");
            message.getStyleClass().setAll("label", "msg-success");
            // Pre-select today's active appointment if there is one.
            list.stream()
                    .filter(a -> a.getDate().equals(LocalDate.now()) && a.getStatus().isActive())
                    .min(Comparator.comparing(Appointment::getTimeSlot))
                    .ifPresent(a -> table.getSelectionModel().select(a));
        } catch (ValidationException ex) {
            table.getItems().clear();
            message.setText(ex.getMessage());
            message.getStyleClass().setAll("label", "msg-error");
        }
    }

    private void showStatus(Appointment a) {
        if (a == null) {
            return;
        }
        QueueStatus qs = queueService.getQueueStatus(a);
        Appointment fresh = qs.appointment();

        Label token = new Label(fresh.getTokenLabel());
        token.getStyleClass().add("token-big");
        VBox tokenBox = new VBox(4, Fx.muted("Your token"), token, Fx.badge(fresh.getStatus().toString()));
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("token-box");

        GridPane facts = Fx.formGrid();
        int row = 0;
        Fx.addRow(facts, row++, "Doctor", new Label(fresh.getDoctorName() + " (" + fresh.getRoom() + ")"));
        Fx.addRow(facts, row++, "Now serving", new Label(qs.nowServingToken() == null ? "-" : qs.nowServingToken()));

        String headline;
        if (fresh.getStatus() == AppointmentStatus.WAITING) {
            Fx.addRow(facts, row++, "Queue position", new Label("#" + qs.position()));
            Fx.addRow(facts, row++, "People ahead", new Label(String.valueOf(qs.peopleAhead())));
            Fx.addRow(facts, row, "Estimated wait", new Label("~" + qs.estimatedWaitMins() + " min"));
            headline = qs.peopleAhead() == 0 ? "You are next! Please be near " + fresh.getRoom() + "."
                    : "Please wait, you will be called soon.";
        } else if (fresh.getStatus() == AppointmentStatus.IN_CONSULTATION) {
            headline = "It's your turn - please go to " + fresh.getRoom() + ".";
        } else {
            headline = "This appointment is " + fresh.getStatus().toString().toLowerCase() + ".";
        }
        Label head = new Label(headline);
        head.setWrapText(true);
        head.getStyleClass().add("headline");

        Button refresh = Fx.secondary("Refresh");
        refresh.setOnAction(e -> showStatus(fresh));
        statusPanel.getChildren().setAll(Fx.card(Fx.sectionTitle("Queue Status"), tokenBox, head, facts, refresh));
    }
}
