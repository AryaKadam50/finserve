import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

export const register = (data) => api.post('/users/register', data);
export const login = (data) => api.post('/users/login', data);
export const applyForLoan = (data) => api.post('/loans', data);
export const getAllLoans = () => api.get('/loans');
export const getLoanById = (id) => api.get(`/loans/${id}`);
export const getUserLoans = (userId) => api.get(`/users/${userId}/loans`);
export const updateLoanStatus = (id, status) => api.put(`/loans/${id}/status`, { status });
export const deleteLoan = (id) => api.delete(`/loans/${id}`);
export const checkEligibility = (data) => api.post('/loans/check-eligibility', data);

export default api;
