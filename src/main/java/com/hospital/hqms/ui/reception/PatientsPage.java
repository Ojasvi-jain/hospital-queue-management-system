package com.hospital.hqms.ui.reception;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.PaymentStatus;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PatientService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Patient directory with a profile panel showing the selected patient's visit history. */
class PatientsPage extends HBox {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final PatientService patientService = new PatientService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final ReceptionPortalView portal;

    private final ObservableList<Patient> all = FXCollections.observableArrayList(patientService.findAll());
    private final FilteredList<Patient> filtered = new FilteredList<>(all);
    private final TableView<Patient> table = new TableView<>();
    private final VBox profile = new VBox();
    private final Label count = new Label();

    PatientsPage(ReceptionPortalView portal) {
        super(20);
        this.portal = portal;

        TextField search = new TextField();
        search.setPromptText("Search by name, phone or patient ID");
        StackPane searchBox = new StackPane(search, Icons.of(Icons.SEARCH, 16));
        searchBox.getStyleClass().add("search-box");
        StackPane.setAlignment(searchBox.getChildren().get(1), Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.trim().toLowerCase();
            filtered.setPredicate(p -> q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getPhone().contains(q)
                    || p.getDisplayId().toLowerCase().contains(q));
            count.setText(filtered.size() + " patients");
        });

        table.getColumns().add(Fx.column("Patient ID", Patient::getDisplayId, 90));
        table.getColumns().add(Fx.column("Name", Patient::getName, 170));
        table.getColumns().add(Fx.column("Age", Patient::getAge, 55));
        table.getColumns().add(Fx.column("Gender", Patient::getGender, 75));
        table.getColumns().add(Fx.column("Phone", Patient::getPhone, 110));
        table.getColumns().add(Fx.column("Blood", Patient::getBloodGroup, 60));
        table.getColumns().add(Fx.column("Area", Patient::getAddress, 180));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        SortedList<Patient> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPrefHeight(600);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, p) -> showProfile(p));

        Button add = Fx.iconButton("New Patient", Icons.PERSON_ADD, "btn-primary");
        add.setOnAction(e -> portal.navigate("register"));
        count.getStyleClass().add("count-pill");
        count.setText(all.size() + " patients");
        VBox listCard = Fx.card(Fx.cardHeader("Patient directory", null, count, add), searchBox, table);
        HBox.setHgrow(listCard, Priority.ALWAYS);

        profile.setPrefWidth(360);
        profile.setMinWidth(340);
        profile.getChildren().add(Fx.card(Fx.emptyState(Icons.PERSON, "No patient selected",
                "Select a patient to see their profile and visit history.")));

        getChildren().addAll(listCard, profile);
        table.getSelectionModel().selectFirst();
    }

    private void showProfile(Patient p) {
        if (p == null) {
            return;
        }
        List<Appointment> visits = appointmentService.findByPatient(p);
        Label name = new Label(p.getName());
        name.getStyleClass().add("profile-name");
        HBox head = new HBox(14, Fx.avatar(p.getName(), 60), new VBox(4, name,
                Fx.row(6, Fx.badge(p.getDisplayId()), p.isSenior() ? Fx.badge("Senior Citizen") : new Label())));
        head.setAlignment(Pos.CENTER_LEFT);

        GridPane facts = Fx.formGrid();
        int r = 0;
        Fx.addRow(facts, r++, "Age / Gender", new Label(p.getAge() + " yrs, " + p.getGender()));
        Fx.addRow(facts, r++, "Blood group", new Label(p.getBloodGroup() == null ? "-" : p.getBloodGroup()));
        Fx.addRow(facts, r++, "Mobile", new Label(p.getPhone()));
        Fx.addRow(facts, r++, "Email", new Label(p.getEmail() == null ? "-" : p.getEmail()));
        Fx.addRow(facts, r, "Address", wrap(p.getAddress()));

        long completed = visits.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        double spent = visits.stream().filter(a -> a.getPaymentStatus() == PaymentStatus.PAID).mapToDouble(Appointment::getFee).sum();
        HBox stats = new HBox(10,
                miniStat(String.valueOf(visits.size()), "Bookings"),
                miniStat(String.valueOf(completed), "Visits"),
                miniStat("₹" + (int) spent, "Paid"));

        VBox history = new VBox(8);
        visits.stream().limit(6).forEach(a -> {
            Label what = new Label(a.getSpecialization() + " • " + a.getDoctorName());
            what.getStyleClass().add("cell-strong");
            HBox row = new HBox(10,
                    new VBox(1, what, Fx.muted(a.getDate().format(DATE_FMT) + "  " + a.getTimeSlot() + "  •  " + a.getTokenLabel())),
                    Fx.spacer(), Fx.badge(a.getStatus().toString()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            history.getChildren().add(row);
        });
        if (visits.isEmpty()) {
            history.getChildren().add(Fx.muted("No appointments yet."));
        }

        Button book = Fx.iconButton("Book Appointment", Icons.CALENDAR, "btn-primary");
        book.setMaxWidth(Double.MAX_VALUE);
        book.setOnAction(e -> portal.openBooking(p.getPhone()));
        Optional<Appointment> due = visits.stream()
                .filter(a -> a.getPaymentStatus() == PaymentStatus.PENDING && a.getStatus() != AppointmentStatus.CANCELLED)
                .findFirst();
        VBox actions = new VBox(8, book);
        due.ifPresent(a -> {
            Button pay = Fx.iconButton("Collect Pending Fee (₹" + (int) a.getFee() + ")", Icons.CARD, "btn-secondary");
            pay.setMaxWidth(Double.MAX_VALUE);
            pay.setOnAction(e -> portal.openPayment(a));
            actions.getChildren().add(pay);
        });

        profile.getChildren().setAll(Fx.card(head, facts, stats, Fx.sectionTitle("Recent visits"), history, actions));
    }

    private static VBox miniStat(String value, String caption) {
        Label v = new Label(value);
        v.getStyleClass().add("mini-stat-value");
        VBox box = new VBox(0, v, Fx.muted(caption));
        box.getStyleClass().add("mini-stat");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Label wrap(String text) {
        Label l = new Label(text == null || text.isBlank() ? "-" : text);
        l.setWrapText(true);
        return l;
    }
}
