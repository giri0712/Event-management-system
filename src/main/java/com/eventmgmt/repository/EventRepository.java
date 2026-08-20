package com.eventmgmt.repository;

import com.eventmgmt.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizerId(Long organizerId);
    
    // Fetch upcoming events ordered by date
    List<Event> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);
    
    // Search upcoming events
    List<Event> findByDateTimeAfterAndTitleContainingIgnoreCaseOrLocationContainingIgnoreCaseOrderByDateTimeAsc(
            LocalDateTime dateTime, String title, String location);
}
