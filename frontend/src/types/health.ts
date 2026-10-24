export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
  service: string;
  version: string;
  timestamp: string;
  environment: string;
}

export interface ServiceHealthState {
  isChecking: boolean;
  data: HealthResponse | null;
  error: string | null;
  latencyMs: number | null;
  lastChecked: string | null;
}
