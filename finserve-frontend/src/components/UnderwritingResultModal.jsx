import React from 'react';
import './UnderwritingResultModal.css';

const UnderwritingResultModal = ({ result, onClose }) => {
  if (!result) return null;

  const getRecommendationColor = (rec) => {
    switch (rec) {
      case 'APPROVE': return 'var(--success)';
      case 'REVIEW': return 'var(--warning)';
      case 'REJECT': return 'var(--danger)';
      default: return 'var(--text-secondary)';
    }
  };

  const getRiskColor = (risk) => {
    switch (risk) {
      case 'LOW': return 'var(--success)';
      case 'MEDIUM': return 'var(--warning)';
      case 'HIGH': return 'var(--danger)';
      default: return 'var(--text-secondary)';
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content ai-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>🤖 AI Underwriting Analysis</h2>
          <button className="btn-close" onClick={onClose}>×</button>
        </div>
        
        <div className="modal-body">
          <div className="ai-summary-cards">
            <div className="ai-card">
              <span className="ai-label">Recommendation</span>
              <h3 style={{ color: getRecommendationColor(result.recommendation) }}>
                {result.recommendation}
              </h3>
            </div>
            <div className="ai-card">
              <span className="ai-label">Risk Level</span>
              <h3 style={{ color: getRiskColor(result.riskLevel) }}>
                {result.riskLevel}
              </h3>
            </div>
            <div className="ai-card">
              <span className="ai-label">Confidence</span>
              <h3>{(result.confidence * 100).toFixed(1)}%</h3>
            </div>
          </div>

          {result.requiresHumanReview && (
            <div className="ai-alert warning">
              <strong>⚠️ Human Review Required</strong>
              <p>The AI confidence is below threshold or mismatched documents were detected.</p>
            </div>
          )}

          <div className="ai-section">
            <h4>Key Reasons</h4>
            <ul className="ai-list">
              {result.reasons && result.reasons.length > 0 ? (
                result.reasons.map((r, i) => <li key={i}>{r}</li>)
              ) : (
                <li>No specific reasons provided.</li>
              )}
            </ul>
          </div>

          {result.verificationIssues && result.verificationIssues.length > 0 && (
            <div className="ai-section ai-issues">
              <h4>Verification Issues</h4>
              <ul className="ai-list danger">
                {result.verificationIssues.map((issue, i) => <li key={i}>{issue}</li>)}
              </ul>
            </div>
          )}

          {result.policyReferences && result.policyReferences.length > 0 && (
            <div className="ai-section ai-policies">
              <h4>📖 Grounded Policy Evidence</h4>
              <ul className="ai-list info">
                {result.policyReferences.map((ref, i) => (
                  <li key={i} style={{ borderLeftColor: 'var(--accent-secondary)' }}>
                    <strong>{ref.document}</strong> (Section: {ref.section})
                    <span style={{ float: 'right', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                      Relevance: {ref.relevance ? (ref.relevance * 100).toFixed(0) + '%' : 'N/A'}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="ai-footer">
            <small>Model: {result.aiModel} | Generated: {new Date(result.createdAt).toLocaleString()}</small>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UnderwritingResultModal;
