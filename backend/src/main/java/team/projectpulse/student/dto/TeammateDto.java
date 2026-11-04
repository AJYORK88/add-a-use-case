package team.projectpulse.student.dto;

/**
 * A team member as a student caller receives her: a {@link StudentDto} without {@code enabled}, because a student is
 * never shown another student's account status (CO-ferpa, UC-STU-deactivate-student). An instructor or course admin
 * receives the full {@link StudentDto}.
 */
public record TeammateDto(Integer id,
                          String username,
                          String firstName,
                          String lastName,
                          String email,
                          String roles,
                          Integer sectionId,
                          String sectionName,
                          Integer teamId,
                          String teamName) {
}
