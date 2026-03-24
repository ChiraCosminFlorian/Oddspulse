package com.oddspulse.controller;

import com.oddspulse.dto.OddsEventResponse;
import com.oddspulse.dto.OddsSnapshotResponse;
import com.oddspulse.service.OddsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odds")
@RequiredArgsConstructor
public class OddsController {

    private final OddsQueryService oddsQueryService;

    @GetMapping("/events")
    public ResponseEntity<List<OddsEventResponse>> getEvents(
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false, defaultValue = "UPCOMING") String status) {
        return ResponseEntity.ok(oddsQueryService.getEvents(sportType, status));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<OddsEventResponse> getEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(oddsQueryService.getEventByEventId(eventId));
    }

    @GetMapping("/events/{eventId}/history")
    public ResponseEntity<List<OddsSnapshotResponse>> getEventHistory(@PathVariable String eventId) {
        return ResponseEntity.ok(oddsQueryService.getEventHistory(eventId));
    }

    @GetMapping("/live")
    public ResponseEntity<List<OddsEventResponse>> getLiveEvents() {
        return ResponseEntity.ok(oddsQueryService.getLiveEvents());
    }

    @GetMapping("/events/{eventId}/latest")
    public ResponseEntity<OddsSnapshotResponse> getLatestSnapshot(@PathVariable String eventId) {
        return ResponseEntity.ok(oddsQueryService.getLatestSnapshot(eventId));
    }
}
