import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';

const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;

export default function Schedule() {
  const nav = useNavigate();
  const [support, setSupport] = useState([]);
  const [list, setList] = useState([]);
  const [f, setF] = useState({
    supportId: '', caseType: 'General Medicine', reason: '',
    scheduledAt: '', durationMinutes: 30, timezone: tz,
  });
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);
  const up = (k, v) => setF(p => ({ ...p, [k]: v }));

  async function load() {
    try { setSupport(await api.get('/users/support')); } catch { /* ignore */ }
    try { setList(await api.get('/schedule')); } catch { /* ignore */ }
  }
  useEffect(() => { load(); }, []);

  async function book() {
    setErr(''); setOk('');
    if (!f.reason.trim()) { setErr('Please add a reason.'); return; }
    if (!f.scheduledAt) { setErr('Please pick a date & time.'); return; }
    setBusy(true);
    try {
      await api.post('/schedule', {
        supportId: f.supportId ? Number(f.supportId) : null,
        caseType: f.caseType,
        reason: f.reason,
        scheduledAt: new Date(f.scheduledAt).toISOString(),
        durationMinutes: Number(f.durationMinutes),
        timezone: f.timezone,
      });
      setOk('Call scheduled! You will get reminders 24h and 1h before.');
      up('reason', ''); up('scheduledAt', '');
      load();
    } catch (e) { setErr(e.message); } finally { setBusy(false); }
  }

  async function cancel(id) {
    try { await api.post(`/schedule/${id}/cancel`); load(); } catch { /* ignore */ }
  }

  return (
    <div className="grid cols-2">
      <div className="card">
        <h2>Schedule a call</h2>
        <p className="sub">Book a future session with a specialist</p>
        {err && <div className="err">{err}</div>}
        {ok && <div className="ok-banner">{ok}</div>}

        <label>Specialist (optional)</label>
        <select value={f.supportId} onChange={e => up('supportId', e.target.value)}>
          <option value="">Any available specialist</option>
          {support.map(s => (
            <option key={s.id} value={s.id}>{s.fullName} — {s.specialty || 'General'}</option>
          ))}
        </select>

        <label>Type</label>
        <select value={f.caseType} onChange={e => up('caseType', e.target.value)}>
          <option>General Medicine</option>
          <option>Yoga</option>
          <option>Mental Wellness</option>
          <option>Nutrition</option>
          <option>Other</option>
        </select>

        <label>Reason</label>
        <textarea value={f.reason} onChange={e => up('reason', e.target.value)}
          placeholder="What would you like to discuss?" />

        <div className="grid cols-2">
          <div>
            <label>Date & time</label>
            <input type="datetime-local" value={f.scheduledAt}
              onChange={e => up('scheduledAt', e.target.value)} />
          </div>
          <div>
            <label>Duration (min)</label>
            <select value={f.durationMinutes} onChange={e => up('durationMinutes', e.target.value)}>
              <option value={15}>15</option>
              <option value={30}>30</option>
              <option value={45}>45</option>
              <option value={60}>60</option>
            </select>
          </div>
        </div>
        <p className="muted">Timezone: {f.timezone}</p>

        <button className="btn block" onClick={book} disabled={busy}>
          {busy ? 'Booking…' : 'Book call'}
        </button>
      </div>

      <div className="card">
        <h2>Your scheduled calls</h2>
        <p className="sub">Upcoming and past bookings</p>
        {list.length === 0 && <p className="muted">No scheduled calls yet.</p>}
        {list.map(s => (
          <div key={s.id} className="list-item">
            <div className="spread">
              <b>{s.caseType || 'Support call'}</b>
              <span className={`tag ${s.status === 'SCHEDULED' ? 'blue' : 'gray'}`}>{s.status}</span>
            </div>
            <div className="muted">{new Date(s.scheduledAt).toLocaleString()} · {s.durationMinutes}m</div>
            <div className="muted" style={{ marginTop: 4 }}>{s.reason}</div>
            {s.status === 'SCHEDULED' && (
              <div className="row" style={{ marginTop: 10 }}>
                <button className="btn sm" onClick={() => nav(`/room/${s.roomId}`)}>Join room</button>
                <button className="btn sm danger" onClick={() => cancel(s.id)}>Cancel</button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
