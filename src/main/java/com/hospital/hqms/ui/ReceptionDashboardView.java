package com.hospital.hqms.ui;

import com.hospital.hqms.db.DatabaseManager;
import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.DashboardStats;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.Payment;
import com.hospital.hqms.model.Priority;
import com.hospital.hqms.model.Staff;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PatientService;
import com.hospital.hqms.service.PaymentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.components.BookingPane;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.PaymentPane;
import com.hospital.hqms.ui.components.RegistrationPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/** Receptionist portal: live queue control, appointment management, walk-ins and payments. */
public class ReceptionDashboardView implements View {

    private static final DateTimeFormatter PAID_AT_FMT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    private final QueueService queueService = new QueueService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final PatientService patientService = new PatientService();
    private final PaymentService paymentService = new PaymentService();

    private final VBox root = new VBox(18);
    private final TabPane tabs = new TabPane();

    // Summary cards
    private final Label statPatients = new Label();
    private final Label statToday = new Label();
    private final Label statWaiting = new Label();
    private final Label statCompleted = new Label();
    private final Label statRevenue = new Label();

    // Live queue tab
    private final ComboBox<Doctor> queueDoctor = new ComboBox<>();
    private final DatePicker queueDate = new DatePicker(LocalDate.now());
    private final TableView<Appointment> waitingTable = new TableView<>();
    private final VBox nowServingBox = new VBox(10);
    private final Label queueSummary = Fx.muted("");
    private final ComboBox<Priority> priorityChoice = new ComboBox<>();

    // All appointments tab
    private final ObservableList<Appointment> allAppointments = FXCollections.observableArrayList();
    private final FilteredList<Appointment> filteredAppointments = new FilteredList<>(allAppointments);
    private final TableView<Appointment> appointmentsTable = new TableView<>();
    private final TextField apptSearch = new TextField();
    private final ComboBox<Object> apptStatusFilter = new ComboBox<>();
    private final DatePicker apptDateFilter = new DatePicker();
    private final CheckBox apptAllDates = new CheckBox("All dates");

    // Patients & payments tabs
    private final ObservableList<Patient> allPatients = FXCollections.observableArrayList();
    private final FilteredList<Patient> filteredPatients = new FilteredList<>(allPatients);
    private final TableView<Patient> patientsTable = new TableView<>();
    private final TableView<Payment> paymentsTable = new TableView<>();
    private final Label paymentsTotal = new Label();

    private final Tab queueTab = new Tab("Live Queue");
    private final Tab bookTab = new Tab("Walk-in Booking");
    private final Tab payTab = new Tab("Collect Payment");
    private BookingPane bookingPane;
    private PaymentPane paymentPane;

    public ReceptionDashboardView(Navigator nav, Staff staff) {
        root.setPadding(new Insets(24, 32, 32, 32));

        Button reset = Fx.secondary("Reset Demo Data");
        reset.setOnAction(e -> {
            if (Fx.confirm("Reset all data?", "This deletes every change and reloads the original sample data.")) {
                DatabaseManager.getInstance().resetDemoData();
                nav.showReceptionDashboard(staff);
            }
        });
        Button logout = Fx.danger("Logout");
        logout.setOnAction(e -> nav.showHome());

        VBox titleBox = new VBox(2, Fx.title("Reception Desk"), Fx.muted("Signed in as " + staff.fullName() + " (" + staff.username() + ")"));
        HBox top = Fx.row(10, titleBox, Fx.spacer(), reset, logout);

        HBox stats = new HBox(14,
                Fx.statCard("Registered patients", statPatients, "accent-blue"),
                Fx.statCard("Appointments today", statToday, "accent-purple"),
                Fx.statCard("Waiting now", statWaiting, "accent-amber"),
                Fx.statCard("Completed today", statCompleted, "accent-green"),
                Fx.statCard("Collected today", statRevenue, "accent-teal"));

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        queueTab.setContent(buildQueueTab());
        Tab apptTab = new Tab("All Appointments", buildAppointmentsTab());
        Tab patientsTab = new Tab("Patients", buildPatientsTab());
        Tab registerTab = new Tab("Register Patient");
        bookingPane = new BookingPane(a -> {
            paymentPane.loadAppointment(a);
            tabs.getSelectionModel().select(payTab);
        });
        paymentPane = new PaymentPane(true, this::refreshAll);
        registerTab.setContent(padded(new RegistrationPane(p -> {
            refreshAll();
            bookingPane.setPhone(p.getPhone());
            tabs.getSelectionModel().select(bookTab);
        })));
        bookTab.setContent(padded(bookingPane));
        payTab.setContent(padded(paymentPane));
        Tab paymentsTab = new Tab("Payment History", buildPaymentsTab());

        tabs.getTabs().addAll(queueTab, apptTab, patientsTab, registerTab, bookTab, payTab, paymentsTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshAll());

        root.getChildren().addAll(top, stats, tabs);
        refreshAll();
    }

    // =========================================================================
    //  Live queue
    // =========================================================================

    private Node buildQueueTab() {
        queueDoctor.getItems().setAll(appointmentService.getAvailableDoctors());
        // Open on the doctor with the longest queue today (falls back to the first doctor).
        Map<String, Long> waiting = queueService.waitingCountByDoctor(LocalDate.now());
        queueDoctor.getItems().stream()
                .max(java.util.Comparator.comparingLong(d -> waiting.getOrDefault(d.getName(), 0L)))
                .ifPresent(queueDoctor::setValue);
        queueDoctor.setPrefWidth(380);
        queueDoctor.valueProperty().addListener((obs, o, n) -> refreshQueue());
        queueDate.valueProperty().addListener((obs, o, n) -> refreshQueue());
        Button refresh = Fx.secondary("Refresh");
        refresh.setOnAction(e -> refreshAll());

        HBox controls = Fx.row(10, new Label("Doctor"), queueDoctor, new Label("Date"), queueDate, refresh);

        // Waiting list table
        TableView<Appointment> t = waitingTable;
        t.getColumns().add(Fx.column("#", a -> t.getItems().indexOf(a) + 1, 40));
        t.getColumns().add(Fx.column("Token", Appointment::getTokenLabel, 85));
        t.getColumns().add(Fx.column("Patient", Appointment::getPatientName, 150));
        t.getColumns().add(Fx.badgeColumn("Priority", Appointment::getPriority, 120));
        t.getColumns().add(Fx.column("Slot", Appointment::getTimeSlot, 60));
        t.getColumns().add(Fx.badgeColumn("Payment", Appointment::getPaymentStatus, 90));
        t.getColumns().add(Fx.column("Symptoms", Appointment::getSymptoms, 200));
        t.setPlaceholder(Fx.muted("Nobody is waiting for this doctor."));
        t.setPrefHeight(320);

        priorityChoice.getItems().addAll(Priority.values());
        priorityChoice.setPromptText("Priority");
        Button applyPriority = Fx.secondary("Set Priority");
        applyPriority.setOnAction(e -> withSelected(waitingTable, a -> {
            if (priorityChoice.getValue() == null) {
                throw new ValidationException("Choose a priority first.");
            }
            appointmentService.changePriority(a, priorityChoice.getValue());
        }));
        Button noShow = Fx.secondary("Mark No-Show");
        noShow.setOnAction(e -> withSelected(waitingTable, a -> appointmentService.changeStatus(a, AppointmentStatus.NO_SHOW)));
        Button cancel = Fx.danger("Cancel Appointment");
        cancel.setOnAction(e -> withSelected(waitingTable, a -> {
            if (Fx.confirm("Cancel " + a.getTokenLabel() + "?", "Paid fees will be marked as refunded.")) {
                appointmentService.changeStatus(a, AppointmentStatus.CANCELLED);
            }
        }));
        Button collect = Fx.secondary("Collect Fee");
        collect.setOnAction(e -> {
            Appointment a = waitingTable.getSelectionModel().getSelectedItem();
            if (a == null) {
                Fx.info("No patient selected", "Select a patient in the waiting list first.");
                return;
            }
            paymentPane.loadAppointment(a);
            tabs.getSelectionModel().select(payTab);
        });

        VBox waitingCard = Fx.card(
                Fx.row(10, Fx.sectionTitle("Waiting List"), Fx.spacer(), queueSummary),
                Fx.muted("Order: Emergency → Senior Citizen → Normal, then by token number."),
                t,
                Fx.row(8, priorityChoice, applyPriority, Fx.spacer(), collect, noShow, cancel));
        HBox.setHgrow(waitingCard, javafx.scene.layout.Priority.ALWAYS);

        nowServingBox.setPrefWidth(300);
        nowServingBox.setMinWidth(280);
        HBox body = new HBox(18, nowServingBox, waitingCard);

        VBox box = new VBox(16, controls, body);
        box.setPadding(new Insets(18, 0, 0, 0));
        return box;
    }

    private void refreshQueue() {
        Doctor d = queueDoctor.getValue();
        LocalDate day = queueDate.getValue();
        if (d == null || day == null) {
            return;
        }
        waitingTable.getItems().setAll(queueService.getWaitingList(d.getId(), day));

        long completed = queueService.getAppointmentsFor(d.getId(), day).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        queueSummary.setText(waitingTable.getItems().size() + " waiting  •  " + completed + " completed");

        Optional<Appointment> current = queueService.getNowServing(d.getId(), day);
        Label token = new Label(current.map(Appointment::getTokenLabel).orElse("—"));
        token.getStyleClass().add("token-big");
        Label who = new Label(current.map(a -> a.getPatientName() + " • " + a.getPriority()).orElse("No patient with the doctor"));
        who.getStyleClass().add("info-value");
        who.setWrapText(true);
        VBox tokenBox = new VBox(4, Fx.muted("Now serving in " + d.getRoom()), token, who);
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("token-box");

        Button callNext = Fx.primary("Call Next Patient");
        callNext.setMaxWidth(Double.MAX_VALUE);
        callNext.setDisable(waitingTable.getItems().isEmpty() && current.isEmpty());
        callNext.setOnAction(e -> {
            Optional<Appointment> next = queueService.callNext(d.getId(), day);
            if (next.isEmpty()) {
                Fx.info("Queue is empty", "The current consultation was completed and nobody else is waiting.");
            }
            refreshAll();
        });

        Button complete = Fx.secondary("Mark Current as Completed");
        complete.setMaxWidth(Double.MAX_VALUE);
        complete.setDisable(current.isEmpty());
        complete.setOnAction(e -> current.ifPresent(a -> run(() -> appointmentService.changeStatus(a, AppointmentStatus.COMPLETED))));

        Button backToQueue = Fx.secondary("Send Back to Queue");
        backToQueue.setMaxWidth(Double.MAX_VALUE);
        backToQueue.setDisable(current.isEmpty());
        backToQueue.setOnAction(e -> current.ifPresent(a -> run(() -> appointmentService.changeStatus(a, AppointmentStatus.WAITING))));

        nowServingBox.getChildren().setAll(Fx.card(
                Fx.sectionTitle(d.getName()),
                Fx.muted(d.getSpecialization() + "  •  " + d.getTimingLabel()),
                tokenBox, callNext, complete, backToQueue));
    }

    // =========================================================================
    //  All appointments
    // =========================================================================

    private Node buildAppointmentsTab() {
        apptSearch.setPromptText("Search patient, phone, token or doctor...");
        apptSearch.setPrefWidth(300);
        apptStatusFilter.getItems().add("All statuses");
        apptStatusFilter.getItems().addAll(AppointmentStatus.values());
        apptStatusFilter.getSelectionModel().selectFirst();
        apptDateFilter.setValue(LocalDate.now());
        apptAllDates.setSelected(false);

        apptSearch.textProperty().addListener((obs, o, n) -> applyAppointmentFilter());
        apptStatusFilter.valueProperty().addListener((obs, o, n) -> applyAppointmentFilter());
        apptDateFilter.valueProperty().addListener((obs, o, n) -> applyAppointmentFilter());
        apptAllDates.selectedProperty().addListener((obs, o, n) -> {
            apptDateFilter.setDisable(n);
            applyAppointmentFilter();
        });

        TableView<Appointment> t = appointmentsTable;
        t.getColumns().add(Fx.column("ID", Appointment::getDisplayId, 75));
        t.getColumns().add(Fx.column("Token", Appointment::getTokenLabel, 80));
        t.getColumns().add(Fx.column("Date", Appointment::getDate, 95));
        t.getColumns().add(Fx.column("Time", Appointment::getTimeSlot, 55));
        t.getColumns().add(Fx.column("Patient", Appointment::getPatientName, 140));
        t.getColumns().add(Fx.column("Phone", Appointment::getPatientPhone, 100));
        t.getColumns().add(Fx.column("Doctor", Appointment::getDoctorName, 140));
        t.getColumns().add(Fx.badgeColumn("Priority", Appointment::getPriority, 115));
        t.getColumns().add(Fx.badgeColumn("Status", Appointment::getStatus, 125));
        t.getColumns().add(Fx.badgeColumn("Payment", Appointment::getPaymentStatus, 90));
        SortedList<Appointment> sorted = new SortedList<>(filteredAppointments);
        sorted.comparatorProperty().bind(t.comparatorProperty());
        t.setItems(sorted);
        t.setPrefHeight(380);
        t.setPlaceholder(Fx.muted("No appointments match the filters."));

        ComboBox<AppointmentStatus> newStatus = new ComboBox<>();
        newStatus.getItems().addAll(AppointmentStatus.values());
        newStatus.setPromptText("New status");
        Button update = Fx.primary("Update Status");
        update.setOnAction(e -> withSelected(appointmentsTable, a -> {
            if (newStatus.getValue() == null) {
                throw new ValidationException("Choose the new status first.");
            }
            appointmentService.changeStatus(a, newStatus.getValue());
        }));
        // Show only the statuses the selected appointment may legally move to.
        t.getSelectionModel().selectedItemProperty().addListener((obs, o, a) -> {
            newStatus.getItems().setAll(a == null ? java.util.List.of() : a.getStatus().allowedNext());
            newStatus.setPromptText(a == null || newStatus.getItems().isEmpty() ? "No changes allowed" : "New status");
        });

        VBox box = new VBox(14,
                Fx.row(10, apptSearch, apptStatusFilter, new Label("Date"), apptDateFilter, apptAllDates),
                t,
                Fx.row(10, new Label("Selected appointment:"), newStatus, update));
        box.setPadding(new Insets(18, 0, 0, 0));
        return box;
    }

    private void applyAppointmentFilter() {
        String q = apptSearch.getText() == null ? "" : apptSearch.getText().trim().toLowerCase();
        Object status = apptStatusFilter.getValue();
        LocalDate date = apptAllDates.isSelected() ? null : apptDateFilter.getValue();
        // Predicate built from lambdas - the FilteredList re-evaluates it on every change.
        filteredAppointments.setPredicate(a ->
                (q.isEmpty()
                        || a.getPatientName().toLowerCase().contains(q)
                        || a.getPatientPhone().contains(q)
                        || a.getTokenLabel().toLowerCase().contains(q)
                        || a.getDoctorName().toLowerCase().contains(q))
                && (!(status instanceof AppointmentStatus s) || a.getStatus() == s)
                && (date == null || a.getDate().equals(date)));
    }

    // =========================================================================
    //  Patients and payments
    // =========================================================================

    private Node buildPatientsTab() {
        TextField search = new TextField();
        search.setPromptText("Search by name, phone or patient ID...");
        search.setPrefWidth(320);
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.trim().toLowerCase();
            filteredPatients.setPredicate(p -> q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getPhone().contains(q)
                    || p.getDisplayId().toLowerCase().contains(q));
        });

        TableView<Patient> t = patientsTable;
        t.getColumns().add(Fx.column("Patient ID", Patient::getDisplayId, 90));
        t.getColumns().add(Fx.column("Name", Patient::getName, 170));
        t.getColumns().add(Fx.column("Age", Patient::getAge, 55));
        t.getColumns().add(Fx.column("Gender", Patient::getGender, 75));
        t.getColumns().add(Fx.column("Phone", Patient::getPhone, 110));
        t.getColumns().add(Fx.column("Blood", Patient::getBloodGroup, 65));
        t.getColumns().add(Fx.column("Email", Patient::getEmail, 200));
        t.getColumns().add(Fx.column("Address", Patient::getAddress, 200));
        SortedList<Patient> sorted = new SortedList<>(filteredPatients);
        sorted.comparatorProperty().bind(t.comparatorProperty());
        t.setItems(sorted);
        t.setPrefHeight(420);

        Button book = Fx.primary("Book for Selected Patient");
        book.setOnAction(e -> {
            Patient p = t.getSelectionModel().getSelectedItem();
            if (p == null) {
                Fx.info("No patient selected", "Select a patient in the table first.");
                return;
            }
            bookingPane.setPhone(p.getPhone());
            tabs.getSelectionModel().select(bookTab);
        });

        VBox box = new VBox(14, Fx.row(10, search, Fx.spacer(), book), t);
        box.setPadding(new Insets(18, 0, 0, 0));
        return box;
    }

    private Node buildPaymentsTab() {
        TableView<Payment> t = paymentsTable;
        t.getColumns().add(Fx.column("Transaction ID", Payment::transactionRef, 260));
        t.getColumns().add(Fx.column("Appointment", p -> String.format("A%05d", p.appointmentId()), 100));
        t.getColumns().add(Fx.column("Amount", p -> "₹" + (int) p.amount(), 90));
        t.getColumns().add(Fx.column("Method", Payment::method, 160));
        t.getColumns().add(Fx.badgeColumn("Status", Payment::status, 100));
        t.getColumns().add(Fx.column("Paid at", p -> p.paidAt().format(PAID_AT_FMT), 150));
        t.setPrefHeight(420);
        paymentsTotal.getStyleClass().add("info-value");

        VBox box = new VBox(14, Fx.row(10, Fx.sectionTitle("All Payments"), Fx.spacer(), paymentsTotal), t);
        box.setPadding(new Insets(18, 0, 0, 0));
        return box;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Re-reads everything from the database so every tab shows current data. */
    private void refreshAll() {
        DashboardStats s = queueService.getDashboardStats(LocalDate.now());
        statPatients.setText(String.valueOf(s.totalPatients()));
        statToday.setText(String.valueOf(s.todaysAppointments()));
        statWaiting.setText(String.valueOf(s.waiting()));
        statCompleted.setText(String.valueOf(s.completed()));
        statRevenue.setText("₹" + (int) s.revenueToday());

        refreshQueue();
        allAppointments.setAll(appointmentService.findAll());
        applyAppointmentFilter();
        allPatients.setAll(patientService.findAll());

        var payments = paymentService.findAll();
        paymentsTable.getItems().setAll(payments);
        double total = payments.stream().mapToDouble(Payment::amount).sum();
        paymentsTotal.setText(payments.size() + " payments  •  Total ₹" + (int) total);

        Map<String, Long> waitingByDoctor = queueService.waitingCountByDoctor(LocalDate.now());
        queueTab.setText("Live Queue (" + waitingByDoctor.values().stream().mapToLong(Long::longValue).sum() + " waiting)");
    }

    @FunctionalInterface
    private interface Action {
        void run() throws ValidationException;
    }

    @FunctionalInterface
    private interface AppointmentAction {
        void apply(Appointment a) throws ValidationException;
    }

    /** Runs an action that may fail validation, shows the error, then refreshes. */
    private void run(Action action) {
        try {
            action.run();
        } catch (ValidationException ex) {
            Fx.error("Action not allowed", ex.getMessage());
        }
        refreshAll();
    }

    private void withSelected(TableView<Appointment> table, AppointmentAction action) {
        Appointment a = table.getSelectionModel().getSelectedItem();
        if (a == null) {
            Fx.info("No appointment selected", "Select a row in the table first.");
            return;
        }
        run(() -> action.apply(a));
    }

    private static Node padded(Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(20, 0, 0, 0));
        return box;
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
