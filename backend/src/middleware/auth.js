import jwt from 'jsonwebtoken';
import { UnauthorizedError, ForbiddenError } from '../utils/response.js';
import prisma from '../config/db.js';

/**
 * Authentication Middleware:
 * Supports:
 * 1. Standard "Authorization: Bearer <token>"
 * 2. Fallback to "X-User-Id" & "X-User-Role" headers during migration
 */
export const authenticate = async (req, res, next) => {
  try {
    let token;
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      token = authHeader.split(' ')[1];
    }

    if (token) {
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'finserve_super_secret_jwt_key_local_dev_2026');
      
      const user = await prisma.user.findUnique({
        where: { id: String(decoded.id) },
        select: { id: true, name: true, email: true, role: true },
      });

      if (!user) {
        throw new UnauthorizedError('User belonging to this token no longer exists');
      }

      req.user = {
        id: String(user.id),
        name: user.name,
        email: user.email,
        role: user.role,
      };
      return next();
    }

    // Fallback: Check X-User-Id header
    const xUserId = req.headers['x-user-id'];
    const xUserRole = req.headers['x-user-role'] || 'USER';

    if (xUserId) {
      const user = await prisma.user.findUnique({
        where: { id: String(xUserId) },
        select: { id: true, name: true, email: true, role: true },
      });

      if (user) {
        req.user = {
          id: String(user.id),
          name: user.name,
          email: user.email,
          role: user.role || xUserRole,
        };
        return next();
      }
    }

    throw new UnauthorizedError('Authentication required. Please provide a valid Bearer token or credentials.');
  } catch (error) {
    next(error);
  }
};

/**
 * Role-Based Access Control Middleware
 * Example: requireRole('ADMIN')
 */
export const requireRole = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return next(new ForbiddenError(`Access denied. Requires one of: ${roles.join(', ')}`));
    }
    next();
  };
};

/**
 * Admin-Only convenience middleware
 */
export const requireAdmin = requireRole('ADMIN');
