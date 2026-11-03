package local.lito381.server;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Actual native cast plus actor-stat projection under altered immutable rows. */
public class CombatProbabilityRegression {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static Object get(Object o,String key)throws Exception{return NativeDirectoryRegression.get(o,key);}
    static void set(Object o,String key,Object value)throws Exception{NativeDirectoryRegression.set(o,key,value);}
    static <T>T copy(T value,Map<String,Object> overrides)throws Exception {
        var components=value.getClass().getRecordComponents();var types=new Class<?>[components.length];var args=new Object[components.length];
        for(int i=0;i<components.length;i++) {
            types[i]=components[i].getType();var method=components[i].getAccessor();method.setAccessible(true);
            args[i]=overrides.containsKey(components[i].getName())?overrides.get(components[i].getName()):method.invoke(value);
        }
        var constructor=value.getClass().getDeclaredConstructor(types);constructor.setAccessible(true);return (T)constructor.newInstance(args);
    }
    static SourceSkillControls.Probability row(double mr,double con,double wis) {
        return new SourceSkillControls.Probability(33,50,50,50,0,0,0,0,false,false,mr,con,wis);
    }
    static Object targetContainer(ClientSession player)throws Exception {
        Class<?> type=Class.forName("local.lito381.server.ClientSession$ArmorBreakTarget");
        var components=type.getRecordComponents();var types=new Class<?>[components.length];var args=new Object[components.length];
        for(int i=0;i<components.length;i++) {
            types[i]=components[i].getType();
            args[i]=types[i]==int.class?Integer.valueOf(0):types[i]==long.class?Long.valueOf(0):types[i]==boolean.class?Boolean.FALSE:null;
            if(components[i].getName().equals("player"))args[i]=player;
            if(components[i].getName().equals("objectId"))args[i]=player.playerObjectId();
        }
        var constructor=type.getDeclaredConstructor(types);constructor.setAccessible(true);return constructor.newInstance(args);
    }
    static void snapshot(Field field,CombatDatabase.Snapshot data,SourceSkillControls.Probability row,
                         Map<Integer,SourceSkillControls.NpcAttributes> npcs,Map<Integer,Integer> bosses)throws Exception {
        var probabilities=new HashMap<>(data.controls().probabilities());probabilities.put(row.skillId(),row);
        field.set(null,new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(
                probabilities,bosses,data.controls().abilities(),npcs,data.controls().skillCoexist(),data.controls().cancellationImmune(),data.controls().eyes(),data.controls().blessings())));
    }
    static void cast(ClientSession caster,ClientSession target)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();set(caster,"playerMp",1000);
        set(caster,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(40318L,20)));
        NativeDirectoryRegression.call(caster,"useSpell",new Class[]{byte[].class},
                new Protocol.Writer().c(0x14).c(4).c(0).d(target.playerObjectId()).h(32741).h(32798).bytes());
    }
    static int bridge(ClientSession caster,Object target,int level,int mr) {
        return CombatProbability.dispatch(BattleSpellCatalog.paralysisSpell(33),"probability","(IIIIIII)I",
                "casterLevel,targetLevel,effectiveIntelligence,magicLevel,magicHit,targetMagicResistance,targetStoneResistance",
                new Object[]{50,level,18,0,0,mr,0},caster,new Object[]{target});
    }
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("curseParalysisFixturePercentileRoll",30,"confusionFixtureD100",30,
                "counterBarrierFixtureD100",30,"mortalBodyFixtureD100",30,"uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1));
        ResourceDatabase.load(config);
        Field field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);
        var data=(CombatDatabase.Snapshot)field.get(null);var npcs=new HashMap<>(data.controls().npcAttributes());
        Field rulesField=CombatRules.class.getDeclaredField("current");rulesField.setAccessible(true);
        var originalRules=(CombatRules.Snapshot)rulesField.get(null);
        var bosses=new HashMap<>(data.controls().bossModifiers());bosses.put(33,1);
        Path root=Path.of(args[1]);Files.createDirectories(root);
        var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var p=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();
            check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"native clock stopped for isolated probability casts");
            var fixtures=List.of(f,p,mate,other);
            for(int i=0;i<4;i++) {
                var fixture=fixtures.get(i);NativeDirectoryRegression.init(fixture,"Probability"+i,0,config.classicServerNames().get(0));
                var character=((List<CharacterSummary>)get(fixture.session,"characters")).get(0);
                set(fixture.session,"characters",List.of(copy(character,Map.of("objectId",i==1?0xf8100002L:0x38100001L+i))));
            }
            long room=FlowRegression.setup(hub,f.session,p.session,mate.session,other.session);
            for(int i=0;i<4;i++) {
                set(fixtures.get(i).session,"positionX",32740+i);set(fixtures.get(i).session,"positionY",32798);
                set(fixtures.get(i).session,"playerHp",1000);set(fixtures.get(i).session,"playerMaxHp",1000);
                set(fixtures.get(i).session,"playerMaxMp",1000);
            }
            set(f.session,"learnedSkills",Set.of(33,37,202));
            var status=(BattleTimedStatus)get(p.session,"timedStatus");
            snapshot(field,data,row(0,2,2),npcs,bosses);
            check(bridge(f.session,p.session,50,36)==38,"actual target CON/WIS used");
            cast(f.session,p.session);check(status.paralysisVisualActive(System.nanoTime()),"configured probability 38 did not beat fixed roll30");
            check(((RuntimeInventory)get(f.session,"inventory")).countCatalog(40318)==18,"actual negative material consumption");
            ((Map<?,?>)get(f.session,"spellReadyNanos")).clear();
            NativeDirectoryRegression.call(f.session,"useSpell",new Class[]{byte[].class},
                    new Protocol.Writer().c(0x14).c(4).c(4).d(p.session.playerObjectId()).bytes());
            var lease=CombatPoisonLifecycle.curseState(status,false);
            check(!status.paralysisVisualActive(System.nanoTime())&&(lease==null||lease.current()==null),"actual native cure37 retires the preceding curse lease");
            NativeDirectoryRegression.call(p.session,"serverExpireTimedStatuses",new Class[]{long.class},System.nanoTime()+30_000_000_000L);
            check(!status.paralysisVisualActive(System.nanoTime()),"later native clock cannot reactivate a cured first cast");
            var original=((List<CharacterSummary>)get(p.session,"characters")).get(0);
            set(p.session,"characters",List.of(copy(original,Map.of("constitution",40,"wisdom",40))));
            check(bridge(f.session,p.session,50,36)==10,"changed actual target CON/WIS changed chance");
            cast(f.session,p.session);check(!status.paralysisVisualActive(System.nanoTime()),"configured probability10 unexpectedly beat fixed roll30");
            check((int)get(f.session,"playerMp")<1000,"failed source chance did not consume MP");
            check(((RuntimeInventory)get(f.session,"inventory")).countCatalog(40318)==18,"failed source chance did not consume materials");
            set(p.session,"characters",List.of(original));
            snapshot(field,data,row(.2,2,2),npcs,bosses);
            check(bridge(f.session,p.session,50,99)==18,"pre-consumption MR operand preserved instead of reread");
            check(bridge(f.session,targetContainer(p.session),50,99)==18,"private target container projects actual player without retaining owner");
            var thunder=BattleSpellCatalog.thunderGrabSpell(192);
            var thunderRow=new SourceSkillControls.Probability(192,50,50,50,0,0,0,0,false,false,0,2,2);
            snapshot(field,data,thunderRow,npcs,bosses);
            Object applied=NativeDirectoryRegression.call(f.session,"applyPlayerThunderGrab",
                    new Class[]{ClientSession.class,thunder.getClass(),int.class,long.class,int.class,int.class,String.class},
                    p.session,thunder,1,System.nanoTime(),30,0,"probability-regression");
            check(Boolean.TRUE.equals(applied)&&status.thunderGrabActive(System.nanoTime()),"configured thunder chance38 controls actual success despite legacy lower-level chance15");
            status.cancelThunderGrab();
            set(p.session,"characters",List.of(copy(original,Map.of("constitution",40,"wisdom",40))));
            applied=NativeDirectoryRegression.call(f.session,"applyPlayerThunderGrab",
                    new Class[]{ClientSession.class,thunder.getClass(),int.class,long.class,int.class,int.class,String.class},
                    p.session,thunder,100,System.nanoTime(),30,0,"probability-regression");
            check(Boolean.FALSE.equals(applied)&&!status.thunderGrabActive(System.nanoTime()),"configured thunder chance10 rejects actual success despite legacy higher-level chance50");
            set(p.session,"characters",List.of(original));
            var confusionRow=new SourceSkillControls.Probability(202,50,50,50,0,0,0,0,false,false,0,2,2);
            snapshot(field,data,confusionRow,npcs,bosses);
            ((Map<?,?>)get(f.session,"spellReadyNanos")).clear();set(f.session,"playerMp",1000);
            NativeDirectoryRegression.call(f.session,"useSpell",new Class[]{byte[].class},
                    new Protocol.Writer().c(0x14).c(25).c(1).d(p.session.playerObjectId()).h(32741).h(32798).bytes());
            check(status.silenceActive(System.nanoTime()),"configured confusion chance38 beats roll30 in actual attack dispatch despite legacy25");
            status.expireSilence(Long.MAX_VALUE);
            set(p.session,"characters",List.of(copy(original,Map.of("constitution",40,"wisdom",40))));
            ((Map<?,?>)get(f.session,"spellReadyNanos")).clear();set(f.session,"playerMp",1000);
            NativeDirectoryRegression.call(f.session,"useSpell",new Class[]{byte[].class},
                    new Protocol.Writer().c(0x14).c(25).c(1).d(p.session.playerObjectId()).h(32741).h(32798).bytes());
            check(!status.silenceActive(System.nanoTime()),"configured confusion chance10 rejects roll30 in actual attack dispatch");
            set(p.session,"characters",List.of(original));
            int npc=45005;check(npcs.containsKey(npc),"qualified common NPC fixture");
            npcs.put(npc,new SourceSkillControls.NpcAttributes(20,24,80,false));
            snapshot(field,data,row(0,2,4),npcs,bosses);
            check(bridge(f.session,SourceNpcPhysicalStats.memoryFixture(npc),1,35)==34,"actual NPC CON/WIS snapshot projection");
            npcs.put(npc,new SourceSkillControls.NpcAttributes(20,24,80,true));bosses.put(33,10);
            snapshot(field,data,row(0,2,4),npcs,bosses);
            check(bridge(f.session,SourceNpcPhysicalStats.memoryFixture(npc),1,35)==3,"actual NPC boss modifier projection");
            bosses.put(33,0);snapshot(field,data,row(0,2,4),npcs,bosses);
            check(bridge(f.session,SourceNpcPhysicalStats.memoryFixture(npc),1,35)==0,"actual NPC boss immunity projection");
            bosses.put(192,10);snapshot(field,data,thunderRow,npcs,bosses);
            check(CombatProbability.npcThunderDuration(f.session,thunder,1,1,true,2,0,123,"probability-regression",
                    new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)})==thunder.minimumBindSeconds(),"configured NPC thunder boss divisor allows fixed roll2");
            check(CombatProbability.npcThunderDuration(f.session,thunder,1,1,true,3,0,123,"probability-regression",
                    new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)})==0,"configured NPC thunder boss divisor rejects fixed roll3");
            bosses.put(192,0);snapshot(field,data,thunderRow,npcs,bosses);
            check(CombatProbability.npcThunderDuration(f.session,thunder,1,1,true,1,0,123,"probability-regression",
                    new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)})==0,"configured NPC thunder immunity precedes minimum chance");
            var companion=(BattleCompanion)get(f.session,"battleCompanion");companion.resetForBattle(32741,32798);
            int companionNpc=companion.snapshot().npcId();npcs.put(companionNpc,new SourceSkillControls.NpcAttributes(20,24,80,false));
            bosses.put(33,1);snapshot(field,data,row(0,1,1),npcs,bosses);
            check(bridge(f.session,companion,50,35)==6,"actual companion projection");
            companion.activatePanic(System.nanoTime(),20,-1);
            check(bridge(f.session,companion,50,35)==8,"companion panic adjusts CON/WIS before source probability");
            companion.cancelPanic();
            var holderAbility=(BattleAbilityStatus)get(p.session,"abilityStatus");
            Object holderEquipment=get(p.session,"sourceEquipmentMagicStats"),attackerEquipment=get(f.session,"sourceEquipmentMagicStats");
            var attackerOriginal=((List<CharacterSummary>)get(f.session,"characters")).get(0);
            set(p.session,"sourceEquipmentMagicStats",accounts.equipmentMagicStats(20));
            set(f.session,"sourceEquipmentMagicStats",accounts.equipmentMagicStats(20));
            set(p.session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
            set(f.session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
            holderAbility.activateCounterBarrier(System.nanoTime(),64);
            var counterRow=new SourceSkillControls.Probability(91,50,50,50,0,0,0,0,false,false,0,2,2);
            snapshot(field,data,counterRow,npcs,bosses);
            var decision=CombatProbability.counterDecision(p.session,50,99,false,30,System.nanoTime(),f.session,new Object[0]);
            check(decision.triggered()&&decision.probability()==38&&decision.sourceDamage()>0,"counter holder is caster and actual player attacker is target");
            int holderHp=(int)get(p.session,"playerHp"),attackerHp=(int)get(f.session,"playerHp");
            NativeDirectoryRegression.call(f.session,"handle",new Class[]{byte[].class},
                    new Protocol.Writer().c(0x17).d(p.session.playerObjectId()).h(32741).h(32798).bytes());
            check((int)get(p.session,"playerHp")==holderHp&&(int)get(f.session,"playerHp")<attackerHp,
                    "actual native melee triggers configured counter and suppresses incoming strike");
            set(f.session,"characters",List.of(copy(attackerOriginal,Map.of("constitution",40,"wisdom",40))));
            decision=CombatProbability.counterDecision(p.session,50,99,false,30,System.nanoTime(),f.session,new Object[0]);
            check(!decision.triggered()&&decision.probability()==10,"counter responds to actual attacker CON/WIS rather than holder");
            set(f.session,"characters",List.of(attackerOriginal));
            var counterPanicRow=new SourceSkillControls.Probability(91,50,50,50,0,0,0,0,false,false,0,1,1);
            snapshot(field,data,counterPanicRow,npcs,bosses);
            decision=CombatProbability.counterDecision(p.session,50,99,false,7,System.nanoTime(),f.session,
                    new Object[]{companion.snapshot(),p.session});
            check(!decision.triggered()&&decision.probability()==6,"counter companion attacker is separate from holder and owner");
            companion.activatePanic(System.nanoTime(),20,-1);
            decision=CombatProbability.counterDecision(p.session,50,99,false,7,System.nanoTime(),f.session,
                    new Object[]{companion.snapshot(),p.session});
            check(decision.triggered()&&decision.probability()==8,"counter companion resolves owner to live Panic-adjusted NPC stats");
            companion.cancelPanic();
            bosses.put(91,10);snapshot(field,data,counterRow,npcs,bosses);
            decision=CombatProbability.counterDecision(p.session,1,99,true,2,System.nanoTime(),p.session,
                    new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)});
            check(decision.triggered()&&decision.probability()==2,"counter NPC boss divisor overrides stale hard immune gate");
            bosses.put(91,0);snapshot(field,data,counterRow,npcs,bosses);
            decision=CombatProbability.counterDecision(p.session,1,99,true,1,System.nanoTime(),p.session,
                    new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)});
            check(!decision.triggered()&&decision.probability()==0,"counter NPC boss immunity");
            var noCounterRow=new HashMap<>(data.controls().probabilities());noCounterRow.remove(91);
            field.set(null,new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(noCounterRow,bosses,data.controls().abilities(),npcs,data.controls().skillCoexist(),data.controls().cancellationImmune(),data.controls().eyes(),data.controls().blessings())));
            int fallback=Math.min(90,Math.max(1,data.skills().get(91).probabilityValue()
                    +2*CombatAccess.integerCall(p.session,"originalMagicHit","()I")
                    +((SourceEquipmentMagicStats)get(p.session,"sourceEquipmentMagicStats")).totalMagicHit()));
            decision=CombatProbability.counterDecision(p.session,50,999,false,100,System.nanoTime(),f.session,new Object[0]);
            check(decision.probability()==fallback,"counter missing-row fallback uses SQL base, original hit and equip hit with no MR term");
            set(p.session,"sourceEquipmentMagicStats",holderEquipment);set(f.session,"sourceEquipmentMagicStats",attackerEquipment);
            holderAbility.activateCounterBarrier(System.nanoTime(),64);
            decision=CombatProbability.counterDecision(p.session,50,0,false,1,System.nanoTime(),f.session,new Object[0]);
            check(!decision.active()&&holderAbility.counterBarrier(System.nanoTime()).remainingSeconds()==0,"counter still removes lease when two-hand sword is absent");
            var mortalRow=new SourceSkillControls.Probability(191,50,50,50,0,0,0,0,false,false,0,2,2);
            snapshot(field,data,mortalRow,npcs,bosses);holderAbility.activateMortalBody(System.nanoTime(),300);
            holderHp=(int)get(p.session,"playerHp");attackerHp=(int)get(f.session,"playerHp");
            NativeDirectoryRegression.call(p.session,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},10,f.session,false);
            check((int)get(p.session,"playerHp")==holderHp-10&&(int)get(f.session,"playerHp")==attackerHp-20,
                    "actual positive native damage commits configured mortal reflection without counter suppression");
            set(f.session,"characters",List.of(copy(attackerOriginal,Map.of("constitution",40,"wisdom",40))));
            attackerHp=(int)get(f.session,"playerHp");
            NativeDirectoryRegression.call(p.session,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},10,f.session,false);
            check((int)get(f.session,"playerHp")==attackerHp,"mortal actual damage path rejects chance10 roll30 using attacker CON/WIS");
            set(f.session,"characters",List.of(attackerOriginal));
            var mortal=BattleSpellCatalog.mortalBodySpell(191);
            var mortalBossRow=new SourceSkillControls.Probability(191,90,90,90,0,0,0,0,false,false,0,0,0);
            bosses.put(191,2);snapshot(field,data,mortalBossRow,npcs,bosses);
            var mortalDecision=CombatProbability.npcMortalDecision(p.session,1,true,new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)});
            check(mortalDecision.triggered()&&mortalDecision.probability()==45&&mortalDecision.reflectedDamage()==20,"mortal NPC boss divisor permits reflection with source base damage");
            bosses.put(191,0);snapshot(field,data,mortalBossRow,npcs,bosses);
            mortalDecision=CombatProbability.npcMortalDecision(p.session,1,true,new Object[]{SourceNpcPhysicalStats.memoryFixture(npc)});
            check(!mortalDecision.triggered()&&mortalDecision.probability()==0,"mortal NPC boss immunity is evaluated before chance floor");
            var withoutMortal=new HashMap<>(data.controls().probabilities());withoutMortal.remove(191);
            field.set(null,new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(withoutMortal,bosses,data.controls().abilities(),npcs,data.controls().skillCoexist(),data.controls().cancellationImmune(),data.controls().eyes(),data.controls().blessings())));
            var alteredMortalRules=new HashMap<>(originalRules.values());alteredMortalRules.put("MORTAL_BODY_ROM",60.0);alteredMortalRules.put("MORTAL_BODY_DMG",13.0);
            rulesField.set(null,new CombatRules.Snapshot(alteredMortalRules,originalRules.skillFile(),originalRules.damageFile()));
            mortalDecision=CombatProbability.mortalDecision(mortal,true,50,50,false,30,true,7,2,3,4,p.session,new Object[]{f.session});
            check(mortalDecision.triggered()&&mortalDecision.probability()>=60&&mortalDecision.baseDamage()==20
                    &&mortalDecision.totalReduction()==9&&mortalDecision.reflectedDamage()==11,"mortal missing-row config chance, changed damage, weapon enchant and reduction ordering");
            alteredMortalRules.put("MORTAL_BODY_ROM",0.0);rulesField.set(null,new CombatRules.Snapshot(alteredMortalRules,originalRules.skillFile(),originalRules.damageFile()));
            mortalDecision=CombatProbability.mortalDecision(mortal,true,50,50,false,30,true,0,0,0,0,p.session,new Object[]{f.session});
            check(!mortalDecision.triggered()&&mortalDecision.probability()<30,"mortal missing-row altered config chance changes actual decision");
            rulesField.set(null,originalRules);holderAbility.removeMortalBody();
            snapshot(field,data,row(0,1,1),npcs,bosses);
            hub.endBattle(room);for(var fixture:fixtures)hub.leaveRoom(room,fixture.session);
            long botRoom=hub.roomSnapshots(0).get(0).id();check(hub.tryJoinRoom(botRoom,f.session).joined(),"ArenaBot fixture test-room join");
            set(f.session,"currentRoomId",botRoom);
            check(bridge(f.session,null,1,0)==48,"explicit ArenaBot CON/WIS defaults use source lower bound");
            var alteredRules=new HashMap<>(originalRules.values());alteredRules.put("arena.bot.con",10.0);alteredRules.put("arena.bot.wis",20.0);
            rulesField.set(null,new CombatRules.Snapshot(alteredRules,originalRules.skillFile(),originalRules.damageFile()));
            check(bridge(f.session,null,1,0)==20,"changed ArenaBot CON/WIS controls reach source chance");
            hub.leaveRoom(botRoom,f.session);
            System.out.println("PASS actual configured negative cast/actor checks="+checks+"; fixed roll30 flips with target CON/WIS; MP/material semantics; pre-consumption MR; NPC/boss and companion/Panic projection; remaining actor paths still require acceptance");
        } finally {field.set(null,data);rulesField.set(null,originalRules);}
    }
}
