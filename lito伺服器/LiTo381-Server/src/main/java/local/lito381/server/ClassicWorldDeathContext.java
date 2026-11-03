package local.lito381.server;

/** Exact retained pre-penalty and post-penalty ordinary WORLD death gates. */
final class ClassicWorldDeathContext {
    /** ConfigOther.encounter_lv default; no selected other.properties override exists. */
    static final int ENCOUNTER_MAX_LEVEL = 20;
    static final int ENCOUNTER_LEVEL_GAP = 10;

    private ClassicWorldDeathContext() {}

    /**
     * Retained L1PcInstance.Death ordering: only a player attacker can trigger
     * the encounter guardian, and a protected death returns immediately after
     * action 8, before item, skill, EXP and PK bookkeeping.
     */
    static boolean encounterProtects(int victimLevel, int attackerLevel) {
        return victimLevel <= ENCOUNTER_MAX_LEVEL
                && attackerLevel > victimLevel
                && attackerLevel - victimLevel >= ENCOUNTER_LEVEL_GAP;
    }

    /**
     * Retained post-penalty PK gate is broader than active castle war: merely
     * dying inside any castle-war field suppresses PK count/lawful processing.
     */
    static boolean castleAreaSuppressesPk(int mapId, int x, int y) {
        return ClassicWorldCastleWarField.castleIdAt(mapId, x, y) != 0;
    }
}
