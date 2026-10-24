const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const TOKEN_STORAGE_KEY = 'sentinelx_jwt_token';
export const USER_STORAGE_KEY = 'sentinelx_user_session';

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearStoredAuth(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
}

export class ApiError extends Error {
  status: number;
  type?: string;
  detail?: string;
  title?: string;
  fieldErrors?: Array<{ field: string; message: string }>;

  constructor(status: number, message: string, data?: Record<string, unknown>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    if (data) {
      this.type = typeof data.type === 'string' ? data.type : undefined;
      this.title = typeof data.title === 'string' ? data.title : undefined;
      this.detail = typeof data.detail === 'string' ? data.detail : undefined;
      if (Array.isArray(data.fieldErrors)) {
        this.fieldErrors = data.fieldErrors as Array<{ field: string; message: string }>;
      }
    }
  }
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(options.headers || {});

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${API_BASE_URL}${cleanEndpoint}`;

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorData: Record<string, unknown> = {};
    try {
      errorData = await response.json();
    } catch {
      // not JSON
    }

    const message =
      (typeof errorData.detail === 'string' && errorData.detail) ||
      (typeof errorData.title === 'string' && errorData.title) ||
      `Request failed with HTTP ${response.status}: ${response.statusText}`;

    throw new ApiError(response.status, message, errorData);
  }

  // Handle empty responses (204 No Content, etc.)
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
