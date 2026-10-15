import prisma from '../config/db.js';
import { NotFoundError, BadRequestError } from '../utils/response.js';

export class LoanService {
  /**
   * Applies for a new loan.
   * Auto-evaluates initial status based on minimum income criteria.
   */
  static async applyForLoan(data) {
    const {
      userId,
      amount,
      tenure,
      monthlyIncome,
      monthlyExpenses,
      existingMonthlyEmi,
      existingLoanCount,
      employmentType,
      yearsOfEmployment,
      creditScore,
      purpose,
    } = data;

    if (!userId || !amount || !tenure || !monthlyIncome || !employmentType) {
      throw new BadRequestError('Required loan fields missing (userId, amount, tenure, monthlyIncome, employmentType).');
    }

    const minIncome = Number(process.env.POLICY_MIN_INCOME || 10000);
    const parsedIncome = Number(monthlyIncome);
    const initialStatus = parsedIncome >= minIncome ? 'PENDING' : 'UNDER_REVIEW';

    const loan = await prisma.loanApplication.create({
      data: {
        userId: String(userId),
        amount: Number(amount),
        tenure: Number(tenure),
        monthlyIncome: parsedIncome,
        monthlyExpenses: monthlyExpenses ? Number(monthlyExpenses) : null,
        existingMonthlyEmi: existingMonthlyEmi ? Number(existingMonthlyEmi) : null,
        existingLoanCount: existingLoanCount ? Number(existingLoanCount) : 0,
        employmentType: String(employmentType),
        yearsOfEmployment: yearsOfEmployment ? Number(yearsOfEmployment) : null,
        creditScore: creditScore ? Number(creditScore) : null,
        purpose: purpose ? String(purpose) : null,
        status: initialStatus,
      },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
      },
    });

    // Record submission audit event
    await prisma.auditEvent.create({
      data: {
        applicationId: loan.id,
        actionType: 'APPLICATION_SUBMITTED',
        description: `Loan application submitted for ₹${amount}. Initial status: ${initialStatus}`,
        actorId: String(userId),
        actorName: loan.user?.name || 'Applicant',
      },
    });

    return loan;
  }

  /**
   * Returns all loan applications (for Admin dashboard).
   */
  static async getAllLoans() {
    return prisma.loanApplication.findMany({
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
   * Returns a single loan application by ID.
   */
  static async getLoanById(id) {
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(id) },
      include: {
        user: {
          select: { id: true, name: true, email: true, phone: true },
        },
      },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication #${id} not found.`);
    }

    return loan;
  }

  /**
   * Returns all loans for a specific user.
   */
  static async getUserLoans(userId) {
    return prisma.loanApplication.findMany({
      where: { userId: String(userId) },
      include: {
        user: {
          select: { id: true, name: true, email: true },
        },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
   * Updates loan status with mandatory override check.
   */
  static async updateLoanStatus(id, { status, overrideReason }, adminId, adminName) {
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(id) },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication #${id} not found.`);
    }

    // Check latest AI underwriting result for override rules
    const latestAi = await prisma.underwritingResult.findFirst({
      where: { applicationId: String(id) },
      orderBy: { createdAt: 'desc' },
    });

    if (latestAi) {
      let isOverride = false;
      if (latestAi.recommendation === 'REJECT' && status === 'APPROVED') {
        isOverride = true;
      } else if (latestAi.recommendation === 'APPROVE' && status === 'REJECTED') {
        isOverride = true;
      } else if (latestAi.requiresHumanReview && status === 'APPROVED') {
        isOverride = true;
      }

      if (isOverride && (!overrideReason || !overrideReason.trim())) {
        throw new BadRequestError('An override reason is required when making a decision contrary to the AI recommendation.');
      }
    }

    // Execute update and audit event in a transaction
    const [updatedLoan] = await prisma.$transaction([
      prisma.loanApplication.update({
        where: { id: String(id) },
        data: { status },
        include: {
          user: {
            select: { id: true, name: true, email: true },
          },
        },
      }),
      prisma.auditEvent.create({
        data: {
          applicationId: String(id),
          actionType: overrideReason ? `ADMIN_OVERRIDE_${status}` : `ADMIN_DECISION_${status}`,
          description: overrideReason
            ? `Admin changed status to ${status}. Override Reason: ${overrideReason}`
            : `Admin changed status to ${status}.`,
          actorId: adminId ? String(adminId) : null,
          actorName: adminName || 'Admin',
        },
      }),
    ]);

    return updatedLoan;
  }

  /**
   * Deletes a loan application.
   */
  static async deleteLoan(id) {
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(id) },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication #${id} not found.`);
    }

    await prisma.loanApplication.delete({
      where: { id: String(id) },
    });

    return true;
  }

  /**
   * Retrieves audit events for a loan.
   */
  static async getAuditEvents(id) {
    return prisma.auditEvent.findMany({
      where: { applicationId: String(id) },
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
   * Quick preliminary eligibility calculation.
   */
  static checkEligibility({ monthlyIncome, requestedAmount, tenure }) {
    const minIncome = Number(process.env.POLICY_MIN_INCOME || 10000);
    const income = Number(monthlyIncome || 0);

    if (income >= minIncome) {
      return {
        eligible: true,
        message: 'Congratulations! You are eligible for the loan.',
      };
    }
    return {
      eligible: false,
      message: 'Your application needs further review based on income criteria.',
    };
  }
}
