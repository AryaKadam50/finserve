import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav style={{ position: 'sticky', top: 0, zIndex: 1000, background: 'rgba(10, 14, 26, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid var(--border)' }}>
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
        <Link to="/" style={{ textDecoration: 'none', fontSize: '1.5rem', fontWeight: 'bold' }} className="text-gradient">
          FinServe
        </Link>

        <button 
          onClick={() => setIsOpen(!isOpen)} 
          style={{ display: 'none' }} // In a full implementation, we'd use media queries to show this on mobile
          className="mobile-menu-btn"
        >
          Menu
        </button>

        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }} className={`nav-links ${isOpen ? 'open' : ''}`}>
          {isAuthenticated ? (
            <>
              <span style={{ color: 'var(--text-secondary)' }}>Hi, {user.name}</span>
              {isAdmin ? (
                <Link to="/admin" style={{ color: isActive('/admin') ? 'var(--accent)' : 'var(--text-primary)', textDecoration: 'none' }}>Dashboard</Link>
              ) : (
                <>
                  <Link to="/apply" style={{ color: isActive('/apply') ? 'var(--accent)' : 'var(--text-primary)', textDecoration: 'none' }}>Apply</Link>
                  <Link to="/my-applications" style={{ color: isActive('/my-applications') ? 'var(--accent)' : 'var(--text-primary)', textDecoration: 'none' }}>My Applications</Link>
                </>
              )}
              <button onClick={handleLogout} className="btn" style={{ padding: '0.5rem 1rem', background: 'transparent', border: '1px solid var(--border)', color: 'var(--text-primary)' }}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" style={{ color: isActive('/login') ? 'var(--accent)' : 'var(--text-primary)', textDecoration: 'none' }}>Login</Link>
              <Link to="/register" className="btn btn-primary" style={{ padding: '0.5rem 1rem', textDecoration: 'none' }}>Register</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
