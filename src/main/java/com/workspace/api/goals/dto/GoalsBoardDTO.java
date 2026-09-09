package com.workspace.api.goals.dto;

import java.time.LocalDate;
import java.util.List;

public record GoalsBoardDTO(Week week,
                            List<GoalsResponseDTO> goals,
                            Summary summary,
                            GoalFocusDTO focus,
                            GoalsRequestDTO defaults,
                            Options options) {

    public record Week(String id, String previousId, String nextId, String currentId,
                       LocalDate start, LocalDate end, boolean current) {}

    public record Option(String value, String label) {}

    public record Summary(int completed, int total, int percentage) {}

    public record Options(List<Option> priorities, List<Option> categories) {}
}
