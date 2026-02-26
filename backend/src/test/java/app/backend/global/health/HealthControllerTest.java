package app.backend.global.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

	private final HealthController healthController = new HealthController();

	@Test
	void returnsApiResponseFormat() {
		var response = healthController.health();

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isNotNull();
		assertThat(response.data().get("status")).isEqualTo("ok");
		assertThat(response.error()).isNull();
	}
}
