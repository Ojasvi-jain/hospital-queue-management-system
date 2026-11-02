package com.hospital.hqms.ui.components;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.service.PatientService;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** New-patient registration form. Re-used by the patient portal and the reception desk. */
public class RegistrationPane extends VBox {

    private final PatientService patientService = new PatientService();

    private final TextField name = new TextField();
    private final TextField age = new TextField();
    private final ComboBox<String> gender = new ComboBox<>();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final ComboBox<String> bloodGroup = new ComboBox<>();
    private final TextArea address = new TextArea();
    private final Label message = new Label();

    public RegistrationPane(Consumer<Patient> onRegistered) {
        setSpacing(16);

        name.setPromptText("e.g. Rahul Sharma");
        age.setPromptText("Years");
        gender.getItems().addAll("Male", "Female", "Other");
        gender.setPromptText("Select");
        phone.setPromptText("10-digit mobile number");
        email.setPromptText("Optional");
        bloodGroup.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown");
        bloodGroup.setValue("Unknown");
        address.setPromptText("Optional");
        address.setPrefRowCount(2);

        GridPane form = Fx.formGrid();
        Fx.addRow(form, 0, "Full name *", name);
        Fx.addRow(form, 1, "Age *", age);
        Fx.addRow(form, 2, "Gender *", gender);
        Fx.addRow(form, 3, "Mobile *", phone);
        Fx.addRow(form, 4, "Email", email);
        Fx.addRow(form, 5, "Blood group", bloodGroup);
        Fx.addRow(form, 6, "Address", address);

        Button register = Fx.primary("Register Patient");
        Button clear = Fx.secondary("Clear");
        message.setWrapText(true);

        register.setOnAction(e -> {
            try {
                Patient p = patientService.register(name.getText(), age.getText(), gender.getValue(),
                        phone.getText(), email.getText(), bloodGroup.getValue(), address.getText());
                showMessage("Registered " + p.getName() + " with Patient ID " + p.getDisplayId() + ".", false);
                clearForm();
                onRegistered.accept(p);
            } catch (ValidationException ex) {
                showMessage(ex.getMessage(), true);
            }
        });
        clear.setOnAction(e -> {
            clearForm();
            message.setText("");
        });

        getChildren().add(Fx.card(
                Fx.sectionTitle("Patient Registration"),
                Fx.muted("Fields marked * are required. The mobile number is used to find the patient later."),
                form,
                Fx.row(10, register, clear),
                message));
        setMaxWidth(640);
    }

    private void showMessage(String text, boolean isError) {
        message.setText(text);
        message.getStyleClass().removeAll("msg-error", "msg-success");
        message.getStyleClass().add(isError ? "msg-error" : "msg-success");
    }

    private void clearForm() {
        name.clear();
        age.clear();
        gender.setValue(null);
        phone.clear();
        email.clear();
        bloodGroup.setValue("Unknown");
        address.clear();
    }
}
