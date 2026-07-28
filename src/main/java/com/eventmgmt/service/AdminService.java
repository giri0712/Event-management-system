package com.eventmgmt.service;

import com.eventmgmt.exception.ResourceNotFoundException;
import com.eventmgmt.exception.UnauthorizedAccessException;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.model.User;
import com.eventmgmt.repository.BookingRepository;
import com.eventmgmt.repository.EventRepository;
import com.eventmgmt.repository.PaymentTransactionRepository;
import com.eventmgmt.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserService userService;

    public AdminService(UserRepository userRepository, EventRepository eventRepository,
                        BookingRepository bookingRepository, PaymentTransactionRepository paymentTransactionRepository,
                        UserService userService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userService = userService;
    }

    private void verifyAdmin(User currentAdmin) {
        if (currentAdmin == null || !currentAdmin.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedAccessException("Administrative privileges required.");
        }
    }

    public List<User> getAllUsers(User currentAdmin) {
        verifyAdmin(currentAdmin);
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long userId, String newRole, User currentAdmin) {
        verifyAdmin(currentAdmin);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!newRole.equals("ROLE_ATTENDEE") && 
            !newRole.equals("ROLE_ORGANIZER") && 
            !newRole.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("Invalid user role specified.");
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, User currentAdmin) {
        verifyAdmin(currentAdmin);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    public List<Event> getAllEvents(User currentAdmin) {
        verifyAdmin(currentAdmin);
        return eventRepository.findAll();
    }

    public List<Booking> getAllBookings(User currentAdmin) {
        verifyAdmin(currentAdmin);
        return bookingRepository.findAll();
    }

    public List<PaymentTransaction> getAllPayments(User currentAdmin) {
        verifyAdmin(currentAdmin);
        return paymentTransactionRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Booking cancelBooking(Long bookingId, User currentAdmin) {
        verifyAdmin(currentAdmin);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("Booking is already cancelled.");
        }

        // Return ticket count back to the event capacity
        Event event = booking.getEvent();
        event.setTicketsSold(Math.max(0, event.getTicketsSold() - booking.getTicketCount()));
        eventRepository.save(event);

        // Update booking status
        booking.setStatus("CANCELLED");
        
        // Update payment transaction record to REFUNDED
        paymentTransactionRepository.findByTransactionId(booking.getTransactionId())
                .ifPresent(payment -> {
                    payment.setStatus("REFUNDED");
                    paymentTransactionRepository.save(payment);
                });

        return bookingRepository.save(booking);
    }
}
