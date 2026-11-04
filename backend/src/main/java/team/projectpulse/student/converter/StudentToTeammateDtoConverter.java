package team.projectpulse.student.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import team.projectpulse.student.Student;
import team.projectpulse.student.dto.TeammateDto;

@Component
public class StudentToTeammateDtoConverter implements Converter<Student, TeammateDto> {

    @Override
    public TeammateDto convert(Student student) {
        // No password, and no account status (CO-ferpa)
        return new TeammateDto(
                student.getId(),
                student.getUsername(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getRoles(),
                student.getSection().getSectionId(),
                student.getSection().getSectionName(),
                student.getTeam() == null ? null : student.getTeam().getTeamId(),
                student.getTeam() == null ? null : student.getTeam().getTeamName()
        );
    }

}
