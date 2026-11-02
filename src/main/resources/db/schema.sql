-- =============================================================
--  Hospital Queue Management System - database schema (SQLite)
--  Executed automatically by DatabaseManager on first launch.
-- =============================================================

CREATE TABLE IF NOT EXISTS staff (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT    NOT NULL UNIQUE,
    password_hash  TEXT    NOT NULL,           -- SHA-256 hex digest
    full_name      TEXT    NOT NULL,
    role           TEXT    NOT NULL DEFAULT 'RECEPTIONIST'
);

CREATE TABLE IF NOT EXISTS doctors (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              TEXT    NOT NULL,
    specialization    TEXT    NOT NULL,
    dept_code         TEXT    NOT NULL,         -- used as token prefix, e.g. CAR-004
    qualification     TEXT,
    room              TEXT,
    consultation_fee  REAL    NOT NULL CHECK (consultation_fee >= 0),
    start_time        TEXT    NOT NULL,         -- HH:mm
    end_time          TEXT    NOT NULL,         -- HH:mm
    slot_minutes      INTEGER NOT NULL DEFAULT 15,
    available         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS patients (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    age          INTEGER NOT NULL CHECK (age BETWEEN 0 AND 120),
    gender       TEXT    NOT NULL,
    phone        TEXT    NOT NULL UNIQUE,
    email        TEXT,
    blood_group  TEXT,
    address      TEXT,
    created_at   TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS appointments (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id      INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id       INTEGER NOT NULL REFERENCES doctors(id),
    appt_date       TEXT    NOT NULL,           -- yyyy-MM-dd
    time_slot       TEXT    NOT NULL,           -- HH:mm
    token_no        INTEGER NOT NULL,
    priority        TEXT    NOT NULL DEFAULT 'NORMAL',   -- NORMAL | SENIOR | EMERGENCY
    status          TEXT    NOT NULL DEFAULT 'WAITING',  -- WAITING | IN_CONSULTATION | COMPLETED | CANCELLED | NO_SHOW
    payment_status  TEXT    NOT NULL DEFAULT 'PENDING',  -- PENDING | PAID | REFUNDED
    symptoms        TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (doctor_id, appt_date, token_no)
);

CREATE TABLE IF NOT EXISTS payments (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    appointment_id   INTEGER NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    amount           REAL    NOT NULL,
    method           TEXT    NOT NULL,          -- CARD | UPI | CASH
    transaction_ref  TEXT    NOT NULL UNIQUE,
    status           TEXT    NOT NULL,          -- SUCCESS | FAILED
    paid_at          TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_appt_doctor_date ON appointments (doctor_id, appt_date);
CREATE INDEX IF NOT EXISTS idx_appt_patient     ON appointments (patient_id);
