/**
 * Custom application error class with HTTP status code.
 */
export class AppError extends Error {
  constructor(message, statusCode = 500) {
    super(message);
    this.statusCode = statusCode;
    this.status = `${statusCode}`.startsWith('4') ? 'fail' : 'error';
    this.isOperational = true;

    Error.captureStackTrace(this, this.constructor);
  }
}

export class NotFoundError extends AppError {
  constructor(message = 'Resource not found') {
    super(message, 404);
  }
}

export class BadRequestError extends AppError {
  constructor(message = 'Bad request') {
    super(message, 400);
  }
}

export class UnauthorizedError extends AppError {
  constructor(message = 'Unauthorized') {
    super(message, 401);
  }
}

export class ForbiddenError extends AppError {
  constructor(message = 'Forbidden') {
    super(message, 403);
  }
}

/**
 * Standard ApiResponse helper matching Spring Boot's ApiResponse structure:
 * { success: boolean, message: string, data: any }
 */
export const successResponse = (res, message, data = null, statusCode = 200) => {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
};

export const errorResponse = (res, message, statusCode = 400) => {
  return res.status(statusCode).json({
    success: false,
    message,
    data: null,
  });
};

/**
 * Safely converts BigInt values to Numbers/Strings and Decimals to Numbers
 * for JSON serialization.
 */
export const serializeData = (obj) => {
  if (obj === null || obj === undefined) return obj;
  if (typeof obj === 'bigint') return Number(obj);
  if (Array.isArray(obj)) return obj.map(serializeData);
  if (typeof obj === 'object') {
    // If it's a Prisma Decimal object
    if (obj.d && obj.e !== undefined) {
      return Number(obj.toString());
    }
    const copy = {};
    for (const key of Object.keys(obj)) {
      copy[key] = serializeData(obj[key]);
    }
    return copy;
  }
  return obj;
};
