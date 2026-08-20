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
import java.util.Map;

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

    // =============================================
    // RAZORPAY TWO-STEP BOOKING FLOW
    // =============================================

    /**
     * Step 1: Create a Razorpay order for the booking.
     * Returns order details to pass to the frontend Razorpay checkout.
     */
    public Map<String, Object> createOrder(Long eventId, int ticketCount, User attendee) {
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Ticket count must be greater than zero.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book tickets for past events.");
        }

        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException(
                    "Only " + event.getTicketsRemaining() + " tickets are available for this event.");
        }

        double totalAmount = event.getPrice() * ticketCount;
        String receipt = "booking_" + attendee.getId() + "_" + eventId + "_" + System.currentTimeMillis();

        return paymentService.createRazorpayOrder(totalAmount, receipt);
    }

    /**
     * Step 2: Verify the Razorpay payment and confirm the booking.
     * Validates signature, saves transaction, deducts tickets, creates booking record.
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Booking verifyAndConfirmBooking(String orderId, String paymentId, String signature,
                                            Long eventId, int ticketCount, User attendee) {
        // Verify Razorpay signature
        boolean isValid = paymentService.verifyRazorpayPayment(orderId, paymentId, signature);
        if (!isValid) {
            throw new IllegalArgumentException("Payment verification failed. Invalid signature.");
        }

        // Fetch event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException(
                    "Only " + event.getTicketsRemaining() + " tickets are available. Booking cancelled.");
        }

        double totalAmount = event.getPrice() * ticketCount;

        // Save Razorpay transaction record
        PaymentTransaction payment = paymentService.saveRazorpayTransaction(
                totalAmount, orderId, paymentId, attendee.getFullName());

        // Deduct tickets
        event.setTicketsSold(event.getTicketsSold() + ticketCount);
        eventRepository.save(event);

        // Create booking
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

        // Send confirmation email asynchronously
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

    // =============================================
    // LEGACY MOCK PAYMENT FLOW
    // =============================================

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Booking createBooking(Long eventId, int ticketCount, String cardholderName,
                                 String cardNumber, String expiry, String cvv, User attendee) {
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Ticket count must be greater than zero.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book tickets for past events.");
        }

        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException(
                    "Only " + event.getTicketsRemaining() + " tickets are available for this event.");
        }

        double totalAmount = event.getPrice() * ticketCount;

        PaymentTransaction payment = paymentService.processPayment(
                totalAmount, cardholderName, cardNumber, expiry, cvv
        );

        event.setTicketsSold(event.getTicketsSold() + ticketCount);
        eventRepository.save(event);

        Booking booking = new Booking(
                attendee, event, LocalDateTime.now(), ticketCount,
                totalAmount, "CONFIRMED", payment.getTransactionId()
        );

        Booking savedBooking = bookingRepository.save(booking);

        emailService.sendTicketConfirmation(
                attendee.getEmail(), attendee.getFullName(), event.getTitle(),
                ticketCount, totalAmount, payment.getTransactionId()
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
