package com.workspace.api.retention;

import com.workspace.api.event.controller.EventController;
import com.workspace.api.event.service.EventService;
import com.workspace.api.goals.service.GoalsConfiguration;
import com.workspace.api.note.controller.NoteController;
import com.workspace.api.note.service.NoteService;
import com.workspace.api.planner.controller.PlannerController;
import com.workspace.api.planner.service.PlannerService;
import com.workspace.api.retention.repository.RetentionRepository;
import com.workspace.api.retention.service.RetentionBatchService;
import com.workspace.api.schedule.tasks.controller.TasksController;
import com.workspace.api.schedule.tasks.service.TasksService;
import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.user.security.RefreshCookie;
import com.workspace.api.user.security.config.JwtConfiguration;
import com.workspace.api.user.security.config.SecurityConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Explicit composition avoids unrelated duplicate legacy security components in the
// application's current component scan, while exercising real services and security.
@Configuration(proxyBeanMethods = false)
@Profile("retention-integration")
@EnableAutoConfiguration
@EntityScan("com.workspace.api")
@EnableJpaRepositories("com.workspace.api")
@Import({EventController.class, EventService.class, GoalsConfiguration.class,
        NoteController.class, NoteService.class, PlannerController.class, PlannerService.class,
        TasksController.class, TasksService.class, CurrentUser.class, RefreshCookie.class,
        JwtConfiguration.class, SecurityConfiguration.class,
        RetentionRepository.class, RetentionBatchService.class, RetentionConfiguration.class})
class RetentionTestConfiguration {
}
