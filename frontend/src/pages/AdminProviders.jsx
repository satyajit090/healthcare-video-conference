import React, { useEffect, useState } from 'react';
import { Modal } from './Dashboard.jsx';
import { api } from '../lib/api.js';

const BLANK = {
  name: '', type: 'INTERNAL', enabled: true, isDefault: false, priority: 1,
  apiKey: '', apiSecret: '', maxParticipants: 2, maxDurationMinutes: 60,
};

export default function AdminProviders() {
  const [list, setList] = useState([]);
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [editing, setEditing] = useState(null); // null | {} | provider

  async function load() {
    try { setList(await api.get('/providers')); } catch (e) { setErr(e.message); }
  }
  useEffect(() => { load(); }, []);

  function flash(msg) { setOk(msg); setTimeout(() => setOk(''), 2500); }

  async function toggle(p) {
    try { await api.post(`/providers/${p.id}/toggle`, { enabled: !p.enabled }); load(); } catch (e) { setErr(e.message); }
  }
  async function makeDefault(p) {
    try { await api.post(`/providers/${p.id}/default`); flash(`${p.name} is now the default provider`); load(); }
    catch (e) { setErr(e.message); }
  }
  async function test(p) {
    try {
      const res = await api.post(`/providers/${p.id}/test`);
      flash(`${p.name} test: ${res.lastTestStatus}`); load();
    } catch (e) { setErr(e.message); }
  }
  async function remove(p) {
    if (!window.confirm(`Delete provider "${p.name}"?`)) return;
    try { await api.del(`/providers/${p.id}`); load(); } catch (e) { setErr(e.message); }
  }

  return (
    <div>
      <div className="card">
        <div className="spread">
          <div>
            <h2>Video providers</h2>
            <p className="sub" style={{ margin: 0 }}>
              Configure the video backend, set a default and a failover priority order.
            </p>
          </div>
          <button className="btn" onClick={() => setEditing({ ...BLANK })}>+ Add provider</button>
        </div>
        {err && <div className="err">{err}</div>}
        {ok && <div className="ok-banner">{ok}</div>}
      </div>

      {list
        .slice()
        .sort((a, b) => a.priority - b.priority)
        .map(p => (
          <div key={p.id} className="card">
            <div className="spread">
              <div>
                <b style={{ fontSize: 16 }}>{p.name}</b>
                {p.isDefault && <span className="tag green" style={{ marginLeft: 8 }}>DEFAULT</span>}
                <span className={`tag ${p.enabled ? 'blue' : 'gray'}`} style={{ marginLeft: 6 }}>
                  {p.enabled ? 'ENABLED' : 'DISABLED'}
                </span>
                <div className="muted" style={{ marginTop: 4 }}>
                  {p.type} · priority {p.priority} · up to {p.maxParticipants} participants · {p.maxDurationMinutes}m max
                  {' · '}credentials: {p.hasCredentials ? 'set' : 'none'}
                  {p.lastTestStatus && ` · last test: ${p.lastTestStatus}`}
                </div>
              </div>
            </div>
            <div className="row" style={{ marginTop: 12 }}>
              <button className="btn sm" onClick={() => test(p)}>Test connection</button>
              <button className="btn sm accent" onClick={() => toggle(p)}>
                {p.enabled ? 'Disable' : 'Enable'}
              </button>
              {!p.isDefault && <button className="btn sm warn" onClick={() => makeDefault(p)}>Set default</button>}
              <button className="btn sm ghost" style={{ border: '1px solid var(--line)', color: 'var(--ink)' }}
                onClick={() => setEditing(p)}>Edit</button>
              <button className="btn sm danger" onClick={() => remove(p)}>Delete</button>
            </div>
          </div>
        ))}

      {editing && (
        <ProviderModal
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); flash('Saved'); load(); }}
          onError={setErr}
        />
      )}
    </div>
  );
}

function ProviderModal({ initial, onClose, onSaved, onError }) {
  const isNew = !initial.id;
  const [f, setF] = useState({
    name: initial.name || '', type: initial.type || 'INTERNAL',
    enabled: initial.enabled ?? true, isDefault: initial.isDefault ?? false,
    priority: initial.priority ?? 1, apiKey: '', apiSecret: '',
    maxParticipants: initial.maxParticipants ?? 2,
    maxDurationMinutes: initial.maxDurationMinutes ?? 60,
  });
  const [busy, setBusy] = useState(false);
  const up = (k, v) => setF(p => ({ ...p, [k]: v }));

  async function save() {
    setBusy(true);
    const body = {
      name: f.name, type: f.type, enabled: f.enabled, isDefault: f.isDefault,
      priority: Number(f.priority),
      apiKey: f.apiKey || null, apiSecret: f.apiSecret || null,
      maxParticipants: Number(f.maxParticipants),
      maxDurationMinutes: Number(f.maxDurationMinutes),
    };
    try {
      if (isNew) await api.post('/providers', body);
      else await api.put(`/providers/${initial.id}`, body);
      onSaved();
    } catch (e) { onError(e.message); setBusy(false); }
  }

  return (
    <Modal title={isNew ? 'Add provider' : `Edit ${initial.name}`} onClose={onClose}>
      <label>Name</label>
      <input value={f.name} onChange={e => up('name', e.target.value)} placeholder="e.g. Internal WebRTC" />
      <div className="grid cols-2">
        <div>
          <label>Type</label>
          <select value={f.type} onChange={e => up('type', e.target.value)}>
            <option value="INTERNAL">INTERNAL (WebRTC)</option>
            <option value="ZOOM">ZOOM</option>
            <option value="TEAMS">TEAMS</option>
          </select>
        </div>
        <div>
          <label>Priority (failover order)</label>
          <input type="number" value={f.priority} onChange={e => up('priority', e.target.value)} />
        </div>
      </div>
      {f.type !== 'INTERNAL' && (<>
        <label>API key</label>
        <input value={f.apiKey} onChange={e => up('apiKey', e.target.value)}
          placeholder={initial.hasCredentials ? '•••••• (leave blank to keep)' : 'enter API key'} />
        <label>API secret</label>
        <input value={f.apiSecret} onChange={e => up('apiSecret', e.target.value)}
          placeholder="leave blank to keep existing" />
      </>)}
      <div className="grid cols-2">
        <div>
          <label>Max participants</label>
          <input type="number" value={f.maxParticipants} onChange={e => up('maxParticipants', e.target.value)} />
        </div>
        <div>
          <label>Max duration (min)</label>
          <input type="number" value={f.maxDurationMinutes} onChange={e => up('maxDurationMinutes', e.target.value)} />
        </div>
      </div>
      <div className="row" style={{ marginTop: 12 }}>
        <label className="row" style={{ margin: 0 }}>
          <input type="checkbox" style={{ width: 'auto' }} checked={f.enabled}
            onChange={e => up('enabled', e.target.checked)} /> <span>Enabled</span>
        </label>
        <label className="row" style={{ margin: 0, marginLeft: 16 }}>
          <input type="checkbox" style={{ width: 'auto' }} checked={f.isDefault}
            onChange={e => up('isDefault', e.target.checked)} /> <span>Default</span>
        </label>
      </div>
      <div className="row" style={{ marginTop: 18, justifyContent: 'flex-end' }}>
        <button className="btn ghost" style={{ border: '1px solid var(--line)', color: 'var(--ink)' }}
          onClick={onClose}>Cancel</button>
        <button className="btn" onClick={save} disabled={busy || !f.name.trim()}>
          {busy ? 'Saving…' : 'Save'}
        </button>
      </div>
    </Modal>
  );
}
