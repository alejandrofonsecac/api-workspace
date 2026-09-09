package com.workspace.api.goals.service;

import com.workspace.api.goals.repository.GoalsRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GoalsInitialization implements ApplicationRunner {
    private final GoalsRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments arguments) {
        // Assign legacy goals once; subsequent reads must not move them to another week.
        repository.assignLegacyWeek(GoalWeeks.id(LocalDate.now(clock)));
    }
}
