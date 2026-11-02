package com.hospital.hqms.service;

import com.hospital.hqms.dao.PatientDao;
import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Patient;
import com.hospital.hqms.util.Validator;

import java.util.List;

public class PatientService {

    private final PatientDao patientDao = new PatientDao();

    /** Validates the form input and registers a new patient. */
    public Patient register(String name, String ageText, String gender, String phone,
                            String email, String bloodGroup, String address) throws ValidationException {
        String validName = Validator.requireName(name);
        int age = Validator.requireAge(ageText);
        Validator.requireSelected(gender, "a gender");
        String validPhone = Validator.requirePhone(phone);
        String validEmail = Validator.optionalEmail(email);

        if (patientDao.findByPhone(validPhone).isPresent()) {
            throw new ValidationException("A patient with phone " + validPhone + " is already registered. "
                    + "Use that number to book an appointment.");
        }
        Patient p = new Patient(0, validName, age, gender, validPhone, validEmail, bloodGroup,
                Validator.trim(address));
        return patientDao.insert(p);
    }

    public Patient findByPhone(String phone) throws ValidationException {
        String validPhone = Validator.requirePhone(phone);
        return patientDao.findByPhone(validPhone)
                .orElseThrow(() -> new ValidationException("No patient registered with phone " + validPhone + "."));
    }

    public List<Patient> findAll() {
        return patientDao.findAll();
    }
}
