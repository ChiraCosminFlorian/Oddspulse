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
public class OddsSnapshotResponse {

    private Long id;
    private Long eventId;
    private BigDecimal homeOdds;
    private BigDecimal drawOdds;
    private BigDecimal awayOdds;
    private BigDecimal totalVolume;
    private LocalDateTime snapshotTime;
    private String source;
}
