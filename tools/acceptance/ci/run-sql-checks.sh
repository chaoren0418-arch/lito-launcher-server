#!/usr/bin/env bash
# CI only (T128 / Issue #379): run tools/acceptance/m4-db-*.sql and m4-seed-exp.sql against a throwaway MySQL 8.0
# listening on 43324 with the same server options as P24Lab's mysql.ini (sql-mode, collation, bind, skip-name-resolve).
# The data is the repository's CI fixture renamed to the experiment schema names; nothing here touches P24Lab.
# Credentials below are fixed, non-secret test values for this disposable container only.
set -euo pipefail

ROOT_PW='m4_ci_root_test_only'
EXP_PW='m4_ci_exp_test_only'
CONTAINER='m4sql'
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS="$(dirname "$HERE")"
WORK="${RUNNER_TEMP:-/tmp}/m4-sql"
mkdir -p "$WORK"

root() { docker exec -i "$CONTAINER" mysql --protocol=TCP -h127.0.0.1 -P43324 -uroot -p"$ROOT_PW" --default-character-set=utf8mb4 "$@"; }
exp() { docker exec -i "$CONTAINER" mysql --protocol=TCP -h127.0.0.1 -P43324 -ulito83_exp -p"$EXP_PW" --default-character-set=utf8mb4 "$@"; }
fail() { echo "::error::$*"; exit 1; }
expect_guard() { # name, expected guard text, command...
  local name="$1" text="$2"; shift 2
  if "$@" > "$WORK/$name.out" 2> "$WORK/$name.err"; then fail "$name: expected the guard to abort, but the script succeeded"; fi
  grep -qi "$text" "$WORK/$name.err" || { cat "$WORK/$name.err"; fail "$name: guard text '$text' not found"; }
  echo "ok   $name (aborted by guard: $text)"
}

echo "::group::start MySQL 8.0 on 43324"
docker run -d --name "$CONTAINER" -e MYSQL_ROOT_PASSWORD="$ROOT_PW" mysql:8.0 \
  --port=43324 --bind-address=127.0.0.1 --skip-name-resolve --mysqlx=0 --skip-log-bin \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --sql-mode=NO_ENGINE_SUBSTITUTION \
  --lower-case-table-names=1 > /dev/null
for attempt in $(seq 1 90); do
  if docker exec "$CONTAINER" mysql --protocol=TCP -h127.0.0.1 -P43324 -uroot -p"$ROOT_PW" -e 'SELECT 1' > /dev/null 2>&1; then break; fi
  [ "$attempt" = 90 ] && { docker logs "$CONTAINER" | tail -n 40; fail 'MySQL did not start'; }
  sleep 2
done
root -e "SELECT VERSION(), @@port, @@bind_address, @@sql_mode, @@collation_server, @@lower_case_table_names;"
echo "::endgroup::"

echo "::group::load fixture (repository CI data renamed to *_lito_exp)"
python3 "$HERE/build_m4_fixture.py" --out "$WORK/fixture"
for file in "$WORK"/fixture/*.sql; do
  start=$(date +%s); root < "$file"; echo "loaded $(basename "$file") in $(( $(date +%s) - start ))s"
done
root -e "CREATE USER 'lito83_exp'@'127.0.0.1' IDENTIFIED BY '$EXP_PW';"
root -e "SELECT table_schema, COUNT(*) AS tables FROM information_schema.TABLES WHERE table_schema LIKE '%\_lito\_exp' GROUP BY table_schema;"
echo "::endgroup::"

NOUUID="SET @m4_expected_uuid=''"

echo "::group::guards must abort"
expect_guard guard-uuid 'm4_guard__server_uuid_is_not_the_recorded_p24lab' root < "$TOOLS/m4-db-prepare.sql"
expect_guard guard-schema-prepare 'm4_guard__schemas_must_be_distinct_and_end_with_lito_exp' root --init-command="SET @m4_expected_uuid='', @m4_static='380_db'" < "$TOOLS/m4-db-prepare.sql"
expect_guard guard-schema-verify 'm4_guard__schemas_must_be_distinct_and_end_with_lito_exp' root --init-command="SET @m4_expected_uuid='', @m4_ac='p24_accounts'" < "$TOOLS/m4-db-verify.sql"
expect_guard guard-schema-seed 'm4_guard__schemas_must_be_distinct_and_end_with_lito_exp' root --init-command="SET @m4_expected_uuid='', @m4_lito='380_lito'" < "$TOOLS/m4-seed-exp.sql"
expect_guard guard-missing-schema 'm4_guard__an_experiment_schema_is_missing_import_snapshot' root --init-command="SET @m4_expected_uuid='', @m4_lito='p24_380lito_lito_exp'" < "$TOOLS/m4-db-prepare.sql"
expect_guard guard-grantee 'm4_guard__grantee_must_be_loopback_and_mode_table_or_schema' root --init-command="SET @m4_expected_uuid='', @m4_grantee='''lito83_exp''@''%'''" < "$TOOLS/m4-db-prepare.sql"
expect_guard guard-seed-placeholders 'm4_guard__logins_not_substituted_load_private_vars_first' root --init-command="$NOUUID" < "$TOOLS/m4-seed-exp.sql"
expect_guard guard-seed-example-vars 'm4_guard__logins_not_substituted_load_private_vars_first' bash -c "cat '$TOOLS/m4-seed-vars.example.sql' '$TOOLS/m4-seed-exp.sql' | docker exec -i $CONTAINER mysql --protocol=TCP -h127.0.0.1 -P43324 -uroot -p'$ROOT_PW' --default-character-set=utf8mb4 --init-command=\"$NOUUID\""
printf "SET @m4_gm_login='m4gm', @m4_a_login='m4a', @m4_b_login='m4b';\n" > "$WORK/vars-no-password.sql"
expect_guard guard-seed-password 'm4_guard__password_placeholder_for_account_to_create' bash -c "cat '$WORK/vars-no-password.sql' '$TOOLS/m4-seed-exp.sql' | docker exec -i $CONTAINER mysql --protocol=TCP -h127.0.0.1 -P43324 -uroot -p'$ROOT_PW' --default-character-set=utf8mb4 --init-command=\"$NOUUID\""
# Nothing may have been written by the aborted runs.
[ "$(root -N -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='p24_accounts_lito_exp' AND TABLE_NAME='character_lito' AND COLUMN_NAME='ranking_points'")" = 0 ] || fail 'an aborted prepare still altered character_lito'
[ "$(root -N -e "SELECT COUNT(*) FROM p24_accounts_lito_exp.accounts WHERE login LIKE 'm4%'")" = 0 ] || fail 'an aborted seed still inserted accounts'
echo "::endgroup::"

echo "::group::verify before prepare (must FAIL)"
root --table --init-command="$NOUUID" < "$TOOLS/m4-db-verify.sql" > "$WORK/verify-before.txt"
cat "$WORK/verify-before.txt"
grep -q 'M4-VERIFY: FAIL' "$WORK/verify-before.txt" || fail 'verify should fail before prepare'
grep -q 'ranking_points' "$WORK/verify-before.txt" || fail 'verify should list the missing ranking columns'
echo "::endgroup::"

echo "::group::prepare (twice: second run must be a no-op) and verify (must PASS)"
root --table --init-command="$NOUUID" < "$TOOLS/m4-db-prepare.sql" > "$WORK/prepare-1.txt"
cat "$WORK/prepare-1.txt"
grep -q 'M4-PREPARE: DONE' "$WORK/prepare-1.txt" || fail 'prepare did not finish'
root -N -e "SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='p24_accounts_lito_exp' AND TABLE_NAME='character_lito' ORDER BY ORDINAL_POSITION; SELECT * FROM p24_lito_lito_exp.lito_source_server; SHOW GRANTS FOR 'lito83_exp'@'127.0.0.1';" > "$WORK/state-1.txt"
root --table --init-command="$NOUUID" < "$TOOLS/m4-db-prepare.sql" > "$WORK/prepare-2.txt"
root -N -e "SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='p24_accounts_lito_exp' AND TABLE_NAME='character_lito' ORDER BY ORDINAL_POSITION; SELECT * FROM p24_lito_lito_exp.lito_source_server; SHOW GRANTS FOR 'lito83_exp'@'127.0.0.1';" > "$WORK/state-2.txt"
diff "$WORK/state-1.txt" "$WORK/state-2.txt" || fail 'prepare is not idempotent'
[ "$(root -N -e "SELECT CONCAT(COUNT(*), ':', SUM(enabled), ':', SUM(enabled = 1 AND character_database = 'p24_accounts_lito_exp')) FROM p24_lito_lito_exp.lito_source_server WHERE enabled = 1 OR character_database = 'p24_accounts_lito_exp'")" = '1:1:1' ] || fail 'route was not reduced to the single experiment source'
root --table --init-command="$NOUUID" < "$TOOLS/m4-db-verify.sql" > "$WORK/verify-after-prepare.txt"
cat "$WORK/verify-after-prepare.txt"
grep -q 'M4-VERIFY: PASS' "$WORK/verify-after-prepare.txt" || fail 'verify should pass after prepare'
echo "::endgroup::"

echo "::group::least privilege as the experiment account"
exp --table --init-command="$NOUUID" < "$TOOLS/m4-db-verify.sql" > "$WORK/verify-as-exp.txt"
grep -q 'M4-VERIFY: PASS' "$WORK/verify-as-exp.txt" || { cat "$WORK/verify-as-exp.txt"; fail 'verify should also pass when run as the experiment account'; }
exp -e "SELECT COUNT(*) FROM p24_accounts_lito_exp.character_lito; SELECT COUNT(*) FROM p24_lito_lito_exp.lito_battle_settlement; SELECT COUNT(*) FROM p24_380db_lito_exp.etcitem;" > /dev/null
if exp -e "UPDATE p24_accounts_lito_exp.characters SET level = level WHERE 1 = 0" 2> /dev/null; then fail 'experiment account can write characters'; fi
if exp -e "UPDATE p24_380db_lito_exp.etcitem SET weight = weight WHERE 1 = 0" 2> /dev/null; then fail 'experiment account can write the static database'; fi
if exp -e "DELETE FROM p24_accounts_lito_exp.character_lito WHERE 1 = 0" 2> /dev/null; then fail 'experiment account can delete from character_lito'; fi
exp -e "UPDATE p24_accounts_lito_exp.character_lito SET wins = wins WHERE 1 = 0; UPDATE p24_lito_lito_exp.lito_battle_settlement SET applied = applied WHERE 1 = 0;"
UNLISTED="$(root -N -e "SELECT t.TABLE_NAME FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA='p24_380db_lito_exp' AND NOT EXISTS (SELECT 1 FROM information_schema.TABLE_PRIVILEGES p WHERE p.TABLE_SCHEMA=t.TABLE_SCHEMA AND p.TABLE_NAME=t.TABLE_NAME AND p.GRANTEE='''lito83_exp''@''127.0.0.1''') ORDER BY t.TABLE_NAME LIMIT 1")"
[ -n "$UNLISTED" ] || fail 'expected at least one static table outside the grant list'
if exp -e "SELECT 1 FROM p24_380db_lito_exp.\`$UNLISTED\` LIMIT 0" 2> /dev/null; then fail "table mode: experiment account can read unlisted table $UNLISTED"; fi
echo "ok   table mode denies unlisted static table $UNLISTED"
echo "::endgroup::"

echo "::group::seed (twice) and verify"
cat > "$WORK/m4-seed-vars.sql" <<'SQL'
SET @m4_gm_login = 'm4gm', @m4_gm_password = 'ci_gm_pw_test_only';
SET @m4_a_login = 'ci_player', @m4_a_password = '__M4_A_PASSWORD__';
SET @m4_b_login = 'm4b', @m4_b_password = 'ci_b_pw_test_only';
SQL
seed() { cat "$WORK/m4-seed-vars.sql" "$TOOLS/m4-seed-exp.sql" | root --table --init-command="$NOUUID"; }
seed > "$WORK/seed-1.txt"
cat "$WORK/seed-1.txt"
grep -q 'M4-SEED: DONE' "$WORK/seed-1.txt" || fail 'seed did not finish'
COUNTS="SELECT CONCAT_WS(':', (SELECT COUNT(*) FROM p24_accounts_lito_exp.accounts), (SELECT COUNT(*) FROM p24_accounts_lito_exp.characters WHERE objid BETWEEN 190379001 AND 190379009), (SELECT COUNT(*) FROM p24_accounts_lito_exp.character_skills WHERE char_obj_id BETWEEN 190379001 AND 190379009), (SELECT COUNT(*) FROM p24_accounts_lito_exp.character_items WHERE id BETWEEN 190379101 AND 190379112), (SELECT COUNT(*) FROM p24_lito_lito_exp.commands WHERE name IN ('polyme','move','loc','item','spawn','de')), (SELECT COUNT(*) FROM p24_accounts_lito_exp.characters WHERE account_name = 'ci_player' AND Banned = 0), (SELECT password FROM p24_accounts_lito_exp.accounts WHERE login = 'ci_player'), (SELECT access_level FROM p24_accounts_lito_exp.accounts WHERE login = 'm4gm'))"
FIRST="$(root -N -e "$COUNTS")"
echo "after first seed: $FIRST"
# accounts 2 fixture + 2 new; 9 characters; 7 skills; 12 item rows; 6 commands; ci_player 1 + 7 = 8 characters; existing password untouched; GM level 200
[ "$FIRST" = '4:9:7:12:6:8:ci_player_pw:200' ] || fail "unexpected seed result: $FIRST"
seed > "$WORK/seed-2.txt"
[ "$(root -N -e "$COUNTS")" = "$FIRST" ] || fail 'seed is not idempotent'
root --table --init-command="$NOUUID" < "$TOOLS/m4-db-verify.sql" > "$WORK/verify-after-seed.txt"
tail -n 12 "$WORK/verify-after-seed.txt"
grep -q 'M4-VERIFY: PASS' "$WORK/verify-after-seed.txt" || fail 'verify should pass after seed'
# The seeded inventory must satisfy the server's own rule: every item row joins exactly one template and count >= 1.
[ "$(root -N -e "SELECT COUNT(*) FROM p24_accounts_lito_exp.character_items i LEFT JOIN p24_380db_lito_exp.etcitem e ON e.item_id = i.item_id WHERE i.id BETWEEN 190379101 AND 190379112 AND (e.item_id IS NULL OR i.count IS NULL OR i.count < 1)")" = 0 ] || fail 'seeded an item row without a template or with an invalid count'
[ "$(root -N -e "SELECT COUNT(*) FROM p24_accounts_lito_exp.character_items i JOIN p24_380db_lito_exp.etcitem e ON e.item_id = i.item_id WHERE e.classname = 'wand.Storm_Walk' AND i.char_id = 190379001")" = 1 ] || fail 'GM wand was not seeded'
echo "::endgroup::"

echo "::group::schema grant mode (#284 ruling) is additive and still verifies"
root --table --init-command="SET @m4_expected_uuid='', @m4_grant_mode='schema'" < "$TOOLS/m4-db-prepare.sql" > "$WORK/prepare-schema-mode.txt"
exp -e "SELECT 1 FROM p24_380db_lito_exp.\`$UNLISTED\` LIMIT 0" > /dev/null
exp --table --init-command="$NOUUID" < "$TOOLS/m4-db-verify.sql" > "$WORK/verify-schema-mode.txt"
tail -n 5 "$WORK/verify-schema-mode.txt"
grep -q 'M4-VERIFY: PASS' "$WORK/verify-schema-mode.txt" || fail 'verify should pass in schema grant mode'
echo "::endgroup::"

echo "::group::snapshot SQL + m4-sql-diff against real mysql --batch output"
pwsh -NoProfile -Command ". '$TOOLS/m4-common.ps1'; [IO.File]::WriteAllText('$WORK/snapshot-before.sql', (Get-M4SnapshotSql -LitoDatabase p24_lito_lito_exp -AccountDatabase p24_accounts_lito_exp -Label before-15)); [IO.File]::WriteAllText('$WORK/snapshot-after.sql', (Get-M4SnapshotSql -LitoDatabase p24_lito_lito_exp -AccountDatabase p24_accounts_lito_exp -Label after-15))"
exp --batch < "$WORK/snapshot-before.sql" > "$WORK/sql-before-15.txt"
# Emulate one settled battle exactly as SettlementSql.drain writes it: M4DK wins (+250), M4IL loses by disconnect (-50, floor 0).
root <<'SQL'
SET @period = DATE_FORMAT(UTC_TIMESTAMP(), '%Y%m') + 0;
INSERT INTO p24_lito_lito_exp.lito_battle_settlement (result_id, battle_id, source_database, source_name, char_obj_id, char_name, won, disconnected, applied, applied_at)
VALUES (REPEAT('a', 64), '00000000-0000-0000-0000-000000000001', 'p24_accounts_lito_exp', 'CI', 190379002, 'M4DK', 1, 0, 1, CURRENT_TIMESTAMP),
       (REPEAT('b', 64), '00000000-0000-0000-0000-000000000001', 'p24_accounts_lito_exp', 'CI', 190379009, 'M4IL', 0, 1, 1, CURRENT_TIMESTAMP);
INSERT INTO p24_accounts_lito_exp.character_lito (char_obj_id, char_name, wins, losses, ranking_points, disconnects, monthly_period, monthly_ranking_points, monthly_wins, monthly_losses, version, last_result_id)
VALUES (190379002, 'M4DK', 1, 0, 250, 0, @period, 250, 1, 0, 1, REPEAT('a', 64)),
       (190379009, 'M4IL', 0, 1, 0, 1, @period, 0, 0, 1, 1, REPEAT('b', 64));
SQL
exp --batch < "$WORK/snapshot-after.sql" > "$WORK/sql-after-15.txt"
cat "$WORK/sql-after-15.txt"
pwsh -NoProfile -File "$TOOLS/m4-sql-diff.ps1" -Before "$WORK/sql-before-15.txt" -After "$WORK/sql-after-15.txt" -ExpectWinner M4DK -ExpectLoser M4IL || fail 'm4-sql-diff should PASS on a correct settlement'
# A tampered counter must be caught.
root -e "UPDATE p24_accounts_lito_exp.character_lito SET wins = wins + 1 WHERE char_obj_id = 190379002"
pwsh -NoProfile -Command ". '$TOOLS/m4-common.ps1'; [IO.File]::WriteAllText('$WORK/snapshot-bad.sql', (Get-M4SnapshotSql -LitoDatabase p24_lito_lito_exp -AccountDatabase p24_accounts_lito_exp -Label after-15-bad))"
exp --batch < "$WORK/snapshot-bad.sql" > "$WORK/sql-after-15-bad.txt"
if pwsh -NoProfile -File "$TOOLS/m4-sql-diff.ps1" -Before "$WORK/sql-before-15.txt" -After "$WORK/sql-after-15-bad.txt" > "$WORK/diff-bad.txt"; then cat "$WORK/diff-bad.txt"; fail 'm4-sql-diff should FAIL when wins is off by one'; fi
grep -q 'M4-SQL-DIFF: FAIL' "$WORK/diff-bad.txt" || fail 'm4-sql-diff did not report FAIL'
echo "ok   m4-sql-diff PASS on correct settlement, FAIL on tampered counters"
echo "::endgroup::"

echo "M4-CI-SQL: PASS"
