package app.backend.global.error;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

	@Test
	void handlesApiExceptionWithErrorCodeStatus() {
		var response = globalExceptionHandler.handleApiException(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().error()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("MEMBER_NOT_FOUND");
	}

	@Test
	void handlesValidationExceptionAsInvalidInput() {
		var response = globalExceptionHandler.handleValidationException(new BindException(new Object(), "request"));

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().error()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("INVALID_INPUT");
	}

	@Test
	void handlesUnexpectedExceptionAsInternalError() {
		var response = globalExceptionHandler.handleUnexpectedException(new RuntimeException("boom"));

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().error()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
	}
}
