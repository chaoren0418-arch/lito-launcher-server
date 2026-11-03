package local.lito381.server;

import java.util.List;
import java.util.Map;

/**
 * Exact retained {@code Npc_Dicarding} quest-122 completion contract.
 *
 * <p>This specialized slice intentionally activates only after quest 122 has
 * been materialized.  Dicarding's earlier four class quests continue through
 * the selected generic story table.</p>
 */
final class ClassicWorldCkewCompletionCatalog {
    static final int MAP_ID = 77;
    static final int NPC_ID = ClassicWorldIvoryTowerCatalog.DICARDING_NPC_ID;
    static final String NPC_SOURCE_SHA256 =
            "0BF238A689C152B3A4837E771DDBB9746205030C444F94D86947962E6BD87587";
    static final String COMPLETION_MESSAGE =
            "\\fT不死魔族再生的秘密任務完成！";

    static final int CROWN_REWARD = 51;
    static final int KNIGHT_REWARD = 56;
    static final int ELF_SWORD_REWARD = 50;
    static final int ELF_BOW_REWARD = 184;
    static final int WIZARD_REWARD = 20225;

    /** Exact {@code delItem} iteration order from the retained executor. */
    static final List<Long> CLEANUP_ITEM_IDS = List.of(
            40742L, 49165L, 49166L, 49167L, 49168L,
            49239L, 65L, 133L, 191L, 192L);

    record Completion(String action, int rewardItemId,
                      String missingHtml, String successHtml) {
        Completion {
            if (action == null || action.isBlank() || rewardItemId <= 0
                    || missingHtml == null || missingHtml.isBlank()
                    || successHtml == null || successHtml.isBlank()) {
                throw new IllegalArgumentException("invalid CKEW completion action");
            }
        }
    }

    record ClassContract(int characterType, String endedHtml,
                         Map<Integer, String> activeDialogue,
                         String retryAction, String retryHtml,
                         Map<String, Completion> completions) {
        ClassContract {
            if (characterType < 0 || characterType > 3
                    || endedHtml == null || endedHtml.isBlank()
                    || activeDialogue == null || activeDialogue.isEmpty()
                    || retryAction == null || retryAction.isBlank()
                    || retryHtml == null || retryHtml.isBlank()
                    || completions == null || completions.isEmpty()) {
                throw new IllegalArgumentException("invalid CKEW class completion contract");
            }
            activeDialogue = Map.copyOf(activeDialogue);
            completions = Map.copyOf(completions);
        }
    }

    private static final Map<Integer, ClassContract> CONTRACTS = Map.of(
            0, contract(0, "dicardingp15",
                    Map.of(0, "dicardingp10", 1, "dicardingp10",
                            2, "dicardingp11", 3, "dicardingp13"),
                    "b", "dicardingp12",
                    Map.of("a", new Completion(
                            "a", CROWN_REWARD, "dicardingp11", "dicardingp14"))),
            1, contract(1, "dicardingk16",
                    Map.of(0, "dicardingk10", 1, "dicardingk11",
                            2, "dicardingk12", 3, "dicardingk14"),
                    "k", "dicardingk13",
                    Map.of("l", new Completion(
                            "l", KNIGHT_REWARD, "dicardingk12", "dicardingk15"))),
            2, contract(2, "dicardinge17",
                    Map.of(0, "dicardinge10", 1, "dicardinge12",
                            2, "dicardinge13", 3, "dicardinge15"),
                    "q", "dicardinge14",
                    Map.of(
                            "y", new Completion(
                                    "y", ELF_BOW_REWARD,
                                    "dicardinge13", "dicardinge16"),
                            "s", new Completion(
                                    "s", ELF_SWORD_REWARD,
                                    "dicardinge13", "dicardinge16"))),
            3, contract(3, "dicardingw15",
                    Map.of(0, "dicardingw6", 1, "dicardingw10",
                            2, "dicardingw11", 3, "dicardingw13"),
                    "w", "dicardingw12",
                    Map.of("x", new Completion(
                            "x", WIZARD_REWARD, "dicardingw11", "dicardingw14"))));

    private ClassicWorldCkewCompletionCatalog() {}

    static boolean isDicarding(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID && npc.npcId() == NPC_ID;
    }

    static ClassContract contract(int characterType) {
        return CONTRACTS.get(characterType);
    }

    static String dialogue(int characterType, int questStep) {
        ClassContract contract = contract(characterType);
        if (contract == null) return null;
        if (questStep == ClassicWorldCkewCatalog.QUEST_ENDED) {
            return contract.endedHtml();
        }
        return contract.activeDialogue().get(questStep);
    }

    static String retryHtml(int characterType, String action) {
        ClassContract contract = contract(characterType);
        return contract != null && contract.retryAction().equalsIgnoreCase(action)
                ? contract.retryHtml() : null;
    }

    static Completion completion(int characterType, String action) {
        ClassContract contract = contract(characterType);
        if (contract == null || action == null) return null;
        return contract.completions().get(action.toLowerCase(java.util.Locale.ROOT));
    }

    static boolean activeQuestStep(int questStep) {
        return questStep > 0 && questStep < ClassicWorldCkewCatalog.QUEST_ENDED;
    }

    private static ClassContract contract(
            int type, String ended, Map<Integer, String> dialogue,
            String retryAction, String retryHtml,
            Map<String, Completion> completions) {
        return new ClassContract(
                type, ended, dialogue, retryAction, retryHtml, completions);
    }
}
