import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getLoanById, getDocuments } from '../services/api';
import StatusBadge from '../components/StatusBadge';

const LoanDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loan, setLoan] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchLoanAndDocs = async () => {
      try {
        const loanRes = await getLoanById(id);
        setLoan(loanRes.data?.data || loanRes.data); // Handle wrapper if any
        
        try {
          const docsRes = await getDocuments(id);
          setDocuments(docsRes.data?.data || []);
        } catch (docErr) {
          console.error("Could not fetch docs", docErr);
        }
      } catch (err) {
        setError('Failed to fetch loan details.');
      } finally {
        setLoading(false);
      }
    };
    fetchLoanAndDocs();
  }, [id]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount || 0);
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
      <div className="glass-card" style={{ width: '100%', maxWidth: '800px' }}>
        <div className="flex justify-between items-center mb-6">
          <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Loan Application #{id}</h2>
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

        <div style={{ borderTop: '1px solid var(--border)', paddingTop: '1.5rem', marginBottom: '1.5rem' }}>
          <h3 className="mb-4" style={{ fontSize: '1.1rem' }}>Applicant & Financial Details</h3>
          
          <div className="grid grid-cols-3 gap-4 mb-4" style={{ fontSize: '0.95rem' }}>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Applicant Name</span>
              <span>{loan.user?.name || 'N/A'}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Applied On</span>
              <span>{formatDate(loan.createdAt || loan.date)}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Credit Score</span>
              <span>{loan.creditScore || 'N/A'}</span>
            </div>
            
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Monthly Income</span>
              <span>{formatCurrency(loan.monthlyIncome)}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Monthly Expenses</span>
              <span>{formatCurrency(loan.monthlyExpenses)}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Existing EMI</span>
              <span>{formatCurrency(loan.existingMonthlyEmi)}</span>
            </div>

            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Employment Type</span>
              <span>{loan.employmentType || 'Salaried'}</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Years Employed</span>
              <span>{loan.yearsOfEmployment || '0'} Years</span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Existing Loans</span>
              <span>{loan.existingLoanCount || '0'}</span>
            </div>
          </div>

          <div style={{ background: 'rgba(17, 24, 39, 0.3)', padding: '1rem', borderRadius: '8px', marginTop: '1rem' }}>
            <span style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: '0.5rem' }}>Purpose of Loan</span>
            <p style={{ lineHeight: '1.5', margin: 0 }}>{loan.purpose || 'Not specified'}</p>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--border)', paddingTop: '1.5rem' }}>
          <h3 className="mb-4" style={{ fontSize: '1.1rem' }}>Uploaded Documents</h3>
          {documents.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>No documents uploaded.</p>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              {documents.map(doc => {
                const statusConfig = {
                  VERIFIED: { icon: '✓', label: 'Verified', color: 'var(--success)', bg: 'rgba(16,185,129,0.1)', border: 'rgba(16,185,129,0.3)' },
                  FLAGGED:  { icon: '⚠', label: 'Under Review', color: 'var(--warning)', bg: 'rgba(245,158,11,0.1)', border: 'rgba(245,158,11,0.3)' },
                  FAILED:   { icon: '✗', label: 'Processing Failed', color: 'var(--danger)', bg: 'rgba(239,68,68,0.1)', border: 'rgba(239,68,68,0.3)' },
                  PENDING:  { icon: '⏳', label: 'Processing…', color: 'var(--text-secondary)', bg: 'rgba(107,114,128,0.1)', border: 'rgba(107,114,128,0.2)' },
                };
                const cfg = statusConfig[doc.verificationStatus] || statusConfig.PENDING;
                return (
                  <div key={doc.id} style={{ background: 'rgba(17,24,39,0.5)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
                    <div style={{ fontWeight: 'bold', marginBottom: '0.35rem' }}>{doc.documentType?.replace('_', ' ')}</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: '0.75rem', wordBreak: 'break-all' }}>{doc.originalFileName}</div>
                    <div style={{
                      display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                      padding: '0.25rem 0.6rem', borderRadius: '9999px', fontSize: '0.78rem', fontWeight: 600,
                      background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}`,
                    }}>
                      {cfg.icon} {cfg.label}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>


        <div className="mt-8 text-center">
          <button onClick={() => navigate(-1)} className="btn" style={{ background: 'transparent', border: '1px solid var(--border)', color: 'var(--text-primary)', padding: '0.5rem 2rem' }}>
            ← Back to Applications
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoanDetails;
