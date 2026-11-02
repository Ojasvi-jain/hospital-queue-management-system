package com.hospital.hqms.ui.components;

import com.hospital.hqms.model.LabelValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builders for the JavaFX charts used on the dashboards, pre-styled to match the theme. */
public final class Charts {

    /** Palette shared by charts and their custom legends. */
    public static final String[] PALETTE = {"#0d9488", "#6366f1", "#f59e0b", "#ef4444", "#0ea5e9", "#a855f7", "#22c55e", "#64748b"};

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM");
    private static final NumberFormat INR = NumberFormat.getIntegerInstance(new Locale("en", "IN"));

    private Charts() {
    }

    /** Formats an amount with Indian digit grouping, e.g. 125000 becomes "₹1,25,000". */
    public static String rupees(double amount) {
        return "₹" + INR.format(Math.round(amount));
    }

    /** Smooth filled line chart for a per-day series. */
    public static Node areaTrend(Map<LocalDate, Double> data, String name, boolean money) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(true);
        y.setMinorTickVisible(false);
        AreaChart<String, Number> chart = new AreaChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(270);
        chart.setVerticalGridLinesVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        data.forEach((day, v) -> series.getData().add(new XYChart.Data<>(day.format(DAY_FMT), v)));
        chart.getData().add(series);
        installTooltips(series, money);
        chart.getStyleClass().add("trend-chart");
        return chart;
    }

    /** Vertical bar chart for a per-day series. */
    public static Node barTrend(Map<LocalDate, Double> data, boolean money) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(270);
        chart.setCategoryGap(22);
        chart.setVerticalGridLinesVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((day, v) -> series.getData().add(new XYChart.Data<>(day.format(DAY_FMT), v)));
        chart.getData().add(series);
        installTooltips(series, money);
        return chart;
    }

    /** Horizontal-category bar chart for labelled values (e.g. revenue per department). */
    public static Node bars(List<LabelValue> data, boolean money) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(280);
        chart.setCategoryGap(14);
        chart.setVerticalGridLinesVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach(lv -> series.getData().add(new XYChart.Data<>(lv.label(), lv.value())));
        chart.getData().add(series);
        installTooltips(series, money);
        return chart;
    }

    /**
     * Donut chart (a pie with a hole) with the total in the middle and a custom legend,
     * so slice colours and legend colours always match.
     */
    public static Node donut(List<LabelValue> data, String centerCaption, boolean money) {
        PieChart pie = new PieChart();
        pie.setLegendVisible(false);
        pie.setLabelsVisible(false);
        pie.setAnimated(false);
        pie.setStartAngle(90);
        pie.setPrefSize(220, 220);
        pie.setMinSize(220, 220);
        pie.setMaxSize(220, 220);
        pie.getStyleClass().add("donut");

        double total = data.stream().mapToDouble(LabelValue::value).sum();
        VBox legend = new VBox(10);
        for (int i = 0; i < data.size(); i++) {
            LabelValue lv = data.get(i);
            PieChart.Data slice = new PieChart.Data(lv.label(), lv.value());
            pie.getData().add(slice);
            String color = PALETTE[i % PALETTE.length];
            Runnable styleSlice = () -> {
                slice.getNode().setStyle("-fx-pie-color: " + color + ";");
                Tooltip.install(slice.getNode(), new Tooltip(lv.label() + ": " + format(lv.value(), money)));
            };
            if (slice.getNode() != null) {
                styleSlice.run();
            } else {
                slice.nodeProperty().addListener((obs, o, n) -> styleSlice.run());
            }

            Region dot = new Region();
            dot.getStyleClass().add("legend-dot");
            dot.setStyle("-fx-background-color: " + color + ";");
            Label name = new Label(lv.label());
            Label value = new Label(format(lv.value(), money));
            value.getStyleClass().add("cell-strong");
            Label share = Fx.muted(total == 0 ? "" : Math.round(lv.value() * 100 / total) + "%");
            share.setMinWidth(38);
            share.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(8, dot, name, Fx.spacer(), value, share);
            row.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(row);
        }

        Circle hole = new Circle(62);
        hole.getStyleClass().add("donut-hole");
        Label totalLabel = new Label(format(total, money));
        totalLabel.getStyleClass().add("donut-total");
        Label caption = Fx.muted(centerCaption);
        VBox center = new VBox(0, totalLabel, caption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);
        StackPane donut = new StackPane(pie, hole, center);
        hole.setMouseTransparent(true);

        if (data.isEmpty()) {
            return Fx.emptyState(Icons.DASHBOARD, "No data yet", "Nothing to show for this period.");
        }
        VBox box = new VBox(16, donut, legend);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /** Lays cards out side by side with the given column widths in percent. */
    public static GridPane grid(double[] percents, Node... cells) {
        GridPane g = new GridPane();
        g.setHgap(20);
        for (int i = 0; i < percents.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percents[i]);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            g.getColumnConstraints().add(cc);
            g.add(cells[i], i, 0);
            if (cells[i] instanceof Region r) {
                r.setMaxHeight(Double.MAX_VALUE);
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return g;
    }

    private static void installTooltips(XYChart.Series<String, Number> series, boolean money) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    Tooltip.install(n, new Tooltip(d.getXValue() + ": " + format(d.getYValue().doubleValue(), money)));
                }
            });
            if (d.getNode() != null) {
                Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + ": " + format(d.getYValue().doubleValue(), money)));
            }
        }
    }

    private static String format(double v, boolean money) {
        return money ? rupees(v) : String.valueOf(Math.round(v));
    }
}
