package com.eventmgmt.controller;

import com.eventmgmt.dto.EventRequest;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.User;
import com.eventmgmt.service.EventService;
import com.eventmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(@RequestParam(value = "search", required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(eventService.searchUpcomingEvents(search));
        }
        return ResponseEntity.ok(eventService.getAllUpcomingEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequest eventRequest) {
        User currentUser = userService.getCurrentUser();
        Event event = new Event(
                eventRequest.getTitle(),
                eventRequest.getDescription(),
                eventRequest.getLocation(),
                eventRequest.getDateTime(),
                eventRequest.getPrice(),
                eventRequest.getCapacity(),
                eventRequest.getBannerUrl(),
                currentUser
        );
        Event savedEvent = eventService.createEvent(event, currentUser);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest eventRequest) {
        User currentUser = userService.getCurrentUser();
        Event eventDetails = new Event(
                eventRequest.getTitle(),
                eventRequest.getDescription(),
                eventRequest.getLocation(),
                eventRequest.getDateTime(),
                eventRequest.getPrice(),
                eventRequest.getCapacity(),
                eventRequest.getBannerUrl(),
                null
        );
        Event updatedEvent = eventService.updateEvent(id, eventDetails, currentUser);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        eventService.deleteEvent(id, currentUser);
        return ResponseEntity.ok().body("{\"message\": \"Event deleted successfully.\"}");
    }

    @GetMapping("/organizer")
    public ResponseEntity<List<Event>> getOrganizerEvents() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(eventService.getOrganizerEvents(currentUser));
    }
}
