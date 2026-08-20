package com.eventmgmt.config;

import com.eventmgmt.model.Event;
import com.eventmgmt.model.Team;
import com.eventmgmt.model.User;
import com.eventmgmt.repository.EventRepository;
import com.eventmgmt.repository.TeamRepository;
import com.eventmgmt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(EventRepository eventRepository, UserRepository userRepository,
                            TeamRepository teamRepository, PasswordEncoder passwordEncoder) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            log.info("Data already initialized, skipping.");
            return;
        }

        log.info("Initializing seed data...");

        // Create demo users
        User admin = new User("admin", passwordEncoder.encode("admin123"), "admin@eventhub.com", "Admin User", "ROLE_ADMIN");
        admin = userRepository.save(admin);

        User organizer = new User("organizer", passwordEncoder.encode("organizer123"), "organizer@eventhub.com", "Riya Sharma", "ROLE_ORGANIZER");
        organizer = userRepository.save(organizer);

        User attendee = new User("attendee", passwordEncoder.encode("attendee123"), "attendee@eventhub.com", "Arjun Patel", "ROLE_ATTENDEE");
        attendee = userRepository.save(attendee);

        // Create 5 dummy events with registrations
        Event e1 = new Event(
            "TechSummit 2026",
            "India's largest developer conference. 3 days of workshops, keynotes, and networking with 5000+ attendees. Featuring speakers from Google, Microsoft, and top startups.",
            "Bangalore International Exhibition Centre",
            LocalDateTime.now().plusDays(45),
            2500.0,
            500,
            "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800",
            organizer
        );
        e1.setTicketsSold(423);
        eventRepository.save(e1);

        Event e2 = new Event(
            "AI & Machine Learning Workshop",
            "Hands-on workshop covering LLMs, computer vision, and MLOps. Build real projects with TensorFlow and PyTorch. Limited seats!",
            "IIT Bombay, Powai",
            LocalDateTime.now().plusDays(20),
            1500.0,
            200,
            "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800",
            organizer
        );
        e2.setTicketsSold(187);
        eventRepository.save(e2);

        Event e3 = new Event(
            "Startup Pitch Night",
            "Watch 15 promising startups pitch to top VCs. Networking dinner included. Investors from Sequoia, Accel, and Lightspeed on the panel.",
            "WeWork BKC, Mumbai",
            LocalDateTime.now().plusDays(12),
            500.0,
            150,
            "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800",
            organizer
        );
        e3.setTicketsSold(142);
        eventRepository.save(e3);

        Event e4 = new Event(
            "DevOps & Cloud Conference",
            "Master Kubernetes, Docker, CI/CD pipelines, and cloud-native architecture. Hands-on labs with AWS, GCP, and Azure credits included.",
            "Jio World Centre, Mumbai",
            LocalDateTime.now().plusDays(60),
            3000.0,
            300,
            "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800",
            organizer
        );
        e4.setTicketsSold(198);
        eventRepository.save(e4);

        Event e5 = new Event(
            "UI/UX Design Masterclass",
            "From wireframes to high-fidelity prototypes. Learn Figma, design systems, and user research methods used by top product teams.",
            "NASSCOM 10000 Startups, Hyderabad",
            LocalDateTime.now().plusDays(30),
            800.0,
            100,
            "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=800",
            organizer
        );
        e5.setTicketsSold(76);
        eventRepository.save(e5);

        // Create 5 teams with rankings
        teamRepository.save(new Team("Phoenix Coders", "Engineering", 5, 42, 85000.0, 1, ""));
        teamRepository.save(new Team("Byte Brigade", "Engineering", 4, 38, 72000.0, 2, ""));
        teamRepository.save(new Team("Data Dynamos", "Data Science", 4, 35, 68000.0, 3, ""));
        teamRepository.save(new Team("Cloud Crusaders", "DevOps", 3, 29, 55000.0, 4, ""));
        teamRepository.save(new Team("Pixel Pioneers", "Design", 3, 24, 45000.0, 5, ""));

        log.info("Seed data initialized: 3 users, 5 events, 5 teams");
        log.info("Demo credentials:");
        log.info("  Admin:     admin / admin123");
        log.info("  Organizer: organizer / organizer123");
        log.info("  Attendee:  attendee / attendee123");
    }
}