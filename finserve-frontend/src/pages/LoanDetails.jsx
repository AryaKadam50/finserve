import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getLoanById } from '../services/api';
import StatusBadge from '../components/StatusBadge';

const LoanDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loan, setLoan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchLoan = async () => {
      try {
        const res = await getLoanById(id);
        setLoan(res.data);
      } catch (err) {
        setError('Failed to fetch loan details.');
      } finally {
        setLoading(false);
      }
    };
    fetchLoan();
  }, [id]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  if (loading) return <div className="page-container text-center"><div style={{ padding: '4rem' }}>Loading...</div></div>;
  if (error || !loan) return <div className="page-container text-center"><div className="glass-card mt-4">{error || 'Loan not found'}</div></div>;

  return (
    <div className="page-container" style={{ display: 'flex', justifyContent: 'center' }}>
      <div className="glass-card" style={{ width: '100%', maxWidth: '700px' }}>
        <div className="flex justify-between items-center mb-6">
          <h2 style={{ fontSize: '1.5rem' }}>Loan Application #{id}</h2>
          <StatusBadge status={loan.status} />
        </div>

        <div className="grid grid-cols-2 gap-4 mb-6">
          <div style={{ background: 'rgba(17, 24, 39, 0.5)', padding: '1rem', borderRadius: '8px' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Amount</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>{formatCurrency(loan.amount)}</div>
          </div>
          <div style={{ background: 'rgba(17, 24, 39, 0.5)', padding: '1rem', borderRadius: '8px' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Tenure</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>
              {loan.tenure} months 
              <span style={{ fontSize: '0.875rem', fontWeight: 'normal', color: 'var(--text-secondary)', marginLeft: '0.5rem' }}>
                ({Math.floor(loan.tenure / 12)} years {loan.tenure % 12} months)
              </span>
            </div>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--border)', paddingTop: '1.5rem' }}>
          <h3 className="mb-4" style={{ fontSize: '1.1rem' }}>Applicant Details</h3>
          
          <div className="grid grid-cols-2 gap-4 mb-4" style={{ fontSize: '0.95rem' }}>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Applicant Name</span>
              <span>{loan.user?.name || 'N/A'}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Applied On</span>
              <span>{formatDate(loan.createdAt || loan.date)}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Monthly Income</span>
              <span>{formatCurrency(loan.monthlyIncome)}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Employment Type</span>
              <span>{loan.employmentType || 'Salaried'}</span>
            </div>
          </div>

          <div style={{ background: 'rgba(17, 24, 39, 0.3)', padding: '1rem', borderRadius: '8px', marginTop: '1rem' }}>
            <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.5rem' }}>Purpose of Loan</span>
            <p style={{ lineHeight: '1.5' }}>{loan.purpose || 'Not specified'}</p>
          </div>
        </div>

        <div className="mt-6">
          <button onClick={() => navigate(-1)} className="btn" style={{ background: 'transparent', border: '1px solid var(--border)', color: 'var(--text-primary)' }}>
            ← Back
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoanDetails;
