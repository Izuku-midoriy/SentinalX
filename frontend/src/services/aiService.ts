import { apiRequest } from './apiClient';

export interface AiStatusResponse {
  online: boolean;
  service_url: string;
  model_loaded: boolean;
  model_name: string;
  model_version: string;
  classes: string[];
  ollama_connected: boolean;
  message: string;
}

export interface FlowScoreRequest {
  destination_port?: number;
  flow_duration?: number;
  total_fwd_packets?: number;
  total_bwd_packets?: number;
  total_length_fwd_packets?: number;
  total_length_bwd_packets?: number;
  flow_bytes_per_s?: number;
  flow_packets_per_s?: number;
  syn_flag_count?: number;
  ack_flag_count?: number;
  rst_flag_count?: number;
  psh_flag_count?: number;
}

export interface FlowScoreResponse {
  model_name: string;
  model_version: string;
  prediction: string;
  is_threat: boolean;
  risk_score: number;
  confidence: number;
  class_probabilities: Record<string, number>;
  explanation: string;
  model_status: string;
}

export interface IncidentSummaryResponse {
  incident_id: string;
  ai_provider: string;
  model_name: string;
  summary_markdown: string;
  suggested_actions: string[];
  disclaimer: string;
}

export const aiService = {
  async getStatus(): Promise<AiStatusResponse> {
    return apiRequest<AiStatusResponse>('/ai/status');
  },

  async scoreFlow(request?: FlowScoreRequest): Promise<FlowScoreResponse> {
    return apiRequest<FlowScoreResponse>('/ai/score-flow', {
      method: 'POST',
      body: JSON.stringify(request || {})
    });
  },

  async summarizeIncident(incidentId: string): Promise<IncidentSummaryResponse> {
    return apiRequest<IncidentSummaryResponse>(`/ai/incidents/${incidentId}/summarize`, {
      method: 'POST'
    });
  }
};
