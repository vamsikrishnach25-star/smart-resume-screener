package com.vamsi.screener.repository;

import com.vamsi.screener.model.Resume;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for parsed resumes. */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    /** Finds a previously parsed resume with identical text, so uploads are not duplicated. */
    Optional<Resume> findByContentHash(String contentHash);

    List<Resume> findAllByOrderByCreatedAtDesc();
}
