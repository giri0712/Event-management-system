package com.eventmgmt.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminUserUpdate {

    @NotBlank(message = "Role is required")
    private String role;

    public AdminUserUpdate() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
