package com.workspace.api.pages.planner.exception;

public class OccupiedPlannerSlotException extends RuntimeException {
    public OccupiedPlannerSlotException(String message) {
        super(message);
    }
}
