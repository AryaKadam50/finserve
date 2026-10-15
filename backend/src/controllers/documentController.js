import { DocumentService } from '../services/documentService.js';
import { successResponse, serializeData } from '../utils/response.js';

export class DocumentController {
  static async uploadDocument(req, res, next) {
    try {
      const loanId = req.params.loanId;
      const userId = req.user.id;
      const userRole = req.user.role;
      const type = req.body.type;
      const file = req.file;

      const document = await DocumentService.uploadDocument({
        loanId,
        userId,
        userRole,
        type,
        file,
      });

      return successResponse(res, 'Document uploaded successfully', serializeData(document), 201);
    } catch (error) {
      next(error);
    }
  }

  static async getDocuments(req, res, next) {
    try {
      const loanId = req.params.loanId;
      const userId = req.user.id;
      const userRole = req.user.role;

      const documents = await DocumentService.getDocumentsForLoan({
        loanId,
        userId,
        userRole,
      });

      return successResponse(res, 'Documents retrieved successfully', serializeData(documents), 200);
    } catch (error) {
      next(error);
    }
  }
}
