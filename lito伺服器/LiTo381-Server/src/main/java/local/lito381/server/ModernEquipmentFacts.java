package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

/** Immutable, explicitly qualified input to the i64 equipment ability pipeline. */
final class ModernEquipmentFacts {
    static final int MAX_RULES = 32_768;
    static final int MAX_SET_ROWS = 4_096;
    static final int MAX_COMBOS = 4_096;
    static final int MAX_SET_IDS_PER_ITEM = 64;
    static final int MAX_COMBO_CARDS = 64;

    enum Stat {
        STR, DEX, CON, INT, WIS, CHA, MAX_HP, MAX_MP, POTION_RECOVERY, SP, MR,
        MAGIC_DAMAGE, MAGIC_HIT, BLIND, SLEEP, FREEZE, STONE, SUSTAIN, STUN, STUN_SUCCESS,
        FIRE, WATER, WIND, EARTH, REDUCTION, HP_REGEN, MP_REGEN, WEIGHT_REDUCTION,
        MELEE_HIT, MELEE_DAMAGE, RANGED_HIT, RANGED_DAMAGE, PIERCE, AC, EVASION,
        MELEE_REDUCTION, RANGED_REDUCTION, IGNORE_REDUCTION, PVP_REDUCTION
    }

    /** Every field must be supplied. Only verified absence/not-applicable becomes zero. */
    record Ability(Map<Stat, Long> values) {
        Ability {
            Objects.requireNonNull(values);
            if (values.size() != Stat.values().length) throw invalid();
            var copy = new EnumMap<Stat, Long>(Stat.class);
            for (Stat stat : Stat.values()) {
                Long value = values.get(stat);
                if (value == null) throw invalid();
                copy.put(stat, value);
            }
            values = Map.copyOf(copy);
        }
        long get(Stat stat) { return values.get(Objects.requireNonNull(stat)); }
        Ability with(Stat stat, long value) {
            var copy = new EnumMap<Stat, Long>(Stat.class);
            copy.putAll(values); copy.put(Objects.requireNonNull(stat), value);
            return new Ability(copy);
        }
        Ability plus(Ability other) {
            Objects.requireNonNull(other);
            var result = new EnumMap<Stat, Long>(Stat.class);
            for (Stat stat : Stat.values()) result.put(stat, Math.addExact(get(stat), other.get(stat)));
            return new Ability(result);
        }
        /** Algebraic zero, not a fallback for a failed query or missing catalog key. */
        static Ability zero() {
            var result = new EnumMap<Stat, Long>(Stat.class);
            for (Stat stat : Stat.values()) result.put(stat, 0L);
            return new Ability(result);
        }
    }

    enum RuleKind { GREATER_TYPE, BLESS_TYPE, BLESS_ITEM, ENCHANT_ITEM, ENCHANT_TYPE, CARD, STONE, VIP }
    /** first/second are the exact SQL key, never a native item/character wire ID. */
    record RuleKey(RuleKind kind, ModernCharacterData.ItemKind itemKind, long first, long second) {
        RuleKey {
            Objects.requireNonNull(kind); Objects.requireNonNull(itemKind);
            if (first < 1 || second < 0) throw invalid();
            boolean auxiliary = kind == RuleKind.CARD || kind == RuleKind.STONE || kind == RuleKind.VIP;
            if (auxiliary != (itemKind == ModernCharacterData.ItemKind.ETC)) throw invalid();
            if ((kind == RuleKind.CARD || kind == RuleKind.VIP) && second != 0) throw invalid();
            if (kind == RuleKind.CARD && first <= 1) throw invalid();
        }
    }

    /** A successful lookup with no matching row is different from an unqueried key. */
    record RuleValue(boolean present, Ability raw) {
        RuleValue {
            Objects.requireNonNull(raw);
            if (!present && !raw.equals(Ability.zero())) throw invalid();
        }
        static RuleValue absent() { return new RuleValue(false, Ability.zero()); }
        static RuleValue present(Ability raw) { return new RuleValue(true, raw); }
    }
    enum VipTableState { VERIFIED_ABSENT, VERIFIED_PRESENT }

    record SetRule(long setId, long pieceCount, Ability raw) {
        SetRule {
            if (setId < 1 || pieceCount < 1) throw invalid();
            Objects.requireNonNull(raw);
        }
    }
    record ComboRule(long comboId, Set<Long> requiredCards, Ability raw) {
        ComboRule {
            if (comboId < 1 || requiredCards == null || requiredCards.isEmpty()
                    || requiredCards.size() > MAX_COMBO_CARDS) throw invalid();
            for (Long card : requiredCards) if (card == null || card <= 1) throw invalid();
            requiredCards = Set.copyOf(requiredCards);
            Objects.requireNonNull(raw);
        }
    }

    /** All matching-set rows and the whole combo catalog must have been bounded/read. */
    record Catalog(int sourceId, String resourceDatabase, Map<RuleKey, RuleValue> lookups,
                   Set<Long> verifiedSetIds, List<SetRule> sets, List<ComboRule> combos,
                   VipTableState vipTableState) {
        Catalog {
            if (sourceId < 1 || resourceDatabase == null || !resourceDatabase.matches("[A-Za-z0-9_]{1,64}")
                    || lookups == null || lookups.size() > MAX_RULES || verifiedSetIds == null
                    || verifiedSetIds.size() > MAX_SET_ROWS || sets == null || sets.size() > MAX_SET_ROWS
                    || combos == null || combos.size() > MAX_COMBOS) throw invalid();
            lookups = Map.copyOf(lookups); verifiedSetIds = Set.copyOf(verifiedSetIds);
            Objects.requireNonNull(vipTableState);
            for (long id : verifiedSetIds) if (id < 1) throw invalid();
            Set<String> setKeys = new HashSet<>();
            for (SetRule rule : sets) {
                if (rule == null || !verifiedSetIds.contains(rule.setId())
                        || !setKeys.add(rule.setId() + ":" + rule.pieceCount())) throw invalid();
            }
            Set<Long> comboIds = new HashSet<>();
            for (ComboRule rule : combos) if (rule == null || !comboIds.add(rule.comboId())) throw invalid();
            sets = sets.stream().sorted(Comparator.comparingLong(SetRule::setId).thenComparingLong(SetRule::pieceCount)).toList();
            combos = combos.stream().sorted(Comparator.comparingLong(ComboRule::comboId)).toList();
            if (vipTableState == VipTableState.VERIFIED_ABSENT
                    && lookups.keySet().stream().anyMatch(key -> key.kind() == RuleKind.VIP)) throw invalid();
        }
        RuleValue require(RuleKey key) {
            RuleValue value = lookups.get(key);
            if (value == null) throw new IllegalArgumentException("equipment rule lookup is not qualified");
            return value;
        }
    }

    /** Separate template modifiers: never count a weapon's direct item layer twice. */
    record TemplatePhysical(long meleeHit, long meleeDamage, long rangedHit, long rangedDamage,
                            long damageReduction, long weightReduction) {
        static TemplatePhysical zero() { return new TemplatePhysical(0, 0, 0, 0, 0, 0); }
        TemplatePhysical plus(TemplatePhysical other) {
            return new TemplatePhysical(Math.addExact(meleeHit, other.meleeHit), Math.addExact(meleeDamage, other.meleeDamage),
                    Math.addExact(rangedHit, other.rangedHit), Math.addExact(rangedDamage, other.rangedDamage),
                    Math.addExact(damageReduction, other.damageReduction), Math.addExact(weightReduction, other.weightReduction));
        }
    }
    record Template(ModernCharacterData.ItemTemplate metadata, EquipmentRules.Facts rules,
                    long retainedType, long safeEnchant, List<Long> setIds,
                    Ability base, TemplatePhysical physical) {
        Template {
            Objects.requireNonNull(metadata); Objects.requireNonNull(rules);
            Objects.requireNonNull(base); Objects.requireNonNull(physical);
            if (setIds == null || setIds.size() > MAX_SET_IDS_PER_ITEM) throw invalid();
            for (Long id : setIds) if (id == null || id < 1) throw invalid();
            // Repeated set IDs retain the source's per-token piece counting, not deduplication.
            setIds = List.copyOf(setIds);
            switch (metadata.kind()) {
                case WEAPON -> { if (!(rules instanceof EquipmentRules.WeaponFacts) || retainedType < 1 || retainedType > 18) throw invalid(); }
                case ARMOR -> { if (!(rules instanceof EquipmentRules.ArmorFacts armor) || retainedType != armor.slot().retainedType()) throw invalid(); }
                case ETC -> {
                    if (rules != EquipmentRules.NonEquipmentFacts.INSTANCE || retainedType != 0 || safeEnchant != 0
                            || !setIds.isEmpty() || !base.equals(Ability.zero()) || !physical.equals(TemplatePhysical.zero())) throw invalid();
                }
            }
        }
        @Override public String toString() { return "ModernEquipmentTemplate[redacted]"; }
    }
    record Stone(long itemId, long tier, OptionalLong expiresAtEpochMillis) {
        Stone {
            Objects.requireNonNull(expiresAtEpochMillis);
            if (itemId < 0 || tier < 0 || expiresAtEpochMillis.isPresent() && expiresAtEpochMillis.getAsLong() < 0) throw invalid();
            if (itemId == 0 && (tier != 0 || expiresAtEpochMillis.isPresent())) throw invalid();
        }
        static Stone none() { return new Stone(0, 0, OptionalLong.empty()); }
        boolean active(long now) { return itemId > 0 && (expiresAtEpochMillis.isEmpty() || now < expiresAtEpochMillis.getAsLong()); }
    }
    record Instance(ModernCharacterData.InventoryItem source, Template template, long greater,
                    RuleValue dark, List<Long> cards, Stone stone) {
        Instance {
            Objects.requireNonNull(source); Objects.requireNonNull(template); Objects.requireNonNull(dark); Objects.requireNonNull(stone);
            if (!source.template().equals(template.metadata()) || greater < 0 || cards == null || cards.size() != 5) throw invalid();
            for (Long card : cards) if (card == null || card < 0) throw invalid();
            cards = List.copyOf(cards);
        }
        EquipmentRules.Item ruleItem() {
            // Unknown blessing values are retained by CharacterData but cannot become permission.
            EquipmentRules.CurseState curse = source.blessing() <= 3
                    ? source.blessing() == 2 ? EquipmentRules.CurseState.CURSED : EquipmentRules.CurseState.NOT_CURSED
                    : EquipmentRules.CurseState.UNKNOWN;
            return new EquipmentRules.Item(source.objectId(), source.template().catalogId(), source.equipped(), curse,
                    source.sealed() ? EquipmentRules.SealState.SEALED : EquipmentRules.SealState.UNSEALED, template.rules());
        }
        @Override public String toString() { return "ModernEquipmentInstance[redacted]"; }
    }
    record Qualified(ModernCharacterData.Snapshot source, Map<Long, Instance> items, Catalog catalog,
                     ModernItemLifetime.Observation lifetimes) {
        Qualified(ModernCharacterData.Snapshot source, Map<Long, Instance> items, Catalog catalog) {
            this(source, items, catalog, ModernItemLifetime.Observation.unqualified(source));
        }
        Qualified {
            Objects.requireNonNull(source); Objects.requireNonNull(items); Objects.requireNonNull(catalog);
            Objects.requireNonNull(lifetimes); lifetimes.validateSource(source);
            if (source.sourceId() != catalog.sourceId() || source.character().characterClass() > 6
                    || items.size() != source.inventory().size()) throw invalid();
            var copy = new LinkedHashMap<Long, Instance>();
            for (var item : source.inventory()) {
                Instance qualified = items.get(item.objectId());
                if (qualified == null || !qualified.source().equals(item)) throw invalid();
                copy.put(item.objectId(), qualified);
            }
            items = Map.copyOf(copy);
            // Qualify potential future candidates too, not just today's equipped rows.
            for (RuleKey key : requiredLookups(List.copyOf(items.values()))) {
                if (key.kind() == RuleKind.VIP && catalog.vipTableState() == VipTableState.VERIFIED_ABSENT) continue;
                catalog.require(key);
            }
            if (!catalog.verifiedSetIds().containsAll(requiredSetIds(List.copyOf(items.values())))) throw invalid();
        }
        EquipmentRules.Loadout ruleLoadout() {
            return new EquipmentRules.Loadout(source.inventory().stream().map(item -> items.get(item.objectId()).ruleItem()).toList());
        }
        @Override public String toString() { return "ModernQualifiedEquipment[redacted]"; }
    }
    static Set<RuleKey> requiredLookups(List<Instance> items) {
        if (items == null || items.size() > ModernCharacterData.MAX_ITEMS) throw invalid();
        Set<RuleKey> keys = new HashSet<>();
        for (Instance item : items) {
            Objects.requireNonNull(item);
            Template template = item.template();
            var kind = template.metadata().kind();
            long catalogId = template.metadata().catalogId();
            if (kind == ModernCharacterData.ItemKind.ETC) {
                keys.add(new RuleKey(RuleKind.VIP, kind, catalogId, 0));
                continue;
            }
            keys.add(new RuleKey(RuleKind.GREATER_TYPE, kind, template.retainedType(), item.greater()));
            keys.add(new RuleKey(RuleKind.BLESS_TYPE, kind, template.retainedType(), item.source().blessing()));
            keys.add(new RuleKey(RuleKind.BLESS_ITEM, kind, catalogId, item.source().blessing()));
            long enchant = item.source().enchantment();
            if (enchant > 0) {
                keys.add(new RuleKey(RuleKind.ENCHANT_ITEM, kind, catalogId, enchant));
                long over = Math.subtractExact(enchant, template.safeEnchant());
                if (over > 0) keys.add(new RuleKey(RuleKind.ENCHANT_TYPE, kind, template.retainedType(), over));
            }
            for (long card : item.cards()) if (card > 1) keys.add(new RuleKey(RuleKind.CARD, ModernCharacterData.ItemKind.ETC, card, 0));
            if (item.stone().itemId() > 0) keys.add(new RuleKey(RuleKind.STONE, ModernCharacterData.ItemKind.ETC,
                    item.stone().itemId(), item.stone().tier()));
        }
        return Set.copyOf(keys);
    }
    static Set<Long> requiredSetIds(List<Instance> items) {
        if (items == null || items.size() > ModernCharacterData.MAX_ITEMS) throw invalid();
        Set<Long> ids = new HashSet<>();
        for (Instance item : items) ids.addAll(Objects.requireNonNull(item).template().setIds());
        return Set.copyOf(ids);
    }
    private ModernEquipmentFacts() {}
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern equipment facts"); }
}
