import fs from 'fs';
import { VerificationStatus, MatchStatus } from '@prisma/client';
import prisma from '../config/db.js';

export class DocumentProcessingService {
  /**
   * Deterministic simulated extraction service.
   * Mirrors Spring Boot's MockDocumentExtractionServiceImpl.
   * Simulates an extraction where net income is 2% lower than declared
   * to trigger realistic verification rules.
   */
  static extractData(document, loanApplication) {
    if (!document || !loanApplication) return null;

    const applicantName = loanApplication.user?.name || 'Unknown';
    const declaredMonthlyIncome = Number(loanApplication.monthlyIncome || 0);

    const extractedFields = {};

    if (document.documentType === 'SALARY_SLIP') {
      extractedFields.employeeName = applicantName;
      extractedFields.employer = 'FinCorp Technologies Pvt. Ltd.';
      extractedFields.payPeriod = 'Recent Pay Period';

      if (declaredMonthlyIncome > 0) {
        // Simulated discrepancy: extracted net income is 2% lower
        const extractedGross = declaredMonthlyIncome;
        const extractedNet = Math.round(declaredMonthlyIncome * 0.98);
        extractedFields.grossIncome = String(extractedGross);
        extractedFields.netIncome = String(extractedNet);
      }
    } else if (document.documentType === 'EMPLOYMENT_PROOF') {
      extractedFields.employeeName = applicantName;
      extractedFields.employer = 'FinCorp Technologies Pvt. Ltd.';
      extractedFields.employmentStatus = 'Active';
      extractedFields.employmentDate = '2022-04-01';
    }

    return extractedFields;
  }

  /**
   * Verifies extracted document data against declared loan application fields.
   * Mirrors Spring Boot's VerificationServiceImpl.
   * 
   * Tolerance rule:
   * toleranceAmount = declared * (incomeTolerancePercent / 100)
   * |declared - extracted| <= toleranceAmount -> MATCH
   */
  static async verifyDocument(documentId, loanApplicationId) {
    const document = await prisma.document.findUnique({
      where: { id: String(documentId) },
    });
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(loanApplicationId) },
      include: { user: true },
    });

    if (!document || !loan) return [];

    const extracted = this.extractData(document, loan);
    if (!extracted) {
      await prisma.document.update({
        where: { id: String(documentId) },
        data: { verificationStatus: VerificationStatus.FAILED },
      });
      return [];
    }

    const incomeTolerancePercent = Number(process.env.VERIFICATION_INCOME_TOLERANCE_PERCENT || 5);
    const verificationResults = [];
    let anyMismatch = false;

    if (document.documentType === 'SALARY_SLIP') {
      // 1. Net Income Verification
      if (extracted.netIncome && loan.monthlyIncome) {
        const declared = Number(loan.monthlyIncome);
        const extractedIncome = Number(extracted.netIncome);
        const tolerance = declared * (incomeTolerancePercent / 100);
        const isMatch = Math.abs(declared - extractedIncome) <= tolerance;
        const status = isMatch ? MatchStatus.MATCH : MatchStatus.MISMATCH;

        if (status === MatchStatus.MISMATCH) anyMismatch = true;

        verificationResults.push({
          documentId: String(documentId),
          field: 'Monthly Income',
          declaredValue: String(declared),
          extractedValue: String(extractedIncome),
          matchStatus: status,
          confidence: 0.95,
        });
      }

      // 2. Employee Name Verification
      if (extracted.employeeName && loan.user?.name) {
        const declared = loan.user.name.trim();
        const extractedName = extracted.employeeName.trim();
        const isMatch = declared.toLowerCase() === extractedName.toLowerCase();
        const status = isMatch ? MatchStatus.MATCH : MatchStatus.MISMATCH;

        if (status === MatchStatus.MISMATCH) anyMismatch = true;

        verificationResults.push({
          documentId: String(documentId),
          field: 'Employee Name',
          declaredValue: declared,
          extractedValue: extractedName,
          matchStatus: status,
          confidence: 0.90,
        });
      }

      // 3. Employer info (Informational)
      if (extracted.employer) {
        verificationResults.push({
          documentId: String(documentId),
          field: 'Employer',
          declaredValue: 'N/A (Declared)',
          extractedValue: extracted.employer,
          matchStatus: MatchStatus.NOT_FOUND,
          confidence: 0.80,
        });
      }
    } else if (document.documentType === 'EMPLOYMENT_PROOF') {
      // 1. Employment Status
      if (extracted.employmentStatus) {
        const isMatch = extracted.employmentStatus.toLowerCase() === 'active';
        const status = isMatch ? MatchStatus.MATCH : MatchStatus.MISMATCH;

        if (status === MatchStatus.MISMATCH) anyMismatch = true;

        verificationResults.push({
          documentId: String(documentId),
          field: 'Employment Status',
          declaredValue: 'Active',
          extractedValue: extracted.employmentStatus,
          matchStatus: status,
          confidence: 0.90,
        });
      }

      // 2. Employee Name
      if (extracted.employeeName && loan.user?.name) {
        const declared = loan.user.name.trim();
        const extractedName = extracted.employeeName.trim();
        const isMatch = declared.toLowerCase() === extractedName.toLowerCase();
        const status = isMatch ? MatchStatus.MATCH : MatchStatus.MISMATCH;

        if (status === MatchStatus.MISMATCH) anyMismatch = true;

        verificationResults.push({
          documentId: String(documentId),
          field: 'Employee Name',
          declaredValue: declared,
          extractedValue: extractedName,
          matchStatus: status,
          confidence: 0.90,
        });
      }

      // 3. Employer
      if (extracted.employer) {
        verificationResults.push({
          documentId: String(documentId),
          field: 'Employer',
          declaredValue: 'N/A (Declared)',
          extractedValue: extracted.employer,
          matchStatus: MatchStatus.NOT_FOUND,
          confidence: 0.80,
        });
      }
    }

    // Persist all results inside a transaction
    await prisma.$transaction([
      prisma.verificationResult.createMany({
        data: verificationResults,
      }),
      prisma.document.update({
        where: { id: String(documentId) },
        data: {
          verificationStatus: anyMismatch ? VerificationStatus.FLAGGED : VerificationStatus.VERIFIED,
        },
      }),
    ]);

    return verificationResults;
  }
}
