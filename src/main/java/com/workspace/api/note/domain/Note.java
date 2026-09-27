package com.workspace.api.note.domain;

import com.workspace.api.retention.domain.RetainedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "tb_note")
@Entity
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Note extends RetainedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDateTime edited;
    private Boolean favourite = false;
    private Boolean fixed   = false;
    private LocalDateTime fixedAt;
}
