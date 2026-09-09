package com.workspace.api.goals.repository;

import com.workspace.api.goals.entity.Goals;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GoalsRepository extends JpaRepository<Goals, Integer> {
    @EntityGraph(attributePaths = "habit")
    List<Goals> findByWeekIdOrderByIdAsc(String weekId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goals g where g.id = :id")
    Optional<Goals> findLockedById(@Param("id") Integer id);

    @Modifying
    @Query("update Goals g set g.weekId = :week where g.weekId is null")
    int assignLegacyWeek(@Param("week") String week);
}
