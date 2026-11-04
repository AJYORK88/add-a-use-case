package team.projectpulse.student;

import jakarta.persistence.EntityManager;
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
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.evaluation.dto.PeerEvaluationDto;
import team.projectpulse.evaluation.dto.RatingDto;
import team.projectpulse.notification.SubmissionStatusService;
import team.projectpulse.notification.SubmittableItem;
import team.projectpulse.system.StatusCode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * BR-student-active-week end to end, against the seed data and the real queries: what a deactivation does to the
 * weeks around it, for peer evaluations, weekly activity reports, the course section's evaluation report, and who
 * owes what (BR-submission-owed).
 *
 * <p>The dev clock is Sunday 2023-08-20 23:30 (2023-W33), so the week being evaluated is 2023-W32. In course section
 * 2, Team1 is John (4), Eric (5), and Jerry (6); for W32 each evaluated every teammate, and John and Eric reported.
 *
 * <ul>
 *     <li><strong>Boundary week:</strong> John is deactivated now, in W33. W32 is still an active week for him.</li>
 *     <li><strong>Inactive week:</strong> John's history reads deactivated in W30 and reactivated in W33, so W31 and
 *     W32 are inactive, while his account stays enabled and he can still authenticate. No use case writes a
 *     reactivation yet (OI-67), so this fixture writes the history directly.</li>
 * </ul>
 */
@DisplayName("Integration tests for the weeks a deactivated student was active in")
class StudentActiveWeekIntegrationTest extends AbstractIntegrationTest {

    private static final int SECTION_ID = 2;
    private static final String WEEK = "2023-W32";
    private static final int JOHN_ID = 4;
    private static final int ERIC_ID = 5;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    EntityManager entityManager;
    @Autowired
    SubmissionStatusService submissionStatusService;
    @Autowired
    Clock clock;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String adminBingyangToken;
    String studentJohnToken;
    String studentEricToken;


    @BeforeEach
    void setUp() throws Exception {
        this.adminBingyangToken = login("b.wei@abc.edu");
        this.studentJohnToken = login("j.smith@abc.edu");
        this.studentEricToken = login("e.hudson@abc.edu");
    }

    // ----- Boundary week: deactivated in W33, so still active in W32 -----

    @Test
    void testBoundaryWeekTeammatesStillOweHimAnEvaluationButHeOwesNothing() {
        // Given: John is deactivated now (W33), and nothing about him is submitted for W32
        deactivateJohnNow();
        deleteEvaluationsOf(JOHN_ID);
        deleteEvaluationsBy(JOHN_ID);
        deleteActivitiesOf(JOHN_ID);

        // Then: Eric and Jerry owe an evaluation of him; he himself is not listed as owing anything
        assertThat(names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.PEER_EVALUATION, WEEK)))
                .contains("Eric Hudson", "Jerry Moon")
                .doesNotContain("John Smith");
        assertThat(names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.WEEKLY_ACTIVITY_REPORT, WEEK)))
                .doesNotContain("John Smith");
    }

    @Test
    void testBoundaryWeekATeammateMayStillEvaluateHim() throws Exception {
        // Given
        deactivateJohnNow();
        this.entityManager.createQuery("delete from Rating r where r.peerEvaluation.evaluator.id = :eric and r.peerEvaluation.evaluatee.id = :john and r.peerEvaluation.week = :week")
                .setParameter("eric", ERIC_ID).setParameter("john", JOHN_ID).setParameter("week", WEEK).executeUpdate();
        this.entityManager.createQuery("delete from PeerEvaluation e where e.evaluator.id = :eric and e.evaluatee.id = :john and e.week = :week")
                .setParameter("eric", ERIC_ID).setParameter("john", JOHN_ID).setParameter("week", WEEK).executeUpdate();
        flushAndClear();

        // When, then
        this.mockMvc.perform(post(this.baseUrl + "/evaluations").contentType(MediaType.APPLICATION_JSON).content(anEvaluation(ERIC_ID, JOHN_ID)).header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.evaluateeId").value(JOHN_ID));
    }

    @Test
    void testBoundaryWeekTheSectionReportListsHimMarkedDeactivated() throws Exception {
        deactivateJohnNow();

        this.mockMvc.perform(get(this.baseUrl + "/evaluations/sections/" + SECTION_ID + "/week/" + WEEK).header(HttpHeaders.AUTHORIZATION, this.adminBingyangToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.peerEvaluationAverages[?(@.studentId == " + JOHN_ID + ")].deactivated").value(Matchers.contains(true)))
                .andExpect(jsonPath("$.data.peerEvaluationAverages[?(@.studentId == " + ERIC_ID + ")].deactivated").value(Matchers.contains(false)));
    }

    @Test
    void testBoundaryWeekHeIsInHisTeammatesEvaluateeList() throws Exception {
        deactivateJohnNow();

        // Names only, never a status (CO-ferpa)
        this.mockMvc.perform(get(this.baseUrl + "/evaluations/weeks/" + WEEK + "/evaluatees").header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data[*].id").value(Matchers.containsInAnyOrder(JOHN_ID, ERIC_ID, 6)))
                .andExpect(jsonPath("$.data[0].*", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].firstName").exists())
                .andExpect(jsonPath("$.data[0].lastName").exists());
    }

    // ----- Inactive week: deactivated in W30, reactivated in W33, so inactive in W32 -----

    @Test
    void testInactiveWeekHeIsAbsentFromHisTeammatesEvaluateeList() throws Exception {
        givenJohnWasInactiveInW31AndW32();

        this.mockMvc.perform(get(this.baseUrl + "/evaluations/weeks/" + WEEK + "/evaluatees").header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data[*].id").value(Matchers.containsInAnyOrder(ERIC_ID, 6)));
    }

    @Test
    void testInactiveWeekHeHasNoEvaluateeListOfHisOwn() throws Exception {
        givenJohnWasInactiveInW31AndW32();

        this.mockMvc.perform(get(this.baseUrl + "/evaluations/weeks/" + WEEK + "/evaluatees").header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You may submit a peer evaluation only for a week you were active in."));
    }

    @Test
    void testAnInstructorHasNoEvaluateeList() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/evaluations/weeks/" + WEEK + "/evaluatees").header(HttpHeaders.AUTHORIZATION, this.adminBingyangToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("Only a student may submit a peer evaluation."));
    }

    @Test
    void testAMalformedWeekIsABadRequest() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/evaluations/weeks/2023-32/evaluatees").header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT));
    }

    @Test
    void testInactiveWeekAnEvaluationOfHimIsRefusedByName() throws Exception {
        givenJohnWasInactiveInW31AndW32();

        this.mockMvc.perform(post(this.baseUrl + "/evaluations").contentType(MediaType.APPLICATION_JSON).content(anEvaluation(ERIC_ID, JOHN_ID)).header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("John Smith is not evaluated for that week."));
    }

    @Test
    void testInactiveWeekHisOwnEvaluationIsRefused() throws Exception {
        givenJohnWasInactiveInW31AndW32();

        this.mockMvc.perform(post(this.baseUrl + "/evaluations").contentType(MediaType.APPLICATION_JSON).content(anEvaluation(JOHN_ID, ERIC_ID)).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You may submit a peer evaluation only for a week you were active in."));
    }

    @Test
    void testInactiveWeekHisOwnActivityIsRefused() throws Exception {
        givenJohnWasInactiveInW31AndW32();
        Map<String, Object> activity = new HashMap<>();
        activity.put("week", WEEK);
        activity.put("category", "DEVELOPMENT");
        activity.put("activity", "Integrate Payment Gateway");
        activity.put("description", "Integrate Stripe payment gateway into the application");
        activity.put("plannedHours", 8.0);
        activity.put("actualHours", 7.5);
        activity.put("status", "COMPLETED");

        this.mockMvc.perform(post(this.baseUrl + "/activities").contentType(MediaType.APPLICATION_JSON).content(this.jsonMapper.writeValueAsString(activity)).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You may submit a weekly activity report only for a week you were active in."));
    }

    @Test
    void testInactiveWeekHeIsAbsentFromTheSectionReport() throws Exception {
        givenJohnWasInactiveInW31AndW32();

        this.mockMvc.perform(get(this.baseUrl + "/evaluations/sections/" + SECTION_ID + "/week/" + WEEK).header(HttpHeaders.AUTHORIZATION, this.adminBingyangToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.peerEvaluationAverages[?(@.studentId == " + JOHN_ID + ")]").isEmpty())
                .andExpect(jsonPath("$.data.peerEvaluationAverages[?(@.studentId == " + ERIC_ID + ")]").isNotEmpty());
    }

    @Test
    void testInactiveWeekNobodyOwesHimAndHeOwesNothing() {
        // Given: nothing about John is submitted for W32
        givenJohnWasInactiveInW31AndW32();
        deleteEvaluationsOf(JOHN_ID);
        deleteEvaluationsBy(JOHN_ID);
        deleteActivitiesOf(JOHN_ID);

        // Then: Eric and Jerry evaluated every teammate who was active in W32, and John owes nothing for it
        assertThat(names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.PEER_EVALUATION, WEEK)))
                .doesNotContain("John Smith", "Eric Hudson", "Jerry Moon");
        assertThat(names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.WEEKLY_ACTIVITY_REPORT, WEEK)))
                .doesNotContain("John Smith");
    }

    // ----- Fixtures -----

    private void deactivateJohnNow() {
        Student john = this.entityManager.find(Student.class, JOHN_ID);
        john.deactivate(LocalDateTime.now(this.clock));
        flushAndClear();
    }

    private void givenJohnWasInactiveInW31AndW32() {
        Student john = this.entityManager.find(Student.class, JOHN_ID);
        this.entityManager.persist(new StudentStatusChange(john, StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 7, 30, 12, 0)));
        this.entityManager.persist(new StudentStatusChange(john, StudentStatusChangeType.REACTIVATED, LocalDateTime.of(2023, 8, 14, 9, 0)));
        flushAndClear();
    }

    private void deleteEvaluationsOf(int evaluateeId) {
        this.entityManager.createQuery("delete from Rating r where r.peerEvaluation.evaluatee.id = :id and r.peerEvaluation.week = :week")
                .setParameter("id", evaluateeId).setParameter("week", WEEK).executeUpdate();
        this.entityManager.createQuery("delete from PeerEvaluation e where e.evaluatee.id = :id and e.week = :week")
                .setParameter("id", evaluateeId).setParameter("week", WEEK).executeUpdate();
        flushAndClear();
    }

    private void deleteEvaluationsBy(int evaluatorId) {
        this.entityManager.createQuery("delete from Rating r where r.peerEvaluation.evaluator.id = :id and r.peerEvaluation.week = :week")
                .setParameter("id", evaluatorId).setParameter("week", WEEK).executeUpdate();
        this.entityManager.createQuery("delete from PeerEvaluation e where e.evaluator.id = :id and e.week = :week")
                .setParameter("id", evaluatorId).setParameter("week", WEEK).executeUpdate();
        flushAndClear();
    }

    private void deleteActivitiesOf(int studentId) {
        this.entityManager.createQuery("delete from Activity a where a.student.id = :id and a.week = :week")
                .setParameter("id", studentId).setParameter("week", WEEK).executeUpdate();
        flushAndClear();
    }

    private void flushAndClear() {
        this.entityManager.flush();
        this.entityManager.clear();
    }

    // The evaluator is stamped from the caller's token; the id here only satisfies the DTO's validation
    private String anEvaluation(int evaluatorId, int evaluateeId) throws Exception {
        List<RatingDto> ratings = List.of(
                new RatingDto(null, 1, 4.0),
                new RatingDto(null, 2, 9.0),
                new RatingDto(null, 3, 7.0),
                new RatingDto(null, 4, 10.0),
                new RatingDto(null, 5, 5.0),
                new RatingDto(null, 6, 6.0));
        return this.jsonMapper.writeValueAsString(new PeerEvaluationDto(null, WEEK, evaluatorId, null, evaluateeId, null, ratings, 41.0, "Good job", "Keep it up", null, null));
    }

    private String login(String email) throws Exception {
        String body = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456")))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

    private static List<String> names(List<Student> students) {
        return students.stream().map(student -> student.getFirstName() + " " + student.getLastName()).toList();
    }

}
