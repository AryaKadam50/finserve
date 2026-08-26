package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.Document;
import com.finserve.model.VerificationResult;

import java.util.List;

/**
 * Interface for deterministic document verification.
 * Implementations compare extracted data against declared loan application values.
 */
public interface VerificationService {
    List<VerificationResult> verify(Document document, ExtractedDocumentData extracted);
}
