package com.hospital.hqms.ui;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.DashboardStats;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** Public landing page: hero section, live snapshot, entry points and departments. */
public class HomeView implements View {

    private final ScrollPane root = new ScrollPane();

    public HomeView(Navigator nav) {
        QueueService queueService = new QueueService();
        List<Doctor> doctors = new AppointmentService().getAvailableDoctors();
        DashboardStats stats = queueService.getDashboardStats(LocalDate.now());

        // ---- top navigation
        StackPane logo = new StackPane(Icons.of(Icons.HOSPITAL, 22));
        logo.getStyleClass().add("brand-logo");
        Label brand = new Label("MediQueue");
        brand.getStyleClass().add("home-brand");
        Button board = Fx.iconButton("Display Board", Icons.TV, "btn-ghost");
        board.setOnAction(e -> nav.showQueueBoard(this));
        Button staff = Fx.iconButton("Staff Login", Icons.SHIELD, "btn-secondary");
        staff.setOnAction(e -> nav.showReceptionLogin());
        Button portal = Fx.primary("Patient Portal");
        portal.setOnAction(e -> nav.showPatientPortal());
        HBox navbar = new HBox(12, logo, new VBox(0, brand, Fx.muted("City General Hospital, Patiala")),
                Fx.spacer(), board, staff, portal);
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.getStyleClass().add("home-nav");

        // ---- hero
        Label eyebrow = new Label("SMART OPD QUEUE MANAGEMENT");
        eyebrow.getStyleClass().add("eyebrow");
        Label headline = new Label("Skip the waiting room,\nnot the care.");
        headline.getStyleClass().add("hero-title");
        Label sub = new Label("Register once, book a specialist, pay online and follow your token live. "
                + "Our front desk sees every queue in real time, so you are called the moment your doctor is ready.");
        sub.getStyleClass().add("hero-subtitle");
        sub.setWrapText(true);
        sub.setMaxWidth(560);
        Button book = Fx.iconButton("Book an Appointment", Icons.CALENDAR, "btn-hero");
        book.setOnAction(e -> nav.showPatientPortal());
        Button track = Fx.iconButton("Track My Token", Icons.CLOCK, "btn-hero-outline");
        track.setOnAction(e -> nav.showPatientPortal());
        HBox trust = new HBox(36,
                heroStat(String.valueOf(doctors.size()), "Specialists on duty"),
                heroStat(String.valueOf(stats.todaysAppointments()), "Appointments today"),
                heroStat(String.valueOf(stats.totalPatients()), "Registered patients"));
        VBox heroText = new VBox(20, eyebrow, headline, sub, new HBox(12, book, track), trust);
        heroText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heroText, Priority.ALWAYS);

        // live snapshot card
        Label dot = new Label();
        dot.getStyleClass().add("live-dot");
        Label liveTitle = new Label("Live now");
        liveTitle.getStyleClass().add("live-title");
        VBox live = new VBox(10, Fx.row(8, dot, liveTitle, Fx.spacer(), Fx.muted(stats.waiting() + " patients waiting")));
        doctors.stream().limit(5).forEach(d -> {
            String token = queueService.getNowServing(d.getId(), LocalDate.now()).map(Appointment::getTokenLabel).orElse("—");
            Label t = new Label(token);
            t.getStyleClass().add("token-chip");
            Label n = new Label(d.getSpecialization());
            n.getStyleClass().add("cell-strong");
            HBox row = new HBox(10, Fx.avatar(d.getName(), 34), new VBox(0, n, Fx.muted(d.getName())), Fx.spacer(), t);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("live-row");
            live.getChildren().add(row);
        });
        Button seeAll = Fx.iconButton("See all queues", Icons.ARROW_RIGHT, "btn-link");
        seeAll.setContentDisplay(ContentDisplay.RIGHT);
        seeAll.setOnAction(e -> nav.showQueueBoard(this));
        live.getChildren().add(seeAll);
        live.getStyleClass().addAll("card", "live-card");
        live.setPadding(new Insets(22));
        live.setPrefWidth(400);
        live.setMinWidth(380);

        HBox hero = new HBox(48, heroText, live);
        hero.setAlignment(Pos.CENTER);
        hero.getStyleClass().add("hero");

        // ---- entry tiles
        GridPane tiles = new GridPane();
        tiles.setHgap(20);
        tiles.add(tile(Icons.CALENDAR, "accent-teal", "Patient Portal",
                "Register, book a specialist, pay the consultation fee and track your token.",
                "Get started", nav::showPatientPortal), 0, 0);
        tiles.add(tile(Icons.SHIELD, "accent-blue", "Reception Desk",
                "Dashboards, live queue control, walk-in booking, billing and records for staff.",
                "Staff login", nav::showReceptionLogin), 1, 0);
        tiles.add(tile(Icons.TV, "accent-amber", "Queue Display Board",
                "Waiting-hall screen showing the token each doctor is serving, updated live.",
                "Open board", () -> nav.showQueueBoard(this)), 2, 0);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            tiles.getColumnConstraints().add(cc);
        }

        // ---- departments
        FlowPane depts = new FlowPane(10, 10);
        doctors.forEach(d -> {
            Label chip = new Label(d.getSpecialization());
            chip.setGraphic(Icons.of(Icons.STETHOSCOPE, 14));
            chip.getStyleClass().add("dept-chip");
            depts.getChildren().add(chip);
        });

        Label footer = Fx.muted("MediQueue  •  Hospital Queue Management System  •  Java 17, JavaFX & JDBC (SQLite)  •  Runs fully offline");
        HBox footerBar = new HBox(footer);
        footerBar.setAlignment(Pos.CENTER);
        footerBar.getStyleClass().add("home-footer");

        VBox body = new VBox(36,
                hero,
                new VBox(16, sectionHeading("Choose your portal", "Everything runs locally on this computer"), tiles),
                new VBox(12, sectionHeading("Departments", "Specialist OPDs running today"), depts));
        body.setPadding(new Insets(36, 64, 44, 64));
        body.setMaxWidth(1320);
        StackPane centered = new StackPane(body);
        centered.setAlignment(Pos.TOP_CENTER);

        VBox page = new VBox(navbar, centered, footerBar);
        page.getStyleClass().add("home");
        root.setContent(page);
        root.setFitToWidth(true);
        root.getStyleClass().add("content-scroll");
        javafx.application.Platform.runLater(() -> root.setVvalue(0));
    }

    private static VBox heroStat(String value, String caption) {
        Label v = new Label(value);
        v.getStyleClass().add("hero-stat-value");
        Label c = new Label(caption);
        c.getStyleClass().add("hero-stat-caption");
        return new VBox(0, v, c);
    }

    private static VBox sectionHeading(String title, String subtitle) {
        Label t = new Label(title);
        t.getStyleClass().add("section-heading");
        return new VBox(2, t, Fx.muted(subtitle));
    }

    private static VBox tile(String icon, String accent, String title, String description, String action, Runnable onClick) {
        Label t = new Label(title);
        t.getStyleClass().add("tile-title");
        Label d = Fx.muted(description);
        d.setMinHeight(40);
        Button b = Fx.iconButton(action, Icons.ARROW_RIGHT, "btn-link");
        b.setContentDisplay(ContentDisplay.RIGHT);
        b.setOnAction(e -> onClick.run());
        VBox card = Fx.card(Fx.iconChip(icon, accent, 48), t, d, b);
        card.getStyleClass().add("tile");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
