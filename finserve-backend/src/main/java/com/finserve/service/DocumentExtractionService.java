package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.Document;

import java.io.File;

/**
 * Interface for document data extraction.
 * Implementations can be swapped (Mock, OCR, LLM-based) without changing the caller.
 */
public interface DocumentExtractionService {
    /**
     * Extract structured data from a document file.
     *
     * @param document The Document metadata entity.
     * @param file     The physical file on disk.
     * @return ExtractedDocumentData with parsed key-value fields, or null on failure.
     */
    ExtractedDocumentData extractData(Document document, File file);
}
