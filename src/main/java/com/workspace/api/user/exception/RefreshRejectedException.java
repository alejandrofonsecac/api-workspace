package com.workspace.api.user.exception;

public class RefreshRejectedException extends RuntimeException {
    public RefreshRejectedException() { super("Refresh session is unavailable"); }
}
