package local.lito381.server;

import java.lang.reflect.*;
import java.util.Arrays;

/** Ctrl intent belongs to one decoded request, never to a session-wide toggle. */
final class ForcedCombat {
    private record Intent(ClientSession attacker, long target) {}
    private static final ThreadLocal<Intent> CURRENT = new ThreadLocal<>();
    private static final byte[] MAGIC = {0x4c,0x46,0x43,0x31};
    private static final Method HANDLE;
    static {
        try {
            HANDLE = ClientSession.class.getDeclaredMethod("handleBeforeForcedCombat", byte[].class);
            HANDLE.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }

    static long target(byte[] packet) {
        if (packet == null || packet.length == 0) return 0;
        int op = packet[0] & 255, offset;
        if (op == 0x17 || op == 0x18 || op == 0x7a) {
            if (packet.length != 9) return 0;
            offset = 1;
        } else if (op == 0x14 && (packet.length == 7 || packet.length == 11)) {
            int skill = ((packet[1]&255)<<3)+(packet[2]&255)+1;
            // These same-length native layouts carry coordinates, ticks,
            // summon selections or strings, not a combat target object.
            if ((packet[2]&255)>7 || skill>220 || skill==5 || skill==51
                    || skill==58 || skill==60 || skill==63 || skill==69
                    || skill==116 || skill==118) return 0;
            offset = 3;
        } else return 0;
        return (packet[offset]&255L) | ((packet[offset+1]&255L)<<8)
                | ((packet[offset+2]&255L)<<16) | ((packet[offset+3]&255L)<<24);
    }

    static byte[] unwrap(byte[] packet) {
        if (packet == null || packet.length < 15) return packet;
        int end = packet.length-8;
        for (int i=0;i<4;i++) if (packet[end+i]!=MAGIC[i]) return packet;
        if (packet[end+4]!=packet[0] || packet[end+5]!=1
                || packet[end+6]!=0x67 || (packet[end+7]&255)!=0xa5)
            throw new IllegalArgumentException("invalid forced-combat extension");
        byte[] original = Arrays.copyOf(packet,end);
        if (target(original)==0) throw new IllegalArgumentException("forced-combat target required");
        return original;
    }

    static void dispatch(ClientSession session, byte[] packet) throws Exception {
        byte[] original = unwrap(packet);
        Intent previous = CURRENT.get();
        // Nested ordinary dispatch must not inherit the caller's Ctrl intent.
        CURRENT.remove();
        if (original != packet) CURRENT.set(new Intent(session,target(original)));
        try { HANDLE.invoke(session,(Object)original); }
        catch (InvocationTargetException e) {
            Throwable cause=e.getCause();
            if (cause instanceof Exception x) throw x;
            if (cause instanceof Error x) throw x;
            throw new IllegalStateException(cause);
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    static boolean canAttack(boolean original, GameHub hub, long roomId,
                             ClientSession attacker, ClientSession target) {
        if (original) return true;
        Intent intent = CURRENT.get();
        if (intent == null || intent.attacker()!=attacker || attacker==target
                || target==null || hub.roomRules(roomId).mode()==BattleRules.Mode.PET_RACE)
            return false;
        var members=hub.roomCombatants(roomId);
        if (!members.contains(attacker) || !members.contains(target)) return false;
        // Preserve all range/LOS/life/observer/skill gates in the original
        // consumers. Only the selected target's faction gate is relaxed.
        if (intent.target()==target.playerObjectId()) return true;
        return target.controlledCompanionSnapshots().stream()
                .anyMatch(p->p.objectId()==intent.target() && p.active());
    }

    /** Multi-target consumers distinguish the selected object from its owner. */
    static boolean canAttackTarget(GameHub hub, long roomId, ClientSession attacker,
                                   ClientSession owner, long objectId) {
        Intent intent = CURRENT.get();
        if (intent == null || intent.attacker() != attacker || intent.target() == objectId)
            return hub.canAttack(roomId, attacker, owner);
        CURRENT.remove();
        try { return hub.canAttack(roomId, attacker, owner); }
        finally { CURRENT.set(intent); }
    }
}
