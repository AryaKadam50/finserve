import { UnderwritingService } from '../services/underwritingService.js';
import { successResponse, serializeData } from '../utils/response.js';

export class UnderwritingController {
  static async analyze(req, res, next) {
    try {
      const applicationId = req.params.applicationId;
      const userRole = req.user.role;

      const result = await UnderwritingService.analyzeApplication(applicationId, userRole);
      return successResponse(res, 'Underwriting analysis complete', serializeData(result), 200);
    } catch (error) {
      next(error);
    }
  }
}
