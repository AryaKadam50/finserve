import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applyForLoan } from '../services/api';
import { useAuth } from '../context/AuthContext';

const ApplyLoan = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    amount: '',
    monthlyIncome: '',
    employmentType: 'Salaried',
    tenure: '',
    purpose: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
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
        monthlyIncome: Number(formData.monthlyIncome),
        tenure: Number(formData.tenure),
        userId: user.id
      };
      await applyForLoan(payload);
      navigate('/my-applications', { state: { message: 'Loan application submitted successfully!' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit application.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ display: 'flex', justifyContent: 'center' }}>
      <div className="glass-card" style={{ width: '100%', maxWidth: '600px' }}>
        <h2 className="mb-6 text-center" style={{ fontSize: '1.75rem' }}>Apply for a Loan</h2>
        
        {error && <div className="mb-4" style={{ color: 'var(--danger)', background: 'rgba(239, 68, 68, 0.1)', padding: '0.75rem', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Loan Amount (₹)</label>
            <input type="number" name="amount" className="form-input" required min="1000" value={formData.amount} onChange={handleChange} />
          </div>
          
          <div className="form-group">
            <label className="form-label">Monthly Income (₹)</label>
            <input type="number" name="monthlyIncome" className="form-input" required min="1000" value={formData.monthlyIncome} onChange={handleChange} />
            {showEligibility && (
              <div style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: isEligible ? 'var(--success)' : 'var(--warning)' }}>
                {isEligible ? '✓ Likely Eligible' : '⚠ May need review'}
              </div>
            )}
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
            <label className="form-label">Tenure (Months)</label>
            <input type="number" name="tenure" className="form-input" required min="1" max="120" value={formData.tenure} onChange={handleChange} />
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>e.g. 12 months = 1 year</span>
          </div>
          
          <div className="form-group">
            <label className="form-label">Purpose</label>
            <textarea name="purpose" className="form-textarea" required rows="3" value={formData.purpose} onChange={handleChange}></textarea>
          </div>
          
          <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? 'Submitting...' : 'Submit Application'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ApplyLoan;
