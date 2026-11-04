package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * BR-student-lifecycle, the course admin half: who owns the course that a student's course section belongs to.
 */
@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;
    @InjectMocks
    StudentSecurityService studentSecurityService;

    @BeforeEach
    void setUp() {
        Instructor bingyang = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        bingyang.setId(1);
        Course course = new Course("Senior Design", "Senior design course");
        course.setCourseAdmin(bingyang);
        Section section = new Section("2023-2024", LocalDate.of(2023, 7, 31), LocalDate.of(2024, 10, 6), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        section.setCourse(course);
        Student john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        john.setId(4);
        john.setSection(section);
        lenient().when(this.studentRepository.findById(4)).thenReturn(Optional.of(john)); // Not every test names John
    }

    @Test
    void testTheCourseAdminOfTheStudentsCourseIsAdmitted() {
        given(this.userUtils.getUserId()).willReturn(1);

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).isTrue();
    }

    @Test
    void testAnyoneElseIsNot() {
        given(this.userUtils.getUserId()).willReturn(3); // another course's admin, or an instructor, or the student

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).isFalse();
    }

    @Test
    void testAnIdThatNamesNoStudentIsDeniedWithoutThrowing() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(99)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(99)).isFalse();
    }

}
