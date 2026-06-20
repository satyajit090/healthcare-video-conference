import React from 'react';
import { Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './lib/auth.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import CallRoom from './pages/CallRoom.jsx';
import Schedule from './pages/Schedule.jsx';
import History from './pages/History.jsx';
import SupportQueue from './pages/SupportQueue.jsx';
import AdminProviders from './pages/AdminProviders.jsx';
import NotificationBell from './components/NotificationBell.jsx';

function Protected({ children, roles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function Shell({ children }) {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  if (!user) return children;
  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">🩺 HealthConnect</Link>
        <nav className="nav">
          <Link to="/">Dashboard</Link>
          {user.role === 'PATIENT' && <Link to="/schedule">Schedule</Link>}
          {user.role === 'SUPPORT' && <Link to="/queue">Queue</Link>}
          <Link to="/history">History</Link>
          {user.role === 'ADMIN' && <Link to="/admin/providers">Providers</Link>}
        </nav>
        <div className="topbar-right">
          <NotificationBell />
          <span className="who">{user.fullName} · <b>{user.role}</b></span>
          <button className="btn ghost" onClick={() => { logout(); nav('/login'); }}>Logout</button>
        </div>
      </header>
      <main className="content">{children}</main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Shell>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/" element={<Protected><Dashboard /></Protected>} />
          <Route path="/room/:roomId" element={<Protected><CallRoom /></Protected>} />
          <Route path="/schedule" element={<Protected roles={['PATIENT']}><Schedule /></Protected>} />
          <Route path="/history" element={<Protected><History /></Protected>} />
          <Route path="/queue" element={<Protected roles={['SUPPORT']}><SupportQueue /></Protected>} />
          <Route path="/admin/providers" element={<Protected roles={['ADMIN']}><AdminProviders /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Shell>
    </AuthProvider>
  );
}
