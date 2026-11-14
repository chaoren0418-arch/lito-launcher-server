USE p24_accounts_lito_exp;
START TRANSACTION;
DELETE FROM character_lito;
INSERT INTO character_lito (`char_obj_id`,`char_name`,`wins`,`losses`,`version`,`updated_at`,`last_result_id`,`ranking_points`,`disconnects`,`monthly_period`,`monthly_ranking_points`,`monthly_wins`,`monthly_losses`) VALUES ('830001','L83Test1','0','0','0','2026-11-06 11:37:08','','0','0','0','0','0','0');
INSERT INTO character_lito (`char_obj_id`,`char_name`,`wins`,`losses`,`version`,`updated_at`,`last_result_id`,`ranking_points`,`disconnects`,`monthly_period`,`monthly_ranking_points`,`monthly_wins`,`monthly_losses`) VALUES ('830002','L83Test2','0','0','0','2026-11-06 11:37:08','','0','0','0','0','0','0');
COMMIT;
