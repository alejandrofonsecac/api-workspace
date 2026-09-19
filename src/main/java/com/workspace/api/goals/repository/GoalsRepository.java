package com.workspace.api.goals.repository;

import com.workspace.api.goals.entity.Goals;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GoalsRepository extends JpaRepository<Goals, Integer> {
    List<Goals> findAllByUserId(String userId);
    Optional<Goals> findByIdAndUserId(Integer id, String userId);
    List<Goals> findByUserIdAndWeekIdOrderByIdAsc(String userId, String weekId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goals g where g.id = :id and g.userId = :userId")
    Optional<Goals> findLockedByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);

    @Modifying
    @Query("update Goals g set g.weekId = :week where g.weekId is null")
    void assignLegacyWeek(@Param("week") String week);
}
