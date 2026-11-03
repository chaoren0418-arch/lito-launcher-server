package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Checked i64 implementation of the traced LegacyEquipmentMagicLoader layers,
 * independent of native numeric schema. This pins that loader's field semantics;
 * it does not claim every effect in the separately evolving classic server sources
 * (notably the newer ItemVIPTable / L1ItemVIP fields).
 */
final class ModernEquipmentEvaluator {
    record Evaluation(Ability direct, Ability sets, Ability weaponItem,
                      TemplatePhysical armorTemplatePhysical, TemplatePhysical weaponTemplatePhysical,
                      long weaponObjectId, int equippedRows, int armorRows, int vipRows, int cardRows,
                      int activeSets, int activeCombos, OptionalLong nextStoneExpiry) {
        long armorClassModifier() { return Math.subtractExact(direct.get(AC), sets.get(AC)); }
        long total(Stat stat) {
            if (stat == AC) return armorClassModifier();
            long value = Math.addExact(direct.get(stat), sets.get(stat));
            return stat == WEIGHT_REDUCTION ? Math.addExact(value, armorTemplatePhysical.weightReduction()) : value;
        }
        long magicReductionBase() { return Math.addExact(armorTemplatePhysical.damageReduction(), sets.get(REDUCTION)); }
        long nonWeaponItem(Stat stat) { return Math.subtractExact(direct.get(stat), weaponItem.get(stat)); }
    }

    private ModernEquipmentEvaluator() {}

    /**
     * Pure candidate evaluation, NOT permission to equip. Use EquipmentRules and an
     * owner/revision-checked authority before commit. Missing lookup is never absence.
     * Does not claim full weapon attack traits, projectile selection or live buffs.
     */
    static Evaluation evaluate(Qualified qualified, Map<Long, Boolean> overrides, long nowEpochMillis) {
        return evaluate(qualified, overrides, nowEpochMillis, true);
    }
    /** Saved total AC includes the original SQL-equipped item layer. Only the
     * effective candidate is expired; masking both sides would retain old AC. */
    static Evaluation evaluateSourceBaseline(Qualified qualified, long nowEpochMillis) {
        return evaluate(qualified, Map.of(), nowEpochMillis, false);
    }
    private static Evaluation evaluate(Qualified qualified, Map<Long, Boolean> overrides, long nowEpochMillis, boolean enforceLifetime) {
        return evaluate(qualified,overrides,nowEpochMillis,enforceLifetime,false,Ability.zero());
    }
    /** Explicit complete-VIP rebuild only. Replace the entire old partial VIP
     * layer before adding anything to base attributes, preserving ETC cards and
     * all ordinary equipment/set/stone rules. No subtract-after-overflow step. */
    static Evaluation evaluateCompleteVip(Qualified qualified,Map<Long,Boolean> overrides,long nowEpochMillis,Ability completeVip) {
        return evaluate(qualified,overrides,nowEpochMillis,true,true,java.util.Objects.requireNonNull(completeVip));
    }
    private static Evaluation evaluate(Qualified qualified, Map<Long, Boolean> overrides, long nowEpochMillis,
            boolean enforceLifetime,boolean replaceVip,Ability completeVip) {
        if (qualified == null || overrides == null || nowEpochMillis < 0) throw invalid();
        for (var entry : overrides.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || !qualified.items().containsKey(entry.getKey())) throw invalid();
        }
        Ability direct = Ability.zero(), setValues = Ability.zero(), weaponItem = Ability.zero();
        TemplatePhysical armorPhysical = TemplatePhysical.zero(), weaponPhysical = TemplatePhysical.zero();
        int rows = 0, armorRows = 0, vipRows = 0, cardRows = 0, activeSets = 0, activeCombos = 0, defensiveRows = 0;
        long weaponObjectId = 0;
        OptionalLong expiry = OptionalLong.empty();
        Map<Long, Long> pieces = new HashMap<>();
        Set<Long> uniqueCards = new HashSet<>();
        Catalog catalog = qualified.catalog();
        for (var source : qualified.source().inventory()) {
            Instance item = qualified.items().get(source.objectId());
            if (!overrides.getOrDefault(source.objectId(), source.equipped())) continue;
            if (enforceLifetime && !qualified.lifetimes().view(source.objectId(), nowEpochMillis).usable()) continue;
            rows++;
            Template template = item.template();
            var kind = template.metadata().kind();
            for (long card : item.cards()) if (card > 1) uniqueCards.add(card);
            if (kind == ModernCharacterData.ItemKind.ETC) {
                if (catalog.vipTableState() == VipTableState.VERIFIED_PRESENT) {
                    RuleValue vip = catalog.require(new RuleKey(RuleKind.VIP, kind, template.metadata().catalogId(), 0));
                    if (vip.present()) { if(!replaceVip)direct = direct.plus(only(vip.raw(), STR, DEX, INT, WIS, CON, MR, SP,
                            HP_REGEN, MP_REGEN, BLIND, SLEEP, FREEZE, STONE, SUSTAIN, STUN, FIRE, WATER, WIND, EARTH,
                            MELEE_HIT, MELEE_DAMAGE, RANGED_HIT, RANGED_DAMAGE)); vipRows++; }
                }
                // Exact retained type2==0 no-op: no template/dark/single-card/stone,
                // but cards above still participate in global card combinations.
                continue;
            }
            Ability local = template.base();
            if (kind == ModernCharacterData.ItemKind.ARMOR) {
                armorRows++;
                if ((template.retainedType() == 7 || template.retainedType() == 13) && ++defensiveRows > 1) throw invalid();
                local = local.with(AC, Math.subtractExact(local.get(AC), source.enchantment()));
                armorPhysical = armorPhysical.plus(template.physical());
            } else {
                if (weaponObjectId != 0) throw invalid();
                weaponObjectId = source.objectId(); weaponPhysical = template.physical();
            }
            for (long setId : template.setIds()) {
                if (!catalog.verifiedSetIds().contains(setId)) throw new IllegalArgumentException("equipment set lookup is not qualified");
                pieces.merge(setId, 1L, Math::addExact);
            }
            local = local.plus(standard(catalog, new RuleKey(RuleKind.GREATER_TYPE, kind, template.retainedType(), item.greater())));
            local = local.plus(standard(catalog, new RuleKey(RuleKind.BLESS_TYPE, kind, template.retainedType(), source.blessing())));
            local = local.plus(standard(catalog, new RuleKey(RuleKind.BLESS_ITEM, kind, template.metadata().catalogId(), source.blessing())));
            if (source.enchantment() > 0) {
                local = local.plus(standard(catalog, new RuleKey(RuleKind.ENCHANT_ITEM, kind, template.metadata().catalogId(), source.enchantment())));
                long over = Math.subtractExact(source.enchantment(), template.safeEnchant());
                if (over > 0) local = local.plus(standard(catalog, new RuleKey(RuleKind.ENCHANT_TYPE, kind, template.retainedType(), over)));
            }
            local = local.plus(item.dark().raw());
            for (long card : item.cards()) {
                if (card <= 1) continue;
                RuleKey key = new RuleKey(RuleKind.CARD, ModernCharacterData.ItemKind.ETC, card, 0);
                RuleValue value = catalog.require(key);
                local = local.plus(standard(catalog, key));
                if (value.present()) cardRows++;
            }
            if (item.stone().active(nowEpochMillis)) {
                RuleValue stone = catalog.require(new RuleKey(RuleKind.STONE, ModernCharacterData.ItemKind.ETC,
                        item.stone().itemId(), item.stone().tier()));
                local = local.plus(only(stone.raw(), STR, DEX, INT, WIS, CON, MAX_HP, MAX_MP, SP, MR));
                if (item.stone().expiresAtEpochMillis().isPresent()) {
                    long end = item.stone().expiresAtEpochMillis().getAsLong();
                    expiry = OptionalLong.of(expiry.isEmpty() ? end : Math.min(end, expiry.getAsLong()));
                }
            }
            if (qualified.source().character().characterClass() == 2 && "$187".equals(template.metadata().nameToken())) {
                local = local.with(MR, Math.addExact(local.get(MR), 5));
            }
            direct = direct.plus(local);
            if (kind == ModernCharacterData.ItemKind.WEAPON) weaponItem = local;
        }
        for (var piece : pieces.entrySet()) {
            SetRule matched = null;
            for (SetRule rule : catalog.sets()) {
                if (rule.setId() == piece.getKey() && rule.pieceCount() <= piece.getValue()
                        && (matched == null || rule.pieceCount() > matched.pieceCount())) matched = rule;
            }
            if (matched != null) { setValues = setValues.plus(matched.raw().with(SP, 0)); activeSets++; }
        }
        for (ComboRule rule : catalog.combos()) {
            if (!uniqueCards.containsAll(rule.requiredCards())) continue;
            Ability applied = rule.raw().with(SP, 0).with(AC, Math.negateExact(rule.raw().get(AC)));
            setValues = setValues.plus(applied); activeCombos++;
        }
        direct=direct.plus(completeVip);
        Evaluation result = new Evaluation(direct, setValues, weaponItem, armorPhysical, weaponPhysical, weaponObjectId,
                rows, armorRows, vipRows, cardRows, activeSets, activeCombos, expiry);
        // Validate every derived output before an authority can publish/save the
        // candidate, not only if a later UI/battle consumer happens to read it.
        for (Stat stat : Stat.values()) {
            result.total(stat);
            result.nonWeaponItem(stat);
        }
        result.magicReductionBase();
        return result;
    }

    /** Preserve the traced field-reader semantics, including deliberately unread columns. */
    private static Ability standard(Catalog catalog, RuleKey key) {
        Ability value = catalog.require(key).raw();
        boolean card = key.kind() == RuleKind.CARD;
        boolean fireRead = card || key.kind() == RuleKind.GREATER_TYPE || key.kind() == RuleKind.BLESS_ITEM;
        value = value.with(FIRE, fireRead ? value.get(FIRE) : 0).with(WEIGHT_REDUCTION, card ? value.get(WEIGHT_REDUCTION) : 0);
        return value.with(AC, key.itemKind() == ModernCharacterData.ItemKind.ARMOR || card ? Math.negateExact(value.get(AC)) : 0);
    }
    private static Ability only(Ability source, Stat... fields) {
        Ability result = Ability.zero();
        for (Stat field : fields) result = result.with(field, source.get(field));
        return result;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid equipment evaluation candidate"); }
}
