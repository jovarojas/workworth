package com.workworth.rewards.api;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.api.dto.CreateRewardRequest;
import com.workworth.rewards.api.dto.RewardCombinationResponse;
import com.workworth.rewards.api.dto.RewardEvaluationResponse;
import com.workworth.rewards.api.dto.RewardRelevanceResponse;
import com.workworth.rewards.api.dto.RewardResponse;
import com.workworth.rewards.api.dto.UpdateRewardRequest;
import com.workworth.rewards.application.RewardCombinationService;
import com.workworth.rewards.application.RewardEvaluationService;
import com.workworth.rewards.application.RewardService;
import com.workworth.rewards.domain.RewardStatus;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardsController {

    private final RewardService rewards;
    private final RewardEvaluationService evaluations;
    private final RewardCombinationService combinations;

    public RewardsController(RewardService rewards, RewardEvaluationService evaluations,
                             RewardCombinationService combinations) {
        this.rewards = rewards;
        this.evaluations = evaluations;
        this.combinations = combinations;
    }

    @PostMapping
    public ResponseEntity<RewardResponse> create(@Valid @RequestBody CreateRewardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RewardResponse.from(rewards.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<RewardResponse>> list(@RequestParam(required = false) RewardStatus status) {
        return ResponseEntity.ok(rewards.list(status).stream().map(RewardResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RewardResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(RewardResponse.from(rewards.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRewardRequest request) {
        return ResponseEntity.ok(RewardResponse.from(rewards.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rewards.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/acquire")
    public ResponseEntity<RewardResponse> acquire(@PathVariable Long id) {
        return ResponseEntity.ok(RewardResponse.from(rewards.acquire(id)));
    }

    @GetMapping("/{id}/evaluations/{context}")
    public ResponseEntity<RewardEvaluationResponse> evaluate(@PathVariable Long id, @PathVariable EarningPeriod context) {
        return ResponseEntity.ok(RewardEvaluationResponse.from(evaluations.evaluate(id, context)));
    }

    @PostMapping("/{id}/relevance")
    public ResponseEntity<RewardRelevanceResponse> relevance(@PathVariable Long id) {
        return ResponseEntity.ok(RewardRelevanceResponse.from(evaluations.relevance(id)));
    }

    @GetMapping("/combinations/{context}")
    public ResponseEntity<RewardCombinationResponse> combination(@PathVariable EarningPeriod context,
        @RequestParam(required = false) Set<Long> excludeRewardIds) {
        return ResponseEntity.ok(RewardCombinationResponse.from(
            combinations.combination(context, excludeRewardIds == null ? Set.of() : excludeRewardIds)));
    }
}
