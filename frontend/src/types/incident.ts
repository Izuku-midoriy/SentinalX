export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'CONTAINED' | 'RESOLVED' | 'CLOSED';
export type IncidentSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface IncidentResponse {
  id: string;
  tenantId: string;
  title: string;
  description: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  assignedTo?: string | null;
  assignedToEmail?: string | null;
  createdBy: string;
  createdByEmail: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string | null;
  linkedAlertIds: string[];
}

export interface IncidentNoteResponse {
  id: string;
  incidentId: string;
  authorId: string;
  authorEmail: string;
  body: string;
  createdAt: string;
}

export interface IncidentTimelineResponse {
  id: string;
  incidentId: string;
  actorId?: string | null;
  actorEmail?: string | null;
  eventType: string;
  summary: string;
  details?: string | null;
  timestamp: string;
}

export interface CreateIncidentRequest {
  title: string;
  description?: string;
  severity?: IncidentSeverity;
  assignedTo?: string | null;
  assignedToEmail?: string | null;
  alertIds?: string[];
}

export interface CreateIncidentFromAlertRequest {
  title?: string;
  description?: string;
  severity?: IncidentSeverity;
  assignedTo?: string | null;
  assignedToEmail?: string | null;
}

export interface UpdateIncidentRequest {
  title?: string;
  description?: string;
  severity?: IncidentSeverity;
  status?: IncidentStatus;
  assignedTo?: string | null;
  assignedToEmail?: string | null;
}

export interface IncidentSearchParams {
  status?: IncidentStatus | 'ALL';
  severity?: string;
  page?: number;
  size?: number;
}
