import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getUserLoans } from '../services/api';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/StatusBadge';

const MyApplications = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message] = useState(location.state?.message || '');

  useEffect(() => {
    const fetchLoans = async () => {
      try {
        const res = await getUserLoans(user.id);
        setLoans(res.data || []);
      } catch (err) {
        console.error('Failed to fetch loans', err);
      } finally {
        setLoading(false);
      }
    };
    if (user?.id) fetchLoans();
  }, [user.id]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  return (
    <div className="page-container">
      <h2 className="mb-6">My Loan Applications</h2>
      
      {message && <div className="mb-6" style={{ color: 'var(--success)', background: 'rgba(16, 185, 129, 0.1)', padding: '1rem', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)' }}>{message}</div>}

      {loading ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading...</div>
      ) : loans.length === 0 ? (
        <div className="glass-card text-center" style={{ padding: '4rem 2rem' }}>
          <h3 className="mb-4 text-secondary">No applications yet</h3>
          <p className="mb-6">Apply for your first loan!</p>
          <button onClick={() => navigate('/apply')} className="btn btn-primary">Apply Now</button>
        </div>
      ) : (
        <>
          {/* Desktop Table */}
          <div className="table-container table-desktop glass-card" style={{ padding: 0, overflow: 'hidden' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Amount</th>
                  <th>Tenure</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {loans.map(loan => (
                  <tr key={loan.id} onClick={() => navigate(`/loans/${loan.id}`)} style={{ cursor: 'pointer' }}>
                    <td>#{loan.id}</td>
                    <td>{formatCurrency(loan.amount)}</td>
                    <td>{loan.tenure} months</td>
                    <td><StatusBadge status={loan.status} /></td>
                    <td>{formatDate(loan.createdAt || loan.date)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="mobile-card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {loans.map(loan => (
              <div key={loan.id} className="glass-card" onClick={() => navigate(`/loans/${loan.id}`)} style={{ cursor: 'pointer' }}>
                <div className="flex justify-between mb-2">
                  <span style={{ color: 'var(--text-secondary)' }}>#{loan.id}</span>
                  <StatusBadge status={loan.status} />
                </div>
                <div style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
                  {formatCurrency(loan.amount)}
                </div>
                <div className="flex justify-between" style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  <span>{loan.tenure} months</span>
                  <span>{formatDate(loan.createdAt || loan.date)}</span>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default MyApplications;
