import { SecurityEventResponse, EventSearchParams, IngestEventPayload } from '../types/events';
import { PageResponse } from '../types/pagination';
import { apiRequest } from './apiClient';

export async function searchEvents(params: EventSearchParams = {}): Promise<PageResponse<SecurityEventResponse>> {
  const query = new URLSearchParams();

  if (params.eventType && params.eventType.trim()) {
    query.set('eventType', params.eventType.trim());
  }
  if (params.severity && params.severity !== 'ALL') {
    query.set('severity', params.severity);
  }
  if (params.sourceType && params.sourceType !== 'ALL') {
    query.set('sourceType', params.sourceType);
  }
  if (params.page !== undefined) {
    query.set('page', params.page.toString());
  }
  if (params.size !== undefined) {
    query.set('size', params.size.toString());
  }

  const endpoint = `/events${query.toString() ? `?${query.toString()}` : ''}`;
  return apiRequest<PageResponse<SecurityEventResponse>>(endpoint);
}

export async function getEventById(id: string): Promise<SecurityEventResponse> {
  return apiRequest<SecurityEventResponse>(`/events/${id}`);
}

export async function ingestEvent(payload: IngestEventPayload): Promise<SecurityEventResponse> {
  return apiRequest<SecurityEventResponse>('/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
