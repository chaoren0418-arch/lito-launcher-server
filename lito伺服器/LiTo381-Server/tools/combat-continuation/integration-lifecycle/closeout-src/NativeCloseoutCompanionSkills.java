package local.lito381.server;

import java.util.Arrays;

/** Actor-owned Source mobskill state. No scheduler, registry, target or owner retention. */
final class NativeCloseoutCompanionSkills {
    static final class State {
        final NativeSourceNpcFactoryType.Stamp factory;
        final NativeCloseoutControlledNpcData.Data data;
        final int[] counts;
        final boolean[] cooling;
        final long[] until;
        Object generation = new Object();
        State(NativeSourceNpcFactoryType.Stamp factory, NativeCloseoutControlledNpcData.Data data) {
            this.factory = factory; this.data = data;
            counts = new int[data.rows().size()]; cooling = new boolean[counts.length]; until = new long[counts.length];
        }
    }
    /** Caller must retain actor then timed-status monitors throughout selection, effect admission and accounting.
     * Existing owner/scene/target admission is still required by the effect adapter. */
    record Lease(BattleCompanion pet, State state, Object life, Object generation) {
        boolean current() {
            monitor(pet);
            if (!Thread.holdsLock(CombatAccess.field(pet, "timedStatus")))
                throw new IllegalStateException("companion mobskill status monitor required");
            return generation != null && state.generation == generation
                    && pet.nativeCloseoutMobSkillState == state && NativeSourceNpcFactoryType.stamp(pet) == state.factory
                    && NativeSourceControlledDisplay.rawLife(pet) == life
                    && (boolean) CombatAccess.field(pet, "active") && NativeSourceRawDamage.number(pet, "hp") > 0;
        }
        private void row(int row) {
            if (!current()) throw new IllegalStateException("stale companion mobskill lease");
            if (row < 0 || row >= state.counts.length) throw new IndexOutOfBoundsException(row);
        }
        int useCount(int row) { row(row); return state.counts[row]; }
        boolean delayed(int row, long now) {
            row(row);
            if (state.cooling[row] && now - state.until[row] >= 0) state.cooling[row] = false;
            return state.cooling[row];
        }
        void incrementUseCount(int row) { row(row); state.counts[row]++; }
        void startDelay(int millis, int row, long now) {
            row(row);
            // Source nonpositive delay is a no-op; an existing delay is never extended.
            if (millis <= 0 || delayed(row, now)) return;
            state.until[row] = now + (long) millis * 1_000_000L;
            state.cooling[row] = true;
        }
    }
    /** Only called immediately after the constructor/evolution's accepted factory assignment. */
    static void install(BattleCompanion pet) {
        var factory = NativeSourceNpcFactoryType.stamp(pet);
        pet.nativeCloseoutMobSkillState = factory != null && (factory.kind() == 5 || factory.kind() == 6)
                && factory.nativeCloseoutControlledNpcData instanceof NativeCloseoutControlledNpcData.Data data
                && data.npc() == factory.npc() ? new State(factory, data) : null;
    }
    static Lease capture(BattleCompanion pet) {
        monitor(pet);
        if (!(pet.nativeCloseoutMobSkillState instanceof State state)) return null;
        if (state.generation == null) state.generation = new Object();
        var lease = new Lease(pet, state, NativeSourceControlledDisplay.rawLife(pet), state.generation);
        return lease.current() ? lease : null;
    }
    /** Source NpcAI.finishAI resets use counts only, leaving reuse timers running. */
    static void finish(BattleCompanion pet) {
        monitor(pet);
        if (pet.nativeCloseoutMobSkillState instanceof State state) {
            Arrays.fill(state.counts, 0); state.generation = null;
        }
    }
    static void terminal(BattleCompanion pet) {
        monitor(pet);
        if (!(boolean) CombatAccess.field(pet, "active") || NativeSourceRawDamage.number(pet, "hp") <= 0) finish(pet);
    }
    /** Existing BATTLE/WORLD tick; rest/no-target is a Source AI finish, ordinary target loss is not. */
    static void tick(BattleCompanion pet) {
        synchronized (pet) {
            if (pet.nativeCloseoutMobSkillState == null) return;
            terminal(pet);
            if (CombatAccess.field(pet, "posture") == BattleCompanion.Posture.STAY
                    && ((Number) CombatAccess.field(pet, "targetObjectId")).longValue() == 0) finish(pet);
        }
    }
    private static void monitor(BattleCompanion pet) {
        if (!Thread.holdsLock(pet)) throw new IllegalStateException("companion mobskill actor monitor required");
    }
    private NativeCloseoutCompanionSkills() {}
}
