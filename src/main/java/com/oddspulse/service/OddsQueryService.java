package com.oddspulse.service;

import com.oddspulse.dto.OddsEventResponse;
import com.oddspulse.dto.OddsSnapshotResponse;
import com.oddspulse.exception.ResourceNotFoundException;
import com.oddspulse.model.OddsEvent;
import com.oddspulse.model.OddsSnapshot;
import com.oddspulse.repository.OddsEventRepository;
import com.oddspulse.repository.OddsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OddsQueryService {

    private final OddsEventRepository eventRepository;
    private final OddsSnapshotRepository snapshotRepository;

    public List<OddsEventResponse> getEvents(String sportType, String status) {
        List<OddsEvent> events;

        if (sportType != null && !sportType.isBlank()) {
            events = eventRepository.findBySportTypeAndStatus(sportType, status);
        } else {
            events = eventRepository.findAll().stream()
                    .filter(e -> status == null || e.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        return events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    public OddsEventResponse getEventByEventId(String eventId) {
        OddsEvent event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        return mapToEventResponse(event);
    }

    public List<OddsSnapshotResponse> getEventHistory(String eventId) {
        OddsEvent event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        List<OddsSnapshot> snapshots = snapshotRepository.findByEventOrderBySnapshotTimeDesc(event);
        return snapshots.stream()
                .map(this::mapToSnapshotResponse)
                .collect(Collectors.toList());
    }

    public List<OddsEventResponse> getLiveEvents() {
        return eventRepository.findAll().stream()
                .filter(e -> "LIVE".equalsIgnoreCase(e.getStatus()))
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    public OddsSnapshotResponse getLatestSnapshot(String eventId) {
        OddsEvent event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        OddsSnapshot snapshot = snapshotRepository.findTopByEventOrderBySnapshotTimeDesc(event)
                .orElseThrow(() -> new ResourceNotFoundException("No snapshots found for event: " + eventId));
        return mapToSnapshotResponse(snapshot);
    }

    // ---- Manual DTO Mapping ----

    private OddsEventResponse mapToEventResponse(OddsEvent event) {
        Optional<OddsSnapshot> latestSnapshot = snapshotRepository
                .findTopByEventOrderBySnapshotTimeDesc(event);

        OddsEventResponse.OddsEventResponseBuilder builder = OddsEventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .sportType(event.getSportType())
                .homeTeam(event.getHomeTeam())
                .awayTeam(event.getAwayTeam())
                .eventTime(event.getEventTime())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt());

        latestSnapshot.ifPresent(snapshot -> {
            builder.latestHomeOdds(snapshot.getHomeOdds());
            builder.latestAwayOdds(snapshot.getAwayOdds());
            builder.latestDrawOdds(snapshot.getDrawOdds());
        });

        return builder.build();
    }

    private OddsSnapshotResponse mapToSnapshotResponse(OddsSnapshot snapshot) {
        return OddsSnapshotResponse.builder()
                .id(snapshot.getId())
                .eventId(snapshot.getEvent().getId())
                .homeOdds(snapshot.getHomeOdds())
                .drawOdds(snapshot.getDrawOdds())
                .awayOdds(snapshot.getAwayOdds())
                .totalVolume(snapshot.getTotalVolume())
                .snapshotTime(snapshot.getSnapshotTime())
                .source(snapshot.getSource())
                .build();
    }
}
