package com.eventmgmt.controller;

import com.eventmgmt.config.RazorpayConfig;
import com.eventmgmt.dto.BookingRequest;
import com.eventmgmt.dto.PaymentVerifyRequest;
import com.eventmgmt.dto.RazorpayOrderRequest;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.User;
import com.eventmgmt.service.BookingService;
import com.eventmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final RazorpayConfig razorpayConfig;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService, RazorpayConfig razorpayConfig) {
        this.bookingService = bookingService;
        this.razorpayConfig = razorpayConfig;
        this.userService = userService;
    }

    // =============================================
    // RAZORPAY PAYMENT ENDPOINTS
    // =============================================

    /**
     * Step 1: Create a Razorpay order.
     * Returns orderId, amount, and currency for the frontend to initiate Razorpay checkout.
     */

    /**
     * Returns the Razorpay public key for the frontend.
     */
    @GetMapping("/razorpay-config")
    public ResponseEntity<Map<String, Object>> getRazorpayConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("keyId", razorpayConfig.getKeyId());
        config.put("mock", razorpayConfig.useMockRazorpay());
        return ResponseEntity.ok(config);
    }
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequest request) {
        User currentUser = userService.getCurrentUser();

        Map<String, Object> order = bookingService.createOrder(
                request.getEventId(),
                request.getTicketCount(),
                currentUser
        );

        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    /**
     * Step 2: Verify Razorpay payment and confirm booking.
     * Receives payment details from Razorpay callback, verifies signature, creates booking.
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<Booking> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        User currentUser = userService.getCurrentUser();

        Booking booking = bookingService.verifyAndConfirmBooking(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                request.getEventId(),
                request.getTicketCount(),
                currentUser
        );

        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    // =============================================
    // LEGACY MOCK PAYMENT ENDPOINT
    // =============================================

    @PostMapping
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingRequest bookingRequest) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingService.createBooking(
                bookingRequest.getEventId(),
                bookingRequest.getTicketCount(),
                bookingRequest.getCardholderName(),
                bookingRequest.getCardNumber(),
                bookingRequest.getExpiry(),
                bookingRequest.getCvv(),
                currentUser
        );
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Booking>> getEventBookings(@PathVariable Long eventId) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(bookingService.getEventBookings(eventId, currentUser));
    }
}
