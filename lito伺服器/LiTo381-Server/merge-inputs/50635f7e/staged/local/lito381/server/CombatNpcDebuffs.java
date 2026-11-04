package local.lito381.server;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Source target policy, native owned negative states and NPC healing; no retained actor/cast references. */
final class CombatNpcDebuffs {
    private static final String P="local/lito381/server/",B="L"+P+"ClientSession$MassSlowCompanionTarget;";
    record Focus(Object handle,ClientSession owner,BattleCompanion controlled,BattleCompanion.Snapshot actor,SourceNpcPhysicalStats stats,boolean world) {}
    static Object armorTarget(Object target,ClientSession caster){
        if(!CombatRules.loaded()||target==null)return target;var player=(ClientSession)CombatAccess.get(target,"player");if(player==null)return null;
        long selected=((Number)CombatAccess.field(caster,"lastCombatTargetObjectId")).longValue();if(selected==0||selected==player.playerObjectId())return target;
        var hub=(GameHub)CombatAccess.field(caster,"hub");var owners=CombatWorldPhysical.world(caster)?(List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",integer(caster,"classicWorldMapId")):hub.roomCombatants(((Number)CombatAccess.field(caster,"currentRoomId")).longValue());
        for(var owner:owners)if(CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",selected)!=null)return null;return target;
    }
    static boolean armorActive(boolean active,ClientSession player){return active&&(!CombatRules.loaded()||player!=null);}
    static BattleTimedStatus timed(BattleCompanion actor){return (BattleTimedStatus)CombatAccess.field(actor,"timedStatus");}
    static boolean strikerActive(boolean legacy,Object target){return legacy||CombatRules.loaded()&&target!=null&&timed(CombatCompanionDamage.controlled(target)).strikerGaleActive(System.nanoTime());}
    static void cancel(BattleCompanion actor){var controls=CombatDatabase.controlsOrNull();if(controls==null)return;var status=timed(actor);if(!controls.cancellationImmune().contains(174))status.expireStrikerGale(Long.MAX_VALUE);if(!controls.cancellationImmune().contains(173))status.expirePolluteWater(Long.MAX_VALUE);}
    static int healingOwned(int amount,BattleCompanion actor,long now){if(actor==null)return amount;var status=timed(actor);return BattleHealingRecovery.adjustedAmount(amount,status.polluteWaterActive(now),status.waterDragonDirtyCurrentActive(now),status.waterDragonHealingErosionActive(now));}
    static int healingBattle(int amount,Object companion,long now){
        return !CombatRules.loaded()||companion==null?amount:healingOwned(amount,CombatCompanionDamage.controlled(companion),now);
    }
    static int healingWorld(int amount,ClientSession owner,BattleCompanion.Snapshot companion,long now){
        return !CombatRules.loaded()||owner==null||companion==null?amount:healingOwned(amount,(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",companion.objectId()),now);
    }
    static int integer(ClientSession actor,String field){return CombatCompanionSpells.integer(actor,field);}
    static boolean rejected(ClientSession caster,int message,String label){CombatCompanionSpells.send(caster,Protocol.messageCode(message),"S_MESSAGE_CODE owned "+label+" rejected="+message);return true;}
    static Focus resolve(ClientSession caster,Protocol.AttackSpellRequest request) {
        boolean world=CombatWorldPhysical.world(caster);Object target=world?CombatWorldPhysical.prepare(null,caster,new Protocol.AttackRequest(request.targetObjectId(),request.x(),request.y())):CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)"+B,request.targetObjectId());
        if(target==null)return null;
        return new Focus(target,(ClientSession)CombatAccess.get(target,"owner"),CombatCompanionDamage.controlled(target),(BattleCompanion.Snapshot)CombatAccess.get(target,"snapshot"),(SourceNpcPhysicalStats)CombatAccess.get(target,"stats"),world);
    }
    static boolean available(ClientSession caster,Focus focus){return focus.world()?CombatWorldPhysical.available(caster,(CombatWorldPhysical.Target)focus.handle()):CombatCompanionSpells.available(caster,focus.handle());}
    static CombatCompanionSpells.Attempt prepare(ClientSession caster,Focus focus,BattleSpellCatalog.TargetNegativeStatusSpell spell,long now) {
        int skill=spell.skillId();String family=skill==173?"PolluteWaterSpell":"StrikerGaleSpell";var controlled=focus.controlled();var lease=controlled.captureEraseMagic(now);int mr=lease.present()?focus.stats().magicResistance()>>2:focus.stats().magicResistance();if(controlled.earthBindActive(now))return new CombatCompanionSpells.Attempt(mr,0,lease);
        var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");var eye=(BattleDragonEyeStatus.Snapshot)CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);int hit=Math.addExact(equipment.totalMagicHit(),eye.magicHit());int chance;
        var controls=CombatDatabase.controls();
        if(controls.probabilities().containsKey(skill))chance=CombatProbability.dispatch(spell,"probabilityForTarget","(IIIIIIZ)I","casterLevel,targetLevel,effectiveIntelligence,magicLevel,magicHit,targetMagicResistance,targetBoss",new Object[]{summary.level(),focus.actor().level(),equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","(J)I",now)),CombatRules.magicLevel(summary.type(),summary.level()),hit,mr,focus.stats().boss()},caster,new Object[]{controlled});
        else {
            int dice=CombatDatabase.value(0,skill,family,"probabilityDice"),base=CombatDatabase.value(0,skill,family,"probabilityValue");int raw=(int)((dice/10D)*(summary.level()-focus.actor().level()))+base+2*CombatAccess.integerCall(caster,"originalMagicHit","()I")+hit;
            if(skill==173)raw-=mr/80;var row=controls.npcAttributes().get(focus.actor().npcId());if(row==null)throw new IllegalStateException("missing source negative-status NPC attributes");int divisor=row.boss()?controls.bossModifiers().getOrDefault(skill,-1):-1;chance=divisor==0?0:Math.max(1,divisor>0?Math.min(90,raw)/divisor:Math.min(90,raw));
        }
        return new CombatCompanionSpells.Attempt(mr,chance,lease);
    }
    static boolean negative(ClientSession caster,byte[] payload,BattleSpellCatalog.TargetNegativeStatusSpell spell,int fixture) {
        return NativeControlledNegativeMaterial.negative(caster,payload,spell,fixture);
    }
    static boolean nativeMaterialControlledNegativeBody(ClientSession caster,byte[] payload,BattleSpellCatalog.TargetNegativeStatusSpell spell,int fixture,Focus focus,boolean admin) {
        int skill=spell.skillId();if(!CombatRules.loaded()||skill!=173&&skill!=174)return false;String family=skill==173?"PolluteWaterSpell":"StrikerGaleSpell",label=skill==173?"Pollute Water":"Striker Gale";
        Protocol.AttackSpellRequest request=Protocol.attackSpellRequest(payload);
        Focus focus2=focus;if(focus2==null)return false;
        long requestNow=System.nanoTime();int sx=integer(caster,"positionX"),sy=integer(caster,"positionY"),map=integer(caster,"classicWorldMapId"),instance=integer(caster,"classicWorldInstanceId");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();var hub=(GameHub)CombatAccess.field(caster,"hub");
        if(fixture<0||fixture>100)throw new IllegalArgumentException("invalid owned negative-status roll fixture");
        var polymorph=(BattlePolymorphCatalog.Form)CombatAccess.call(CombatAccess.field(caster,"polymorphStatus"),"active","(J)L"+P+"BattlePolymorphCatalog$Form;",requestNow);var status=(BattleTimedStatus)CombatAccess.field(caster,"timedStatus");
        if((integer(caster,"elfAttribute")&spell.attribute())==0&&!admin||polymorph!=null&&!polymorph.skillUsable()||status.silenceActive(requestNow)||CombatAccess.integerCall(caster,"currentWeight240","(J)I",requestNow)>=BattleInventoryWeight.BUFFED_REGEN_LIMIT||!((Set<Integer>)CombatAccess.field(caster,"learnedSkills")).contains(skill)||!BattleAnimationCatalog.get().supportsTimedAction(caster.battleGfx(),spell.actionId()))return true;
        var actor=focus.actor();int x=actor.x(),y=actor.y();boolean geometry;
        if(focus.world()){var worldMap=ClassicWorldMapCatalog.forMap(map);geometry=!worldMap.isSafetyZone(sx,sy)&&!worldMap.isSafetyZone(x,y)&&worldMap.attackPosition(sx,sy,x,y,spell.maximumRange());}
        else geometry=(focus.owner()==caster||ForcedCombat.canAttackTarget(hub,room,caster,focus.owner(),actor.objectId()))&&BattleSpellCatalog.hasNativeLineOfSight(ArenaMapCatalog.forMap(hub.battleMapId(room,caster)),sx,sy,x,y);
        if(!geometry||!NativeControlledNegativeMaterial.available(caster,focus)||Math.max(Math.abs(sx-x),Math.abs(sy-y))>spell.maximumRange()||!BattleSpellCatalog.isInNativeScreen(sx,sy,x,y))return true;
        var attempt=prepare(caster,focus,spell,requestNow);var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");int mpCost=spell.effectiveMpCost(BattleSpellCatalog.originalMagicConsumeReduction(summary.type(),summary.originalIntelligence())),hpCost=CombatDatabase.value(0,skill,family,"hpCost"),material=CombatDatabase.value(0,skill,family,"itemConsumeId"),count=CombatDatabase.value(0,skill,family,"itemConsumeCount");
        if(!NativeControlledNegativeMaterial.available(caster,focus))return true;
        RuntimeInventory previous,next;long now;int hp,mp;
        synchronized(caster) {
            now=System.nanoTime();hp=integer(caster,"playerHp");mp=integer(caster,"playerMp");var ready=(Map<Integer,Long>)CombatAccess.field(caster,"spellReadyNanos");
            boolean sameContext=focus.world()?CombatWorldPhysical.world(caster)&&integer(caster,"classicWorldMapId")==map&&integer(caster,"classicWorldInstanceId")==instance:((Enum<?>)CombatAccess.field(caster,"phase")).name().equals("BATTLE")&&((Number)CombatAccess.field(caster,"currentRoomId")).longValue()==room;
            if(!sameContext||(boolean)CombatAccess.field(caster,"observer")||hp<=0||now<ready.getOrDefault(skill,0L)||integer(caster,"positionX")!=sx||integer(caster,"positionY")!=sy)return true;
            if(hp<=hpCost)return rejected(caster,279,label);if(mp<mpCost)return rejected(caster,278,label);
            previous=(RuntimeInventory)CombatAccess.field(caster,"inventory");next=previous.copy();if(!CombatDatabase.consumeMaterial(next,material,count))return rejected(caster,299,label);
            CombatAccess.call(caster,"cancelMeditation","(Ljava/lang/String;)V","spell-"+skill);hp-=hpCost;mp-=mpCost;CombatAccess.set(caster,"inventory",next);CombatAccess.set(caster,"playerHp",hp);CombatAccess.set(caster,"playerMp",mp);CombatAccess.call(caster,"commitSpellReady","(IJ)V",skill,now+spell.reuseDelayMillis()*1_000_000L);
        }
        CombatAccess.call(caster,"revealInvisibility","(Ljava/lang/String;)V","spell-"+skill);CombatAccess.call(caster,"cancelAbsoluteBarrier","(Ljava/lang/String;)V","spell-"+skill);CombatAccess.call(caster,"sendInventoryDelta","(L"+P+"RuntimeInventory;L"+P+"RuntimeInventory;)V",previous,next);
        boolean accepted=NativeControlledNegativeMaterial.available(caster,focus),applied=false;int roll=fixture>0?fixture:ThreadLocalRandom.current().nextInt(1,101);
        if(accepted){focus.controlled().consumeEraseMagic(attempt.eraseMagic());focus.controlled().cancelSleep();if(attempt.probability()>0&&attempt.probability()>=roll)synchronized(focus.controlled()){var current=focus.controlled().snapshot();if(current.active()&&current.hp()>0&&current.x()==x&&current.y()==y){if(skill==173)timed(focus.controlled()).activatePolluteWater(now,spell.durationSeconds());else timed(focus.controlled()).activateStrikerGale(now,spell.durationSeconds());applied=true;}}}
        var recipients=focus.world()?(List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",map):hub.roomMembers(room);byte[] action=Protocol.objectAction(caster.playerObjectId(),spell.actionId()),effect=Protocol.effectObject(actor.objectId(),spell.effectGfx());
        for(var member:recipients){if(focus.world()&&!BattleSpellCatalog.isInNativeScreen(integer(member,"positionX"),integer(member,"positionY"),sx,sy)&&!BattleSpellCatalog.isInNativeScreen(integer(member,"positionX"),integer(member,"positionY"),x,y))continue;CombatCompanionSpells.send(member,action,"S_ACTION owned "+label+" caster="+caster.playerObjectId());if(applied)CombatCompanionSpells.send(member,effect,"S_EFFECT owned "+label+" target="+actor.objectId()+" gfx="+spell.effectGfx());}
        CombatCompanionSpells.send(caster,Protocol.manaPoint(mp,integer(caster,"playerMaxMp")),"S_MANA_POINT owned "+label+" target="+actor.objectId()+" world="+focus.world()+" chance="+attempt.probability()+" roll="+roll+" applied="+applied);if(hpCost!=0)CombatCompanionSpells.send(caster,new Protocol.Writer().c(Protocol.S_HIT_POINT).h(hp).h(integer(caster,"playerMaxHp")).bytes(),"S_HIT_POINT owned "+label);if(!applied)CombatCompanionSpells.send(caster,Protocol.messageCode(spell.failureMessage()),"S_MESSAGE_CODE owned "+label+" failure");
        System.out.println("[LiTo] owned "+label+" target="+actor.objectId()+" world="+focus.world()+" chance="+attempt.probability()+" roll="+roll+" applied="+applied+" mr="+attempt.magicResistance()+" duration="+spell.durationSeconds());return true;
    }
}
