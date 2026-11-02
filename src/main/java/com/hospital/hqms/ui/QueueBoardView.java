package com.hospital.hqms.ui;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Waiting-hall TV screen (dark theme): the token each doctor is serving and who is next.
 * A JavaFX {@link Timeline} re-reads the queues every few seconds.
 */
public class QueueBoardView implements View {

    private static final int REFRESH_SECONDS = 5;

    private final QueueService queueService = new QueueService();
    private final AppointmentService appointmentService = new AppointmentService();

    private final BorderPane root = new BorderPane();
    private final TilePane grid = new TilePane(18, 18);
    private final Label clock = new Label();
    private final Label date = new Label();
    private final Timeline timeline;

    public QueueBoardView(Runnable onBack) {
        root.getStyleClass().add("board-root");

        StackPane logo = new StackPane(Icons.of(Icons.HOSPITAL, 24));
        logo.getStyleClass().add("brand-logo");
        Label title = new Label("City General Hospital");
        title.getStyleClass().add("board-title");
        Label subtitle = new Label("OPD  •  Now Serving");
        subtitle.getStyleClass().add("board-subtitle");
        clock.getStyleClass().add("board-clock");
        date.getStyleClass().add("board-date");
        Button back = Fx.iconButton("Exit", Icons.ARROW_LEFT, "board-exit");
        back.setOnAction(e -> onBack.run());
        VBox time = new VBox(0, clock, date);
        time.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(14, logo, new VBox(0, title, subtitle), Fx.spacer(), time, back);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("board-header");

        grid.setPrefColumns(4);
        grid.setPrefTileWidth(300);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(24, 32, 24, 32));
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("board-scroll");

        Label ticker = new Label("Please keep your token number ready  •  Emergency and senior citizen patients are seen on priority  "
                + "•  Pay your consultation fee online in the Patient Portal  •  Maintain silence in the waiting area");
        ticker.getStyleClass().add("board-ticker");
        HBox tickerBar = new HBox(ticker);
        tickerBar.getStyleClass().add("board-ticker-bar");
        tickerBar.setAlignment(Pos.CENTER_LEFT);

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(tickerBar);

        refresh();
        timeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS), e -> refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void refresh() {
        clock.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        date.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")));
        LocalDate today = LocalDate.now();
        grid.getChildren().clear();
        for (Doctor d : appointmentService.getAvailableDoctors()) {
            grid.getChildren().add(doctorTile(d, today));
        }
    }

    private Node doctorTile(Doctor d, LocalDate today) {
        String serving = queueService.getNowServing(d.getId(), today).map(Appointment::getTokenLabel).orElse("—");
        List<Appointment> waiting = queueService.getWaitingList(d.getId(), today);

        Label dept = new Label(d.getSpecialization().toUpperCase());
        dept.getStyleClass().add("board-dept");
        Label room = new Label(d.getRoom());
        room.getStyleClass().add("board-room");
        Label name = new Label(d.getName());
        name.getStyleClass().add("board-doctor");

        Label token = new Label(serving);
        token.getStyleClass().add("board-token");
        VBox tokenBox = new VBox(token);
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("board-token-box");

        HBox next = new HBox(8);
        next.setAlignment(Pos.CENTER_LEFT);
        Label nextCaption = new Label("NEXT");
        nextCaption.getStyleClass().add("board-next-caption");
        next.getChildren().add(nextCaption);
        if (waiting.isEmpty()) {
            Label none = new Label("No one waiting");
            none.getStyleClass().add("board-next-none");
            next.getChildren().add(none);
        }
        waiting.stream().limit(3).forEach(a -> {
            Label chip = new Label(a.getTokenLabel());
            chip.getStyleClass().add("board-next-chip");
            next.getChildren().add(chip);
        });

        Label count = new Label(waiting.size() + " waiting");
        count.getStyleClass().add("board-count");
        VBox card = new VBox(12, new HBox(8, dept, Fx.spacer(), room), name, tokenBox, next, count);
        card.getStyleClass().add("board-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(300);
        return card;
    }

    @Override
    public void onHide() {
        timeline.stop();
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
