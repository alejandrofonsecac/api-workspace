package com.workspace.api.goals.repository;

import com.workspace.api.goals.entity.GoalFocus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalFocusRepository extends JpaRepository<GoalFocus, com.workspace.api.goals.entity.GoalFocusId> {
    java.util.Optional<GoalFocus> findByUserIdAndWeekId(String userId, String weekId);
}
