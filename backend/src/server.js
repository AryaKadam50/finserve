import dotenv from 'dotenv';
dotenv.config();

import app from './app.js';
import { UserService } from './services/userService.js';
import { RagPolicyService } from './services/ragService.js';

const PORT = process.env.PORT || 8080;

const server = app.listen(PORT, async () => {
  console.log(`=======================================================`);
  console.log(`  FinServe Node.js/Express Backend running on port ${PORT}`);
  console.log(`  Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`=======================================================`);

  // Initialize RAG knowledge base on startup
  try {
    await RagPolicyService.initialize();
  } catch (err) {
    console.warn('[Server Startup] Policy ingestion notice:', err.message);
  }

  // Auto-seed initial admin if needed
  try {
    await UserService.bootstrapAdmin();
  } catch (err) {
    console.warn('[Server Startup] Admin bootstrap notice:', err.message);
  }
});

// Handle graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received. Shutting down gracefully...');
  server.close(() => {
    console.log('Process terminated.');
  });
});

export default server;
