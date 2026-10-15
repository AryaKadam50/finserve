import express from 'express';
import cors from 'cors';
import userRoutes from './routes/userRoutes.js';
import loanRoutes from './routes/loanRoutes.js';
import documentRoutes from './routes/documentRoutes.js';
import underwritingRoutes from './routes/underwritingRoutes.js';
import { errorHandler } from './middleware/errorHandler.js';

const app = express();

// Enable CORS for frontend Vite dev server and origins
app.use(
  cors({
    origin: ['http://localhost:5173', 'http://127.0.0.1:5173', 'http://localhost:3000'],
    credentials: true,
  })
);

// Body parsing
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health Check / Actuator Equivalent
app.get('/api/health', (req, res) => {
  res.status(200).json({ status: 'UP', service: 'finserve-node-backend' });
});

// API Routes
app.use('/api/users', userRoutes);
app.use('/api/loans', loanRoutes);
app.use('/api/loans/:loanId/documents', documentRoutes);
app.use('/api/underwriting', underwritingRoutes);

// Fallback for 404s
app.use((req, res, next) => {
  res.status(404).json({
    success: false,
    message: `Endpoint not found: ${req.method} ${req.originalUrl}`,
    data: null,
  });
});

// Centralized Error Handling Middleware
app.use(errorHandler);

export default app;
