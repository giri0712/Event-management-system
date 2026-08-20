package com.eventmgmt.service;

import com.eventmgmt.exception.PaymentFailedException;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.repository.PaymentTransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentTransactionRepository paymentTransactionRepository;
    @Nullable
    private final RazorpayClient razorpayClient;
    private final boolean useMock;

    @Value("${razorpay.key.secret:YOUR_KEY_SECRET}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    private static final String MOCK_SECRET = "mock_secret_eventhub_dev_2026";

    public PaymentService(PaymentTransactionRepository paymentTransactionRepository,
                          @Nullable RazorpayClient razorpayClient,
                          boolean useMockRazorpay) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.razorpayClient = razorpayClient;
        this.useMock = useMockRazorpay;
    }

    public Map<String, Object> createRazorpayOrder(double amount, String receipt) {
        if (useMock) return createMockOrder(amount, receipt);
        return createRealOrder(amount, receipt);
    }

    private Map<String, Object> createRealOrder(double amount, String receipt) {
        try {
            JSONObject req = new JSONObject();
            req.put("amount", (long)(amount * 100));
            req.put("currency", currency);
            req.put("receipt", receipt);
            req.put("payment_capture", 1);
            com.razorpay.Order order = razorpayClient.orders.create(req);
            Map<String, Object> resp = new HashMap<>();
            resp.put("orderId", order.get("id"));
            resp.put("amount", order.get("amount"));
            resp.put("currency", order.get("currency"));
            return resp;
        } catch (RazorpayException e) {
            throw new PaymentFailedException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    private Map<String, Object> createMockOrder(double amount, String receipt) {
        String orderId = "ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long amountPaise = (long)(amount * 100);
        log.info("[MOCK RAZORPAY] Order created: {} for {} {}", orderId, amountPaise, currency);
        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", orderId);
        resp.put("amount", amountPaise);
        resp.put("currency", currency);
        return resp;
    }

    public boolean verifyRazorpayPayment(String orderId, String paymentId, String razorpaySignature) {
        if (useMock) return verifyMockPayment(orderId, paymentId, razorpaySignature);
        return verifyRealPayment(orderId, paymentId, razorpaySignature);
    }

    private boolean verifyRealPayment(String orderId, String paymentId, String sig) {
        try {
            String payload = orderId + "|" + paymentId;
            String expected = generateHmacSha256(payload, razorpayKeySecret);
            return expected.equals(sig);
        } catch (Exception e) {
            throw new PaymentFailedException("Payment verification failed: " + e.getMessage());
        }
    }

    private boolean verifyMockPayment(String orderId, String paymentId, String sig) {
        try {
            String payload = orderId + "|" + paymentId;
            String expected = generateHmacSha256(payload, MOCK_SECRET);
            boolean valid = expected.equals(sig);
            log.info("[MOCK RAZORPAY] Verification: orderId={}, valid={}", orderId, valid);
            return valid;
        } catch (Exception e) {
            throw new PaymentFailedException("Mock verification failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction saveRazorpayTransaction(double amount, String orderId, String paymentId, String cardholderName) {
        String prefix = useMock ? "MOCK_" : "RZP_";
        String txnId = prefix + paymentId.replaceAll("[^a-zA-Z0-9]", "")
                .substring(0, Math.min(paymentId.length(), 16)).toUpperCase();
        PaymentTransaction t = new PaymentTransaction(txnId, amount, "SUCCESS",
                cardholderName, (useMock ? "[MOCK] " : "Razorpay: ") + orderId, LocalDateTime.now());
        return paymentTransactionRepository.save(t);
    }

    public String getMockSecret() { return useMock ? MOCK_SECRET : null; }
    public boolean isMockMode() { return useMock; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction processPayment(Double amount, String cardholderName, String cardNumber, String expiry, String cvv) {
        String cleanCard = cardNumber.replaceAll("\\s|-", "");
        if (cleanCard.length() < 15 || cleanCard.length() > 19) throw new PaymentFailedException("Invalid card number.");
        if (cvv.length() < 3 || cvv.length() > 4) throw new PaymentFailedException("Invalid CVV.");
        if (!expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) throw new PaymentFailedException("Invalid expiry format.");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        if (cleanCard.endsWith("0000")) {
            paymentTransactionRepository.save(new PaymentTransaction("TXN_DECLINED_" + UUID.randomUUID().toString().substring(0,8).toUpperCase(), amount, "FAILED", cardholderName, maskCardNumber(cleanCard), LocalDateTime.now()));
            throw new PaymentFailedException("Payment declined.");
        }
        if (cleanCard.endsWith("9999")) throw new PaymentFailedException("Payment error.");
        String txnId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0,16).toUpperCase();
        return paymentTransactionRepository.save(new PaymentTransaction(txnId, amount, "SUCCESS", cardholderName, maskCardNumber(cleanCard), LocalDateTime.now()));
    }

    private String maskCardNumber(String c) { return c.length() <= 4 ? c : "*".repeat(c.length()-4) + c.substring(c.length()-4); }

    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) { String h = Integer.toHexString(0xff & b); if (h.length()==1) hex.append('0'); hex.append(h); }
        return hex.toString();
    }
}