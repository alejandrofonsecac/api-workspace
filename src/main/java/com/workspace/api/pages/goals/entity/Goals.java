package com.workspace.api.pages.goals.entity;

import com.workspace.api.user.domain.UserOwnedEntity;
import com.workspace.api.util.domain.Category;
import com.workspace.api.util.domain.Priority;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Table(name = "tb_goals")
@Entity
@Data
@lombok.EqualsAndHashCode(callSuper = true)
public class Goals extends UserOwnedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String goalName;
    @Column(nullable = false)
    private String expectedTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 8)
    private String weekId;

    private Instant createdAt;
    private Instant completedAt;

    @Column(columnDefinition = "integer default 0")
    private int deferredCount;

}
