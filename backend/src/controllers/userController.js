import { UserService } from '../services/userService.js';
import { LoanService } from '../services/loanService.js';
import { successResponse, serializeData } from '../utils/response.js';

export class UserController {
  static async register(req, res, next) {
    try {
      const result = await UserService.register(req.body);
      return successResponse(res, 'Registration successful', result, 201);
    } catch (error) {
      next(error);
    }
  }

  static async login(req, res, next) {
    try {
      const result = await UserService.login(req.body);
      return successResponse(res, 'Login successful', result, 200);
    } catch (error) {
      next(error);
    }
  }

  static async getUserLoans(req, res, next) {
    try {
      const targetUserId = String(req.params.userId);

      // Ownership check: regular users can only fetch their own applications
      if (req.user.role !== 'ADMIN' && String(req.user.id) !== targetUserId) {
        return res.status(403).json({
          success: false,
          message: 'Access denied: You cannot view loan applications of other borrowers.',
          data: null,
        });
      }

      const loans = await LoanService.getUserLoans(targetUserId);
      return res.status(200).json(serializeData(loans));
    } catch (error) {
      next(error);
    }
  }
}
