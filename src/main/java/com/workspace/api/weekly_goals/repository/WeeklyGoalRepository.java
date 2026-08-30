package com.workspace.api.weekly_goals.repository;

import com.workspace.api.weekly_goals.entity.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Integer> {
}
