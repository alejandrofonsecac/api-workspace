package com.workspace.api.pages.planner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record PlannerAvailabilityResponseDTO(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        int totalBlocks,
        int occupiedBlocks,
        int availableBlocks,
        int availableHours,
        List<PlannerSlotDTO> slots
) {
}
