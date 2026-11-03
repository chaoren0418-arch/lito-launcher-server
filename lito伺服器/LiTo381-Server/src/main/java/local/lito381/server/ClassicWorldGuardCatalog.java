package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact live {@code L1GuardInstance} rows on every currently imported map. */
final class ClassicWorldGuardCatalog {
    static final int POPULATION = 190;
    static final int STATIC_ZERO_HP_POPULATION = 66;
    static final int MISSING_MAP_2005_POPULATION = 8;
    static final long WANTED_SECONDS = 60L * 60L;

    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String RETAINED_GUARD_SOURCE_SHA256 =
            "14075C2AAEDF6D10CE81854D0D0F5B385E76AB583C24D1B134D77698CBA39EA6";
    static final String SECONDARY_GUARD_SOURCE_SHA256 =
            "5D9AFAE16F9C3DAC8A24116AEADA26C522967ECF7B36FFDAA95F7896D35EB8AF";
    static final String RETAINED_PC_DEATH_SOURCE_SHA256 =
            "D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F";

    static final Map<Integer, Integer> POPULATION_BY_MAP = Map.of(
            3, 2,
            4, 158,
            29, 8,
            52, 8,
            64, 8,
            66, 6);

    private static final Set<Integer> RANGE_TEN = Set.of(
            60501, 60509, 60517, 60524, 60528,
            60533, 60538, 60539, 60551);
    private static final Set<Integer> RANGE_ONE = Set.of(
            60530, 60531, 60540, 60560,
            70857, 70862, 70863, 70986, 70992,
            70993, 70994, 70995, 70996);
    private static final Set<Integer> SOURCE_AGGRESSIVE = Set.of(60560, 70986);

    enum TalkArguments {
        NONE,
        PLAYER_NAME,
        CASTLE_IDENTITY
    }

    record TalkProfile(
            int castleId, String memberHtml, String outsiderHtml,
            TalkArguments arguments) {
        TalkProfile {
            if (castleId < 1 || castleId > 8 || memberHtml == null
                    || memberHtml.isBlank() || outsiderHtml == null
                    || arguments == null) {
                throw new IllegalArgumentException("invalid L1Guard talk profile");
            }
        }
    }

    private static final List<ClassicWorldNpcCatalog.StaticNpc> NPCS = loadNpcs();
    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition> DEFINITIONS =
            NPCS.stream().map(ClassicWorldGuardCatalog::definition).toList();

    static {
        if (NPCS.size() != POPULATION) {
            throw new ExceptionInInitializerError(
                    "selected live guard population changed actual=" + NPCS.size());
        }
        Map<Integer, Integer> actual = new LinkedHashMap<>();
        for (ClassicWorldNpcCatalog.StaticNpc npc : NPCS) {
            actual.merge(npc.mapId(), 1, Math::addExact);
        }
        if (!actual.equals(POPULATION_BY_MAP)) {
            throw new ExceptionInInitializerError(
                    "selected live guard map distribution changed actual=" + actual);
        }
        if (NPCS.stream().map(ClassicWorldNpcCatalog.StaticNpc::npcId)
                .distinct().count() != 44) {
            throw new ExceptionInInitializerError(
                    "selected live guard template count changed");
        }
    }

    private ClassicWorldGuardCatalog() {}

    static List<ClassicWorldNpcCatalog.StaticNpc> all() {
        return NPCS;
    }

    static List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions() {
        return DEFINITIONS;
    }

    static boolean isGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.hp() > 0
                && npc.implementation().equals("L1Guard")
                && POPULATION_BY_MAP.containsKey(npc.mapId());
    }

    static boolean isGuard(ClassicWorldMonsterPopulation.Snapshot actor) {
        return actor != null && actor.template().guard();
    }

    static boolean wanted(long lastPkEpochSeconds, long nowEpochSeconds) {
        return lastPkEpochSeconds > 0 && nowEpochSeconds >= lastPkEpochSeconds
                && nowEpochSeconds - lastPkEpochSeconds <= WANTED_SECONDS;
    }

    static int attackRange(int npcId) {
        if (RANGE_TEN.contains(npcId)) return 10;
        if (RANGE_ONE.contains(npcId)) return 1;
        return 2;
    }

    static int bowActionId(int npcId) {
        return RANGE_TEN.contains(npcId) ? 66 : 0;
    }

    static boolean sourceAggressive(int npcId) {
        return SOURCE_AGGRESSIVE.contains(npcId);
    }

    /** Exact retained L1GuardInstance.onTalkAction castle branches. */
    static TalkProfile talkProfile(int npcId) {
        return switch (npcId) {
            case 70549, 70985 -> gate(1, "gateokeeper");
            case 70656 -> gate(1, "gatekeeper");
            case 70600, 70986 -> new TalkProfile(
                    2, "orckeeper", "orckeeperop", TalkArguments.NONE);
            case 70687, 70987 -> gate(3, "gateokeeper");
            case 70778 -> gate(3, "gatekeeper");
            case 70800, 70988, 70989, 70990, 70991 ->
                    gate(4, "gateokeeper");
            case 70817 -> gate(4, "gatekeeper");
            case 70862, 70992 -> gate(5, "gateokeeper");
            case 70863 -> gate(5, "gatekeeper");
            case 70993, 70994 -> gate(6, "gateokeeper");
            case 70995 -> gate(6, "gatekeeper");
            case 70996 -> gate(7, "gatekeeper");
            case 60514 -> castleGuard(1, "ktguard6");
            case 60560 -> castleGuard(2, "orcguard6");
            case 60552 -> castleGuard(3, "wdguard6");
            case 60524, 60525, 60529 -> castleGuard(4, "grguard6");
            case 70857 -> castleGuard(5, "heguard6");
            case 60530, 60531 -> castleGuard(6, "dcguard6");
            case 60533, 60534 -> castleGuard(7, "adguard6");
            case 81156 -> castleGuard(8, "ktguard6");
            default -> null;
        };
    }

    private static TalkProfile gate(int castleId, String memberHtml) {
        return new TalkProfile(castleId, memberHtml, "gatekeeperop",
                TalkArguments.PLAYER_NAME);
    }

    private static TalkProfile castleGuard(int castleId, String html) {
        return new TalkProfile(castleId, html, html,
                TalkArguments.CASTLE_IDENTITY);
    }

    private static List<ClassicWorldNpcCatalog.StaticNpc> loadNpcs() {
        List<ClassicWorldNpcCatalog.StaticNpc> result = new ArrayList<>();
        for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
            for (ClassicWorldNpcCatalog.StaticNpc npc
                    : ClassicWorldNpcCatalog.all(mapId)) {
                if (isGuard(npc)) result.add(npc);
            }
        }
        return List.copyOf(result);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition definition(
            ClassicWorldNpcCatalog.StaticNpc npc) {
        int attackRange = attackRange(npc.npcId());
        int bowActionId = bowActionId(npc.npcId());
        boolean sourceAggressive = sourceAggressive(npc.npcId());
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        npc.npcId(), npc.nameId(), "L1Guard", npc.gfxId(),
                        npc.level(), npc.hp(), npc.mp(), 0,
                        126, 126, 126, 126, 126, 0,
                        npc.experience(), npc.lawful(), "large",
                        sourceAggressive ? 4 : 0, attackRange,
                        npc.passispeed(), npc.atkspeed(), bowActionId, 0,
                        0, 0, sourceAggressive, 0,
                        false, false, false,
                        npc.lightSize(), npc.changeHead());
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                npc.mapId(), npc.spawnId(), "selected-L1Guard", 1, 0,
                npc.x(), npc.y(), 0, 0, 0, 0, 0, 0, npc.heading(),
                0, 0, false, npc.movementDistance(), false, 0, template);
    }
}
