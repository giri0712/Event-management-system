package com.eventmgmt.service;

import com.eventmgmt.exception.PaymentFailedException;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Process payment card transactions using our own payment processing logic.
     * Propagation.REQUIRES_NEW is used to persist failed payments even if the wrapping booking transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction processPayment(Double amount, String cardholderName, String cardNumber, String expiry, String cvv) {
        // Clean card formatting
        String cleanCard = cardNumber.replaceAll("\\s|-", "");
        
        // Basic Card Validations
        if (cleanCard.length() < 15 || cleanCard.length() > 19) {
            throw new PaymentFailedException("Invalid card number length. Must be between 15 and 19 digits.");
        }
        if (cvv.length() < 3 || cvv.length() > 4) {
            throw new PaymentFailedException("Invalid CVV format. Must be 3 or 4 digits.");
        }
        if (!expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            throw new PaymentFailedException("Invalid expiry date format. Use MM/YY.");
        }

        // Simulate network delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException ignored) {}

        // Mock payment decline rules for testing/education:
        // Card ending in '0000' triggers decline
        if (cleanCard.endsWith("0000")) {
            String maskedCard = maskCardNumber(cleanCard);
            PaymentTransaction transaction = new PaymentTransaction(
                    "TXN_DECLINED_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    amount,
                    "FAILED",
                    cardholderName,
                    maskedCard,
                    LocalDateTime.now()
            );
            paymentTransactionRepository.save(transaction);
            throw new PaymentFailedException("Payment declined: Insufficient funds on card.");
        }

        // Card ending in '9999' triggers error
        if (cleanCard.endsWith("9999")) {
            throw new PaymentFailedException("Payment processing error: Unable to contact issuing bank.");
        }

        // Standard payment approved
        String transactionId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String maskedCard = maskCardNumber(cleanCard);
        
        PaymentTransaction transaction = new PaymentTransaction(
                transactionId,
                amount,
                "SUCCESS",
                cardholderName,
                maskedCard,
                LocalDateTime.now()
        );

        return paymentTransactionRepository.save(transaction);
    }

    private String maskCardNumber(String cardNumber) {
        int length = cardNumber.length();
        if (length <= 4) {
            return cardNumber;
        }
        return "*".repeat(length - 4) + cardNumber.substring(length - 4);
    }
}
