package com.oddspulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OddsEventResponse {

    private Long id;
    private String eventId;
    private String sportType;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime eventTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BigDecimal latestHomeOdds;
    private BigDecimal latestAwayOdds;
    private BigDecimal latestDrawOdds;
}
