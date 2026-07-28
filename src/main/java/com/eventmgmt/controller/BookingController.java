package com.eventmgmt.controller;

import com.eventmgmt.dto.BookingRequest;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.User;
import com.eventmgmt.service.BookingService;
import com.eventmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

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
