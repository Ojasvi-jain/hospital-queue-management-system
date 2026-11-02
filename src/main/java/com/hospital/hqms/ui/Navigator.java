package com.hospital.hqms.ui;

import com.hospital.hqms.model.Staff;
import com.hospital.hqms.ui.reception.ReceptionPortalView;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/** Switches the main window between the top-level screens. */
public class Navigator {

    private final StackPane root = new StackPane();
    private View current;

    public Parent getRoot() {
        return root;
    }

    public void show(View view) {
        if (current != null) {
            current.onHide();
        }
        current = view;
        root.getChildren().setAll(view.getRoot());
    }

    public void showHome() {
        show(new HomeView(this));
    }

    public void showPatientPortal() {
        show(new PatientPortalView(this));
    }

    public void showReceptionLogin() {
        show(new ReceptionLoginView(this));
    }

    public void showReceptionPortal(Staff staff) {
        show(new ReceptionPortalView(this, staff));
    }

    /** Opens the waiting-hall display; its Back button returns to {@code returnTo}. */
    public void showQueueBoard(View returnTo) {
        show(new QueueBoardView(() -> show(returnTo)));
    }
}
