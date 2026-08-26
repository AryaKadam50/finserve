package com.finserve.controller;

import com.finserve.dto.ApiResponse;
import com.finserve.dto.DocumentDTO;
import com.finserve.model.DocumentType;
import com.finserve.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/loans/{loanId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentDTO>> uploadDocument(
            @PathVariable Long loanId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String userRole,
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file) {
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("X-User-Id header is required for authorization"));
        }

        DocumentDTO document = documentService.uploadDocument(loanId, userId, userRole, type, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", document));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocuments(
            @PathVariable Long loanId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String userRole) {
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("X-User-Id header is required for authorization"));
        }

        List<DocumentDTO> documents = documentService.getDocumentsForLoan(loanId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }
}
