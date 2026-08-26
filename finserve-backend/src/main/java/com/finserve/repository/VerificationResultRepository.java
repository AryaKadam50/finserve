package com.finserve.repository;

import com.finserve.model.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {
    List<VerificationResult> findByDocumentId(Long documentId);
}
