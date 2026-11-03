package local.lito381.server;
import java.nio.file.*;
import java.util.*;

public class ReturnNatureRegression extends WeaponBuffRegression {
    static int serial;
    static SourceSkillControls.Probability row(int high,int low,double mr,double con,double wis){return new SourceSkillControls.Probability(145,high,high,low,0,0,0,0,false,false,mr,con,wis);}
    static CombatDatabase.Snapshot tune(CombatDatabase.Snapshot data,int npc,SourceSkillControls.Probability row,boolean boss,int divisor) {
        var c=data.controls();var probabilities=new HashMap<>(c.probabilities());if(row==null)probabilities.remove(145);else probabilities.put(145,row);
        var npcs=new HashMap<>(c.npcAttributes());npcs.put(npc,new SourceSkillControls.NpcAttributes(20,24,80,boss));var bosses=new HashMap<>(c.bossModifiers());if(divisor<0)bosses.remove(145);else bosses.put(145,divisor);
        return new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(probabilities,bosses,c.abilities(),npcs,c.skillCoexist(),c.cancellationImmune(),c.eyes(),c.blessings()));
    }
    static List<BattleCompanion> owned(ClientSession owner)throws Exception{return (List<BattleCompanion>)get(owner,"tamedCompanions");}
    static void resources(ClientSession caster,int crystals,int mp)throws Exception{set(caster,"inventory",RuntimeInventory.fromCatalogCounts(crystals==0?Map.of():Map.of(InventoryCatalog.SPIRIT_CRYSTAL,crystals)));set(caster,"playerMp",mp);set(caster,"playerMaxMp",1000);((Map<?,?>)get(caster,"spellReadyNanos")).clear();}
    static long crystals(ClientSession caster)throws Exception{return ((RuntimeInventory)get(caster,"inventory")).catalogCounts().getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL,0);}
    static void level(ClientSession actor,int level)throws Exception{var old=((List<CharacterSummary>)get(actor,"characters")).get(0);set(actor,"characters",List.of(copy(old,Map.of("level",level))));}
    static BattleCompanion summon(ClientSession owner,SourceNpcPhysicalStats stats,int x,int y)throws Exception {
        owned(owner).clear();var target=BattleCompanion.zombie(0x38188000L+(++serial),new BattleSpellCatalog.ZombieTemplate(45065,52,"$57",6,100),stats,6,x,y);owned(owner).add(target);return target;
    }
    static void castNature(ClientSession caster,BattleCompanion target)throws Exception {var t=target.snapshot();call(caster,"handle",new Class[]{byte[].class},new Protocol.Writer().c(0x14).c(18).c(0).d(t.objectId()).h(t.x()).h(t.y()).bytes());}
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("returnToNatureFixturePercentileRoll",30));ResourceDatabase.load(config);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        var ordinary=SourceNpcPhysicalStats.memoryFixture(45065);check(ordinary!=null,"native memory fixture has source zombie physical row");var stats=copy(ordinary,Map.of("magicResistance",80,"boss",false));var physical=new HashMap<Integer,SourceNpcPhysicalStats>();physical.put(45065,stats);
        AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
        var accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(ReturnNatureRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
            if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats")&&physical.containsKey((int)a[0]))return physical.get((int)a[0]);
            try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
        });
        Path root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);for(int i=0;i<4;i++){var fixture=fixtures.get(i);NativeDirectoryRegression.init(fixture,"Nature"+i,0,config.classicServerNames().get(0));var old=((List<CharacterSummary>)get(fixture.session,"characters")).get(0);set(fixture.session,"characters",List.of(copy(old,Map.of("objectId",0x38180001L+i))));}
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var caster=f.session;var owner=peer.session;level(caster,50);set(caster,"learnedSkills",Set.of(145));
            for(boolean world:new boolean[]{false,true}) {
                int x=world?33089:32740,y=world?32717:32798;
                for(var actor:List.of(caster,owner)){set(actor,"positionX",actor==caster?x:x+1);set(actor,"positionY",y);set(actor,"playerHp",1000);set(actor,"playerMaxHp",1000);if(world){Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,x,y,0);}}
                level(owner,90);df.set(null,tune(data,45065,row(80,20,1,0,0),false,-1));physical.put(45065,stats);var target=summon(owner,stats,x+1,y);long now=System.nanoTime();target.activateEraseMagic(now,60);target.activateSleep(now,60);
                resources(caster,0,1000);castNature(caster,target);check((int)get(caster,"playerMp")==1000&&target.captureEraseMagic(System.nanoTime()).present()&&target.sleepActive(System.nanoTime()),"missing material preserves MP/target leases world="+world);
                resources(caster,10,0);castNature(caster,target);check(crystals(caster)==10&&target.captureEraseMagic(System.nanoTime()).present()&&target.sleepActive(System.nanoTime()),"insufficient MP preserves materials/target leases world="+world);
                resources(caster,10,1000);castNature(caster,target);check(target.snapshot().active()&&!target.captureEraseMagic(System.nanoTime()).present()&&!target.sleepActive(System.nanoTime()),"accepted failed probability consumes captured Erase and clears Sleep world="+world);
                check((int)get(caster,"playerMp")<1000&&crystals(caster)==8,"accepted failed probability charges source MP/crystals world="+world);
                level(owner,10);target=summon(owner,stats,x+1,y);now=System.nanoTime();target.activateEraseMagic(now,60);target.activateSleep(now,60);resources(caster,10,1000);castNature(caster,target);
                check(!target.snapshot().active()&&crystals(caster)==8,"actual summon released at chance60 using captured quarter MR world="+world);
                level(owner,90);target=summon(owner,stats,x+1,y);df.set(null,tune(data,45065,row(80,20,0,0,0),false,-1));
                var tamed=BattleCompanion.tamed(0x38189901L,45065,52,"$57",6,100,100,0,0,10,10,x+1,y);
                check(CombatReturnNature.prepare(BattleSpellCatalog.returnToNatureSpell(145),50,90,10,0,0,80,caster,owner,target,System.nanoTime()).probability()==20
                        &&CombatReturnNature.prepare(BattleSpellCatalog.returnToNatureSpell(145),50,90,10,0,0,80,caster,owner,tamed,System.nanoTime()).probability()==20,"source SUMMON and TAMED both use master defense level world="+world);
                df.set(null,tune(data,45065,row(73,73,0,1,1),false,-1));resources(caster,10,1000);castNature(caster,target);check(target.snapshot().active(),"CON20/WIS24 chance29 fails roll30 world="+world);
                target.activatePanic(System.nanoTime(),60,-1);resources(caster,10,1000);castNature(caster,target);check(!target.snapshot().active(),"actual Panic CON19/WIS23 chance31 succeeds roll30 world="+world);
                level(owner,10);target=summon(owner,stats,x+1,y);target.activateEraseMagic(System.nanoTime(),60);target.activateEarthBind(System.nanoTime(),60);df.set(null,tune(data,45065,row(80,80,0,0,0),false,-1));resources(caster,10,1000);castNature(caster,target);
                check(target.snapshot().active()&&target.earthBindActive(System.nanoTime())&&!target.captureEraseMagic(System.nanoTime()).present()&&crystals(caster)==8,"Earth Bind blocks release after accepted costs/status consumption world="+world);
                physical.put(45065,copy(stats,Map.of("boss",true)));df.set(null,tune(data,45065,row(80,80,0,0,0),true,2));target=summon(owner,stats,x+1,y);resources(caster,10,1000);castNature(caster,target);check(!target.snapshot().active(),"positive source boss divisor permits chance40 release world="+world);
                df.set(null,tune(data,45065,row(80,80,0,0,0),true,0));target=summon(owner,stats,x+1,y);target.activateEraseMagic(System.nanoTime(),60);resources(caster,10,1000);castNature(caster,target);check(target.snapshot().active()&&!target.captureEraseMagic(System.nanoTime()).present()&&crystals(caster)==8,"source boss immunity rejects after accepted cost/status phase world="+world);
                physical.put(45065,stats);df.set(null,tune(data,45065,row(80,80,0,0,0),false,-1));target=summon(owner,stats,x+1,y);now=System.nanoTime();target.activateEraseMagic(now,60);
                var proposal=CombatReturnNature.prepare(BattleSpellCatalog.returnToNatureSpell(145),50,10,10,0,0,80,caster,owner,target,now);target.activateEraseMagic(now+1,90);CombatReturnNature.commit(proposal,target);
                check(proposal.magicResistance()==20&&target.captureEraseMagic(now+2).present(),"old accepted proposal cannot consume a refreshed Erase lease world="+world);
                var fallback=tune(data,45065,null,false,-1);var skills=new HashMap<>(fallback.skills());skills.put(145,skills.get(145).withProbability(3,20));df.set(null,fallback.withSkills(skills));
                level(owner,52);target=summon(owner,stats,x+1,y);int originalHit=(int)call(caster,"originalMagicHit",new Class[]{});
                check(CombatReturnNature.prepare(BattleSpellCatalog.returnToNatureSpell(145),50,52,10,0,3,80,caster,owner,target,System.nanoTime()).probability()==Math.max(1,Math.min(90,22+originalHit*2)),"source no-row fallback uses fractional level coefficient truncation, original hit and MR/80 world="+world);
                skills.put(145,skills.get(145).withProbability(10,20));df.set(null,fallback.withSkills(skills));level(owner,90);resources(caster,10,1000);castNature(caster,target);check(target.snapshot().active(),"actual source no-row fallback low SQL base fails world="+world);
                skills.put(145,skills.get(145).withProbability(10,100));df.set(null,fallback.withSkills(skills));resources(caster,10,1000);castNature(caster,target);check(!target.snapshot().active(),"actual changed SQL probabilityValue flips no-row fallback outcome world="+world);
                var bossFallback=tune(data,45065,null,true,2);var bossSkills=new HashMap<>(bossFallback.skills());bossSkills.put(145,bossSkills.get(145).withProbability(10,1000));df.set(null,bossFallback.withSkills(bossSkills));target=summon(owner,stats,x+1,y);
                check(CombatReturnNature.prepare(BattleSpellCatalog.returnToNatureSpell(145),50,90,10,0,0,80,caster,owner,target,System.nanoTime()).probability()==45,"source no-row fallback caps90 before boss division world="+world);
            }
            // Source Taming also constructs L1SummonInstance: owner90 must defend it.
            var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");var origin=population.withinTileDistance(4,0,0,65535).stream().filter(t->t.alive()&&t.present()&&t.template().level()<50&&BattleAnimationCatalog.get().supportsTimedAction(t.gfxId(),BattleActionCodes.WALK)&&BattleAnimationCatalog.get().supportsTimedAction(t.gfxId(),BattleActionCodes.ATTACK)&&ClassicWorldMapCatalog.forMap(4).isNormalZone(t.x(),t.y())&&ClassicWorldMapCatalog.forMap(4).hasLineOfSight(t.x()+1,t.y(),t.x(),t.y())).findFirst().orElseThrow();
            for(var actor:List.of(caster,owner)){set(actor,"positionX",origin.x()+1);set(actor,"positionY",origin.y());}level(owner,90);var npcStats=copy(stats,Map.of("npcId",origin.npcId()));physical.put(origin.npcId(),npcStats);
            df.set(null,tune(data,origin.npcId(),row(80,20,0,0,0),false,-1));var captured=hub.captureClassicWorldMonster(4,-1,origin.objectId());check(captured!=null,"native source WORLD actor captured");
            var tamed=BattleCompanion.tamedFromClassicWorld(0x38189902L,captured.captured(),-1,npcStats);owned(owner).clear();owned(owner).add(tamed);resources(caster,10,1000);castNature(caster,tamed);
            check(tamed.snapshot().active()&&!hub.classicWorldMonster(4,-1,origin.objectId()).present(),"actual WORLD tamed target uses owner90 and fails rather than NPC level");
            level(owner,10);resources(caster,10,1000);castNature(caster,tamed);
            check(!tamed.snapshot().active()&&hub.classicWorldMonster(4,-1,origin.objectId()).present(),"actual WORLD tamed release restores original source actor with owner10");
            check(crystals(caster)==8,"real tamed release charges exactly two Spirit Crystals");
        } finally {df.set(null,data);}
        System.out.println("PASS Return Nature checks="+checks+"; native BATTLE/WORLD costs, summon master level, genuine WORLD tamed restoration, Panic, pre-consumption Erase MR, Sleep, Earth Bind, source boss divisor/immunity and refreshed lease isolation; no SQL writes/deployment");
    }
}
