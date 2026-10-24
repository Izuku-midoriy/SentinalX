import { HealthResponse } from '../types/health';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export async function fetchHealth(): Promise<{ data: HealthResponse; latencyMs: number }> {
  const startTime = performance.now();
  const response = await fetch(`${API_BASE_URL}/health`, {
    headers: {
      'Accept': 'application/json',
    },
  });

  const latencyMs = Math.round(performance.now() - startTime);

  if (!response.ok) {
    throw new Error(`Health check failed with HTTP ${response.status}: ${response.statusText}`);
  }

  const data: HealthResponse = await response.json();
  return { data, latencyMs };
}
