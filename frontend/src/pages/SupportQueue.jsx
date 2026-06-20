import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';

export default function SupportQueue() {
  const nav = useNavigate();
  const [queue, setQueue] = useState([]);
  const [me, setMe] = useState(null);
  const [err, setErr] = useState('');

  async function load() {
    try { setMe(await api.get('/users/me')); } catch { /* ignore */ }
    try { setQueue(await api.get('/calls/queue')); } catch (e) { setErr(e.message); }
  }
  useEffect(() => {
    load();
    const t = setInterval(load, 7000);
    return () => clearInterval(t);
  }, []);

  async function setAvail(a) {
    try { setMe(await api.patch('/users/me/availability', { availability: a })); } catch { /* ignore */ }
  }

  async function accept(call) {
    try {
      await api.post(`/calls/${call.id}/accept`);
      nav(`/room/${call.roomId}`);
    } catch (e) { setErr(e.message); }
  }

  async function reject(call) {
    try { await api.post(`/calls/${call.id}/reject`); load(); } catch (e) { setErr(e.message); }
  }

  return (
    <div>
      <div className="card">
        <div className="spread">
          <div>
            <h2>Availability</h2>
            <p className="sub" style={{ margin: 0 }}>You currently are <b>{me?.availability || '…'}</b></p>
          </div>
          <div className="row">
            {['AVAILABLE', 'BUSY', 'OFFLINE'].map(a => (
              <button key={a}
                className={`btn sm ${me?.availability === a ? '' : 'ghost'}`}
                style={me?.availability === a ? {} : { border: '1px solid var(--line)', color: 'var(--ink)' }}
                onClick={() => setAvail(a)}>{a}</button>
            ))}
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Waiting queue</h2>
        <p className="sub">Urgent calls are shown first. Accept to join the video room.</p>
        {err && <div className="err">{err}</div>}
        {queue.length === 0 && <p className="muted">No patients waiting. The queue refreshes automatically.</p>}

        {queue.map(c => (
          <div key={c.id} className="list-item">
            <div className="spread">
              <div>
                <b>{c.patientName}</b>
                {c.urgent && <span className="tag red" style={{ marginLeft: 8 }}>URGENT</span>}
                <div className="muted">{c.caseType || 'General'} · {c.patientLanguage || 'en'}
                  {' · '}waiting since {new Date(c.createdAt).toLocaleTimeString()}</div>
              </div>
              <span className="tag amber">{c.status}</span>
            </div>
            <p style={{ margin: '8px 0' }}>{c.reason}</p>
            {c.aiTriageSummary && (
              <div className="bubble"><b>AI triage:</b> {c.aiTriageSummary}</div>
            )}
            <div className="row" style={{ marginTop: 8 }}>
              <button className="btn sm" onClick={() => accept(c)}>Accept & join</button>
              <button className="btn sm danger" onClick={() => reject(c)}>Reject</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
