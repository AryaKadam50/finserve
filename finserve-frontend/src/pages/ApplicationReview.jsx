import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getLoanById, getDocuments, getAuditEvents, analyzeUnderwriting, updateLoanStatus } from '../services/api';
import StatusBadge from '../components/StatusBadge';

const ApplicationReview = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [loan, setLoan] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [underwritingResult, setUnderwritingResult] = useState(null);
  const [auditEvents, setAuditEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [decision, setDecision] = useState(''); // 'APPROVED', 'REJECTED', 'REQUEST_INFO'
  const [overrideReason, setOverrideReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [loanRes, docsRes, auditRes] = await Promise.all([
        getLoanById(id),
        getDocuments(id),
        getAuditEvents(id)
      ]);
      setLoan(loanRes.data);
      setDocuments(docsRes.data.data);
      setAuditEvents(auditRes.data || []);
      
      // Attempt to fetch AI underwriting if it exists, or trigger analysis if needed.
      // Usually it's better to fetch if it exists. Wait, there's `analyzeUnderwriting(loanId)` which generates it or returns existing?
      // For now, let's just trigger analyze to get it, or assume it's there. 
      // In a real app, maybe there's a getUnderwritingResult endpoint, but the specs mention `analyzeUnderwriting`.
      try {
        const aiRes = await analyzeUnderwriting(id);
        setUnderwritingResult(aiRes.data.data);
      } catch (aiErr) {
        console.log('No underwriting result available or failed to fetch', aiErr);
      }

    } catch (err) {
      console.error('Failed to fetch application data', err);
      alert('Failed to load application details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount || 0);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('en-IN');
  };

  const handleDecisionSubmit = async () => {
    if (!decision) {
      alert('Please select a decision.');
      return;
    }
    
    let isOverride = false;
    if (underwritingResult && underwritingResult.recommendation) {
      if ((decision === 'APPROVED' && underwritingResult.recommendation !== 'APPROVE') ||
          (decision === 'REJECTED' && underwritingResult.recommendation !== 'REJECT')) {
        isOverride = true;
      }
    }

    if (isOverride && !overrideReason.trim()) {
      alert('You are overriding the AI recommendation. Please provide a mandatory override reason.');
      return;
    }

    try {
      setSubmitting(true);
      await updateLoanStatus(id, { 
        status: decision, 
        overrideReason: overrideReason.trim() || null 
      });
      alert('Decision recorded successfully!');
      fetchData(); // Reload
      setDecision('');
      setOverrideReason('');
    } catch (err) {
      console.error(err);
      alert('Failed to update decision.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="page-container flex justify-center items-center" style={{ height: '50vh' }}>Loading...</div>;
  }

  if (!loan) {
    return <div className="page-container">Application not found.</div>;
  }

  return (
    <div className="page-container">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2>Application #{loan.id} Review</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Applicant: {loan.user?.name || 'N/A'} | Applied on: {formatDate(loan.createdAt)}</p>
        </div>
        <button className="btn" style={{ background: 'var(--bg-secondary)' }} onClick={() => navigate('/admin')}>
          Back to Queue
        </button>
      </div>

      <div className="grid" style={{ gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        
        {/* LEFT COLUMN */}
        <div className="flex flex-col gap-6">
          
          {/* Applicant & Loan Details */}
          <div className="glass-card">
            <div className="flex justify-between items-center mb-4">
              <h3 style={{ margin: 0 }}>Borrower & Loan Details</h3>
              <StatusBadge status={loan.status} />
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Applicant Name</p>
                <p style={{ margin: 0, fontWeight: 'bold' }}>{loan.user?.name || 'N/A'}</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Employment / Tenure</p>
                <p style={{ margin: 0, fontWeight: 'bold' }}>{loan.employmentStatus?.replace('_', ' ')} / {loan.yearsEmployed} yrs</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Monthly Income</p>
                <p style={{ margin: 0, fontWeight: 'bold', color: 'var(--success)' }}>{formatCurrency(loan.monthlyIncome)}</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Monthly Expenses / EMI</p>
                <p style={{ margin: 0, fontWeight: 'bold', color: 'var(--danger)' }}>{formatCurrency(loan.monthlyExpenses)} / {formatCurrency(loan.existingMonthlyEmi)}</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Credit Score</p>
                <p style={{ margin: 0, fontWeight: 'bold' }}>{loan.creditScore || 'N/A'}</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Existing Loans</p>
                <p style={{ margin: 0, fontWeight: 'bold' }}>{loan.existingLoansCount || 0}</p>
              </div>
            </div>

            <hr style={{ borderColor: 'var(--border)', margin: '1.5rem 0' }} />
            
            <div className="grid grid-cols-3 gap-4">
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Loan Amount</p>
                <p style={{ margin: 0, fontWeight: 'bold', fontSize: '1.25rem' }}>{formatCurrency(loan.amount)}</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Tenure</p>
                <p style={{ margin: 0, fontWeight: 'bold', fontSize: '1.25rem' }}>{loan.tenure} months</p>
              </div>
              <div>
                <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Purpose</p>
                <p style={{ margin: 0, fontWeight: 'bold', fontSize: '1rem' }}>{loan.purpose?.replace('_', ' ')}</p>
              </div>
            </div>
          </div>

          {/* AI Underwriting Results */}
          {underwritingResult && (
            <div className="glass-card" style={{ borderLeft: `4px solid ${underwritingResult.recommendation === 'APPROVE' ? 'var(--success)' : underwritingResult.recommendation === 'REJECT' ? 'var(--danger)' : 'var(--warning)'}`}}>
              <h3 style={{ margin: '0 0 1rem 0' }}>🤖 AI Underwriting Analysis</h3>
              
              <div className="grid grid-cols-3 gap-4 mb-4">
                <div style={{ background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '8px' }}>
                  <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Recommendation</p>
                  <p style={{ margin: 0, fontWeight: 'bold', color: underwritingResult.recommendation === 'APPROVE' ? 'var(--success)' : underwritingResult.recommendation === 'REJECT' ? 'var(--danger)' : 'var(--warning)' }}>
                    {underwritingResult.recommendation}
                  </p>
                </div>
                <div style={{ background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '8px' }}>
                  <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Risk Level</p>
                  <p style={{ margin: 0, fontWeight: 'bold' }}>{underwritingResult.riskLevel}</p>
                </div>
                <div style={{ background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '8px' }}>
                  <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Confidence Score</p>
                  <p style={{ margin: 0, fontWeight: 'bold' }}>{(underwritingResult.confidenceScore * 100).toFixed(0)}%</p>
                </div>
              </div>

              {underwritingResult.reasons && underwritingResult.reasons.length > 0 && (
                <div className="mb-4">
                  <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem' }}>Key Factors</h4>
                  <ul style={{ margin: 0, paddingLeft: '1.25rem', color: 'var(--text-secondary)' }}>
                    {underwritingResult.reasons.map((r, i) => <li key={i}>{r}</li>)}
                  </ul>
                </div>
              )}

              {underwritingResult.verificationIssues && underwritingResult.verificationIssues.length > 0 && (
                <div className="mb-4">
                  <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem', color: 'var(--warning)' }}>Verification Issues</h4>
                  <ul style={{ margin: 0, paddingLeft: '1.25rem', color: 'var(--danger)' }}>
                    {underwritingResult.verificationIssues.map((v, i) => <li key={i}>{v}</li>)}
                  </ul>
                </div>
              )}

              {underwritingResult.policyReferences && underwritingResult.policyReferences.length > 0 && (
                <div>
                  <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem' }}>Policy References</h4>
                  <div className="flex gap-2 flex-wrap">
                    {underwritingResult.policyReferences.map((p, i) => (
                      <span key={i} style={{ background: 'rgba(255,255,255,0.1)', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        {p}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Documents Section */}
          <div className="glass-card">
            <h3 style={{ margin: '0 0 1rem 0' }}>📄 Document Verification</h3>
            
            {documents.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No documents uploaded.</p>
            ) : (
              documents.map(doc => (
                <div key={doc.id} style={{ marginBottom: '1.5rem', borderBottom: '1px solid var(--border)', paddingBottom: '1.5rem' }}>
                  <div className="flex justify-between items-center mb-3">
                    <div>
                      <span style={{ fontWeight: 'bold', fontSize: '1rem' }}>
                        {doc.documentType?.replace('_', ' ')}
                      </span>
                      <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginLeft: '0.75rem' }}>
                        {doc.originalFileName}
                      </span>
                    </div>
                    <span style={{
                      padding: '0.25rem 0.75rem',
                      borderRadius: '9999px',
                      fontSize: '0.75rem',
                      fontWeight: 'bold',
                      background: doc.verificationStatus === 'VERIFIED' ? 'rgba(16,185,129,0.15)' : doc.verificationStatus === 'FLAGGED' ? 'rgba(239,68,68,0.15)' : 'rgba(245,158,11,0.15)',
                      color: doc.verificationStatus === 'VERIFIED' ? 'var(--success)' : doc.verificationStatus === 'FLAGGED' ? 'var(--danger)' : 'var(--warning)',
                    }}>
                      {doc.verificationStatus}
                    </span>
                  </div>

                  {doc.verificationResults && doc.verificationResults.length > 0 && (
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                      <thead>
                        <tr style={{ borderBottom: '1px solid var(--border)' }}>
                          <th style={{ textAlign: 'left', padding: '0.5rem', color: 'var(--text-secondary)' }}>Field</th>
                          <th style={{ textAlign: 'left', padding: '0.5rem', color: 'var(--text-secondary)' }}>Declared</th>
                          <th style={{ textAlign: 'left', padding: '0.5rem', color: 'var(--text-secondary)' }}>Extracted</th>
                          <th style={{ textAlign: 'center', padding: '0.5rem', color: 'var(--text-secondary)' }}>Match</th>
                        </tr>
                      </thead>
                      <tbody>
                        {doc.verificationResults.map(vr => (
                          <tr key={vr.id} style={{ borderBottom: '1px solid var(--border)', background: vr.matchStatus === 'MISMATCH' ? 'rgba(239,68,68,0.07)' : 'transparent' }}>
                            <td style={{ padding: '0.5rem' }}>{vr.field}</td>
                            <td style={{ padding: '0.5rem' }}>{vr.declaredValue}</td>
                            <td style={{ padding: '0.5rem', color: vr.matchStatus === 'MISMATCH' ? 'var(--danger)' : 'var(--text-primary)' }}>{vr.extractedValue}</td>
                            <td style={{ padding: '0.5rem', textAlign: 'center', color: vr.matchStatus === 'MATCH' ? 'var(--success)' : vr.matchStatus === 'MISMATCH' ? 'var(--danger)' : 'var(--text-secondary)' }}>
                              {vr.matchStatus}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* RIGHT COLUMN */}
        <div className="flex flex-col gap-6">
          
          {/* Decision Panel */}
          <div className="glass-card" style={{ position: 'sticky', top: '2rem' }}>
            <h3 style={{ margin: '0 0 1rem 0' }}>Admin Decision</h3>
            
            <div className="flex flex-col gap-3 mb-4">
              <button 
                className={`btn ${decision === 'APPROVED' ? 'btn-success' : ''}`}
                style={{ background: decision === 'APPROVED' ? 'var(--success)' : 'var(--bg-secondary)' }}
                onClick={() => setDecision('APPROVED')}
              >
                Approve Application
              </button>
              <button 
                className={`btn ${decision === 'REJECTED' ? 'btn-danger' : ''}`}
                style={{ background: decision === 'REJECTED' ? 'var(--danger)' : 'var(--bg-secondary)' }}
                onClick={() => setDecision('REJECTED')}
              >
                Reject Application
              </button>
              <button 
                className={`btn ${decision === 'REQUEST_INFO' ? 'btn-primary' : ''}`}
                style={{ background: decision === 'REQUEST_INFO' ? 'var(--primary)' : 'var(--bg-secondary)' }}
                onClick={() => setDecision('REQUEST_INFO')}
              >
                Request Info (Pending Human Review)
              </button>
            </div>

            {underwritingResult && underwritingResult.recommendation && decision && (
              ((decision === 'APPROVED' && underwritingResult.recommendation !== 'APPROVE') ||
               (decision === 'REJECTED' && underwritingResult.recommendation !== 'REJECT')) && (
                 <div className="mb-4">
                   <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--warning)', fontWeight: 'bold' }}>
                     Override Reason (Mandatory)
                   </label>
                   <textarea
                     className="input-field"
                     rows="3"
                     placeholder="Explain why you are overriding the AI recommendation..."
                     value={overrideReason}
                     onChange={(e) => setOverrideReason(e.target.value)}
                   />
                 </div>
               )
            )}

            <button 
              className="btn btn-primary" 
              style={{ width: '100%', padding: '0.75rem', fontWeight: 'bold' }}
              onClick={handleDecisionSubmit}
              disabled={submitting || !decision}
            >
              {submitting ? 'Submitting...' : 'Submit Decision'}
            </button>
          </div>

          {/* Audit Timeline */}
          <div className="glass-card">
            <h3 style={{ margin: '0 0 1rem 0' }}>Audit Timeline</h3>
            {auditEvents.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No audit events found.</p>
            ) : (
              <div style={{ position: 'relative', borderLeft: '2px solid var(--border)', marginLeft: '1rem', paddingLeft: '1rem' }}>
                {auditEvents.map((evt, idx) => (
                  <div key={idx} style={{ marginBottom: '1.5rem', position: 'relative' }}>
                    <div style={{
                      position: 'absolute',
                      left: '-1.35rem',
                      top: '0',
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      background: 'var(--primary)',
                      border: '2px solid var(--bg-primary)'
                    }}></div>
                    <p style={{ margin: '0 0 0.25rem 0', fontWeight: 'bold', fontSize: '0.9rem' }}>{evt.action || evt.eventType}</p>
                    <p style={{ margin: '0 0 0.25rem 0', color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                      {formatDate(evt.createdAt || evt.timestamp)}
                    </p>
                    {evt.details && <p style={{ margin: 0, fontSize: '0.85rem' }}>{evt.details}</p>}
                    {evt.actorRole && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--primary)', background: 'rgba(168, 85, 247, 0.1)', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                        {evt.actorRole}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ApplicationReview;
