package com.hospital.hqms.ui.reception;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.LoadSummary;
import com.hospital.hqms.model.Priority;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.service.ReportService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** Master-detail queue control: pick a doctor on the left, run their queue on the right. */
class LiveQueuePage extends HBox {

    private final QueueService queueService = new QueueService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final ReportService reportService = new ReportService();
    private final ReceptionPortalView portal;

    private final ListView<Doctor> doctors = new ListView<>();
    private final DatePicker date = new DatePicker(LocalDate.now());
    private final VBox servingCard = new VBox();
    private final TableView<Appointment> table = new TableView<>();
    private final Label waitingCount = new Label();
    private final ComboBox<Priority> priority = new ComboBox<>();
    private Map<Integer, LoadSummary> loads = Map.of();

    LiveQueuePage(ReceptionPortalView portal, int initialDoctorId) {
        super(20);
        this.portal = portal;

        // ---- doctor list (left)
        doctors.getStyleClass().add("doctor-list");
        doctors.setPrefWidth(290);
        doctors.setMinWidth(270);
        doctors.setPrefHeight(640);
        doctors.setCellFactory(lv -> new DoctorCell());
        doctors.getItems().setAll(appointmentService.getAvailableDoctors());
        doctors.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                portal.rememberQueueDoctor(n.getId());
                refresh();
            }
        });
        date.valueProperty().addListener((obs, o, n) -> refresh());
        date.setPrefWidth(150);
        VBox left = Fx.card(Fx.cardHeader("Doctors on duty", "Waiting patients per doctor"), date, doctors);
        left.setMinWidth(300);
        left.setMaxWidth(320);

        // ---- waiting table (right)
        table.getColumns().add(Fx.column("#", a -> table.getItems().indexOf(a) + 1, 40));
        table.getColumns().add(Fx.column("Token", Appointment::getTokenLabel, 90));
        table.getColumns().add(Fx.column("Patient", Appointment::getPatientName, 160));
        table.getColumns().add(Fx.badgeColumn("Priority", Appointment::getPriority, 125));
        table.getColumns().add(Fx.column("Slot", Appointment::getTimeSlot, 65));
        table.getColumns().add(Fx.badgeColumn("Payment", Appointment::getPaymentStatus, 95));
        table.getColumns().add(Fx.column("Reason for visit", Appointment::getSymptoms, 210));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Fx.emptyState(Icons.CHECK, "Queue is clear", "Nobody is waiting for this doctor."));
        table.setPrefHeight(390);

        priority.getItems().addAll(Priority.values());
        priority.setPromptText("Priority");
        Button setPriority = Fx.secondary("Set Priority");
        setPriority.setOnAction(e -> withSelected(a -> {
            if (priority.getValue() == null) {
                throw new ValidationException("Choose a priority first.");
            }
            appointmentService.changePriority(a, priority.getValue());
            return a.getTokenLabel() + " is now " + priority.getValue();
        }));
        Button collect = Fx.iconButton("Collect Fee", Icons.CARD, "btn-secondary");
        collect.setOnAction(e -> {
            Appointment a = table.getSelectionModel().getSelectedItem();
            if (a == null) {
                Fx.toast(this, "Select a patient in the waiting list first.", false);
            } else {
                portal.openPayment(a);
            }
        });
        Button noShow = Fx.secondary("Mark No-Show");
        noShow.setOnAction(e -> withSelected(a -> {
            appointmentService.changeStatus(a, AppointmentStatus.NO_SHOW);
            return a.getTokenLabel() + " marked as no-show";
        }));
        Button cancel = Fx.danger("Cancel");
        cancel.setOnAction(e -> withSelected(a -> {
            if (!Fx.confirm("Cancel " + a.getTokenLabel() + "?", "Any fee already paid will be marked as refunded.")) {
                return null;
            }
            appointmentService.changeStatus(a, AppointmentStatus.CANCELLED);
            return a.getTokenLabel() + " cancelled";
        }));

        waitingCount.getStyleClass().add("count-pill");
        VBox waitingCard = Fx.card(
                Fx.cardHeader("Waiting list", "Emergency → Senior citizen → Normal, then by token number", waitingCount),
                table,
                Fx.row(8, priority, setPriority, Fx.spacer(), collect, noShow, cancel));

        VBox right = new VBox(20, servingCard, waitingCard);
        HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);
        getChildren().addAll(left, right);

        Doctor initial = doctors.getItems().stream().filter(d -> d.getId() == initialDoctorId).findFirst()
                .orElseGet(this::busiestDoctor);
        if (initial != null) {
            doctors.getSelectionModel().select(initial);
            doctors.scrollTo(initial);
        }
    }

    private Doctor busiestDoctor() {
        Map<Integer, LoadSummary> today = reportService.loadByDoctor(LocalDate.now());
        return doctors.getItems().stream()
                .max(java.util.Comparator.comparingInt(d -> today.getOrDefault(d.getId(), LoadSummary.EMPTY).waiting()))
                .orElse(null);
    }

    private void refresh() {
        Doctor d = doctors.getSelectionModel().getSelectedItem();
        LocalDate day = date.getValue();
        loads = reportService.loadByDoctor(day);
        doctors.refresh();
        if (d == null || day == null) {
            return;
        }
        table.getItems().setAll(queueService.getWaitingList(d.getId(), day));
        waitingCount.setText(table.getItems().size() + " waiting");
        buildServingCard(d, day);
        portal.refreshBadges();
    }

    private void buildServingCard(Doctor d, LocalDate day) {
        Optional<Appointment> current = queueService.getNowServing(d.getId(), day);
        LoadSummary load = loads.getOrDefault(d.getId(), LoadSummary.EMPTY);

        Label tokenCaption = new Label("NOW SERVING");
        tokenCaption.getStyleClass().add("serving-caption");
        Label token = new Label(current.map(Appointment::getTokenLabel).orElse("—"));
        token.getStyleClass().add("serving-token");
        token.setMinWidth(Region.USE_PREF_SIZE);
        VBox tokenBox = new VBox(2, tokenCaption, token);
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("serving-token-box");

        VBox info = new VBox(6);
        if (current.isPresent()) {
            Appointment a = current.get();
            Label patient = new Label(a.getPatientName());
            patient.getStyleClass().add("serving-patient");
            info.getChildren().addAll(
                    patient,
                    Fx.row(8, Fx.badge(a.getPriority().toString()), Fx.badge(a.getPaymentStatus().toString()),
                            Fx.muted("Slot " + a.getTimeSlot() + "  •  " + a.getPatientPhone())),
                    Fx.muted(a.getSymptoms() == null ? "" : "Reason: " + a.getSymptoms()));
        } else {
            Label idle = new Label("No patient with the doctor");
            idle.getStyleClass().add("serving-patient");
            info.getChildren().addAll(idle, Fx.muted("Press \"Call Next Patient\" to bring in the first person in the queue."));
        }
        Label meta = Fx.muted(d.getSpecialization() + "  •  " + d.getRoom()
                + "  •  " + load.completed() + " of " + load.total() + " seen today");
        info.getChildren().add(0, meta);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Button callNext = Fx.iconButton("Call Next Patient", Icons.ARROW_RIGHT, "btn-primary");
        callNext.setMaxWidth(Double.MAX_VALUE);
        callNext.setDisable(table.getItems().isEmpty() && current.isEmpty());
        callNext.setOnAction(e -> {
            Optional<Appointment> next = queueService.callNext(d.getId(), day);
            refresh();
            Fx.toast(this, next.map(a -> "Calling " + a.getTokenLabel() + " - " + a.getPatientName() + " to " + d.getRoom())
                    .orElse("Consultation completed. The queue is now empty."), true);
        });
        Button complete = Fx.iconButton("Mark Completed", Icons.CHECK, "btn-secondary");
        complete.setMaxWidth(Double.MAX_VALUE);
        complete.setDisable(current.isEmpty());
        complete.setOnAction(e -> current.ifPresent(a -> act(() -> {
            appointmentService.changeStatus(a, AppointmentStatus.COMPLETED);
            return a.getTokenLabel() + " completed";
        })));
        Button back = Fx.secondary("Send Back to Queue");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setDisable(current.isEmpty());
        back.setOnAction(e -> current.ifPresent(a -> act(() -> {
            appointmentService.changeStatus(a, AppointmentStatus.WAITING);
            return a.getTokenLabel() + " sent back to the queue";
        })));
        VBox buttons = new VBox(8, callNext, complete, back);
        buttons.setPrefWidth(210);
        buttons.setMinWidth(210);

        HBox banner = new HBox(22, tokenBox, info, buttons);
        banner.setAlignment(Pos.CENTER_LEFT);
        servingCard.getChildren().setAll(Fx.card(banner));
    }

    @FunctionalInterface
    private interface QueueAction {
        /** Performs the change and returns the success message (or null if the user backed out). */
        String run() throws ValidationException;
    }

    @FunctionalInterface
    private interface SelectedAction {
        String apply(Appointment a) throws ValidationException;
    }

    private void act(QueueAction action) {
        try {
            String msg = action.run();
            refresh();
            if (msg != null) {
                Fx.toast(this, msg, true);
            }
        } catch (ValidationException ex) {
            Fx.toast(this, ex.getMessage(), false);
        }
    }

    private void withSelected(SelectedAction action) {
        Appointment a = table.getSelectionModel().getSelectedItem();
        if (a == null) {
            Fx.toast(this, "Select a patient in the waiting list first.", false);
            return;
        }
        act(() -> action.apply(a));
    }

    /** List row: avatar, name, specialization and a waiting-count pill. */
    private class DoctorCell extends ListCell<Doctor> {
        @Override
        protected void updateItem(Doctor d, boolean empty) {
            super.updateItem(d, empty);
            if (empty || d == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            LoadSummary l = loads.getOrDefault(d.getId(), LoadSummary.EMPTY);
            Label name = new Label(d.getName());
            name.getStyleClass().add("cell-strong");
            Label spec = Fx.muted(d.getSpecialization());
            Label count = new Label(String.valueOf(l.waiting()));
            count.getStyleClass().add(l.waiting() > 0 ? "count-pill" : "count-pill-zero");
            HBox row = new HBox(10, Fx.avatar(d.getName(), 34), new VBox(1, name, spec), Fx.spacer(), count);
            row.setAlignment(Pos.CENTER_LEFT);
            count.setMinWidth(Region.USE_PREF_SIZE);
            setPrefWidth(0); // follow the list's width instead of the content's
            setText(null);
            setGraphic(row);
        }
    }
}
