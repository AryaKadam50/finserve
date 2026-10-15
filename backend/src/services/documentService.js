import prisma from '../config/db.js';
import { NotFoundError, BadRequestError, ForbiddenError } from '../utils/response.js';
import { DocumentProcessingService } from './documentProcessingService.js';

export class DocumentService {
  /**
   * Handles document upload, storage, and triggers verification.
   */
  static async uploadDocument({ loanId, userId, userRole, type, file }) {
    if (!file) {
      throw new BadRequestError('File is required for upload.');
    }

    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(loanId) },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication #${loanId} not found.`);
    }

    // Ownership check: only owner or Admin can upload
    if (userRole !== 'ADMIN' && Number(loan.userId) !== Number(userId)) {
      throw new ForbiddenError('Access denied: You do not own this loan application.');
    }

    const document = await prisma.document.create({
      data: {
        loanApplicationId: String(loanId),
        documentType: type,
        originalFileName: file.originalname,
        storagePath: file.path,
        verificationStatus: 'PENDING',
      },
    });

    // Run extraction & deterministic verification
    await DocumentProcessingService.verifyDocument(document.id, loanId);

    // Record audit event
    await prisma.auditEvent.create({
      data: {
        applicationId: String(loanId),
        actionType: 'DOCUMENT_UPLOADED',
        description: `Uploaded ${type}: ${file.originalname}`,
        actorId: String(userId),
        actorName: userRole === 'ADMIN' ? 'Admin' : 'Applicant',
      },
    });

    const updated = await prisma.document.findUnique({
      where: { id: document.id },
      include: { verificationResults: true },
    });

    return updated;
  }

  /**
   * Retrieves documents for a loan.
   */
  static async getDocumentsForLoan({ loanId, userId, userRole }) {
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(loanId) },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication #${loanId} not found.`);
    }

    if (userRole !== 'ADMIN' && Number(loan.userId) !== Number(userId)) {
      throw new ForbiddenError('Access denied: You do not own this loan application.');
    }

    const documents = await prisma.document.findMany({
      where: { loanApplicationId: String(loanId) },
      include: { verificationResults: true },
      orderBy: { uploadedAt: 'desc' },
    });

    return documents;
  }
}
