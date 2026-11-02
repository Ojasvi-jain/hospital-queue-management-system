package com.hospital.hqms.service.payment;

import com.hospital.hqms.exception.PaymentException;
import com.hospital.hqms.model.PaymentMethod;

/** Cash collected by the receptionist at the counter - always succeeds instantly. */
public class CashPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public void validate() throws PaymentException {
        // Nothing to validate for cash.
    }

    @Override
    public String authorize(double amount) {
        return newTransactionRef();
    }
}
