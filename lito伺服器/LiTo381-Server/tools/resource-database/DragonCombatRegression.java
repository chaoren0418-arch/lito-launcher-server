package local.lito381.server;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Source phase boundaries and real native Foe Slayer casts, with no SQL writes. */
public class DragonCombatRegression {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Object get(Object o,String name)throws Exception{return NativeDirectoryRegression.get(o,name);}
    static void set(Object o,String name,Object value)throws Exception{NativeDirectoryRegression.set(o,name,value);}
    static <T>T copy(T value,Map<String,Object> changes)throws Exception {
        var fields=value.getClass().getRecordComponents();var types=new Class<?>[fields.length];var values=new Object[fields.length];
        for(int i=0;i<fields.length;i++) {types[i]=fields[i].getType();var m=fields[i].getAccessor();m.setAccessible(true);values[i]=changes.getOrDefault(fields[i].getName(),m.invoke(value));}
        var ctor=value.getClass().getDeclaredConstructor(types);ctor.setAccessible(true);return (T)ctor.newInstance(values);
    }
    static void cast(ClientSession caster,long target)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();
        NativeDirectoryRegression.call(caster,"useSpell",new Class[]{byte[].class},
                new Protocol.Writer().c(0x14).c(23).c(2).d(target).h(32741).h(32798).bytes());
    }
    static void attack(ClientSession caster,long target)throws Exception {
        NativeDirectoryRegression.call(caster,"handle",new Class[]{byte[].class},
                new Protocol.Writer().c(0x17).d(target).h(32741).h(32798).bytes());
    }
    static void stage3(BattleChainswordWeakness weakness,long target,long now) {
        weakness.clear();weakness.attack(5,18,target,now,0);weakness.attack(5,18,target,now+1,99);weakness.attack(5,18,target,now+2,99);
    }
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,
                "uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1,"counterBarrierFixtureD100",1));
        ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var originalRules=(CombatRules.Snapshot)rf.get(null);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var originalData=(CombatDatabase.Snapshot)df.get(null);
        try {
            long now=System.nanoTime();var weakness=new BattleChainswordWeakness();
            check(weakness.attack(5,18,101,now,33).afterLevel()==0,"source phase0 requires roll strictly below33");
            var result=weakness.attack(5,18,101,now,32);
            check(result.afterLevel()==1&&result.damageBonus()==10,"source first exposure damage10");
            result=weakness.attack(5,18,202,now+1,32);
            check(result.afterLevel()==1&&result.damageBonus()==10&&!result.targetReset(),"source exposure is caster bound, target switch retains phase");
            result=weakness.attack(5,18,202,now+2,99);
            check(result.afterLevel()==2&&result.damageBonus()==20,"source phase1 advances on every roll >=threshold");
            result=weakness.attack(5,18,202,now+3,99);
            check(result.afterLevel()==3&&result.damageBonus()==30,"source phase2 advances on every roll >=threshold");
            long lease=weakness.snapshot(now+3).remainingNanos();
            result=weakness.attack(5,18,202,now+4,33);
            check(!result.triggered()&&result.damageBonus()==0&&result.remainingNanos()==lease-1,"phase3 failed roll neither adds damage nor refreshes");
            check(CombatDragon.weaknessDeadline(10_800_000_000L,0)==27_000_000_000L,"integer-second source expiration is elapsed >16 seconds");
            long expiry=now+3+lease;
            check(weakness.snapshot(expiry+1).level()==3,"source state is expired by normal attack admission, not background snapshot");
            check(weakness.consumeFoeSlayer(5,999,expiry+1).consumedLevel()==3&&weakness.snapshot(expiry+1).level()==3,
                    "Foe cast does not preconsume or expire source exposure");
            weakness.sourceNormalAttack(expiry-1);check(weakness.snapshot(expiry-1).level()==3,"source normal attack retains phase immediately before second boundary");
            weakness.sourceNormalAttack(expiry);check(weakness.snapshot(expiry).level()==0,"normal attack clears expired state at source second boundary");
            stage3(weakness,101,now);
            check(weakness.sourceConsumeOnHit()==3&&weakness.sourceConsumeOnHit()==0,"successful Foe consumes exposure exactly once");
            var spell=BattleSpellCatalog.foeSlayerSpell(187);
            check(spell.hitCount()==5&&spell.damageForExposureLevel(3)==0,"current player Foe5 and no repeated flat exposure damage");
            check(CombatDragon.extraDamage(max->max)==10&&CombatDragon.extraDamage(max->1)==1,"source extra roll inclusive1..configured10");
            var tuned=new HashMap<>(originalRules.values());tuned.put("VULNERABILITY_ROM",100.0);tuned.put("VULNERABILITY_1",7.0);tuned.put("VULNERABILITY_2",11.0);tuned.put("VULNERABILITY_3",13.0);
            rf.set(null,new CombatRules.Snapshot(tuned,originalRules.skillFile(),originalRules.damageFile()));weakness.clear();
            result=weakness.attack(5,18,101,now,99);
            check(result.afterLevel()==1&&result.damageBonus()==7,"changed exposure threshold100 and phase1 damage reach actual state machine");
            check(weakness.attack(5,18,202,now+1,99).afterLevel()==1,"threshold100 keeps existing phase1");
            tuned.put("VULNERABILITY_ROM",0.0);rf.set(null,new CombatRules.Snapshot(tuned,originalRules.skillFile(),originalRules.damageFile()));
            result=weakness.attack(5,18,202,now+2,0);check(result.afterLevel()==2&&result.damageBonus()==11,"changed threshold0 advances existing phase1 and uses changed phase2 damage");
            result=weakness.attack(5,18,202,now+3,0);check(result.afterLevel()==3&&result.damageBonus()==13,"changed phase3 damage reaches actual state machine");
            tuned.put("FOE_SLAYER_DMG",0.0);rf.set(null,new CombatRules.Snapshot(tuned,originalRules.skillFile(),originalRules.damageFile()));
            int[] rolls={0};check(CombatDragon.extraDamage(max->{rolls[0]++;return 1;})==0&&rolls[0]==0,"zero Foe control skips extra damage and random draw");
            rf.set(null,originalRules);
            Path root=Path.of(args[1]);Files.createDirectories(root);
            var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
            try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
                var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var p=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
                var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();
                check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"isolated fixture clock stopped before deterministic actor checks");
                var fixtures=List.of(f,p,mate,other);
                for(int i=0;i<4;i++) {
                    var fixture=fixtures.get(i);NativeDirectoryRegression.init(fixture,"DragonSource"+i,0,config.classicServerNames().get(0));
                    var summary=((List<CharacterSummary>)get(fixture.session,"characters")).get(0);
                    set(fixture.session,"characters",List.of(copy(summary,Map.of("objectId",0x38110001L+i,"type",i==0?5:0,"armorClass",10))));
                    set(fixture.session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
                }
                long room=FlowRegression.setup(hub,f.session,p.session,mate.session,other.session);
                for(int i=0;i<4;i++) {
                    set(fixtures.get(i).session,"positionX",32740+i);set(fixtures.get(i).session,"positionY",32798);
                    set(fixtures.get(i).session,"playerMaxHp",5000);set(fixtures.get(i).session,"playerHp",5000);
                    set(fixtures.get(i).session,"playerMaxMp",1000);set(fixtures.get(i).session,"playerMp",1000);
                }
                var physical=copy(SourceEquipmentPhysicalStats.none(),Map.of("weapon",new SourceWeaponPhysicalStats(272,18,1,1,2,0,0,0,0),"itemMeleeDamage",3));
                set(f.session,"sourceEquipmentMagicStats",new SourceEquipmentMagicStats(physical,0,0));set(f.session,"learnedSkills",Set.of(187));
                var values=new HashMap<>(originalRules.values());values.put("VULNERABILITY_ROM",0.0);values.put("FOE_SLAYER_DMG",1.0);
                rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));
                var live=(BattleChainswordWeakness)get(f.session,"chainswordWeakness");
                attack(f.session,p.session.playerObjectId());int ordinary=5000-(int)get(p.session,"playerHp");
                check(ordinary>=4&&live.snapshot(System.nanoTime()).level()==0,"one-sided die plus fixed item damage yields deterministic hit without exposure and distinguishes1.3 truncation");
                values.put("VULNERABILITY_ROM",33.0);rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));
                stage3(live,123456,System.nanoTime());var liveSequence=(BattleFoeSlayerSequence)get(f.session,"foeSlayerSequence");liveSequence.begin(p.session.playerObjectId(),3,0,5);
                var strike=CombatDragon.consumeOnHit(liveSequence.next(p.session.playerObjectId()),f.session,false,p.session.playerObjectId());
                check(live.snapshot(System.nanoTime()).level()==3&&CombatDragon.multiplier(1,strike)==1,"actual actor miss retains exposure");
                strike=CombatDragon.consumeOnHit(liveSequence.next(p.session.playerObjectId()),f.session,true,p.session.playerObjectId());
                check(live.snapshot(System.nanoTime()).level()==0&&CombatDragon.multiplier(1,strike)==1.3,"first actual actor hit consumes stage3 and contributes1.3");
                check(CombatDragon.multiplier(1.8,strike)==1.8,"Foe finishing candidate does not multiply stronger proc");
                strike=CombatDragon.consumeOnHit(liveSequence.next(p.session.playerObjectId()),f.session,true,p.session.playerObjectId());
                check(strike.exposureLevel()==0&&CombatDragon.multiplier(1,strike)==1,"later hits have no repeated finishing multiplier");liveSequence.clear();
                stage3(live,123456,System.nanoTime());set(p.session,"playerHp",5000);set(f.session,"playerHp",1000);
                cast(f.session,p.session.playerObjectId());
                int expected=(int)(ordinary*1.3)+4*ordinary+1,actual=5000-(int)get(p.session,"playerHp");
                check(actual==expected,"native Foe exactly5 physical hits, first finishing multiplier and one extra damage: expected="+expected+" actual="+actual);
                check((int)get(f.session,"playerHp")==980&&live.snapshot(System.nanoTime()).level()==0&&liveSequence.snapshot().remainingStrikes()==0,
                        "native Foe charges HP once and clears sequence without rebuilding exposure");
                var rows=new HashMap<>(originalData.controls().probabilities());rows.put(91,new SourceSkillControls.Probability(91,90,90,90,0,0,0,0,false,false,0,0,0));
                df.set(null,new CombatDatabase.Snapshot(originalData.skills(),new SourceSkillControls.Snapshot(rows,originalData.controls().bossModifiers(),originalData.controls().abilities(),originalData.controls().npcAttributes(),originalData.controls().skillCoexist(),originalData.controls().cancellationImmune(),originalData.controls().eyes(),originalData.controls().blessings())));
                set(p.session,"sourceEquipmentMagicStats",accounts.equipmentMagicStats(20));
                ((BattleAbilityStatus)get(p.session,"abilityStatus")).activateCounterBarrier(System.nanoTime(),64);
                stage3(live,123456,System.nanoTime());set(p.session,"playerHp",5000);set(f.session,"playerHp",1000);
                cast(f.session,p.session.playerObjectId());
                check((int)get(p.session,"playerHp")==4999&&live.snapshot(System.nanoTime()).level()==3,
                        "five countered attacks preserve exposure and source final mode damage still commits once");
                check((int)get(f.session,"playerHp")==770&&liveSequence.snapshot().remainingStrikes()==0,"counter damage applied five times and Foe cleanup retained");
                set(f.session,"playerHp",20);int targetHp=(int)get(p.session,"playerHp");cast(f.session,p.session.playerObjectId());
                check((int)get(f.session,"playerHp")==20&&(int)get(p.session,"playerHp")==targetHp,"resource rejection causes no strikes or extra mode damage");
                values.put("VULNERABILITY_ROM",0.0);rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));
                var companion=(BattleCompanion)get(f.session,"battleCompanion");live.clear();set(f.session,"playerHp",1000);
                companion.resetForBattle(32741,32798);int companionHp=companion.snapshot().hp();
                attack(f.session,companion.snapshot().objectId());int ordinaryNpc=companionHp-companion.snapshot().hp();
                check(ordinaryNpc>0&&5*ordinaryNpc+1<companionHp,"qualified companion ordinary hit fixture remains alive for five hits");
                companion.resetForBattle(32741,32798);cast(f.session,companion.snapshot().objectId());
                check(companionHp-companion.snapshot().hp()==5*ordinaryNpc+1,"actual own summon Foe five physical hits plus one mode damage");
                check(live.snapshot(System.nanoTime()).level()==0&&liveSequence.snapshot().remainingStrikes()==0,"summon Foe does not rebuild exposure or retain sequence");
                hub.endBattle(room);for(var fixture:fixtures)hub.leaveRoom(room,fixture.session);
                long botRoom=hub.roomSnapshots(0).get(0).id();check(hub.tryJoinRoom(botRoom,f.session).joined(),"native ArenaBot room admission");
                Object botState=FlowRegression.room(hub,botRoom);set(botState,"battleStarted",true);set(botState,"battleGeneration",1L);set(botState,"botHp",GameHub.ARENA_BOT_MAX_HP);
                ((Map<ClientSession,Integer>)get(botState,"forces")).put(f.session,0);((Map<ClientSession,Integer>)get(botState,"battleForces")).put(f.session,0);
                ((Collection<ClientSession>)get(botState,"battleRoster")).add(f.session);
                ((BattleRules.MatchState)get(botState,"match")).begin(List.of(f.session.playerObjectId(),2L),Map.of(f.session.playerObjectId(),0,2L,1),f.session.playerObjectId(),System.nanoTime());
                var bot=hub.botPosition(botRoom);
                var prepare=Regression.class.getDeclaredMethod("prepareBattle",ClientSession.class,long.class,int.class,int.class);prepare.setAccessible(true);
                prepare.invoke(null,f.session,botRoom,bot.x()-1,bot.y());set(f.session,"playerHp",1000);set(f.session,"playerMaxHp",1000);
                int botBefore=hub.botHp(botRoom);cast(f.session,2);
                check(hub.botHp(botRoom)==botBefore-5*ordinary-1,"actual ArenaBot Foe five physical hits plus one mode damage");
                int expectedCasterHp=980-(config.battleAiEnabled()?0:5*Math.max(0,config.battleAiDamage()));
                check((int)get(f.session,"playerHp")==expectedCasterHp&&liveSequence.snapshot().remainingStrikes()==0,"ArenaBot Foe single HP cost, five native retaliations and cleanup");
                hub.endBattle(botRoom);hub.leaveRoom(botRoom,f.session);
                var world=ClassicWorldMapCatalog.forMap(4);check(!world.isSafetyZone(33089,32717)&&world.attackPosition(33089,32717,33090,32717,2),"qualified ordinary-world physical gate");
                Object phase=get(f.session,"phase");Object worldPhase=Enum.valueOf((Class)phase.getClass(),"WORLD");
                for(var session:List.of(f.session,p.session)) {
                    set(session,"phase",worldPhase);set(session,"classicWorldMapId",4);set(session,"classicWorldInstanceId",-1);
                    set(session,"positionY",32717);set(session,"playerHp",session==f.session?1000:5000);
                }
                set(f.session,"positionX",33089);set(p.session,"positionX",33090);values.put("VULNERABILITY_ROM",33.0);
                hub.enterClassicWorld(f.session,4,33089,32717,0);hub.enterClassicWorld(p.session,4,33090,32717,0);
                rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));
                ((BattleAbilityStatus)get(p.session,"abilityStatus")).activateCounterBarrier(System.nanoTime(),64);
                stage3(live,123456,System.nanoTime());cast(f.session,p.session.playerObjectId());
                check((int)get(p.session,"playerHp")==4999&&(int)get(f.session,"playerHp")==770&&live.snapshot(System.nanoTime()).level()==3,
                        "WORLD-player counter before damage calculation does not preconsume Foe exposure");
                ((BattleAbilityStatus)get(p.session,"abilityStatus")).removeCounterBarrier();values.put("VULNERABILITY_ROM",0.0);
                rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));live.clear();set(p.session,"playerHp",5000);
                attack(f.session,p.session.playerObjectId());int worldOrdinary=5000-(int)get(p.session,"playerHp");check(worldOrdinary>0,"ordinary WORLD physical hit fixture");
                values.put("VULNERABILITY_ROM",33.0);rf.set(null,new CombatRules.Snapshot(values,originalRules.skillFile(),originalRules.damageFile()));
                stage3(live,123456,System.nanoTime());set(p.session,"playerHp",5000);set(f.session,"playerHp",1000);cast(f.session,p.session.playerObjectId());
                check(5000-(int)get(p.session,"playerHp")==((int)(worldOrdinary*1.3)+4*worldOrdinary+1)
                        &&live.snapshot(System.nanoTime()).level()==0&&liveSequence.snapshot().remainingStrikes()==0,"actual WORLD Foe five hits, first finishing multiplier, final extra and cleanup");
                var protectedStatus=(BattleTimedStatus)get(p.session,"timedStatus");protectedStatus.activateEarthBind(System.nanoTime(),60);
                stage3(live,123456,System.nanoTime());targetHp=(int)get(p.session,"playerHp");attack(f.session,p.session.playerObjectId());
                check(live.snapshot(System.nanoTime()).level()==3&&(int)get(p.session,"playerHp")==targetHp,"ordinary physical damage-zero state skips exposure calculation");
                cast(f.session,p.session.playerObjectId());
                check(live.snapshot(System.nanoTime()).level()==3&&(int)get(p.session,"playerHp")==targetHp,"source protected target does not consume Foe exposure; Earth Bind also blocks final extra");
                protectedStatus.cancelEarthBind();
            }
        } finally {rf.set(null,originalRules);df.set(null,originalData);}
        System.out.println("PASS source Dragon Knight checks="+checks+"; actual five-hit native casts, source exposure transitions/target independence, first accepted hit max multiplier, counter/miss retention, final extra damage, costs and cleanup; no SQL writes");
    }
}
