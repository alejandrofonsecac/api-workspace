package com.workspace.api.retention;

import com.workspace.api.pages.event.controller.EventController;
import com.workspace.api.pages.event.service.EventService;
import com.workspace.api.pages.goals.service.GoalsConfiguration;
import com.workspace.api.pages.event.service.CalendarQueryService;
import com.workspace.api.pages.note.controller.NoteController;
import com.workspace.api.pages.note.service.NoteService;
import com.workspace.api.pages.event.controller.CalendarController;
import com.workspace.api.pages.planner.controller.PlannerExceptionHandler;
import com.workspace.api.pages.planner.controller.PlannerController;
import com.workspace.api.pages.planner.service.PlannerService;
import com.workspace.api.retention.repository.RetentionRepository;
import com.workspace.api.retention.service.RetentionBatchService;
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
@Import({PlannerExceptionHandler.class, CalendarController.class, CalendarQueryService.class, EventController.class, EventService.class, GoalsConfiguration.class,
        NoteController.class, NoteService.class, PlannerController.class, PlannerService.class,
        CurrentUser.class, RefreshCookie.class,
        JwtConfiguration.class, SecurityConfiguration.class,
        RetentionRepository.class, RetentionBatchService.class, RetentionConfiguration.class})
class RetentionTestConfiguration {
}
