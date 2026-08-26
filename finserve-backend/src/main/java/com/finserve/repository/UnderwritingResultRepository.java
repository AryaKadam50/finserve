package com.finserve.repository;

import com.finserve.model.UnderwritingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnderwritingResultRepository extends JpaRepository<UnderwritingResult, Long> {
    Optional<UnderwritingResult> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    List<UnderwritingResult> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
