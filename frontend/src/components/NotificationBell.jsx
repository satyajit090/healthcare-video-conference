import React, { useEffect, useRef, useState } from 'react';
import { api, getUser } from '../lib/api.js';
import { createStompClient } from '../lib/ws.js';

// Bell icon with unread badge + dropdown list.
// Receives notifications live over STOMP (/topic/notifications/{userId})
// and also polls as a fallback so it works even if the socket drops.
export default function NotificationBell() {
  const user = getUser();
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const clientRef = useRef(null);

  const unread = items.filter(n => !n.read).length;

  async function load() {
    try { setItems(await api.get('/notifications')); } catch { /* ignore */ }
  }

  useEffect(() => {
    if (!user) return;
    load();
    const poll = setInterval(load, 15000);

    const client = createStompClient(() => {
      client.subscribe(`/topic/notifications/${user.userId}`, () => load());
    });
    clientRef.current = client;

    return () => {
      clearInterval(poll);
      try { client.deactivate(); } catch { /* ignore */ }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function markRead(n) {
    if (n.read) return;
    try {
      await api.post(`/notifications/${n.id}/read`);
      setItems(prev => prev.map(x => x.id === n.id ? { ...x, read: true } : x));
    } catch { /* ignore */ }
  }

  function fmt(ts) {
    try { return new Date(ts).toLocaleString(); } catch { return ''; }
  }

  return (
    <div className="bell" onClick={() => setOpen(o => !o)}>
      🔔{unread > 0 && <span className="dot">{unread}</span>}
      {open && (
        <div className="dropdown" onClick={e => e.stopPropagation()}>
          <div className="n" style={{ fontWeight: 700, background: '#f8fafc' }}>
            Notifications
          </div>
          {items.length === 0 && (
            <div className="n muted">No notifications yet</div>
          )}
          {items.map(n => (
            <div key={n.id} className={`n ${n.read ? '' : 'unread'}`} onClick={() => markRead(n)}>
              <div style={{ fontWeight: 600 }}>{n.title}</div>
              <div className="muted" style={{ marginTop: 2 }}>{n.message}</div>
              <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>{fmt(n.createdAt)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
