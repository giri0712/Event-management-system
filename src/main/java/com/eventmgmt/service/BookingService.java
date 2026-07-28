package com.eventmgmt.service;

import com.eventmgmt.exception.ResourceNotFoundException;
import com.eventmgmt.exception.TicketCapacityExceededException;
import com.eventmgmt.exception.UnauthorizedAccessException;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.model.User;
import com.eventmgmt.repository.BookingRepository;
import com.eventmgmt.repository.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository,
                          PaymentService paymentService, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true) // Clear cache when tickets availability changes
    public Booking createBooking(Long eventId, int ticketCount, String cardholderName, 
                                 String cardNumber, String expiry, String cvv, User attendee) {
        
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Ticket count must be greater than zero.");
        }

        // Fetch Event and verify
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book tickets for past events.");
        }

        // Concurrency-safe check (Optimistic locking version comparison triggers on save if changed)
        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException(
                    "Only " + event.getTicketsRemaining() + " tickets are available for this event.");
        }

        double totalAmount = event.getPrice() * ticketCount;

        // Call our internal Payment API (Propagation.REQUIRES_NEW runs in separate database context)
        PaymentTransaction payment = paymentService.processPayment(
                totalAmount, cardholderName, cardNumber, expiry, cvv
        );

        // Deduct ticket capacity
        event.setTicketsSold(event.getTicketsSold() + ticketCount);
        eventRepository.save(event); // Triggers optimistic locking check if updated concurrently

        // Save Booking
        Booking booking = new Booking(
                attendee,
                event,
                LocalDateTime.now(),
                ticketCount,
                totalAmount,
                "CONFIRMED",
                payment.getTransactionId()
        );
        
        Booking savedBooking = bookingRepository.save(booking);

        // Send confirmation email asynchronously (offloaded to thread executor)
        emailService.sendTicketConfirmation(
                attendee.getEmail(),
                attendee.getFullName(),
                event.getTitle(),
                ticketCount,
                totalAmount,
                payment.getTransactionId()
        );

        return savedBooking;
    }

    public List<Booking> getMyBookings(User attendee) {
        return bookingRepository.findByAttendeeIdOrderByBookingDateDesc(attendee.getId());
    }

    public List<Booking> getEventBookings(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (!event.getOrganizer().getId().equals(organizer.getId()) && 
            !organizer.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedAccessException("You are not authorized to view bookings for this event.");
        }

        return bookingRepository.findByEventId(eventId);
    }
}
