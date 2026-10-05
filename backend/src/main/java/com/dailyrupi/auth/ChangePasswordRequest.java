package com.dailyrupi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        // 72 is the longest input BCrypt uses in full.
        @NotBlank @Size(min = 12, max = 72, message = "must be 12 to 72 characters") String newPassword) {
}
