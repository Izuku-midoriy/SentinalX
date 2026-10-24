package com.sentinelx.backend.detection.repository;

import com.sentinelx.backend.detection.entity.DetectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DetectionRuleRepository extends JpaRepository<DetectionRule, UUID> {
    Optional<DetectionRule> findByRuleKey(String ruleKey);
    List<DetectionRule> findByEnabledTrue();
}
