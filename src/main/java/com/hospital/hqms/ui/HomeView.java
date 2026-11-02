package com.hospital.hqms.ui;

import com.hospital.hqms.model.DashboardStats;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.components.Fx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/** Landing screen: choose between the patient portal, reception desk and queue display board. */
public class HomeView implements View {

    private final VBox root = new VBox(28);

    public HomeView(Navigator nav) {
        root.setPadding(new Insets(40, 48, 40, 48));
        root.setAlignment(Pos.TOP_CENTER);

        Label heading = Fx.title("Welcome to City General Hospital");
        Label sub = Fx.subtitle("Register, book a doctor, pay online and track your token - no more standing in long queues.");

        HBox tiles = new HBox(24,
                tile("P", "accent-blue", "Patient Portal",
                        "Register as a new patient, book an appointment, pay the consultation fee and track your token.",
                        "Open Patient Portal", nav::showPatientPortal),
                tile("R", "accent-teal", "Reception Desk",
                        "Staff login to manage the live queues, call the next patient, update statuses and collect payments.",
                        "Staff Login", nav::showReceptionLogin),
                tile("Q", "accent-amber", "Queue Display Board",
                        "The waiting-hall screen showing which token each doctor is serving right now. Refreshes automatically.",
                        "Open Display Board", nav::showQueueBoard));
        tiles.setAlignment(Pos.CENTER);

        DashboardStats s = new QueueService().getDashboardStats(LocalDate.now());
        HBox stats = new HBox(16,
                Fx.statCard("Appointments today", new Label(String.valueOf(s.todaysAppointments())), "accent-blue"),
                Fx.statCard("Patients waiting now", new Label(String.valueOf(s.waiting())), "accent-amber"),
                Fx.statCard("Consultations completed", new Label(String.valueOf(s.completed())), "accent-green"));
        stats.setMaxWidth(980);

        root.getChildren().addAll(new VBox(6, heading, sub), tiles, stats);
        ((VBox) root.getChildren().get(0)).setAlignment(Pos.CENTER);
    }

    private VBox tile(String glyph, String accent, String title, String description, String action, Runnable onClick) {
        Node i = Fx.icon(glyph, accent);
        Label t = new Label(title);
        t.getStyleClass().add("tile-title");
        Label d = Fx.muted(description);
        d.setMinHeight(60);
        Button b = Fx.primary(action);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> onClick.run());

        VBox spacerBox = new VBox();
        VBox.setVgrow(spacerBox, Priority.ALWAYS);
        VBox card = Fx.card(i, t, d, spacerBox, b);
        card.getStyleClass().add("tile");
        card.setPrefWidth(310);
        card.setPrefHeight(270);
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
