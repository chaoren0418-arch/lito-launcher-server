# 0.6.42 unified configuration

Canonical Java sources: server/tools/shared-database/UnifiedConfiguration.java and SharedDatabase.java. build.py patches the pinned 0.6.41 core and preserves all earlier gameplay fixes. ClientSession and native DLL are unchanged.

Only config/server.properties remains as operator controls. SQL lito_source_server provides fixed native IDs 1..8, labels, character schemas and enable switches. GUI/BAT/core share one Java reader. Legacy repository aliases are created only in memory. Runtime capture overlays contain a canonical path and capture directory, no credentials. Unknown or duplicate settings fail closed.

ConfigurationRegression tests actual SQL reads/config consumption and invalid settings. MigrateSources is a one-time, idempotent LiTo-only migration; the portable SQL is server/db/lito_source_server.sql. configuration-before.zip is the compact pre-migration backup (contains credentials; keep local). Historical shared-database/build.py must not replace this release.
