import React, { useState, useEffect } from 'react';
import { getAllLoans, updateLoanStatus } from '../services/api';
import StatusBadge from '../components/StatusBadge';

const AdminDashboard = () => {
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchLoans = async () => {
    try {
      const res = await getAllLoans();
      setLoans(res.data || []);
    } catch (err) {
      console.error('Failed to fetch loans', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLoans();
  }, []);

  const handleStatusChange = async (id, status) => {
    if (!window.confirm(`Are you sure you want to mark this loan as ${status}?`)) return;
    try {
      await updateLoanStatus(id, status);
      fetchLoans();
    } catch (err) {
      alert('Failed to update status');
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  };

  const stats = {
    total: loans.length,
    pending: loans.filter(l => l.status === 'PENDING' || l.status === 'UNDER_REVIEW').length,
    approved: loans.filter(l => l.status === 'APPROVED').length,
    rejected: loans.filter(l => l.status === 'REJECTED').length,
  };

  return (
    <div className="page-container">
      <div className="flex justify-between items-center mb-6">
        <h2>Admin Dashboard</h2>
        <span style={{ color: 'var(--text-secondary)' }}>Total Applications: {stats.total}</span>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="glass-card text-center" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', fontWeight: 'bold' }}>{stats.total}</div>
          <div style={{ color: 'var(--text-secondary)' }}>Total</div>
        </div>
        <div className="glass-card text-center" style={{ padding: '1.5rem', borderBottom: '4px solid var(--warning)' }}>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--warning)' }}>{stats.pending}</div>
          <div style={{ color: 'var(--text-secondary)' }}>Pending</div>
        </div>
        <div className="glass-card text-center" style={{ padding: '1.5rem', borderBottom: '4px solid var(--success)' }}>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--success)' }}>{stats.approved}</div>
          <div style={{ color: 'var(--text-secondary)' }}>Approved</div>
        </div>
        <div className="glass-card text-center" style={{ padding: '1.5rem', borderBottom: '4px solid var(--danger)' }}>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--danger)' }}>{stats.rejected}</div>
          <div style={{ color: 'var(--text-secondary)' }}>Rejected</div>
        </div>
      </div>

      <div className="table-container table-desktop glass-card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Applicant</th>
                <th>Amount</th>
                <th>Tenure</th>
                <th>Income</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loans.map(loan => (
                <tr key={loan.id}>
                  <td>#{loan.id}</td>
                  <td>{loan.user?.name || 'Unknown'}</td>
                  <td>{formatCurrency(loan.amount)}</td>
                  <td>{loan.tenure} mo</td>
                  <td>{formatCurrency(loan.monthlyIncome)}</td>
                  <td><StatusBadge status={loan.status} /></td>
                  <td>
                    {(loan.status === 'PENDING' || loan.status === 'UNDER_REVIEW') && (
                      <div className="flex gap-2">
                        <button onClick={() => handleStatusChange(loan.id, 'APPROVED')} className="btn btn-success" style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}>Approve</button>
                        <button onClick={() => handleStatusChange(loan.id, 'REJECTED')} className="btn btn-danger" style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}>Reject</button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="mobile-card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {loans.map(loan => (
          <div key={loan.id} className="glass-card">
            <div className="flex justify-between mb-2">
              <span style={{ fontWeight: 'bold' }}>{loan.user?.name || 'Unknown'} (#{loan.id})</span>
              <StatusBadge status={loan.status} />
            </div>
            <div className="mb-2">
              Amount: {formatCurrency(loan.amount)} | {loan.tenure} mo
            </div>
            {(loan.status === 'PENDING' || loan.status === 'UNDER_REVIEW') && (
              <div className="flex gap-2 mt-4">
                <button onClick={() => handleStatusChange(loan.id, 'APPROVED')} className="btn btn-success" style={{ flex: 1 }}>Approve</button>
                <button onClick={() => handleStatusChange(loan.id, 'REJECTED')} className="btn btn-danger" style={{ flex: 1 }}>Reject</button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default AdminDashboard;
