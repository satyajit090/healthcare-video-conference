// Tiny fetch wrapper with JWT handling. Base URL is proxied to :8080 in dev,
// and same-origin (served behind nginx) in production.
const TOKEN_KEY = 'hc_token';
const USER_KEY = 'hc_user';

export function getToken() { return localStorage.getItem(TOKEN_KEY); }
export function getUser() {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}
export function setSession(auth) {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify({
    userId: auth.userId, fullName: auth.fullName, email: auth.email, role: auth.role
  }));
}
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`/api${path}`, {
    method, headers, body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401 || res.status === 403) {
    // token invalid/expired
    if (!path.startsWith('/auth')) { clearSession(); }
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`);
  return data;
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b),
  patch: (p, b) => request('PATCH', p, b),
  del: (p) => request('DELETE', p),
};
