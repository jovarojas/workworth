package com.workworth.dashboard.api;

import com.workworth.dashboard.api.dto.DashboardMotivationResponse;
import com.workworth.dashboard.application.DashboardMotivationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardMotivationService motivation;

    public DashboardController(DashboardMotivationService motivation) {
        this.motivation = motivation;
    }

    @GetMapping("/motivation")
    public ResponseEntity<DashboardMotivationResponse> motivation() {
        return ResponseEntity.ok(DashboardMotivationResponse.from(motivation.motivation()));
    }
}
