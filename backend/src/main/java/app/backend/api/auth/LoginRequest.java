package app.backend.api.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank String loginId,
		@NotBlank String password
) {
}
