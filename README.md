# MediQueue: Hospital Queue Management System

A desktop application for a hospital OPD (out-patient department). Patients can find a specialist, register, book an appointment, pay the consultation fee online and follow their token live. Reception staff work from an admin dashboard that has charts, live queue control, billing and patient records. It is built with **Java 17, JavaFX and JDBC (SQLite)**.

It runs fully offline on your own machine. There is no cloud hosting, no database server to install and no external services.

▶ **[Watch the demo video](demo/MediQueue-demo.mp4)** (about 3 minutes, captioned). It covers patient registration, booking, payment and token tracking, then the reception dashboard, live queue, records and the display board.

---

## Screens at a glance

| Area | Screens |
|---|---|
| **Home** | Landing page with a live "Now serving" snapshot, today's numbers, the portal entry points and the list of departments |
| **Patient Portal** | Find a Doctor · Register · Book Appointment · Pay Fee · Track Token |
| **Reception Desk** (login) | Dashboard · Live Queue · Register Patient · Book Appointment · Collect Payment · Appointments · Patients · Doctors · Transactions |
| **Queue Display Board** | Dark, TV-style waiting-hall screen that refreshes every 5 seconds |

Both portals use the same layout: a dark navigation sidebar, a top bar with the page title, and a content area made of cards. All icons are vector (SVG paths), and short pop-up notifications confirm actions.

---

## Features

| Module | What it does |
|---|---|
| **Dashboard & analytics** | KPI cards with comparisons against yesterday (patients, today's appointments, waiting, completed, revenue). Charts: a 14-day patient-footfall area chart, a donut of today's statuses, a 7-day revenue bar chart, a department load list with progress bars, and a live summary of every doctor's queue. |
| **Patient registration** | Validated form (name, age, gender, 10-digit mobile, email, blood group). The mobile number must be unique. Each patient gets an ID such as `P0181`. |
| **Doctor directory** | Cards for 12 specialists showing qualification, room, OPD timings, slot length, fee and today's progress. Patients can book straight from a card. Staff can mark a doctor *On leave* or *Available*. |
| **Appointment booking** | Pick a doctor, a date (up to 30 days ahead) and a free time slot. Slots that are already taken are hidden. Senior citizens (60+) get priority automatically. |
| **Token generation** | A token is generated in a database transaction, per doctor per day, e.g. `GEN-007`, `CAR-002`. |
| **Priority queue** | Patients are called in this order: Emergency, then Senior Citizen, then Normal. Within the same priority, the lower token number goes first. |
| **Live queue control** | A list of doctors with their waiting counts, a "Now serving" banner, and the actions Call Next, Mark Completed, Send Back, No-Show, Cancel (paid fees are marked refunded), Set Priority and Collect Fee. |
| **Queue status tracking** | A patient enters their mobile number to see their position, how many people are ahead, the token being served now and an estimated wait. |
| **Simulated payment** | Card (with Luhn, expiry and CVV checks), UPI or Cash (reception only). The gateway call runs on a background thread, then a receipt with a transaction ID is shown. |
| **Appointments** | Search by patient, phone, token or doctor, and filter by status, doctor and date. Columns can be sorted. Status changes follow allowed transitions only, so for example *Completed → Waiting* is rejected. The visible rows can be exported to **CSV**. |
| **Patients** | A searchable directory. Selecting a patient shows a profile panel with their details, visit statistics, recent visits, a quick "Book" button and a "Collect pending fee" button. |
| **Transactions** | Revenue KPIs, a donut of payment methods, a revenue-by-department bar chart, and a searchable list of every payment. |
| **Queue display board** | Shows the "Now serving" token and the next three tokens for every doctor, with a clock and a scrolling notice bar. |

### Sample data
Running *Reset Demo Data* (on the Reception Desk) restores the following:

- **12 doctors** in 12 departments. One of them (Psychiatry) is *on leave*.
- **180 patients**, with ages from infants to over 80, including about 35 registered this week.
- **About 1,050 appointments**:
  - **two weeks of history** (mostly completed, plus some no-shows and cancellations),
  - **about 70 for today**, with live queues for every doctor,
  - upcoming bookings for the next 6 days.
- **About 850 payments** made by UPI, card and cash.

All dates are relative to the day the database is created, so there is always a realistic "today".

---

## Tech stack

- **Java 17** (records, switch expressions, text blocks, pattern matching for `instanceof`)
- **JavaFX 21** for the UI. Views are built in code (no FXML) and styled with one CSS theme (`src/main/resources/css/styles.css`). Charts use JavaFX's built-in `AreaChart`, `BarChart` and `PieChart`.
- **JDBC** with **SQLite** (`org.xerial:sqlite-jdbc`). The database is a single file at `data/hospital.db`.
- **Maven**, through the included **Maven Wrapper** (`mvnw` / `mvnw.cmd`). You do not need to install Maven.

---

## Setup and running

### Prerequisites
- **JDK 17 or newer** (e.g. [Eclipse Temurin](https://adoptium.net/)). Check with `java -version`.
- Internet access **the first time only**, so the wrapper can download Maven, JavaFX and the SQLite driver. After that, everything runs offline.

### Run

```bash
git clone https://github.com/Ojasvi-jain/hospital-queue-management-system.git
cd hospital-queue-management-system
```

Windows (PowerShell):
```bash
.\mvnw.cmd javafx:run
```

Windows (Command Prompt):
```bash
mvnw.cmd javafx:run
```

macOS / Linux:
```bash
./mvnw javafx:run
```

On the first launch the app creates `data/hospital.db`, runs `schema.sql`, and loads `data.sql`.

### Running from an IDE
Import the folder as a **Maven project** in IntelliJ IDEA, Eclipse or VS Code, then run the Maven goal **`javafx:run`**. In IntelliJ this is under Maven → Plugins → javafx.

Running `HospitalApp` directly with the ▶ button gives the error *"JavaFX runtime components are missing"*, because JavaFX has to be on the module path. Use the Maven goal instead.

### Demo logins

| Role | Username | Password |
|---|---|---|
| Receptionist | `reception` | `reception123` |
| Administrator | `admin` | `admin123` |

### Test payment details

| Method | Succeeds | Fails (to demo error handling) |
|---|---|---|
| Card | `4111 1111 1111 1111`, expiry `12/29`, CVV `123` | `4000 0000 0000 0002` (declined) |
| UPI | `anything@okaxis` | any ID ending in `@fail` |
| Cash | Reception desk only | n/a |

Sample patient mobile numbers: `9876543210` (Aarav Patel), `9812345678` (Diya Malhotra), `9988776655` (Kabir Khanna, senior), `9345678123` (Meera Iyer, senior).

---

## Database setup

No manual setup is needed. The scripts live in `src/main/resources/db/`:

- **`schema.sql`** creates the tables `staff`, `doctors`, `patients`, `appointments` and `payments`, with foreign keys, CHECK/UNIQUE constraints and indexes.
- **`data.sql`** inserts the sample data. Every date is written relative to today, e.g. `date('now','localtime','-3 day')`.

```
patients 1 ──── * appointments * ──── 1 doctors
                     │
                     1
                     │
                     * payments          staff (logins)
```

To start fresh, click **Reset Demo Data** in the reception desk, or close the app, delete the `data/` folder and relaunch.
To inspect the data, open `data/hospital.db` in any SQLite browser (e.g. *DB Browser for SQLite*).

---

## Suggested demo flow (about 7 minutes)

1. **Home**: point out the live "Now serving" card, today's numbers and the three portals.
2. **Patient Portal → Find a Doctor**: browse the doctor cards and click **Book Appointment** on one.
3. **Book**: enter `9876543210`, pick a slot and book. A token such as `DER-006` is issued. (Or **Register** a new patient aged 65 to show the automatic *Senior Citizen* priority.)
4. **Proceed to Payment**: first try the declined card (an error is shown), then pay with `4111…` and show the receipt.
5. **Track Token**: show the queue position, the number of people ahead and the estimated wait.
6. **Reception Desk** (login) **→ Dashboard**: walk through the KPIs, the footfall trend, the status donut, the department load and the revenue chart.
7. **Live Queue**: pick a doctor and click **Call Next Patient** (an emergency patient jumps the queue). Mark a patient as No-Show, then cancel a paid appointment (it becomes *Refunded*).
8. **Appointments**: search and filter, try an invalid status change to show the validation message, then **Export CSV**.
9. **Patients**: open a profile to see the visit history. **Doctors**: mark a doctor on leave. **Transactions**: show the revenue analytics.
10. **Display Board**: show the waiting-hall screen updating live.

---

## Project structure

```
src/main/java/com/hospital/hqms/
├── HospitalApp.java              JavaFX entry point
├── db/DatabaseManager.java       Singleton: JDBC connections, schema + sample data loader
├── model/                        Person (abstract) → Patient, Doctor; Appointment;
│                                 enums Priority, AppointmentStatus, PaymentStatus, PaymentMethod;
│                                 records Payment, Staff, QueueStatus, DashboardStats, LabelValue, LoadSummary
├── dao/                          Dao<T> interface, BaseDao<T> (shared JDBC code), PatientDao, DoctorDao,
│                                 AppointmentDao, PaymentDao, StaffDao, ReportDao (GROUP BY analytics)
├── service/                      PatientService, AppointmentService, QueueService (PriorityQueue),
│   │                             PaymentService, ReportService (chart series), AuthService
│   └── payment/                  PaymentProcessor interface + Card / Upi / Cash implementations
├── exception/                    HospitalException hierarchy + DataAccessException
├── util/                         Validator (regex, Luhn), PasswordUtil (SHA-256)
└── ui/
    ├── AppShell.java             Sidebar + top bar layout shared by both portals
    ├── Navigator.java            Switches between top-level screens
    ├── HomeView, PatientPortalView, ReceptionLoginView, QueueBoardView
    ├── reception/                ReceptionPortalView + Dashboard, LiveQueue, Appointments,
    │                             Patients, Doctors, Transactions pages
    └── components/               Fx (UI factory), Icons (SVG), Charts, and the reusable
                                  Registration / Booking / Payment / Track panes
src/main/resources/
├── db/schema.sql, db/data.sql
└── css/styles.css
```

The code follows a layered architecture: **UI → Service → DAO → Database**. The UI never runs SQL directly, and the DAOs contain no business rules.

---

## Java concepts demonstrated

| Concept | Where |
|---|---|
| **Abstraction & inheritance** | `Person` is an abstract class extended by `Patient` and `Doctor`. `BaseDao<T>` is an abstract DAO with an abstract `mapRow`. |
| **Encapsulation** | Private fields with getters and setters in every model class |
| **Polymorphism / Strategy pattern** | `PaymentProcessor` interface. `PaymentService.pay()` accepts any implementation (`CardPaymentProcessor`, `UpiPaymentProcessor`, `CashPaymentProcessor`). |
| **Interfaces & default methods** | `Dao<T>`, `PaymentProcessor` (default `newTransactionRef()`), `View` |
| **Generics** | `Dao<T>`, `BaseDao<T>`, generic `query(..., RowMapper<R>)`, `Fx.column(...)` |
| **Collections** | `PriorityQueue` with a `Comparator` (the queue), `EnumSet` (allowed status transitions), `HashSet` (booked slots), `LinkedHashMap` (ordered chart series), `TreeMap` + `groupingBy`, `List`, `Map`, `Optional` |
| **Streams & lambdas** | Slot filtering, duplicate-booking check, KPI calculations, `FilteredList` / `SortedList` predicates in tables |
| **Enums with fields & behaviour** | `Priority(rank, label)`, `AppointmentStatus.allowedNext()` / `canMoveTo()` |
| **Records (Java 16+)** | `Payment`, `Staff`, `QueueStatus`, `DashboardStats`, `LabelValue`, `LoadSummary`, `AppShell.NavItem` |
| **Exception handling** | Custom checked hierarchy: `HospitalException` → `ValidationException`, `SlotUnavailableException`, `PaymentException`. Unchecked `DataAccessException` wraps `SQLException`. Errors are shown in the UI as messages or pop-up notifications. |
| **JDBC** | `PreparedStatement` (prevents SQL injection), try-with-resources, **transactions** with commit/rollback (booking + token generation, payment + status update), foreign keys, **aggregate queries** (`GROUP BY`, `SUM`, `CASE WHEN`) for the dashboards |
| **File I/O** | CSV export of appointments with `BufferedWriter` / `Files.newBufferedWriter` |
| **Multithreading** | JavaFX `Task` runs the payment gateway call on a background thread. `Timeline` auto-refreshes the display board. Animations are used for the notifications. |
| **Design patterns** | Singleton (`DatabaseManager`), DAO, Strategy (payments), Factory method (`PaymentPane.createProcessor()`), layered MVC-style separation |
| **Security basics** | Passwords are stored as SHA-256 hashes, and all input is validated with regex |

---

## Troubleshooting

- **`JAVA_HOME` not set / wrong Java version**: install JDK 17+ and make sure `java -version` shows 17 or newer.
- **`mvnw.cmd` is not recognized** in PowerShell: run it as `.\mvnw.cmd javafx:run`.
- **The app shows old dates**: sample appointments are created relative to the day the database was first made. Click **Reset Demo Data** (reception desk) to regenerate today's queues.
- **Need a clean database**: close the app and delete the `data/` folder.
