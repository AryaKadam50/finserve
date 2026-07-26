import React from 'react';

const StatusBadge = ({ status }) => {
  const getBadgeClass = (status) => {
    switch (status?.toUpperCase()) {
      case 'APPROVED': return 'badge-approved';
      case 'REJECTED': return 'badge-rejected';
      case 'UNDER_REVIEW':
      case 'UNDER REVIEW': return 'badge-under-review';
      case 'PENDING':
      default: return 'badge-pending';
    }
  };

  const displayText = status?.toUpperCase().replace('_', ' ') || 'PENDING';

  return (
    <span className={`badge ${getBadgeClass(status)}`}>
      {displayText}
    </span>
  );
};

export default StatusBadge;
