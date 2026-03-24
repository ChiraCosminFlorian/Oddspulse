package com.oddspulse.repository;

import com.oddspulse.model.OddsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OddsEventRepository extends JpaRepository<OddsEvent, Long> {

    Optional<OddsEvent> findByEventId(String eventId);

    List<OddsEvent> findBySportTypeAndStatus(String sportType, String status);
}
