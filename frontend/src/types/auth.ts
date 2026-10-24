export type UserRole = 'ANALYST' | 'TENANT_ADMIN' | 'PLATFORM_ADMIN';

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  tenantSlug?: string;
  role?: UserRole;
}

export interface LoginResponse {
  token: string;
  userId: string;
  email: string;
  displayName?: string;
  role: UserRole;
  tenantId: string;
  tenantSlug: string;
}

export interface UserSession {
  userId: string;
  email: string;
  displayName?: string;
  role: UserRole;
  tenantId: string;
  tenantSlug: string;
}
