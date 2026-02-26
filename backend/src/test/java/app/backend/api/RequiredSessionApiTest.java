package app.backend.api;

import app.backend.domain.session.QrCodeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class RequiredSessionApiTest {

	private MockMvc mockMvc;

	@Autowired
	private QrCodeRepository qrCodeRepository;

	@Autowired
	private WebApplicationContext context;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void requiredApis_10_to_14_sessionEndpoints() throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/v1/admin/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Regular Meeting",
								  "date": "2026-03-02",
								  "time": "14:00:00",
								  "location": "Gangnam"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("SCHEDULED"))
				.andReturn();

		long sessionId = extractDataId(createResult);

		mockMvc.perform(get("/api/v1/sessions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].id").exists());

		mockMvc.perform(get("/api/v1/admin/sessions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].attendanceSummary.total").value(0));

		mockMvc.perform(put("/api/v1/admin/sessions/{id}", sessionId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Regular Meeting Updated",
								  "status": "IN_PROGRESS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.data.title").value("Regular Meeting Updated"));

		mockMvc.perform(delete("/api/v1/admin/sessions/{id}", sessionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("CANCELLED"))
				.andExpect(jsonPath("$.data.qrActive").value(false));
	}

	@Test
	void requiredApis_15_16_qrCodeEndpoints() throws Exception {
		MvcResult createSessionResult = mockMvc.perform(post("/api/v1/admin/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "QR Session",
								  "date": "2026-03-03",
								  "time": "15:00:00",
								  "location": "Jamsil"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		long sessionId = extractDataId(createSessionResult);

		qrCodeRepository.findTopBySessionIdAndExpiresAtAfterOrderByExpiresAtDesc(sessionId, Instant.now())
				.ifPresent(qr -> {
					qr.setExpiresAt(Instant.now().minusSeconds(1));
					qrCodeRepository.save(qr);
				});

		MvcResult createQrResult = mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/qrcodes", sessionId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.sessionId").value(sessionId))
				.andReturn();

		long qrCodeId = extractDataId(createQrResult);

		MvcResult renewResult = mockMvc.perform(put("/api/v1/admin/qrcodes/{qrCodeId}", qrCodeId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.sessionId").value(sessionId))
				.andReturn();

		long renewedQrCodeId = extractDataId(renewResult);
		assertThat(renewedQrCodeId).isNotEqualTo(qrCodeId);
	}

	private long extractDataId(MvcResult result) throws Exception {
		String json = result.getResponse().getContentAsString();
		Boolean success = JsonPath.read(json, "$.success");
		assertThat(success).isTrue();
		Number id = JsonPath.read(json, "$.data.id");
		return id.longValue();
	}
}
