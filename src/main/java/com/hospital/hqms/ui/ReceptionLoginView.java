package com.hospital.hqms.ui;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Staff;
import com.hospital.hqms.service.AuthService;
import com.hospital.hqms.ui.components.Fx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ReceptionLoginView implements View {

    private final VBox root = new VBox();
    private final AuthService authService = new AuthService();

    public ReceptionLoginView(Navigator nav) {
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(70, 20, 40, 20));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = new Label();
        error.getStyleClass().add("msg-error");
        error.setWrapText(true);

        Button login = Fx.primary("Sign In");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);
        login.setOnAction(e -> {
            try {
                Staff staff = authService.login(username.getText(), password.getText());
                nav.showReceptionDashboard(staff);
            } catch (ValidationException ex) {
                error.setText(ex.getMessage());
                password.clear();
            }
        });

        GridPane form = Fx.formGrid();
        Fx.addRow(form, 0, "Username", username);
        Fx.addRow(form, 1, "Password", password);

        VBox card = Fx.card(
                Fx.sectionTitle("Reception Desk Login"),
                Fx.muted("Demo credentials:  reception / reception123"),
                form, login, error);
        card.setMaxWidth(440);
        root.getChildren().add(card);
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
