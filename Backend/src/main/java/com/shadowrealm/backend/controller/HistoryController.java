package com.shadowrealm.backend.controller;

import com.shadowrealm.backend.dto.MatchDetailResponse;
import com.shadowrealm.backend.dto.MatchSummaryResponse;
import com.shadowrealm.backend.service.MatchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Match history. Every endpoint requires a JWT and only shows the caller's own matches. */
@RestController
@RequestMapping("/history")
public class HistoryController {

    private final MatchHistoryService service;

    public HistoryController(MatchHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MatchSummaryResponse>> myHistory() {
        return ResponseEntity.ok(service.historyForCurrentUser());
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailResponse> detail(@PathVariable Long matchId) {
        return ResponseEntity.ok(service.detail(matchId));
    }

    @GetMapping("/room/{roomCode}")
    public ResponseEntity<MatchDetailResponse> detailForRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(service.detailForRoom(roomCode));
    }
}
