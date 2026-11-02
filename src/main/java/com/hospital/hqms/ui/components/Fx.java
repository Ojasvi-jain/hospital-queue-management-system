package com.hospital.hqms.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Function;

/** Small factory methods that keep the view classes short and consistently styled. */
public final class Fx {

    public static final String STYLESHEET = Fx.class.getResource("/css/styles.css").toExternalForm();

    private Fx() {
    }

    /** Rounded, softly tinted square holding a vector icon - used on KPI cards and tiles. */
    public static StackPane iconChip(String iconPath, String accentClass, double size) {
        StackPane p = new StackPane(Icons.of(iconPath, size * 0.48));
        p.getStyleClass().addAll("icon-chip", accentClass);
        p.setMinSize(size, size);
        p.setMaxSize(size, size);
        return p;
    }

    /** Circle with a person's initials, e.g. "AM" for Anjali Mehta. */
    public static StackPane avatar(String name, double size) {
        String[] parts = name.replace("Dr. ", "").trim().split("\\s+");
        String initials = (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "")).toUpperCase();
        Label l = new Label(initials);
        l.getStyleClass().add("avatar-text");
        l.setStyle("-fx-font-size: " + Math.round(size * 0.38) + "px;");
        StackPane p = new StackPane(l);
        p.getStyleClass().addAll("avatar", "avatar-" + Math.abs(name.hashCode() % 6));
        p.setMinSize(size, size);
        p.setMaxSize(size, size);
        return p;
    }

    /**
     * KPI tile: icon, big number, caption and an optional trend line underneath.
     * The value label is passed in so the caller can update it later.
     */
    public static VBox kpi(String caption, Label value, String iconPath, String accentClass, Label trend) {
        value.getStyleClass().add("kpi-value");
        Label c = new Label(caption);
        c.getStyleClass().add("kpi-caption");
        HBox top = row(10, c, spacer(), iconChip(iconPath, accentClass, 38));
        VBox box = new VBox(6, top, value);
        if (trend != null) {
            trend.getStyleClass().add("kpi-trend");
            box.getChildren().add(trend);
        }
        box.getStyleClass().addAll("card", "kpi-card");
        box.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Card title row with an optional subtitle and right-aligned actions. */
    public static HBox cardHeader(String title, String subtitle, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        VBox text = new VBox(2, t);
        if (subtitle != null) {
            Label s = new Label(subtitle);
            s.getStyleClass().add("card-subtitle");
            s.setWrapText(true);
            text.getChildren().add(s);
        }
        HBox h = new HBox(10, text, spacer());
        h.getChildren().addAll(actions);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static Button iconButton(String text, String iconPath, String styleClass) {
        Button b = new Button(text);
        b.setGraphic(Icons.of(iconPath, 15));
        b.getStyleClass().add(styleClass);
        b.setGraphicTextGap(8);
        return b;
    }

    public static VBox emptyState(String iconPath, String title, String message) {
        Label t = new Label(title);
        t.getStyleClass().add("empty-title");
        Label m = muted(message);
        m.setAlignment(Pos.CENTER);
        VBox box = new VBox(8, iconChip(iconPath, "accent-slate", 48), t, m);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        return box;
    }

    /** Small notification that slides in at the bottom-right of the window and fades away. */
    public static void toast(Node anchor, String message, boolean success) {
        if (anchor == null || anchor.getScene() == null) {
            return;
        }
        Window window = anchor.getScene().getWindow();
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(320);
        HBox box = new HBox(10, Icons.of(success ? Icons.CHECK : Icons.WARNING, 18), text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("toast", success ? "toast-success" : "toast-error");
        box.getStylesheets().add(STYLESHEET);

        Popup popup = new Popup();
        popup.getContent().add(box);
        popup.setAutoFix(true);
        popup.show(window);
        popup.setX(window.getX() + window.getWidth() - box.getWidth() - 32);
        popup.setY(window.getY() + window.getHeight() - box.getHeight() - 32);

        FadeTransition in = new FadeTransition(Duration.millis(180), box);
        in.setFromValue(0);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(400), box);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(2.6)), out);
        seq.setOnFinished(e -> popup.hide());
        seq.play();
    }

    public static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    public static Label subtitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-subtitle");
        l.setWrapText(true);
        return l;
    }

    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    public static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(12, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        return box;
    }

    public static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        return b;
    }

    public static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-secondary");
        return b;
    }

    public static Button danger(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-danger");
        return b;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Two-column form grid: labels on the left, inputs stretching on the right. */
    public static GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints(130);
        ColumnConstraints inputs = new ColumnConstraints();
        inputs.setHgrow(Priority.ALWAYS);
        inputs.setFillWidth(true);
        g.getColumnConstraints().addAll(labels, inputs);
        return g;
    }

    public static void addRow(GridPane grid, int row, String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        grid.add(l, 0, row);
        grid.add(input, 1, row);
        if (input instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /** Big number + caption, used for dashboard statistics. */
    public static VBox statCard(String caption, Label value, String accentClass) {
        value.getStyleClass().add("stat-value");
        Label c = new Label(caption);
        c.getStyleClass().add("stat-caption");
        VBox box = new VBox(4, value, c);
        box.getStyleClass().addAll("card", "stat-card", accentClass);
        box.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Creates a read-only text column from any getter (method reference or lambda). */
    public static <S> TableColumn<S, String> column(String title, Function<S, Object> getter, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> {
            Object v = getter.apply(cd.getValue());
            return new SimpleStringProperty(v == null ? "" : v.toString());
        });
        col.setPrefWidth(width);
        col.setMinWidth(width * 0.85);
        return col;
    }

    /** Same as {@link #column} but renders the value as a coloured pill ("badge"). */
    public static <S> TableColumn<S, String> badgeColumn(String title, Function<S, Object> getter, double width) {
        TableColumn<S, String> col = column(title, getter, width);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    setGraphic(badge(item));
                }
            }
        });
        return col;
    }

    public static Label badge(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", "badge-" + text.toLowerCase().replaceAll("[^a-z]", ""));
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static HBox row(double spacing, Node... nodes) {
        HBox h = new HBox(spacing, nodes);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ----- Dialogs -----------------------------------------------------------------

    public static void error(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle("Error");
        a.setHeaderText(header);
        style(a);
        a.showAndWait();
    }

    public static void info(String header, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle("Information");
        a.setHeaderText(header);
        style(a);
        a.showAndWait();
    }

    public static boolean confirm(String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Please confirm");
        a.setHeaderText(header);
        style(a);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    private static void style(Alert a) {
        a.getDialogPane().getStylesheets().add(STYLESHEET);
    }
}
