package com.hospital.hqms.ui;

import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Staff;
import com.hospital.hqms.service.AuthService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Split-screen staff sign-in: brand panel on the left, form on the right. */
public class ReceptionLoginView implements View {

    private final HBox root = new HBox();
    private final AuthService authService = new AuthService();

    public ReceptionLoginView(Navigator nav) {
        root.getStyleClass().add("login-root");

        // ---- brand panel
        StackPane logo = new StackPane(Icons.of(Icons.HOSPITAL, 26));
        logo.getStyleClass().add("brand-logo-large");
        Label brand = new Label("MediQueue");
        brand.getStyleClass().add("login-brand");
        Label title = new Label("Reception Desk");
        title.getStyleClass().add("login-hero");
        Label sub = new Label("Run every OPD queue from one screen - call patients, handle walk-ins, collect fees and keep records up to date.");
        sub.getStyleClass().add("login-sub");
        sub.setWrapText(true);
        VBox features = new VBox(14,
                feature(Icons.QUEUE, "Live priority queues for every doctor"),
                feature(Icons.DASHBOARD, "Dashboards with real-time hospital KPIs"),
                feature(Icons.CARD, "Cash, card and UPI fee collection"),
                feature(Icons.SHIELD, "Role-based staff access, hashed passwords"));
        VBox brandPanel = new VBox(26, Fx.row(12, logo, brand), new VBox(10, title, sub), features);
        brandPanel.getStyleClass().add("login-brand-panel");
        brandPanel.setPadding(new Insets(56));
        brandPanel.setPrefWidth(520);
        brandPanel.setAlignment(Pos.CENTER_LEFT);

        // ---- form
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
                nav.showReceptionPortal(staff);
            } catch (ValidationException ex) {
                error.setText(ex.getMessage());
                password.clear();
            }
        });
        Button back = Fx.iconButton("Back to home", Icons.ARROW_LEFT, "btn-link");
        back.setOnAction(e -> nav.showHome());

        Label formTitle = new Label("Welcome back");
        formTitle.getStyleClass().add("form-title");
        Label hint = new Label("Demo accounts:  reception / reception123   •   admin / admin123");
        hint.getStyleClass().add("hint-box");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(14,
                back,
                new VBox(4, formTitle, Fx.muted("Sign in with your staff account to continue.")),
                hint,
                new VBox(6, Fx.muted("Username"), username),
                new VBox(6, Fx.muted("Password"), password),
                login, error);
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER_LEFT);
        StackPane formPanel = new StackPane(form);
        formPanel.getStyleClass().add("login-form-panel");
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        root.getChildren().addAll(brandPanel, formPanel);
        javafx.application.Platform.runLater(username::requestFocus);
    }

    private static HBox feature(String icon, String text) {
        StackPane chip = new StackPane(Icons.of(icon, 16));
        chip.getStyleClass().add("login-feature-icon");
        Label l = new Label(text);
        l.getStyleClass().add("login-feature");
        HBox h = new HBox(12, chip, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
