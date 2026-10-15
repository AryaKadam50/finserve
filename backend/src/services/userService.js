import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import prisma from '../config/db.js';
import { BadRequestError, NotFoundError, UnauthorizedError } from '../utils/response.js';

export class UserService {
  /**
   * Registers a new borrower.
   */
  static async register({ name, email, password, phone }) {
    if (!name || !email || !password) {
      throw new BadRequestError('Name, email, and password are required.');
    }

    if (password.length < 6) {
      throw new BadRequestError('Password must be at least 6 characters long.');
    }

    const existingUser = await prisma.user.findUnique({
      where: { email: email.toLowerCase().trim() },
    });

    if (existingUser) {
      throw new BadRequestError('Email is already registered.');
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const user = await prisma.user.create({
      data: {
        name: name.trim(),
        email: email.toLowerCase().trim(),
        password: hashedPassword,
        phone: phone || null,
        role: 'USER',
      },
    });

    const token = jwt.sign(
      { id: String(user.id), email: user.email, role: user.role, name: user.name },
      process.env.JWT_SECRET || 'finserve_super_secret_jwt_key_local_dev_2026',
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    return {
      id: String(user.id),
      name: user.name,
      email: user.email,
      role: user.role,
      token,
      message: 'Registration successful',
    };
  }

  /**
   * Authenticates a user (borrower or admin).
   */
  static async login({ email, password }) {
    if (!email || !password) {
      throw new BadRequestError('Email and password are required.');
    }

    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase().trim() },
    });

    if (!user) {
      throw new NotFoundError('User not found with this email.');
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      throw new UnauthorizedError('Invalid credentials.');
    }

    const token = jwt.sign(
      { id: String(user.id), email: user.email, role: user.role, name: user.name },
      process.env.JWT_SECRET || 'finserve_super_secret_jwt_key_local_dev_2026',
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    return {
      id: String(user.id),
      name: user.name,
      email: user.email,
      role: user.role,
      token,
      message: 'Login successful',
    };
  }

  /**
   * Automatically bootstrap/seed an admin user on server start if none exists.
   */
  static async bootstrapAdmin() {
    const adminEmail = process.env.ADMIN_EMAIL || 'admin@finserve.com';
    const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';
    const adminName = process.env.ADMIN_NAME || 'FinServe Admin';
    const adminPhone = process.env.ADMIN_PHONE || '9999999999';

    try {
      const exists = await prisma.user.findUnique({
        where: { email: adminEmail },
      });

      if (!exists) {
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(adminPassword, salt);

        await prisma.user.create({
          data: {
            name: adminName,
            email: adminEmail,
            password: hashedPassword,
            phone: adminPhone,
            role: 'ADMIN',
          },
        });
        console.log(`[AdminSeeder] Initial admin created: ${adminEmail}`);
      }
    } catch (err) {
      console.warn('[AdminSeeder] Could not auto-seed admin (database may not be ready yet):', err.message);
    }
  }
}
