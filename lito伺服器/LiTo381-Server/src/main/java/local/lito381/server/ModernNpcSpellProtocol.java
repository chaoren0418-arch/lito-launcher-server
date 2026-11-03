package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** v24 skill4 against a typed NPC. Neither target nor caster life is inferred
 * from the other. All HP/MP/cooldown values remain server-authoritative. */
final class ModernNpcSpellProtocol {
    static final int VERSION=24,REQUEST_KIND=40,RESULT_KIND=41,REQUEST_BYTES=80,STATE_BYTES=120;
    record Request(ModernRooms.Scope player,ModernNpcRoster.Scope world,long sequence,long casterLife,long serial,long targetLife) {
        Request {Objects.requireNonNull(player);Objects.requireNonNull(world);
            if(sequence<1||casterLife<1||serial<1||targetLife<1)throw bad();}
        ModernNpcRoster.Id target(){return new ModernNpcRoster.Id(world,serial);}
    }
    record Reply(ModernNpcProtocol.Status status,ModernNpcMagic.Reason reason,ModernNpcProtocol.Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==ModernNpcProtocol.Status.OK)!=(reason==ModernNpcMagic.Reason.NONE)
                    ||frame==null&&!(status==ModernNpcProtocol.Status.UNAVAILABLE&&reason==ModernNpcMagic.Reason.NOT_ACTIVE))throw bad();}
    }
    static void validate(ModernApplicationProtocol.NpcSpellResult message) {
        var r=Objects.requireNonNull(message.echo());var reply=Objects.requireNonNull(message.reply());var frame=reply.frame();if(frame==null)return;
        var world=frame.combat().world();
        if(!frame.scope().equals(r.world())||!world.descriptor().has(ModernMatchDescriptor.NPC_LIGHT_ARROW))throw bad();
        var id=ModernParticipantId.human(r.player().sourceId(),r.player().objectId());
        var actor=world.actors().stream().filter(a->a.member().identity().equals(id)).findFirst().orElseThrow(ModernNpcSpellProtocol::bad);
        var vital=frame.combat().vital(actor.entityId());
        var cast=frame.combat().npcSpells().stream().filter(s->s.entityId()==actor.entityId()).findFirst().orElseThrow(ModernNpcSpellProtocol::bad);
        var clock=world.casts().stream().filter(s->s.entityId()==actor.entityId()).findFirst().orElseThrow(ModernNpcSpellProtocol::bad);
        var life=world.quest().life(actor.entityId());long currentLife=life==null?1:life.incarnation();
        if(reply.status()==ModernNpcProtocol.Status.OK) {
            if(!(world.phase()==ModernWorldProtocol.Phase.ACTIVE||world.phase()==ModernWorldProtocol.Phase.FINISHED&&world.timeAttack()!=null)||vital.hp()==0||currentLife!=r.casterLife()
                    ||clock.requestSequence()!=r.sequence()||cast.requestSequence()!=r.sequence()||cast.revision()!=eventRevision(world)
                    ||cast.casterLife()!=r.casterLife()||!r.target().equals(cast.target())||cast.targetLife()!=r.targetLife())throw bad();
        } else if(reply.status()==ModernNpcProtocol.Status.DENIED&&reply.reason()!=ModernNpcMagic.Reason.NOT_ACTIVE) {
            if(world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                    ||(reply.reason()==ModernNpcMagic.Reason.STALE_STATE?clock.requestSequence()<r.sequence():clock.requestSequence()!=r.sequence()))throw bad();
            if(reply.reason()==ModernNpcMagic.Reason.DEAD&&vital.hp()!=0
                    ||reply.reason()==ModernNpcMagic.Reason.NO_MP&&vital.mp()>=cast.mpCost())throw bad();
        }
    }
    static void validateStates(ModernWorldProtocol.Frame w,List<ModernCombatProtocol.Vital> vitals,List<ModernNpcMagic.CastState> states) {
        boolean enabled=w.descriptor()!=null&&w.descriptor().has(ModernMatchDescriptor.NPC_LIGHT_ARROW);
        if(enabled!=!states.isEmpty())throw bad();if(!enabled)return;
        if(states.size()!=w.actors().size()||w.casts().size()!=states.size()||vitals.size()!=states.size())throw bad();
        long end=eventRevision(w);
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);var actor=w.actors().get(i);var clock=w.casts().get(i);var v=vitals.get(i);
            if(s.entityId()!=actor.entityId()||s.revision()>end||s.requestSequence()>clock.requestSequence()
                    ||Math.addExact(s.castCount(),clock.castCount())>clock.requestSequence()
                    ||(actor.member().identity().isHuman()?s.mpCost()<1:!s.equals(ModernNpcMagic.CastState.initial(s.entityId(),0))))throw bad();
            if(s.castCount()==0)continue;
            var scope=s.target().scope();var life=w.quest().life(s.entityId());
            if(scope.roomId()!=w.room().summary().id()||scope.matchId()!=w.matchId()||scope.generation()!=w.generation()
                    ||s.targetLane()!=actor.member().team()||!ModernWorldProtocol.contains(w.manifest(),s.from())||!ModernWorldProtocol.contains(w.manifest(),s.to())
                    ||s.outcome().death()!=(s.outcome().remainingHp()==0)
                    ||w.phase()==ModernWorldProtocol.Phase.LOADING||w.phase()==ModernWorldProtocol.Phase.COUNTDOWN
                    ||(life==null?s.casterLife()!=1:s.casterLife()>life.incarnation()
                    ||(s.casterLife()==life.incarnation()?s.revision()<=life.respawnRevision():s.revision()>life.respawnRevision()))
                    ||s.revision()==end&&(v.hp()==0||!actor.position().equals(s.from())||clock.requestSequence()!=s.requestSequence()
                        ||w.timeAttack()!=null&&w.timeAttack().departed(s.entityId())))throw bad();
        }
    }
    static void validateNpcs(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long spawned,List<ModernNpcProtocol.Actor> actors) {
        long end=eventRevision(combat.world());
        for(var s:combat.npcSpells())if(s.castCount()>0) {
            if(!s.target().scope().equals(scope)||s.target().serial()>spawned)throw bad();
            var target=actors.stream().filter(n->n.serial()==s.target().serial()).findFirst().orElse(null);
            if(target!=null&&(target.lane()!=s.targetLane()||target.life()<s.targetLife()
                    ||s.outcome().remainingHp()>target.maxHp()||s.outcome().appliedDamage()>target.maxHp()-s.outcome().remainingHp()))throw bad();
            if(s.revision()==end&&(target==null||target.life()!=s.targetLife()||target.hp()!=s.outcome().remainingHp()))throw bad();
        }
    }
    private static long eventRevision(ModernWorldProtocol.Frame w) {
        return w.timeAttack()!=null&&w.timeAttack().receipt()!=null?Math.incrementExact(w.timeAttack().receipt().revision()):w.revision();
    }
    static void putStates(ByteBuffer b,List<ModernNpcMagic.CastState> states) {
        if(states.size()>100)throw bad();b.putShort((short)states.size());zeroes(b,6);
        for(var s:states) {
            boolean present=s.castCount()>0;var o=s.outcome();
            b.putLong(s.entityId()).putLong(s.requestSequence()).putLong(s.castCount()).putLong(s.revision()).putLong(s.mpSpent()).putLong(s.mpCost())
                    .putLong(s.casterLife()).putLong(present?s.target().serial():0).putLong(s.targetLife());
            b.put((byte)s.targetLane()).put((byte)(present?1|(o.death()?2:0)|(o.critical()?4:0):0));zeroes(b,6);
            b.putInt(present?s.from().x():0).putInt(present?s.from().y():0).putInt(present?s.to().x():0).putInt(present?s.to().y():0)
                    .putLong(present?o.rawDamage():0).putLong(present?o.appliedDamage():0).putLong(present?o.remainingHp():0);
        }
    }
    static List<ModernNpcMagic.CastState> getStates(ByteBuffer b,ModernWorldProtocol.Frame world) {
        int count=Short.toUnsignedInt(b.getShort());zero(b,6);if(count>100||count!=0&&count!=world.actors().size()||b.remaining()<count*STATE_BYTES)throw bad();
        var states=new ArrayList<ModernNpcMagic.CastState>(count);var scope=new ModernNpcRoster.Scope(world.room().summary().id(),world.matchId(),world.generation());
        for(int i=0;i<count;i++) {
            long entity=b.getLong(),seq=b.getLong(),casts=b.getLong(),rev=b.getLong(),spent=b.getLong(),cost=b.getLong(),casterLife=b.getLong(),serial=b.getLong(),life=b.getLong();
            int lane=Byte.toUnsignedInt(b.get()),flags=Byte.toUnsignedInt(b.get());zero(b,6);
            int x=b.getInt(),y=b.getInt(),tx=b.getInt(),ty=b.getInt();long raw=b.getLong(),applied=b.getLong(),hp=b.getLong();
            if(flags>7||(flags&1)==0&&(flags!=0||serial!=0||x!=0||y!=0||tx!=0||ty!=0||raw!=0||applied!=0||hp!=0))throw bad();
            boolean present=(flags&1)!=0;if(present!=(casts>0))throw bad();
            states.add(new ModernNpcMagic.CastState(entity,seq,casts,rev,spent,cost,casterLife,present?new ModernNpcRoster.Id(scope,serial):null,life,lane==255?-1:lane,
                    present?new ModernWorldMap.Cell(x,y):null,present?new ModernWorldMap.Cell(tx,ty):null,
                    present?new ModernMagicCombat.Outcome(raw,applied,hp,(flags&2)!=0,(flags&4)!=0):null));
        }
        return List.copyOf(states);
    }
    static void putRequest(ByteBuffer b,Request r) {
        var p=r.player();var w=r.world();b.putInt(p.sourceId()).putLong(p.objectId()).putLong(p.epoch()).put((byte)p.channelId());zeroes(b,3);
        b.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation()).putLong(r.sequence()).putLong(r.casterLife()).putLong(r.serial()).putLong(r.targetLife());
    }
    static Request getRequest(ByteBuffer b) {
        int source=b.getInt();long obj=b.getLong(),epoch=b.getLong();int channel=Byte.toUnsignedInt(b.get());zero(b,3);
        return new Request(new ModernRooms.Scope(source,obj,epoch,channel),new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong()),b.getLong(),b.getLong(),b.getLong(),b.getLong());
    }
    static void putResult(ByteBuffer b,ModernApplicationProtocol.NpcSpellResult m,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();validate(m);putRequest(b,m.echo());var r=m.reply();
        b.put((byte)r.status().ordinal()).put((byte)r.reason().ordinal()).put((byte)(r.frame()==null?0:1)).put((byte)0);
        if(r.frame()!=null)ModernNpcProtocol.putFrame(b,r.frame(),version);
    }
    static ModernApplicationProtocol.NpcSpellResult getResult(ByteBuffer b,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();var request=getRequest(b);int status=Byte.toUnsignedInt(b.get()),reason=Byte.toUnsignedInt(b.get()),present=Byte.toUnsignedInt(b.get());zero(b,1);
        if(status>=ModernNpcProtocol.Status.values().length||reason>=ModernNpcMagic.Reason.values().length||present>1)throw bad();
        return new ModernApplicationProtocol.NpcSpellResult(id,request,new Reply(ModernNpcProtocol.Status.values()[status],ModernNpcMagic.Reason.values()[reason],present==1?ModernNpcProtocol.getFrame(b,version):null));
    }
    private static void zeroes(ByteBuffer b,int n){while(n-->0)b.put((byte)0);}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC light-arrow wire contract");}
    private ModernNpcSpellProtocol(){}
}
