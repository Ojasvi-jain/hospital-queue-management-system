package com.hospital.hqms.ui.reception;

import com.hospital.hqms.model.Doctor;
import com.hospital.hqms.model.LoadSummary;
import com.hospital.hqms.service.AppointmentService;
import com.hospital.hqms.service.ReportService;
import com.hospital.hqms.ui.components.Charts;
import com.hospital.hqms.ui.components.Fx;
import com.hospital.hqms.ui.components.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Doctor directory shown as cards. Staff see today's load and can toggle availability;
 * patients (when {@code onBook} is given) get a "Book" button instead.
 */
public class DoctorsPage extends VBox {

    private final AppointmentService appointmentService = new AppointmentService();
    private final ReportService reportService = new ReportService();
    private final ReceptionPortalView portal;
    private final Consumer<Doctor> onBook;
    private final FlowPane grid = new FlowPane(20, 20);

    /**
     * @param portal the reception portal (staff mode), or null for the patient portal
     * @param onBook called when a patient clicks "Book" on a card (patient mode)
     */
    public DoctorsPage(ReceptionPortalView portal, Consumer<Doctor> onBook) {
        super(16);
        this.portal = portal;
        this.onBook = onBook;
        grid.setPrefWrapLength(1000);
        getChildren().add(grid);
        reload();
    }

    private void reload() {
        Map<Integer, LoadSummary> loads = reportService.loadByDoctor(LocalDate.now());
        grid.getChildren().clear();
        for (Doctor d : portal == null ? appointmentService.getAvailableDoctors() : appointmentService.getAllDoctors()) {
            grid.getChildren().add(card(d, loads.getOrDefault(d.getId(), LoadSummary.EMPTY)));
        }
    }

    private VBox card(Doctor d, LoadSummary load) {
        Label name = new Label(d.getName());
        name.getStyleClass().add("doctor-name");
        Label status = new Label(d.isAvailable() ? "Available" : "On leave");
        status.getStyleClass().addAll("status-pill", d.isAvailable() ? "status-on" : "status-off");
        HBox head = new HBox(14, Fx.avatar(d.getName(), 54), new VBox(3, name, Fx.muted(d.getQualification())));
        head.setAlignment(Pos.CENTER_LEFT);

        VBox facts = new VBox(8,
                fact(Icons.STETHOSCOPE, d.getSpecialization()),
                fact(Icons.HOSPITAL, "Room " + d.getRoom()),
                fact(Icons.CLOCK, d.getTimingLabel() + "  •  " + d.getSlotMinutes() + " min slots"),
                fact(Icons.RUPEE, Charts.rupees(d.getConsultationFee()) + " consultation"));

        VBox card = Fx.card(Fx.row(8, Fx.badge(d.getDeptCode()), Fx.spacer(), status), head, facts);
        if (d.isAvailable()) {
            ProgressBar pb = new ProgressBar(load.progress());
            pb.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(
                    Fx.row(8, Fx.muted("Today"), Fx.spacer(),
                            Fx.muted(load.completed() + " seen  •  " + load.waiting() + " waiting")),
                    pb);
        }

        HBox actions = new HBox(8);
        if (portal != null) {
            if (d.isAvailable()) {
                Button queue = Fx.iconButton("Open Queue", Icons.QUEUE, "btn-primary");
                queue.setOnAction(e -> portal.openQueue(d.getId()));
                actions.getChildren().add(queue);
            }
            Button toggle = Fx.secondary(d.isAvailable() ? "Mark On Leave" : "Mark Available");
            toggle.setOnAction(e -> {
                if (d.isAvailable() && load.waiting() > 0
                        && !Fx.confirm("Patients are waiting", d.getName() + " still has " + load.waiting()
                        + " patient(s) waiting today. Mark on leave anyway?")) {
                    return;
                }
                appointmentService.setDoctorAvailable(d, !d.isAvailable());
                Fx.toast(this, d.getName() + (d.isAvailable() ? " marked on leave" : " is available again"), true);
                reload();
            });
            actions.getChildren().add(toggle);
        } else if (onBook != null) {
            Button book = Fx.iconButton("Book Appointment", Icons.CALENDAR, "btn-primary");
            book.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(book, javafx.scene.layout.Priority.ALWAYS);
            book.setOnAction(e -> onBook.accept(d));
            actions.getChildren().add(book);
        }
        card.getChildren().add(actions);
        card.setPrefWidth(318);
        card.setMaxWidth(318);
        card.getStyleClass().add("doctor-card");
        if (!d.isAvailable()) {
            card.getStyleClass().add("doctor-card-off");
        }
        return card;
    }

    private static HBox fact(String icon, String text) {
        HBox h = new HBox(10, Icons.of(icon, 15), new Label(text));
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("fact");
        return h;
    }
}
