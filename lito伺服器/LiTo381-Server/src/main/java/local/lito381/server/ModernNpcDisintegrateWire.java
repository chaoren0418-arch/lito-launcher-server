package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** v29 typed NPC77 sections and atomic-frame validation. Application dispatch
 * lives in ModernNpcDisintegrateProtocol; player renderer admission remains
 * separate from server/transport capability negotiation.
 * Big-endian; the enclosing atomic NPC frame supplies the immutable scope.
 */
final class ModernNpcDisintegrateWire {
    static final int VERSION=29, REQUEST_BYTES=80, STATE_BYTES=144, MAX_ACTORS=100;

    static void putRequest(ByteBuffer out,ModernNpcDisintegrateState.Request r,int version) {
        require(out,version);Objects.requireNonNull(r);var p=r.player();var w=r.target().scope();
        out.putInt(p.sourceId()).putLong(p.objectId()).putLong(p.epoch()).put((byte)p.channelId());zeroes(out,3);
        out.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation())
                .putLong(r.sequence()).putLong(r.casterLife()).putLong(r.target().serial()).putLong(r.targetLife());
    }
    static ModernNpcDisintegrateState.Request getRequest(ByteBuffer in,int version) {
        require(in,version);
        int source=in.getInt();long obj=in.getLong(),epoch=in.getLong();int channel=Byte.toUnsignedInt(in.get());zero(in,3);
        var player=new ModernRooms.Scope(source,obj,epoch,channel);
        var scope=new ModernNpcRoster.Scope(in.getLong(),in.getLong(),in.getLong());
        long sequence=in.getLong(),casterLife=in.getLong(),serial=in.getLong(),targetLife=in.getLong();
        return new ModernNpcDisintegrateState.Request(player,new ModernNpcRoster.Id(scope,serial),sequence,casterLife,targetLife);
    }
    static void putStates(ByteBuffer out,ModernNpcRoster.Scope scope,List<ModernNpcDisintegrateState.State> states,int actors,int version) {
        require(out,version);Objects.requireNonNull(scope);states=List.copyOf(states);count(states.size(),actors);
        // Validate the entire section before writing, including cross-scope receipts.
        long previous=0;
        for(var s:states){ordered(s.entityId(),previous);previous=s.entityId();if(s.hit()!=null&&!s.hit().target().scope().equals(scope))throw bad();}
        if(out.remaining()<8+states.size()*STATE_BYTES)throw bad();
        out.putShort((short)states.size());zeroes(out,6);
        for(var s:states) {
            var h=s.hit();var o=h==null?null:h.outcome();
            out.putLong(s.entityId()).putLong(s.castCount()).putLong(s.revision()).putLong(s.lastSequence()).putLong(s.mpSpent())
                    .putLong(s.initialLawful()).putLong(s.lawful()).putInt(s.mpCost()).putInt(s.reuseMillis());
            out.put((byte)(h==null?0:1)).put((byte)(h==null?255:h.lane())).put((byte)(h==null?0:(o.death()?1:0)|(o.critical()?2:0)));zeroes(out,5);
            out.putLong(h==null?0:h.casterLife()).putLong(h==null?0:h.target().serial()).putLong(h==null?0:h.targetLife())
                    .putInt(h==null?0:h.from().x()).putInt(h==null?0:h.from().y()).putInt(h==null?0:h.to().x()).putInt(h==null?0:h.to().y())
                    .putLong(h==null?0:h.hpBefore()).putLong(h==null?0:o.rawDamage()).putLong(h==null?0:o.appliedDamage()).putLong(h==null?0:o.remainingHp());
        }
    }
    static List<ModernNpcDisintegrateState.State> getStates(ByteBuffer in,ModernNpcRoster.Scope scope,int actors,int version) {
        require(in,version);Objects.requireNonNull(scope);int count=Short.toUnsignedInt(in.getShort());zero(in,6);count(count,actors);
        if(in.remaining()<count*STATE_BYTES)throw bad();var result=new ArrayList<ModernNpcDisintegrateState.State>(count);long previous=0;
        for(int i=0;i<count;i++) {
            long entity=in.getLong(),casts=in.getLong(),revision=in.getLong(),sequence=in.getLong(),spent=in.getLong(),initialLawful=in.getLong(),lawful=in.getLong();
            int cost=in.getInt(),reuse=in.getInt(),present=Byte.toUnsignedInt(in.get()),lane=Byte.toUnsignedInt(in.get()),flags=Byte.toUnsignedInt(in.get());zero(in,5);
            long casterLife=in.getLong(),serial=in.getLong(),targetLife=in.getLong();
            int x=in.getInt(),y=in.getInt(),tx=in.getInt(),ty=in.getInt();long before=in.getLong(),raw=in.getLong(),applied=in.getLong(),hp=in.getLong();
            if(present>1||flags>3||present==0&&(lane!=255||flags!=0||casterLife!=0||serial!=0||targetLife!=0||x!=0||y!=0||tx!=0||ty!=0||before!=0||raw!=0||applied!=0||hp!=0))throw bad();
            var hit=present==0?null:new ModernNpcDisintegrateState.Hit(new ModernNpcRoster.Id(scope,serial),targetLife,lane,casterLife,
                    new ModernWorldMap.Cell(x,y),new ModernWorldMap.Cell(tx,ty),before,new ModernDisintegrate.Outcome(raw,applied,hp,(flags&1)!=0,(flags&2)!=0));
            var state=new ModernNpcDisintegrateState.State(entity,casts,revision,sequence,spent,cost,initialLawful,lawful,hit);
            ordered(entity,previous);previous=entity;if(reuse!=state.reuseMillis())throw bad();result.add(state);
        }
        return List.copyOf(result);
    }
    static void validateStates(ModernWorldProtocol.Frame w,List<ModernCombatProtocol.Vital> vitals,List<ModernItemProtocol.State> items,
            List<ModernRegenProtocol.State> regen,List<ModernNpcMagic.CastState> arrows,List<ModernNpcDisintegrateState.State> states) {
        if(states.isEmpty())return;int n=w.actors().size();
        if(w.descriptor()==null||!w.descriptor().has(ModernMatchDescriptor.NPC_DISINTEGRATE|ModernMatchDescriptor.NPC|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.REGEN)
                ||n<1||n>100||states.size()!=n||vitals.size()!=n||w.casts().size()!=n||regen.size()!=n
                ||!items.isEmpty()&&items.size()!=n||!arrows.isEmpty()&&arrows.size()!=n||w.room().summary().config().mode()!=(w.timeAttack()==null?2:4))throw bad();
        var revisions=new HashSet<Long>();var occupied=new HashSet<Long>();
        for(var c:w.casts())occupied.add(c.castRevision());for(var a:arrows)occupied.add(a.revision());for(var i:items)occupied.add(i.useRevision());
        for(var v:vitals){occupied.add(v.attackRevision());occupied.add(v.hurtRevision());occupied.add(v.deathRevision());}
        for(var r:regen){occupied.add(r.hpRevision());occupied.add(r.mpRevision());}
        for(var e:w.quest().exchanges())occupied.add(e.revision());
        for(var l:w.quest().lives()){occupied.add(l.respawnRevision());occupied.add(l.deathRevision());}
        for(var a:w.quest().actors()){occupied.add(a.hurtRevision());occupied.add(a.deathRevision());}
        if(w.timeAttack()!=null)for(var e:w.timeAttack().exchanges())occupied.add(Math.incrementExact(e.revision()));
        long end=eventRevision(w);
        for(int i=0;i<n;i++) {
            var s=states.get(i);var a=w.actors().get(i);var v=vitals.get(i);var c=w.casts().get(i);
            if(s.entityId()!=a.entityId()||s.revision()>end||s.lastSequence()>c.requestSequence()
                    ||Math.addExact(Math.addExact(c.castCount(),arrows.isEmpty()?0:arrows.get(i).castCount()),s.castCount())>c.requestSequence()
                    ||(a.member().identity().isHuman()?s.mpCost()==0:!s.equals(ModernNpcDisintegrateState.State.initial(s.entityId(),0,0)))
                    ||(w.phase()==ModernWorldProtocol.Phase.LOADING||w.phase()==ModernWorldProtocol.Phase.COUNTDOWN)&&s.castCount()!=0)throw bad();
            if(s.hit()==null)continue;var h=s.hit();var scope=h.target().scope();var life=w.quest().life(s.entityId());long currentLife=life==null?1:life.incarnation();
            if(!revisions.add(s.revision())||occupied.contains(s.revision())||scope.roomId()!=w.room().summary().id()||scope.matchId()!=w.matchId()||scope.generation()!=w.generation()
                    ||h.lane()!=a.member().team()||!ModernWorldProtocol.contains(w.manifest(),h.from())||!ModernWorldProtocol.contains(w.manifest(),h.to())
                    ||h.casterLife()>currentLife||life!=null&&(h.casterLife()==currentLife?s.revision()<=life.respawnRevision():s.revision()>life.respawnRevision())
                    ||h.casterLife()==currentLife&&v.hp()==0&&s.revision()>=v.deathRevision()
                    ||s.revision()==end&&(v.hp()==0||c.requestSequence()!=s.lastSequence()||h.casterLife()!=currentLife||!a.position().equals(h.from())
                        ||w.timeAttack()!=null&&w.timeAttack().departed(s.entityId())))throw bad();
        }
    }
    static void validateNpcs(ModernCombatProtocol.Frame combat,ModernNpcRoster.Scope scope,long spawned,List<ModernNpcProtocol.Actor> actors,List<ModernNpcBattle.Exchange> exchanges) {
        long end=eventRevision(combat.world());
        for(var s:combat.npcDisintegrations())if(s.hit()!=null) {
            var h=s.hit();if(!h.target().scope().equals(scope)||h.target().serial()>spawned||exchanges.stream().anyMatch(e->e.revision()==s.revision()))throw bad();
            var target=actors.stream().filter(n->n.serial()==h.target().serial()).findFirst().orElse(null);
            if(target!=null&&(target.lane()!=h.lane()||target.life()<h.targetLife()||h.hpBefore()>target.maxHp()
                    ||h.outcome().death()&&h.targetLife()==target.life()&&target.hp()>0))throw bad();
            if(s.revision()==end&&(target==null||target.life()!=h.targetLife()||!target.cell().equals(h.to())||target.hp()!=h.outcome().remainingHp()
                    ||combat.vital(s.entityId()).hp()==0||h.outcome().appliedDamage()>0&&target.action()!=(h.outcome().death()?ModernNpcRoster.Action.DEATH:ModernNpcRoster.Action.HURT)))throw bad();
        }
    }
    private static long eventRevision(ModernWorldProtocol.Frame w) {
        return w.timeAttack()!=null&&w.timeAttack().receipt()!=null?Math.incrementExact(w.timeAttack().receipt().revision()):
                w.completion()!=null?Math.incrementExact(w.completion().event().engineRevision()):w.revision();
    }
    /** Monotonic receipt ledger for two already-validated atomic views of the
     * SAME scoped roster. Full frame validation still owns geometry, vitals,
     * sequence budgets, NPC life, other event revisions and terminal progress.
     */
    static boolean follows(ModernNpcRoster.Scope beforeScope,List<ModernNpcDisintegrateState.State> before,long beforeRevision,
                           ModernNpcRoster.Scope afterScope,List<ModernNpcDisintegrateState.State> after,long afterRevision) {
        if(beforeScope==null||!beforeScope.equals(afterScope)||beforeRevision<1||afterRevision<beforeRevision||before.size()!=after.size())return false;
        try {
            count(before.size(),before.isEmpty()?1:before.size());long previous=0;
            for(int i=0;i<before.size();i++) {
                var a=before.get(i);var b=after.get(i);ordered(a.entityId(),previous);previous=a.entityId();
                if(a.entityId()!=b.entityId()||a.mpCost()!=b.mpCost()||a.initialLawful()!=b.initialLawful()
                        ||a.revision()>beforeRevision||b.revision()>afterRevision||b.castCount()<a.castCount()
                        ||a.hit()!=null&&!a.hit().target().scope().equals(beforeScope)||b.hit()!=null&&!b.hit().target().scope().equals(afterScope))return false;
                if(a.castCount()==b.castCount()){if(!a.equals(b))return false;continue;}
                if(b.revision()<=beforeRevision||b.lastSequence()<=a.lastSequence()
                        ||b.castCount()-a.castCount()>b.lastSequence()-a.lastSequence()
                        ||b.castCount()-a.castCount()>afterRevision-beforeRevision
                        ||a.hit()!=null&&(b.hit().casterLife()<a.hit().casterLife()
                            ||a.hit().target().equals(b.hit().target())&&b.hit().targetLife()<a.hit().targetLife()))return false;
            }
            return true;
        }catch(IllegalArgumentException|ArithmeticException ex){return false;}
    }
    private static void require(ByteBuffer b,int version){if(version<VERSION||version>ModernWorldProtocol.CODEC_MAX_VERSION||b.order()!=ByteOrder.BIG_ENDIAN)throw bad();}
    private static void count(int count,int actors){if(actors<1||actors>MAX_ACTORS||count<0||count>MAX_ACTORS||count!=0&&count!=actors)throw bad();}
    private static void ordered(long entity,long previous){if(entity<=previous||entity>MAX_ACTORS)throw bad();}
    private static void zeroes(ByteBuffer b,int count){while(count-->0)b.put((byte)0);}
    private static void zero(ByteBuffer b,int count){while(count-->0)if(b.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC77 wire section");}
    private ModernNpcDisintegrateWire(){}
}
