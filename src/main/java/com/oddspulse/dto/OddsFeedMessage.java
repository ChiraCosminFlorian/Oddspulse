package com.oddspulse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsFeedMessage {

    private String eventId;
    private String sportType;
    private String homeTeam;
    private String awayTeam;
    private BigDecimal homeOdds;
    private BigDecimal drawOdds;
    private BigDecimal awayOdds;
    private BigDecimal totalVolume;
    private LocalDateTime eventTime;
}
