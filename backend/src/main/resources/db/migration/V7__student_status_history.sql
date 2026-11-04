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

-- A student disabled before this history existed counts as deactivated from the moment this migration runs, and
-- active in every earlier week. NOW() is the database server's time (UTC on Azure), not app.timezone, so a run near
-- midnight can date this in a neighbouring week; that is harmless for accounts that were already disabled.
INSERT INTO `student_status_change` (`student_id`, `change_type`, `changed_at`)
SELECT `id`, 'DEACTIVATED', NOW(6)
FROM `peer_evaluation_user`
WHERE `dtype` = 'Student' AND `enabled` = b'0';
