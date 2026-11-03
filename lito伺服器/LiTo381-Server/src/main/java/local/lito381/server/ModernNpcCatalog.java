package local.lito381.server;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Explicit selected NPC-catalog facts, never a player source or an NPC-id-only global cache. */
final class ModernNpcCatalog {
    static final int MAX_NPCS = 128;
    /** Every numeric field is required; zeros are observations, never missing-column defaults. */
    enum Field {
        GRAPHIC("gfxid"), LEVEL("lvl"), HP("hp"), MP("mp"), AC("ac"), STR("str"), DEX("dex"),
        INT("intel"), MR("mr"), LAWFUL("lawful"), REDUCTION("damage_reduction"),
        UNDEAD("undead"), WEAK_ATTRIBUTE("weakAttr"), HARD("hard"), BOSS("boss"),
        RANGE("ranged"), BOW_ACTION("bowActId"), POISON("poison_atk"), PARALYSIS("paralysis_atk"),
        TURN_UNDEAD("IsTU"), ERASE("IsErase"), CANNOT_RESURRECT("cant_resurrect"),
        WALK_MILLIS("passispeed"), ATTACK_MILLIS("atkspeed"), MAGIC_MILLIS("atk_magic_speed"), SUB_MAGIC_MILLIS("sub_magic_speed"),
        BRAVE("bravespeed"), TELEPORT("teleport"), DOPPEL("doppel"),
        HP_REGEN_INTERVAL("hprinterval"), HP_REGEN("hpr"), MP_REGEN_INTERVAL("mprinterval"), MP_REGEN("mpr"),
        RANDOM_LEVEL("randomlevel"), RANDOM_HP("randomhp"), RANDOM_MP("randommp"), RANDOM_AC("randomac"),
        RANDOM_EXP("randomexp"), RANDOM_LAWFUL("randomlawful"), TRANSFORM_ID("transform_id"), TRANSFORM_GRAPHIC("transform_gfxid");
        private final String column;
        Field(String column) { this.column = column; }
        String column() { return column; }
        String alias() { return "n_" + name().toLowerCase(java.util.Locale.ROOT); }
    }
    record Key(int sourceId, String database, int npcId) {
        Key { if (sourceId < 1 || npcId < 1) throw bad(); ModernPhysicalCombat.identifier(database); }
        @Override public String toString() { return "NpcCatalogKey[redacted]"; }
    }
    record Facts(Map<Field, Long> values) {
        Facts {
            Objects.requireNonNull(values);
            if (values.size() != Field.values().length) throw bad();
            var copy = new EnumMap<Field, Long>(Field.class);
            for (var field : Field.values()) {
                Long value = values.get(field); if (value == null) throw bad();
                validate(field, value); copy.put(field, value);
            }
            values = Map.copyOf(copy);
        }
        long get(Field field) { return values.get(Objects.requireNonNull(field)); }
        private static void validate(Field field, long value) {
            switch (field) {
                case LEVEL, HP -> { if (value < 1) throw bad(); }
                case AC, MR, LAWFUL, RANDOM_AC, RANDOM_LAWFUL -> { }
                case GRAPHIC, TRANSFORM_GRAPHIC, BOW_ACTION -> { if (value < 0 || value > 65535) throw bad(); }
                case HARD, BOSS, TURN_UNDEAD, ERASE, CANNOT_RESURRECT, BRAVE, TELEPORT, DOPPEL -> {
                    if (value != 0 && value != 1) throw bad();
                }
                case UNDEAD -> { if (value < 0 || value > 5) throw bad(); }
                case WEAK_ATTRIBUTE -> { if (!Set.of(-8L, -4L, -2L, -1L, 0L, 1L, 2L, 4L, 8L).contains(value)) throw bad(); }
                case WALK_MILLIS, ATTACK_MILLIS, MAGIC_MILLIS, SUB_MAGIC_MILLIS -> {
                    if (value < 0 || value > 60_000) throw bad();
                }
                case TRANSFORM_ID -> { if (value < -1 || value > Integer.MAX_VALUE) throw bad(); }
                default -> { if (value < 0) throw bad(); }
            }
        }
    }
    enum Pending { CUSTOM_IMPLEMENTATION, GRAPHICS, NO_WALK_CLOCK, NO_ATTACK_CLOCK, MOBSKILLS,
        RANGED, POISON, PARALYSIS, BRAVE, TELEPORT, DOPPEL, REGENERATION, RANDOMIZED_STATS, TRANSFORMATION }
    record Npc(Key key, String name, String nameId, String implementation, boolean large, Facts facts, boolean hasMobskills) {
        Npc {
            Objects.requireNonNull(key); Objects.requireNonNull(facts);
            text(name, 256, false); text(nameId, 256, true); text(implementation, 96, false);
        }
        long get(Field field) { return facts.get(field); }
        /** These are remaining behavior requirements, not a list silently ignored by an AI. */
        Set<Pending> pendingCleanMelee() {
            var result = new LinkedHashSet<Pending>();
            if (!implementation.equals("L1Monster")) result.add(Pending.CUSTOM_IMPLEMENTATION);
            if (!Set.of(1649L, 2158L, 1082L, 1047L, 1020L, 1578L, 1517L, 2535L, 240L, 145L).contains(get(Field.GRAPHIC))) result.add(Pending.GRAPHICS);
            if (get(Field.WALK_MILLIS) == 0) result.add(Pending.NO_WALK_CLOCK);
            if (get(Field.ATTACK_MILLIS) == 0) result.add(Pending.NO_ATTACK_CLOCK);
            if (hasMobskills) result.add(Pending.MOBSKILLS);
            if (get(Field.RANGE) > 1 || get(Field.BOW_ACTION) != 0) result.add(Pending.RANGED);
            if (get(Field.POISON) != 0) result.add(Pending.POISON);
            if (get(Field.PARALYSIS) != 0) result.add(Pending.PARALYSIS);
            if (get(Field.BRAVE) != 0) result.add(Pending.BRAVE);
            if (get(Field.TELEPORT) != 0) result.add(Pending.TELEPORT);
            if (get(Field.DOPPEL) != 0) result.add(Pending.DOPPEL);
            for (var field : new Field[]{Field.HP_REGEN_INTERVAL, Field.HP_REGEN, Field.MP_REGEN_INTERVAL, Field.MP_REGEN})
                if (get(field) != 0) result.add(Pending.REGENERATION);
            for (var field : new Field[]{Field.RANDOM_LEVEL, Field.RANDOM_HP, Field.RANDOM_MP, Field.RANDOM_AC, Field.RANDOM_EXP, Field.RANDOM_LAWFUL})
                if (get(field) != 0) result.add(Pending.RANDOMIZED_STATS);
            if (get(Field.TRANSFORM_ID) > 0 || get(Field.TRANSFORM_GRAPHIC) != 0) result.add(Pending.TRANSFORMATION);
            return Set.copyOf(result);
        }
        /** Numeric adapter only. Caller must still implement every pending behavior before admission. */
        ModernNpcRoster.Template rosterTemplate() {
            if (get(Field.WALK_MILLIS) == 0) throw bad();
            return new ModernNpcRoster.Template(key.sourceId(), key.database(), key.npcId(), Math.toIntExact(get(Field.GRAPHIC)),
                    get(Field.HP), get(Field.MP), Math.toIntExact(get(Field.WALK_MILLIS)), Math.toIntExact(get(Field.ATTACK_MILLIS)));
        }
        void validateActor(ModernNpcRoster.Actor actor) {
            if (actor == null || !actor.template().equals(rosterTemplate())) throw bad();
        }
        @Override public String toString() { return "NpcCatalogEntry[redacted]"; }
    }
    record Catalog(int sourceId, String database, Map<Integer, Npc> npcs, ModernPhysicalCombat.Growth growth) {
        Catalog {
            if (sourceId < 1) throw bad(); ModernPhysicalCombat.identifier(database);
            Objects.requireNonNull(npcs); Objects.requireNonNull(growth);
            if (npcs.isEmpty() || npcs.size() > MAX_NPCS || growth.sourceId() != sourceId || !growth.database().equals(database)) throw bad();
            var copy = new TreeMap<Integer, Npc>();
            for (var entry : npcs.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getKey() != entry.getValue().key().npcId()
                        || entry.getValue().key().sourceId() != sourceId || !entry.getValue().key().database().equals(database)) throw bad();
                copy.put(entry.getKey(), entry.getValue());
            }
            npcs = Map.copyOf(copy);
        }
        Npc require(Key key) {
            if (key == null || key.sourceId() != sourceId || !key.database().equals(database) || !npcs.containsKey(key.npcId())) throw bad();
            return npcs.get(key.npcId());
        }
        @Override public String toString() { return "NpcCatalog[redacted]"; }
    }
    static void text(String value, int maxBytes, boolean empty) {
        if (value == null || !java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(value)
                || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxBytes
                || !(empty && value.isEmpty()) && !ModernCharacterData.validText(value)) throw bad();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid selected NPC catalog"); }
    private ModernNpcCatalog() {}
}
