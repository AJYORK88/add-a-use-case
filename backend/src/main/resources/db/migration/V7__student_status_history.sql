-- UC-STU-deactivate-student: a student's deactivation and reactivation history (BR-student-active-week).
-- One row per change, kept for as long as the student exists and deleted with her (UC-STU-delete-student).
-- changed_at is calendar time in app.timezone, written from the application's Clock.
CREATE TABLE `student_status_change` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `change_type` varchar(255) NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_student_status_change_student_changed_at` (`student_id`, `changed_at`),
  CONSTRAINT `FK_student_status_change_student` FOREIGN KEY (`student_id`) REFERENCES `peer_evaluation_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `CK_student_status_change_type` CHECK (`change_type` in ('DEACTIVATED', 'REACTIVATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A student disabled before this history existed has no recorded deactivation date, so the backfill dates one 21
-- days before this migration runs. Dated at the run itself, she would count as active in the week the migration runs
-- and in every earlier week (a change dated in a week makes her active in it), so for the two weeks after the deploy
-- her former teammates would be listed as owing her a peer evaluation, reminded about it, and shown her on the
-- instructor's report. Dated 21 days back, the week that date falls in (three weeks before the run) still counts as
-- active, and every week after it, including the one open for evaluation at deploy time and the one after, counts as
-- inactive. Earlier weeks keep counting as active, because when she was really disabled is unknown.
-- The cost: a student disabled less than 21 days before the run counts as inactive in a week or two she was in fact
-- active in, so those weeks no longer list her or accept a submission from or about her.
-- NOW() is the database server's time (UTC on Azure), not app.timezone, so a run near midnight can move the backfilled
-- date into a neighbouring week, which shifts by one week which old week is her last active one.
INSERT INTO `student_status_change` (`student_id`, `change_type`, `changed_at`)
SELECT `id`, 'DEACTIVATED', NOW(6) - INTERVAL 21 DAY
FROM `peer_evaluation_user`
WHERE `dtype` = 'Student' AND `enabled` = b'0';
