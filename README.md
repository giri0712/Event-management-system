# EventHub - Event Management System

A full-stack Event Management System built with Spring Boot, PostgreSQL, Redis/Valkey, and Razorpay payment integration. It allows organizers to create events, attendees to browse and book tickets, and administrators to manage users, events, and payments.

---

## Features

### Core Features
- User Management - Registration, login/logout with session-based authentication
- Role-Based Access Control - Three roles: ROLE_ATTENDEE, ROLE_ORGANIZER, ROLE_ADMIN
- Event CRUD - Create, read, update, and delete events
- Event Discovery - Browse upcoming events with search
- Ticket Booking - Select tickets, pay via Razorpay, receive confirmation
- Async Email Notifications

### Payment
- Razorpay Integration - Secure two-step payment flow
- HMAC SHA256 Signature Verification
- Transaction Logging

### Admin Panel
- User Management, Event Management, Booking Management, Payment Audit

### Frontend
- Modern Glassmorphism dark UI, Responsive, SPA-like navigation, Dashboard

### Infrastructure
- Redis/Valkey caching, Spring Session, Optimistic Locking, Flyway, Async email, H2 Console

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security (Session-based) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Prod) / H2 (Dev) |
| Cache | Redis / Valkey |
| Payments | Razorpay (razorpay-java 1.4.6) |
| Migrations | Flyway |
| Frontend | HTML5 / CSS3 / JavaScript ES6+ |
| Build | Maven |
| Testing | JUnit 5, Testcontainers |

---

## Getting Started

### Prerequisites
- Java 17+, Maven 3.8+, Docker (optional), Razorpay account

### Quick Start
    git clone https://github.com/your-username/event-management.git
    cd event-management
    mvn spring-boot:run

Application: http://localhost:8080
H2 Console: http://localhost:8080/h2-console

### Configuration
Edit src/main/resources/application.properties:
- razorpay.key.id / razorpay.key.secret (from Razorpay dashboard)
- spring.datasource.* for PostgreSQL
- spring.data.redis.* for Redis/Valkey

---

## Razorpay Integration

1. Sign up at https://dashboard.razorpay.com/
2. Get API keys from Settings > API Keys
3. Set razorpay.key.id and razorpay.key.secret in application.properties
4. Set the key in js/app.js initiateRazorpayCheckout() function
5. Test with card 4111 1111 1111 1111

Flow: Create Order -> Razorpay Modal -> Verify Signature -> Confirm Booking

---

## API Reference

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/auth/register | Register | No |
| POST | /api/auth/login | Login | No |
| POST | /api/auth/logout | Logout | No |
| GET | /api/auth/me | Current user | No |
| GET | /api/events | List events | No |
| GET | /api/events?search=x | Search events | No |
| GET | /api/events/{id} | Event details | No |
| POST | /api/events | Create event | Organizer |
| PUT | /api/events/{id} | Update event | Owner/Admin |
| DELETE | /api/events/{id} | Delete event | Owner/Admin |
| POST | /api/bookings/create-order | Create Razorpay order | Yes |
| POST | /api/bookings/verify-payment | Verify payment | Yes |
| GET | /api/bookings/my-bookings | My bookings | Yes |
| GET | /api/admin/users | All users | Admin |
| PUT | /api/admin/users/{id}/role | Update role | Admin |
| DELETE | /api/admin/users/{id} | Delete user | Admin |
| GET | /api/admin/events | All events | Admin |
| GET | /api/admin/bookings | All bookings | Admin |
| GET | /api/admin/payments | All payments | Admin |
| POST | /api/admin/bookings/{id}/cancel | Cancel booking | Admin |

---

## Project Structure

    src/main/java/com/eventmgmt/
    +-- config/        (AsyncConfig, RazorpayConfig, SecurityConfig)
    +-- controller/    (Auth, Event, Booking, Admin)
    +-- dto/           (Request/Response DTOs)
    +-- exception/     (GlobalExceptionHandler + custom exceptions)
    +-- model/         (User, Event, Booking, PaymentTransaction)
    +-- repository/    (JPA Repositories)
    +-- service/       (User, Event, Booking, Payment, Email, Admin)

    src/main/resources/static/
    +-- css/style.css  (Glassmorphism dark theme)
    +-- js/app.js, auth.js, main.js, event-detail.js, dashboard.js
    +-- index.html, event.html, login.html, register.html, dashboard.html

---

## Security

- Session-based auth with CSRF protection
- Public: Event browsing, auth endpoints
- Authenticated: Bookings, payments
- Admin only: /api/admin/**
- Payment: HMAC SHA256 verification, no card data stored (PCI DSS compliant)

---

## Performance

- Tomcat: 800 threads, 10000 connections
- HikariCP: 50 max pool size
- Optimistic Locking prevents ticket overselling
- Async email via thread pool (10-50 threads)
- Enable Redis caching: spring.cache.type=redis

---

## Testing

    mvn test

Uses Testcontainers for PostgreSQL and H2 for unit tests.

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Port 8080 in use | Change server.port |
| Razorpay fails | Check key.secret matches dashboard |
| CSRF 403 errors | Include X-XSRF-TOKEN header |
| Email not sending | Check SMTP config (fallback logs locally) |

---

## License: MIT

## Acknowledgments
- Spring Boot, Razorpay, Valkey, Flyway, Testcontainers, Google Fonts