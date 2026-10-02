package com.workspace.api.pages.goals.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalFocusId implements Serializable {
    private String userId;
    private String weekId;
}
