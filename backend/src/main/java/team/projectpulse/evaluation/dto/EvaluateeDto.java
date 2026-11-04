package team.projectpulse.evaluation.dto;

/**
 * A teammate the caller evaluates for a week: names only. No account status, because a student is never shown another
 * student's status (CO-ferpa); the server has already decided who is listed (BR-student-active-week).
 */
public record EvaluateeDto(Integer id,
                           String firstName,
                           String lastName) {
}
