package com.workworth.goals.api;

import com.workworth.goals.api.dto.CreateGoalRequest;
import com.workworth.goals.api.dto.GoalResponse;
import com.workworth.goals.api.dto.UpdateGoalRequest;
import com.workworth.goals.application.GoalService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalsController {

    private final GoalService goals;

    public GoalsController(GoalService goals) {
        this.goals = goals;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        var goal = goals.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GoalResponse.from(goal, goals.progress(goal)));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> active() {
        return ResponseEntity.ok(goals.active().stream().map(goal -> GoalResponse.from(goal, goals.progress(goal))).toList());
    }

    @GetMapping("/history")
    public ResponseEntity<List<GoalResponse>> history() {
        return ResponseEntity.ok(goals.history().stream().map(goal -> GoalResponse.from(goal, null)).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> get(@PathVariable Long id) {
        var goal = goals.get(id);
        return ResponseEntity.ok(GoalResponse.from(goal, goals.progress(goal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateGoalRequest request) {
        var goal = goals.update(id, request);
        return ResponseEntity.ok(GoalResponse.from(goal, goals.progress(goal)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<GoalResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(GoalResponse.from(goals.complete(id), null));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<GoalResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(GoalResponse.from(goals.cancel(id), null));
    }
}
