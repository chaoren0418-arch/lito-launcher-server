// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// 待驗證：候選的 nativeShockCompanionBody 保留移除 monitor 後的 POP／例外表；尚未通過完整語意驗收，見 issue-68 報告。
package local.lito381.server;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

/** Missing native BATTLE controlled-target ingress. References exist only in this call. */
final class CombatCompanionSpells {
    private static final String P="local/lito381/server/",T="L"+P+"ClientSession$MassSlowCompanionTarget;";
    record Attempt(int magicResistance,int probability,BattleEraseMagicStatus.Lease eraseMagic) {}
    /** Actor references belong to one native stack-local batch, never a session field/global cache. */
    record Hit(Object target,Attempt attempt,int damage,int roll) {}
    record Batch(List<Hit> hits) {Batch{hits=List.copyOf(hits);}}
    static Attempt prepareFreeze(ClientSession caster,BattleCompanion controlled,SourceNpcPhysicalStats stats,BattleSpellCatalog.FreezeAttackSpell spell,long now) {
        var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");var eye=(BattleDragonEyeStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
        BattleEraseMagicStatus.Lease lease;int mr;boolean earth;synchronized(controlled){lease=controlled.captureEraseMagic(now);mr=lease.present()?stats.magicResistance()>>2:stats.magicResistance();earth=controlled.earthBindActive(now);}
        int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","(J)I",now)),level=spell.includeMagicLevel()?CombatRules.magicLevel(summary.type(),summary.level()):0;
        int chance=earth?0:CombatProbability.dispatch(spell,"probability","(IIIIIII)I","casterLevel,targetLevel,effectiveIntelligence,magicLevel,magicHit,targetMagicResistance,targetFreezeResistance",new Object[]{summary.level(),controlled.snapshot().level(),intelligence,level,Math.addExact(equipment.totalMagicHit(),eye.magicHit()),mr,0},caster,new Object[]{controlled});
        return new Attempt(mr,chance,lease);
    }
    static Batch captureCenteredFreeze(ClientSession caster,BattleSpellCatalog.FreezeAttackSpell spell,long now) {
        var hits=new ArrayList<Hit>();if(!CombatRules.loaded())return new Batch(hits);
        var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();int x=integer(caster,"positionX"),y=integer(caster,"positionY"),map=hub.battleMapId(room,caster);var seen=new HashSet<Long>();
        for(var owner:hub.roomCombatants(room)) {
            // This is the same owner/faction policy as the native centered-area family.
            if(owner==caster||!hub.canAttack(room,caster,owner))continue;
            for(var actor:owner.controlledCompanionSnapshots()) {
                if(!actor.active()||actor.hp()<=0||Math.max(Math.abs(x-actor.x()),Math.abs(y-actor.y()))>spell.areaRadius()
                        ||!BattleSpellCatalog.isInNativeScreen(x,y,actor.x(),actor.y())||!BattleSpellCatalog.hasNativeLineOfSight(ArenaMapCatalog.forMap(map),x,y,actor.x(),actor.y())||!seen.add(actor.objectId()))continue;
                Object target=CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)"+T,actor.objectId());if(target==null||!available(caster,target))continue;
                var controlled=(BattleCompanion)CombatAccess.get(target,"controlled");var stats=(SourceNpcPhysicalStats)CombatAccess.get(target,"stats");if(stats==null||controlled.freezeActive(now))continue;
                hits.add(new Hit(target,prepareFreeze(caster,controlled,stats,spell,now),-1,-1));
            }
        }
        return new Batch(hits);
    }
    static Batch drawCenteredFreeze(ClientSession caster,BattleSpellCatalog.FreezeAttackSpell spell,Batch captured,long now,List<Protocol.AttackManyTarget> wire) {
        return drawCenteredFreeze(caster,spell,captured,now,wire,sides->ThreadLocalRandom.current().nextInt(1,sides+1));
    }
    static Batch drawCenteredFreeze(ClientSession caster,BattleSpellCatalog.FreezeAttackSpell spell,Batch captured,long now,List<Protocol.AttackManyTarget> wire,IntUnaryOperator roll) {
        var hits=new ArrayList<Hit>();if(captured==null)return new Batch(hits);
        for(var prepared:captured.hits()) {
            if(!available(caster,prepared.target()))continue;var controlled=(BattleCompanion)CombatAccess.get(prepared.target(),"controlled");if(controlled.freezeActive(now))continue;
            int source=spell.baseDamage();for(int i=0;i<spell.diceCount();i++)source=Math.addExact(source,checkedRoll(roll,spell.dieSides()));
            int damage=CombatCompanionDamage.ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,0,prepared.attempt().magicResistance(),0,"centered-freeze-companion="+controlled.snapshot().objectId(),controlled);if(controlled.earthBindActive(now))damage=0;
            hits.add(new Hit(prepared.target(),prepared.attempt(),damage,checkedRoll(roll,100)));wire.add(new Protocol.AttackManyTarget(controlled.snapshot().objectId(),damage>0));
        }
        return new Batch(hits);
    }
    static int commitCenteredFreeze(ClientSession caster,BattleSpellCatalog.FreezeAttackSpell spell,Batch batch,long now,List<ClientSession> recipients) {
        return commitTargets(caster,spell.skillId(),spell,batch,now,recipients,"centered-freeze");
    }
    static int commitTargets(ClientSession caster,int skill,BattleSpellCatalog.FreezeAttackSpell spell,Batch batch,long now,List<ClientSession> recipients,String family) {
        int frozen=0;if(batch==null)return frozen;
        for(var hit:batch.hits()) {
            if(hit.damage()<0||!available(caster,hit.target()))continue;var controlled=(BattleCompanion)CombatAccess.get(hit.target(),"controlled");
            int applied=NativeSourceControlledDisplay.invokeMagic(caster,"applyBattleSpellCompanionDamage","("+T+"II)I",new Object[]{hit.target(),hit.damage(),skill});boolean accepted=applied>0||hit.damage()==0&&available(caster,hit.target());
            if(accepted){controlled.consumeEraseMagic(hit.attempt().eraseMagic());controlled.cancelSleep();}
            boolean freeze=spell!=null&&accepted&&hit.attempt().probability()>=hit.roll()&&hit.roll()>0&&available(caster,hit.target())&&controlled.activateFreeze(now,spell.durationSeconds());
            if(freeze) {
                var actor=controlled.snapshot();CombatAccess.call(caster,"placeBattleFreezeEffect","(JIIJILjava/util/List;Ljava/lang/String;)V",actor.objectId(),actor.x(),actor.y(),now,spell.durationSeconds(),recipients,spell.key());
                for(var member:recipients)send(member,Protocol.poison(actor.objectId(),2),"S_POISON "+family+" companion="+actor.objectId()+" type=2 reason="+spell.key());frozen++;
            }
            System.out.println("[LiTo] "+family+" controlled skill="+skill+" target="+controlled.snapshot().objectId()+" damage="+applied+" probability="+hit.attempt().probability()+" roll="+hit.roll()+" frozen="+freeze);
        }
        return frozen;
    }
    static boolean available(ClientSession caster,Object target){return (boolean)CombatAccess.call(caster,"battleSpellCompanionStillAvailable","("+T+")Z",target);}
    static void send(ClientSession player,byte[] bytes,String note){CombatAccess.call(player,"sendEncrypted","([BLjava/lang/String;)V",bytes,note);}
    static int integer(ClientSession player,String field){return ((Number)CombatAccess.field(player,field)).intValue();}
    static int heading(int sx,int sy,int tx,int ty){int x=Integer.compare(tx,sx),y=Integer.compare(ty,sy);return x>0?(y>0?3:y<0?1:2):x<0?(y>0?5:y<0?7:6):y>0?4:0;}
    static Attempt prepare(ClientSession caster,BattleCompanion controlled,SourceNpcPhysicalStats stats,BattleSpellCatalog.ShockStunSpell spell,long now) {
        var actor=controlled.snapshot();var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
        var eye=(BattleDragonEyeStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
        BattleEraseMagicStatus.Lease lease;int mr;boolean earth;
        synchronized(controlled){lease=controlled.captureEraseMagic(now);mr=lease.present()?stats.magicResistance()>>2:stats.magicResistance();earth=controlled.earthBindActive(now);}
        int chance=earth?0:CombatProbability.dispatch(spell,"probability","(IIIIIIZ)I","casterLevel,targetLevel,totalMagicHit,targetMr,targetStunResistance,stunSuccess,targetBoss",
                new Object[]{caster.battleLevel(),actor.level(),Math.addExact(equipment.totalMagicHit(),eye.magicHit()),mr,0,equipment.totalStunSuccess(),stats.boss()},caster,new Object[]{controlled});
        return new Attempt(mr,chance,lease);
    }
    static boolean shock(ClientSession caster,byte[] payload,BattleSpellCatalog.ShockStunSpell spell) {
        return shock(caster,payload,spell,sides->ThreadLocalRandom.current().nextInt(1,sides+1));
    }
    static boolean shock(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ShockStunSpell shockStunSpell, IntUnaryOperator intUnaryOperator) {
        return NativeShockCompanionAction.shock(clientSession, byArray, shockStunSpell, intUnaryOperator);
    }
    static int checkedRoll(IntUnaryOperator roll,int sides){int value=roll.applyAsInt(sides);if(value<1||value>sides)throw new IllegalArgumentException("controlled spell random out of range");return value;}

    static boolean nativeShockCompanionBody(ClientSession caster,byte[] payload,BattleSpellCatalog.ShockStunSpell spell,IntUnaryOperator roll,Object selectedTarget) {
        if(!CombatRules.loaded())return false;
        var request=Protocol.attackSpellRequest(payload);if(request.skillId()!=spell.skillId())return false;
        Object target=selectedTarget;if(target==null)return false;
        var owner=(ClientSession)CombatAccess.get(target,"owner");var controlled=(BattleCompanion)CombatAccess.get(target,"controlled");var actor=(BattleCompanion.Snapshot)CombatAccess.get(target,"snapshot");var stats=(SourceNpcPhysicalStats)CombatAccess.get(target,"stats");
        var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();int sx=integer(caster,"positionX"),sy=integer(caster,"positionY");int map=hub.battleMapId(room,caster),walk=caster.battleWalkAction(),action=BattleActionCodes.attackForWalk(walk);
        if(stats==null||!NativeShockCompanionAction.available(caster,target)||owner!=caster&&!hub.canAttack(room,caster,owner)
                ||Math.max(Math.abs(sx-actor.x()),Math.abs(sy-actor.y()))>spell.maximumRange()||!spell.supportsWalkAction(walk)
                ||!BattleAnimationCatalog.get().supportsTimedAction(caster.battleGfx(),action)
                ||!BattleSpellCatalog.isInNativeScreen(sx,sy,actor.x(),actor.y())
                ||!BattleSpellCatalog.hasNativeLineOfSight(ArenaMapCatalog.forMap(map),sx,sy,actor.x(),actor.y()))return true;
        var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
        int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","()I"));int mpCost=NativeSkillConsumptionBridges.ShockStunSpell_effectiveMpCost_III(spell,intelligence,BattleSpellCatalog.originalMagicConsumeReduction(summary.type(),summary.originalIntelligence()));
        int hpCost=CombatDatabase.value(0,87,"ShockStunSpell","hpCost"),material=CombatDatabase.value(0,87,"ShockStunSpell","itemConsumeId"),count=CombatDatabase.value(0,87,"ShockStunSpell","itemConsumeCount");
        var attempt=prepare(caster,controlled,stats,spell,System.nanoTime());long now;int hp,mp;RuntimeInventory previous=null,inventory;
        { // Candidate admission owns the player action lock; no caster monitor.
            now=System.nanoTime();var ready=(Map<Integer,Long>)CombatAccess.field(caster,"spellReadyNanos");hp=integer(caster,"playerHp");mp=integer(caster,"playerMp");
            if(!NativeShockCompanionAction.available(caster,target)||sx!=integer(caster,"positionX")||sy!=integer(caster,"positionY")||hp<=hpCost||mp<mpCost||now<ready.getOrDefault(87,0L))return true;
            previous=null;inventory=null;if(!NativeShockCompanionAction.admit(caster,target,spell))return true;previous=NativeCastMaterial.previous(caster);inventory=NativeCastMaterial.next(caster);now=System.nanoTime();
            CombatAccess.call(caster,"cancelMeditation","(Ljava/lang/String;)V","spell-87");hp-=hpCost;mp-=mpCost;CombatAccess.set(caster,"playerHp",hp);CombatAccess.set(caster,"playerMp",mp=NativeSkillConsumption.boundMp(caster,mp));CombatAccess.call(caster,"commitSpellReady","(IJ)V",87,now+spell.reuseDelayMillis()*1_000_000L);
        }
        if(previous!=null)CombatAccess.call(caster,"sendInventoryDelta","(L"+P+"RuntimeInventory;L"+P+"RuntimeInventory;)V",previous,inventory);
        int source=spell.baseDamage();for(int i=0;i<spell.diceCount();i++)source=Math.addExact(source,checkedRoll(roll,spell.dieSides()));
        int damage=CombatCompanionDamage.ordinaryComplete(caster,87,spell.attribute(),source,0,attempt.magicResistance(),0,"battle-Shock-Stun-companion="+actor.objectId(),controlled);
        if(controlled.earthBindActive(now))damage=0;
        int face=heading(sx,sy,actor.x(),actor.y());var recipients=hub.roomMembers(room);
        if((integer(caster,"playerHeading")&7)!=face){CombatAccess.set(caster,"playerHeading",face);byte[] packet=new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION).d(caster.playerObjectId()).c(face).bytes();for(var member:recipients)if(!(boolean)CombatAccess.field(caster,"invisible")||member==caster)send(member,packet,"S_CHANGE_DIRECTION controlled Shock Stun");}
        byte[] attack=Protocol.shockStunAttack(action,caster.playerObjectId(),actor.objectId(),damage,face),impact=Protocol.effectObject(actor.objectId(),spell.impactGfx());
        for(var member:recipients){if(!(boolean)CombatAccess.field(caster,"invisible")||member==caster)send(member,attack,"S_ATTACK controlled Shock Stun target="+actor.objectId()+" damage="+damage);send(member,impact,"S_EFFECT controlled Shock Stun target="+actor.objectId());}
        send(caster,Protocol.manaPoint(mp,integer(caster,"playerMaxMp")),"S_MANA_POINT controlled Shock Stun");if(hpCost!=0)send(caster,new Protocol.Writer().c(Protocol.S_HIT_POINT).h(hp).h(integer(caster,"playerMaxHp")).bytes(),"S_HIT_POINT controlled Shock Stun");
        int appliedDamage=NativeSourceControlledDisplay.invokeMagic(caster,"applyBattleSpellCompanionDamage","("+T+"II)I",new Object[]{target,damage,87});
        // Damaging attempts release target mutations only after native damage acceptance.
        // The source queues zero-damage target mutations directly after accepted cast costs.
        boolean accepted=appliedDamage>0||damage==0&&NativeShockCompanionAction.available(caster,target);
        if(accepted){controlled.consumeEraseMagic(attempt.eraseMagic());controlled.cancelSleep();}
        int percentile=checkedRoll(roll,100),duration=0;boolean stunned=false;
        if(accepted&&attempt.probability()>0&&percentile<=attempt.probability()&&NativeShockCompanionAction.available(caster,target)) {
            duration=spell.rollStunSeconds(sides->checkedRoll(roll,sides));var status=(BattleTimedStatus)CombatAccess.field(controlled,"timedStatus");
            synchronized(controlled){status.cancelShockStun();stunned=controlled.activateShockStun(now,duration,spell.maximumStunSeconds())!=null;}
            if(stunned){var placement=hub.placeShockStunEffect(room,actor.objectId(),actor.x(),actor.y(),now,duration);if(placement!=null)for(var member:recipients){if(placement.previous()!=null)send(member,Protocol.removeObject(placement.previous().objectId()),"S_REMOVE_OBJECT controlled Shock Stun effect="+placement.previous().objectId());send(member,Protocol.shockStunEffect(placement.placed()),"S_PUT_OBJECT controlled Shock Stun effect="+placement.placed().objectId()+" target="+actor.objectId()+" duration="+duration);}}
        }
        System.out.println("[LiTo] controlled BATTLE Shock Stun target="+actor.objectId()+" kind="+actor.kind()+" damage="+appliedDamage+" probability="+attempt.probability()+" roll="+percentile+" mr="+attempt.magicResistance()+" applied="+stunned+" seconds="+duration+" mp-cost="+mpCost+" hp-cost="+hpCost);
        return true;
    }
}
