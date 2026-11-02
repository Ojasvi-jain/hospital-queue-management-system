package com.hospital.hqms;

import com.hospital.hqms.db.DatabaseManager;
import com.hospital.hqms.ui.Navigator;
import com.hospital.hqms.ui.components.Fx;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
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
        Scene scene = new Scene(navigator.getRoot(), 1360, 860);
        scene.getStylesheets().add(Fx.STYLESHEET);
        navigator.showHome();

        stage.setTitle("MediQueue - Hospital Queue Management System");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
