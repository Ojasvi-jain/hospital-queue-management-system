package com.hospital.hqms.util;

import com.hospital.hqms.exception.ValidationException;

import java.util.regex.Pattern;

/** Static input-validation helpers built on regular expressions. */
public final class Validator {

    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,59}$");
    private static final Pattern UPI = Pattern.compile("^[\\w.-]{2,}@[A-Za-z]{2,}$");

    private Validator() {
    }

    public static String requireName(String value) throws ValidationException {
        String v = trim(value);
        if (!NAME.matcher(v).matches()) {
            throw new ValidationException("Please enter a valid full name (letters and spaces only).");
        }
        return v;
    }

    public static String requirePhone(String value) throws ValidationException {
        String v = trim(value);
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Phone number must be a 10-digit Indian mobile number.");
        }
        return v;
    }

    /** Email is optional: returns null if blank, otherwise validates it. */
    public static String optionalEmail(String value) throws ValidationException {
        String v = trim(value);
        if (v.isEmpty()) {
            return null;
        }
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Email address looks invalid.");
        }
        return v;
    }

    public static int requireAge(String value) throws ValidationException {
        try {
            int age = Integer.parseInt(trim(value));
            if (age < 0 || age > 120) {
                throw new ValidationException("Age must be between 0 and 120.");
            }
            return age;
        } catch (NumberFormatException e) {
            throw new ValidationException("Age must be a whole number.");
        }
    }

    public static <T> T requireSelected(T value, String fieldName) throws ValidationException {
        if (value == null) {
            throw new ValidationException("Please select " + fieldName + ".");
        }
        return value;
    }

    public static boolean isValidUpi(String value) {
        return UPI.matcher(trim(value)).matches();
    }

    /** Luhn checksum - the standard check-digit algorithm used by real card numbers. */
    public static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
