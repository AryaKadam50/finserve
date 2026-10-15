import test from 'node:test';
import assert from 'node:assert/strict';
import { UnderwritingService } from '../src/services/underwritingService.js';
import { DocumentProcessingService } from '../src/services/documentProcessingService.js';
import { RagPolicyService } from '../src/services/ragService.js';

test('UnderwritingService: estimateMonthlyEmi computes accurate loan amortizations', () => {
  // Principal: 100,000, 12 months, 12% p.a.
  const emi = UnderwritingService.estimateMonthlyEmi(100000, 12);
  assert.ok(emi > 8800 && emi < 8950, `Expected monthly EMI ~8885, got ${emi}`);

  // Zero tenure or principal returns 0
  assert.equal(UnderwritingService.estimateMonthlyEmi(0, 12), 0);
  assert.equal(UnderwritingService.estimateMonthlyEmi(100000, 0), 0);
});

test('DocumentProcessingService: extracts simulated fields from salary slip', () => {
  const mockDoc = { documentType: 'SALARY_SLIP' };
  const mockLoan = {
    user: { name: 'Rahul Sharma' },
    monthlyIncome: 50000,
  };

  const extracted = DocumentProcessingService.extractData(mockDoc, mockLoan);
  assert.equal(extracted.employeeName, 'Rahul Sharma');
  assert.equal(extracted.employer, 'FinCorp Technologies Pvt. Ltd.');
  assert.equal(extracted.grossIncome, '50000');
  // 2% discrepancy: 50000 * 0.98 = 49000
  assert.equal(extracted.netIncome, '49000');
});

test('RagPolicyService: parses policy chunks and executes cosine similarity', async () => {
  await RagPolicyService.initialize();
  assert.ok(RagPolicyService.chunks.length > 0, 'Expected policy chunks to be loaded');

  // Test vector similarity logic
  const v1 = [1, 0, 0];
  const v2 = [1, 0, 0];
  const v3 = [0, 1, 0];

  assert.equal(RagPolicyService.cosineSimilarity(v1, v2), 1.0);
  assert.equal(RagPolicyService.cosineSimilarity(v1, v3), 0.0);
});

test('UnderwritingService: runDeterministicMock flags DTI and income violations correctly', () => {
  const lowIncomeContext = {
    financialProfile: { monthlyIncome: 5000, creditScore: 700 },
    dtiResult: { dtiExceedsLimit: false, debtToIncomeRatio: 0.2, maxAllowedDti: 0.45 },
    documentVerification: { anyFlagged: false, anyFailed: false, totalDocuments: 2 },
    eligibilityRules: { minimumMonthlyIncome: 10000, minimumCreditScore: 650 },
    coreLoan: { purpose: 'Home improvement' },
  };

  const lowIncomeResult = UnderwritingService.runDeterministicMock(lowIncomeContext);
  assert.equal(lowIncomeResult.recommendation, 'REJECT');
  assert.equal(lowIncomeResult.riskLevel, 'HIGH');
  assert.equal(lowIncomeResult.isSimulated, true);

  const healthyContext = {
    financialProfile: { monthlyIncome: 60000, creditScore: 780 },
    dtiResult: { dtiExceedsLimit: false, debtToIncomeRatio: 0.25, maxAllowedDti: 0.45 },
    documentVerification: { anyFlagged: false, anyFailed: false, totalDocuments: 2 },
    eligibilityRules: { minimumMonthlyIncome: 10000, minimumCreditScore: 650 },
    coreLoan: { purpose: 'Debt consolidation' },
  };

  const healthyResult = UnderwritingService.runDeterministicMock(healthyContext);
  assert.equal(healthyResult.recommendation, 'APPROVE');
  assert.equal(healthyResult.riskLevel, 'LOW');
  assert.equal(healthyResult.requiresHumanReview, false);
});
