package com.eventmgmt.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    private Integer eventsAttended = 0;

    private Integer totalRegistrations = 0;

    private Double totalSpent = 0.0;

    private Integer ranking = 0;

    private String avatarUrl;

    public Team() {}

    public Team(String name, String department, Integer eventsAttended, Integer totalRegistrations, Double totalSpent, Integer ranking, String avatarUrl) {
        this.name = name;
        this.department = department;
        this.eventsAttended = eventsAttended;
        this.totalRegistrations = totalRegistrations;
        this.totalSpent = totalSpent;
        this.ranking = ranking;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getEventsAttended() { return eventsAttended; }
    public void setEventsAttended(Integer eventsAttended) { this.eventsAttended = eventsAttended; }
    public Integer getTotalRegistrations() { return totalRegistrations; }
    public void setTotalRegistrations(Integer totalRegistrations) { this.totalRegistrations = totalRegistrations; }
    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }
    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}