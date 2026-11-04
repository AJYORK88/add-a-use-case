package team.projectpulse.student;

import tools.jackson.databind.json.JsonMapper;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.exception.StudentAlreadyDeactivatedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for Student API endpoints")
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    String adminToken;

    String instructorBillToken;

    String studentJohnToken;

    String studentEricToken;

    String adminTimToken;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    @BeforeEach
    void setUp() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.wei@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        MvcResult mvcResult = resultActions.andDo(print()).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(contentAsString);
        this.adminToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.instructorBillToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentJohnToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("e.hudson@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentEricToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("t.cook@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.adminTimToken = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    void testFindStudentsByCriteria() throws Exception {
        // Given
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("teamName", "Team1");
        String json = this.jsonMapper.writeValueAsString(searchCriteria);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "0");
        requestParams.add("size", "30");
        requestParams.add("sort", "lastName,asc");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students successfully"))
                .andExpect(jsonPath("$.data.content", Matchers.hasSize(3)));
    }

    @Test
    void testInstructorBillFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testAdminTimFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testStudentJohnFindOwnInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testStudentJohnFindAnotherStudentsInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/5").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testAddStudent() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "Abc123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add student successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("lucas"))
                .andExpect(jsonPath("$.data.firstName").value("Lucas"))
                .andExpect(jsonPath("$.data.lastName").value("Santos"))
                .andExpect(jsonPath("$.data.email").value("l.santos@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testAddStudentWithInvalidUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "marcos");
        studentDto.put("firstName", "Marcos");
        studentDto.put("lastName", "Grant");
        studentDto.put("email", "m.grant@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "invalidRegistrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("m.grant@abc.edu is not invited to register. Please contact the course admin."));
    }

    @Test
    void testAddStudentWithNoWrongUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "instructor"); // This is wrong, Lucas should be a student.

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You are not allowed to register as instructor for email: l.santos@abc.edu"));
    }

    @Test
    void testStudentJohnUpdatesOwnInfo() throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "john");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false); // Student cannot change their enabled status.
        studentDto.put("roles", "instructor"); // Student cannot change their role.

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Update student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.data.firstName").value("John (updated)"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.enabled").value(true)) // The enabled status should not be changed.
                .andExpect(jsonPath("$.data.roles").value("student")); // The role should not be changed.
    }

    @Test
    void testStudentJohnUpdatesAnotherStudentsInfo() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "eric");
        studentDto.put("firstName", "Eric (updated)");
        studentDto.put("lastName", "Hudson");
        studentDto.put("email", "e.hudson@abc.edu");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/5").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

//    @Test
//    void testAdminDeletesStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/3").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
//                .andExpect(jsonPath("$.flag").value(true))
//                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
//                .andExpect(jsonPath("$.message").value("Delete instructor successfully"));
//    }
//
//    @Test
//    void testStudentDeletesAnotherStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentToken))
//                .andExpect(jsonPath("$.flag").value(false))
//                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
//                .andExpect(jsonPath("$.message").value("No permission."))
//                .andExpect(jsonPath("$.data").value("Access Denied"));
//    }

    @Test
    void testStudentJohnFindStudentsInOwnTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students by team id successfully"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void testStudentJohnFindStudentsInAnotherTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/2").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    // ----- UC-STU-deactivate-student -----
    // The dev clock is Sunday 2023-08-20 23:30 in America/Chicago (2023-W33). John (4) and Eric (5) are on Team1 in
    // course section 2, which Bingyang (course admin of its course) and Bill are assigned to. Tim is the course admin
    // of another course.

    private static final int JOHN_ID = 4;
    private static final LocalDateTime DEV_NOW = LocalDateTime.of(2023, 8, 20, 23, 30);

    @Autowired
    EntityManager entityManager;
    @Autowired
    StudentService studentService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Main flow: an instructor assigned to her course section deactivates her, and the deactivation is recorded")
    void testInstructorBillDeactivatesJohn() throws Exception {
        // Given
        long activities = count("select count(a) from Activity a where a.student.id = " + JOHN_ID);
        long evaluations = count("select count(e) from PeerEvaluation e where e.evaluator.id = " + JOHN_ID + " or e.evaluatee.id = " + JOHN_ID);

        // When
        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(JOHN_ID))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.sectionId").value(2))
                .andExpect(jsonPath("$.data.teamName").value("Team1"));

        // Then: one DEACTIVATED change, dated by the dev clock, read back from the database
        this.entityManager.flush();
        this.entityManager.clear();
        Student john = this.entityManager.find(Student.class, JOHN_ID);
        assertThat(john.isEnabled()).isFalse();
        assertThat(john.getStatusChanges()).singleElement().satisfies(change -> {
            assertThat(change.getType()).isEqualTo(StudentStatusChangeType.DEACTIVATED);
            // Calendar time round trip: a driver that shifted this to UTC would read back 2023-08-21T04:30, in W34
            assertThat(change.getChangedAt()).isEqualTo(DEV_NOW);
        });
        // Her course section, team, and work are kept
        assertThat(john.getSection().getSectionId()).isEqualTo(2);
        assertThat(john.getTeam().getTeamName()).isEqualTo("Team1");
        assertThat(count("select count(a) from Activity a where a.student.id = " + JOHN_ID)).isEqualTo(activities);
        assertThat(count("select count(e) from PeerEvaluation e where e.evaluator.id = " + JOHN_ID + " or e.evaluatee.id = " + JOHN_ID)).isEqualTo(evaluations);
    }

    @Test
    @DisplayName("Main flow: the course admin who owns the course may deactivate her without being assigned to her course section")
    void testCourseAdminNotAssignedToTheSectionDeactivatesJohn() throws Exception {
        // Given: Bingyang is no longer assigned to course section 2, but still owns its course
        Section section2 = this.entityManager.find(Section.class, 2);
        Instructor bingyang = this.entityManager.createQuery("select i from Instructor i where i.email = 'b.wei@abc.edu'", Instructor.class).getSingleResult();
        section2.removeInstructor(bingyang);
        this.entityManager.flush();

        // When, then
        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("3a: deactivating a student who is already deactivated is a conflict, and nothing changes")
    void testDeactivatingAnAlreadyDeactivatedStudentIsAConflict() throws Exception {
        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));

        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.CONFLICT))
                .andExpect(jsonPath("$.message").value("This student is already deactivated."));

        assertThat(historyRowsOf(JOHN_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("3b: a student, herself included, cannot deactivate a student")
    void testStudentsCannotDeactivateStudents() throws Exception {
        expectForbidden(JOHN_ID, this.studentJohnToken); // herself
        expectForbidden(5, this.studentJohnToken);       // a teammate
        assertJohnUntouched();
    }

    @Test
    @DisplayName("3b: another course's admin cannot deactivate her")
    void testAnotherCoursesAdminCannotDeactivateJohn() throws Exception {
        expectForbidden(JOHN_ID, this.adminTimToken);
        assertJohnUntouched();
    }

    @Test
    @DisplayName("3b: an instructor no longer assigned to her course section cannot deactivate her")
    void testAnUnassignedInstructorCannotDeactivateJohn() throws Exception {
        // Given: Bill is no longer assigned to course section 2, and does not own its course
        Section section2 = this.entityManager.find(Section.class, 2);
        Instructor bill = this.entityManager.createQuery("select i from Instructor i where i.email = 'b.gates@abc.edu'", Instructor.class).getSingleResult();
        section2.removeInstructor(bill);
        this.entityManager.flush();

        expectForbidden(JOHN_ID, this.instructorBillToken);
        assertJohnUntouched();
    }

    @Test
    @DisplayName("3b: an id that names no student is refused at the route, not reported as missing")
    void testDeactivatingAnIdThatNamesNoStudentIsForbidden() throws Exception {
        expectForbidden(9999, this.adminToken);
    }

    @Test
    @DisplayName("FR-SEC-active-account: her open session is refused on its next request once she is deactivated")
    void testJohnsOpenSessionIsRefusedAfterHeIsDeactivated() throws Exception {
        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));

        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("The generic PUT ignores enabled: it neither deactivates her nor records anything")
    void testInstructorPutWithEnabledFalseLeavesJohnActive() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID).contentType(MediaType.APPLICATION_JSON).content(johnAsJson(false)).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.enabled").value(true));

        assertThat(historyRowsOf(JOHN_ID)).isZero();
    }

    @Test
    @DisplayName("The generic PUT ignores enabled: it cannot re-enable her, and keeps her history")
    void testInstructorPutWithEnabledTrueLeavesJohnDeactivated() throws Exception {
        this.mockMvc.perform(post(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));

        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID).contentType(MediaType.APPLICATION_JSON).content(johnAsJson(true)).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.firstName").value("Johnny"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(historyRowsOf(JOHN_ID)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Each deactivation must commit in its own transaction
    @DisplayName("3a under concurrency: two instructors confirming at once deactivate her exactly once")
    void testTwoConcurrentDeactivationsSerialize() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> deactivate = () -> {
            start.await();
            try {
                this.studentService.deactivateStudent(JOHN_ID);
                return "deactivated";
            } catch (StudentAlreadyDeactivatedException ex) {
                return "conflict";
            }
        };
        TransactionTemplate transaction = new TransactionTemplate(this.transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(deactivate);
            Future<String> second = executor.submit(deactivate);
            start.countDown();
            List<String> outcomes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(outcomes).containsExactlyInAnyOrder("deactivated", "conflict");
            Long historyRows = transaction.execute(status -> historyRowsOf(JOHN_ID));
            assertThat(historyRows).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            // These commits bypassed the test's rollback, so put John back as the seed has him for the other tests
            transaction.executeWithoutResult(status -> {
                this.entityManager.createQuery("delete from StudentStatusChange c where c.student.id = :id").setParameter("id", JOHN_ID).executeUpdate();
                this.entityManager.createQuery("update PeerEvaluationUser u set u.enabled = true where u.id = :id").setParameter("id", JOHN_ID).executeUpdate();
            });
        }
    }

    private void expectForbidden(int studentId, String token) throws Exception {
        this.mockMvc.perform(post(this.baseUrl + "/students/" + studentId + "/deactivation").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
    }

    private void assertJohnUntouched() {
        this.entityManager.flush();
        this.entityManager.clear();
        assertThat(this.entityManager.find(Student.class, JOHN_ID).isEnabled()).isTrue();
        assertThat(historyRowsOf(JOHN_ID)).isZero();
    }

    private long historyRowsOf(int studentId) {
        return this.entityManager.createQuery("select count(c) from StudentStatusChange c where c.student.id = :id", Long.class)
                .setParameter("id", studentId).getSingleResult();
    }

    private long count(String jpql) {
        return this.entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private String johnAsJson(boolean enabled) throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "j.smith@abc.edu");
        studentDto.put("firstName", "Johnny");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", enabled);
        return this.jsonMapper.writeValueAsString(studentDto);
    }

}
