import {
  IncidentResponse,
  IncidentSearchParams,
  CreateIncidentRequest,
  CreateIncidentFromAlertRequest,
  UpdateIncidentRequest,
  IncidentNoteResponse,
  IncidentTimelineResponse,
} from '../types/incident';
import { PageResponse } from '../types/pagination';
import { apiRequest } from './apiClient';

export async function searchIncidents(params: IncidentSearchParams = {}): Promise<PageResponse<IncidentResponse>> {
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

  const endpoint = `/incidents${query.toString() ? `?${query.toString()}` : ''}`;
  return apiRequest<PageResponse<IncidentResponse>>(endpoint);
}

export async function getIncidentById(id: string): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/incidents/${id}`);
}

export async function createIncident(data: CreateIncidentRequest): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>('/incidents', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function createIncidentFromAlert(alertId: string, data?: CreateIncidentFromAlertRequest): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/alerts/${alertId}/incidents`, {
    method: 'POST',
    body: JSON.stringify(data || {}),
  });
}

export async function updateIncident(id: string, update: UpdateIncidentRequest): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/incidents/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(update),
  });
}

export async function addIncidentNote(id: string, body: string): Promise<IncidentNoteResponse> {
  return apiRequest<IncidentNoteResponse>(`/incidents/${id}/notes`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
}

export async function getIncidentNotes(id: string): Promise<IncidentNoteResponse[]> {
  return apiRequest<IncidentNoteResponse[]>(`/incidents/${id}/notes`);
}

export async function getIncidentTimeline(id: string): Promise<IncidentTimelineResponse[]> {
  return apiRequest<IncidentTimelineResponse[]>(`/incidents/${id}/timeline`);
}
