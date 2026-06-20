import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../lib/api.js';
import { useAuth } from '../lib/auth.jsx';

const LANGS = [['en','English'],['hi','Hindi'],['or','Odia'],['es','Spanish'],['fr','French']];

export default function Register() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ fullName: '', email: '', password: '', role: 'PATIENT',
    specialty: '', language: 'en' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const up = (k, v) => setF(p => ({ ...p, [k]: v }));

  async function submit() {
    setErr(''); setBusy(true);
    try {
      const auth = await api.post('/auth/register', f);
      login(auth);
      nav('/');
    } catch (e) { setErr(e.message); } finally { setBusy(false); }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Create account</h1>
        <p className="sub">Join as a patient/learner or a support specialist</p>
        {err && <div className="err">{err}</div>}
        <label>Full name</label>
        <input value={f.fullName} onChange={e => up('fullName', e.target.value)} />
        <label>Email</label>
        <input value={f.email} onChange={e => up('email', e.target.value)} />
        <label>Password</label>
        <input type="password" value={f.password} onChange={e => up('password', e.target.value)} />
        <div className="grid cols-2">
          <div>
            <label>I am a…</label>
            <select value={f.role} onChange={e => up('role', e.target.value)}>
              <option value="PATIENT">Patient / Learner</option>
              <option value="SUPPORT">Support / Instructor</option>
            </select>
          </div>
          <div>
            <label>Preferred language</label>
            <select value={f.language} onChange={e => up('language', e.target.value)}>
              {LANGS.map(([c, n]) => <option key={c} value={c}>{n}</option>)}
            </select>
          </div>
        </div>
        {f.role === 'SUPPORT' && (<>
          <label>Specialty</label>
          <input placeholder="e.g. General Medicine, Yoga" value={f.specialty}
            onChange={e => up('specialty', e.target.value)} />
        </>)}
        <div style={{ height: 18 }} />
        <button className="btn block" onClick={submit} disabled={busy}>
          {busy ? 'Creating…' : 'Create account'}
        </button>
        <p className="muted" style={{ marginTop: 16 }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
