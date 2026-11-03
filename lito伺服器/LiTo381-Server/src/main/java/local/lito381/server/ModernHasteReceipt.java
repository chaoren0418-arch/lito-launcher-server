package local.lito381.server;

import java.util.Objects;

/** Event-time status target; never a fabricated damage/healing impact.
 * The caster's ordinary use/resource ledger remains the unique MP authority.
 * Public projection removes the enclosing private source/clock; only explicit
 * v44 negotiation can carry the complete skill43 state. */
record ModernHasteReceipt(long activeElapsedNanos, ModernHasteReceipt.Target target) implements ModernSpellRuntime.Detail {
    sealed interface Target permits Player, Npc { ModernWorldMap.Cell cell(); long life(); }
    record Player(ModernTargetHistory.Participant participant, long life, long equipmentRevision,
                  ModernWorldMap.Cell cell, ModernTimedEffects.State before, ModernTimedEffects.State after) implements Target {
        Player {
            Objects.requireNonNull(participant); Objects.requireNonNull(cell); Objects.requireNonNull(before); Objects.requireNonNull(after);
            if (life < 1 || equipmentRevision < 0 || before.entityId() != participant.entityId()
                    || after.entityId() != participant.entityId()) throw bad();
        }
    }
    record Npc(ModernNpcRoster.Id id, long life, int lane, ModernWorldMap.Cell cell, long rosterRevision,
               ModernNpcSpeed.State before, ModernNpcSpeed.State after) implements Target {
        Npc {
            Objects.requireNonNull(id); Objects.requireNonNull(cell); Objects.requireNonNull(before); Objects.requireNonNull(after);
            if (life < 1 || lane < 0 || lane > 1 || rosterRevision < 1 || before.revision() > rosterRevision
                    || after.revision() != Math.incrementExact(rosterRevision)) throw bad();
        }
    }
    ModernHasteReceipt {
        Objects.requireNonNull(target);
        if (activeElapsedNanos < 0 || activeElapsedNanos > ModernTimedEffects.MAX_OBSERVED_NANOS) throw bad();
        if (target instanceof Player p) {
            if (!ModernTimedEffects.applyHaste(p.before(), activeElapsedNanos, p.after().revision()).equals(p.after())) throw bad();
        } else if (target instanceof Npc n) {
            if (!ModernNpcSpeed.haste(n.before(), activeElapsedNanos / 1_000_000, n.after().revision()).equals(n.after())) throw bad();
        }
    }
    void validate(ModernTargetHistory.Batch batch) {
        validate(batch.world(),batch.caster(),batch.lane(),batch.use(),batch.targets());
    }
    /** Same event proof after removing the private source scope/epoch. */
    void validate(ModernTargetHistory.World world,ModernTargetHistory.Participant caster,ModernUseHistory.Lane lane,
                  ModernUseHistory.Cast use,java.util.List<ModernTargetHistory.Effect> targets) {
        if (lane != ModernUseHistory.Lane.HASTE || !targets.isEmpty()
                || use.cost() < 15 || use.cost() > ModernHaste.BASE_MP
                || !ModernHasteCast.inScreen(new ModernWorldMap.Cell(use.before().x(), use.before().y()), target.cell())) throw bad();
        if (target instanceof Player p) {
            if (p.after().revision() != use.revision()) throw bad();
            if (p.participant().entityId() == caster.entityId() || p.participant().identity().equals(caster.identity())) {
                if (!p.participant().equals(caster) || p.life() != use.before().life()
                        || p.equipmentRevision() != use.before().equipmentRevision()
                        || p.cell().x() != use.before().x() || p.cell().y() != use.before().y()) throw bad();
            }
        } else if (target instanceof Npc n) {
            if (!world.contains(n.id().scope()) || n.after().revision() >= use.revision()) throw bad();
        }
    }
    @Override public String toString() { return "HasteReceipt[redacted]"; }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern haste receipt"); }
}
