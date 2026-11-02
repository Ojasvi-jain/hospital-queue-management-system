-- =============================================================
--  Sample data for demonstration.
--  Appointment dates are relative to the current day, so the
--  receptionist always sees a live queue for "today".
--  Receptionist login:  reception / reception123
-- =============================================================

INSERT INTO staff (username, password_hash, full_name, role) VALUES
 ('reception', '5145dba3b6bda2d610d2c5c435a1c2481eefd3146b6a7e004ad73f794386e031', 'Front Desk', 'RECEPTIONIST');

INSERT INTO doctors (name, specialization, dept_code, qualification, room, consultation_fee, start_time, end_time, slot_minutes) VALUES
 ('Dr. Anjali Mehta',   'General Medicine', 'GEN', 'MBBS, MD',            'OPD-101', 300, '09:00', '13:00', 15),
 ('Dr. Rajesh Verma',   'Cardiology',       'CAR', 'MBBS, MD, DM',        'OPD-204', 800, '10:00', '14:00', 20),
 ('Dr. Priya Sharma',   'Pediatrics',       'PED', 'MBBS, DCH',           'OPD-110', 500, '09:00', '12:00', 15),
 ('Dr. Arvind Singh',   'Orthopedics',      'ORT', 'MBBS, MS (Ortho)',    'OPD-305', 700, '11:00', '15:00', 20),
 ('Dr. Neha Kapoor',    'Dermatology',      'DER', 'MBBS, MD (Derm)',     'OPD-212', 600, '14:00', '18:00', 15),
 ('Dr. Sameer Gupta',   'ENT',              'ENT', 'MBBS, MS (ENT)',      'OPD-118', 450, '09:30', '13:30', 15);

INSERT INTO patients (name, age, gender, phone, email, blood_group, address) VALUES
 ('Aarav Patel',    34, 'Male',   '9876543210', 'aarav.patel@example.com', 'B+',  'Sector 22, Patiala'),
 ('Diya Malhotra',  27, 'Female', '9812345678', 'diya.m@example.com',      'O+',  'Model Town, Patiala'),
 ('Kabir Khanna',   68, 'Male',   '9988776655', NULL,                      'A+',  'Leela Bhawan, Patiala'),
 ('Ishita Rao',      6, 'Female', '9123456780', 'rao.family@example.com',  'AB+', 'Urban Estate, Patiala'),
 ('Rohan Joshi',    45, 'Male',   '9090909090', 'rohan.j@example.com',     'O-',  'Tripuri, Patiala'),
 ('Meera Iyer',     72, 'Female', '9345678123', NULL,                      'B-',  'Bhupindra Road, Patiala'),
 ('Vikram Chauhan', 39, 'Male',   '9555512345', 'vikram.c@example.com',    'A-',  'Ranjit Nagar, Patiala'),
 ('Sneha Reddy',    31, 'Female', '9666612345', 'sneha.r@example.com',     'B+',  'Gurbax Colony, Patiala');

-- Today: General Medicine queue (one done, one in consultation, several waiting incl. senior/emergency)
INSERT INTO appointments (patient_id, doctor_id, appt_date, time_slot, token_no, priority, status, payment_status, symptoms) VALUES
 (1, 1, date('now','localtime'), '09:00', 1, 'NORMAL',    'COMPLETED',       'PAID',    'Fever and body ache'),
 (2, 1, date('now','localtime'), '09:15', 2, 'NORMAL',    'IN_CONSULTATION', 'PAID',    'Persistent cough'),
 (5, 1, date('now','localtime'), '09:30', 3, 'NORMAL',    'WAITING',         'PAID',    'Headache'),
 (6, 1, date('now','localtime'), '09:45', 4, 'SENIOR',    'WAITING',         'PENDING', 'High blood pressure follow-up'),
 (7, 1, date('now','localtime'), '10:00', 5, 'NORMAL',    'WAITING',         'PENDING', 'Stomach pain'),
 (8, 1, date('now','localtime'), '10:15', 6, 'EMERGENCY', 'WAITING',         'PAID',    'Severe allergic reaction');

-- Today: Cardiology and Pediatrics
INSERT INTO appointments (patient_id, doctor_id, appt_date, time_slot, token_no, priority, status, payment_status, symptoms) VALUES
 (3, 2, date('now','localtime'), '10:00', 1, 'SENIOR', 'WAITING',   'PAID',    'Chest discomfort'),
 (5, 2, date('now','localtime'), '10:20', 2, 'NORMAL', 'WAITING',   'PENDING', 'ECG review'),
 (4, 3, date('now','localtime'), '09:00', 1, 'NORMAL', 'COMPLETED', 'PAID',    'Vaccination'),
 (4, 3, date('now','localtime'), '09:15', 2, 'NORMAL', 'NO_SHOW',   'PENDING', 'Follow-up');

-- Tomorrow
INSERT INTO appointments (patient_id, doctor_id, appt_date, time_slot, token_no, priority, status, payment_status, symptoms) VALUES
 (1, 4, date('now','localtime','+1 day'), '11:00', 1, 'NORMAL', 'WAITING', 'PAID',    'Knee pain'),
 (2, 5, date('now','localtime','+1 day'), '14:00', 1, 'NORMAL', 'WAITING', 'PENDING', 'Skin rash');

INSERT INTO payments (appointment_id, amount, method, transaction_ref, status) VALUES
 (1,  300, 'UPI',  'TXN-DEMO-0001', 'SUCCESS'),
 (2,  300, 'CARD', 'TXN-DEMO-0002', 'SUCCESS'),
 (3,  300, 'CASH', 'TXN-DEMO-0003', 'SUCCESS'),
 (6,  300, 'UPI',  'TXN-DEMO-0004', 'SUCCESS'),
 (7,  800, 'CARD', 'TXN-DEMO-0005', 'SUCCESS'),
 (9,  500, 'CASH', 'TXN-DEMO-0006', 'SUCCESS'),
 (11, 700, 'UPI',  'TXN-DEMO-0007', 'SUCCESS');
