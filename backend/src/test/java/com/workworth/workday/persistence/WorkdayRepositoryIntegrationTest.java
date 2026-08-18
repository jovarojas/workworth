package com.workworth.workday.persistence;
import static org.assertj.core.api.Assertions.assertThat;
import com.workworth.workday.domain.ScheduleVariant;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
@DataJpaTest @Testcontainers class WorkdayRepositoryIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired WorkdayRepository repository;
 @Autowired AppUserRepository users;
 @Test void persistsOneWorkdayPerLocalDate(){var date=LocalDate.of(2026,7,6); var user=users.save(new AppUser(UUID.randomUUID(),"test|workday-repository","workday-repository@test.invalid","Europe/Madrid",Instant.EPOCH)); repository.saveAndFlush(new Workday(user,date,"Europe/Madrid",ScheduleVariant.SUMMER,LocalTime.of(8,0),LocalTime.of(15,0),Duration.ofHours(7).getSeconds(),Instant.parse("2026-07-06T06:00:00Z"))); assertThat(repository.findByUserIdAndLocalDate(user.getId(),date)).isPresent();}
}
