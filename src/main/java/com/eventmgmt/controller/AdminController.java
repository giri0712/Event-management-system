package com.eventmgmt.controller;

import com.eventmgmt.dto.AdminUserUpdate;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.PaymentTransaction;
import com.eventmgmt.model.User;
import com.eventmgmt.service.AdminService;
import com.eventmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(adminService.getAllUsers(currentUser));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @Valid @RequestBody AdminUserUpdate userUpdate) {
        User currentUser = userService.getCurrentUser();
        User updatedUser = adminService.updateUserRole(id, userUpdate.getRole(), currentUser);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        adminService.deleteUser(id, currentUser);
        return ResponseEntity.ok().body("{\"message\": \"User deleted successfully.\"}");
    }

    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(adminService.getAllEvents(currentUser));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(adminService.getAllBookings(currentUser));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentTransaction>> getAllPayments() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(adminService.getAllPayments(currentUser));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Booking updatedBooking = adminService.cancelBooking(id, currentUser);
        return ResponseEntity.ok(updatedBooking);
    }
}
