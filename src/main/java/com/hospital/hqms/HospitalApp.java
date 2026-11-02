package com.hospital.hqms;

import com.hospital.hqms.db.DatabaseManager;
import com.hospital.hqms.ui.Navigator;
import com.hospital.hqms.ui.components.Fx;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** JavaFX entry point. Run with:  mvnw javafx:run */
public class HospitalApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.getInstance().initialize();
        } catch (RuntimeException e) {
            Fx.error("Database error", "Could not open the database:\n" + e.getMessage());
            Platform.exit();
            return;
        }

        // Any unexpected error on the UI thread is shown to the user instead of failing silently.
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Fx.error("Unexpected error", String.valueOf(e.getMessage()));
        });

        Navigator navigator = new Navigator();
        // Fit the window to the screen (laptops with display scaling have little vertical room).
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1440, screen.getWidth() - 40);
        double height = Math.min(900, screen.getHeight() - 40);
        Scene scene = new Scene(navigator.getRoot(), width, height);
        scene.getStylesheets().add(Fx.STYLESHEET);
        navigator.showHome();

        stage.setTitle("MediQueue - Hospital Queue Management System");
        stage.setMinWidth(Math.min(1200, width));
        stage.setMinHeight(Math.min(700, height));
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
