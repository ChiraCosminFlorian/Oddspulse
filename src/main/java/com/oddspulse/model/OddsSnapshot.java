package com.oddspulse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "odds_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OddsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private OddsEvent event;

    @Column(precision = 10, scale = 2)
    private BigDecimal homeOdds;

    @Column(precision = 10, scale = 2)
    private BigDecimal drawOdds;

    @Column(precision = 10, scale = 2)
    private BigDecimal awayOdds;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalVolume;

    private LocalDateTime snapshotTime;

    private String source;
}
