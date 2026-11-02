# MediQueue: Hospital Queue Management System

A desktop application for a hospital OPD (out-patient department). Patients can register, book a doctor, pay the consultation fee and track their token. The receptionist manages the live queues from a dashboard. It is built with **Java 17, JavaFX and JDBC (SQLite)** as a Java Lab project.

It runs fully offline on your own machine. There is no cloud hosting, no database server to install and no external services.

---

## Features

| Module | What it does |
|---|---|
| **Patient registration** | Validated form (name, age, gender, 10-digit mobile, email, blood group). The mobile number must be unique. Each patient gets an ID such as `P0009`. |
| **Appointment booking** | Choose a doctor and see their specialization, timings, room and fee. Pick a date (up to 30 days ahead) and a free time slot. Slots that are already booked are hidden. |
| **Token generation** | A token is generated in a database transaction, per doctor per day, e.g. `GEN-007`, `CAR-002`. |
| **Priority queue** | Patients are called in this order: Emergency, then Senior Citizen (age 60+ is applied automatically), then Normal. Within the same priority, the lower token number goes first. |
| **Queue status tracking** | A patient enters their mobile number to see their queue position, the number of people ahead, the token being served now and an estimated wait. |
| **Simulated payment** | Card (with Luhn check, expiry and CVV checks), UPI or Cash (reception only). The gateway call runs on a background thread, then a receipt with a transaction ID is shown. |
| **Reception desk** | Login. Summary cards (patients, today's appointments, waiting, completed, money collected today). Live queue per doctor: Call Next, Complete, Send Back, No-Show, Cancel (paid fees are marked refunded), change priority, collect fee. |
| **Appointment management** | A searchable and filterable table of all appointments. Status changes follow allowed transitions only, so for example *Completed → Waiting* is rejected. |
| **Walk-ins** | The receptionist can register and book for walk-in patients using the same forms as the patient portal. |
| **Queue display board** | A waiting-hall screen showing the "Now Serving" token and the next tokens for every doctor. It refreshes every 5 seconds. |
| **Sample data** | 6 doctors, 8 patients and a live queue for **today**. *Reset Demo Data* restores everything. |

---

## Tech stack

- **Java 17** (records, switch expressions, text blocks, pattern matching for `instanceof`)
- **JavaFX 21** for the UI. Views are built in code and styled with CSS (`src/main/resources/css/styles.css`).
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

Windows:
```bash
mvnw.cmd javafx:run
```

macOS / Linux:
```bash
./mvnw javafx:run
```

On the first launch the app creates `data/hospital.db`, runs `schema.sql`, and loads `data.sql`.

### Demo login

| Role | Username | Password |
|---|---|---|
| Receptionist | `reception` | `reception123` |

### Test payment details

| Method | Succeeds | Fails (to demo error handling) |
|---|---|---|
| Card | `4111 1111 1111 1111`, expiry `12/29`, CVV `123` | `4000 0000 0000 0002` (declined) |
| UPI | `anything@okaxis` | any ID ending in `@fail` |
| Cash | Reception desk only | n/a |

Sample patient mobile numbers: `9876543210`, `9812345678`, `9988776655` (senior), `9345678123` (senior).

### Running from an IDE
Import the folder as a **Maven project** in IntelliJ IDEA, Eclipse or VS Code. Then run the Maven goal `javafx:run`, or run `com.hospital.hqms.HospitalApp`.

---

## Database setup

No manual setup is needed. The scripts live in `src/main/resources/db/`:

- **`schema.sql`** creates the tables `staff`, `doctors`, `patients`, `appointments` and `payments`, with foreign keys, CHECK/UNIQUE constraints and indexes.
- **`data.sql`** inserts the sample data. Appointment dates use `date('now','localtime')`, so there is always a queue for *today*.

```
patients 1 ──── * appointments * ──── 1 doctors
                     │
                     1
                     │
                     * payments          staff (receptionist login)
```

To start fresh, click **Reset Demo Data** in the reception desk, or delete the `data/` folder and relaunch.
To inspect the data, open `data/hospital.db` in any SQLite browser (e.g. *DB Browser for SQLite*).

---

## Suggested demo flow (about 5 minutes)

1. **Home**: show the three portals and the live counters.
2. **Patient Portal → Register**: add a new patient aged 65. The app moves to *Book* with the phone number filled in.
3. **Book**: choose *Dr. Anjali Mehta*, today's date and a slot. The token is shown, and the priority is automatically *Senior Citizen*.
4. **Proceed to Payment**: first try the declined card (error message), then pay with `4111…` and show the receipt.
5. **Track Token**: show the queue position. Point out that the senior patient is placed ahead of normal patients.
6. **Queue Display Board**: open it in one view.
7. **Reception Desk** (login): in Live Queue, click *Call Next Patient* (the emergency patient goes first). Mark someone as No-Show and cancel a paid appointment (it becomes *Refunded*).
8. **All Appointments**: filter and search. Try an invalid status change to show validation.
9. **Payment History**: show all transactions and the totals.

---

## Project structure

```
src/main/java/com/hospital/hqms/
├── HospitalApp.java            JavaFX entry point
├── db/DatabaseManager.java     Singleton: JDBC connections, schema + sample data loader
├── model/                      Person (abstract) → Patient, Doctor; Appointment;
│                               enums Priority, AppointmentStatus, PaymentStatus, PaymentMethod;
│                               records Payment, Staff, QueueStatus, DashboardStats
├── dao/                        Dao<T> interface, BaseDao<T> (shared JDBC code),
│                               PatientDao, DoctorDao, AppointmentDao, PaymentDao, StaffDao
├── service/                    Business logic: PatientService, AppointmentService,
│   │                           QueueService (PriorityQueue), PaymentService, AuthService
│   └── payment/                PaymentProcessor interface + Card / Upi / Cash implementations
├── exception/                  HospitalException hierarchy + DataAccessException
├── util/                       Validator (regex, Luhn), PasswordUtil (SHA-256)
└── ui/                         Navigator, screens (Home, PatientPortal, ReceptionLogin,
    └── components/             ReceptionDashboard, QueueBoard) + reusable form panes
src/main/resources/
├── db/schema.sql, db/data.sql
└── css/styles.css
```

The code follows a layered architecture: **UI → Service → DAO → Database**. The UI never runs SQL directly, and the DAOs contain no business rules.

---

## Java concepts demonstrated

| Concept | Where |
|---|---|
| **Abstraction & inheritance** | `Person` is an abstract class with abstract `getDisplayId()`/`getRole()`. `Patient` and `Doctor` extend it. `BaseDao<T>` is an abstract DAO with an abstract `mapRow`. |
| **Encapsulation** | Private fields with getters and setters in every model class |
| **Polymorphism / Strategy pattern** | `PaymentProcessor` interface. `PaymentService.pay()` accepts any implementation (`CardPaymentProcessor`, `UpiPaymentProcessor`, `CashPaymentProcessor`). |
| **Interfaces & default methods** | `Dao<T>`, `PaymentProcessor` (default `newTransactionRef()`), `View` |
| **Generics** | `Dao<T>`, `BaseDao<T>`, generic `query(..., RowMapper<R>)`, `Fx.column(...)` |
| **Collections** | `PriorityQueue` with a `Comparator` (the queue), `EnumSet` (allowed status transitions), `HashSet` (booked slots), `TreeMap` + `groupingBy` (waiting count per doctor), `List`, `Map`, `Optional` |
| **Streams & lambdas** | Slot filtering, duplicate-booking check, dashboard counts, `FilteredList` predicates in tables |
| **Enums with fields & behaviour** | `Priority(rank, label)`, `AppointmentStatus.allowedNext()` / `canMoveTo()` |
| **Records (Java 16+)** | `Payment`, `Staff`, `QueueStatus`, `DashboardStats` |
| **Exception handling** | Custom checked hierarchy: `HospitalException` → `ValidationException`, `SlotUnavailableException`, `PaymentException`. Unchecked `DataAccessException` wraps `SQLException`. Errors are shown to the user in the UI. |
| **JDBC** | `PreparedStatement` (prevents SQL injection), try-with-resources, **transactions** with commit/rollback (booking + token generation, payment + status update), foreign keys |
| **Multithreading** | JavaFX `Task` runs the payment gateway call on a background thread. `Timeline` auto-refreshes the display board. |
| **Design patterns** | Singleton (`DatabaseManager`), DAO, Strategy (payments), Factory method (`PaymentPane.createProcessor()`), layered MVC-style separation |
| **Security basics** | Passwords are stored as SHA-256 hashes, and all input is validated with regex |

---

## Troubleshooting

- **`JAVA_HOME` not set / wrong Java version**: install JDK 17+ and make sure `java -version` shows 17 or newer.
- **The app shows yesterday's data**: sample appointments are created for the day the database was first made. Click **Reset Demo Data** (reception desk) to regenerate today's queue.
- **Need a clean database**: close the app and delete the `data/` folder.
