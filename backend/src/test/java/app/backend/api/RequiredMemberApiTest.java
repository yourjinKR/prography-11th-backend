package app.backend.api;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class RequiredMemberApiTest {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void requiredApis_01_02_loginAndGetMember() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"admin\",\"password\":\"admin1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.loginId").value("admin"));

		mockMvc.perform(get("/api/v1/members/{id}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.loginId").value("admin"));
	}

	@Test
	void requiredApis_03_to_07_adminMemberCrudAndDashboard() throws Exception {
		String loginId = "member_req_1";

		MvcResult createResult = mockMvc.perform(post("/api/v1/admin/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "loginId": "%s",
								  "password": "password123",
								  "name": "Member One",
								  "phone": "010-1111-1111",
								  "cohortId": 2,
								  "partId": 6,
								  "teamId": 1
								}
								""".formatted(loginId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.loginId").value(loginId))
				.andReturn();

		long memberId = extractDataId(createResult);

		mockMvc.perform(get("/api/v1/admin/members")
						.param("page", "0")
						.param("size", "10")
						.param("searchType", "loginId")
						.param("searchValue", loginId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].loginId").value(loginId));

		mockMvc.perform(get("/api/v1/admin/members/{id}", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(memberId));

		mockMvc.perform(put("/api/v1/admin/members/{id}", memberId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Member Updated",
								  "phone": "010-2222-2222",
								  "partId": 7,
								  "teamId": 2
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.name").value("Member Updated"))
				.andExpect(jsonPath("$.data.partName").value("WEB"))
				.andExpect(jsonPath("$.data.teamName").value("Team B"));

		mockMvc.perform(delete("/api/v1/admin/members/{id}", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
	}

	@Test
	void requiredApi_01_loginFailsWithWrongPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"admin\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
	}

	private long extractDataId(MvcResult result) throws Exception {
		String json = result.getResponse().getContentAsString();
		Boolean success = JsonPath.read(json, "$.success");
		assertThat(success).isTrue();
		Number id = JsonPath.read(json, "$.data.id");
		return id.longValue();
	}
}
