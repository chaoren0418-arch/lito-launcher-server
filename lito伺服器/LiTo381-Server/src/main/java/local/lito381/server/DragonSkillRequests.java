package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/** Explicit new-class C_USE_SPELL targets, scoped to one synchronous dispatch. */
final class DragonSkillRequests {
    private static final Set<Integer> TARGETED = Set.of(183,184,187,188,192,193,194);
    private static final ThreadLocal<Protocol.AttackSpellRequest> REQUEST = new ThreadLocal<>();
    private static final Method LEGACY;
    private static final Method COMPANION;
    static {
        try {
            LEGACY = ClientSession.class.getDeclaredMethod("useSpellBeforeDragonTargets", byte[].class);
            LEGACY.setAccessible(true);
            Class<?> target = Class.forName(ClientSession.class.getName()+"$ArmorBreakTarget");
            COMPANION = target.getDeclaredMethod("companion");
            COMPANION.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    static void dispatch(ClientSession session, byte[] payload) throws IOException {
        if (IllusionSkillRequests.dispatch(session,payload)) return;
        Protocol.SpellSelector selector = Protocol.spellSelector(payload);
        byte[] forwarded = payload;
        if (TARGETED.contains(selector.skillId())) {
            // Old clients cannot specify these targets. Do not guess a nearby victim.
            if (payload.length != 11) {
                System.out.println("[LiTo] Dragon spell rejected missing target skill="+selector.skillId());
                return;
            }
            Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(payload);
            if (request.targetObjectId() <= 0) return;
            REQUEST.set(request);
            forwarded = Arrays.copyOf(payload,3);
            System.out.println("[LiTo] Dragon explicit target skill="+selector.skillId()+" target="+request.targetObjectId());
        }
        try { LEGACY.invoke(session, (Object)forwarded); }
        catch (InvocationTargetException e) {
            Throwable cause=e.getCause();
            if(cause instanceof IOException x) throw x;
            if(cause instanceof RuntimeException x) throw x;
            if(cause instanceof Error x) throw x;
            throw new IOException(cause);
        } catch(IllegalAccessException e) { throw new IOException(e); }
        finally { REQUEST.remove(); }
    }
    static Protocol.AttackSpellRequest current() { var request=REQUEST.get(); return request!=null?request:IllusionSkillRequests.current(); }
    static long guardTarget(long previous,int x,int y,int range,List<BattleGuardBrake.Candidate> candidates) {
        var request=REQUEST.get();
        if(request==null) return BattleGuardBrake.resolveTarget(previous,x,y,range,candidates);
        return candidates.stream().filter(c->c.objectId()==request.targetObjectId()
            && Math.max(Math.abs(x-c.x()),Math.abs(y-c.y()))<=range)
            .mapToLong(BattleGuardBrake.Candidate::objectId).findFirst().orElse(0);
    }
    static long fearTarget(long previous,int x,int y,int range,List<BattleResistFear.Candidate> candidates) {
        var request=REQUEST.get();
        if(request==null) return BattleResistFear.resolveTarget(previous,x,y,range,candidates);
        return candidates.stream().filter(c->c.objectId()==request.targetObjectId()
            && Math.max(Math.abs(x-c.x()),Math.abs(y-c.y()))<=range)
            .mapToLong(BattleResistFear.Candidate::objectId).findFirst().orElse(0);
    }
    private static BattleCompanion companion(Object target) {
        try { return (BattleCompanion)COMPANION.invoke(target); }
        catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
    }
    static int fearNpcId(int original,Object target) {
        BattleCompanion c=companion(target);
        return c==null?original:c.snapshot().npcId();
    }
    static int fearMagicResistance(SourceNpcPhysicalStats stats,Object target,long now) {
        BattleCompanion c=companion(target);
        return c==null?stats.magicResistance():c.effectiveMagicResistance(stats.magicResistance(),now);
    }
}
