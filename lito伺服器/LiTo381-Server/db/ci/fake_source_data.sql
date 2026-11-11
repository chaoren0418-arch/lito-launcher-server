-- CI-only fake rows for one source character database (Issue #334 / T119).
-- build_ci_database.py replaces @@SOURCE_DB@@ with 380_ac / 380_ac_1 / 380_ac_2.
-- These are invented values: no account, character or item comes from a real dump.
-- Column lists follow the schema extracted from db/380_ac_1.sql.
USE `@@SOURCE_DB@@`;
SET NAMES utf8mb4;

INSERT INTO `accounts` (`access_level`,`server_no`,`login`,`password`,`ip`,`host`,`banned`,`character_slot`,`spw`,`warehouse`,`ezpay`)
VALUES (0,0,'ci_player','ci_player_pw','127.0.0.1','127.0.0.1',0,0,'',-256,0),
       (200,0,'ci_gm','ci_gm_pw','127.0.0.1','127.0.0.1',0,0,'',-256,0);

INSERT INTO `characters` (`account_name`,`AccessLevel`,`char_name`,`objid`,`level`,`HighLevel`,`MaxHp`,`CurHp`,`MaxMp`,`CurMp`,
  `Ac`,`Str`,`Con`,`Dex`,`Cha`,`Intel`,`Wis`,`Class`,`Sex`,`Type`,`LocX`,`LocY`,`MapID`,`Food`,`Lawful`,
  `OriginalStr`,`OriginalCon`,`OriginalDex`,`OriginalCha`,`OriginalInt`,`OriginalWis`)
VALUES ('ci_player',0,'CiKnight',90000001,50,50,500,500,100,100,-10,18,16,12,8,8,12,61,0,1,33440,32798,4,40,0,18,16,12,8,8,12),
       ('ci_gm',200,'CiWizard',90000002,50,50,400,400,300,300,-5,8,12,8,8,18,16,734,0,3,33441,32798,4,40,0,8,12,8,8,18,16);

INSERT INTO `character_other` (`char_obj_id`,`deathCount`,`killCount`) VALUES (90000001,0,0),(90000002,0,0);
