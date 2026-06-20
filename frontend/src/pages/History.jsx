import React, { useEffect, useState } from 'react';
import { api, getUser } from '../lib/api.js';

const STATUS_TAG = {
  COMPLETED: 'green', ACTIVE: 'blue', REQUESTED: 'amber', RINGING: 'amber',
  CANCELLED: 'gray', REJECTED: 'red', ESCALATED: 'red',
};

export default function History() {
  const me = getUser();
  const [calls, setCalls] = useState([]);
  const [err, setErr] = useState('');
  const [open, setOpen] = useState(null);
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    api.get('/calls/history')
      .then(list => setCalls([...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))))
      .catch(e => setErr(e.message));
  }, []);

  async function toggle(call) {
    if (open === call.id) { setOpen(null); setDetail(null); return; }
    setOpen(call.id); setDetail(null);
    try { setDetail(await api.get(`/calls/${call.id}`)); } catch { /* ignore */ }
  }

  function fmtDur(s) {
    if (!s) return '—';
    const m = Math.floor(s / 60), sec = s % 60;
    return `${m}m ${sec}s`;
  }

  return (
    <div className="card">
      <h2>Call history</h2>
      <p className="sub">Your past and present sessions</p>
      {err && <div className="err">{err}</div>}
      {calls.length === 0 && <p className="muted">No calls yet.</p>}

      {calls.map(c => (
        <div key={c.id} className="list-item">
          <div className="spread">
            <div>
              <b>{c.caseType || 'Support call'}</b>
              {c.urgent && <span className="tag red" style={{ marginLeft: 8 }}>URGENT</span>}
              <div className="muted">
                {me.userId === c.patientId ? `with ${c.supportName || 'unassigned'}` : `patient: ${c.patientName}`}
                {' · '}{new Date(c.createdAt).toLocaleString()}
              </div>
            </div>
            <span className={`tag ${STATUS_TAG[c.status] || 'gray'}`}>{c.status}</span>
          </div>

          <div className="row" style={{ marginTop: 8 }}>
            <span className="muted">Duration: {fmtDur(c.durationSeconds)}</span>
            {c.rating != null && <span className="muted">· Rating: {c.rating}★</span>}
            <button className="btn sm ghost" style={{ border: '1px solid var(--line)', color: 'var(--ink)' }}
              onClick={() => toggle(c)}>{open === c.id ? 'Hide' : 'Details'}</button>
          </div>

          {open === c.id && (
            <div style={{ marginTop: 12, borderTop: '1px solid var(--line)', paddingTop: 12 }}>
              <p><b>Reason:</b> {c.reason}</p>
              {c.aiTriageSummary && (
                <div className="bubble"><b>AI triage:</b> {c.aiTriageSummary}</div>
              )}
              {c.escalationReason && (
                <p className="muted"><b>Escalated:</b> {c.escalationReason}</p>
              )}
              {c.ratingComment && <p className="muted"><b>Feedback:</b> {c.ratingComment}</p>}
              <p style={{ fontWeight: 600, marginTop: 10 }}>Notes</p>
              {!detail && <p className="muted">Loading…</p>}
              {detail && detail.notes.length === 0 && <p className="muted">No notes.</p>}
              {detail && detail.notes.map(n => (
                <div key={n.id} className="bubble">
                  {n.text}
                  <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>
                    {n.authorName} · {new Date(n.createdAt).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
