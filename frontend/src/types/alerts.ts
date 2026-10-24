export type AlertStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';
export type AlertSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface AlertResponse {
  id: string;
  tenantId: string;
  ruleKey: string;
  ruleVersion: string;
  title: string;
  description: string;
  severity: AlertSeverity;
  confidence?: number | null;
  status: AlertStatus;
  firstSeenAt: string;
  lastSeenAt: string;
  eventCount: number;
  assignedTo?: string | null;
  deduplicationKey: string;
  supportingEventIds: string[];
}

export interface AlertSearchParams {
  severity?: string;
  status?: AlertStatus | 'ALL';
  page?: number;
  size?: number;
}

export interface UpdateAlertRequest {
  status?: AlertStatus;
  assignedTo?: string | null;
}
