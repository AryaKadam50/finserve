import rateLimit from 'express-rate-limit';

/**
 * Rate Limiting Middleware for AI Underwriting
 * Enforces a maximum of 10 requests per minute globally/per-IP,
 * mirroring Spring Boot's Bucket4j configuration.
 */
export const underwritingRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 10, // 10 requests
  message: {
    success: false,
    message: 'Too many underwriting requests. Please try again later.',
    data: null,
  },
  standardHeaders: true,
  legacyHeaders: false,
});
