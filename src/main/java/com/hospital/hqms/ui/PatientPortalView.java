package com.hospital.hqms.ui;

import com.hospital.hqms.ui.components.BookingPane;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import com.hospital.hqms.ui.components.PaymentPane;
import com.hospital.hqms.ui.components.RegistrationPane;
import com.hospital.hqms.ui.components.TrackPane;
import com.hospital.hqms.ui.reception.DoctorsPage;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Self-service portal: Find a doctor -> Register -> Book -> Pay -> Track. */
public class PatientPortalView implements View {

    private final AppShell shell = new AppShell("Patient Portal");

    public PatientPortalView(Navigator nav) {
        TrackPane trackPane = new TrackPane();
        PaymentPane paymentPane = new PaymentPane(false, null);
        BookingPane bookingPane = new BookingPane(appointment -> {
            shell.navigate("pay");
            paymentPane.loadAppointment(appointment);
        });
        RegistrationPane registrationPane = new RegistrationPane(patient -> {
            shell.navigate("book");
            bookingPane.setPhone(patient.getPhone());
        });

        shell.addSection("Get started");
        shell.addItem("doctors", "Find a Doctor", Icons.STETHOSCOPE, "Our specialists, OPD timings and fees",
                () -> new DoctorsPage(null, doctor -> {
                    shell.navigate("book");
                    bookingPane.selectDoctor(doctor.getId());
                }));
        shell.addItem("register", "Register", Icons.PERSON_ADD, "New here? Create your patient record in a minute",
                () -> registrationPane);
        shell.addSection("Your visit");
        shell.addItem("book", "Book Appointment", Icons.CALENDAR, "Choose a doctor, date and time - get your token instantly",
                () -> {
                    bookingPane.reloadDoctors();
                    return bookingPane;
                });
        shell.addItem("pay", "Pay Fee", Icons.CARD, "Pay the consultation fee online",
                () -> paymentPane);
        shell.addItem("track", "Track Token", Icons.CLOCK, "See your live position in the queue",
                () -> trackPane);

        Label help = new Label("Need help?");
        help.getStyleClass().add("user-name");
        Label phone = new Label("Front desk: 0175-2390000");
        phone.getStyleClass().add("user-role");
        Button home = Fx.iconButton("Back to Home", Icons.ARROW_LEFT, "sidebar-button");
        home.setMaxWidth(Double.MAX_VALUE);
        home.setOnAction(e -> nav.showHome());
        VBox footer = new VBox(4, help, phone, home);
        VBox.setMargin(home, new javafx.geometry.Insets(10, 0, 0, 0));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("user-card");
        shell.setFooter(footer);

        shell.navigate("book");
    }

    @Override
    public Node getRoot() {
        return shell;
    }
}
