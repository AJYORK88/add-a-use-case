package team.projectpulse.student;

import team.projectpulse.section.Section;
import team.projectpulse.system.WeekKeys;
import team.projectpulse.system.exception.StudentAlreadyDeactivatedException;
import team.projectpulse.team.Team;
import team.projectpulse.user.PeerEvaluationUser;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Entity
public class Student extends PeerEvaluationUser {

    @ManyToOne(optional = false)
    private Section section;
    @ManyToOne
    private Team team;

    /**
     * Her deactivation and reactivation history (BR-student-active-week). Never cleared: {@code orphanRemoval} is
     * here so that UC-STU-delete-student removes it with her, not so anything else may. Never assign this field from
     * a converted or detached {@code Student}; a merge would delete the history.
     *
     * <p>Invariant: {@code enabled} equals the latest change ({@code false} after {@code DEACTIVATED}), or
     * {@code true} when there is none. {@code enabled} stays the authentication flag so that login and the per-request
     * account check need not read the history.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt")
    private List<StudentStatusChange> statusChanges = new ArrayList<>();


    public Student() {
    }

    public Student(String username, String firstName, String lastName, String email, String password, boolean enabled, String roles) {
        super(username, firstName, lastName, email, password, enabled, roles);
    }

    /**
     * Deactivates her account and records the deactivation in her history (UC-STU-deactivate-student step 6).
     *
     * @param at calendar time, read from the injected {@code Clock}
     * @throws StudentAlreadyDeactivatedException if she is already deactivated (extension 3a); nothing changes
     */
    public void deactivate(LocalDateTime at) {
        if (!isEnabled()) {
            throw new StudentAlreadyDeactivatedException();
        }
        setEnabled(false);
        addStatusChange(StudentStatusChangeType.DEACTIVATED, at);
    }

    /**
     * Whether she was active for any part of the week (BR-student-active-week). The one implementation of the rule:
     * {@code evaluation}, {@code activity}, and {@code notification} call this rather than working it out.
     *
     * <ul>
     *     <li>A change dated in the week makes her active in it: a deactivation in the week means she was active
     *     before it, and a reactivation means she is active after it.</li>
     *     <li>Otherwise the latest change before the week decides: none, or a reactivation, means active; a
     *     deactivation means not.</li>
     * </ul>
     *
     * <p>Weeks are compared by their Monday, never by the string order of their keys. Reads the history, so call it
     * inside a transaction, and through a query that fetch-joins the history when checking many students.
     *
     * @param weekKey an ISO week key such as {@code "2023-W32"}
     * @throws IllegalArgumentException if the key is not a valid week
     */
    public boolean isActiveIn(String weekKey) {
        LocalDate monday = WeekKeys.monday(weekKey);
        LocalDate nextMonday = monday.plusWeeks(1);

        boolean changedInWeek = this.statusChanges.stream()
                .map(change -> change.getChangedAt().toLocalDate())
                .anyMatch(day -> !day.isBefore(monday) && day.isBefore(nextMonday));
        if (changedInWeek) {
            return true;
        }

        // Pick the latest by date rather than trusting the list order, which @OrderBy guarantees only once loaded
        return this.statusChanges.stream()
                .filter(change -> change.getChangedAt().toLocalDate().isBefore(monday))
                .max(Comparator.comparing(StudentStatusChange::getChangedAt))
                .map(latest -> latest.getType() == StudentStatusChangeType.REACTIVATED)
                .orElse(true);
    }

    /**
     * Appends a change to her history. Package-private: deactivation goes through {@link #deactivate}, and no use case
     * writes a reactivation yet (OI-67), so only the unit tests in this package call this directly.
     */
    void addStatusChange(StudentStatusChangeType type, LocalDateTime at) {
        this.statusChanges.add(new StudentStatusChange(this, type, at));
    }

    public List<StudentStatusChange> getStatusChanges() {
        return Collections.unmodifiableList(this.statusChanges);
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

}
