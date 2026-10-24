import { LoginRequest, LoginResponse, RegisterRequest, UserSession } from '../types/auth';
import { apiRequest, setStoredToken, clearStoredAuth } from './apiClient';

export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const response = await apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });

  if (response.token) {
    setStoredToken(response.token);
  }

  return response;
}

export async function registerUser(payload: RegisterRequest): Promise<LoginResponse> {
  const response = await apiRequest<LoginResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (response.token) {
    setStoredToken(response.token);
  }

  return response;
}

export async function getCurrentUser(): Promise<UserSession> {
  return apiRequest<UserSession>('/auth/me');
}

export async function logout(): Promise<void> {
  try {
    await apiRequest('/auth/logout', { method: 'POST' });
  } catch {
    // ignore
  } finally {
    clearStoredAuth();
  }
}
