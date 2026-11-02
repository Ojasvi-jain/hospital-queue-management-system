package com.hospital.hqms.ui;

import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.components.Fx;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Waiting-hall display: the token each doctor is serving and who is next.
 * A JavaFX {@link Timeline} re-reads the queue every few seconds.
 */
public class QueueBoardView implements View {

    private static final int REFRESH_SECONDS = 5;

    private final QueueService queueService = new QueueService();
    private final AppointmentService appointmentService = new AppointmentService();

    private final VBox root = new VBox(20);
    private final TilePane grid = new TilePane(20, 20);
    private final Label clock = new Label();
    private final Timeline timeline;

    public QueueBoardView() {
        root.setPadding(new Insets(28, 36, 36, 36));
        root.getStyleClass().add("board-root");

        clock.getStyleClass().add("board-clock");
        root.getChildren().addAll(
                Fx.row(12, new VBox(4, Fx.title("Now Serving"),
                        Fx.muted("Please wait for your token to appear. This screen updates every " + REFRESH_SECONDS + " seconds.")),
                        Fx.spacer(), clock),
                grid);

        grid.setPrefColumns(3);
        grid.setPrefTileWidth(340);
        grid.setTileAlignment(Pos.TOP_LEFT);

        refresh();
        timeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS), e -> refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void refresh() {
        clock.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        LocalDate today = LocalDate.now();
        grid.getChildren().clear();
        for (Doctor d : appointmentService.getAvailableDoctors()) {
            grid.getChildren().add(doctorTile(d, today));
        }
    }

    private Node doctorTile(Doctor d, LocalDate today) {
        String serving = queueService.getNowServing(d.getId(), today).map(Appointment::getTokenLabel).orElse("—");
        List<Appointment> waiting = queueService.getWaitingList(d.getId(), today);
        String next = waiting.isEmpty() ? "No one waiting"
                : waiting.stream().limit(3).map(Appointment::getTokenLabel).collect(Collectors.joining("   "));

        Label token = new Label(serving);
        token.getStyleClass().add("board-token");
        Label name = new Label(d.getName());
        name.getStyleClass().add("doctor-name");
        Label nextLabel = new Label("Next:  " + next);
        nextLabel.getStyleClass().add("board-next");

        VBox tokenBox = new VBox(token);
        tokenBox.setAlignment(Pos.CENTER);
        tokenBox.getStyleClass().add("board-token-box");

        VBox card = Fx.card(
                Fx.row(8, Fx.badge(d.getSpecialization()), Fx.spacer(), Fx.muted(d.getRoom())),
                name, tokenBox, nextLabel,
                Fx.muted(waiting.size() + " waiting"));
        card.setPrefWidth(340);
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
