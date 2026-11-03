-- Dedicated LiTo modern layout schema v1. Explicit administrator migration only.
-- Never run against a source/auth/catalog schema; the modern listener NEVER runs DDL or publishes rows.
-- Grant the runtime principal SELECT on these two tables only. Publish a new immutable revision
-- transactionally, then configure its namespace/revision/content SHA and restart the listener.
-- Do not update a revision in place. A new revision is not a live reload and is not an outbox namespace.
CREATE TABLE lito_modern_world_policy (
    namespace_sha256 BINARY(32) NOT NULL,
    revision BIGINT NOT NULL,
    schema_version INT NOT NULL,
    binding_count INT NOT NULL,
    content_sha256 BINARY(32) NOT NULL,
    published TINYINT NOT NULL,
    PRIMARY KEY (namespace_sha256, revision)
) ENGINE=InnoDB;

CREATE TABLE lito_modern_world_layout (
    namespace_sha256 BINARY(32) NOT NULL,
    revision BIGINT NOT NULL,
    mode INT NOT NULL,
    logical_map_id INT NOT NULL,
    physical_map_id INT NOT NULL,
    manifest_sha256 BINARY(32) NOT NULL,
    anchor_x INT NOT NULL,
    anchor_y INT NOT NULL,
    camp0_x INT NULL,
    camp0_y INT NULL,
    camp1_x INT NULL,
    camp1_y INT NULL,
    load_timeout_millis INT NULL,
    countdown_nanos BIGINT NULL,
    active_timeout_millis INT NULL,
    PRIMARY KEY (namespace_sha256, revision, mode, logical_map_id)
) ENGINE=InnoDB;

-- Semantic validation is mandatory in Java, including MySQL versions that ignore CHECK:
-- schema=1, published=1, revision>0, count 1..40, exact complete locally offered binding set.
-- All hashes are nonzero 32 bytes. All four camp columns are NULL together or present together;
-- camps are allowed only for team modes. SQL cannot provide tower/gfx/HP/features or sector paths.
-- Each SQL NULL clock retains its local binding override; only a remaining NULL uses the global limit.
-- Bounds: load 50..30000 ms, countdown 0..30000000000 ns, active 50..1800000 ms.
-- The content SHA uses ModernWorldPolicy.contentHash: DataOutputStream big-endian, writeUTF domain
-- LITO_MODERN_WORLD_POLICY_V1, i32 schema1, namespace32, i64 revision, i32 rowcount, sorted rows:
-- i32 mode/logical/physical, manifest32, i32 anchorX/Y, bool campsPresent + 4*i32 when present,
-- bool loadPresent + i32, bool countdownPresent + i64, bool activePresent + i32.
