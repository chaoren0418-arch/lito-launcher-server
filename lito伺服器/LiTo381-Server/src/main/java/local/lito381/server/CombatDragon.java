package local.lito381.server;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import java.util.Set;

/** Current source Dragon Knight rules; all mutable leases remain on the session. */
final class CombatDragon {
    private static final String P="local/lito381/server/";
    // Translate monotonic lease time into the source's integer wall-second band.
    // No actor, cast, inventory or random state is stored in this class.
    private static final long EPOCH_OFFSET_NANOS=System.currentTimeMillis()*1_000_000L-System.nanoTime();
    static long weaknessDeadline(long now) {return weaknessDeadline(now,EPOCH_OFFSET_NANOS);}
    static long weaknessDeadline(long now,long epochOffset) {
        long second=Math.floorDiv(Math.addExact(now,epochOffset),1_000_000_000L);
        return Math.subtractExact(Math.multiplyExact(Math.addExact(second,17),1_000_000_000L),epochOffset);
    }
    static void normalAttack(ClientSession caster) {
        if(!CombatRules.loaded())return;
        var sequence=(BattleFoeSlayerSequence)CombatAccess.field(caster,"foeSlayerSequence");
        boolean active;
        synchronized(sequence) {active=((Number)CombatAccess.field(sequence,"remainingStrikes")).intValue()!=0;}
        if(!active)
            ((BattleChainswordWeakness)CombatAccess.field(caster,"chainswordWeakness")).sourceNormalAttack(System.nanoTime());
    }
    static BattleFoeSlayerSequence.Strike consumeOnHit(BattleFoeSlayerSequence.Strike strike,ClientSession caster,boolean hit,long target) {
        if(!CombatRules.loaded()||!strike.active())return strike;
        int consumed=hit&&!damageBlocked(caster,target,System.nanoTime())
                ?((BattleChainswordWeakness)CombatAccess.field(caster,"chainswordWeakness")).sourceConsumeOnHit():0;
        return new BattleFoeSlayerSequence.Strike(true,strike.targetMismatch(),strike.strikeIndex(),strike.totalStrikes(),
                consumed,0,strike.finalStrike());
    }
    static BattleChainswordWeakness.AttackOutcome weaknessAttack(BattleChainswordWeakness state,int type,int weapon,
            long target,long now,int roll,ClientSession caster) {
        return CombatRules.loaded()&&damageBlocked(caster,target,now)?BattleChainswordWeakness.AttackOutcome.none()
                :state.attack(type,weapon,target,now,roll);
    }
    private static boolean damageBlocked(ClientSession caster,long target,long now) {
        var hub=(GameHub)CombatAccess.field(caster,"hub");
        String phase=CombatAccess.field(caster,"phase").toString();
        long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();
        int map=((Number)CombatAccess.field(caster,"classicWorldMapId")).intValue();
        int instance=((Number)CombatAccess.field(caster,"classicWorldInstanceId")).intValue();
        var players=phase.equals("WORLD")?hub.classicWorldMembers(map,instance):hub.roomCombatants(room);
        for(ClientSession player:players)if(player.playerObjectId()==target)return player.cubeQuakeBindProtected(now);
        for(ClientSession owner:players) {
            var controlled=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",target);
            if(controlled!=null&&controlled.snapshot().active())return controlled.freezeActive(now)||controlled.earthBindActive(now);
        }
        return phase.equals("WORLD")?hub.classicWorldMonsterFreezeActive(map,instance,target,now)
                ||hub.classicWorldMonsterEarthBindActive(map,instance,target,now)
                :hub.questMonsterFreezeActive(room,target,now)||hub.questMonsterEarthBindActive(room,target,now)
                    ||hub.timeAttackActorFreezeActive(room,target,now)||hub.timeAttackActorEarthBindActive(room,target,now);
    }
    static double multiplier(double existing,BattleFoeSlayerSequence.Strike strike) {
        return CombatRules.loaded()&&strike.active()&&strike.exposureLevel()==3?Math.max(existing,1.3):existing;
    }
    static int extraDamage(IntUnaryOperator roll) {
        int maximum=(int)CombatRules.number("FOE_SLAYER_DMG",0);
        if(maximum<=0)return 0;
        int value=roll.applyAsInt(maximum);
        if(value<1||value>maximum)throw new IllegalArgumentException("Foe Slayer extra roll outside source bound");
        return value;
    }
    static void finishFoe(ClientSession caster,Object target,boolean accepted) throws java.io.IOException {
        if(!CombatRules.loaded()||!accepted)return;
        int raw=extraDamage(maximum->ThreadLocalRandom.current().nextInt(maximum)+1);
        if(raw==0)return;
        long id=((Number)CombatAccess.get(target,"objectId")).longValue();
        var player=(ClientSession)CombatAccess.get(target,"player");
        var hub=(GameHub)CombatAccess.field(caster,"hub");
        long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();
        String phase=CombatAccess.field(caster,"phase").toString(),source="foe-slayer-mode";
        int applied=0;
        if(player!=null) {
            if(!CombatAccess.field(player,"phase").toString().equals(phase))return;
            if(phase.equals("BATTLE")&&((Number)CombatAccess.field(player,"currentRoomId")).longValue()!=room)return;
            if(phase.equals("WORLD")&&(!CombatAccess.field(player,"classicWorldMapId").equals(CombatAccess.field(caster,"classicWorldMapId"))
                    ||!CombatAccess.field(player,"classicWorldInstanceId").equals(CombatAccess.field(caster,"classicWorldInstanceId"))))return;
            int before=((Number)CombatAccess.field(player,"playerHp")).intValue();
            // SkillMode replaces the preceding magic calculation; cooking belongs to that calculation.
            int damage=raw;
            if(phase.equals("BATTLE"))CombatAccess.call(player,"takePlayerDamage","(IL"+P+"ClientSession;Z)V",damage,caster,true);
            else if(phase.equals("WORLD"))CombatAccess.call(player,"takeClassicWorldPlayerDamage","(IL"+P+"ClientSession;Z)V",damage,caster,true);
            applied=Math.max(0,before-((Number)CombatAccess.field(player,"playerHp")).intValue());
        } else if(CombatAccess.get(target,"world") instanceof ClassicWorldMonsterPopulation.Snapshot world) {
            if(!phase.equals("WORLD"))return;
            int instance=((Number)CombatAccess.field(caster,"classicWorldInstanceId")).intValue();
            var hit=hub.damageClassicWorldMonster(world.mapId(),instance,id,caster.playerObjectId(),raw);
            if(hit!=null) {
                applied=hit.appliedDamage();
                hub.provokeClassicWorldMonster(world.mapId(),instance,id,caster.playerObjectId());
                CombatAccess.call(caster,"publishClassicWorldSpellMonsterDamage","(IL"+P+"ClassicWorldMonsterPopulation$Hit;)V",187,hit);
            }
        } else if(phase.equals("BATTLE")) {
            String method=CombatAccess.get(target,"quest")!=null?"applyQuestMonsterDamage"
                    :CombatAccess.get(target,"time")!=null?"applyTimeAttackActorDamage"
                    :CombatAccess.get(target,"tower")!=null?"applyGuardianTowerDamage":null;
            if(method!=null)applied=CombatAccess.integerCall(caster,method,"(JILjava/lang/String;)I",id,raw,source);
            else if(Boolean.TRUE.equals(CombatAccess.get(target,"bot"))) {
                int before=hub.botHp(room);if(before<=0)return;
                int after=hub.damageBot(room,raw);applied=Math.max(0,before-after);
                CombatAccess.call(caster,"broadcastHitRatio","(JIILjava/lang/String;)V",id,after,GameHub.ARENA_BOT_MAX_HP,
                        after==0?"spell-bot-death":"spell-bot-damage");
                if(after==0) {
                    var resolution=new BattleRules.Resolution(true,false,Set.of(caster.playerObjectId()),Set.of(),"arena-bot-defeated-by-spell");
                    for(ClientSession member:hub.roomMembers(room)) {
                        CombatAccess.call(member,"sendEncrypted","([BLjava/lang/String;)V",Protocol.objectAction(id,BattleActionCodes.DIE),"S_ACTION spell bot death");
                        member.serverEndBattle(resolution);
                    }
                    hub.endBattle(room);
                }
            } else {
                Object companion=CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)L"+P+"ClientSession$MassSlowCompanionTarget;",id);
                if(companion!=null)applied=CombatAccess.integerCall(caster,"applyBattleSpellCompanionDamage",
                        "(L"+P+"ClientSession$MassSlowCompanionTarget;II)I",companion,raw,187);
            }
        }
        System.out.println("[LiTo] Foe Slayer mode extra caster="+caster.playerName()+" target="+id+" raw="+raw+" applied="+applied+" phase="+phase);
    }
}
