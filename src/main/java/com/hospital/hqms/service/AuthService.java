package com.hospital.hqms.service;

import com.hospital.hqms.dao.StaffDao;
import com.hospital.hqms.exception.ValidationException;
import com.hospital.hqms.model.Staff;
import com.hospital.hqms.util.PasswordUtil;
import com.hospital.hqms.util.Validator;

public class AuthService {

    private final StaffDao staffDao = new StaffDao();

    public Staff login(String username, String password) throws ValidationException {
        String user = Validator.trim(username);
        if (user.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Enter both username and password.");
        }
        return staffDao.findByCredentials(user, PasswordUtil.sha256(password))
                .orElseThrow(() -> new ValidationException("Invalid username or password."));
    }
}
