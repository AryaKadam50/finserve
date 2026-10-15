import { Router } from 'express';
import { UserController } from '../controllers/userController.js';
import { authenticate } from '../middleware/auth.js';

const router = Router();

router.post('/register', UserController.register);
router.post('/login', UserController.login);
router.get('/:userId/loans', authenticate, UserController.getUserLoans);

export default router;
