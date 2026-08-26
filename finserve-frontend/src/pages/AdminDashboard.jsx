import React, { useState, useEffect } from 'react';
import { getAllLoans } from '../services/api';
import StatusBadge from '../components/StatusBadge';
import { useNavigate } from 'react-router-dom';

const AdminDashboard = () => {
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const navigate = useNavigate();
  
  const fetchLoans = async () => {
    try {
      const res = await getAllLoans();
      // Handle both direct array or wrapped ApiResponse
      setLoans(Array.isArray(res.data) ? res.data : (res.data?.data || []));
    } catch (err) {
      console.error('Failed to fetch loans', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLoans();
  }, []);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount || 0);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN');
  };

  const tabs = ['ALL', 'PENDING', 'PENDING_HUMAN_REVIEW', 'AI_RECOMMENDED', 'APPROVED', 'REJECTED'];

  const filteredLoans = loans.filter(loan => {
    if (filter === 'ALL') return true;
    return loan.status === filter;
  });

  return (
    <div className="page-container">
      <div className="flex justify-between items-center mb-6">
        <h2>Underwriting Queue</h2>
        <span style={{ color: 'var(--text-secondary)' }}>Total Applications: {loans.length}</span>
      </div>

      <div className="flex gap-2 mb-6" style={{ overflowX: 'auto', paddingBottom: '0.5rem' }}>
        {tabs.map(tab => (
          <button
            key={tab}
            onClick={() => setFilter(tab)}
            className={`btn ${filter === tab ? 'btn-primary' : ''}`}
            style={{ 
              background: filter === tab ? 'var(--primary)' : 'var(--bg-secondary)',
              color: filter === tab ? 'white' : 'var(--text-primary)',
              whiteSpace: 'nowrap'
            }}
          >
            {tab.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="table-container table-desktop glass-card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>
        ) : (
          <table className="table" style={{ width: '100%' }}>
            <thead>
              <tr>
                <th>ID</th>
                <th>Applicant</th>
                <th>Amount</th>
                <th>Income</th>
                <th>Risk Level</th>
                <th>Status</th>
                <th>Created Date</th>
              </tr>
            </thead>
            <tbody>
              {filteredLoans.map(loan => (
                <tr 
                  key={loan.id} 
                  onClick={() => navigate(`/admin/loans/${loan.id}`)}
                  style={{ cursor: 'pointer' }}
                  className="hover:bg-gray-800 transition-colors"
                >
                  <td>#{loan.id}</td>
                  <td>{loan.user?.name || 'Unknown'}</td>
                  <td>{formatCurrency(loan.amount)}</td>
                  <td>{formatCurrency(loan.monthlyIncome)}</td>
                  <td>{loan.riskLevel || 'N/A'}</td>
                  <td><StatusBadge status={loan.status} /></td>
                  <td>{formatDate(loan.createdAt)}</td>
                </tr>
              ))}
              {filteredLoans.length === 0 && (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                    No applications found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;
