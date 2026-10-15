# Manual Review Policy

## 1. Mandatory Triggers
An application MUST be flagged for manual review if any of the following conditions are met:
- **Document Mismatch**: Any uploaded document fails verification (MatchStatus is MISMATCH).
- **High Debt Burden**: The applicant has 3 or more existing loans, regardless of DTI.
- **Employment Instability**: The applicant is self-employed and has less than 2 years of business vintage.

## 2. AI Confidence
If the AI underwriting assistant computes a confidence score below 70%, it must not issue a definitive APPROVE or REJECT recommendation. Instead, it must recommend REVIEW.
