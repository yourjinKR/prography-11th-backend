package app.backend.api;

import app.backend.domain.cohort.CohortRepository;
import app.backend.domain.member.CohortMember;
import app.backend.domain.member.CohortMemberRepository;
import app.backend.domain.session.QrCodeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class BonusAttendanceApiTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private QrCodeRepository qrCodeRepository;

	@Autowired
	private CohortRepository cohortRepository;

	@Autowired
	private CohortMemberRepository cohortMemberRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void bonusApis_memberCheckInAndMemberViews() throws Exception {
		long sessionId = createSession("Bonus CheckIn Session", "2099-01-01", "10:00:00");
		setSessionInProgress(sessionId);
		String qrHash = getActiveQrHash(sessionId);

		mockMvc.perform(post("/api/v1/attendances")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "hashValue": "%s",
								  "memberId": 1
								}
								""".formatted(qrHash)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.sessionId").value(sessionId))
				.andExpect(jsonPath("$.data.memberId").value(1));

		mockMvc.perform(post("/api/v1/attendances")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "hashValue": "%s",
								  "memberId": 1
								}
								""".formatted(qrHash)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("ATTENDANCE_ALREADY_CHECKED"));

		mockMvc.perform(get("/api/v1/attendances").param("memberId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].sessionId").value(sessionId));

		mockMvc.perform(get("/api/v1/members/{memberId}/attendance-summary", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value(1))
				.andExpect(jsonPath("$.data.present").value(1));
	}

	@Test
	void bonusApis_adminAttendanceFlowsAndViews() throws Exception {
		long sessionId = createSession("Bonus Admin Session", "2026-05-01", "10:00:00");

		MvcResult registerResult = mockMvc.perform(post("/api/v1/admin/attendances")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": %d,
								  "memberId": 1,
								  "status": "ABSENT",
								  "reason": "No show"
								}
								""".formatted(sessionId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("ABSENT"))
				.andExpect(jsonPath("$.data.penaltyAmount").value(10000))
				.andReturn();

		long attendanceId = extractDataId(registerResult);

		mockMvc.perform(put("/api/v1/admin/attendances/{id}", attendanceId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "EXCUSED",
								  "reason": "Sick leave"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("EXCUSED"))
				.andExpect(jsonPath("$.data.penaltyAmount").value(0));

		mockMvc.perform(get("/api/v1/admin/attendances/sessions/{sessionId}/summary", sessionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].memberId").exists());

		mockMvc.perform(get("/api/v1/admin/attendances/members/{memberId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value(1))
				.andExpect(jsonPath("$.data.attendances.length()").value(1));

		mockMvc.perform(get("/api/v1/admin/attendances/sessions/{sessionId}", sessionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.sessionId").value(sessionId))
				.andExpect(jsonPath("$.data.attendances.length()").value(1));

		long cohortMemberId = currentCohortMemberId(1L);
		mockMvc.perform(get("/api/v1/admin/cohort-members/{cohortMemberId}/deposits", cohortMemberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.length()").value(3)) // INITIAL + PENALTY + REFUND
				.andExpect(jsonPath("$.data[1].type").value("PENALTY"))
				.andExpect(jsonPath("$.data[2].type").value("REFUND"));
	}

	@Test
	void bonusApis_excuseLimitExceededAndDepositInsufficient() throws Exception {
		for (int i = 0; i < 3; i++) {
			long sessionId = createSession("Excused Session " + i, "2026-06-0" + (i + 1), "10:00:00");
			mockMvc.perform(post("/api/v1/admin/attendances")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "sessionId": %d,
									  "memberId": 1,
									  "status": "EXCUSED",
									  "reason": "Personal"
									}
									""".formatted(sessionId)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true));
		}

		long overSessionId = createSession("Excused Session Over", "2026-06-10", "10:00:00");
		mockMvc.perform(post("/api/v1/admin/attendances")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": %d,
								  "memberId": 1,
								  "status": "EXCUSED",
								  "reason": "Over limit"
								}
								""".formatted(overSessionId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("EXCUSE_LIMIT_EXCEEDED"));

		CohortMember cohortMember = cohortMemberRepository.findById(currentCohortMemberId(1L)).orElseThrow();
		cohortMember.setDeposit(0);

		long penaltySessionId = createSession("Penalty Session", "2026-06-20", "10:00:00");
		mockMvc.perform(post("/api/v1/admin/attendances")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": %d,
								  "memberId": 1,
								  "status": "ABSENT",
								  "reason": "No show"
								}
								""".formatted(penaltySessionId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("DEPOSIT_INSUFFICIENT"));
	}

	private long createSession(String title, String date, String time) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/admin/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "date": "%s",
								  "time": "%s",
								  "location": "Seoul"
								}
								""".formatted(title, date, time)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return extractDataId(result);
	}

	private void setSessionInProgress(long sessionId) throws Exception {
		mockMvc.perform(put("/api/v1/admin/sessions/{id}", sessionId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "IN_PROGRESS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
	}

	private String getActiveQrHash(long sessionId) {
		return qrCodeRepository.findTopBySessionIdAndExpiresAtAfterOrderByExpiresAtDesc(sessionId, Instant.now())
				.map(qr -> qr.getHashValue())
				.orElseThrow();
	}

	private long currentCohortMemberId(Long memberId) {
		Long cohortId = cohortRepository.findByGeneration(11).orElseThrow().getId();
		return cohortMemberRepository.findByMemberIdAndCohortId(memberId, cohortId).orElseThrow().getId();
	}

	private long extractDataId(MvcResult result) throws Exception {
		String json = result.getResponse().getContentAsString();
		Boolean success = JsonPath.read(json, "$.success");
		assertThat(success).isTrue();
		Number id = JsonPath.read(json, "$.data.id");
		return id.longValue();
	}
}
