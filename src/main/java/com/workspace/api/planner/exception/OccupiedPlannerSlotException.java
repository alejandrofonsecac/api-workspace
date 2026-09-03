package com.workspace.api.planner.exception;

public class OccupiedPlannerSlotException extends RuntimeException {
    public OccupiedPlannerSlotException(String message) {
        super(message);
    }
}
