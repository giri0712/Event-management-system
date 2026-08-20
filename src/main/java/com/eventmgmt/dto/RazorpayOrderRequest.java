package com.eventmgmt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RazorpayOrderRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Ticket count is required")
    @Positive(message = "Ticket count must be greater than zero")
    private Integer ticketCount;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }
}
