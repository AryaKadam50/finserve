import { LoanService } from '../services/loanService.js';
import { serializeData, successResponse } from '../utils/response.js';

export class LoanController {
  static async applyForLoan(req, res, next) {
    try {
      const loan = await LoanService.applyForLoan(req.body);
      return res.status(201).json(serializeData(loan));
    } catch (error) {
      next(error);
    }
  }

  static async getAllLoans(req, res, next) {
    try {
      const loans = await LoanService.getAllLoans();
      return res.status(200).json(serializeData(loans));
    } catch (error) {
      next(error);
    }
  }

  static async getLoanById(req, res, next) {
    try {
      const loan = await LoanService.getLoanById(req.params.id);
      return res.status(200).json(serializeData(loan));
    } catch (error) {
      next(error);
    }
  }

  static async updateLoanStatus(req, res, next) {
    try {
      const adminId = req.user?.id;
      const adminName = req.user?.name || `Admin User (ID: ${adminId})`;
      const updated = await LoanService.updateLoanStatus(req.params.id, req.body, adminId, adminName);
      return res.status(200).json(serializeData(updated));
    } catch (error) {
      next(error);
    }
  }

  static async getAuditEvents(req, res, next) {
    try {
      const events = await LoanService.getAuditEvents(req.params.id);
      return res.status(200).json(serializeData(events));
    } catch (error) {
      next(error);
    }
  }

  static async deleteLoan(req, res, next) {
    try {
      await LoanService.deleteLoan(req.params.id);
      return res.status(204).send();
    } catch (error) {
      next(error);
    }
  }

  static async checkEligibility(req, res, next) {
    try {
      const result = LoanService.checkEligibility(req.body);
      return res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }
}
