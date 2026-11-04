package team.projectpulse.student;

/**
 * The two kinds of entry in a student's deactivation and reactivation history (BR-student-active-week).
 *
 * <p>Only {@link #DEACTIVATED} is written by the application today. {@link #REACTIVATED} is written by
 * UC-STU-reactivate-student, which is not built yet (OI-67), but {@link Student#isActiveIn(String)} already applies it
 * because the rule it implements does.
 */
public enum StudentStatusChangeType {

    DEACTIVATED,

    REACTIVATED

}
