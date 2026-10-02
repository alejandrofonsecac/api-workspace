package com.workspace.api.retention;

import com.workspace.api.retention.service.RetentionBatchService;
import com.workspace.api.retention.service.RetentionJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RetentionSchedulingTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RetentionConfiguration.class, RetentionJob.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(RetentionBatchService.class, () -> mock(RetentionBatchService.class));

    @Test
    void registersTheDailyJobWithDefaultConfiguration() {
        context.run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(RetentionJob.class);
            assertThat(application.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
        });
    }

    @Test
    void disablesMaintenanceByConfiguration() {
        context.withPropertyValues("app.retention.enabled=false").run(application -> {
            assertThat(application).hasNotFailed().doesNotHaveBean(RetentionJob.class);
            assertThat(application.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
        });
    }
}
