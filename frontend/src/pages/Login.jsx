import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../lib/api.js';
import { useAuth } from '../lib/auth.jsx';

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('patient@health.dev');
  const [password, setPassword] = useState('patient123');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    setErr(''); setBusy(true);
    try {
      const auth = await api.post('/auth/login', { email, password });
      login(auth);
      nav('/');
    } catch (e) { setErr(e.message); } finally { setBusy(false); }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>🩺 HealthConnect</h1>
        <p className="sub">Secure video support for patients & yoga learners</p>
        {err && <div className="err">{err}</div>}
        <label>Email</label>
        <input value={email} onChange={e => setEmail(e.target.value)} />
        <label>Password</label>
        <input type="password" value={password} onChange={e => setPassword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && submit()} />
        <div style={{ height: 18 }} />
        <button className="btn block" onClick={submit} disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="muted" style={{ marginTop: 16 }}>
          No account? <Link to="/register">Create one</Link>
        </p>
        <div className="bubble" style={{ marginTop: 12 }}>
          <b>Demo logins</b><br/>
          Patient: patient@health.dev / patient123<br/>
          Support: dr.smith@health.dev / support123<br/>
          Admin: admin@health.dev / admin123
        </div>
      </div>
    </div>
  );
}
