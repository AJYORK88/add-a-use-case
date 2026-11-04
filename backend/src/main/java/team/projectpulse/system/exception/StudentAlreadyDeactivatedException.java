package team.projectpulse.system.exception;

/**
 * UC-STU-deactivate-student extension 3a: the student is already deactivated, so nothing changes. Mapped to
 * {@code 409 CONFLICT} rather than an idempotent success, so the instructor is never told a deactivation happened that
 * did not.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("This student is already deactivated.");
    }

}
