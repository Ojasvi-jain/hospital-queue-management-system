package com.hospital.hqms.exception;

/**
 * Unchecked wrapper around {@link java.sql.SQLException}.
 * Database failures are unexpected at runtime, so the DAO layer converts them
 * into this unchecked exception instead of forcing every caller to catch them.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
