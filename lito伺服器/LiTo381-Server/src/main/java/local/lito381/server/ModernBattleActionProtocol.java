package local.lito381.server;

import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.ByteBuffer;
import java.util.Objects;

/** v37 explicitly negotiated life-bound mutations. Inner opcodes retain their
 * existing typed payload/ACK rules. Rejection is NOT a consumed-sequence ACK.
 * No authentication, SQL/shop operation, LOAD, departure or nested envelope. */
final class ModernBattleActionProtocol {
    static final int VERSION=37, REQUEST_KIND=62, RESULT_KIND=63;
    static final int PREFIX_BYTES=100, MIN_REQUEST_BYTES=188, MAX_REQUEST_BYTES=236;
    static final int MIN_RESULT_BYTES=MIN_REQUEST_BYTES+24;
    static final int MAX_RESULT_BYTES=MAX_REQUEST_BYTES+8+1024*1024;
    enum Kind { NONE, PLAYER, NPC, QUEST, HONOR, TIME_TOWER, SIEGE }
    static final Target NO_TARGET=new Target(Kind.NONE,0,0);
    record Target(Kind kind,long id,long life) {
        Target {
            Objects.requireNonNull(kind);
            boolean valid=switch(kind) {
                case NONE,TIME_TOWER -> id==0&&life==0;
                case PLAYER -> id>=1&&id<=100&&life>=1;
                case NPC -> id>=1&&life>=1;
                case QUEST -> id>=1&&id<=8&&life>=1; // canonical birth revision
                case HONOR -> id>=3&&id<=102&&life==0;
                case SIEGE -> id>=0&&id<ModernSiege.Slot.values().length&&life==0;
            };
            if(!valid)throw bad();
        }
        @Override public String toString(){return "BattleActionTarget[redacted]";}
    }
    record Request(ModernBattleCommandLife.Proof proof,Target target,Message command) {
        Request {Objects.requireNonNull(proof);Objects.requireNonNull(target);Objects.requireNonNull(command);bind(proof,target,command);}
        @Override public String toString(){return "BattleActionRequest[redacted]";}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&ModernSessionProtocol.CAP_ACTION_LIFE)!=0;
    }
    static boolean mutation(Message m) {
        if(m instanceof WorldRequest r)return r.request().action()==ModernWorldProtocol.Action.MOVE;
        if(m instanceof CombatRequest r)return r.request().action()==ModernCombatProtocol.Action.ATTACK;
        if(m instanceof ItemRequest r)return r.request().action()==ModernItemProtocol.Action.USE;
        if(m instanceof AmmoRequest r)return r.request().action()==ModernAmmoProtocol.Action.SELECT;
        if(m instanceof NpcRequest r)return r.request().action()!=ModernNpcProtocol.Action.READ&&r.request().action()!=ModernNpcProtocol.Action.CONFIRM_DEATH;
        return m instanceof CastRequest||m instanceof SiegeRequest||m instanceof DisintegrateRequest
                ||m instanceof NpcSpellRequest||m instanceof NpcDisintegrateRequest||m instanceof NpcFireStormRequest;
    }
    private static void bind(ModernBattleCommandLife.Proof p,Target t,Message m) {
        if(!mutation(m)||m.requestId()==0)throw bad();
        ModernRooms.Scope s;long match,generation,room=0;
        if(m instanceof WorldRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();none(t);
        } else if(m instanceof CombatRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();actorOrHonor(t,q.targetEntityId());
        } else if(m instanceof CastRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();
            if(q.skillId()==5||q.skillId()==70){if(q.targetEntityId()!=0)throw bad();none(t);}
            else if(q.skillId()==4)actorOrHonor(t,q.targetEntityId());
            else if(q.skillId()==1){if(t.kind()!=Kind.HONOR||t.id()!=q.targetEntityId())throw bad();}
            else throw bad();
        } else if(m instanceof ItemRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();none(t);
        } else if(m instanceof AmmoRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();none(t);
        } else if(m instanceof NpcRequest r) {
            var q=r.request();s=q.player();match=q.world().matchId();generation=q.world().generation();room=q.world().roomId();
            switch(q.action()) {
                case ATTACK -> exact(t,Kind.NPC,q.serial(),q.life());
                case ATTACK_OBJECTIVE -> {if(t.kind()!=Kind.QUEST||t.id()!=q.serial())throw bad();}
                case ATTACK_TOWER -> exact(t,Kind.TIME_TOWER,0,0);
                default -> throw bad();
            }
        } else if(m instanceof SiegeRequest r) {
            var q=r.request();s=q.player();match=q.world().matchId();generation=q.world().generation();room=q.world().roomId();
            if(q.action()==ModernSiegeProtocol.Action.PORTAL)none(t);else exact(t,Kind.SIEGE,q.tower().ordinal(),0);
        } else if(m instanceof DisintegrateRequest r) {
            var q=r.request();s=q.scope();match=q.matchId();generation=q.generation();
            if(q.casterLife()!=p.life())throw bad();exact(t,Kind.PLAYER,q.targetEntityId(),q.targetLife());
        } else if(m instanceof NpcSpellRequest r) {
            var q=r.request();s=q.player();match=q.world().matchId();generation=q.world().generation();room=q.world().roomId();
            if(q.casterLife()!=p.life())throw bad();exact(t,Kind.NPC,q.target().serial(),q.targetLife());
        } else if(m instanceof NpcDisintegrateRequest r) {
            var q=r.request();s=q.player();match=q.target().scope().matchId();generation=q.target().scope().generation();room=q.target().scope().roomId();
            if(q.casterLife()!=p.life())throw bad();exact(t,Kind.NPC,q.target().serial(),q.targetLife());
        } else if(m instanceof NpcFireStormRequest r) {
            var q=r.request();s=q.player();match=q.world().matchId();generation=q.world().generation();room=q.world().roomId();
            if(q.casterLife()!=p.life())throw bad();none(t);
        } else throw bad();
        if(!s.equals(p.player())||match!=p.world().matchId()||generation!=p.world().generation()||room!=0&&room!=p.world().roomId())throw bad();
    }
    private static void none(Target t){exact(t,Kind.NONE,0,0);}
    private static void exact(Target t,Kind k,long id,long life){if(t.kind()!=k||t.id()!=id||t.life()!=life)throw bad();}
    private static void actorOrHonor(Target t,long id){if((t.kind()!=Kind.PLAYER&&t.kind()!=Kind.HONOR)||t.id()!=id)throw bad();}
    static int requestKind(Message m) {
        if(m instanceof WorldRequest)return 20;if(m instanceof CombatRequest)return 22;if(m instanceof CastRequest)return 26;
        if(m instanceof ItemRequest)return 34;if(m instanceof AmmoRequest)return 36;if(m instanceof NpcRequest)return 38;
        if(m instanceof NpcSpellRequest)return 40;if(m instanceof SiegeRequest)return 42;if(m instanceof DisintegrateRequest)return 44;
        if(m instanceof NpcDisintegrateRequest)return 46;if(m instanceof NpcFireStormRequest)return 48;throw bad();
    }
    private static boolean requestKind(int k){return k==20||k==22||k==26||k==34||k==36||k==38||k==40||k==42||k==44||k==46||k==48;}
    private static int resultKind(Message m) {
        if(m instanceof WorldResult)return 21;if(m instanceof CombatResult)return 23;if(m instanceof CastResult)return 27;
        if(m instanceof ItemResult)return 35;if(m instanceof AmmoResult)return 37;if(m instanceof NpcResult)return 39;
        if(m instanceof NpcSpellResult)return 41;if(m instanceof SiegeResult)return 43;if(m instanceof DisintegrateResult)return 45;
        if(m instanceof NpcDisintegrateResult)return 47;if(m instanceof NpcFireStormResult)return 49;throw bad();
    }
    static void validateResult(BattleActionResult r) {
        var echo=r.echo();var inner=r.result();var p=echo.proof();
        if(r.requestId()!=echo.command().requestId()||inner.requestId()!=r.requestId())throw bad();
        if(r.rejection()==ModernBattleCommandLife.Rejection.NONE) {
            if(resultKind(inner)!=requestKind(echo.command())+1||!echo.command().equals(echoCommand(inner)))throw bad();
        } else {
            ModernRooms.Scope s;long match,generation;long room=0;ModernWorldProtocol.Frame world;
            if(inner instanceof WorldResult v) {
                if(v.echo().action()!=ModernWorldProtocol.Action.READ||v.reply().status()!=ModernWorldProtocol.Status.OK||v.reply().reason()!=ModernWorldProtocol.Reason.NONE||v.reply().frame()==null)throw bad();
                s=v.echo().scope();match=v.echo().matchId();generation=v.echo().generation();
                world=v.reply().frame();
            } else if(inner instanceof CombatResult v) {
                if(v.echo().action()!=ModernCombatProtocol.Action.READ||v.reply().status()!=ModernCombatProtocol.Status.OK||v.reply().reason()!=ModernCombatProtocol.Reason.NONE||v.reply().frame()==null)throw bad();
                s=v.echo().scope();match=v.echo().matchId();generation=v.echo().generation();
                world=v.reply().frame().world();
            } else if(inner instanceof NpcResult v) {
                if(v.echo().action()!=ModernNpcProtocol.Action.READ||v.reply().status()!=ModernNpcProtocol.Status.OK||v.reply().reason()!=ModernNpcProtocol.Reason.NONE||v.reply().frame()==null)throw bad();
                s=v.echo().player();match=v.echo().world().matchId();generation=v.echo().world().generation();room=v.echo().world().roomId();
                world=v.reply().frame().combat().world();
            } else throw bad();
            if(!s.equals(p.player())||match!=p.world().matchId()||generation!=p.world().generation()||room!=0&&room!=p.world().roomId())throw bad();
            if(world.matchId()!=match||world.generation()!=generation||world.room().summary().id()!=p.world().roomId()
                    ||world.revision()<=p.revision()||world.actors().stream().noneMatch(a->a.entityId()==p.entity()
                        &&a.member().identity().equals(ModernParticipantId.human(s.sourceId(),s.objectId()))))throw bad();
        }
    }
    private static Message echoCommand(Message m) {
        if(m instanceof WorldResult r)return new WorldRequest(r.requestId(),r.echo());
        if(m instanceof CombatResult r)return new CombatRequest(r.requestId(),r.echo());
        if(m instanceof CastResult r)return new CastRequest(r.requestId(),r.echo());
        if(m instanceof ItemResult r)return new ItemRequest(r.requestId(),r.echo());
        if(m instanceof AmmoResult r)return new AmmoRequest(r.requestId(),r.echo());
        if(m instanceof NpcResult r)return new NpcRequest(r.requestId(),r.echo());
        if(m instanceof SiegeResult r)return new SiegeRequest(r.requestId(),r.echo());
        if(m instanceof DisintegrateResult r)return new DisintegrateRequest(r.requestId(),r.echo());
        if(m instanceof NpcSpellResult r)return new NpcSpellRequest(r.requestId(),r.echo());
        if(m instanceof NpcDisintegrateResult r)return new NpcDisintegrateRequest(r.requestId(),r.echo());
        if(m instanceof NpcFireStormResult r)return new NpcFireStormRequest(r.requestId(),r.echo());
        throw bad();
    }
    static void putPrefix(ByteBuffer b,Request r) {
        var p=r.proof();var s=p.player();var w=p.world();var t=r.target();
        b.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).putInt(s.channelId());
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation()).putLong(p.entity()).putLong(p.life()).putLong(p.revision());
        b.put((byte)t.kind().ordinal());for(int i=0;i<7;i++)b.put((byte)0);b.putLong(t.id()).putLong(t.life());
    }
    static Request getRequest(ByteBuffer b,int requestId,int version)throws ProtocolException {
        if(version<VERSION)throw new ProtocolException();
        var s=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),b.getInt());
        var w=new ModernTargetHistory.World(b.getLong(),b.getLong(),b.getLong());
        var p=new ModernBattleCommandLife.Proof(s,w,b.getLong(),b.getLong(),b.getLong());
        int k=Byte.toUnsignedInt(b.get());if(k>=Kind.values().length)throw bad();for(int i=0;i<7;i++)if(b.get()!=0)throw bad();
        var t=new Target(Kind.values()[k],b.getLong(),b.getLong());int length=b.getInt();
        if(length<72||length>120||length>b.remaining())throw bad();
        // Check inner kind BEFORE recursion or allocation; nested frames and
        // credentials never enter the inner decoder.
        int pos=b.position();if(!requestKind(Short.toUnsignedInt(b.getShort(pos+12))))throw bad();
        byte[] inner=new byte[length];b.get(inner);var m=ModernApplicationProtocol.decode(inner,version);
        if(m.requestId()!=requestId)throw bad();return new Request(p,t,m);
    }
    static byte[] encodeRequest(BattleActionRequest r,int version)throws ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||r.requestId()==0||r.requestId()!=r.request().command().requestId())throw new ProtocolException();
        var inner=ModernApplicationProtocol.encode(r.request().command(),version);
        var b=header(16+PREFIX_BYTES+inner.length,r.requestId(),REQUEST_KIND);putPrefix(b,r.request());b.putInt(inner.length).put(inner);return b.array();
    }
    static byte[] encodeResult(BattleActionResult r,int version)throws ProtocolException {
        if(version<VERSION||version>ModernSessionProtocol.MAX_VERSION||r.requestId()==0)throw new ProtocolException();validateResult(r);
        var request=ModernApplicationProtocol.encode(r.echo().command(),version);var result=ModernApplicationProtocol.encode(r.result(),version);
        int length=Math.addExact(16+PREFIX_BYTES+request.length+8,result.length);
        if(length>MAX_RESULT_BYTES)throw new ProtocolException();
        var b=header(length,r.requestId(),RESULT_KIND);
        putPrefix(b,r.echo());b.putInt(request.length).put(request).put((byte)r.rejection().ordinal()).put((byte)0).putShort((short)0).putInt(result.length).put(result);
        return b.array();
    }
    private static ByteBuffer header(int length,int id,int kind) {
        var b=ByteBuffer.allocate(length);b.putInt(0x4c544d31).putInt(length).putInt(id).putShort((short)kind).putShort((short)0);return b;
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid battle action envelope");}
    private ModernBattleActionProtocol(){}
}
