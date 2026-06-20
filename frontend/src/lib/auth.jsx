import React, { createContext, useContext, useEffect, useState } from 'react';
import { getUser, setSession, clearSession } from './api.js';

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getUser());
  useEffect(() => { setUser(getUser()); }, []);
  const login = (auth) => { setSession(auth); setUser(getUser()); };
  const logout = () => { clearSession(); setUser(null); };
  return <AuthCtx.Provider value={{ user, login, logout }}>{children}</AuthCtx.Provider>;
}

export function useAuth() { return useContext(AuthCtx); }
