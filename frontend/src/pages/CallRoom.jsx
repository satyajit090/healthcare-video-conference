import React, { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, getUser } from '../lib/api.js';
import { createStompClient } from '../lib/ws.js';
import { Modal } from './Dashboard.jsx';

const ICE = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] };

export default function CallRoom() {
  const { roomId } = useParams();
  const nav = useNavigate();
  const me = getUser();

  const [call, setCall] = useState(null);
  const [status, setStatus] = useState('Connecting…');
  const [micOn, setMicOn] = useState(true);
  const [camOn, setCamOn] = useState(true);
  const [sharing, setSharing] = useState(false);
  const [remoteJoined, setRemoteJoined] = useState(false);
  const [showRate, setShowRate] = useState(false);
  const [ended, setEnded] = useState(false);

  const localVideo = useRef(null);
  const remoteVideo = useRef(null);
  const localStream = useRef(null);
  const camTrack = useRef(null);
  const pc = useRef(null);
  const client = useRef(null);
  const pendingIce = useRef([]);
  const remoteDescSet = useRef(false);
  const initiator = useRef(false);
  const offered = useRef(false);
  const endedRef = useRef(false);

  const send = useCallback((type, payload) => {
    if (!client.current?.connected) return;
    client.current.publish({
      destination: `/app/signal/${roomId}`,
      body: JSON.stringify({ type, fromUserId: me.userId, fromName: me.fullName, payload }),
    });
  }, [roomId, me.userId, me.fullName]);

  const buildPc = useCallback(() => {
    const peer = new RTCPeerConnection(ICE);
    peer.onicecandidate = (e) => { if (e.candidate) send('ice', e.candidate); };
    peer.ontrack = (e) => {
      if (remoteVideo.current && e.streams[0]) {
        remoteVideo.current.srcObject = e.streams[0];
        setRemoteJoined(true);
        setStatus('Connected');
      }
    };
    peer.onconnectionstatechange = () => {
      if (['disconnected', 'failed', 'closed'].includes(peer.connectionState)) {
        setRemoteJoined(false);
      }
    };
    localStream.current?.getTracks().forEach(t => peer.addTrack(t, localStream.current));
    return peer;
  }, [send]);

  const makeOffer = useCallback(async () => {
    if (offered.current) return;
    if (pc.current.signalingState !== 'stable') return;
    offered.current = true;
    try {
      const offer = await pc.current.createOffer();
      await pc.current.setLocalDescription(offer);
      send('offer', offer);
    } catch (e) { offered.current = false; console.error('offer failed', e); }
  }, [send]);

  const flushIce = useCallback(async () => {
    for (const c of pendingIce.current) {
      try { await pc.current.addIceCandidate(c); } catch (e) { console.error(e); }
    }
    pendingIce.current = [];
  }, []);

  const onSignal = useCallback(async (msg) => {
    if (msg.fromUserId === me.userId) return;

    if (msg.type === 'hello') {
      send('hello-ack', null);
      if (initiator.current) makeOffer();
    } else if (msg.type === 'hello-ack') {
      if (initiator.current) makeOffer();
    } else if (msg.type === 'offer') {
      await pc.current.setRemoteDescription(new RTCSessionDescription(msg.payload));
      remoteDescSet.current = true;
      await flushIce();
      const answer = await pc.current.createAnswer();
      await pc.current.setLocalDescription(answer);
      send('answer', answer);
    } else if (msg.type === 'answer') {
      await pc.current.setRemoteDescription(new RTCSessionDescription(msg.payload));
      remoteDescSet.current = true;
      await flushIce();
    } else if (msg.type === 'ice') {
      const cand = new RTCIceCandidate(msg.payload);
      if (remoteDescSet.current) {
        try { await pc.current.addIceCandidate(cand); } catch (e) { console.error(e); }
      } else {
        pendingIce.current.push(cand);
      }
    } else if (msg.type === 'bye') {
      setRemoteJoined(false);
      setStatus('The other participant left');
    }
  }, [me.userId, send, makeOffer, flushIce]);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      let callData;
      try {
        callData = await api.get(`/calls/room/${roomId}`);
        if (cancelled) return;
        setCall(callData);
      } catch (e) {
        setStatus('Could not load this call: ' + e.message);
        return;
      }

      const otherId = me.userId === callData.patientId ? callData.supportId : callData.patientId;
      initiator.current = otherId == null ? false : me.userId < otherId;

      // DO NOT mark active here — only mark active when both peers are in the room
      // The support user accepting the call handles status transitions

      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return; }
        localStream.current = stream;
        camTrack.current = stream.getVideoTracks()[0];
        if (localVideo.current) localVideo.current.srcObject = stream;
      } catch (e) {
        setStatus('Camera/mic permission needed to join the call.');
        return;
      }

      pc.current = buildPc();

      const c = createStompClient(() => {
        c.subscribe(`/topic/room/${roomId}`, (frame) => {
          try { onSignal(JSON.parse(frame.body)); } catch (e) { console.error(e); }
        });
        send('hello', null);
        setStatus('Waiting for the other participant…');
      });
      client.current = c;
    })();

    return () => {
      cancelled = true;
      // Cleanup media and connections only — do NOT call the end API here
      // to avoid marking calls as COMPLETED on component unmount/remount
      try { send('bye', null); } catch { }
      try { client.current?.deactivate(); } catch { }
      try { pc.current?.close(); } catch { }
      localStream.current?.getTracks().forEach(t => t.stop());
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  function toggleMic() {
    const track = localStream.current?.getAudioTracks()[0];
    if (track) { track.enabled = !track.enabled; setMicOn(track.enabled); }
  }

  function toggleCam() {
    const track = camTrack.current;
    if (track) { track.enabled = !track.enabled; setCamOn(track.enabled); }
  }

  async function toggleShare() {
    const sender = pc.current?.getSenders().find(s => s.track && s.track.kind === 'video');
    if (!sharing) {
      try {
        const ds = await navigator.mediaDevices.getDisplayMedia({ video: true });
        const screenTrack = ds.getVideoTracks()[0];
        await sender?.replaceTrack(screenTrack);
        if (localVideo.current) localVideo.current.srcObject = ds;
        setSharing(true);
        screenTrack.onended = () => stopShare(sender);
      } catch { /* user cancelled */ }
    } else {
      stopShare(sender);
    }
  }

  async function stopShare(sender) {
    await sender?.replaceTrack(camTrack.current);
    if (localVideo.current) localVideo.current.srcObject = localStream.current;
    setSharing(false);
  }

  async function endCall() {
    // Guard against double-firing (e.g. button clicked twice, or cleanup + click)
    if (endedRef.current) return;
    endedRef.current = true;
    setEnded(true);

    try { await api.post(`/calls/${call.id}/end`); } catch { /* ignore */ }
    send('bye', null);
    try { client.current?.deactivate(); } catch { }
    try { pc.current?.close(); } catch { }
    localStream.current?.getTracks().forEach(t => t.stop());

    if (me.role === 'PATIENT') {
      setShowRate(true);
    } else {
      nav('/history');
    }
  }

  return (
    <div className="room">
      <div className="stage">
        <div className="videos">
          <div className="video-tile">
            <video ref={localVideo} autoPlay playsInline muted />
            <span className="label">You {!micOn && '🔇'} {!camOn && '📷❌'}</span>
          </div>
          <div className="video-tile">
            <video ref={remoteVideo} autoPlay playsInline />
            <span className="label">
              {call
                ? (me.userId === call.patientId
                    ? (call.supportName || 'Specialist')
                    : call.patientName)
                : 'Peer'}
              {!remoteJoined && ' (waiting…)'}
            </span>
          </div>
        </div>

        <div className="controls">
          <button className={`ctrl ${micOn ? 'active' : 'off'}`} onClick={toggleMic} title="Mic">
            {micOn ? '🎤' : '🔇'}
          </button>
          <button className={`ctrl ${camOn ? 'active' : 'off'}`} onClick={toggleCam} title="Camera">
            {camOn ? '📹' : '🚫'}
          </button>
          <button className={`ctrl ${sharing ? 'off' : 'active'}`} onClick={toggleShare} title="Share screen">
            🖥️
          </button>
          <button className="ctrl end" onClick={endCall} disabled={ended}>End call</button>
        </div>

        <p style={{ color: '#94a3b8', textAlign: 'center', marginTop: 10, fontSize: 13 }}>
          {ended ? 'Call ended.' : status}
          {call?.recordingConsent && !ended && ' · 🔴 recording consented'}
        </p>
      </div>

      <SidePanel call={call} roomId={roomId} me={me} />

      {showRate && (
        <RateModal callId={call.id} onDone={() => nav('/history')} />
      )}
    </div>
  );
}

function SidePanel({ call, roomId, me }) {
  const [tab, setTab] = useState('chat');
  return (
    <div className="sidepanel">
      <div className="tabs">
        <button className={tab === 'chat' ? 'active' : ''} onClick={() => setTab('chat')}>Chat</button>
        {me.role === 'SUPPORT' && (
          <button className={tab === 'notes' ? 'active' : ''} onClick={() => setTab('notes')}>Notes</button>
        )}
        {me.role === 'SUPPORT' && (
          <button className={tab === 'ai' ? 'active' : ''} onClick={() => setTab('ai')}>AI Assist</button>
        )}
      </div>
      {tab === 'chat' && <ChatTab roomId={roomId} me={me} />}
      {tab === 'notes' && <NotesTab call={call} />}
      {tab === 'ai' && <AiTab call={call} />}
    </div>
  );
}

function ChatTab({ roomId, me }) {
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const client = useRef(null);

  useEffect(() => {
    const c = createStompClient(() => {
      c.subscribe(`/topic/chat/${roomId}`, (frame) => {
        try { setMsgs(prev => [...prev, JSON.parse(frame.body)]); } catch { }
      });
    });
    client.current = c;
    return () => { try { c.deactivate(); } catch { } };
  }, [roomId]);

  function sendMsg() {
    if (!text.trim() || !client.current?.connected) return;
    client.current.publish({
      destination: `/app/chat/${roomId}`,
      body: JSON.stringify({
        fromUserId: me.userId,
        fromName: me.fullName,
        text,
        sentAt: new Date().toISOString(),
      }),
    });
    setText('');
  }

  return (
    <>
      <div className="panel-body">
        {msgs.length === 0 && <p className="muted">No messages yet. Say hello 👋</p>}
        {msgs.map((m, i) => (
          <div key={i} className="chat-msg">
            <b>{m.fromUserId === me.userId ? 'You' : m.fromName}:</b> {m.text}
          </div>
        ))}
      </div>
      <div className="panel-foot">
        <input
          value={text}
          onChange={e => setText(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && sendMsg()}
          placeholder="Type a message…"
        />
        <button className="btn sm" onClick={sendMsg}>Send</button>
      </div>
    </>
  );
}

function NotesTab({ call }) {
  const [notes, setNotes] = useState([]);
  const [text, setText] = useState('');

  async function load() {
    if (!call) return;
    try {
      const d = await api.get(`/calls/${call.id}`);
      setNotes(d.notes);
    } catch { }
  }

  useEffect(() => { load(); }, [call]);

  async function add() {
    if (!text.trim()) return;
    try {
      await api.post(`/calls/${call.id}/notes`, { text });
      setText('');
      load();
    } catch { }
  }

  return (
    <>
      <div className="panel-body">
        {notes.length === 0 && <p className="muted">No notes yet.</p>}
        {notes.map(n => (
          <div key={n.id} className="bubble">
            {n.text}
            <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>
              {n.authorName} · {new Date(n.createdAt).toLocaleTimeString()}
            </div>
          </div>
        ))}
      </div>
      <div className="panel-foot">
        <input
          value={text}
          onChange={e => setText(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && add()}
          placeholder="Add a private note…"
        />
        <button className="btn sm" onClick={add}>Save</button>
      </div>
    </>
  );
}

function AiTab({ call }) {
  const [prompt, setPrompt] = useState('');
  const [answer, setAnswer] = useState('');
  const [busy, setBusy] = useState(false);

  async function ask() {
    if (!prompt.trim()) return;
    setBusy(true);
    setAnswer('');
    try {
      const res = await api.post('/ai/assist', {
        prompt,
        context: call ? `${call.caseType}: ${call.reason}` : '',
      });
      setAnswer(res.response);
    } catch (e) {
      setAnswer('AI error: ' + e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="panel-body">
        <p className="muted">
          Ask the AI assistant for phrasing help, follow-up questions or resources. Non-diagnostic.
        </p>
        {answer && <div className="bubble">{answer}</div>}
        {busy && <p className="muted">Thinking…</p>}
      </div>
      <div className="panel-foot">
        <input
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && ask()}
          placeholder="e.g. Suggest calming breathing steps"
        />
        <button className="btn sm" onClick={ask} disabled={busy}>Ask</button>
      </div>
    </>
  );
}

function RateModal({ callId, onDone }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    try { await api.post(`/calls/${callId}/rate`, { rating, comment }); } catch { }
    onDone();
  }

  return (
    <Modal title="How was your call?" onClose={onDone}>
      <div className="stars" style={{ textAlign: 'center' }}>
        {[1, 2, 3, 4, 5].map(n => (
          <span key={n} className={n <= rating ? 'on' : ''} onClick={() => setRating(n)}>★</span>
        ))}
      </div>
      <label>Comments (optional)</label>
      <textarea
        value={comment}
        onChange={e => setComment(e.target.value)}
        placeholder="Tell us about your experience…"
      />
      <div className="row" style={{ marginTop: 16, justifyContent: 'flex-end' }}>
        <button
          className="btn ghost"
          style={{ border: '1px solid var(--line)', color: 'var(--ink)' }}
          onClick={onDone}
        >
          Skip
        </button>
        <button className="btn" onClick={submit} disabled={busy || rating === 0}>
          Submit
        </button>
      </div>
    </Modal>
  );
}