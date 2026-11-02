package com.hospital.hqms.service.payment;

import com.hospital.hqms.exception.PaymentException;
import com.hospital.hqms.model.PaymentMethod;
import com.hospital.hqms.util.Validator;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Simulated card payment.
 * Use 4111 1111 1111 1111 for success and 4000 0000 0000 0002 to see a declined card.
 */
public class CardPaymentProcessor implements PaymentProcessor {

    public static final String DECLINED_TEST_CARD = "4000000000000002";

    private final String cardNumber;
    private final String holderName;
    private final String expiry;
    private final String cvv;

    public CardPaymentProcessor(String cardNumber, String holderName, String expiry, String cvv) {
        this.cardNumber = Validator.trim(cardNumber).replaceAll("[\\s-]", "");
        this.holderName = Validator.trim(holderName);
        this.expiry = Validator.trim(expiry);
        this.cvv = Validator.trim(cvv);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public void validate() throws PaymentException {
        if (!cardNumber.matches("\\d{16}") || !Validator.passesLuhn(cardNumber)) {
            throw new PaymentException("Invalid card number. Try the test card 4111 1111 1111 1111.");
        }
        if (holderName.length() < 2) {
            throw new PaymentException("Enter the name printed on the card.");
        }
        try {
            YearMonth exp = YearMonth.parse(expiry, DateTimeFormatter.ofPattern("MM/yy"));
            if (exp.isBefore(YearMonth.now())) {
                throw new PaymentException("This card has expired.");
            }
        } catch (DateTimeParseException e) {
            throw new PaymentException("Expiry must be in MM/YY format.");
        }
        if (!cvv.matches("\\d{3}")) {
            throw new PaymentException("CVV must be 3 digits.");
        }
    }

    @Override
    public String authorize(double amount) throws PaymentException {
        simulateNetworkDelay();
        if (DECLINED_TEST_CARD.equals(cardNumber)) {
            throw new PaymentException("Card declined by the bank (insufficient funds).");
        }
        return newTransactionRef();
    }
}
