package com.workworth.dashboard.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.dashboard.application.DashboardMotivation;
import com.workworth.dashboard.application.DashboardMotivationService;
import com.workworth.dashboard.application.DashboardMotivationState;
import com.workworth.dashboard.application.DashboardPrimaryReward;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.identity.application.TestUsers;
import com.workworth.rewards.application.RewardEvaluation;
import com.workworth.rewards.domain.RewardOutcome;
import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardMotivationService motivation;

    @Test
    void exposesTheDashboardMotivationContractOverHttp() throws Exception {
        Reward reward = new Reward(TestUsers.user("test|dashboard-controller"), "Auriculares", 1,
            new BigDecimal("120.00"), "EUR", Instant.EPOCH);
        ReflectionTestUtils.setField(reward, "id", 12L);
        RewardEvaluation evaluation = new RewardEvaluation(12L, EarningPeriod.MONTH, true,
            RewardOutcome.AFFORDABLE, new BigDecimal("120.00"), new BigDecimal("120.00"), "EUR",
            BigDecimal.ZERO, null);
        when(motivation.motivation()).thenReturn(new DashboardMotivation(DashboardMotivationState.AVAILABLE,
            new DashboardPrimaryReward(reward, evaluation), null));

        mockMvc.perform(get("/api/v1/dashboard/motivation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("AVAILABLE"))
            .andExpect(jsonPath("$.primaryReward.reward.id").value(12))
            .andExpect(jsonPath("$.primaryReward.relevantContext").value("MONTH"))
            .andExpect(jsonPath("$.primaryReward.progressContext").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.primaryReward.outcome").value("AFFORDABLE"))
            .andExpect(jsonPath("$.primaryReward.surplus").value(0))
            .andExpect(jsonPath("$.combination").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void exposesAnEmptyMotivationWithoutRewardOrCombination() throws Exception {
        when(motivation.motivation()).thenReturn(new DashboardMotivation(DashboardMotivationState.EMPTY, null, null));

        mockMvc.perform(get("/api/v1/dashboard/motivation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("EMPTY"))
            .andExpect(jsonPath("$.primaryReward").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.combination").value(org.hamcrest.Matchers.nullValue()));
    }
}
