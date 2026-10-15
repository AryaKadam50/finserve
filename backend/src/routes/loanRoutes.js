import { Router } from 'express';
import { LoanController } from '../controllers/loanController.js';
import { authenticate, requireAdmin } from '../middleware/auth.js';

const router = Router();

router.post('/', authenticate, LoanController.applyForLoan);
router.get('/', authenticate, requireAdmin, LoanController.getAllLoans);
router.get('/:id', authenticate, LoanController.getLoanById);
router.put('/:id/status', authenticate, requireAdmin, LoanController.updateLoanStatus);
router.get('/:id/audit-events', authenticate, requireAdmin, LoanController.getAuditEvents);
router.delete('/:id', authenticate, requireAdmin, LoanController.deleteLoan);
router.post('/check-eligibility', LoanController.checkEligibility);

export default router;
