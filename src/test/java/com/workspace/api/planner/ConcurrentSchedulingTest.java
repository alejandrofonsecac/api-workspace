package com.workspace.api.planner;

import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.event.service.EventService;
import com.workspace.api.event.repository.EventRepository;
import com.workspace.api.planner.exception.OccupiedPlannerSlotException;
import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentSchedulingTest {
    @Autowired UserRepository users;
    @Autowired EventService events;
    @Autowired EventRepository repository;

    @Test
    void onlyOneConcurrentWriterCanReserveTheSameEmptySlot() throws Exception {
        User account = new User();
        account.setName("Concurrent test");
        account.setEmail(account.getId() + "@example.com");
        account.setPasswordHash("test");
        users.saveAndFlush(account);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> reserve = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(account.getId(), null, List.of()));
            try {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Missing start signal");
                events.create(new EventRequestDTO("Reservation", LocalDate.of(2030, 1, 1),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, EventSource.internal, EventCategory.work));
                return "saved";
            } catch (OccupiedPlannerSlotException expected) {
                return "conflict";
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(reserve);
            var second = executor.submit(reserve);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("saved", "conflict");
            assertThat(repository.findAllByUserId(account.getId())).hasSize(1);
        }
    }
}
