from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, model_validator


class NetworkFlowFeatures(BaseModel):
    """
    Standardized Network Flow Feature Contract based on CIC-IDS2017 schema.
    Explicitly excludes identifiers (src_ip, dst_ip, timestamp, flow_id) to prevent data leakage.
    Robust to null / partial inputs.
    """
    @model_validator(mode="before")
    @classmethod
    def clean_nulls(cls, data: Any) -> Any:
        if isinstance(data, dict):
            return {k: v for k, v in data.items() if v is not None}
        return data
    destination_port: Optional[int] = Field(default=80, ge=0, le=65535, description="Destination TCP/UDP port")
    flow_duration: Optional[float] = Field(default=1000.0, ge=0.0, description="Flow duration in microseconds")
    total_fwd_packets: Optional[int] = Field(default=5, ge=0, description="Total packets in forward direction")
    total_bwd_packets: Optional[int] = Field(default=5, ge=0, description="Total packets in backward direction")
    total_length_fwd_packets: Optional[float] = Field(default=250.0, ge=0.0, description="Total length of forward packets in bytes")
    total_length_bwd_packets: Optional[float] = Field(default=1200.0, ge=0.0, description="Total length of backward packets in bytes")
    fwd_packet_length_max: Optional[float] = Field(default=150.0, ge=0.0)
    fwd_packet_length_min: Optional[float] = Field(default=0.0, ge=0.0)
    fwd_packet_length_mean: Optional[float] = Field(default=50.0, ge=0.0)
    fwd_packet_length_std: Optional[float] = Field(default=10.0, ge=0.0)
    bwd_packet_length_max: Optional[float] = Field(default=500.0, ge=0.0)
    bwd_packet_length_min: Optional[float] = Field(default=0.0, ge=0.0)
    bwd_packet_length_mean: Optional[float] = Field(default=240.0, ge=0.0)
    bwd_packet_length_std: Optional[float] = Field(default=50.0, ge=0.0)
    flow_bytes_per_s: Optional[float] = Field(default=1450.0, ge=0.0, description="Throughput in bytes per second")
    flow_packets_per_s: Optional[float] = Field(default=10.0, ge=0.0, description="Packet rate in packets per second")
    flow_iat_mean: Optional[float] = Field(default=100.0, ge=0.0, description="Mean inter-arrival time in microseconds")
    flow_iat_std: Optional[float] = Field(default=20.0, ge=0.0)
    flow_iat_max: Optional[float] = Field(default=200.0, ge=0.0)
    flow_iat_min: Optional[float] = Field(default=1.0, ge=0.0)
    fwd_iat_total: Optional[float] = Field(default=500.0, ge=0.0)
    bwd_iat_total: Optional[float] = Field(default=500.0, ge=0.0)
    fwd_psh_flags: Optional[int] = Field(default=0, ge=0, le=1)
    bwd_psh_flags: Optional[int] = Field(default=0, ge=0, le=1)
    fwd_urg_flags: Optional[int] = Field(default=0, ge=0, le=1)
    bwd_urg_flags: Optional[int] = Field(default=0, ge=0, le=1)
    fin_flag_count: Optional[int] = Field(default=0, ge=0)
    syn_flag_count: Optional[int] = Field(default=1, ge=0)
    rst_flag_count: Optional[int] = Field(default=0, ge=0)
    psh_flag_count: Optional[int] = Field(default=1, ge=0)
    ack_flag_count: Optional[int] = Field(default=1, ge=0)
    urg_flag_count: Optional[int] = Field(default=0, ge=0)
    down_up_ratio: Optional[float] = Field(default=1.0, ge=0.0)
    average_packet_size: Optional[float] = Field(default=145.0, ge=0.0)
    init_win_bytes_forward: Optional[int] = Field(default=29200, ge=-1)
    init_win_bytes_backward: Optional[int] = Field(default=28960, ge=-1)


class ScoreResponse(BaseModel):
    model_name: str
    model_version: str
    prediction: str
    is_threat: bool
    risk_score: int = Field(..., ge=0, le=100, description="Calibrated risk score 0-100")
    confidence: float = Field(..., ge=0.0, le=1.0)
    class_probabilities: Dict[str, float]
    explanation: str
    model_status: str = "active"


class BatchScoreRequest(BaseModel):
    flows: List[NetworkFlowFeatures]


class BatchScoreResponse(BaseModel):
    model_name: str
    model_version: str
    results: List[ScoreResponse]


class IncidentSummaryRequest(BaseModel):
    incident_id: str
    title: str
    severity: str
    description: Optional[str] = None
    alert_count: int = 1
    sample_alert_titles: List[str] = []
    source_ips: List[str] = []
    destination_ips: List[str] = []


class IncidentSummaryResponse(BaseModel):
    incident_id: str
    ai_provider: str
    model_name: str
    summary_markdown: str
    suggested_actions: List[str]
    disclaimer: str = (
        "AI-generated analysis (experimental). Findings are derived from ingested telemetry "
        "and do not constitute definitive deterministic proof or automated containment actions."
    )


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: Optional[str] = None
    model_version: Optional[str] = None
    classes: List[str] = []
    ollama_connected: bool = False
