-- Explicit operator maintenance in a dedicated modern race-settlement database.
-- Never executed by ModernMain. No source rewards, DM/Honor counters, or native tables.
-- No IF NOT EXISTS: a conflicting existing schema requires operator inspection.
CREATE TABLE `lito_modern_pet_race_result` (
  `result_id` BINARY(16) NOT NULL,
  `match_id` BIGINT NOT NULL,
  `generation` BIGINT NOT NULL,
  `terminal_revision` BIGINT NOT NULL,
  `logical_map_id` INT NOT NULL,
  `physical_map_id` INT NOT NULL,
  `recorded_at_millis` BIGINT NOT NULL,
  `participant_count` INT NOT NULL,
  `content_hash` BINARY(32) NOT NULL,
  `event_body` VARBINARY(4096) NOT NULL,
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `lito_modern_pet_race_participant` (
  `result_id` BINARY(16) NOT NULL,
  `entity_id` BIGINT NOT NULL,
  `source_id` INT NOT NULL,
  `object_id` BIGINT NOT NULL,
  `runner_revision` BIGINT NOT NULL,
  `progress_move_sequence` BIGINT NOT NULL,
  `completed_laps` BIGINT NOT NULL,
  `next_checkpoint` BIGINT NOT NULL,
  `placement` BIGINT NOT NULL,
  `finish_elapsed_nanos` BIGINT NOT NULL,
  `dnf_reason` BIGINT NOT NULL,
  `base_graphics_id` BIGINT NOT NULL,
  PRIMARY KEY (`result_id`,`source_id`,`object_id`),
  UNIQUE KEY `uq_pet_race_entity` (`result_id`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

-- MySQL 5.5 does not enforce CHECK constraints; the writer validates complete
-- event bytes, signed numeric fields, InnoDB columns and unprefixed unique keys.
-- Do not install triggers or grant this writer privileges on source schemas.
