package com.finserve.repository;

import com.finserve.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
