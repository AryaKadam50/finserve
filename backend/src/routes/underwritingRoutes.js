import { Router } from 'express';
import { UnderwritingController } from '../controllers/underwritingController.js';
import { authenticate, requireAdmin } from '../middleware/auth.js';
import { underwritingRateLimiter } from '../middleware/rateLimiter.js';

const router = Router();

router.post(
  '/:applicationId/analyze',
  authenticate,
  requireAdmin,
  underwritingRateLimiter,
  UnderwritingController.analyze
);

export default router;
