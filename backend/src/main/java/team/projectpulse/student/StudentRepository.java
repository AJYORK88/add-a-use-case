package team.projectpulse.student;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    /**
     * Every student in a course section, with her deactivation and reactivation history fetched in the same query, so
     * that asking each one {@code isActiveIn(week)} costs no extra select per student.
     */
    @EntityGraph(attributePaths = "statusChanges")
    List<Student> findBySectionSectionId(Integer sectionId);

    /**
     * Loads a student with a pessimistic write lock, so two concurrent deactivations of her serialize and the second
     * sees the first one's result (UC-STU-deactivate-student 3a).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Student> findWithLockById(Integer id);

    List<Student> findByTeamTeamId(Integer teamId);
}
