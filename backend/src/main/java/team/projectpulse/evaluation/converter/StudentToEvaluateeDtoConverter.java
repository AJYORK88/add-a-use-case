package team.projectpulse.evaluation.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import team.projectpulse.evaluation.dto.EvaluateeDto;
import team.projectpulse.student.Student;

@Component
public class StudentToEvaluateeDtoConverter implements Converter<Student, EvaluateeDto> {

    @Override
    public EvaluateeDto convert(Student student) {
        return new EvaluateeDto(student.getId(), student.getFirstName(), student.getLastName());
    }

}
