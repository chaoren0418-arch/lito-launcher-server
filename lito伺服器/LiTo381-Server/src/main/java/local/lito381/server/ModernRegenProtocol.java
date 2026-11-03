package local.lito381.server;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** v12 append-only applied-vital ledger. No random rolls, source account or clock are sent. */
final class ModernRegenProtocol {
    static final int STATE_BYTES=104;
    record State(long entityId,long hpGain,long hpLoss,long hpRevision,long hpBefore,long hpAfter,
            long mpGain,long mpLoss,long mpRevision,long mpBefore,long mpAfter,long itemGain,long damageLoss) {
        State {
            if(entityId<1||hpGain<0||hpLoss<0||hpRevision<0||hpBefore<0||hpAfter<0||mpGain<0||mpLoss<0
                    ||mpRevision<0||mpBefore<0||mpAfter<0||itemGain<0||damageLoss<0)throw bad();
            axis(hpGain,hpLoss,hpRevision,hpBefore,hpAfter,true);axis(mpGain,mpLoss,mpRevision,mpBefore,mpAfter,false);
        }
        static State initial(long entityId){return new State(entityId,0,0,0,0,0,0,0,0,0,0,0,0);}
        State regenerated(ModernNaturalRegen.Outcome out,long revision) {
            if(revision<=Math.max(hpRevision,mpRevision))throw bad();
            boolean h=out.hpGain()!=0||out.hpLoss()!=0,m=out.mpGain()!=0||out.mpLoss()!=0;
            return new State(entityId,Math.addExact(hpGain,out.hpGain()),Math.addExact(hpLoss,out.hpLoss()),h?revision:hpRevision,
                    h?out.hpBefore():hpBefore,h?out.hpAfter():hpAfter,Math.addExact(mpGain,out.mpGain()),Math.addExact(mpLoss,out.mpLoss()),
                    m?revision:mpRevision,m?out.mpBefore():mpBefore,m?out.mpAfter():mpAfter,itemGain,damageLoss);
        }
        State healed(long restored){if(restored<0)throw bad();return new State(entityId,hpGain,hpLoss,hpRevision,hpBefore,hpAfter,mpGain,mpLoss,mpRevision,mpBefore,mpAfter,Math.addExact(itemGain,restored),damageLoss);}
        State damaged(long applied){if(applied<0)throw bad();return new State(entityId,hpGain,hpLoss,hpRevision,hpBefore,hpAfter,mpGain,mpLoss,mpRevision,mpBefore,mpAfter,itemGain,Math.addExact(damageLoss,applied));}
        State wireRevision(){return new State(entityId,hpGain,hpLoss,hpRevision==0?0:Math.incrementExact(hpRevision),hpBefore,hpAfter,
                mpGain,mpLoss,mpRevision==0?0:Math.incrementExact(mpRevision),mpBefore,mpAfter,itemGain,damageLoss);}
    }
    private static void axis(long gain,long loss,long revision,long before,long after,boolean hp) {
        if(revision==0){if(gain!=0||loss!=0||before!=0||after!=0)throw bad();return;}
        if(gain==0&&loss==0||before==after||hp&&(before<1||after<1)
                ||after>before&&after-before>gain||before>after&&before-after>loss)throw bad();
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states) {
        validateStates(world,vitals,damages,items,states,ModernHonor.Snapshot.disabled());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor) {
        validateStates(world,vitals,damages,items,states,honor,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires) {
        validateStates(world,vitals,damages,items,states,honor,fires,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires,List<ModernNpcMagic.CastState> npcs) {
        validateStates(world,vitals,damages,items,states,honor,fires,npcs,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires,List<ModernNpcMagic.CastState> npcs,List<ModernDisintegrateState.State> disintegrations) {
        validateStates(world,vitals,damages,items,states,honor,fires,npcs,disintegrations,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires,List<ModernNpcMagic.CastState> npcs,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations) {
        validateStates(world,vitals,damages,items,states,honor,fires,npcs,disintegrations,npcDisintegrations,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals,
            List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,List<State> states,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fires,List<ModernNpcMagic.CastState> npcs,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations,List<ModernNpcFireStormState.State> npcFires) {
        if(states.isEmpty())return;
        if(states.size()!=world.actors().size()||states.size()!=vitals.size())throw bad();
        for(int i=0;i<states.size();i++) {
            var s=states.get(i);var v=vitals.get(i);var actor=world.actors().get(i);
            long tpRevision=world.casts().isEmpty()?0:world.casts().get(i).castRevision();
            long damageRevision=damages.isEmpty()?0:damages.get(i).castRevision();
            long itemRevision=items.isEmpty()?0:items.get(i).useRevision();
            long healRevision=honor.enabled()?honor.heals().get(i).revision():0;
            var questLife=world.quest().life(v.entityId());
            var siegeLife=world.siege()==null?null:world.siege().snapshot().lives().get(i);
            long respawnRevision=siegeLife!=null?(siegeLife.respawnRevision()==0?0:Math.incrementExact(siegeLife.respawnRevision())):honor.enabled()?honor.lives().get(i).respawnRevision():questLife==null?0:questLife.respawnRevision();
            long fireRevision=fires.isEmpty()?0:fires.get(i).castRevision();
            long npcRevision=npcs.isEmpty()?0:npcs.get(i).revision();
            long disRevision=disintegrations.isEmpty()?0:disintegrations.get(i).revision();
            long npcDisRevision=npcDisintegrations.isEmpty()?0:npcDisintegrations.get(i).revision();
            long npcFireRevision=npcFires.isEmpty()?0:npcFires.get(i).revision();
            if(s.entityId()!=actor.entityId()||s.entityId()!=v.entityId()||s.hpRevision()>world.revision()||s.mpRevision()>world.revision()
                    ||s.hpBefore()>v.maxHp()||s.hpAfter()>v.maxHp()||s.mpBefore()>v.maxMp()||s.mpAfter()>v.maxMp())throw bad();
            if(!actor.member().identity().isHuman()&&!s.equals(State.initial(s.entityId()).damaged(s.damageLoss())))throw bad();
            if((world.phase()==ModernWorldProtocol.Phase.LOADING||world.phase()==ModernWorldProtocol.Phase.COUNTDOWN)&&!s.equals(State.initial(s.entityId())))throw bad();
            if(s.hpRevision()!=0&&(s.hpRevision()==v.hurtRevision()||s.hpRevision()==itemRevision
                    ||s.hpRevision()==tpRevision||s.hpRevision()==damageRevision))throw bad();
            if(s.mpRevision()!=0&&(s.mpRevision()==v.hurtRevision()||s.mpRevision()==itemRevision
                    ||s.mpRevision()==tpRevision||s.mpRevision()==damageRevision))throw bad();
            if(v.hp()==0&&(s.hpRevision()>=v.deathRevision()||s.mpRevision()>=v.deathRevision()))throw bad();
            if(s.hpRevision()!=0&&(s.hpRevision()==healRevision||s.hpRevision()==respawnRevision)
                    ||s.mpRevision()!=0&&(s.mpRevision()==healRevision||s.mpRevision()==respawnRevision))throw bad();
            if(s.hpRevision()!=0&&s.hpRevision()==fireRevision||s.mpRevision()!=0&&s.mpRevision()==fireRevision)throw bad();
            if(s.hpRevision()!=0&&s.hpRevision()==npcRevision||s.mpRevision()!=0&&s.mpRevision()==npcRevision)throw bad();
            if(s.hpRevision()!=0&&s.hpRevision()==disRevision||s.mpRevision()!=0&&s.mpRevision()==disRevision)throw bad();
            if(s.hpRevision()!=0&&s.hpRevision()==npcDisRevision||s.mpRevision()!=0&&s.mpRevision()==npcDisRevision)throw bad();
            if(s.hpRevision()!=0&&s.hpRevision()==npcFireRevision||s.mpRevision()!=0&&s.mpRevision()==npcFireRevision)throw bad();
            validateTotals(s,v,world.casts().isEmpty()?0:world.casts().get(i).mpSpent(),damages.isEmpty()?0:damages.get(i).mpSpent(),
                    siegeLife!=null?siegeLife.hpGain():honor.enabled()?honor.lives().get(i).respawnHpGain():questLife==null?0:questLife.hpGain(),honor.enabled()?honor.heals().get(i).mpSpent():0,fires.isEmpty()?0:fires.get(i).mpSpent(),npcs.isEmpty()?0:npcs.get(i).mpSpent(),disintegrations.isEmpty()?0:disintegrations.get(i).mpSpent(),npcDisintegrations.isEmpty()?0:npcDisintegrations.get(i).mpSpent(),npcFires.isEmpty()?0:npcFires.get(i).mpSpent());
            if(items.isEmpty()){if(s.itemGain()!=0)throw bad();}else validateHealing(s,items.get(i));
            long hpAnchor=Math.max(s.hpRevision(),itemRevision),hpAfter=hpAnchor==0?v.maxHp():s.hpRevision()>itemRevision?s.hpAfter():items.get(i).hpAfter();
            if(respawnRevision>hpAnchor){hpAnchor=respawnRevision;hpAfter=v.maxHp();}
            if(v.hp()>hpAfter||v.hurtRevision()<=hpAnchor&&v.hp()!=hpAfter)throw bad();
            long mpAfter=s.mpRevision()==0?v.maxMp():s.mpAfter();
            if(v.mp()>mpAfter||s.mpRevision()>Math.max(npcFireRevision,Math.max(npcDisRevision,Math.max(disRevision,Math.max(npcRevision,Math.max(fireRevision,Math.max(healRevision,Math.max(tpRevision,damageRevision)))))))&&v.mp()!=mpAfter)throw bad();
            if(world.completion()!=null&&(s.hpRevision()>Math.addExact(world.completion().event().engineRevision(),1)
                    ||s.mpRevision()>Math.addExact(world.completion().event().engineRevision(),1)))throw bad();
        }
    }
    private static void validateHealing(State s,ModernItemProtocol.State item) {
        if(s.itemGain()<item.restored()||item.useCount()==0&&s.itemGain()!=0||item.useCount()==1&&s.itemGain()!=item.restored())throw bad();
        BigInteger upper=BigInteger.ZERO;
        for(var stock:item.stocks()) {
            var kind=ModernItemProtocol.HealingKind.find(stock.itemId());if(kind==null)continue;
            long consumed=Math.subtractExact(stock.initial(),stock.remaining());
            upper=upper.add(big(consumed).multiply(big(kind.maximum).add(big(item.fixedRecovery()))));
        }
        var last=ModernItemProtocol.HealingKind.find(item.itemId());
        if(last!=null&&item.useCount()>0)upper=upper.subtract(big(last.maximum).add(big(item.fixedRecovery()))).add(big(item.restored()));
        if(big(s.itemGain()).compareTo(upper)>0)throw bad();
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,0,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,respawnHpGain,healingSpent,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent,long fireSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,respawnHpGain,healingSpent,fireSpent,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent,long fireSpent,long npcSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,respawnHpGain,healingSpent,fireSpent,npcSpent,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent,long fireSpent,long npcSpent,long disintegrateSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,respawnHpGain,healingSpent,fireSpent,npcSpent,disintegrateSpent,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent,long fireSpent,long npcSpent,long disintegrateSpent,long npcDisintegrateSpent) {
        validateTotals(s,v,teleportSpent,damageSpent,respawnHpGain,healingSpent,fireSpent,npcSpent,disintegrateSpent,npcDisintegrateSpent,0);
    }
    static void validateTotals(State s,ModernCombatProtocol.Vital v,long teleportSpent,long damageSpent,long respawnHpGain,long healingSpent,long fireSpent,long npcSpent,long disintegrateSpent,long npcDisintegrateSpent,long npcFireSpent) {
        if(teleportSpent<0||damageSpent<0)throw bad();
        if(respawnHpGain<0||healingSpent<0||fireSpent<0||npcSpent<0||disintegrateSpent<0||npcDisintegrateSpent<0||npcFireSpent<0)throw bad();
        var hp=big(v.maxHp()).add(big(s.hpGain())).subtract(big(s.hpLoss())).add(big(s.itemGain())).subtract(big(s.damageLoss())).add(big(respawnHpGain));
        var mp=big(v.maxMp()).add(big(s.mpGain())).subtract(big(s.mpLoss())).subtract(big(teleportSpent)).subtract(big(damageSpent)).subtract(big(healingSpent)).subtract(big(fireSpent)).subtract(big(npcSpent)).subtract(big(disintegrateSpent)).subtract(big(npcDisintegrateSpent)).subtract(big(npcFireSpent));
        if(!hp.equals(big(v.hp()))||!mp.equals(big(v.mp()))||(s.damageLoss()==0)!=(v.hurtRevision()==0))throw bad();
    }
    static void putStates(ByteBuffer out,List<State> states) {
        if(states.size()>100)throw bad();out.putShort((short)states.size());zeroes(out,6);long previous=0;
        for(var s:states){if(s.entityId()<=previous)throw bad();previous=s.entityId();
            out.putLong(s.entityId()).putLong(s.hpGain()).putLong(s.hpLoss()).putLong(s.hpRevision()).putLong(s.hpBefore()).putLong(s.hpAfter())
                    .putLong(s.mpGain()).putLong(s.mpLoss()).putLong(s.mpRevision()).putLong(s.mpBefore()).putLong(s.mpAfter()).putLong(s.itemGain()).putLong(s.damageLoss());}
    }
    static List<State> getStates(ByteBuffer in,int actors)throws ModernApplicationProtocol.ProtocolException {
        int count=Short.toUnsignedInt(in.getShort());for(int i=0;i<6;i++)if(in.get()!=0)fail();
        if(count>100||count!=0&&count!=actors||in.remaining()<count*STATE_BYTES)fail();var result=new ArrayList<State>();long previous=0;
        for(int i=0;i<count;i++){var s=new State(in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong());
            if(s.entityId()<=previous)fail();previous=s.entityId();result.add(s);}return List.copyOf(result);
    }
    private static BigInteger big(long value){return BigInteger.valueOf(value);}
    private static void zeroes(ByteBuffer out,int count){for(int i=0;i<count;i++)out.put((byte)0);}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid natural regeneration ledger");}
    private static void fail()throws ModernApplicationProtocol.ProtocolException{throw new ModernApplicationProtocol.ProtocolException();}
    private ModernRegenProtocol(){}
}
