package local.lito381.server;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** v27 skill77 actor-only authority receipt. This payload is NOT negotiated by v1-26.
 * The enclosing combat frame binds it to the frozen match and actors.
 * Life tokens are one-based; coordinates are copied at commit, not live pointers.
 */
final class ModernDisintegrateState {
    static final int STATE_BYTES=152, MAX_ACTORS=100;
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP,
        NUMERIC, TARGET_DEAD, SAME_TEAM, OUT_OF_RANGE, BLOCKED, TOO_HEAVY,
        STALE_EFFECT, STALE_REGEN, LAWFUL, STALE_LIFE, STALE_STATE }
    record Request(ModernRooms.Scope scope,long matchId,long generation,long sequence,
                   long casterLife,long targetEntityId,long targetLife) {
        Request {Objects.requireNonNull(scope);if(matchId<1||generation<1||sequence<1||casterLife<1||targetEntityId<1||targetLife<1)throw bad();}
    }
    record Hit(long targetEntityId,long casterLife,long targetLife,int fromX,int fromY,int toX,int toY,
               long hpBefore,long hpAfter,long rawDamage,long appliedDamage,long damageLossAfter,boolean critical) {
        Hit {
            long range=Math.max(Math.abs((long)fromX-toX),Math.abs((long)fromY-toY));
            if(targetEntityId<1||casterLife<1||targetLife<1||range<1||range>ModernDisintegrate.RANGE
                    ||hpBefore<1||hpAfter<0||hpAfter>hpBefore||rawDamage<0||appliedDamage!=Math.min(hpBefore,rawDamage)
                    ||appliedDamage!=hpBefore-hpAfter||damageLossAfter<appliedDamage)throw bad();
        }
        int effect(){return critical?ModernDisintegrate.CRITICAL_EFFECT:ModernDisintegrate.EFFECT;}
    }
    record State(long entityId,long castCount,long revision,long mpSpent,long lastSequence,
                 long initialLawful,long lawful,int mpCost,int reuseMillis,Hit hit) {
        State {
            if(entityId<1||castCount<0||revision<0||mpSpent<0||lastSequence<castCount
                    ||mpCost!=0&&(mpCost<59||mpCost>70)||reuseMillis!=(mpCost==0?0:ModernDisintegrate.REUSE_MILLIS)
                    ||mpSpent!=Math.multiplyExact(castCount,mpCost)||lawful!=Math.addExact(initialLawful,castCount))throw bad();
            if(castCount==0){if(revision!=0||lastSequence!=0||hit!=null)throw bad();}
            else if(revision==0||mpCost==0||hit==null||hit.targetEntityId()==entityId||!ModernDisintegrate.lawfulAllows(initialLawful))throw bad();
            if(mpCost==0&&(castCount!=0||initialLawful!=0))throw bad();
        }
        static State initial(long entityId,int cost,long lawful){return new State(entityId,0,0,0,0,lawful,lawful,cost,cost==0?0:ModernDisintegrate.REUSE_MILLIS,null);}
        State cast(long next,long sequence,Hit receipt) {
            if(next<=revision||sequence<=lastSequence)throw bad();
            var spending=ModernSkillSpending.advance(castCount,mpSpent,mpCost);
            return new State(entityId,spending.casts(),next,spending.spent(),sequence,initialLawful,
                    ModernDisintegrate.lawfulAfterCast(lawful),mpCost,reuseMillis,receipt);
        }
        State wireRevision(){return new State(entityId,castCount,revision==0?0:Math.incrementExact(revision),mpSpent,lastSequence,initialLawful,lawful,mpCost,reuseMillis,hit);}
    }
    /** Called by the engine snapshot too: a receipt cannot silently diverge from
     * HP/MP, another spell, a later life, or an elimination result. */
    static void validate(long revision,ModernBattleEngine.Phase phase,ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest map,
            List<ModernBattleEngine.Actor> actors,List<ModernCombatProtocol.Vital> vitals,List<ModernSpellProtocol.CastState> casts,
            List<ModernDamageSpellProtocol.State> damage,List<ModernItemProtocol.State> items,List<ModernRegenProtocol.State> regen,
            ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires,List<State> states) {
        if(states.isEmpty())return;
        int size=actors.size();
        if(size<1||size>MAX_ACTORS||states.size()!=size||vitals.size()!=size||casts.size()!=size||regen.size()!=size
                ||damage.size()!=size||!fires.isEmpty()&&fires.size()!=size||room.summary().config().mode()!=1&&!honor.enabled())throw bad();
        var revisions=new HashSet<Long>();
        for(int i=0;i<size;i++) {
            var s=states.get(i);var a=actors.get(i);var v=vitals.get(i);var c=casts.get(i);
            if(s.entityId()!=a.entityId()||s.revision()>revision||s.lastSequence()>c.requestSequence())throw bad();
            if(a.member().identity().isHuman()?s.mpCost()==0:!s.equals(State.initial(s.entityId(),0,0)))throw bad();
            if((phase==ModernBattleEngine.Phase.LOADING||phase==ModernBattleEngine.Phase.COUNTDOWN)&&s.castCount()!=0)throw bad();
            long total=Math.addExact(c.castCount(),damage.get(i).castCount());
            total=Math.addExact(total,honor.enabled()?honor.heals().get(i).castCount():0);
            total=Math.addExact(total,fires.isEmpty()?0:fires.get(i).castCount());
            if(Math.addExact(total,s.castCount())>c.requestSequence())throw bad();
            if(s.hit()==null)continue;
            if(!revisions.add(s.revision())||s.revision()==c.castRevision()||s.revision()==damage.get(i).castRevision()
                    ||s.revision()==v.attackRevision()||s.revision()==v.hurtRevision()
                    ||s.revision()==v.deathRevision()||honor.enabled()&&s.revision()==honor.lives().get(i).respawnRevision()
                    ||s.revision()==regen.get(i).hpRevision()||s.revision()==regen.get(i).mpRevision()
                    ||!items.isEmpty()&&s.revision()==items.get(i).useRevision()
                    ||!fires.isEmpty()&&s.revision()==fires.get(i).castRevision()
                    ||honor.enabled()&&s.revision()==honor.heals().get(i).revision())throw bad();
            if(v.deathRevision()>0&&s.revision()>=v.deathRevision()
                    &&(v.hp()==0||honor.enabled()&&s.revision()<=honor.lives().get(i).respawnRevision()))throw bad();
            var h=s.hit();if(h.targetEntityId()>size)throw bad();int n=Math.toIntExact(h.targetEntityId()-1);
            var t=actors.get(n);var tv=vitals.get(n);var r=regen.get(n);
            if(!ModernWorldProtocol.contains(map,new ModernWorldMap.Cell(h.fromX(),h.fromY()))
                    ||!ModernWorldProtocol.contains(map,new ModernWorldMap.Cell(h.toX(),h.toY()))
                    ||TournamentRoomPolicy.rules(room.summary().config().mode(),room.summary().config().mapId()).teamBased()&&a.member().team()==t.member().team()
                    ||h.hpBefore()>tv.maxHp()||h.damageLossAfter()>r.damageLoss()
                    ||h.appliedDamage()>0&&tv.hurtRevision()<s.revision()||h.hpAfter()==0&&tv.deathRevision()<s.revision())throw bad();
            long casterLife=honor.enabled()?Math.incrementExact(honor.lives().get(i).respawnCount()):1;
            long targetLife=honor.enabled()?Math.incrementExact(honor.lives().get(n).respawnCount()):1;
            if(h.casterLife()>casterLife||h.targetLife()>targetLife)throw bad();
            if(honor.enabled()) {
                var cl=honor.lives().get(i);var tl=honor.lives().get(n);
                if((h.casterLife()==casterLife?s.revision()<=cl.respawnRevision():s.revision()>cl.respawnRevision())
                        ||(h.targetLife()==targetLife?s.revision()<=tl.respawnRevision():s.revision()>tl.respawnRevision()))throw bad();
            }
            if(h.hpAfter()==0&&h.targetLife()==targetLife&&tv.hp()!=0)throw bad();
            if(r.hpRevision()<=s.revision()&&(items.isEmpty()||items.get(n).useRevision()<=s.revision())) {
                var hp=BigInteger.valueOf(tv.maxHp()).multiply(BigInteger.valueOf(h.targetLife()))
                        .add(BigInteger.valueOf(r.hpGain())).subtract(BigInteger.valueOf(r.hpLoss()))
                        .add(BigInteger.valueOf(r.itemGain())).subtract(BigInteger.valueOf(h.damageLossAfter()));
                if(!hp.equals(BigInteger.valueOf(h.hpAfter())))throw bad();
            }
            if(s.revision()==revision&&(v.hp()==0||c.requestSequence()!=s.lastSequence()||h.casterLife()!=casterLife||h.targetLife()!=targetLife
                    ||a.position().x()!=h.fromX()||a.position().y()!=h.fromY()||t.position().x()!=h.toX()||t.position().y()!=h.toY()
                    ||tv.hp()!=h.hpAfter()||r.damageLoss()!=h.damageLossAfter()
                    ||h.appliedDamage()>0&&tv.hurtRevision()!=revision||h.appliedDamage()==0&&tv.hurtRevision()==revision
                    ||h.hpAfter()==0&&tv.deathRevision()!=revision))throw bad();
        }
    }
    /** Big-endian, append-only v27 combat payload. Codec support alone does not
     * authorize source preparation, a request, resource loading or UI casting. */
    static byte[] encode(List<State> states) {
        states=List.copyOf(states);if(states.size()>MAX_ACTORS)throw bad();
        var out=ByteBuffer.allocate(8+STATE_BYTES*states.size());out.putShort((short)states.size());zero(out,6);
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);if(s.entityId()!=i+1)throw bad();
            out.putLong(s.entityId()).putLong(s.castCount()).putLong(s.revision()).putLong(s.mpSpent()).putLong(s.lastSequence())
                    .putLong(s.initialLawful()).putLong(s.lawful()).putInt(s.mpCost()).putInt(s.reuseMillis());
            var h=s.hit();if(h==null){zero(out,88);continue;}
            out.putLong(h.targetEntityId()).putLong(h.casterLife()).putLong(h.targetLife()).putInt(h.fromX()).putInt(h.fromY()).putInt(h.toX()).putInt(h.toY())
                    .putLong(h.hpBefore()).putLong(h.hpAfter()).putLong(h.rawDamage()).putLong(h.appliedDamage()).putLong(h.damageLossAfter()).put((byte)(h.critical()?1:0));zero(out,7);
        }
        return out.array();
    }
    static List<State> decode(byte[] bytes) {
        if(bytes==null||bytes.length<8||bytes.length>8+MAX_ACTORS*STATE_BYTES)throw bad();
        var in=ByteBuffer.wrap(bytes);int count=Short.toUnsignedInt(in.getShort());zeros(in,6);
        if(count>MAX_ACTORS||bytes.length!=8+count*STATE_BYTES)throw bad();var result=new ArrayList<State>(count);
        for(int i=0;i<count;i++) {
            long id=in.getLong(),casts=in.getLong(),revision=in.getLong(),spent=in.getLong(),seq=in.getLong(),initial=in.getLong(),lawful=in.getLong();
            int cost=in.getInt(),reuse=in.getInt();Hit h=null;
            if(casts==0)zeros(in,88);
            else {
                long target=in.getLong(),casterLife=in.getLong(),targetLife=in.getLong();int fx=in.getInt(),fy=in.getInt(),tx=in.getInt(),ty=in.getInt();
                long before=in.getLong(),after=in.getLong(),raw=in.getLong(),applied=in.getLong(),loss=in.getLong();int critical=Byte.toUnsignedInt(in.get());zeros(in,7);
                if(critical>1)throw bad();h=new Hit(target,casterLife,targetLife,fx,fy,tx,ty,before,after,raw,applied,loss,critical==1);
            }
            if(id!=i+1)throw bad();result.add(new State(id,casts,revision,spent,seq,initial,lawful,cost,reuse,h));
        }
        return List.copyOf(result);
    }
    static void put(ByteBuffer out,List<State> states){out.put(encode(states));}
    static List<State> get(ByteBuffer in,int actors) {
        if(in.remaining()<8)throw bad();
        int count=Short.toUnsignedInt(in.getShort(in.position()));
        if(count>MAX_ACTORS||count!=0&&count!=actors||in.remaining()<8+count*STATE_BYTES)throw bad();
        byte[] bytes=new byte[8+count*STATE_BYTES];in.get(bytes);return decode(bytes);
    }
    private static void zero(ByteBuffer out,int n){for(int i=0;i<n;i++)out.put((byte)0);}
    private static void zeros(ByteBuffer in,int n){for(int i=0;i<n;i++)if(in.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid disintegrate receipt");}
    private ModernDisintegrateState(){}
}
