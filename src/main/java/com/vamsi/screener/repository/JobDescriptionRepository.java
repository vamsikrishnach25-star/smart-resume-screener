package com.vamsi.screener.repository;

import com.vamsi.screener.model.JobDescription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for job descriptions. */
@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {

    List<JobDescription> findAllByOrderByCreatedAtDesc();
}
