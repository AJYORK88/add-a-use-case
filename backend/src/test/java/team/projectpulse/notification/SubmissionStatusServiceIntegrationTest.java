package team.projectpulse.notification;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.activity.Activity;
import team.projectpulse.student.Student;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BR-submission-owed against the seed data and the real queries. In course section 2 for 2023-W32, Team1 (John,
 * Eric, Jerry) evaluated every teammate and John and Eric reported activities; Team2 and Team3 submitted nothing;
 * Tracy, Arline, Shad, and Lynne are on no team. What a deactivation does to the weeks around it is in
 * {@code StudentActiveWeekIntegrationTest}.
 */
@DisplayName("Integration tests for who owes a weekly item")
class SubmissionStatusServiceIntegrationTest extends AbstractIntegrationTest {

    private static final int SECTION_ID = 2;
    private static final String WEEK = "2023-W32";

    @Autowired
    SubmissionStatusService submissionStatusService;
    @Autowired
    EntityManager entityManager;

    @Test
    void testWeeklyActivityReportOwedInSeedData() {
        List<String> owing = names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.WEEKLY_ACTIVITY_REPORT, WEEK));

        assertThat(owing).containsExactly(
                "Jerry Moon",
                "Woody Allen", "Agustin Freeman", "Cora Manning", "Amanda Wagner",
                "Mavis Huber", "Rosendo Maxwell", "Jan Mckinney", "Mary Vargas");
    }

    @Test
    void testPeerEvaluationOwedInSeedData() {
        List<String> owing = names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.PEER_EVALUATION, WEEK));

        assertThat(owing).containsExactly(
                "Woody Allen", "Agustin Freeman", "Cora Manning", "Amanda Wagner",
                "Mavis Huber", "Rosendo Maxwell", "Jan Mckinney", "Mary Vargas");
    }

    @Test
    void testDeletingAStudentsLastActivityMakesTheReportOwedAgain() {
        // Given: Eric's one activity for the week
        List<Activity> erics = this.entityManager.createQuery(
                        "select a from Activity a where a.week = :week and a.student.email = 'e.hudson@abc.edu'", Activity.class)
                .setParameter("week", WEEK).getResultList();
        assertThat(erics).hasSize(1);

        // When
        this.entityManager.remove(erics.get(0));
        this.entityManager.flush();

        // Then
        assertThat(names(this.submissionStatusService.findOwing(SECTION_ID, SubmittableItem.WEEKLY_ACTIVITY_REPORT, WEEK)))
                .contains("Eric Hudson");
    }

    private static List<String> names(List<Student> students) {
        return students.stream().map(student -> student.getFirstName() + " " + student.getLastName()).toList();
    }

}
