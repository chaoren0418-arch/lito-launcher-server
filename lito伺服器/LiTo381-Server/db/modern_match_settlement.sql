-- Explicit maintenance only. Select a dedicated modern settlement database first.
-- This script is NOT executed by ModernMain or ModernMatchSettlementJdbc.
-- Requires transactional InnoDB, including on the retained MySQL 5.5 server.
-- Never substitute the native MyISAM lito_tournament_character_record table.
-- No DROP/ALTER/IF NOT EXISTS: a collision requires operator inspection.

CREATE TABLE `lito_modern_match` (
  `result_id` BINARY(16) NOT NULL,
  `match_id` BIGINT NOT NULL,
  `generation` BIGINT NOT NULL,
  `engine_revision` BIGINT NOT NULL,
  `mode` INT NOT NULL,
  `map_id` INT NOT NULL,
  `finished_at_millis` BIGINT NOT NULL,
  `participant_count` INT NOT NULL,
  `event_hash` BINARY(32) NOT NULL,
  `event_body` VARBINARY(4096) NOT NULL,
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `lito_modern_match_participant` (
  `result_id` BINARY(16) NOT NULL,
  `entity_id` BIGINT NOT NULL,
  `source_id` INT NOT NULL,
  `object_id` BIGINT NOT NULL,
  `team` TINYINT NOT NULL,
  `verdict` TINYINT NOT NULL,
  PRIMARY KEY (`result_id`,`source_id`,`object_id`),
  UNIQUE KEY `uq_modern_match_entity` (`result_id`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `lito_modern_character_record` (
  `source_id` INT NOT NULL,
  `object_id` BIGINT NOT NULL,
  `wins` BIGINT NOT NULL,
  `losses` BIGINT NOT NULL,
  `revision` BIGINT NOT NULL,
  PRIMARY KEY (`source_id`,`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

-- Application checks enforce signed i64 positivity and revision = wins + losses;
-- Only needed when explicitly enabling ROOM_TEST_DUMMY_V1. BOT identifiers are
-- not native character object IDs and never receive character-record totals.
CREATE TABLE `lito_modern_match_bot` (
  `result_id` BINARY(16) NOT NULL,
  `entity_id` BIGINT NOT NULL,
  `source_id` INT NOT NULL,
  `bot_id` BIGINT NOT NULL,
  `team` TINYINT NOT NULL,
  `verdict` TINYINT NOT NULL,
  PRIMARY KEY (`result_id`,`source_id`,`bot_id`),
  UNIQUE KEY `uq_modern_match_bot_entity` (`result_id`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

-- MySQL 5.5 does not enforce CHECK constraints. Do not grant source-schema writes
-- to the settlement account. Do not install cross-schema triggers on these tables.
