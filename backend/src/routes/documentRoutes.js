import { Router } from 'express';
import { DocumentController } from '../controllers/documentController.js';
import { authenticate } from '../middleware/auth.js';
import { upload } from '../middleware/upload.js';

const router = Router({ mergeParams: true });

router.post('/', authenticate, upload.single('file'), DocumentController.uploadDocument);
router.get('/', authenticate, DocumentController.getDocuments);

export default router;
