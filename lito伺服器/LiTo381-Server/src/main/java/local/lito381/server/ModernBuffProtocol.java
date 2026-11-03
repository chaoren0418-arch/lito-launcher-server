package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** v10 append-only, full-roster timed authority. All clocks are relative to this match's active start. */
final class ModernBuffProtocol {
    static final int PREFIX_BYTES = 8, STATE_BYTES = 56;
    private ModernBuffProtocol() {}
    static void putStates(ByteBuffer out, List<ModernTimedEffects.State> states) {
        if (states.size() > 100 || states.stream().anyMatch(s->s.hasteOrigin()!=ModernTimedEffects.HasteOrigin.NONE
                &&s.hasteOrigin()!=ModernTimedEffects.HasteOrigin.POTION)) throw bad();
        states.forEach(ModernTimedEffects::requireNoSlow);
        out.putShort((short) states.size()); zeroes(out, 6);
        long previous = 0;
        for (var s : states) {
            if (s.entityId() <= previous) throw bad(); previous = s.entityId();
            out.putLong(s.entityId()).putLong(s.revision()).putLong(s.observedNanos())
                    .putLong(s.hasteUntilNanos()).putLong(s.braveUntilNanos()).putLong(s.wisdomUntilNanos())
                    .put((byte) s.braveKind()); zeroes(out, 7);
        }
    }
    static List<ModernTimedEffects.State> getStates(ByteBuffer in, int actorCount)
            throws ModernApplicationProtocol.ProtocolException {
        int count = Short.toUnsignedInt(in.getShort()); zero(in, 6);
        if (actorCount < 1 || actorCount > 100 || count > 100 || count != 0 && count != actorCount
                || in.remaining() < count * STATE_BYTES) fail();
        var states = new ArrayList<ModernTimedEffects.State>(count);
        long previous = 0;
        for (int i = 0; i < count; i++) {
            long entity = in.getLong(), revision = in.getLong(), observed = in.getLong();
            long haste = in.getLong(), brave = in.getLong(), wisdom = in.getLong();
            int kind = Byte.toUnsignedInt(in.get()); zero(in, 7);
            if (entity <= previous) fail(); previous = entity;
            states.add(new ModernTimedEffects.State(entity, revision, observed, haste, brave, wisdom, kind));
        }
        return List.copyOf(states);
    }
    static void validateStates(ModernWorldProtocol.Frame world, List<ModernCombatProtocol.Vital> vitals,
            List<ModernItemProtocol.State> items, List<ModernTimedEffects.State> buffs) {
        if (buffs.isEmpty()) {
            for (var item : items) {
                if (ModernTimedEffects.Kind.find(item.itemId()) != null
                        || item.stocks().stream().anyMatch(s -> ModernTimedEffects.Kind.find(s.itemId()) != null)) throw bad();
            }
            return;
        }
        if (buffs.size() != world.actors().size() || buffs.size() != items.size() || buffs.size() != vitals.size()) throw bad();
        for (int i = 0; i < buffs.size(); i++) {
            var s = buffs.get(i); var actor = world.actors().get(i); var vital = vitals.get(i); var item = items.get(i);
            ModernTimedEffects.requireNoSlow(s);
            if(s.hasteOrigin()!=ModernTimedEffects.HasteOrigin.NONE&&s.hasteOrigin()!=ModernTimedEffects.HasteOrigin.POTION)throw bad();
            if (s.entityId() != actor.entityId() || s.entityId() != vital.entityId() || s.entityId() != item.entityId()
                    || s.revision() > world.revision()) throw bad();
            if (!actor.member().identity().isHuman() && !s.equals(ModernTimedEffects.initial(s.entityId()))) throw bad();
            if (world.phase() != ModernWorldProtocol.Phase.ACTIVE && s.hasEffects() || vital.hp() == 0 && s.hasEffects()) throw bad();
            if(s.hasEffects()&&s.revision()<=vital.deathRevision())throw bad();
            if ((world.phase() == ModernWorldProtocol.Phase.LOADING || world.phase() == ModernWorldProtocol.Phase.COUNTDOWN)
                    && s.revision() != 0) throw bad();
            for (var kind : ModernTimedEffects.Kind.values()) {
                if (s.holds(kind)) {
                    if (!kind.eligible(actor.member().characterClass())
                            || item.stocks().stream().noneMatch(stock -> stock.itemId() == kind.itemId && stock.remaining() < stock.initial())) throw bad();
                }
            }
            if ((s.hasteUntilNanos() != 0 || s.braveKind() != 0)
                    && (actor.stepIntervalNanos() % 1_000_000 != 0 || vital.attackIntervalNanos() % 1_000_000 != 0)) throw bad();
            var last = ModernTimedEffects.Kind.find(item.itemId());
            if (last != null && item.useRevision() == world.revision()
                    && (s.revision() != world.revision() || !s.holds(last)
                        || s.until(last) - s.observedNanos() != last.durationNanos)) throw bad();
            if (last != null && item.useRevision() == s.revision() && s.revision() != 0
                    && (!s.holds(last) || s.until(last) - s.observedNanos() != last.durationNanos)) throw bad();
            if (world.completion() != null && s.revision() > Math.addExact(world.completion().event().engineRevision(), 1)) throw bad();
        }
    }
    private static void zeroes(ByteBuffer out, int count) { for (int i = 0; i < count; i++) out.put((byte) 0); }
    private static void zero(ByteBuffer in, int count) throws ModernApplicationProtocol.ProtocolException {
        for (int i = 0; i < count; i++) if (in.get() != 0) fail();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern timed frame"); }
    private static void fail() throws ModernApplicationProtocol.ProtocolException { throw new ModernApplicationProtocol.ProtocolException(); }
}
