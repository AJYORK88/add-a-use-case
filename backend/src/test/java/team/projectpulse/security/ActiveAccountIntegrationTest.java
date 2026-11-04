package team.projectpulse.security;

import jakarta.persistence.EntityManager;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.student.Student;
import team.projectpulse.system.StatusCode;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: a session that is already open is refused on its next request once its account is deactivated
 * or deleted, for a student and an instructor alike, and a new login is refused with the generic message.
 */
@DisplayName("Integration tests for refusing the open session of a deactivated account")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    private static final int JOHN_ID = 4;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager entityManager;
    @Autowired
    Clock clock;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String studentJohnToken;
    String instructorBillToken;


    @BeforeEach
    void setUp() throws Exception {
        this.studentJohnToken = login("j.smith@abc.edu");
        this.instructorBillToken = login("b.gates@abc.edu");
    }

    @Test
    void testAnActiveStudentsTokenStillWorks() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));
    }

    @Test
    void testADeactivatedStudentsOpenSessionIsRefusedOnItsNextRequest() throws Exception {
        // Given: John's token was issued before he was deactivated
        Student john = this.entityManager.find(Student.class, JOHN_ID);
        john.deactivate(LocalDateTime.now(this.clock));
        this.entityManager.flush();

        // When, then
        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testADeactivatedStudentCannotLogInAgain() throws Exception {
        Student john = this.entityManager.find(Student.class, JOHN_ID);
        john.deactivate(LocalDateTime.now(this.clock));
        this.entityManager.flush();

        // The refusal stays generic: it does not say the account is deactivated
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("User account is abnormal."));
    }

    @Test
    void testADeactivatedInstructorsOpenSessionIsRefusedOnItsNextRequest() throws Exception {
        // Given: Bill's token was issued before his account was disabled (UC-INS-deactivate-instructor)
        Instructor bill = this.entityManager.createQuery("select i from Instructor i where i.email = 'b.gates@abc.edu'", Instructor.class).getSingleResult();
        bill.setEnabled(false);
        this.entityManager.flush();

        // When, then
        this.mockMvc.perform(get(this.baseUrl + "/instructors/" + bill.getId()).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testATokenOfADeletedAccountIsRefused() throws Exception {
        // Given: John's row is gone. Deleted directly, with foreign key checks off for this connection so his activities
        // and evaluations need not go first; the test transaction rolls it all back.
        this.entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        this.entityManager.createNativeQuery("delete from peer_evaluation_user where id = " + JOHN_ID).executeUpdate();
        this.entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        this.entityManager.clear();

        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    private String login(String email) throws Exception {
        String body = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456")))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

}
