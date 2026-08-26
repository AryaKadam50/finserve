import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

api.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('finserve_user'));
  if (user && user.id) {
    if (config.headers && typeof config.headers.set === 'function') {
      config.headers.set('X-User-Id', String(user.id));
      config.headers.set('X-User-Role', String(user.role));
    } else {
      config.headers['X-User-Id'] = String(user.id);
      config.headers['X-User-Role'] = String(user.role);
    }
  }
  return config;
});

export const register = (data) => api.post('/users/register', data);
export const login = (data) => api.post('/users/login', data);
export const applyForLoan = (data) => api.post('/loans', data);
export const getAllLoans = () => api.get('/loans');
export const getLoanById = (id) => api.get(`/loans/${id}`);
export const getUserLoans = (userId) => api.get(`/users/${userId}/loans`);
export const updateLoanStatus = (id, adminDecisionRequest) => api.put(`/loans/${id}/status`, adminDecisionRequest);
export const deleteLoan = (id) => api.delete(`/loans/${id}`);
export const getAuditEvents = (id) => api.get(`/loans/${id}/audit-events`);
export const checkEligibility = (data) => api.post('/loans/check-eligibility', data);

// Documents
export const uploadDocument = (loanId, type, file) => {
  const formData = new FormData();
  formData.append('type', type);
  formData.append('file', file);
  return api.post(`/loans/${loanId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const getDocuments = (loanId) => api.get(`/loans/${loanId}/documents`);

// Underwriting AI
export const analyzeUnderwriting = (loanId) => api.post(`/underwriting/${loanId}/analyze`);

export default api;
