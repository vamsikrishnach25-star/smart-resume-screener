package com.vamsi.screener.repository;

import com.vamsi.screener.model.ScreeningResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for stored screening outcomes. */
@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, UUID> {

    List<ScreeningResult> findByJobDescriptionIdOrderByMatchScoreDesc(UUID jobId);

    Optional<ScreeningResult> findByResumeIdAndJobDescriptionId(UUID resumeId, UUID jobId);

    void deleteByResumeId(UUID resumeId);

    void deleteByJobDescriptionId(UUID jobId);
}
