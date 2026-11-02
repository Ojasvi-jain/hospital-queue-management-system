package com.hospital.hqms.ui;

import com.hospital.hqms.ui.components.BookingPane;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.PaymentPane;
import com.hospital.hqms.ui.components.RegistrationPane;
import com.hospital.hqms.ui.components.TrackPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

/** Self-service portal: Register -> Book -> Pay -> Track, each step in its own tab. */
public class PatientPortalView implements View {

    private final VBox root = new VBox(16);

    public PatientPortalView() {
        root.setPadding(new Insets(28, 36, 28, 36));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab registerTab = new Tab("1.  Register");
        Tab bookTab = new Tab("2.  Book Appointment");
        Tab payTab = new Tab("3.  Pay Fee");
        Tab trackTab = new Tab("4.  Track Token");

        TrackPane trackPane = new TrackPane();
        PaymentPane paymentPane = new PaymentPane(false, null);
        BookingPane bookingPane = new BookingPane(appointment -> {
            paymentPane.loadAppointment(appointment);
            tabs.getSelectionModel().select(payTab);
        });
        RegistrationPane registrationPane = new RegistrationPane(patient -> {
            bookingPane.setPhone(patient.getPhone());
            tabs.getSelectionModel().select(bookTab);
        });

        registerTab.setContent(padded(registrationPane));
        bookTab.setContent(padded(bookingPane));
        payTab.setContent(padded(paymentPane));
        trackTab.setContent(padded(trackPane));
        tabs.getTabs().addAll(registerTab, bookTab, payTab, trackTab);
        tabs.getSelectionModel().select(bookTab);

        root.getChildren().addAll(
                Fx.title("Patient Portal"),
                Fx.subtitle("New here? Start with Register. Already registered? Book using your mobile number."),
                tabs);
    }

    private static Node padded(Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(20, 0, 0, 0));
        return box;
    }

    @Override
    public Node getRoot() {
        return root;
    }
}
