package com.hospital.hqms.ui;

import com.hospital.hqms.model.Staff;
import com.hospital.hqms.ui.components.Fx;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Owns the main window layout (header + content area) and switches between screens. */
public class Navigator {

    private final BorderPane root = new BorderPane();
    private final HBox navButtons = new HBox(8);
    private View current;

    public Navigator() {
        Label logo = new Label("+");
        logo.getStyleClass().add("logo-mark");
        Label name = new Label("MediQueue");
        name.getStyleClass().add("app-name");
        Label hospital = new Label("City General Hospital  •  OPD Queue Management");
        hospital.getStyleClass().add("app-tagline");
        VBox brand = new VBox(0, name, hospital);

        Label today = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        today.getStyleClass().add("header-date");

        HBox header = Fx.row(12, logo, brand, Fx.spacer(), today, navButtons);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(14, 24, 14, 24));
        root.setTop(header);
        root.getStyleClass().add("app-root");
    }

    public BorderPane getRoot() {
        return root;
    }

    private void show(View view, boolean showHomeButton) {
        if (current != null) {
            current.onHide();
        }
        current = view;
        ScrollPane scroll = new ScrollPane(view.getRoot());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        root.setCenter(scroll);

        navButtons.getChildren().clear();
        if (showHomeButton) {
            Button home = new Button("Home");
            home.getStyleClass().add("nav-button");
            home.setOnAction(e -> showHome());
            navButtons.getChildren().add(home);
        }
    }

    public void showHome() {
        show(new HomeView(this), false);
    }

    public void showPatientPortal() {
        show(new PatientPortalView(), true);
    }

    public void showReceptionLogin() {
        show(new ReceptionLoginView(this), true);
    }

    public void showReceptionDashboard(Staff staff) {
        show(new ReceptionDashboardView(this, staff), true);
    }

    public void showQueueBoard() {
        show(new QueueBoardView(), true);
    }
}
