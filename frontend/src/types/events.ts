export type EventSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface SecurityEventResponse {
  id: string;
  tenantId: string;
  eventTime: string;
  ingestedAt: string;
  sourceType: string;
  sourceName: string;
  eventType: string;
  severity: EventSeverity;
  host?: string | null;
  sourceIp?: string | null;
  destinationIp?: string | null;
  username?: string | null;
  processName?: string | null;
  processId?: number | null;
  message?: string | null;
  rawEvent?: string | null;
  schemaVersion: string;
  idempotencyKey?: string | null;
  duplicate: boolean;
}

export interface EventSearchParams {
  eventType?: string;
  severity?: string;
  sourceType?: string;
  page?: number;
  size?: number;
}

export interface IngestEventPayload {
  timestamp: string;
  sourceType: string;
  sourceName: string;
  eventType: string;
  severity: EventSeverity;
  host?: string;
  sourceIp?: string;
  destinationIp?: string;
  username?: string;
  processName?: string;
  processId?: number;
  message?: string;
  rawEvent?: string;
  idempotencyKey?: string;
}
