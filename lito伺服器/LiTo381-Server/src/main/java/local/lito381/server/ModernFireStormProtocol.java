package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** v14 append-only, bounded last-AoE receipts. Handles resolve through this frozen world only. */
final class ModernFireStormProtocol {
    static final int STATE_BYTES=64,TARGET_BYTES=56,MAX_TARGETS=101;
    record Target(long entityId,long hpBefore,long hpAfter,long appliedDamage,long damageLossAfter,
                  long respawnCount,int x,int y) {
        Target {
            if(entityId<1||hpBefore<1||hpAfter<0||hpAfter>hpBefore||appliedDamage!=hpBefore-hpAfter
                    ||damageLossAfter<appliedDamage||respawnCount<0)throw bad();
        }
    }
    record State(long entityId,long castCount,long castRevision,long mpSpent,long lastCastSequence,
                 int fromX,int fromY,int mpCost,int reuseMillis,List<Target> targets) {
        State {
            targets=List.copyOf(targets);
            if(entityId<1||castCount<0||castRevision<0||mpSpent<0||lastCastSequence<castCount
                    ||targets.size()>MAX_TARGETS||mpCost!=0&&(mpCost<39||mpCost>48)
                    ||reuseMillis!=(mpCost==0?0:ModernFireStorm.REUSE_MILLIS)
                    ||Math.multiplyExact(castCount,mpCost)!=mpSpent)throw bad();
            if(castCount==0){if(castRevision!=0||lastCastSequence!=0||fromX!=0||fromY!=0||!targets.isEmpty())throw bad();}
            else if(castRevision==0||mpCost==0)throw bad();
            long previous=0;for(var target:targets){if(target.entityId()<=previous||target.entityId()==entityId
                    ||Math.max(Math.abs((long)fromX-target.x()),Math.abs((long)fromY-target.y()))>ModernFireStorm.RADIUS)throw bad();previous=target.entityId();}
        }
        static State initial(long entityId,int cost){return new State(entityId,0,0,0,0,0,0,cost,cost==0?0:ModernFireStorm.REUSE_MILLIS,List.of());}
        State wireRevision(){return new State(entityId,castCount,castRevision==0?0:Math.incrementExact(castRevision),mpSpent,lastCastSequence,fromX,fromY,mpCost,reuseMillis,targets);}
    }
    static void validate(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
            List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<State> states) {
        if(states.isEmpty())return;
        if(states.size()!=world.actors().size()||vitals.size()!=states.size()||regen.size()!=states.size()
                ||world.casts().size()!=states.size())throw bad();
        var revisions=new HashSet<Long>();
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);var actor=world.actors().get(i);var cast=world.casts().get(i);
            if(s.entityId()!=actor.entityId()||s.castRevision()>world.revision()||s.lastCastSequence()>cast.requestSequence())throw bad();
            if(!actor.member().identity().isHuman()){if(!s.equals(State.initial(s.entityId(),0)))throw bad();}
            else if(s.mpCost()==0)throw bad();
            if((world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN)
                    &&!s.equals(State.initial(s.entityId(),s.mpCost())))throw bad();
            long total=Math.addExact(cast.castCount(),damages.isEmpty()?0:damages.get(i).castCount());
            total=Math.addExact(total,honor.enabled()?honor.heals().get(i).castCount():0);
            if(Math.addExact(total,s.castCount())>cast.requestSequence())throw bad();
            if(s.castCount()==0)continue;
            if(!revisions.add(s.castRevision())||s.castRevision()==cast.castRevision()
                    ||!damages.isEmpty()&&s.castRevision()==damages.get(i).castRevision()
                    ||honor.enabled()&&s.castRevision()==honor.heals().get(i).revision()
                    ||!ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(s.fromX(),s.fromY())))throw bad();
            if(s.castRevision()==vitals.get(i).attackRevision()||s.castRevision()==vitals.get(i).hurtRevision()
                    ||!items.isEmpty()&&s.castRevision()==items.get(i).useRevision())throw bad();
            if(world.completion()!=null&&s.castRevision()>Math.incrementExact(world.completion().event().engineRevision()))throw bad();
            if(s.castRevision()==world.revision()&&(actor.position().x()!=s.fromX()||actor.position().y()!=s.fromY()
                    ||vitals.get(i).hp()==0||cast.requestSequence()!=s.lastCastSequence()))throw bad();
            for(var t:s.targets()) {
                if(!ModernWorldProtocol.contains(world.manifest(),new ModernWorldMap.Cell(t.x(),t.y())))throw bad();
                long max,hp,loss,hurt,death,respawns;int team,x,y;
                if(t.entityId()<=vitals.size()) {
                    int index=Math.toIntExact(t.entityId()-1);var v=vitals.get(index);var a=world.actors().get(index);
                    max=v.maxHp();hp=v.hp();loss=regen.get(index).damageLoss();hurt=v.hurtRevision();death=v.deathRevision();
                    respawns=honor.enabled()?honor.lives().get(index).respawnCount():0;team=a.member().team();x=a.position().x();y=a.position().y();
                    // A historical receipt still has an exact HP anchor when
                    // there has been no later heal/HP tick. Its life token
                    // reconstructs prior lives without borrowing later respawns.
                    if(regen.get(index).hpRevision()<=s.castRevision()&&(items.isEmpty()||items.get(index).useRevision()<=s.castRevision())) {
                        var r=regen.get(index);var expected=java.math.BigInteger.valueOf(max)
                                .multiply(java.math.BigInteger.valueOf(t.respawnCount()).add(java.math.BigInteger.ONE))
                                .add(java.math.BigInteger.valueOf(r.hpGain())).subtract(java.math.BigInteger.valueOf(r.hpLoss()))
                                .add(java.math.BigInteger.valueOf(r.itemGain())).subtract(java.math.BigInteger.valueOf(t.damageLossAfter()));
                        if(!expected.equals(java.math.BigInteger.valueOf(t.hpAfter())))throw bad();
                    }
                }else {
                    var o=honor.objective(t.entityId());if(o==null)throw bad();max=o.maximumHp();hp=o.hp();loss=o.damageLoss();hurt=o.hurtRevision();death=o.deathRevision();
                    respawns=0;team=o.team();x=o.x();y=o.y();
                    if(o.revision()<s.castRevision())throw bad();
                    if(o.healGain()==0||honor.heals().stream().allMatch(h->h.revision()<=s.castRevision())) {
                        var expected=java.math.BigInteger.valueOf(max).add(java.math.BigInteger.valueOf(o.healGain()))
                                .subtract(java.math.BigInteger.valueOf(t.damageLossAfter()));
                        if(!expected.equals(java.math.BigInteger.valueOf(t.hpAfter())))throw bad();
                    }
                }
                if(team==actor.member().team()||t.hpBefore()>max||t.damageLossAfter()>loss||t.respawnCount()>respawns
                        ||t.appliedDamage()>0&&hurt<s.castRevision()||t.hpAfter()==0&&death<s.castRevision()
                        ||t.hpAfter()==0&&t.respawnCount()==respawns&&hp!=0)throw bad();
                if(s.castRevision()==world.revision()&&(hp!=t.hpAfter()||loss!=t.damageLossAfter()||respawns!=t.respawnCount()
                        ||x!=t.x()||y!=t.y()||t.appliedDamage()>0&&hurt!=s.castRevision()
                        ||t.appliedDamage()==0&&hurt==s.castRevision()||t.hpAfter()==0&&death!=s.castRevision()))throw bad();
            }
        }
    }
    static void put(ByteBuffer out,List<State> states){
        if(states.size()>100)throw bad();out.putShort((short)states.size());zero(out,6);
        for(var s:states){out.putLong(s.entityId()).putLong(s.castCount()).putLong(s.castRevision()).putLong(s.mpSpent()).putLong(s.lastCastSequence())
                .putInt(s.fromX()).putInt(s.fromY()).putInt(s.mpCost()).putInt(s.reuseMillis()).putShort((short)s.targets().size());zero(out,6);
            for(var t:s.targets())out.putLong(t.entityId()).putLong(t.hpBefore()).putLong(t.hpAfter()).putLong(t.appliedDamage())
                    .putLong(t.damageLossAfter()).putLong(t.respawnCount()).putInt(t.x()).putInt(t.y());}
    }
    static List<State> get(ByteBuffer in,int actorCount)throws ModernApplicationProtocol.ProtocolException {
        int count=Short.toUnsignedInt(in.getShort());zeros(in,6);
        if(count>100||count!=0&&count!=actorCount||in.remaining()<count*STATE_BYTES)throw new ModernApplicationProtocol.ProtocolException();
        var values=new ArrayList<State>(count);
        for(int i=0;i<count;i++) {
            long id=in.getLong(),casts=in.getLong(),revision=in.getLong(),spent=in.getLong(),seq=in.getLong();
            int x=in.getInt(),y=in.getInt(),cost=in.getInt(),reuse=in.getInt(),targets=Short.toUnsignedInt(in.getShort());zeros(in,6);
            if(targets>MAX_TARGETS||in.remaining()<targets*TARGET_BYTES)throw new ModernApplicationProtocol.ProtocolException();
            var receipts=new ArrayList<Target>(targets);for(int j=0;j<targets;j++)receipts.add(new Target(in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getInt(),in.getInt()));
            values.add(new State(id,casts,revision,spent,seq,x,y,cost,reuse,receipts));
        }
        return List.copyOf(values);
    }
    private static void zero(ByteBuffer out,int count){for(int i=0;i<count;i++)out.put((byte)0);}
    private static void zeros(ByteBuffer in,int count)throws ModernApplicationProtocol.ProtocolException{for(int i=0;i<count;i++)if(in.get()!=0)throw new ModernApplicationProtocol.ProtocolException();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid fire storm receipt");}
    private ModernFireStormProtocol(){}
}
