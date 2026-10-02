package com.workspace.api.retention.domain;

import com.workspace.api.user.domain.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Shared lifecycle for records eligible for automatic retention cleanup. */
@Getter
@Setter
@MappedSuperclass
public abstract class RetainedEntity extends UserOwnedEntity {
    private Boolean completed = false;
    private Instant completedAt;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(columnDefinition = "TEXT")
    private String recurrenceRule;

    public void updateRetention(Boolean completed, Boolean recurring, String recurrenceRule, Instant now) {
        if (completed != null) {
            if (completed && !Boolean.TRUE.equals(this.completed)) {
                completedAt = now;
            } else if (!completed) {
                completedAt = null;
            }
            this.completed = completed;
        }
        if (recurring != null) this.recurring = recurring;
        if (recurrenceRule != null) this.recurrenceRule = recurrenceRule.isBlank() ? null : recurrenceRule.trim();
    }
}
