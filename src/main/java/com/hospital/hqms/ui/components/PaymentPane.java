package com.hospital.hqms.ui.components;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.AppointmentStatus;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.model.Payment;
import com.hospital.hqms.model.PaymentMethod;
import com.hospital.hqms.model.PaymentStatus;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.PatientService;
import com.hospital.hqms.service.PaymentService;
import com.hospital.hqms.service.payment.CardPaymentProcessor;
import com.hospital.hqms.service.payment.CashPaymentProcessor;
import com.hospital.hqms.service.payment.PaymentProcessor;
import com.hospital.hqms.service.payment.UpiPaymentProcessor;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Simulated payment screen. The actual gateway call runs on a background
 * {@link Task} so the window stays responsive while "contacting the bank".
 */
public class PaymentPane extends HBox {

    private static final DateTimeFormatter RECEIPT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final PatientService patientService = new PatientService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final PaymentService paymentService = new PaymentService();

    private final TextField phone = new TextField();
    private final ComboBox<Appointment> appointment = new ComboBox<>();
    private final Label amount = new Label("₹0");
    private final ToggleGroup methodGroup = new ToggleGroup();

    private final TextField cardNumber = new TextField();
    private final TextField cardHolder = new TextField();
    private final TextField expiry = new TextField();
    private final PasswordField cvv = new PasswordField();
    private final TextField upiId = new TextField();

    private final VBox methodDetails = new VBox(10);
    private final Button payButton = Fx.primary("Pay Now");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label message = new Label();
    private final VBox receiptPanel = new VBox(16);

    private final Runnable onPaid;

    public PaymentPane(boolean allowCash, Runnable onPaid) {
        this.onPaid = onPaid;
        setSpacing(20);

        phone.setPromptText("Registered mobile number");
        Button find = Fx.secondary("Load");
        find.setOnAction(e -> loadForPhone());
        phone.setOnAction(e -> loadForPhone());
        HBox phoneRow = Fx.row(8, phone, find);
        HBox.setHgrow(phone, Priority.ALWAYS);

        appointment.setPromptText("Unpaid appointments appear here");
        appointment.setMaxWidth(Double.MAX_VALUE);
        appointment.valueProperty().addListener((obs, o, n) ->
                amount.setText(n == null ? "₹0" : "₹" + (int) n.getFee()));
        amount.getStyleClass().add("amount");

        RadioButton card = radio("Card", PaymentMethod.CARD);
        RadioButton upi = radio("UPI", PaymentMethod.UPI);
        HBox methods = Fx.row(18, card, upi);
        if (allowCash) {
            methods.getChildren().add(radio("Cash", PaymentMethod.CASH));
        }
        methodGroup.selectedToggleProperty().addListener((obs, o, n) -> showMethodFields());
        card.setSelected(true);

        cardNumber.setPromptText("4111 1111 1111 1111");
        cardHolder.setPromptText("Name on card");
        expiry.setPromptText("MM/YY");
        cvv.setPromptText("123");
        upiId.setPromptText("yourname@okbank");

        GridPane form = Fx.formGrid();
        Fx.addRow(form, 0, "Patient mobile *", phoneRow);
        Fx.addRow(form, 1, "Appointment *", appointment);
        Fx.addRow(form, 2, "Amount", amount);
        Fx.addRow(form, 3, "Pay using", methods);

        spinner.setVisible(false);
        spinner.setPrefSize(28, 28);
        payButton.setOnAction(e -> pay());
        message.setWrapText(true);

        VBox formCard = Fx.card(
                Fx.sectionTitle("Consultation Fee Payment"),
                Fx.muted("This is a simulated payment gateway - no real money is charged."),
                form, methodDetails, Fx.row(12, payButton, spinner), message);
        formCard.setPrefWidth(560);
        formCard.setMinWidth(480);

        receiptPanel.setPrefWidth(380);
        receiptPanel.setMinWidth(320);
        receiptPanel.getChildren().add(Fx.card(
                Fx.sectionTitle("Test details"),
                Fx.muted("Card that succeeds: 4111 1111 1111 1111, any future expiry (e.g. 12/29), any 3-digit CVV."),
                Fx.muted("Card that is declined: 4000 0000 0000 0002."),
                Fx.muted("UPI succeeds for e.g. patient@okaxis; any ID ending in @fail is declined.")));
        getChildren().addAll(formCard, receiptPanel);
        showMethodFields();
    }

    /** Opens the screen with a specific appointment already selected (after booking). */
    public void loadAppointment(Appointment a) {
        phone.setText(a.getPatientPhone());
        loadForPhone();
        appointment.getItems().stream().filter(x -> x.getId() == a.getId()).findFirst().ifPresent(appointment::setValue);
    }

    private RadioButton radio(String text, PaymentMethod method) {
        RadioButton rb = new RadioButton(text);
        rb.setUserData(method);
        rb.setToggleGroup(methodGroup);
        return rb;
    }

    private PaymentMethod selectedMethod() {
        return methodGroup.getSelectedToggle() == null ? PaymentMethod.CARD
                : (PaymentMethod) methodGroup.getSelectedToggle().getUserData();
    }

    private void showMethodFields() {
        GridPane g = Fx.formGrid();
        switch (selectedMethod()) {
            case CARD -> {
                Fx.addRow(g, 0, "Card number", cardNumber);
                Fx.addRow(g, 1, "Card holder", cardHolder);
                Fx.addRow(g, 2, "Expiry / CVV", Fx.row(8, expiry, cvv));
            }
            case UPI -> Fx.addRow(g, 0, "UPI ID", upiId);
            case CASH -> Fx.addRow(g, 0, "", Fx.muted("Collect the exact amount in cash and press Pay Now."));
        }
        methodDetails.getChildren().setAll(g);
    }

    private void loadForPhone() {
        message.setText("");
        try {
            Patient p = patientService.findByPhone(phone.getText());
            List<Appointment> unpaid = appointmentService.findByPatient(p).stream()
                    .filter(a -> a.getPaymentStatus() == PaymentStatus.PENDING)
                    .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                    .toList();
            appointment.getItems().setAll(unpaid);
            if (unpaid.isEmpty()) {
                setMessage(p.getName() + " has no pending payments.", false);
            } else {
                appointment.setValue(unpaid.get(0));
            }
        } catch (ValidationException ex) {
            appointment.getItems().clear();
            setMessage(ex.getMessage(), true);
        }
    }

    /** Factory: builds the right processor for the chosen method (polymorphism). */
    private PaymentProcessor createProcessor() {
        return switch (selectedMethod()) {
            case CARD -> new CardPaymentProcessor(cardNumber.getText(), cardHolder.getText(), expiry.getText(), cvv.getText());
            case UPI -> new UpiPaymentProcessor(upiId.getText());
            case CASH -> new CashPaymentProcessor();
        };
    }

    private void pay() {
        Appointment selected = appointment.getValue();
        if (selected == null) {
            setMessage("Load the patient's appointments and pick one to pay for.", true);
            return;
        }
        PaymentProcessor processor = createProcessor();

        Task<Payment> task = new Task<>() {
            @Override
            protected Payment call() throws Exception {
                return paymentService.pay(selected, processor);
            }
        };
        task.setOnRunning(e -> {
            payButton.setDisable(true);
            spinner.setVisible(true);
            setMessage("Contacting payment gateway...", false);
        });
        task.setOnSucceeded(e -> {
            payButton.setDisable(false);
            spinner.setVisible(false);
            Payment payment = task.getValue();
            setMessage("Payment successful.", false);
            showReceipt(selected, payment);
            clearSensitiveFields();
            appointment.getItems().remove(selected);
            appointment.setValue(appointment.getItems().isEmpty() ? null : appointment.getItems().get(0));
            if (onPaid != null) {
                onPaid.run();
            }
        });
        task.setOnFailed(e -> {
            payButton.setDisable(false);
            spinner.setVisible(false);
            Throwable ex = task.getException();
            setMessage("Payment failed: " + ex.getMessage(), true);
        });

        Thread worker = new Thread(task, "payment-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void showReceipt(Appointment a, Payment p) {
        receiptPanel.getChildren().setAll(Fx.card(
                Fx.row(10, Fx.sectionTitle("Payment Receipt"), Fx.spacer(), Fx.badge("Paid")),
                line("Transaction ID", p.transactionRef()),
                line("Paid on", p.paidAt().format(RECEIPT_FMT)),
                line("Method", p.method().toString()),
                line("Patient", a.getPatientName()),
                line("Doctor", a.getDoctorName()),
                line("Token", a.getTokenLabel()),
                line("Amount paid", "₹" + (int) p.amount()),
                Fx.muted("Please show this token at the reception desk.")));
    }

    private HBox line(String key, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("info-value");
        return Fx.row(8, Fx.muted(key), Fx.spacer(), v);
    }

    private void clearSensitiveFields() {
        cardNumber.clear();
        expiry.clear();
        cvv.clear();
    }

    private void setMessage(String text, boolean isError) {
        message.setText(text);
        message.getStyleClass().setAll("label", isError ? "msg-error" : "msg-success");
    }
}
