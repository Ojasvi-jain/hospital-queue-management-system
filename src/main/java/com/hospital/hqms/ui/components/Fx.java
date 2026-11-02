package com.hospital.hqms.ui.components;

import javafx.beans.property.SimpleStringProperty;
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

    /** Round coloured icon with a short text glyph (avoids relying on emoji fonts). */
    public static StackPane icon(String glyph, String accentClass) {
        Label l = new Label(glyph);
        l.getStyleClass().add("icon-glyph");
        StackPane p = new StackPane(l);
        p.getStyleClass().addAll("icon-circle", accentClass);
        p.setMinSize(52, 52);
        p.setMaxSize(52, 52);
        return p;
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
