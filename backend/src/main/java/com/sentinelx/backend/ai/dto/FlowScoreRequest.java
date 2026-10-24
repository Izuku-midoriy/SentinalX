package com.sentinelx.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record FlowScoreRequest(
        @JsonProperty("destination_port") Integer destinationPort,
        @JsonProperty("flow_duration") Double flowDuration,
        @JsonProperty("total_fwd_packets") Integer totalFwdPackets,
        @JsonProperty("total_bwd_packets") Integer totalBwdPackets,
        @JsonProperty("total_length_fwd_packets") Double totalLengthFwdPackets,
        @JsonProperty("total_length_bwd_packets") Double totalLengthBwdPackets,
        @JsonProperty("flow_bytes_per_s") Double flowBytesPerS,
        @JsonProperty("flow_packets_per_s") Double flowPacketsPerS,
        @JsonProperty("syn_flag_count") Integer synFlagCount,
        @JsonProperty("ack_flag_count") Integer ackFlagCount,
        @JsonProperty("rst_flag_count") Integer rstFlagCount,
        @JsonProperty("psh_flag_count") Integer pshFlagCount
) {
    public static FlowScoreRequest defaults() {
        return new FlowScoreRequest(80, 1000.0, 5, 5, 250.0, 1200.0, 1450.0, 10.0, 1, 1, 0, 1);
    }
}
