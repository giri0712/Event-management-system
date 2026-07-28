package com.eventmgmt.service;

import com.eventmgmt.exception.ResourceNotFoundException;
import com.eventmgmt.exception.UnauthorizedAccessException;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.User;
import com.eventmgmt.repository.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Cacheable(value = "events", key = "'upcoming'")
    public List<Event> getAllUpcomingEvents() {
        // Sleep to simulate network delay so cache hit becomes visually evident
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        return eventRepository.findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now());
    }

    public List<Event> searchUpcomingEvents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUpcomingEvents();
        }
        return eventRepository.findByDateTimeAfterAndTitleContainingIgnoreCaseOrLocationContainingIgnoreCaseOrderByDateTimeAsc(
                LocalDateTime.now(), query, query);
    }

    @Cacheable(value = "events", key = "#id")
    public Event getEventById(Long id) {
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Event createEvent(Event event, User organizer) {
        event.setOrganizer(organizer);
        event.setTicketsSold(0);
        return eventRepository.save(event);
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Event updateEvent(Long eventId, Event eventDetails, User currentUser) {
        Event event = getEventById(eventId);
        
        // Ensure user is organizer of the event or is an admin
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && 
            !currentUser.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedAccessException("You are not authorized to update this event.");
        }

        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setLocation(eventDetails.getLocation());
        event.setDateTime(eventDetails.getDateTime());
        event.setPrice(eventDetails.getPrice());
        event.setCapacity(eventDetails.getCapacity());
        event.setBannerUrl(eventDetails.getBannerUrl());

        return eventRepository.save(event);
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void deleteEvent(Long eventId, User currentUser) {
        Event event = getEventById(eventId);
        
        // Ensure user is organizer of the event or is an admin
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && 
            !currentUser.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedAccessException("You are not authorized to delete this event.");
        }

        eventRepository.delete(event);
    }

    public List<Event> getOrganizerEvents(User organizer) {
        return eventRepository.findByOrganizerId(organizer.getId());
    }
}
