import OpenAI from 'openai';
import prisma from '../config/db.js';
import { RagPolicyService } from './ragService.js';
import { NotFoundError, BadRequestError } from '../utils/response.js';

export class UnderwritingService {
  /**
   * Deterministic EMI calculation assuming 12% p.a.
   */
  static estimateMonthlyEmi(principal, tenureMonths) {
    if (!principal || !tenureMonths || tenureMonths <= 0) return 0;
    const p = Number(principal);
    const r = 0.12 / 12; // 1% monthly
    const n = tenureMonths;
    const emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    return Math.round(emi);
  }

  /**
   * Read-only deterministic tools inspecting loan application financials
   */
  static async collectUnderwritingContext(applicationId) {
    const loan = await prisma.loanApplication.findUnique({
      where: { id: String(applicationId) },
      include: {
        user: true,
        documents: {
          include: { verificationResults: true },
        },
      },
    });

    if (!loan) {
      throw new NotFoundError(`LoanApplication not found with id ${applicationId}`);
    }

    // 1. Core loan
    const coreLoan = {
      applicationId: Number(loan.id),
      requestedAmount: Number(loan.amount),
      tenureMonths: loan.tenure,
      purpose: loan.purpose,
      status: loan.status,
      createdAt: loan.createdAt,
    };

    // 2. Financial Profile
    const income = Number(loan.monthlyIncome || 0);
    const expenses = Number(loan.monthlyExpenses || 0);
    const existingEmi = Number(loan.existingMonthlyEmi || 0);
    const creditScore = loan.creditScore || null;

    const financialProfile = {
      monthlyIncome: income,
      monthlyExpenses: expenses,
      existingMonthlyEmi: existingEmi,
      existingLoanCount: loan.existingLoanCount || 0,
      employmentType: loan.employmentType,
      yearsOfEmployment: loan.yearsOfEmployment || 0,
      creditScore,
    };

    // 3. Loan History
    const history = await prisma.loanApplication.findMany({
      where: { userId: loan.userId },
    });
    const approvedLoans = history.filter((l) => l.status === 'APPROVED');
    const rejectedLoans = history.filter((l) => l.status === 'REJECTED');
    const totalApprovedAmount = approvedLoans.reduce((sum, l) => sum + Number(l.amount), 0);

    const loanHistory = {
      totalApplications: history.length,
      approvedLoans: approvedLoans.length,
      rejectedLoans: rejectedLoans.length,
      totalApprovedAmount,
    };

    // 4. DTI Calculation
    const newEmi = this.estimateMonthlyEmi(loan.amount, loan.tenure);
    const totalObligations = existingEmi + newEmi;
    const maxDtiRatio = Number(process.env.POLICY_MAX_DTI_RATIO || 0.45);
    const dti = income > 0 ? totalObligations / income : 1.0;
    const roundedDti = Math.round(dti * 10000) / 10000;

    const dtiResult = {
      existingMonthlyEmi: existingEmi,
      estimatedNewEmi: newEmi,
      totalMonthlyObligations: totalObligations,
      monthlyIncome: income,
      debtToIncomeRatio: roundedDti,
      maxAllowedDti: maxDtiRatio,
      dtiExceedsLimit: roundedDti > maxDtiRatio,
    };

    // 5. Document Verification Results
    const docs = loan.documents || [];
    const anyFlagged = docs.some((d) => d.verificationStatus === 'FLAGGED');
    const anyFailed = docs.some((d) => d.verificationStatus === 'FAILED');
    const allVerified = docs.length > 0 && docs.every((d) => d.verificationStatus === 'VERIFIED');

    const documentVerification = {
      totalDocuments: docs.length,
      anyFlagged,
      anyFailed,
      allVerified,
      documents: docs.map((d) => ({
        documentType: d.documentType,
        verificationStatus: d.verificationStatus,
        fieldResults: (d.verificationResults || []).map((vr) => ({
          field: vr.field,
          status: vr.matchStatus,
          declared: vr.declaredValue,
          extracted: vr.extractedValue,
        })),
      })),
    };

    // 6. Eligibility Rules
    const eligibilityRules = {
      minimumMonthlyIncome: Number(process.env.POLICY_MIN_INCOME || 10000),
      minimumCreditScore: Number(process.env.POLICY_MIN_CREDIT_SCORE || 650),
      maximumDebtToIncomeRatio: maxDtiRatio,
      maximumLoanToIncomeMultiplier: Number(process.env.POLICY_MAX_LOAN_TO_INCOME_MULTIPLIER || 60),
    };

    return {
      coreLoan,
      financialProfile,
      loanHistory,
      dtiResult,
      documentVerification,
      eligibilityRules,
    };
  }

  /**
   * Deterministic Smart Mock Underwriting
   * Used when OPENAI_API_KEY is not configured or in local development.
   */
  static runDeterministicMock(context) {
    const { dtiResult, financialProfile, documentVerification, eligibilityRules, coreLoan } = context;

    const reasons = [];
    const verificationIssues = [];
    let recommendation = 'APPROVE';
    let riskLevel = 'LOW';
    let confidence = 0.90;
    let requiresHumanReview = false;

    // Check income limit
    if (financialProfile.monthlyIncome < eligibilityRules.minimumMonthlyIncome) {
      recommendation = 'REJECT';
      riskLevel = 'HIGH';
      reasons.push(`Monthly income (₹${financialProfile.monthlyIncome}) falls below minimum policy threshold of ₹${eligibilityRules.minimumMonthlyIncome}.`);
    }

    // Check DTI
    if (dtiResult.dtiExceedsLimit) {
      if (recommendation !== 'REJECT') {
        recommendation = 'REVIEW';
        riskLevel = 'HIGH';
      }
      reasons.push(`Estimated DTI ratio of ${(dtiResult.debtToIncomeRatio * 100).toFixed(1)}% exceeds the maximum allowed limit of ${(dtiResult.maxAllowedDti * 100).toFixed(0)}%.`);
    } else {
      reasons.push(`DTI ratio of ${(dtiResult.debtToIncomeRatio * 100).toFixed(1)}% is healthy and within policy limits.`);
    }

    // Check Credit Score
    if (financialProfile.creditScore) {
      if (financialProfile.creditScore < eligibilityRules.minimumCreditScore) {
        if (recommendation === 'APPROVE') recommendation = 'REVIEW';
        riskLevel = 'HIGH';
        reasons.push(`Credit score (${financialProfile.creditScore}) is below standard prime benchmark (${eligibilityRules.minimumCreditScore}).`);
      } else {
        reasons.push(`Applicant has a solid credit score of ${financialProfile.creditScore}.`);
      }
    }

    // Check Documents
    if (documentVerification.anyFlagged || documentVerification.anyFailed) {
      requiresHumanReview = true;
      if (recommendation === 'APPROVE') recommendation = 'REVIEW';
      riskLevel = riskLevel === 'LOW' ? 'MEDIUM' : riskLevel;
      verificationIssues.push('Discrepancies or unverified fields detected in uploaded proof documents.');
    } else if (documentVerification.totalDocuments === 0) {
      requiresHumanReview = true;
      if (recommendation === 'APPROVE') recommendation = 'REVIEW';
      verificationIssues.push('No income verification documents uploaded by applicant.');
    } else {
      reasons.push('All submitted verification documents verified successfully.');
    }

    if (recommendation === 'REVIEW') {
      requiresHumanReview = true;
    }

    return {
      recommendation,
      riskLevel,
      confidence,
      reasons,
      verificationIssues,
      policyReferences: [
        { policy: 'DTI Policy Guidelines', section: 'Debt-to-Income Max 45%' },
        { policy: 'Income Verification Policy', section: 'Minimum Threshold' },
      ],
      requiresHumanReview,
      aiModel: 'finserve-deterministic-mock-v1',
      isSimulated: true,
    };
  }

  /**
   * Runs the complete AI underwriting agent with RAG policy grounding.
   */
  static async analyzeApplication(applicationId, userRole) {
    if (userRole !== 'ADMIN') {
      throw new BadRequestError('Only admins can perform underwriting analysis');
    }

    const context = await this.collectUnderwritingContext(applicationId);

    // Retrieve Top-3 relevant policy chunks via RAG
    const ragQuery = `Loan purpose: ${context.coreLoan.purpose}, DTI: ${context.dtiResult.debtToIncomeRatio}, Credit Score: ${context.financialProfile.creditScore || 'N/A'}, Income: ${context.financialProfile.monthlyIncome}`;
    const policyChunks = await RagPolicyService.search(ragQuery, 3);

    const apiKey = process.env.OPENAI_API_KEY;
    let aiResult;

    if (apiKey && apiKey.trim().length > 0) {
      try {
        const openai = new OpenAI({ apiKey });
        const systemPrompt = `You are an expert commercial loan underwriting assistant at FinServe.
Analyze the applicant's financial profile, DTI ratio, and document verification results against retrieved lending policy guidelines.
Return your assessment in valid JSON adhering strictly to this schema:
{
  "recommendation": "APPROVE" | "REJECT" | "REVIEW",
  "riskLevel": "LOW" | "MEDIUM" | "HIGH",
  "confidence": number between 0.0 and 1.0,
  "reasons": string[],
  "verificationIssues": string[],
  "policyReferences": [{"policy": string, "section": string}],
  "requiresHumanReview": boolean
}`;

        const userPrompt = `Applicant Financial Context:
${JSON.stringify(context, null, 2)}

Retrieved Policy Documents:
${policyChunks.map((c) => `--- ${c.docName} (${c.section}) ---\n${c.text}`).join('\n\n')}

Provide the structured underwriting decision in JSON.`;

        const completion = await openai.chat.completions.create({
          model: process.env.OPENAI_MODEL || 'gpt-4o-mini',
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          response_format: { type: 'json_object' },
          temperature: 0.1,
        });

        const parsed = JSON.parse(completion.choices[0].message.content);
        aiResult = {
          recommendation: parsed.recommendation || 'REVIEW',
          riskLevel: parsed.riskLevel || 'MEDIUM',
          confidence: Number(parsed.confidence || 0.8),
          reasons: Array.isArray(parsed.reasons) ? parsed.reasons : [],
          verificationIssues: Array.isArray(parsed.verificationIssues) ? parsed.verificationIssues : [],
          policyReferences: Array.isArray(parsed.policyReferences) ? parsed.policyReferences : [],
          requiresHumanReview: Boolean(parsed.requiresHumanReview),
          aiModel: process.env.OPENAI_MODEL || 'gpt-4o-mini',
          isSimulated: false,
        };
      } catch (err) {
        console.warn('[Underwriting] OpenAI API call failed, falling back to mock:', err.message);
        aiResult = this.runDeterministicMock(context);
      }
    } else {
      aiResult = this.runDeterministicMock(context);
    }

    // Safety Invariant: Force human review if confidence is below cutoff or docs flagged
    const confidenceThreshold = Number(process.env.AI_CONFIDENCE_THRESHOLD || 0.70);
    if (aiResult.confidence < confidenceThreshold || context.documentVerification.anyFlagged || context.documentVerification.anyFailed) {
      aiResult.requiresHumanReview = true;
    }

    // Persist result in database for audit timeline
    const saved = await prisma.underwritingResult.create({
      data: {
        applicationId: String(applicationId),
        recommendation: aiResult.recommendation,
        riskLevel: aiResult.riskLevel,
        confidence: aiResult.confidence,
        reasons: JSON.stringify(aiResult.reasons),
        verificationIssues: JSON.stringify(aiResult.verificationIssues),
        requiresHumanReview: aiResult.requiresHumanReview,
        policyReferences: JSON.stringify(aiResult.policyReferences),
        aiModel: aiResult.aiModel,
      },
    });

    // Update loan status to AI_RECOMMENDED or PENDING_HUMAN_REVIEW (AI NEVER directly approves/rejects!)
    const nextStatus = aiResult.requiresHumanReview ? 'PENDING_HUMAN_REVIEW' : 'AI_RECOMMENDED';
    await prisma.loanApplication.update({
      where: { id: String(applicationId) },
      data: { status: nextStatus },
    });

    return {
      id: String(saved.id),
      applicationId: String(saved.applicationId),
      recommendation: saved.recommendation,
      riskLevel: saved.riskLevel,
      confidence: saved.confidence,
      reasons: aiResult.reasons,
      verificationIssues: aiResult.verificationIssues,
      policyReferences: aiResult.policyReferences,
      requiresHumanReview: saved.requiresHumanReview,
      aiModel: saved.aiModel,
      createdAt: saved.createdAt,
      isSimulated: aiResult.isSimulated,
    };
  }
}
