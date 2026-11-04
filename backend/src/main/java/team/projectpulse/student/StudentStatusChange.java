package team.projectpulse.student;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One entry in a student's deactivation and reactivation history: what happened to her account, and when.
 *
 * <p>The history is part of the {@link Student} aggregate and is never cleared, because BR-student-active-week decides
 * from it which weeks she was active in, and a reactivation must not make her past inactive weeks active.
 * {@code changedAt} is calendar time, read from the injected {@code Clock} in {@code app.timezone}.
 */
@Entity
@Table(name = "student_status_change",
        indexes = @Index(name = "IX_student_status_change_student_changed_at", columnList = "student_id, changed_at"))
public class StudentStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private StudentStatusChangeType type;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;


    public StudentStatusChange() {
    }

    public StudentStatusChange(Student student, StudentStatusChangeType type, LocalDateTime changedAt) {
        this.student = student;
        this.type = type;
        this.changedAt = changedAt;
    }

    public Integer getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public StudentStatusChangeType getType() {
        return type;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

}
