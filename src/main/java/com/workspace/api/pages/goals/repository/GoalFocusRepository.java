package com.workspace.api.pages.goals.repository;

import com.workspace.api.pages.goals.entity.GoalFocus;
import com.workspace.api.pages.goals.entity.GoalFocusId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalFocusRepository extends JpaRepository<GoalFocus, GoalFocusId> {
    java.util.Optional<GoalFocus> findByUserIdAndWeekId(String userId, String weekId);
}
