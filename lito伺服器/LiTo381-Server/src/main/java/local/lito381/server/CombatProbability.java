package local.lito381.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Stateless bridge. Uses the actual loop target and pre-consumption MR operand. */
final class CombatProbability {
    private static final Map<String,List<String>> PARAMETERS=new ConcurrentHashMap<>();
    private static final String P="local/lito381/server/";
    static int dispatch(Object spell,String method,String descriptor,String parameterNames,Object[] args,
                        ClientSession caster,Object[] candidates) {
        var controls=CombatDatabase.controlsOrNull();
        int skill=CombatAccess.number(spell,"skillId");
        if(controls==null)
            return CombatAccess.integerCall(spell,method,descriptor,args);
        List<String> names=PARAMETERS.computeIfAbsent(parameterNames,key->List.of(key.split(",")));
        long now=System.nanoTime();
        var resolved=new Resolved();for(Object candidate:candidates)resolved.collect(candidate,0);
        Integer level=argument(names,args,"targetLevel","targetOwnerLevel","attackerLevel");
        Integer mr=argument(names,args,"targetMagicResistance","targetMr","attackerMagicResistance");
        if(!controls.probabilities().containsKey(skill)) {
            if(resolved.player!=null)return CombatAccess.integerCall(spell,method,descriptor,withGuardianMr(names,args,resolved.player));
            var target=resolved.target(caster,controls,level,mr,now);
            int modifier=target.boss()?controls.bossModifiers().getOrDefault(skill,-1):-1;
            if(modifier==0)return 0;
            Object[] ordinary=args.clone();
            for(String name:List.of("targetBoss","bossImmune","bossTarget")) {
                int at=names.indexOf(name);if(at>=0)ordinary[at]=false;
            }
            int result=CombatAccess.integerCall(spell,method,descriptor,ordinary);
            if(modifier>0)result=Math.min(90,result)/modifier;
            return Math.max(1,result);
        }
        var sourceCaster=sourceCaster(caster,now);
        SourceSkillControls.Target target=resolved.target(caster,controls,level,mr,now);
        return SourceSkillControls.chance(controls,skill,sourceCaster,target);
    }
    private static SourceSkillControls.Caster sourceCaster(ClientSession caster,long now) {
        var equipment=(SourceEquipmentMagicStats)CombatAccess.call(caster,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
        var summary=(CharacterSummary)CombatAccess.call(caster,"selectedCharacterSummary","()L"+P+"CharacterSummary;");
        if(summary==null)throw new IllegalStateException("configured probability has no selected caster");
        Object eye=CombatAccess.call(CombatAccess.field(caster,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
        int magicHit=Math.addExact(equipment.totalMagicHit(),CombatAccess.number(eye,"magicHit"));
        int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(caster,"baseIntelligence","(J)I",now));
        return new SourceSkillControls.Caster(summary.level(),caster.battleStrength(),caster.battleDexterity(),
                intelligence,CombatRules.magicLevel(summary.type(),summary.level()),magicHit,equipment.totalStunSuccess());
    }
    static BattleCounterBarrier.Decision counterDecision(ClientSession holder,int attackerLevel,int attackerMr,
            boolean attackerBoss,int roll,long now,ClientSession executing,Object[] candidates) {
        var controls=CombatDatabase.controlsOrNull();
        if(controls==null)return (BattleCounterBarrier.Decision)CombatAccess.call(holder,"counterBarrierDecision",
                "(IIZIJ)L"+P+"BattleCounterBarrier$Decision;",attackerLevel,attackerMr,attackerBoss,roll,now);
        var spell=BattleSpellCatalog.counterBarrierSpell(91);
        SourceWeaponPhysicalStats weapon;
        // Keep the existing lease/weapon transition atomic; never retain a
        // holder lock while reading another actor's synchronized status methods.
        synchronized(holder) {
            var ability=(BattleAbilityStatus)CombatAccess.field(holder,"abilityStatus");
            boolean active=ability.counterBarrier(now).remainingSeconds()!=0;
            weapon=(SourceWeaponPhysicalStats)CombatAccess.call(holder,"activePhysicalWeapon","()L"+P+"SourceWeaponPhysicalStats;");
            if(active&&!spell.supportsWeapon(weapon)) {
                ability.removeCounterBarrier();active=false;
                System.out.println("[LiTo] Counter Barrier removed player="+holder.playerName()+" reason=trigger-without-two-hand-sword");
            }
            if(!active||!spell.supportsWeapon(weapon))return BattleCounterBarrier.Decision.inactive();
            if(roll<1||roll>100)throw new IllegalArgumentException("Counter Barrier roll outside 1..100");
        }
        int probability=reflectionChance(91,holder,executing,candidates,attackerLevel,now);
        boolean triggered=probability>0&&roll<=probability;
        int damage=triggered?spell.reflectedSourceDamage(weapon):0;
        return new BattleCounterBarrier.Decision(true,probability,roll,triggered&&damage>0,damage);
    }
    private static int reflectionChance(int skill,ClientSession holder,ClientSession executing,Object[] candidates,
            int attackerLevel,long now) {
        var controls=CombatDatabase.controls();var resolved=new Resolved();
        for(Object candidate:candidates)if(candidate!=holder)resolved.collect(candidate,0);
        boolean npc=resolved.companion!=null||resolved.world!=null||resolved.quest!=null||resolved.time!=null||resolved.stats!=null;
        if(resolved.companion!=null)resolved.companionOwner=resolved.player!=null?resolved.player:executing;
        if(npc)resolved.player=null;
        if(!npc&&resolved.player==null&&executing!=holder)resolved.collect(executing,0);
        var target=resolved.target(holder,controls,attackerLevel,null,now);var caster=sourceCaster(holder,now);
        if(controls.probabilities().containsKey(skill))return SourceSkillControls.chance(controls,skill,caster,target);
        // L1MagicPc fallbacks share the level/original/equipment-hit terms.
        // Counter Barrier uses SQL base; Mortal Body uses ConfigSkill's base.
        int modifier=target.boss()?controls.bossModifiers().getOrDefault(skill,-1):-1;
        if(modifier==0)return 0;
        int base=skill==91?CombatDatabase.value(0,91,"CounterBarrierSpell","probabilityValue")
                :(int)CombatRules.number("MORTAL_BODY_ROM",23);
        long raw=(long)base+caster.level()-target.level()
                +2L*CombatAccess.integerCall(holder,"originalMagicHit","()I")+caster.magicHit();
        int probability=(int)Math.max(1,Math.min(90,raw));
        return modifier>0?Math.max(1,probability/modifier):probability;
    }
    static BattleMortalBody.Decision mortalDecision(BattleSpellCatalog.MortalBodySpell spell,boolean active,
            int holderLevel,int attackerLevel,boolean boss,int roll,boolean player,int enchant,
            int armor,int pvp,int melee,ClientSession holder,Object[] candidates) {
        if(CombatDatabase.controlsOrNull()==null)return BattleMortalBody.decide(spell,active,holderLevel,attackerLevel,boss,
                roll,player,enchant,armor,pvp,melee);
        if(!active)return BattleMortalBody.Decision.inactive();
        if(roll<1||roll>100||armor<0||pvp<0||melee<0)throw new IllegalArgumentException("invalid Mortal Body trigger input");
        int probability=reflectionChance(191,holder,holder,candidates,attackerLevel,System.nanoTime());
        if(probability<=0||roll>probability)return new BattleMortalBody.Decision(true,probability,roll,false,0,0,0);
        int base=Math.addExact(spell.baseReflectionDamage(),player?Math.max(0,enchant):0);
        int reduction=player?Math.addExact(armor,Math.addExact(pvp,melee)):0;
        return new BattleMortalBody.Decision(true,probability,roll,true,base,reduction,Math.max(1,base-reduction));
    }
    static BattleMortalBody.Decision npcMortalDecision(ClientSession holder,int attackerLevel,boolean boss,Object[] candidates) {
        if(CombatDatabase.controlsOrNull()==null)return (BattleMortalBody.Decision)CombatAccess.call(holder,"mortalBodyNpcDecision",
                "(IZ)L"+P+"BattleMortalBody$Decision;",attackerLevel,boss);
        var spell=BattleSpellCatalog.mortalBodySpell(191);
        var ability=(BattleAbilityStatus)CombatAccess.field(holder,"abilityStatus");
        boolean active=ability.mortalBody(System.nanoTime()).remainingSeconds()!=0;
        int roll=CombatAccess.integerCall(holder,"mortalBodyRoll","()I");
        return mortalDecision(spell,active,CombatAccess.integerCall(holder,"battleLevel","()I"),Math.max(1,attackerLevel),
                boss,roll,false,0,0,0,0,holder,candidates);
    }
    static boolean bossGate(boolean actualBoss,Object spell) {
        if(!actualBoss)return false;
        var controls=CombatDatabase.controlsOrNull();
        return controls==null || controls.bossModifiers().getOrDefault(CombatAccess.number(spell,"skillId"),-1)==0;
    }
    static boolean thunderSucceeds(BattleSpellCatalog.ThunderGrabSpell spell,int casterLevel,int targetLevel,
            int sustain,boolean boss,int roll,ClientSession caster,ClientSession target) {
        if(roll<1||roll>100)throw new IllegalArgumentException("Thunder Grab roll is outside percentile range");
        int chance=dispatch(spell,"successChancePercent","(IIIZ)I",
                "casterLevel,targetLevel,targetSustainResistance,bossTarget",
                new Object[]{casterLevel,targetLevel,sustain,boss},caster,new Object[]{target});
        return chance>0 && roll<=chance;
    }
    static int confusionChance(BattleSpellCatalog.ConfusionSpell spell,boolean boss,ClientSession caster,Object[] candidates) {
        return dispatch(spell,"chancePercent","(Z)I","bossTarget",new Object[]{boss},caster,candidates);
    }
    static boolean confusionSucceeds(BattleSpellCatalog.ConfusionSpell spell,boolean boss,int roll,
            ClientSession caster,Object[] candidates) {
        if(roll<1||roll>100)throw new IllegalArgumentException("Confusion roll is outside percentile range");
        int chance=confusionChance(spell,boss,caster,candidates);
        return chance>0&&roll<=chance;
    }
    static int npcThunderDuration(ClientSession caster,BattleSpellCatalog.ThunderGrabSpell spell,
            int casterLevel,int targetLevel,boolean boss,int rollFixture,int durationFixture,
            long targetId,String source,Object[] candidates) {
        if(CombatDatabase.controlsOrNull()==null)
            return CombatAccess.integerCall(caster,"rollNpcThunderGrabDuration",
                    "(L"+P+"BattleSpellCatalog$ThunderGrabSpell;IIZIIJLjava/lang/String;)I",
                    spell,casterLevel,targetLevel,boss,rollFixture,durationFixture,targetId,source);
        int chance=dispatch(spell,"successChancePercent","(IIIZ)I",
                "casterLevel,targetLevel,targetSustainResistance,bossTarget",
                new Object[]{casterLevel,Math.max(1,targetLevel),0,boss},caster,candidates);
        int roll=rollFixture>=1?rollFixture:ThreadLocalRandom.current().nextInt(1,101);
        if(roll<1||roll>100)throw new IllegalArgumentException("Thunder Grab roll is outside percentile range");
        if(chance<=0||roll>chance) {
            System.out.println("[LiTo] Thunder Grab NPC MOVE_STOP rejected target="+targetId+" source="+source
                    +" boss="+boss+" caster-level="+casterLevel+" target-level="+targetLevel+" probability="+chance+" roll="+roll);
            return 0;
        }
        int offset=durationFixture>=0?durationFixture:ThreadLocalRandom.current().nextInt(
                spell.maximumBindSeconds()-spell.minimumBindSeconds()+1);
        return spell.bindDurationSeconds(offset);
    }
    /** L1MagicMode.getTargetMr adds the PC target's lawful guardian MR to every probability MR term. */
    private static Object[] withGuardianMr(List<String> names,Object[] args,ClientSession player) {
        int bonus=CombatPlayerMagic.guardianMr(player.characterLawful());
        if(bonus==0)return args;
        for(String name:List.of("targetMagicResistance","targetMr","attackerMagicResistance")) {
            int index=names.indexOf(name);
            if(index<0)continue;
            Object[] copy=args.clone();copy[index]=Math.addExact(((Number)args[index]).intValue(),bonus);return copy;
        }
        return args;
    }
    private static Integer argument(List<String> names,Object[] args,String... aliases) {
        for(String name:aliases) {int index=names.indexOf(name);if(index>=0)return ((Number)args[index]).intValue();}
        return null;
    }
    static SourceSkillControls.Target playerTarget(ClientSession player,Integer requestedLevel,Integer requestedMr,long now) {
        var summary=(CharacterSummary)CombatAccess.call(player,"selectedCharacterSummary","()L"+P+"CharacterSummary;");
        if(summary==null)throw new IllegalStateException("configured probability has no selected target");
        var equipment=(SourceEquipmentMagicStats)CombatAccess.call(player,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
        var eye=CombatAccess.call(CombatAccess.field(player,"dragonEyeStatus"),"snapshot","(J)L"+P+"BattleDragonEyeStatus$Snapshot;",now);
        var resistances=Map.of(1,resistance((long)equipment.totalFreezeResistance()+CombatAccess.number(eye,"freezeResistance")),
                2,resistance((long)equipment.totalStoneResistance()+CombatAccess.number(eye,"stoneResistance")),
                3,resistance((long)equipment.totalSleepResistance()+CombatAccess.number(eye,"sleepResistance")),
                4,equipment.totalBlindResistance(),5,equipment.totalStunResistance(),6,equipment.totalSustainResistance());
        int mr=requestedMr==null?CombatAccess.integerCall(player,"effectiveBaseMagicResistance","(J)I",now):requestedMr;
        return new SourceSkillControls.Target(requestedLevel==null?summary.level():requestedLevel,
                Math.addExact(mr,CombatPlayerMagic.guardianMr(player.characterLawful())),
                CombatAccess.integerCall(player,"effectiveConstitution","(J)I",now),
                CombatAccess.integerCall(player,"effectiveWisdom","(J)I",now),resistances,false);
    }
    private static int resistance(long value) {return (int)Math.max(-128,Math.min(127,value));}
    private static final class Resolved {
        ClientSession player,companionOwner;
        ClassicWorldMonsterPopulation.Snapshot world;
        BattleQuestEncounter.Snapshot quest;
        BattleTimeAttackEncounter.Snapshot time;
        BattleCompanion.Snapshot companion;
        BattleCompanion controlled;
        SourceNpcPhysicalStats stats;
        boolean bot;
        void collect(Object value,int depth) {
            if(value==null||depth>5)return;
            if(value instanceof ClientSession session){player=session;return;}
            if(value instanceof ClassicWorldMonsterPopulation.Snapshot snapshot){world=snapshot;return;}
            if(value instanceof BattleQuestEncounter.Snapshot snapshot){quest=snapshot;return;}
            if(value instanceof BattleTimeAttackEncounter.Snapshot snapshot){time=snapshot;return;}
            if(value instanceof BattleCompanion.Snapshot snapshot){companion=snapshot;return;}
            if(value instanceof BattleCompanion actor){controlled=actor;companion=actor.snapshot();return;}
            if(value instanceof SourceNpcPhysicalStats snapshot){stats=snapshot;return;}
            // Only explicit target containers participate; their owner is not a PC target.
            if(!value.getClass().getName().startsWith(P.replace('/','.')+"ClientSession$"))return;
            Object owner=CombatAccess.get(value,"companionOwner");
            if(owner==null)owner=CombatAccess.get(value,"owner");
            if(owner instanceof ClientSession session)companionOwner=session;
            bot|=Boolean.TRUE.equals(CombatAccess.get(value,"bot"));
            Object kind=CombatAccess.get(value,"kind");if(kind!=null&&kind.toString().equals("ARENA_BOT"))bot=true;
            for(String field:List.of("player","world","quest","time","monster","identity","companion","controlled","snapshot","stats","npcStats"))
                collect(CombatAccess.get(value,field),depth+1);
        }
        SourceSkillControls.Target target(ClientSession caster,SourceSkillControls.Snapshot controls,Integer level,Integer mr,long now) {
            if(player!=null)return playerTarget(player,level,mr,now);
            var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();
            int npc=companion!=null?companion.npcId():world!=null?world.npcId():quest!=null?quest.npcId():time!=null?time.npcId():stats!=null?stats.npcId():0;
            if(npc==0 && (bot || world==null&&quest==null&&time==null&&companion==null&&stats==null)
                    &&NativeRoomRules.activeBot(hub,room))
                return new SourceSkillControls.Target(level==null?1:level,mr==null?0:mr,
                        (int)CombatRules.number("arena.bot.con",1),(int)CombatRules.number("arena.bot.wis",1),Map.of(),false);
            var attributes=controls.npcAttributes().get(npc);
            if(attributes==null)throw new IllegalStateException("configured probability has no qualified NPC attributes: npcid="+npc);
            int modifier=0;
            if(world!=null)modifier=hub.classicWorldMonsterPanicAbilityScoreModifier(world.mapId(),
                    ((Number)CombatAccess.field(caster,"classicWorldInstanceId")).intValue(),world.objectId(),now);
            else if(quest!=null)modifier=hub.questMonsterPanicAbilityScoreModifier(room,quest.objectId(),now);
            else if(time!=null)modifier=hub.timeAttackActorPanicAbilityScoreModifier(room,time.objectId(),now);
            else if(companion!=null) {
                if(controlled==null&&companionOwner!=null)controlled=(BattleCompanion)CombatAccess.call(companionOwner,"ownedCompanion","(J)L"+P+"BattleCompanion;",companion.objectId());
                if(controlled!=null)modifier=controlled.panicAbilityScoreModifier(now);
            }
            int targetMr=attributes.magicResistance();
            if(mr!=null)targetMr=mr;
            else if(world!=null)targetMr=hub.classicWorldMonsterEffectiveMagicResistance(world.mapId(),
                    ((Number)CombatAccess.field(caster,"classicWorldInstanceId")).intValue(),world.objectId(),targetMr,now);
            else if(quest!=null||time!=null)targetMr=CombatAccess.integerCall(caster,"effectiveNpcMagicResistance",
                    "(L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;IJ)I",quest,time,targetMr,now);
            else if(controlled!=null)targetMr=controlled.effectiveMagicResistance(targetMr,now);
            int targetLevel=level==null?companion!=null?companion.level():quest!=null?quest.level():time!=null?time.level():
                    world!=null?CombatAccess.number(world.template(),"level"):1:level;
            boolean boss=attributes.boss() || stats!=null&&stats.boss() || world!=null&&world.template().boss() || time!=null&&time.finalBoss();
            return new SourceSkillControls.Target(Math.max(1,targetLevel),targetMr,attributes.constitution(modifier),
                    attributes.wisdom(modifier),Map.of(),boss);
        }
    }
}
