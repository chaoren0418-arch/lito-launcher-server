package local.lito381.server;

import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.ByteBuffer;
import java.util.Objects;

/** v38 evolution of explicitly negotiated BATTLE_DATA + ACTION_LIFE. Never a
 * legacy typed ACK: it carries the exact operation's schema-2 audience and
 * service persistence lifecycle. This alone does not authorize equipment SET. */
final class ModernBattleOperationProtocol {
    static final int VERSION=38,REQUEST_KIND=64,RESULT_KIND=65;
    static final int MIN_REQUEST_BYTES=ModernBattleActionProtocol.MIN_REQUEST_BYTES;
    static final int MAX_REQUEST_BYTES=ModernBattleActionProtocol.MAX_REQUEST_BYTES;
    static final int TRAILER_BYTES=32;
    static final int MIN_RESULT_BYTES=MIN_REQUEST_BYTES+TRAILER_BYTES+16;
    static final int MAX_RESULT_BYTES=MAX_REQUEST_BYTES+TRAILER_BYTES+ModernBattleWire.MAX_WIRE_BYTES;
    enum Disposition { UNAVAILABLE, APPLIED, DENIED, REJECTED }
    /** Fixed new wire values, not ordinals borrowed from a legacy opcode. */
    enum Reason { NONE,NOT_ACTIVE,STALE_POSITION,TOO_FAST,BLOCKED,OCCUPIED,DEAD,TARGET_DEAD,
        SAME_TEAM,OUT_OF_RANGE,UNSUPPORTED,NUMERIC,TOO_HEAVY,STALE_EFFECT,NO_AMMO,STALE_REGEN,
        UNLEARNED,NO_MP,NO_DESTINATION,EMPTY,CLASS_RESTRICTED,ATTACKER,NOT_AT_PORTAL,WRONG_LANE,
        STALE_STATE,LAWFUL,STALE_LIFE,WAIT,NO_TARGET,BUDGET }
    record Outcome(Disposition disposition,Reason reason) {}
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        return p!=null&&p.version()>=VERSION&&ModernBattleActionProtocol.negotiated(p)
                &&ModernBattleReadProtocol.negotiated(p);
    }
    static byte[] encodeRequest(BattleOperationRequest r,int version)throws ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION)throw new ProtocolException();
        var b=ModernBattleActionProtocol.encodeRequest(new BattleActionRequest(r.requestId(),r.request()),version);
        ByteBuffer.wrap(b).putShort(12,(short)REQUEST_KIND);return b;
    }
    static Outcome outcome(ModernBattleActionProtocol.Request request,ModernBattleActionReceipt receipt) {
        if(receipt.rejection()!=ModernBattleCommandLife.Rejection.NONE)return new Outcome(Disposition.REJECTED,Reason.NONE);
        if(!receipt.available())return new Outcome(Disposition.UNAVAILABLE,Reason.NOT_ACTIVE);
        var command=request.command();var o=receipt.outcome();String reason;
        if(command instanceof WorldRequest&&o instanceof ModernBattleEngine.MoveResult r)reason=r.status().name();
        else if(command instanceof CombatRequest&&o instanceof ModernBattleEngine.AttackResult r)reason=r.status().name();
        else if(command instanceof CastRequest&&o instanceof ModernBattleEngine.CastResult r)reason=r.reason().name();
        else if(command instanceof ItemRequest&&o instanceof ModernBattleEngine.ItemResult r)reason=r.reason().name();
        else if(command instanceof AmmoRequest&&o instanceof ModernBattleEngine.AmmoResult r)reason=r.reason().name();
        else if(command instanceof DisintegrateRequest&&o instanceof ModernBattleEngine.DisintegrateResult r)reason=r.reason().name();
        else if(command instanceof NpcRequest&&o instanceof ModernNpcBattle.Result r)reason=r.reason().name();
        else if(command instanceof NpcSpellRequest&&o instanceof ModernNpcMagic.Result r)reason=r.reason().name();
        else if(command instanceof NpcDisintegrateRequest&&o instanceof ModernNpcDisintegrateState.Result r)reason=r.reason().name();
        else if(command instanceof NpcFireStormRequest&&o instanceof ModernNpcFireStormState.Result r)reason=r.reason().name();
        else if(command instanceof SiegeRequest q&&q.request().action()==ModernSiegeProtocol.Action.ATTACK_TOWER&&o instanceof ModernBattleEngine.AttackResult r)reason=r.status().name();
        else if(command instanceof SiegeRequest q&&q.request().action()==ModernSiegeProtocol.Action.PORTAL&&o instanceof ModernBattleEngine.SiegePortalResult r)reason=r.reason().name();
        else throw bad();
        var normalized=switch(reason){case "MOVED","ATTACKED"->Reason.NONE;case "ABORTED"->Reason.NOT_ACTIVE;default->Reason.valueOf(reason);};
        return new Outcome(normalized==Reason.NONE?Disposition.APPLIED:Disposition.DENIED,normalized);
    }
    static void validate(BattleOperationResult result) {
        Objects.requireNonNull(result);var q=result.echo();var r=result.receipt();var p=q.proof();var v=r.view();
        if(result.requestId()==0||result.requestId()!=q.command().requestId()
                ||!p.player().equals(v.personal().scope())||!p.world().equals(v.world().identity())
                ||p.entity()!=v.personal().entityId()||p.revision()>v.world().revision())throw bad();
        outcome(q,r);
    }
    static byte[] encodeResult(BattleOperationResult result,int version)throws ProtocolException {
        return encodeResult(result,ModernBattleWire.Encoding.legacy(version));
    }
    static byte[] encodeResult(BattleOperationResult result,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        return encodeResult(result,ModernBattleWire.Encoding.selected(peer));
    }
    private static byte[] encodeResult(BattleOperationResult result,ModernBattleWire.Encoding format)throws ProtocolException {
        int version=format.version();
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION)throw new ProtocolException();
        validate(result);var r=result.receipt();var lifecycle=r.lifecycle();var outcome=outcome(result.echo(),r);
        var echo=encodeRequest(new BattleOperationRequest(result.requestId(),result.echo()),version);
        var payload=format.payload(r.view());
        var b=ByteBuffer.allocate(Math.addExact(echo.length+TRAILER_BYTES,payload.length));
        b.put(echo);b.putInt(4,b.capacity()).putShort(12,(short)RESULT_KIND);
        b.put((byte)r.rejection().ordinal()).put((byte)outcome.disposition().ordinal()).put((byte)outcome.reason().ordinal()).put((byte)0);
        putLifecycle(b,lifecycle);
        b.putInt(payload.length).put(payload);return b.array();
    }
    static void putLifecycle(ByteBuffer b,ModernBattleLifecycle lifecycle) {
        b.putLong(lifecycle.revision()).put((byte)lifecycle.phase().ordinal()).put((byte)lifecycle.abortReason().ordinal());
        b.put((byte)(lifecycle.settlement()==null?0:1+lifecycle.settlement().ordinal()));
        var race=lifecycle.race();b.put((byte)race.settlement().ordinal()).putLong(race.terminalRevision()).putInt(0);
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical operation binding");}
    private ModernBattleOperationProtocol(){}
}
