package com.dailyrupi.common;

/** Body of every error response. {@code code} is stable and safe for the frontend to switch on. */
public record ApiError(String code, String message) {
}
