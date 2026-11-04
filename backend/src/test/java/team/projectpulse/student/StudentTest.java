package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.projectpulse.system.exception.StudentAlreadyDeactivatedException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UC-STU-deactivate-student's domain rules on the {@link Student} aggregate: deactivation (step 6, extension 3a) and
 * BR-student-active-week as {@link Student#isActiveIn(String)} applies it.
 *
 * <p>2023-W32 runs Monday 2023-08-07 to Sunday 2023-08-13, and 2023-W33 from 2023-08-14 to 2023-08-20.
 */
@DisplayName("Unit tests for a student's deactivation and active weeks")
class StudentTest {

    private Student john;

    @BeforeEach
    void setUp() {
        this.john = new Student("john", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
    }

    @Test
    void testDeactivateDisablesHerAndRecordsTheChange() {
        LocalDateTime at = LocalDateTime.of(2023, 8, 20, 23, 30);

        this.john.deactivate(at);

        assertThat(this.john.isEnabled()).isFalse();
        assertThat(this.john.getStatusChanges()).singleElement().satisfies(change -> {
            assertThat(change.getType()).isEqualTo(StudentStatusChangeType.DEACTIVATED);
            assertThat(change.getChangedAt()).isEqualTo(at);
            assertThat(change.getStudent()).isSameAs(this.john);
        });
    }

    @Test
    void testDeactivateAnAlreadyDeactivatedStudentThrowsAndChangesNothing() {
        this.john.deactivate(LocalDateTime.of(2023, 8, 14, 9, 0));

        assertThatThrownBy(() -> this.john.deactivate(LocalDateTime.of(2023, 8, 20, 23, 30)))
                .isInstanceOf(StudentAlreadyDeactivatedException.class)
                .hasMessage("This student is already deactivated.");
        assertThat(this.john.getStatusChanges()).hasSize(1);
        assertThat(this.john.isEnabled()).isFalse();
    }

    @Test
    void testNoHistoryMeansActiveInEveryWeek() {
        assertThat(this.john.isActiveIn("2023-W01")).isTrue();
        assertThat(this.john.isActiveIn("2023-W33")).isTrue();
    }

    @Test
    void testDeactivatedInAWeekIsActiveInItButNotAfter() {
        this.john.deactivate(LocalDateTime.of(2023, 8, 20, 23, 30)); // Sunday of W33

        assertThat(this.john.isActiveIn("2023-W32")).isTrue();
        assertThat(this.john.isActiveIn("2023-W33")).isTrue();
        assertThat(this.john.isActiveIn("2023-W34")).isFalse();
        assertThat(this.john.isActiveIn("2024-W10")).isFalse();
    }

    @Test
    void testReactivatedInAWeekIsActiveInItButNotTheWeekBefore() {
        this.john.addStatusChange(StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 7, 30, 12, 0)); // W30
        this.john.addStatusChange(StudentStatusChangeType.REACTIVATED, LocalDateTime.of(2023, 8, 14, 0, 0)); // Monday of W33

        assertThat(this.john.isActiveIn("2023-W30")).isTrue();
        assertThat(this.john.isActiveIn("2023-W31")).isFalse();
        assertThat(this.john.isActiveIn("2023-W32")).isFalse();
        assertThat(this.john.isActiveIn("2023-W33")).isTrue();
        assertThat(this.john.isActiveIn("2023-W34")).isTrue();
    }

    @Test
    void testDeactivatedAndReactivatedInOneWeekIsActiveInIt() {
        this.john.addStatusChange(StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 8, 8, 10, 0));
        this.john.addStatusChange(StudentStatusChangeType.REACTIVATED, LocalDateTime.of(2023, 8, 10, 10, 0));

        assertThat(this.john.isActiveIn("2023-W32")).isTrue();
        assertThat(this.john.isActiveIn("2023-W33")).isTrue();
    }

    @Test
    void testDeactivatedAgainAfterAReactivationIsInactiveInTheWeeksAfter() {
        this.john.addStatusChange(StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 7, 30, 12, 0)); // W30
        this.john.addStatusChange(StudentStatusChangeType.REACTIVATED, LocalDateTime.of(2023, 8, 14, 0, 0)); // W33
        this.john.addStatusChange(StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 8, 28, 9, 0)); // W35

        assertThat(this.john.isActiveIn("2023-W34")).isTrue();
        assertThat(this.john.isActiveIn("2023-W35")).isTrue();
        assertThat(this.john.isActiveIn("2023-W36")).isFalse();
        assertThat(this.john.isActiveIn("2023-W31")).isFalse(); // The earlier inactive weeks stay inactive
    }

    @Test
    void testWeeksAreComparedByDateAcrossAYearBoundary() {
        // Sunday 2023-12-31 is in 2023-W52; Monday 2024-01-01 starts 2024-W01
        this.john.deactivate(LocalDateTime.of(2023, 12, 31, 22, 0));

        assertThat(this.john.isActiveIn("2023-W52")).isTrue();
        assertThat(this.john.isActiveIn("2024-W01")).isFalse();
    }

    @Test
    void testHistoryAppendedOutOfOrderStillPicksTheLatestChange() {
        this.john.addStatusChange(StudentStatusChangeType.REACTIVATED, LocalDateTime.of(2023, 8, 14, 0, 0));
        this.john.addStatusChange(StudentStatusChangeType.DEACTIVATED, LocalDateTime.of(2023, 7, 30, 12, 0));

        assertThat(this.john.isActiveIn("2023-W40")).isTrue();
    }

}
