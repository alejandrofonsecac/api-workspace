package com.workspace.api.goals.repository;

import com.workspace.api.goals.entity.Goals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalsRepository extends JpaRepository<Goals, Integer> {}