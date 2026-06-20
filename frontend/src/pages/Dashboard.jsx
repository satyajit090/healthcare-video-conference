import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, getUser } from '../lib/api.js';

const LANG_NAMES = { en: 'English', hi: 'Hindi', or: 'Odia', es: 'Spanish', fr: 'French' };

export default function Dashboard() {
  const user = getUser();
  if (user.role === 'PATIENT') return <PatientHome />;
  if (user.role === 'SUPPORT') return <SupportHome />;
  return <AdminHome />;
}

/* ---------------- PATIENT ---------------- */
function PatientHome() {
  const nav = useNavigate();
  const [support, setSupport] = useState([]);
  const [upcoming, setUpcoming] = useState([]);
  const [showModal, setShowModal] = useState(false);

  async function load() {
    try { setSupport(await api.get('/users/support')); } catch { /* ignore */ }
    try {
      const all = await api.get('/schedule');
      setUpcoming(all.filter(s => s.status === 'SCHEDULED'));
    } catch { /* ignore */ }
  }
  useEffect(() => { load(); }, []);

  const online = support.filter(s => s.availability === 'AVAILABLE');

  return (
    <div>
      <div className="card">
        <div className="spread">
          <div>
            <h2>Need help now?</h2>
            <p className="sub" style={{ margin: 0 }}>
              Start an instant video support call. The next available specialist will pick it up.
            </p>
          </div>
          <button className="btn" onClick={() => setShowModal(true)}>🎥 Start Video Support</button>
        </div>
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h2>Specialists online</h2>
          <p className="sub">Who can take your call right now</p>
          {support.length === 0 && <p className="muted">No specialists registered yet.</p>}
          {support.map(s => (
            <div key={s.id} className="list-item spread">
              <div>
                <b>{s.fullName}</b>
                <div className="muted">{s.specialty || 'General'} · {LANG_NAMES[s.language] || s.language}</div>
              </div>
              <span className={`tag ${s.availability === 'AVAILABLE' ? 'green' : s.availability === 'BUSY' ? 'amber' : 'gray'}`}>
                {s.availability}
              </span>
            </div>
          ))}
          <p className="muted">{online.length} available now</p>
        </div>

        <div className="card">
          <div className="spread">
            <h2>Upcoming calls</h2>
            <button className="btn sm accent" onClick={() => nav('/schedule')}>Schedule new</button>
          </div>
          <p className="sub">Your booked sessions</p>
          {upcoming.length === 0 && <p className="muted">Nothing scheduled.</p>}
          {upcoming.map(s => (
            <div key={s.id} className="list-item">
              <div className="spread">
                <b>{s.caseType || 'Support call'}</b>
                <span className="tag blue">{new Date(s.scheduledAt).toLocaleString()}</span>
              </div>
              <div className="muted">{s.reason}</div>
              <button className="btn sm" style={{ marginTop: 8 }}
                onClick={() => nav(`/room/${s.roomId}`)}>Join room</button>
            </div>
          ))}
        </div>
      </div>

      {showModal && <InstantCallModal onClose={() => setShowModal(false)}
        onStarted={(call) => nav(`/room/${call.roomId}`)} />}
    </div>
  );
}

function InstantCallModal({ onClose, onStarted }) {
  const [caseType, setCaseType] = useState('General Medicine');
  const [reason, setReason] = useState('');
  const [consent, setConsent] = useState(false);
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function start() {
    if (!reason.trim()) { setErr('Please describe the reason briefly.'); return; }
    setErr(''); setBusy(true);
    try {
      const call = await api.post('/calls/instant',
        { caseType, reason, recordingConsent: consent });
      onStarted(call);
    } catch (e) { setErr(e.message); setBusy(false); }
  }

  return (
    <Modal title="Start video support" onClose={onClose}>
      {err && <div className="err">{err}</div>}
      <label>Type of support</label>
      <select value={caseType} onChange={e => setCaseType(e.target.value)}>
        <option>General Medicine</option>
        <option>Yoga</option>
        <option>Mental Wellness</option>
        <option>Nutrition</option>
        <option>Other</option>
      </select>
      <label>What do you need help with?</label>
      <textarea value={reason} onChange={e => setReason(e.target.value)}
        placeholder="Briefly describe your concern…" />
      <label className="row" style={{ marginTop: 12 }}>
        <input type="checkbox" style={{ width: 'auto' }} checked={consent}
          onChange={e => setConsent(e.target.checked)} />
        <span>I consent to this call being recorded for quality & safety</span>
      </label>
      <div className="row" style={{ marginTop: 18, justifyContent: 'flex-end' }}>
        <button className="btn ghost" style={{ border: '1px solid var(--line)', color: 'var(--ink)' }}
          onClick={onClose}>Cancel</button>
        <button className="btn" onClick={start} disabled={busy}>
          {busy ? 'Connecting…' : 'Start call'}
        </button>
      </div>
    </Modal>
  );
}

/* ---------------- SUPPORT ---------------- */
function SupportHome() {
  const nav = useNavigate();
  const [me, setMe] = useState(null);
  const [queueCount, setQueueCount] = useState(0);

  async function load() {
    try { setMe(await api.get('/users/me')); } catch { /* ignore */ }
    try { const q = await api.get('/calls/queue'); setQueueCount(q.length); } catch { /* ignore */ }
  }
  useEffect(() => {
    load();
    const t = setInterval(load, 10000);
    return () => clearInterval(t);
  }, []);

  async function setAvail(a) {
    try { setMe(await api.patch('/users/me/availability', { availability: a })); } catch { /* ignore */ }
  }

  return (
    <div>
      <div className="card">
        <h2>Your availability</h2>
        <p className="sub">Patients only see you for instant calls when you are AVAILABLE.</p>
        <div className="row">
          {['AVAILABLE', 'BUSY', 'OFFLINE'].map(a => (
            <button key={a}
              className={`btn ${me?.availability === a ? '' : 'ghost'}`}
              style={me?.availability === a ? {} : { border: '1px solid var(--line)', color: 'var(--ink)' }}
              onClick={() => setAvail(a)}>{a}</button>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="spread">
          <div>
            <h2>Waiting queue</h2>
            <p className="sub" style={{ margin: 0 }}>
              {queueCount > 0 ? `${queueCount} patient(s) waiting` : 'No patients waiting right now'}
            </p>
          </div>
          <button className="btn accent" onClick={() => nav('/queue')}>Open queue</button>
        </div>
      </div>
    </div>
  );
}

/* ---------------- ADMIN ---------------- */
function AdminHome() {
  const nav = useNavigate();
  const [data, setData] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    api.get('/analytics/overview').then(setData).catch(e => setErr(e.message));
  }, []);

  return (
    <div>
      <div className="card">
        <div className="spread">
          <div>
            <h2>Platform overview</h2>
            <p className="sub" style={{ margin: 0 }}>Live metrics across all calls</p>
          </div>
          <button className="btn accent" onClick={() => nav('/admin/providers')}>Manage providers</button>
        </div>
      </div>

      {err && <div className="err">{err}</div>}
      {data && (
        <>
          <div className="grid cols-3">
            <Stat label="Total calls" value={data.totalCalls} />
            <Stat label="Completed" value={data.completedCalls} />
            <Stat label="Avg rating" value={data.averageRating ? `${data.averageRating} ★` : '—'} />
          </div>
          <div className="card">
            <h2>Calls by status</h2>
            {Object.entries(data.byStatus || {}).map(([k, v]) => (
              <div key={k} className="spread" style={{ padding: '6px 0', borderBottom: '1px solid var(--line)' }}>
                <span>{k}</span><b>{v}</b>
              </div>
            ))}
            <p className="muted" style={{ marginTop: 10 }}>
              Average duration: {fmtDuration(data.averageDurationSeconds)}
            </p>
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="card" style={{ textAlign: 'center' }}>
      <div style={{ fontSize: 32, fontWeight: 800, color: 'var(--brand-dark)' }}>{value}</div>
      <div className="muted">{label}</div>
    </div>
  );
}

function fmtDuration(s) {
  if (!s) return '—';
  const m = Math.floor(s / 60), sec = s % 60;
  return `${m}m ${sec}s`;
}

/* ---------------- shared modal ---------------- */
export function Modal({ title, children, onClose }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(15,23,36,.5)',
      display: 'grid', placeItems: 'center', zIndex: 50, padding: 20
    }} onClick={onClose}>
      <div className="card" style={{ width: '100%', maxWidth: 460, margin: 0 }}
        onClick={e => e.stopPropagation()}>
        <h2 style={{ marginBottom: 14 }}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
