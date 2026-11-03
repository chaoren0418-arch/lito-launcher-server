package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** v6 independent targeted-cast history. The v4 teleport history is never replaced. */
final class ModernDamageSpellProtocol {
    static final int STATE_BYTES = 64;
    record State(long entityId, long castCount, long castRevision, long mpSpent, int skillId,
                 long targetEntityId, int fromX, int fromY, int toX, int toY, int mpCost) {
        State {
            if (entityId < 1 || castCount < 0 || castRevision < 0 || mpSpent < 0 || mpCost < 1 || mpCost > 3
                    || Math.multiplyExact(castCount, mpCost) != mpSpent) throw bad();
            if (castCount == 0) {
                if (castRevision != 0 || skillId != 0 || targetEntityId != 0 || fromX != 0 || fromY != 0 || toX != 0 || toY != 0) throw bad();
            } else if (castRevision == 0 || skillId != 4 || targetEntityId < 1 || targetEntityId == entityId
                    || fromX == toX && fromY == toY
                    || Math.max(Math.abs((long) fromX - toX), Math.abs((long) fromY - toY)) > 10) throw bad();
        }
        static State initial(long entityId, int mpCost) { return new State(entityId,0,0,0,0,0,0,0,0,0,mpCost); }
        State wireRevision() { return new State(entityId,castCount,castRevision == 0 ? 0 : Math.addExact(castRevision,1),
                mpSpent,skillId,targetEntityId,fromX,fromY,toX,toY,mpCost); }
    }
    private ModernDamageSpellProtocol() {}
    static void validate(ModernWorldProtocol.Frame world, List<State> states) {
        validate(world,states,ModernHonor.Snapshot.disabled());
    }
    static void validate(ModernWorldProtocol.Frame world,List<State> states,ModernHonor.Snapshot honor) {
        if (states.isEmpty()) return;
        if (states.size() != world.actors().size() || world.casts().size() != states.size()) throw bad();
        for (int i = 0; i < states.size(); i++) {
            var state = states.get(i); var teleport = world.casts().get(i);
            if (state.entityId() != i + 1 || state.castRevision() > world.revision()
                    || Math.addExact(state.castCount(),teleport.castCount()) > teleport.requestSequence()) throw bad();
            if (state.castCount() > 0 && (world.actors().stream().noneMatch(actor -> actor.entityId() == state.targetEntityId())&&honor.objective(state.targetEntityId())==null
                    || !ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(state.fromX(),state.fromY()))
                    || !ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(state.toX(),state.toY())))) throw bad();
            if (world.completion() != null && state.castRevision() > Math.addExact(world.completion().event().engineRevision(),1)) throw bad();
        }
    }
    static void put(ByteBuffer out, List<State> states) {
        out.putShort((short)states.size()).putShort((short)0);
        for (var s : states) out.putLong(s.entityId()).putLong(s.castCount()).putLong(s.castRevision()).putLong(s.mpSpent())
                .putInt(s.skillId()).putLong(s.targetEntityId()).putInt(s.fromX()).putInt(s.fromY()).putInt(s.toX()).putInt(s.toY()).putInt(s.mpCost());
    }
    static List<State> get(ByteBuffer in, int actorCount) throws ModernApplicationProtocol.ProtocolException {
        int count = Short.toUnsignedInt(in.getShort());
        if (in.getShort() != 0 || count != 0 && count != actorCount || count > 100 || in.remaining() < count * STATE_BYTES)
            throw new ModernApplicationProtocol.ProtocolException();
        var states = new ArrayList<State>(count);
        for (int i = 0; i < count; i++) states.add(new State(in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getInt(),
                in.getLong(),in.getInt(),in.getInt(),in.getInt(),in.getInt(),in.getInt()));
        return List.copyOf(states);
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid targeted magic history"); }
}
