package local.lito381.server;

import java.util.List;

/**
 * Exact fixed-arrow apertures retained from {@code L1BowInstance}.
 *
 * <p>The object deliberately has GFX zero and its {@code onPerceive} method
 * does not call the ordinary NPC implementation.  It therefore remains a
 * server-side projectile origin: clients never receive an S_PUT_OBJECT for
 * the aperture itself, but the aperture object id is still the attacker id in
 * each native S_ATTACK projectile.</p>
 */
final class ClassicWorldBowCatalog {
    static final int MAP_ID =
            ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID;
    static final int NPC_ID = 70_558;
    static final int PROJECTILE_GFX = 66;
    static final int ATTACK_ACTION = BattleActionCodes.ATTACK;
    static final int DAMAGE = 15;
    static final int TICK_INTERVAL_MILLIS = 1_500;
    static final long NATIVE_PROJECTILE_SEQUENCE = 0x0000_0152L;

    static final String CURRENT_BOW_SOURCE_SHA256 =
            "32942BBEA50AA8C824639FC37985A46B929A182CFA43B5A42FA9A4DAE3463DF2";
    static final String CURRENT_TIMER_SOURCE_SHA256 =
            "EB5CC4B6C036E9DBC0E15ADE79DDC16691287319A3E9B1A65C90B2468969B509";
    static final String OLDER_BOW_SOURCE_SHA256 =
            "15CEB68232E3BC9404AB5D7F47A7142AA9EF2CF8463BBD0471A2D7F779287F57";
    static final String OLDER_TIMER_SOURCE_SHA256 =
            "378F77A266BA6CA62A784DA1DFF358A70AB5401F603FBC840291189EA662BBC4";

    record Lane(ClassicWorldNpcCatalog.StaticNpc bow,
                int endpointX, int endpointY) {
        Lane {
            if (!isBow(bow) || endpointX < 0 || endpointX > 0xFFFF
                    || endpointY < 0 || endpointY > 0xFFFF) {
                throw new IllegalArgumentException("invalid retained bow lane");
            }
        }

        boolean contains(int x, int y) {
            return switch (bow.heading()) {
                case 0 -> x == bow.x() && y <= bow.y() && y >= endpointY;
                case 2 -> y == bow.y() && x >= bow.x() && x <= endpointX;
                case 4 -> x == bow.x() && y >= bow.y() && y <= endpointY;
                case 6 -> y == bow.y() && x <= bow.x() && x >= endpointX;
                default -> false;
            };
        }

        int projectileX(int targetX) {
            return bow.heading() == 2 || bow.heading() == 6
                    ? targetX : endpointX;
        }

        int projectileY(int targetY) {
            return bow.heading() == 0 || bow.heading() == 4
                    ? targetY : endpointY;
        }
    }

    private ClassicWorldBowCatalog() {}

    static boolean isBow(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID && npc.npcId() == NPC_ID
                && npc.implementation().equals("L1Bow") && npc.gfxId() == 0;
    }

    static List<ClassicWorldNpcCatalog.StaticNpc> all() {
        return ClassicWorldNpcCatalog.all(MAP_ID).stream()
                .filter(ClassicWorldBowCatalog::isBow)
                .toList();
    }

    /**
     * Preserves the retained post-increment/decrement endpoint exactly.  The
     * first raw tile zero is read and then the coordinate advances once more,
     * so the stored endpoint is one cell beyond that zero tile.
     */
    static Lane lane(ClassicWorldNpcCatalog.StaticNpc bow) {
        if (!isBow(bow) || (bow.heading() & 1) != 0) {
            throw new IllegalArgumentException("unsupported retained bow row");
        }
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(bow.mapId());
        int x = bow.x();
        int y = bow.y();
        int dx = switch (bow.heading()) {
            case 2 -> 1;
            case 6 -> -1;
            default -> 0;
        };
        int dy = switch (bow.heading()) {
            case 0 -> -1;
            case 4 -> 1;
            default -> 0;
        };
        int maximumReads = Math.addExact(
                dx == 0 ? map.height() : map.width(), 2);
        for (int reads = 0; reads < maximumReads; reads++) {
            int raw = map.rawCell(x, y);
            x = Math.addExact(x, dx);
            y = Math.addExact(y, dy);
            if (raw == 0) return new Lane(bow, x, y);
        }
        throw new IllegalStateException(
                "retained bow lane did not encounter raw tile zero spawn="
                        + bow.spawnId());
    }
}
