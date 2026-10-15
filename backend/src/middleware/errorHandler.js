import { AppError } from '../utils/response.js';

/**
 * Global Express centralized error handling middleware.
 */
export const errorHandler = (err, req, res, next) => {
  let error = { ...err };
  error.message = err.message;
  error.statusCode = err.statusCode || 500;

  // Log error in development
  if (process.env.NODE_ENV === 'development') {
    console.error('Unhandled Error:', err);
  }

  // Handle Prisma Known Request Errors
  if (err.code === 'P2002') {
    const target = err.meta?.target || 'field';
    error = new AppError(`Duplicate value for ${target}. Please use another value.`, 400);
  } else if (err.code === 'P2025') {
    error = new AppError('Record not found', 404);
  }

  // Handle JWT errors
  if (err.name === 'JsonWebTokenError') {
    error = new AppError('Invalid token. Please log in again.', 401);
  }
  if (err.name === 'TokenExpiredError') {
    error = new AppError('Your token has expired. Please log in again.', 401);
  }

  return res.status(error.statusCode).json({
    success: false,
    message: error.message || 'Internal Server Error',
    data: null,
  });
};
