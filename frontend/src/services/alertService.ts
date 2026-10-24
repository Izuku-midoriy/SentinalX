import { AlertResponse, AlertSearchParams, UpdateAlertRequest } from '../types/alerts';
import { PageResponse } from '../types/pagination';
import { apiRequest } from './apiClient';

export async function searchAlerts(params: AlertSearchParams = {}): Promise<PageResponse<AlertResponse>> {
  const query = new URLSearchParams();

  if (params.severity && params.severity !== 'ALL') {
    query.set('severity', params.severity);
  }
  if (params.status && params.status !== 'ALL') {
    query.set('status', params.status);
  }
  if (params.page !== undefined) {
    query.set('page', params.page.toString());
  }
  if (params.size !== undefined) {
    query.set('size', params.size.toString());
  }

  const endpoint = `/alerts${query.toString() ? `?${query.toString()}` : ''}`;
  return apiRequest<PageResponse<AlertResponse>>(endpoint);
}

export async function getAlertById(id: string): Promise<AlertResponse> {
  return apiRequest<AlertResponse>(`/alerts/${id}`);
}

export async function updateAlert(id: string, update: UpdateAlertRequest): Promise<AlertResponse> {
  return apiRequest<AlertResponse>(`/alerts/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(update),
  });
}
