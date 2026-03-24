package com.oddspulse.repository;

import com.oddspulse.model.OddsEvent;
import com.oddspulse.model.OddsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OddsSnapshotRepository extends JpaRepository<OddsSnapshot, Long> {

    List<OddsSnapshot> findByEventOrderBySnapshotTimeDesc(OddsEvent event);

    Optional<OddsSnapshot> findTopByEventOrderBySnapshotTimeDesc(OddsEvent event);
}
