import { apiRequest } from './apiClient';
import {
  ThreatIntelIoc,
  ThreatIntelStats,
  CreateIocPayload,
  IocLookupResult,
} from '../types/threatIntel';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export async function fetchThreatIntelIocs(params?: {
  type?: string;
  search?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<ThreatIntelIoc>> {
  const query = new URLSearchParams();
  if (params?.type && params.type !== 'ALL') query.set('type', params.type);
  if (params?.search) query.set('search', params.search);
  if (params?.page !== undefined) query.set('page', params.page.toString());
  if (params?.size !== undefined) query.set('size', params.size.toString());

  const qs = query.toString();
  return apiRequest<PageResponse<ThreatIntelIoc>>(`/threat-intel/iocs${qs ? `?${qs}` : ''}`);
}

export async function fetchThreatIntelStats(): Promise<ThreatIntelStats> {
  return apiRequest<ThreatIntelStats>('/threat-intel/stats');
}

export async function lookupIoc(value: string): Promise<IocLookupResult> {
  return apiRequest<IocLookupResult>('/threat-intel/lookup', {
    method: 'POST',
    body: JSON.stringify({ value }),
  });
}

export async function createThreatIntelIoc(payload: CreateIocPayload): Promise<ThreatIntelIoc> {
  return apiRequest<ThreatIntelIoc>('/threat-intel/iocs', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function deleteThreatIntelIoc(iocId: string): Promise<void> {
  return apiRequest<void>(`/threat-intel/iocs/${iocId}`, {
    method: 'DELETE',
  });
}
