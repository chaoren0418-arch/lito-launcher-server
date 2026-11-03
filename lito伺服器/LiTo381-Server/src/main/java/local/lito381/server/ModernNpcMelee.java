package local.lito381.server;

import static local.lito381.server.ModernNpcCatalog.Field.*;

import java.math.BigInteger;
import java.util.Objects;

/**
 * MODERN_NPC_MELEE_V1: pure server-owned PC/NPC ordinary-melee arithmetic.
 * Player equipment/STR/class terms are the existing NEW_MATCH_MELEE_V1 terms,
 * not a new classic player import. NPC target hit/AC/size/material and outgoing
 * hit/STR/night terms are separate from PC/PvP defense. No SQL, owned RNG,
 * HP/cooldown publication, active buffs, proc, global classic configuration,
 * source data migration, client permission, or Quest admission is supplied here.
 *
 * NPC decimal factors are exact rationals, with truncation after the leverage-10
 * layer. Boss equipment terms remain unqualified and boss combat is rejected.
 * This deliberately removes binary64 rounding
 * artifacts and native int saturation; it is NOT a bit-for-bit classic double
 * emulator. Final out-of-i64 results fail before any state can be committed.
 */
final class ModernNpcMelee {
    static final String RULESET = "MODERN_NPC_MELEE_V1";
    static final String PC_REFERENCE_SHA256 = "3FDC94CEDC6C3EC803E731BE238093BD7FE55C432CF84BE6822A079A1627DDFC";
    static final String NPC_REFERENCE_SHA256 = "7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8";
    enum Failure { UNSUPPORTED, INVALID_STATE, INVALID_RANDOM, ARITHMETIC_OVERFLOW }
    static final class CombatException extends IllegalArgumentException {
        private final Failure failure;
        CombatException(Failure failure) { super("modern NPC melee: " + failure); this.failure = failure; }
        Failure failure() { return failure; }
    }
    enum BlessingConvention { LEGACY_ZERO_BLESSED, INTERNAL_ONE_BLESSED }
    /** Explicit operator/source contract. No default, guessing from 0/1, or packet-code reinterpretation. */
    record BlessingBinding(int sourceId, String catalogDatabase, BlessingConvention convention) {
        BlessingBinding {
            if (sourceId < 1) throw invalid(); ModernPhysicalCombat.identifier(catalogDatabase); Objects.requireNonNull(convention);
        }
        @Override public String toString() { return "NpcMeleeBlessingBinding[redacted]"; }
    }
    static final class Player {
        private final ModernPhysicalCombat.Fighter fighter;
        private final BlessingBinding blessing;
        private Player(ModernPhysicalCombat.Fighter fighter, BlessingBinding blessing) { this.fighter = fighter; this.blessing = blessing; }
        ModernPhysicalCombat.Fighter fighter() { return fighter; }
        private boolean blessed() {
            var weapon = fighter.profile().weapon().facts();
            return weapon != null && weapon.templateBlessing() == (blessing.convention() == BlessingConvention.LEGACY_ZERO_BLESSED ? 0 : 1);
        }
        @Override public String toString() { return "NpcMeleePlayer[redacted]"; }
    }
    static Player player(ModernPhysicalCombat.Fighter fighter, BlessingBinding binding) {
        Objects.requireNonNull(fighter); Objects.requireNonNull(binding);
        if (fighter.key().sourceId() != binding.sourceId() || !fighter.catalogDatabase().equals(binding.catalogDatabase())) throw invalid();
        if (!fighter.supported()) throw unsupported();
        return new Player(fighter, binding);
    }

    /** Qualified incoming defense. A BOT never receives a fabricated SQL character or Fighter. */
    static final class Victim {
        private final ModernParticipantId identity;
        private final long maximumHp, evasion;
        private final ModernPhysicalCombat.Defense defense;
        private Victim(ModernParticipantId identity, long maximumHp, long evasion, ModernPhysicalCombat.Defense defense) {
            this.identity = identity; this.maximumHp = maximumHp; this.evasion = evasion; this.defense = defense;
        }
        ModernParticipantId identity() { return identity; }
        @Override public String toString() { return "NpcMeleeVictim[redacted]"; }
    }
    static Victim victim(Player player) {
        Objects.requireNonNull(player); var fighter = player.fighter;
        return new Victim(ModernParticipantId.human(fighter.key()), fighter.profile().effective().maximumHp(),
                fighter.profile().equipmentEvaluation().total(ModernEquipmentFacts.Stat.EVASION), fighter.defense());
    }
    static Victim victim(ModernParticipantId identity, ModernBotTemplate template) {
        Objects.requireNonNull(identity); Objects.requireNonNull(template);
        if (identity.isHuman()) throw invalid();
        // ROOM_TEST_DUMMY_V1 explicitly has zero evasion and fixed defense. This
        // adapter grants no offense, equipment binding, learned skills or owner.
        return new Victim(identity, template.maxHp(), 0, template.physicalValues().defense());
    }

    /** Result carries both pre-defense and post-defense damage, before HP/overkill limiting. */
    record Outcome(boolean hit, long rawDamage, long damage, long appliedDamage, long remainingHp,
                   boolean death, boolean maximumTriggered, boolean criticalTriggered, long materialBonus, long blessingBonus) {
        Outcome {
            if (damage < 0 || appliedDamage < 0 || appliedDamage > damage || remainingHp < 0
                    || materialBonus < 0 || materialBonus > 20 || blessingBonus < 0 || blessingBonus > 4
                    || !hit && (rawDamage != 0 || damage != 0 || appliedDamage != 0 || death || maximumTriggered || criticalTriggered || materialBonus != 0 || blessingBonus != 0)
                    || death != (remainingHp == 0 && appliedDamage > 0)) throw invalid();
        }
    }
    /**
     * Opaque victim-side plan, not a player attack grant. The enclosing match
     * owner must first authorize both identities, geometry, lane, phase, player
     * life/equipment/cooldown and atomically commit all attacker-side costs.
     */
    static final class Incoming {
        private final ModernNpcRoster.Scope scope;
        private final long revision;
        private final ModernNpcRoster.Actor before;
        private final TournamentPresence.CharacterKey attacker;
        private final long equipmentRevision;
        private final Outcome outcome;
        private Incoming(ModernNpcRoster.Snapshot frame, ModernNpcRoster.Actor before, Player player, Outcome outcome) {
            scope = frame.scope(); revision = frame.revision(); this.before = before;
            attacker = player.fighter.key(); equipmentRevision = player.fighter.profile().equipmentRevision(); this.outcome = outcome;
        }
        ModernNpcRoster.Scope scope() { return scope; }
        long revision() { return revision; }
        ModernNpcRoster.Actor before() { return before; }
        TournamentPresence.CharacterKey attacker() { return attacker; }
        long equipmentRevision() { return equipmentRevision; }
        Outcome outcome() { return outcome; }
        @Override public String toString() { return "NpcIncomingMelee[redacted]"; }
    }
    static Incoming prepareIncoming(Player player, ModernNpcCatalog.Catalog catalog, ModernNpcRoster.Snapshot frame,
                                    ModernNpcRoster.Id id, long life, int weightHitPenalty, ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(player); Objects.requireNonNull(frame); Objects.requireNonNull(id);
        if (frame.closed() || !id.scope().equals(frame.scope()) || life < 1) throw invalid();
        var before = frame.actors().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow(ModernNpcMelee::invalid);
        if (!before.alive() || before.life() != life) throw invalid();
        return new Incoming(frame, before, player, againstNpc(player, catalog, before, weightHitPenalty, random));
    }

    static Outcome againstNpc(Player attacker, ModernNpcCatalog.Catalog catalog, ModernNpcRoster.Actor defender,
                              int weightHitPenalty, ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(attacker); Objects.requireNonNull(random);
        var npc = checkedNpc(catalog, defender);
        var pc = attacker.fighter; var values = pc.values();
        if (weightHitPenalty != 0 && weightHitPenalty != 1 && weightHitPenalty != 3 && weightHitPenalty != 5) throw invalid();
        // Hard-skin accepted-hit weapon durability is not implemented by a pure damage result.
        if (npc.get(HARD) != 0 && !values.unarmed()) throw unsupported();
        try {
            long rate = Math.subtractExact(values.offense().totalHit(), weightHitPenalty);
            long dice = add(draw(random, 20), 2, rate, -10);
            // PC -> NPC uses a FIXED target die, including negative AC; NOT the PvP 1.5*AC draw.
            long defense = Math.subtractExact(10, npc.get(AC));
            int chance = dice <= Math.subtractExact(rate, 9) ? 15 : dice >= Math.addExact(rate, 10) ? 100 : dice > defense ? 100 : 15;
            if (one(random, 100) > chance) return miss(defender.hp());
            long base, material = 0, blessing = 0; boolean maximum = false, critical = false;
            if (values.unarmed()) base = Math.max(0, add((draw(random, 5) + 4) >> 2, values.offense().classDamage()));
            else {
                var weapon = values.weapon();
                long dieMaximum = npc.large() && weapon.damageLarge() > 0 ? weapon.damageLarge() : weapon.damageSmall();
                if (dieMaximum < 1) throw unsupported();
                maximum = weapon.maximumDamageChance() > 0 && one(random, 100) <= weapon.maximumDamageChance();
                long die = maximum ? dieMaximum : one(random, weapon.maximumDamageChance() > 0 && dieMaximum > 1 ? dieMaximum - 1 : dieMaximum);
                int materialMaximum = materialMaximum(weapon.material(), npc.get(UNDEAD));
                if (materialMaximum > 0) material = one(random, materialMaximum);
                if (attacker.blessed() && npc.get(UNDEAD) >= 1 && npc.get(UNDEAD) <= 3) blessing = one(random, 4);
                var o = values.offense();
                base = Math.max(0, add(die, o.growthDamage(), o.originalDamage(), o.itemDamage(), o.templateDamage(), o.enchantDamage(), material, blessing, o.classDamage()));
                critical = weapon.criticalChance() > 0 && one(random, 100) <= weapon.criticalChance();
                if (critical) base = values.multiplier().applyAtLeastOne(base);
            }
            long maximumAcReduction = Math.max(0, Math.subtractExact(10, npc.get(AC))) / 7;
            long ac = maximumAcReduction == 0 ? 0 : add(maximumAcReduction, draw(random, maximumAcReduction));
            // Player ignore-armor/PvP/directional reduction is not NPC template reduction.
            long damage = Math.max(0, Math.subtractExact(base, add(ac, npc.get(REDUCTION))));
            return hit(base, damage, defender.hp(), maximum, critical, material, blessing);
        } catch (ArithmeticException overflow) { throw overflow(); }
    }

    static Outcome againstPlayer(ModernNpcCatalog.Catalog catalog, ModernNpcRoster.Actor attacker, Player defender,
                                 long currentHp, boolean night, ModernPhysicalCombat.BoundedRandom random) {
        return againstParticipant(catalog, attacker, victim(defender), currentHp, night, random);
    }
    static Outcome againstParticipant(ModernNpcCatalog.Catalog catalog, ModernNpcRoster.Actor attacker, Victim defender,
                                      long currentHp, boolean night, ModernPhysicalCombat.BoundedRandom random) {
        return againstProtectedParticipant(catalog,attacker,defender,currentHp,night,random,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<Outcome> againstProtectedParticipant(ModernNpcCatalog.Catalog catalog,
            ModernNpcRoster.Actor attacker,Victim defender,long currentHp,boolean night,ModernPhysicalCombat.BoundedRandom random,
            ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(defender); Objects.requireNonNull(random);
        Objects.requireNonNull(protection);protection.requireTarget(defender.identity());
        var npc = checkedNpc(catalog, attacker);
        if (currentHp < 1 || currentHp > defender.maximumHp) throw invalid();
        try {
            long evasion = defender.evasion;
            if (evasion != 0 && one(random, 1000) <= evasion) return new ModernIncomingDamage.Resolved<>(miss(currentHp),protection.apply(0,currentHp));
            long rate = add(npc.get(LEVEL), 5);
            long dice = add(draw(random, 20), 1, rate, -3);
            long ac = defender.defense.armorClass();
            long defense = ac >= 0 ? Math.subtractExact(10, ac) : add(10, one(random, Math.negateExact(ac)));
            int chance = dice <= rate ? 15 : dice >= add(rate, 19) ? 100 : dice > defense ? 100 : 15;
            // Native NPC->PC boss +30 occurs AFTER chance selection; never move it ahead of this comparison.
            if (one(random, 100) > chance) return new ModernIncomingDamage.Resolved<>(miss(currentHp),protection.apply(0,currentHp));
            long levelRoll = draw(random, npc.get(LEVEL)); boolean critical = draw(random, 100) < 15;
            boolean undeadNight = night && (npc.get(UNDEAD) == 1 || npc.get(UNDEAD) == 3 || npc.get(UNDEAD) == 4);
            long raw = sourceSubtotal(npc.get(LEVEL), npc.get(STR), catalog.growth().at(false, npc.get(STR)).damage(), levelRoll, critical, undeadNight);
            var d = defender.defense;
            long acReduction = d.acMaximum() == 0 ? 0 : add(draw(random, d.acMaximum()), Math.max(1, d.acMaximum() >> 3));
            long armor = d.armorReduction() <= 10 ? d.armorReduction() : add(11, draw(random, Math.subtractExact(d.armorReduction(), 10)));
            // No directional/PvP/ignore-reduction branch exists in this clean NPC->PC path.
            long damage = Math.max(0, Math.subtractExact(raw, add(acReduction, armor)));
            var impact=protection.apply(damage,currentHp);
            return new ModernIncomingDamage.Resolved<>(hit(raw,impact.damage(),currentHp,false,critical,0,0),impact);
        } catch (ArithmeticException overflow) { throw overflow(); }
    }

    /** Signed subtotal: clamp only AFTER defense, so negative source/armor terms are not silently reordered. */
    static long sourceSubtotal(long level, long strength, long strengthDamage, long levelRoll,
                               boolean critical, boolean undeadNight) {
        if (level < 1 || strength < 0 || levelRoll < 0 || levelRoll >= level) throw invalid();
        try {
            // Bounded i64 inputs, fixed factors and no loops/external decimal scale: intermediates stay < 256 bits.
            BigInteger numerator = BigInteger.valueOf(levelRoll).add(BigInteger.valueOf(strengthDamage)).multiply(BigInteger.valueOf(5))
                    .add(BigInteger.valueOf(strength).multiply(BigInteger.valueOf(4)));
            long denominator = 5;
            if (critical) { numerator = numerator.multiply(BigInteger.valueOf(9)); denominator *= 5; }
            if (undeadNight) { numerator = numerator.multiply(BigInteger.valueOf(6)); denominator *= 5; }
            BigInteger whole = numerator.divide(BigInteger.valueOf(denominator)); // leverage=10: truncate toward zero here.
            return whole.longValueExact();
        } catch (ArithmeticException overflow) { throw overflow(); }
    }
    private static ModernNpcCatalog.Npc checkedNpc(ModernNpcCatalog.Catalog catalog, ModernNpcRoster.Actor actor) {
        Objects.requireNonNull(catalog); Objects.requireNonNull(actor);
        if (!actor.alive()) throw invalid();
        var template = actor.template();
        var npc = catalog.require(new ModernNpcCatalog.Key(template.catalogSourceId(), template.catalogDatabase(), template.npcId()));
        npc.validateActor(actor);
        // Remaining behaviors and per-PC boss damage/reduction cannot be silently discarded.
        if (!npc.pendingCleanMelee().isEmpty() || npc.get(BOSS) != 0) throw unsupported();
        return npc;
    }
    private static int materialMaximum(int material, long undead) {
        if (undead == 1 || undead == 3 || undead == 5) return material == 14 || material == 17 || material == 22 ? 20 : 0;
        return undead == 2 && (material == 17 || material == 22) ? 3 : 0;
    }
    private static Outcome miss(long hp) { return new Outcome(false, 0, 0, 0, hp, false, false, false, 0, 0); }
    private static Outcome hit(long raw, long damage, long hp, boolean maximum, boolean critical, long material, long blessing) {
        long applied = Math.min(damage, hp), remaining = hp - applied;
        return new Outcome(true, raw, damage, applied, remaining, remaining == 0, maximum, critical, material, blessing);
    }
    private static long draw(ModernPhysicalCombat.BoundedRandom random, long bound) {
        if (bound < 1) throw new CombatException(Failure.INVALID_RANDOM);
        long value = random.nextLong(bound); if (value < 0 || value >= bound) throw new CombatException(Failure.INVALID_RANDOM); return value;
    }
    private static long one(ModernPhysicalCombat.BoundedRandom random, long bound) { return Math.addExact(draw(random, bound), 1); }
    private static long add(long... values) { long result = 0; for (long value : values) result = Math.addExact(result, value); return result; }
    private static CombatException invalid() { return new CombatException(Failure.INVALID_STATE); }
    private static CombatException unsupported() { return new CombatException(Failure.UNSUPPORTED); }
    private static CombatException overflow() { return new CombatException(Failure.ARITHMETIC_OVERFLOW); }
    private ModernNpcMelee() {}
}
