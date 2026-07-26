import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="page-container text-center">
      <div style={{ padding: '4rem 1rem' }}>
        <h1 style={{ fontSize: '3.5rem', marginBottom: '1rem', fontWeight: 800 }} className="text-gradient">
          Your Financial Journey Starts Here
        </h1>
        <p style={{ fontSize: '1.25rem', color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto 2.5rem' }}>
          Experience the fastest, most transparent digital loan application process. Apply in minutes, get approved in hours.
        </p>
        
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', marginBottom: '4rem' }}>
          <Link to="/register" className="btn btn-primary" style={{ textDecoration: 'none', fontSize: '1.1rem' }}>
            Get Started
          </Link>
          <Link to="/login" className="btn" style={{ background: 'rgba(17, 24, 39, 0.8)', border: '1px solid var(--border)', color: 'white', textDecoration: 'none', fontSize: '1.1rem' }}>
            Login to Dashboard
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-4" style={{ textAlign: 'left' }}>
          <div className="glass-card">
            <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', color: 'var(--accent)' }}>Quick Application</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Fill out our simple digital form in less than 5 minutes. No paperwork required.</p>
          </div>
          <div className="glass-card">
            <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', color: 'var(--success)' }}>Fast Approval</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Our automated system checks eligibility instantly to speed up your approval process.</p>
          </div>
          <div className="glass-card">
            <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', color: 'var(--warning)' }}>Track Status</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Log in anytime to see real-time updates on where your application stands.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
