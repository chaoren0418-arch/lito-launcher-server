package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Hash-locked selected {@code 380_db.系統_劇情任務} rows and generic matcher. */
final class ClassicWorldStoryQuestCatalog {
    static final int ROW_COUNT = 679;
    static final int GUNTER_NPC_ID = 70522;
    static final int ZERO_NPC_ID = 70554;
    static final int JIM_NPC_ID = 70555;
    static final int JIM_ROW_COUNT = 15;
    static final String JIM_SOURCE_SHA256 =
            "5C8060A069A47FF9417C688CB138CAB1F00957FC951D28E418E7D0C58AF5114F";
    static final String SOURCE_SHA256 =
            "176ECB72747E0761F08F5C089EED536A1DFDF0266FAE56F146DF7793066C903B";
    static final String RESOURCE_SHA256 =
            "E219A4B3D14725E4261B99C82D00E05059194CD10116C3673043CE16814D3F9C";

    enum EventType { NPC_TALK, NPC_ACTION }

    record ItemAmount(long itemId, int count) {
        ItemAmount {
            if (itemId <= 0 || itemId > 0xFFFF_FFFFL || count <= 0) {
                throw new IllegalArgumentException("invalid story item amount");
            }
        }
    }

    record Event(
            int id, int questId, String questName, EventType eventType,
            int classMask, int minimumLevel, int maximumLevel, int prerequisiteQuestId,
            int npcId, String action, int minimumStep, int maximumStep,
            String requiredGfx, List<ItemAmount> requiredItems,
            List<ItemAmount> consumedItems, List<ItemAmount> givenItems,
            int setStep, boolean startQuest, String html,
            Map<String, String> parameters, int priority) {
        Event {
            questName = questName == null ? "" : questName;
            action = action == null ? "" : action;
            requiredGfx = requiredGfx == null ? "" : requiredGfx;
            html = html == null ? "" : html;
            requiredItems = List.copyOf(requiredItems);
            consumedItems = List.copyOf(consumedItems);
            givenItems = List.copyOf(givenItems);
            parameters = Map.copyOf(parameters);
            if (id <= 0 || questId <= 0 || eventType == null
                    || classMask < 0 || classMask > 0x7F
                    || minimumLevel < 0 || maximumLevel < 0
                    || prerequisiteQuestId < 0 || npcId <= 0
                    || minimumStep < 0 || maximumStep < 0 || setStep < 0) {
                throw new IllegalArgumentException("invalid selected story event");
            }
        }

        boolean chainOnly() { return flag("chain_only"); }
        boolean matchNoRecord() { return flag("match_no_record"); }
        boolean skipOnMissing() { return flag("skip_on_missing"); }

        int chainedQuestId() {
            String value = parameters.get("chain_start_quest_id");
            if (value == null || value.isBlank()) return 0;
            try {
                return Math.max(0, Integer.parseInt(value));
            } catch (NumberFormatException invalid) {
                return 0;
            }
        }

        List<ItemAmount> forbiddenItems() {
            return parseItems(parameters.getOrDefault("forbid_items", ""));
        }

        List<ItemAmount> cleanupItems() {
            return parseItems(parameters.getOrDefault("cleanup_items", ""));
        }

        List<Integer> removedSkillEffects() {
            String encoded = parameters.getOrDefault("remove_skill_effects", "");
            if (encoded.isBlank()) return List.of();
            List<Integer> result = new ArrayList<>();
            for (String token : encoded.split("[|,]")) {
                int value = optionalInt(token, 0);
                if (value > 0) result.add(value);
            }
            return List.copyOf(result);
        }

        Map.Entry<Integer, Integer> setSkillEffect() {
            String encoded = parameters.getOrDefault("set_skill_effect", "");
            if (encoded.isBlank()) return null;
            String[] field = encoded.split(",", -1);
            if (field.length != 2) return null;
            int effect = optionalInt(field[0], 0);
            int durationMillis = optionalInt(field[1], 0);
            return effect <= 0 || durationMillis <= 0
                    ? null : Map.entry(effect, durationMillis);
        }

        boolean deleteQuestRowOnComplete() {
            return "del_row".equalsIgnoreCase(
                    parameters.getOrDefault("complete_mode", ""));
        }

        private boolean flag(String name) {
            return "1".equals(parameters.get(name));
        }
    }

    record Dialogue(Event event, String html, Map<Integer, Integer> questSteps) {
        Dialogue {
            if (event == null || html == null) {
                throw new IllegalArgumentException("invalid story dialogue");
            }
            questSteps = Map.copyOf(questSteps);
        }
    }

    /**
     * Retained {@code QuestGate} distinguishes an unknown action from a known
     * story action whose materials are missing.  The latter owns the request
     * and must return missing-item UI instead of falling through to another
     * NPC executor.
     */
    record ActionResolution(Event event, List<ItemAmount> missingItems) {
        ActionResolution {
            if (event == null || missingItems == null) {
                throw new IllegalArgumentException("invalid story action resolution");
            }
            missingItems = List.copyOf(missingItems);
        }

        boolean admitted() { return missingItems.isEmpty(); }

        String missingHtml() {
            return event.parameters().getOrDefault("missing_html", "");
        }
    }

    private static final int MAGIC = 0x4C575131; // LWQ1
    private static final List<Event> EVENTS = load();
    private static final Map<Integer, List<Event>> BY_NPC = index(EVENTS);

    private ClassicWorldStoryQuestCatalog() {}

    static List<Event> all() { return EVENTS; }

    static boolean hasNpc(int npcId) { return BY_NPC.containsKey(npcId); }

    static Dialogue dialogue(int npcId, int characterType, int level, int gfx,
                             Map<Integer, Integer> questSteps,
                             Map<Long, Integer> inventory) {
        return dialogue(npcId, characterType, level, gfx, 0, 0,
                questSteps, inventory);
    }

    static Dialogue dialogue(int npcId, int characterType, int level, int gfx,
                             int lawful, int npcGfx,
                             Map<Integer, Integer> questSteps,
                             Map<Long, Integer> inventory) {
        Event event = resolveDialogue(npcId, characterType, level, gfx,
                lawful, npcGfx, questSteps, inventory);
        if (event == null) return null;
        Map<Integer, Integer> nextSteps = applyQuestMutation(event, questSteps);
        String html = htmlForStep(event, questSteps);
        // The selected table retains a few blank chain-only recovery rows.
        // If a prior durable commit was interrupted, start the missing stage
        // and render the newly active row rather than sending an empty page.
        if (html.isBlank() && event.chainOnly()
                && !nextSteps.equals(questSteps)) {
            Event active = resolveDialogue(npcId, characterType, level, gfx,
                    lawful, npcGfx, nextSteps, inventory);
            if (active != null) html = htmlForStep(active, nextSteps);
        }
        return new Dialogue(event, html, nextSteps);
    }

    static Event action(int npcId, String action, int characterType, int level, int gfx,
                        Map<Integer, Integer> questSteps,
                        Map<Long, Integer> inventory) {
        ActionResolution resolution = actionResolution(
                npcId, action, characterType, level, gfx, 0, 0,
                questSteps, inventory);
        return resolution != null && resolution.admitted()
                ? resolution.event() : null;
    }

    static ActionResolution actionResolution(
            int npcId, String action, int characterType, int level, int gfx,
            int lawful, int npcGfx, Map<Integer, Integer> questSteps,
            Map<Long, Integer> inventory) {
        if (action == null || action.isBlank() || characterType < 0
                || characterType > 6 || level < 1
                || questSteps == null || inventory == null) return null;
        List<Event> candidates = BY_NPC.getOrDefault(npcId, List.of());
        Event best = null;
        List<ItemAmount> bestMissing = List.of();
        for (Event event : candidates) {
            if (event.eventType() != EventType.NPC_ACTION
                    || !event.action().equalsIgnoreCase(action)
                    || !eligibleBase(event, characterType, level, gfx,
                            lawful, npcGfx, questSteps, inventory)) continue;
            List<ItemAmount> missing = missingItems(inventory, event);
            if (!missing.isEmpty() && event.skipOnMissing()) continue;
            if (best == null || event.priority() > best.priority()
                    || (event.priority() == best.priority() && event.id() < best.id())) {
                best = event;
                bestMissing = missing;
            }
        }
        return best == null ? null : new ActionResolution(best, bestMissing);
    }

    static Map<Integer, Integer> applyQuestMutation(
            Event event, Map<Integer, Integer> current) {
        if (event == null || current == null) {
            throw new IllegalArgumentException("story event and quest state are required");
        }
        Map<Integer, Integer> result = new LinkedHashMap<>(current);
        if (event.startQuest()) result.putIfAbsent(event.questId(), 0);
        if (event.setStep() > 0) result.put(event.questId(), event.setStep());
        int chained = event.chainedQuestId();
        if (chained > 0) result.putIfAbsent(chained, 0);
        if (event.deleteQuestRowOnComplete()) result.remove(event.questId());
        return Map.copyOf(result);
    }

    static boolean canConsume(Map<Long, Integer> inventory, Event event) {
        return contains(inventory, event.consumedItems());
    }

    static String responseHtml(Event event, Map<Integer, Integer> nextQuestSteps) {
        if (event == null || nextQuestSteps == null) {
            throw new IllegalArgumentException("story response state is required");
        }
        if (nextQuestSteps.getOrDefault(event.questId(), 0) == 1) {
            String complete = event.parameters().get("html_complete");
            if (complete != null && !complete.isBlank()) return complete;
        }
        String staged = event.setStep() > 0
                ? event.parameters().get("stage_next_view_step") : null;
        if (staged == null || staged.isBlank()) {
            staged = event.parameters().get("stage_view_step");
        }
        int step = optionalInt(staged,
                nextQuestSteps.getOrDefault(event.questId(), 0));
        String override = event.parameters().get("html_step_" + step);
        return override == null || override.isBlank() ? event.html() : override;
    }

    private static Event resolveDialogue(
            int npcId, int characterType, int level, int gfx, int lawful,
            int npcGfx, Map<Integer, Integer> questSteps,
            Map<Long, Integer> inventory) {
        if (characterType < 0 || characterType > 6 || level < 1
                || questSteps == null || inventory == null) return null;
        List<Event> candidates = BY_NPC.getOrDefault(npcId, List.of());
        Event best = null;
        for (Event event : candidates) {
            if (event.eventType() != EventType.NPC_TALK
                    || !eligibleBase(event, characterType, level, gfx,
                            lawful, npcGfx, questSteps, inventory)
                    || !missingItems(inventory, event).isEmpty()) continue;
            if (best == null || event.priority() > best.priority()
                    || (event.priority() == best.priority() && event.id() < best.id())) {
                best = event;
            }
        }
        return best;
    }

    private static boolean eligibleBase(
            Event event, int characterType, int level, int gfx,
            int lawful, int npcGfx, Map<Integer, Integer> questSteps,
            Map<Long, Integer> inventory) {
        int classBit = 1 << characterType;
        if (event.classMask() != 0 && (event.classMask() & classBit) == 0) return false;
        if (level < event.minimumLevel()
                || (event.maximumLevel() > 0 && level > event.maximumLevel())) return false;
        if (event.prerequisiteQuestId() > 0
                && questSteps.getOrDefault(event.prerequisiteQuestId(), 0) < 1) return false;
        boolean present = questSteps.containsKey(event.questId());
        if (event.eventType() == EventType.NPC_TALK) {
            if ((event.matchNoRecord() || event.chainOnly()) && present) return false;
            if (!event.matchNoRecord() && !event.chainOnly()) {
                if (event.startQuest()) {
                    if (present) return false;
                } else if (!present) {
                    return false;
                }
            }
        } else if (event.startQuest()) {
            if (present) return false;
        } else if (!present) {
            return false;
        }
        if (present) {
            int step = questSteps.get(event.questId());
            if (event.minimumStep() == 0 && event.maximumStep() == 0) {
                if (step != 0) return false;
            } else if (step < event.minimumStep()
                    || (event.maximumStep() > 0 && step > event.maximumStep())) {
                return false;
            }
        }
        if (!gfxMatches(event.requiredGfx(), gfx)) return false;
        if (!gfxMatches(event.parameters().getOrDefault("require_npc_gfx", ""),
                npcGfx)) return false;
        int lawfulMin = optionalInt(event.parameters().get("lawful_min"), Integer.MIN_VALUE);
        int lawfulMax = optionalInt(event.parameters().get("lawful_max"), Integer.MAX_VALUE);
        if (lawful < lawfulMin || lawful > lawfulMax) return false;
        for (ItemAmount forbidden : event.forbiddenItems()) {
            if (inventory.getOrDefault(forbidden.itemId(), 0) >= forbidden.count()) {
                return false;
            }
        }
        return true;
    }

    private static boolean gfxMatches(String expression, int gfx) {
        if (expression == null || expression.isBlank()) return true;
        boolean negated = expression.startsWith("!");
        String numeric = negated ? expression.substring(1) : expression;
        boolean equal = false;
        for (String token : numeric.split(",")) {
            try {
                if (gfx == Integer.parseInt(token.trim())) {
                    equal = true;
                    break;
                }
            } catch (NumberFormatException invalid) {
                // Retained matcher skips malformed alternatives.
            }
        }
        return negated ? !equal : equal;
    }

    private static List<ItemAmount> missingItems(
            Map<Long, Integer> inventory, Event event) {
        Map<Long, Integer> required = new LinkedHashMap<>();
        for (ItemAmount item : event.requiredItems()) {
            required.merge(item.itemId(), item.count(), Math::max);
        }
        for (ItemAmount item : event.consumedItems()) {
            required.merge(item.itemId(), item.count(), Math::max);
        }
        List<ItemAmount> missing = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : required.entrySet()) {
            int available = inventory.getOrDefault(entry.getKey(), 0);
            if (available < entry.getValue()) {
                missing.add(new ItemAmount(
                        entry.getKey(), entry.getValue() - available));
            }
        }
        return List.copyOf(missing);
    }

    private static boolean contains(
            Map<Long, Integer> inventory, List<ItemAmount> requirements) {
        for (ItemAmount requirement : requirements) {
            if (inventory.getOrDefault(requirement.itemId(), 0) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    private static String htmlForStep(Event event, Map<Integer, Integer> questSteps) {
        int step = optionalInt(event.parameters().get("stage_view_step"),
                questSteps.getOrDefault(event.questId(), 0));
        String override = event.parameters().get("html_step_" + step);
        return override == null || override.isBlank() ? event.html() : override;
    }

    private static int optionalInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    private static Map<Integer, List<Event>> index(List<Event> events) {
        Map<Integer, List<Event>> result = new LinkedHashMap<>();
        for (Event event : events) {
            result.computeIfAbsent(event.npcId(), ignored -> new ArrayList<>()).add(event);
        }
        result.replaceAll((ignored, rows) -> List.copyOf(rows));
        return Map.copyOf(result);
    }

    private static List<Event> load() {
        String resourceName = "/world/classic-story-quests.lwq.gz";
        try (InputStream resource = ClassicWorldStoryQuestCatalog.class
                .getResourceAsStream(resourceName)) {
            if (resource == null) throw new IOException("missing story quest resource");
            byte[] compressed = resource.readAllBytes();
            if (!RESOURCE_SHA256.equals(sha256(compressed))) {
                throw new IOException("story quest resource SHA-256 mismatch");
            }
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                    new ByteArrayInputStream(compressed)))) {
                if (in.readInt() != MAGIC) throw new IOException("invalid story quest magic");
                int expectedRows = in.readInt();
                String sourceHash = HexFormat.of().withUpperCase()
                        .formatHex(in.readNBytes(32));
                if (expectedRows != ROW_COUNT || !SOURCE_SHA256.equals(sourceHash)) {
                    throw new IOException("story quest source contract mismatch");
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        in, StandardCharsets.UTF_8))) {
                    String header = reader.readLine();
                    if (header == null || header.split("\t", -1).length != 22) {
                        throw new IOException("invalid story quest header");
                    }
                    List<Event> rows = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        rows.add(parse(line));
                    }
                    if (rows.size() != expectedRows) {
                        throw new IOException("story quest row count mismatch");
                    }
                    return List.copyOf(rows);
                }
            }
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Event parse(String line) throws IOException {
        String[] fields = line.split("\t", -1);
        if (fields.length != 22) throw new IOException("invalid story quest row width");
        try {
            return new Event(
                    integer(fields[0]), integer(fields[1]), fields[2],
                    EventType.valueOf(fields[3].toUpperCase(Locale.ROOT)),
                    integer(fields[5]), integer(fields[6]), integer(fields[7]),
                    integer(fields[8]), integer(fields[9]), fields[10],
                    integer(fields[11]), integer(fields[12]), fields[13],
                    parseItems(fields[14]), parseItems(fields[15]), parseItems(fields[16]),
                    integer(fields[17]), integer(fields[18]) != 0, fields[19],
                    parseParameters(fields[20]), integer(fields[21]));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("invalid selected story quest row", invalid);
        }
    }

    private static List<ItemAmount> parseItems(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        List<ItemAmount> result = new ArrayList<>();
        for (String token : encoded.split("[|;]")) {
            String[] pair = token.trim().split(",", -1);
            if (pair.length != 2) throw new IllegalArgumentException("invalid story item list");
            result.add(new ItemAmount(Long.parseLong(pair[0]), Integer.parseInt(pair[1])));
        }
        return List.copyOf(result);
    }

    private static Map<String, String> parseParameters(String encoded) {
        if (encoded == null || encoded.isBlank()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : encoded.split(";")) {
            String[] pair = token.trim().split("=", 2);
            if (pair.length == 2 && !pair[0].isBlank()) result.put(pair[0], pair[1]);
        }
        return Map.copyOf(result);
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().withUpperCase()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 unavailable", impossible);
        }
    }
}
