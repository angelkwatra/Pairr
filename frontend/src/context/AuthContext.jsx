import { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { AuthContext } from './AuthContext';

function decodeToken(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  // Re-sync user details from API
  useEffect(() => {
    if (token) {
      api.get('/api/user/me')
        .then((userData) => {
          setUser(prev => {
            const enriched = { ...prev, ...userData };
            localStorage.setItem('user', JSON.stringify(enriched));
            return enriched;
          });
        })
        .catch(() => {
          const decoded = decodeToken(token);
          if (decoded) {
            setUser(prev => {
              if (prev?.id) return prev;
              const fallback = {
                ...prev,
                id: decoded.id || decoded.sub || decoded.userId,
                email: decoded.email || prev?.email,
                displayName: decoded.displayName || prev?.displayName
              };
              localStorage.setItem('user', JSON.stringify(fallback));
              return fallback;
            });
          }
        });
    }
  }, [token]);

  const login = useCallback((tokenValue, userData) => {
    const decoded = decodeToken(tokenValue);
    const enrichedUser = {
      ...userData,
      id: decoded?.id || decoded?.sub || decoded?.userId || userData.id
    };
    
    localStorage.setItem('token', tokenValue);
    localStorage.setItem('user', JSON.stringify(enrichedUser));
    setToken(tokenValue);
    setUser(enrichedUser);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
}
