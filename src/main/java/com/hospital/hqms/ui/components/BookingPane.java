package com.hospital.hqms.ui.components;

import com.hospital.hqms.exception.HospitalException;
import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.Priority;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PatientService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Doctor selection, date / slot scheduling and token generation. */
public class BookingPane extends HBox {

    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    private final PatientService patientService = new PatientService();
    private final AppointmentService appointmentService = new AppointmentService();

    private final TextField phone = new TextField();
    private final Label patientInfo = Fx.muted("No patient selected.");
    private final ComboBox<Doctor> doctor = new ComboBox<>();
    private final DatePicker date = new DatePicker(LocalDate.now());
    private final ComboBox<LocalTime> slot = new ComboBox<>();
    private final ComboBox<Priority> priority = new ComboBox<>();
    private final TextArea symptoms = new TextArea();
    private final Label slotHint = Fx.muted("");
    private final Label message = new Label();
    private final VBox sidePanel = new VBox(16);

    private Patient patient;
    private final Consumer<Appointment> onPayRequested;

    public BookingPane(Consumer<Appointment> onPayRequested) {
        this.onPayRequested = onPayRequested;
        setSpacing(20);

        phone.setPromptText("Registered mobile number");
        Button find = Fx.secondary("Find");
        find.setOnAction(e -> findPatient());
        phone.setOnAction(e -> findPatient());
        HBox phoneRow = Fx.row(8, phone, find);
        HBox.setHgrow(phone, javafx.scene.layout.Priority.ALWAYS);

        doctor.setPromptText("Choose a doctor");
        doctor.setMaxWidth(Double.MAX_VALUE);
        doctor.valueProperty().addListener((obs, o, n) -> {
            refreshSlots();
            showDoctorInfo(n);
        });

        date.setEditable(false);
        date.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                boolean outOfRange = d.isBefore(LocalDate.now())
                        || d.isAfter(LocalDate.now().plusDays(AppointmentService.MAX_DAYS_AHEAD));
                setDisable(empty || outOfRange);
            }
        });
        date.valueProperty().addListener((obs, o, n) -> refreshSlots());

        slot.setPromptText("Select a doctor and date first");
        slot.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(SLOT_FMT);
            }

            @Override
            public LocalTime fromString(String s) {
                return null;
            }
        });

        priority.getItems().addAll(Priority.values());
        priority.setValue(Priority.NORMAL);
        symptoms.setPromptText("Briefly describe the problem (optional)");
        symptoms.setPrefRowCount(3);

        GridPane form = Fx.formGrid();
        Fx.addRow(form, 0, "Patient mobile *", phoneRow);
        Fx.addRow(form, 1, "", patientInfo);
        Fx.addRow(form, 2, "Doctor *", doctor);
        Fx.addRow(form, 3, "Date *", date);
        Fx.addRow(form, 4, "Time slot *", slot);
        Fx.addRow(form, 5, "", slotHint);
        Fx.addRow(form, 6, "Priority", priority);
        Fx.addRow(form, 7, "Symptoms", symptoms);

        Button book = Fx.primary("Book Appointment & Generate Token");
        book.setOnAction(e -> book());
        message.setWrapText(true);

        VBox formCard = Fx.card(
                Fx.sectionTitle("Book an Appointment"),
                Fx.muted("Senior citizens (60+) are automatically given priority in the queue."),
                form, book, message);
        formCard.setPrefWidth(560);
        formCard.setMinWidth(480);

        sidePanel.setPrefWidth(380);
        sidePanel.setMinWidth(320);
        getChildren().addAll(formCard, sidePanel);
        reloadDoctors();
        showDoctorInfo(null);
    }

    /** Pre-fills the phone number (e.g. straight after registration) and looks the patient up. */
    public void setPhone(String number) {
        phone.setText(number);
        findPatient();
    }

    public void reloadDoctors() {
        Doctor selected = doctor.getValue();
        doctor.getItems().setAll(appointmentService.getAvailableDoctors());
        if (selected != null) {
            doctor.getItems().stream().filter(d -> d.getId() == selected.getId()).findFirst().ifPresent(doctor::setValue);
        }
    }

    private void findPatient() {
        try {
            patient = patientService.findByPhone(phone.getText());
            patientInfo.setText(patient.getName() + "  •  " + patient.getDisplayId() + "  •  "
                    + patient.getAge() + " yrs, " + patient.getGender() + (patient.isSenior() ? "  •  Senior citizen" : ""));
            patientInfo.getStyleClass().setAll("label", "msg-success");
        } catch (ValidationException ex) {
            patient = null;
            patientInfo.setText(ex.getMessage());
            patientInfo.getStyleClass().setAll("label", "msg-error");
        }
    }

    private void refreshSlots() {
        slot.getItems().clear();
        Doctor d = doctor.getValue();
        LocalDate day = date.getValue();
        if (d == null || day == null) {
            slotHint.setText("");
            return;
        }
        List<LocalTime> free = appointmentService.getFreeSlots(d, day);
        slot.getItems().setAll(free);
        slot.setPromptText(free.isEmpty() ? "Fully booked" : "Choose a time");
        slotHint.setText(free.size() + " of " + d.generateSlots().size() + " slots free on " + day.format(DATE_FMT));
    }

    private void showDoctorInfo(Doctor d) {
        sidePanel.getChildren().clear();
        if (d == null) {
            sidePanel.getChildren().add(Fx.card(
                    Fx.sectionTitle("Our Doctors"),
                    Fx.muted("Select a doctor to see their timings, room and consultation fee. "
                            + "A token number is generated as soon as the appointment is booked.")));
            return;
        }
        Label name = new Label(d.getName());
        name.getStyleClass().add("doctor-name");
        sidePanel.getChildren().add(Fx.card(
                Fx.badge(d.getSpecialization()),
                name,
                Fx.muted(d.getQualification()),
                infoLine("OPD timings", d.getTimingLabel()),
                infoLine("Room", d.getRoom()),
                infoLine("Slot length", d.getSlotMinutes() + " minutes"),
                infoLine("Consultation fee", "₹" + (int) d.getConsultationFee())));
    }

    private HBox infoLine(String key, String value) {
        Label k = Fx.muted(key);
        Label v = new Label(value);
        v.getStyleClass().add("info-value");
        return Fx.row(8, k, Fx.spacer(), v);
    }

    private void book() {
        try {
            Appointment a = appointmentService.book(patient, doctor.getValue(), date.getValue(), slot.getValue(),
                    priority.getValue(), symptoms.getText());
            message.setText("");
            symptoms.clear();
            refreshSlots();
            showConfirmation(a);
        } catch (HospitalException ex) {
            message.setText(ex.getMessage());
            message.getStyleClass().setAll("label", "msg-error");
            refreshSlots();
        }
    }

    private void showConfirmation(Appointment a) {
        Label token = new Label(a.getTokenLabel());
        token.getStyleClass().add("token-big");
        Label caption = Fx.muted("Your token number");

        VBox tokenBox = new VBox(4, caption, token);
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("token-box");

        Button pay = Fx.primary("Proceed to Payment  (₹" + (int) a.getFee() + ")");
        pay.setMaxWidth(Double.MAX_VALUE);
        pay.setOnAction(e -> onPayRequested.accept(a));

        sidePanel.getChildren().setAll(Fx.card(
                Fx.badge("Booked"),
                Fx.sectionTitle("Appointment Confirmed"),
                tokenBox,
                infoLine("Appointment ID", a.getDisplayId()),
                infoLine("Patient", a.getPatientName()),
                infoLine("Doctor", a.getDoctorName()),
                infoLine("Date", a.getDate().format(DATE_FMT)),
                infoLine("Reporting time", a.getTimeSlot().format(SLOT_FMT)),
                infoLine("Room", a.getRoom()),
                infoLine("Queue priority", a.getPriority().toString()),
                pay));
    }
}
