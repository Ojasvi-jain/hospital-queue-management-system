package com.hospital.hqms.ui;

import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Admin-style application frame: a dark navigation sidebar on the left, a top bar with
 * the page title, and a scrollable content area. Both portals are built from it.
 */
public class AppShell extends BorderPane {

    /** One entry in the sidebar. The page is created lazily by the supplier on every visit. */
    private record NavItem(HBox button, Label badge, Supplier<Node> page, String title, String subtitle) {
    }

    private final VBox navList = new VBox(2);
    private final VBox footer = new VBox();
    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final HBox topActions = new HBox(10);
    private final ScrollPane scroll = new ScrollPane();
    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private String currentId;

    public AppShell(String portalName) {
        getStyleClass().add("app-shell");

        // ---- sidebar
        Region logoIcon = Icons.of(Icons.HOSPITAL, 22);
        StackPane logo = new StackPane(logoIcon);
        logo.getStyleClass().add("brand-logo");
        Label name = new Label("MediQueue");
        name.getStyleClass().add("brand-name");
        Label portal = new Label(portalName);
        portal.getStyleClass().add("brand-portal");
        HBox brand = new HBox(12, logo, new VBox(0, name, portal));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox sidebar = new VBox(brand, navList, grow, footer);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        setLeft(sidebar);

        // ---- top bar
        pageTitle.getStyleClass().add("topbar-title");
        pageSubtitle.getStyleClass().add("topbar-subtitle");
        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")));
        date.getStyleClass().add("date-chip");
        date.setGraphic(Icons.of(Icons.CALENDAR, 14));
        topActions.setAlignment(Pos.CENTER_RIGHT);
        HBox topbar = new HBox(16, new VBox(2, pageTitle, pageSubtitle), Fx.spacer(), topActions, date);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");

        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        BorderPane main = new BorderPane(scroll);
        main.setTop(topbar);
        setCenter(main);
    }

    public void addSection(String caption) {
        Label l = new Label(caption.toUpperCase());
        l.getStyleClass().add("nav-section");
        navList.getChildren().add(l);
    }

    public void addItem(String id, String label, String icon, String subtitle, Supplier<Node> page) {
        Label text = new Label(label);
        Label badge = new Label();
        badge.getStyleClass().add("nav-badge");
        badge.setVisible(false);
        badge.setManaged(false);
        HBox button = new HBox(12, Icons.of(icon, 18), text, Fx.spacer(), badge);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-item");
        button.setOnMouseClicked(e -> navigate(id));
        navList.getChildren().add(button);
        items.put(id, new NavItem(button, badge, page, label, subtitle));
    }

    /** Small counter pill next to a sidebar item, e.g. number of patients waiting. */
    public void setBadge(String id, String text) {
        Label badge = items.get(id).badge();
        boolean show = text != null && !text.isEmpty();
        badge.setText(show ? text : "");
        badge.setVisible(show);
        badge.setManaged(show);
    }

    public void setFooter(Node node) {
        footer.getChildren().setAll(node);
    }

    public void setTopActions(Node... nodes) {
        topActions.getChildren().setAll(nodes);
    }

    public void navigate(String id) {
        NavItem item = items.get(id);
        if (item == null) {
            return;
        }
        if (currentId != null) {
            items.get(currentId).button().getStyleClass().remove("active");
        }
        currentId = id;
        item.button().getStyleClass().add("active");
        pageTitle.setText(item.title());
        pageSubtitle.setText(item.subtitle());

        VBox content = new VBox(item.page().get());
        content.getStyleClass().add("page");
        content.setPadding(new Insets(24, 28, 32, 28));
        scroll.setContent(content);
        scroll.setVvalue(0);
    }

    public String currentPage() {
        return currentId;
    }
}
