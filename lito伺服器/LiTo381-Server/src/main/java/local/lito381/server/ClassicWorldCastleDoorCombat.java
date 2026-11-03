package local.lito381.server;

/** Shared retained {@code L1DoorInstance} combat geometry and crack lifecycle. */
final class ClassicWorldCastleDoorCombat {
    /** Retained {@code DoorSpawnTable} creates every door from this NPC template. */
    static final int NPC_TEMPLATE_ID = 81158;

    enum Family { KENT, WINDAWOOD, GIRAN, HEINE }

    record Definition(Family family, int castleId, int doorId, long objectId,
                      int gfxId, int x, int y, int mapId, int direction,
                      int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Definition {
            if (family == null || castleId < 1 || castleId > 8 || doorId <= 0
                    || objectId <= 0 || gfxId <= 0 || x <= 0 || y <= 0
                    || mapId < 0 || direction < 0 || direction > 1
                    || edgeLow <= 0 || edgeHigh < edgeLow || maximumHp < 0
                    || keeperNpcId <= 0) {
                throw new IllegalArgumentException("invalid castle door definition");
            }
        }

        boolean controls(int cellX, int cellY) {
            return direction == 0
                    ? cellY == entranceY()
                        && cellX >= edgeLow - 1 && cellX <= edgeHigh + 1
                    : cellX == entranceX()
                        && cellY >= edgeLow - 1 && cellY <= edgeHigh + 1;
        }

        int entranceX() { return direction == 0 ? x : x - 1; }
        int entranceY() { return direction == 0 ? y + 1 : y; }
    }

    record View(Definition definition, int currentHp, boolean open, int crackStage) {
        View {
            if (definition == null || currentHp < 0
                    || currentHp > definition.maximumHp()
                    || crackStage < 0 || crackStage > 5
                    || currentHp == 0 && definition.maximumHp() > 1 && !open) {
                throw new IllegalArgumentException("invalid castle door view");
            }
        }

        boolean destructible() { return definition.maximumHp() > 1; }
    }

    record Hit(View before, View after, int appliedDamage, int action) {
        Hit {
            if (before == null || after == null || appliedDamage < 0
                    || before.definition().objectId() != after.definition().objectId()
                    || before.currentHp() - after.currentHp() != appliedDamage
                    || action < 0 || action > 0xFF) {
                throw new IllegalArgumentException("invalid castle door hit");
            }
        }

        boolean diedNow() {
            return before.currentHp() > 0 && after.currentHp() == 0;
        }
    }

    record DamageResult(int nextHp, boolean nextOpen, int nextCrackStage,
                        int appliedDamage, int action) {}

    record Target(View view, ClassicWorldMonsterPopulation.Snapshot combat) {}

    private ClassicWorldCastleDoorCombat() {}

    static Target target(View view, int attackerX, int attackerY) {
        if (view == null || !view.destructible() || view.currentHp() <= 0) return null;
        Definition door = view.definition();
        int combatX = door.direction() == 0
                ? clamp(attackerX, door.edgeLow(), door.edgeHigh()) : door.entranceX();
        int combatY = door.direction() == 0
                ? door.entranceY() : clamp(attackerY, door.edgeLow(), door.edgeHigh());
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        NPC_TEMPLATE_ID, "", "L1Monster", door.gfxId(),
                        1, door.maximumHp(), 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        "large", 0, 0, 0, 0, 0, 0,
                        0, 0, false, 0, false, false, false, 0, false);
        ClassicWorldMonsterCatalog.SpawnDefinition spawn =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        door.mapId(), door.doorId(),
                        door.family().name() + " Castle door", 1, 0,
                        combatX, combatY, 0, 0, 0, 0, 0, 0,
                        door.direction(), 0, 0, false, 0, true, 0, template);
        ClassicWorldMonsterPopulation.Snapshot combat =
                new ClassicWorldMonsterPopulation.Snapshot(
                        door.mapId(), door.doorId(), 1, door.objectId(),
                        NPC_TEMPLATE_ID,
                        combatX, combatY, door.direction(), view.currentHp(),
                        door.maximumHp(), 0, 0, true, true, spawn);
        return new Target(view, combat);
    }

    static DamageResult damage(View before, int damage) {
        if (before == null || damage <= 0 || !before.destructible()
                || before.currentHp() <= 0) return null;
        int nextHp = Math.max(0, before.currentHp() - damage);
        int applied = before.currentHp() - nextHp;
        boolean died = nextHp == 0;
        int nextCrack = died ? before.crackStage()
                : crackStage(nextHp, before.definition().maximumHp());
        int action = died ? BattleActionCodes.DOOR_DIE
                : nextCrack != 0 && nextCrack != before.crackStage()
                    ? BattleActionCodes.DOOR_CRACK_1 + nextCrack - 1 : 0;
        return new DamageResult(nextHp, died || before.open(), nextCrack,
                applied, action);
    }

    static int crackStage(int hp, int maximumHp) {
        if (maximumHp * 1 / 6 > hp) return 5;
        if (maximumHp * 2 / 6 > hp) return 4;
        if (maximumHp * 3 / 6 > hp) return 3;
        if (maximumHp * 4 / 6 > hp) return 2;
        if (maximumHp * 5 / 6 > hp) return 1;
        return 0;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
