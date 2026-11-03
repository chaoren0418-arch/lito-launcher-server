# 0.6.41 runtime lifecycle

Run build.py against its pinned 0.6.40 base. It compiles RuntimeLifecycle and the canonical server tools/shared-database/SharedDatabase.java, then replaces only those classes and five ClientSession call sites. Native binaries are unchanged.

LifecycleRegression: 100 memory purchase/reset cycles, same-session duplicate settlement, source profile release, no state file, actual account-only roster hook and removed-identity pruning. StatsEngineRegression: settlement write gate and capability cleanup, invalid +2 result, eight InnoDB/MyISAM insert/update/race cases using connection-local temporary tables. repair-040/Regression: live lobby/ranking packet and rank scope. No player result test writes.

The old server JAR is retained only as build/base.jar for reproducible patching/rollback; no copied game assets or extra release directory.
