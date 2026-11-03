package local.lito381.server;

import java.util.List;

/** Exact selected-source castle-gate restriction during an active war. */
final class ClassicWorldCastleGateRules {
    private ClassicWorldCastleGateRules() {}

    /**
     * {@code C_NPCAction.openCloseGate} skips only a castle door whose
     * {@code isNowWar} is true and whose source {@code maxHp} is greater than
     * one.  Zero-HP scenery/inner gates remain operator-controlled in war.
     */
    static boolean blocksToggle(boolean isNowWar, int maximumHp) {
        if (maximumHp < 0) {
            throw new IllegalArgumentException("negative castle-door maximum HP");
        }
        return isNowWar && maximumHp > 1;
    }

    static boolean blocksKeeperToggle(
            int castleId, int keeperNpcId, boolean isNowWar) {
        List<Integer> maximumHp = switch (castleId) {
            case ClassicWorldHeineCatalog.CASTLE_ID ->
                    ClassicWorldHeineCatalog.doors().stream()
                            .filter(door -> door.keeperNpcId() == keeperNpcId)
                            .map(ClassicWorldHeineCatalog.Door::maximumHp)
                            .toList();
            case ClassicWorldDwarfCastleCatalog.CASTLE_ID ->
                    ClassicWorldDwarfCastleCatalog.doors().stream()
                            .filter(door -> door.keeperNpcId() == keeperNpcId)
                            .map(ClassicWorldDwarfCastleCatalog.Door::maximumHp)
                            .toList();
            case ClassicWorldAdenToiCatalog.CASTLE_ID -> {
                ClassicWorldAdenToiCatalog.Door door =
                        ClassicWorldAdenToiCatalog.innerGate();
                yield door.keeperNpcId() == keeperNpcId
                        ? List.of(door.maximumHp()) : List.of();
            }
            default -> List.of();
        };
        if (maximumHp.size() != 1) {
            throw new IllegalArgumentException(
                    "keeper must resolve to exactly one retained castle door: castle="
                            + castleId + " keeper=" + keeperNpcId
                            + " matches=" + maximumHp.size());
        }
        return blocksToggle(isNowWar, maximumHp.get(0));
    }
}
