package com.finserve.service;

import com.finserve.dto.DocumentDTO;
import com.finserve.dto.ExtractedDocumentData;
import com.finserve.exception.BadRequestException;
import com.finserve.model.*;
import com.finserve.repository.DocumentRepository;
import com.finserve.repository.LoanRepository;
import com.finserve.repository.VerificationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private VerificationResultRepository verificationResultRepository;
    @Mock private DocumentExtractionService documentExtractionService;
    @Mock private VerificationService verificationService;
    @Mock private com.finserve.repository.AuditEventRepository auditEventRepository;

    @InjectMocks
    private DocumentService documentService;

    private LoanApplication loan;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "uploadDir", "./test-uploads");

        user = new User();
        user.setId(1L);
        user.setName("John Doe");

        loan = new LoanApplication();
        loan.setId(100L);
        loan.setUser(user);
        loan.setMonthlyIncome(java.math.BigDecimal.valueOf(80000));
    }

    // ─── Authorization Tests ─────────────────────────────────────────────────

    @Test
    void uploadDocument_AuthorizedOwner_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentExtractionService.extractData(any(), any())).thenReturn(null);

        DocumentDTO result = documentService.uploadDocument(100L, 1L, "USER", DocumentType.SALARY_SLIP, file);

        assertNotNull(result);
        assertEquals("test.pdf", result.getOriginalFileName());
    }

    @Test
    void uploadDocument_UnauthorizedUser_ThrowsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        // User 2 trying to upload to loan owned by User 1
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            documentService.uploadDocument(100L, 2L, "USER", DocumentType.SALARY_SLIP, file));

        assertTrue(ex.getMessage().contains("not authorized"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_AdminBypassesOwnership_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentExtractionService.extractData(any(), any())).thenReturn(null);

        // Admin with userId=999 (not the owner) can still upload
        DocumentDTO result = documentService.uploadDocument(100L, 999L, "ADMIN", DocumentType.SALARY_SLIP, file);
        assertNotNull(result);
    }

    // ─── Extraction Pipeline Tests ───────────────────────────────────────────

    @Test
    void uploadDocument_ExtractionSucceeds_TriggersVerification() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExtractedDocumentData mockData = new ExtractedDocumentData();
        mockData.addField("netIncome", "80000");
        when(documentExtractionService.extractData(any(), any())).thenReturn(mockData);

        documentService.uploadDocument(100L, 1L, "USER", DocumentType.SALARY_SLIP, file);

        verify(verificationService, times(1)).verify(any(), any());
    }

    @Test
    void uploadDocument_ExtractionReturnsNull_StatusSetToFailed() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentExtractionService.extractData(any(), any())).thenReturn(null);

        DocumentDTO result = documentService.uploadDocument(100L, 1L, "USER", DocumentType.SALARY_SLIP, file);

        assertEquals(VerificationStatus.FAILED, result.getVerificationStatus());
        verify(verificationService, never()).verify(any(), any());
    }
}
