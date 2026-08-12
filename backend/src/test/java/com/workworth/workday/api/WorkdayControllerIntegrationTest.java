package com.workworth.workday.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.workworth.workday.persistence.MealBreakRepository;
import com.workworth.workday.persistence.PartialAbsenceRepository;
import com.workworth.workday.persistence.WorkdayRepository;
import com.workworth.workday.persistence.WorkdayTimeCorrectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {com.workworth.WorkWorthApplication.class, WorkdayControllerIntegrationTest.FixedClockConfiguration.class})
@AutoConfigureMockMvc
@Testcontainers
class WorkdayControllerIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;
 @Autowired MutableClock clock;
 @Autowired WorkdayRepository workdays;
 @Autowired MealBreakRepository mealBreaks;
 @Autowired PartialAbsenceRepository partialAbsences;
 @Autowired WorkdayTimeCorrectionRepository corrections;

 @BeforeEach void resetClock() {
  corrections.deleteAll();
  partialAbsences.deleteAll();
  mealBreaks.deleteAll();
  workdays.deleteAll();
  clock.set(Instant.parse("2026-07-06T08:00:00Z"));
 }

 @Test void returnsProblemDetailForWeekendWorkday() throws Exception {mvc.perform(get("/api/v1/workdays/2026-07-04")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));}

 @Test void returnsEmptyIntervalCollectionsForAWorkdayWithoutIntervals() throws Exception {
  mvc.perform(get("/api/v1/workdays/current"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ACTIVE"))
          .andExpect(jsonPath("$.economicSeconds").isNumber())
          .andExpect(jsonPath("$.mealBreaks").isArray())
          .andExpect(jsonPath("$.mealBreaks").isEmpty())
          .andExpect(jsonPath("$.partialAbsences").isArray())
          .andExpect(jsonPath("$.partialAbsences").isEmpty());
 }

 @Test void recoversOnlyTheRequestedWorkdayIntervalsIncludingAnOpenMealBreak() throws Exception {
  String startResponse = mvc.perform(post("/api/v1/workdays/2026-07-06/meal-breaks/start"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").isNumber())
          .andExpect(jsonPath("$.startedAt").value("2026-07-06T08:00:00Z"))
          .andExpect(jsonPath("$.endedAt").value(nullValue()))
          .andReturn().getResponse().getContentAsString();
  Number mealBreakId = JsonPath.read(startResponse, "$.id");

  mvc.perform(get("/api/v1/workdays/current"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ON_MEAL_BREAK"))
          .andExpect(jsonPath("$.mealBreaks[0].id").value(mealBreakId.longValue()))
          .andExpect(jsonPath("$.mealBreaks[0].startedAt").value("2026-07-06T08:00:00Z"))
          .andExpect(jsonPath("$.mealBreaks[0].endedAt").value(nullValue()))
          .andExpect(jsonPath("$.partialAbsences").isEmpty());

  clock.set(Instant.parse("2026-07-06T08:15:00Z"));
  mvc.perform(post("/api/v1/workdays/2026-07-06/meal-breaks/{id}/end", mealBreakId.longValue()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.endedAt").value("2026-07-06T08:15:00Z"));

  mvc.perform(post("/api/v1/workdays/2026-07-06/partial-absences")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"startedAt\":\"2026-07-06T09:00:00Z\",\"endedAt\":\"2026-07-06T10:00:00Z\",\"reason\":\"Medical appointment\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reason").value("Medical appointment"));

  mvc.perform(get("/api/v1/workdays/current"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ACTIVE"))
          .andExpect(jsonPath("$.mealBreaks[0].id").value(mealBreakId.longValue()))
          .andExpect(jsonPath("$.mealBreaks[0].endedAt").value("2026-07-06T08:15:00Z"))
          .andExpect(jsonPath("$.partialAbsences[0].startedAt").value("2026-07-06T09:00:00Z"))
          .andExpect(jsonPath("$.partialAbsences[0].endedAt").value("2026-07-06T10:00:00Z"))
          .andExpect(jsonPath("$.partialAbsences[0].reason").value("Medical appointment"));

  mvc.perform(get("/api/v1/workdays/2026-07-07"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.mealBreaks").isEmpty())
          .andExpect(jsonPath("$.partialAbsences").isEmpty());
 }

 @Test void keepsExistingPauseAbsenceAndCancellationEndpointsAndAllowsPutFromAngular() throws Exception {
  mvc.perform(post("/api/v1/workdays/2026-07-06/partial-absences")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"startedAt\":\"2026-07-06T09:00:00Z\",\"endedAt\":\"2026-07-06T10:00:00Z\"}"))
          .andExpect(status().isOk());
  mvc.perform(post("/api/v1/workdays/2026-07-07/cancel"))
          .andExpect(status().isNoContent());
  mvc.perform(options("/api/v1/workdays/2026-07-06/meal-breaks/1")
                  .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                  .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
          .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
          .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
          .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")));
 }

 @TestConfiguration
 static class FixedClockConfiguration {
  @Bean("testClock") @Primary MutableClock testClock() { return new MutableClock(Instant.parse("2026-07-06T08:00:00Z"), ZoneId.of("Europe/Madrid")); }
 }

 static class MutableClock extends Clock {
  private final AtomicReference<Instant> instant;
  private final ZoneId zone;
  MutableClock(Instant initial, ZoneId zone) { instant = new AtomicReference<>(initial); this.zone = zone; }
  void set(Instant value) { instant.set(value); }
  @Override public ZoneId getZone() { return zone; }
  @Override public Clock withZone(ZoneId requestedZone) { return new MutableClock(instant.get(), requestedZone); }
  @Override public Instant instant() { return instant.get(); }
 }
}
