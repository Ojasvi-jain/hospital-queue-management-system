package com.hospital.hqms.service.payment;

import com.hospital.hqms.exception.PaymentException;
import com.hospital.hqms.model.PaymentMethod;
import com.hospital.hqms.util.Validator;

/** Simulated UPI payment. Any UPI id ending in "@fail" is rejected, for demonstration. */
public class UpiPaymentProcessor implements PaymentProcessor {

    private final String upiId;

    public UpiPaymentProcessor(String upiId) {
        this.upiId = Validator.trim(upiId).toLowerCase();
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public void validate() throws PaymentException {
        if (!Validator.isValidUpi(upiId)) {
            throw new PaymentException("Enter a valid UPI ID, e.g. name@okbank.");
        }
    }

    @Override
    public String authorize(double amount) throws PaymentException {
        simulateNetworkDelay();
        if (upiId.endsWith("@fail")) {
            throw new PaymentException("UPI request was declined by the payer.");
        }
        return newTransactionRef();
    }
}
