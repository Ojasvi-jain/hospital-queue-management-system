package com.hospital.hqms.ui.reception;

import com.hospital.hqms.db.DatabaseManager;
import com.hospital.hqms.model.Appointment;
import com.hospital.hqms.model.Staff;
import com.hospital.hqms.service.QueueService;
import com.hospital.hqms.ui.AppShell;
import com.hospital.hqms.ui.Navigator;
import com.hospital.hqms.ui.View;
import com.hospital.hqms.ui.components.BookingPane;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import com.hospital.hqms.ui.components.PaymentPane;
import com.hospital.hqms.ui.components.RegistrationPane;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Receptionist portal. Owns the sidebar shell and the shared front-desk forms, and
 * offers small navigation helpers ({@link #openQueue}, {@link #openPayment}, ...) to its pages.
 */
public class ReceptionPortalView implements View {

    private final AppShell shell = new AppShell("Reception Desk");
    private final QueueService queueService = new QueueService();
    private final BookingPane bookingPane;
    private final PaymentPane paymentPane;
    private final RegistrationPane registrationPane;
    private int queueDoctorId = -1;

    public ReceptionPortalView(Navigator nav, Staff staff) {
        bookingPane = new BookingPane(this::openPayment);
        paymentPane = new PaymentPane(true, this::refreshBadges);
        registrationPane = new RegistrationPane(p -> openBooking(p.getPhone()));

        shell.addSection("Overview");
        shell.addItem("dashboard", "Dashboard", Icons.DASHBOARD, "Today's hospital activity at a glance",
                () -> new DashboardPage(this));
        shell.addItem("queue", "Live Queue", Icons.QUEUE, "Call patients in and manage each doctor's queue",
                () -> new LiveQueuePage(this, queueDoctorId));

        shell.addSection("Front desk");
        shell.addItem("register", "Register Patient", Icons.PERSON_ADD, "Create a record for a new patient",
                () -> registrationPane);
        shell.addItem("book", "Book Appointment", Icons.CALENDAR, "Schedule a visit and issue a token",
                () -> {
                    bookingPane.reloadDoctors();
                    return bookingPane;
                });
        shell.addItem("pay", "Collect Payment", Icons.CARD, "Take consultation fees by cash, card or UPI",
                () -> paymentPane);

        shell.addSection("Records");
        shell.addItem("appointments", "Appointments", Icons.QUEUE, "Search, filter, update and export appointments",
                () -> new AppointmentsPage(this));
        shell.addItem("patients", "Patients", Icons.PEOPLE, "Patient directory and visit history",
                () -> new PatientsPage(this));
        shell.addItem("doctors", "Doctors", Icons.STETHOSCOPE, "Doctor directory, OPD timings and availability",
                () -> new DoctorsPage(this, null));
        shell.addItem("transactions", "Transactions", Icons.RECEIPT, "Payments received and revenue analysis",
                () -> new TransactionsPage());

        Button board = Fx.iconButton("Display Board", Icons.TV, "btn-secondary");
        board.setOnAction(e -> nav.showQueueBoard(this));
        Button reset = Fx.iconButton("Reset Demo Data", Icons.REFRESH, "btn-ghost");
        reset.setOnAction(e -> {
            if (Fx.confirm("Reset all data?", "This removes every change and reloads the original sample data.")) {
                DatabaseManager.getInstance().resetDemoData();
                nav.showReceptionPortal(staff);
            }
        });
        shell.setTopActions(reset, board);

        Label name = new Label(staff.fullName());
        name.getStyleClass().add("user-name");
        Label role = new Label(staff.role().charAt(0) + staff.role().substring(1).toLowerCase());
        role.getStyleClass().add("user-role");
        Button logout = new Button();
        logout.setGraphic(Icons.of(Icons.LOGOUT, 18));
        logout.getStyleClass().add("sidebar-icon-button");
        logout.setOnAction(e -> nav.showHome());
        HBox user = new HBox(10, Fx.avatar(staff.fullName(), 38), new VBox(1, name, role), Fx.spacer(), logout);
        user.setAlignment(Pos.CENTER_LEFT);
        user.getStyleClass().add("user-card");
        shell.setFooter(user);

        refreshBadges();
        shell.navigate("dashboard");
    }

    // ---- navigation helpers used by the pages ------------------------------------

    public void openQueue(int doctorId) {
        queueDoctorId = doctorId;
        shell.navigate("queue");
    }

    void rememberQueueDoctor(int doctorId) {
        queueDoctorId = doctorId;
    }

    public void openBooking(String phone) {
        shell.navigate("book");
        bookingPane.setPhone(phone);
    }

    public void openPayment(Appointment appointment) {
        shell.navigate("pay");
        paymentPane.loadAppointment(appointment);
    }

    public void navigate(String page) {
        shell.navigate(page);
    }

    /** Updates the "waiting" counter shown next to Live Queue in the sidebar. */
    public void refreshBadges() {
        long waiting = queueService.waitingCountByDoctor(LocalDate.now()).values().stream()
                .mapToLong(Long::longValue).sum();
        shell.setBadge("queue", waiting == 0 ? null : String.valueOf(waiting));
    }

    @Override
    public Node getRoot() {
        return shell;
    }
}
