package com.eventmgmt.controller;

import com.eventmgmt.model.Team;
import com.eventmgmt.repository.TeamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeamsRanked() {
        return ResponseEntity.ok(teamRepository.findAllByOrderByRankingAsc());
    }
}