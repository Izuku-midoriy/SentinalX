import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserSession, RegisterRequest } from '../types/auth';
import { login as apiLogin, registerUser as apiRegister, logout as apiLogout, getCurrentUser } from '../services/authService';
import { getStoredToken, clearStoredAuth } from '../services/apiClient';

export interface AuthContextType {
  user: UserSession | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password?: string) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  quickSwitch: (roleOrTenant: 'alpha' | 'beta' | 'admin') => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const PRESET_ACCOUNTS = {
  alpha: {
    label: 'Alpha Defense Lab (Analyst)',
    email: 'analyst@alpha.local',
    password: 'DevAnalyst123!',
    tenantSlug: 'alpha-lab',
    role: 'ANALYST',
  },
  beta: {
    label: 'Beta Security Ops (Analyst)',
    email: 'analyst@beta.local',
    password: 'DevBetaAnalyst123!',
    tenantSlug: 'beta-ops',
    role: 'ANALYST',
  },
  admin: {
    label: 'Alpha Defense Lab (Admin)',
    email: 'admin@alpha.local',
    password: 'DevAdmin123!',
    tenantSlug: 'alpha-lab',
    role: 'TENANT_ADMIN',
  },
} as const;

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(null);
  const [token, setToken] = useState<string | null>(getStoredToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const initAuth = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    const existingToken = getStoredToken();

    if (existingToken) {
      try {
        const session = await getCurrentUser();
        setUser(session);
        setToken(existingToken);
        setIsLoading(false);
        return;
      } catch (err) {
        console.warn('Existing token invalid or expired, fallback to default lab login', err);
        clearStoredAuth();
        setToken(null);
      }
    }

    // Auto-login to Alpha Lab by default for seamless SOC testing experience
    try {
      const res = await apiLogin({
        email: PRESET_ACCOUNTS.alpha.email,
        password: PRESET_ACCOUNTS.alpha.password,
      });
      setToken(res.token);
      setUser({
        userId: res.userId,
        email: res.email,
        role: res.role,
        tenantId: res.tenantId,
        tenantSlug: res.tenantSlug,
      });
    } catch (err) {
      // Backend might be offline during initial boot
      console.warn('Initial auto-login could not reach backend API', err);
      setError('Unable to authenticate with backend API. Ensure Spring Boot is running on port 8080.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  const handleLogin = async (email: string, password = 'DevAnalyst123!') => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiLogin({ email, password });
      setToken(res.token);
      setUser({
        userId: res.userId,
        email: res.email,
        role: res.role,
        tenantId: res.tenantId,
        tenantSlug: res.tenantSlug,
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Authentication failed';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (payload: RegisterRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiRegister(payload);
      setToken(res.token);
      setUser({
        userId: res.userId,
        email: res.email,
        displayName: res.displayName,
        role: res.role,
        tenantId: res.tenantId,
        tenantSlug: res.tenantSlug,
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Registration failed';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    setIsLoading(true);
    try {
      await apiLogout();
    } finally {
      setUser(null);
      setToken(null);
      setIsLoading(false);
    }
  };

  const handleQuickSwitch = async (target: 'alpha' | 'beta' | 'admin') => {
    const creds = PRESET_ACCOUNTS[target];
    await handleLogin(creds.email, creds.password);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        isLoading,
        error,
        login: handleLogin,
        register: handleRegister,
        logout: handleLogout,
        quickSwitch: handleQuickSwitch,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
