import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applyForLoan, uploadDocument } from '../services/api';
import { useAuth } from '../context/AuthContext';

const ApplyLoan = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    amount: '',
    tenure: '',
    purpose: '',
    monthlyIncome: '',
    monthlyExpenses: '',
    existingMonthlyEmi: '0',
    existingLoanCount: '0',
    employmentType: 'Salaried',
    yearsOfEmployment: '',
    creditScore: ''
  });
  
  const [documents, setDocuments] = useState({
    salarySlip: null,
    employmentProof: null
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e) => {
    setDocuments({ ...documents, [e.target.name]: e.target.files[0] });
  };

  const isEligible = Number(formData.monthlyIncome) >= 50000;
  const showEligibility = formData.monthlyIncome.length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    try {
      setLoading(true);
      const payload = {
        ...formData,
        amount: Number(formData.amount),
        tenure: Number(formData.tenure),
        monthlyIncome: Number(formData.monthlyIncome),
        monthlyExpenses: formData.monthlyExpenses ? Number(formData.monthlyExpenses) : null,
        existingMonthlyEmi: formData.existingMonthlyEmi ? Number(formData.existingMonthlyEmi) : 0,
        existingLoanCount: formData.existingLoanCount ? Number(formData.existingLoanCount) : 0,
        yearsOfEmployment: formData.yearsOfEmployment ? Number(formData.yearsOfEmployment) : null,
        creditScore: formData.creditScore ? Number(formData.creditScore) : null,
        userId: user.id
      };
      
      const res = await applyForLoan(payload);
      const loanId = res.data?.data?.id;

      if (loanId) {
        // Upload documents if selected
        if (documents.salarySlip) {
          await uploadDocument(loanId, 'SALARY_SLIP', documents.salarySlip);
        }
        if (documents.employmentProof) {
          await uploadDocument(loanId, 'EMPLOYMENT_PROOF', documents.employmentProof);
        }
      }

      navigate('/my-applications', { state: { message: 'Loan application submitted successfully!' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit application.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ display: 'flex', justifyContent: 'center' }}>
      <div className="glass-card" style={{ width: '100%', maxWidth: '800px' }}>
        <h2 className="mb-6 text-center" style={{ fontSize: '1.75rem' }}>Apply for a Loan</h2>
        
        {error && <div className="mb-4" style={{ color: 'var(--danger)', background: 'rgba(239, 68, 68, 0.1)', padding: '0.75rem', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          
          <h3 className="mb-4" style={{ borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>1. Loan Details</h3>
          <div className="grid grid-cols-2 gap-4 mb-6">
            <div className="form-group">
              <label className="form-label">Loan Amount (₹)</label>
              <input type="number" name="amount" className="form-input" required min="1000" value={formData.amount} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Tenure (Months)</label>
              <input type="number" name="tenure" className="form-input" required min="1" max="120" value={formData.tenure} onChange={handleChange} />
            </div>
            <div className="form-group col-span-2">
              <label className="form-label">Purpose</label>
              <textarea name="purpose" className="form-textarea" required rows="2" value={formData.purpose} onChange={handleChange}></textarea>
            </div>
          </div>

          <h3 className="mb-4" style={{ borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>2. Financial Profile</h3>
          <div className="grid grid-cols-2 gap-4 mb-6">
            <div className="form-group">
              <label className="form-label">Monthly Income (₹)</label>
              <input type="number" name="monthlyIncome" className="form-input" required min="1000" value={formData.monthlyIncome} onChange={handleChange} />
              {showEligibility && (
                <div style={{ marginTop: '0.25rem', fontSize: '0.875rem', color: isEligible ? 'var(--success)' : 'var(--warning)' }}>
                  {isEligible ? '✓ Likely Eligible' : '⚠ May need review'}
                </div>
              )}
            </div>
            <div className="form-group">
              <label className="form-label">Monthly Expenses (₹)</label>
              <input type="number" name="monthlyExpenses" className="form-input" required min="0" value={formData.monthlyExpenses} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Existing Monthly EMI (₹)</label>
              <input type="number" name="existingMonthlyEmi" className="form-input" min="0" value={formData.existingMonthlyEmi} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Existing Loan Count</label>
              <input type="number" name="existingLoanCount" className="form-input" min="0" value={formData.existingLoanCount} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Employment Type</label>
              <select name="employmentType" className="form-select" value={formData.employmentType} onChange={handleChange}>
                <option value="Salaried">Salaried</option>
                <option value="Self-Employed">Self-Employed</option>
                <option value="Business">Business</option>
                <option value="Freelancer">Freelancer</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Years of Employment</label>
              <input type="number" name="yearsOfEmployment" className="form-input" required min="0" value={formData.yearsOfEmployment} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Credit Score (Synthetic)</label>
              <input type="number" name="creditScore" className="form-input" min="300" max="900" value={formData.creditScore} onChange={handleChange} />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Enter a score between 300-900</span>
            </div>
          </div>

          <h3 className="mb-4" style={{ borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>3. Supporting Documents (Optional)</h3>
          <div className="grid grid-cols-2 gap-4 mb-8">
            <div className="form-group">
              <label className="form-label">Salary Slip</label>
              <input type="file" name="salarySlip" className="form-input" onChange={handleFileChange} accept=".pdf,.png,.jpg,.jpeg" />
            </div>
            <div className="form-group">
              <label className="form-label">Employment Proof</label>
              <input type="file" name="employmentProof" className="form-input" onChange={handleFileChange} accept=".pdf,.png,.jpg,.jpeg" />
            </div>
          </div>
          
          <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '1rem', fontSize: '1.1rem' }} disabled={loading}>
            {loading ? 'Submitting Application...' : 'Submit Application'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ApplyLoan;
