package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** All seven source class keys through actual native PC/NPC physical hits. */
public class ClassDamageRegression extends WeaponBuffRegression {
    static CombatRules.Snapshot with(CombatRules.Snapshot original,Map<String,Double> changes) {
        var values=new HashMap<>(original.values());values.putAll(changes);return new CombatRules.Snapshot(values,original.skillFile(),original.damageFile(),original.characterFile(),original.defaultedKeys(),original.mapDirectory());
    }
    static void packetAttack(ClientSession caster,long object,int x,int y)throws Exception{call(caster,"handle",new Class[]{byte[].class},new Protocol.Writer().c(0x17).d(object).h(x).h(y).bytes());if(!CombatMonsterCurseLifecycle.awaitDamage((GameHub)get(caster,"hub"),5000))throw new AssertionError("accepted physical HP did not drain");}
    static int npcAttack(ClientSession caster,BattleCompanion npc)throws Exception {int before=npc.snapshot().hp();packetAttack(caster,npc.snapshot().objectId(),npc.snapshot().x(),npc.snapshot().y());return before-npc.snapshot().hp();}
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,"uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1));ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var original=(CombatRules.Snapshot)rf.get(null);
        String[] names={"Crown","Knight","Elf","Wizard","Darkelf","DragonKnight","Illusionist"};
        check(CombatClassDamage.keys().size()==14&&original.values().size()==58,"fourteen source physical keys loaded with existing controls");
        var one=new HashMap<String,Double>();for(var key:CombatClassDamage.keys()){check(original.values().get(key)==1.0,"current source multiplier="+key);one.put(key,1.0);}
        var baseline=with(original,one);
        for(int type=0;type<7;type++)for(boolean player:new boolean[]{false,true}) {
            String key="Other_To_is"+names[type]+(player?"_pc":"_npc");check(CombatClassDamage.key(type,player).equals(key),"source class key identity="+type+" PC="+player);
            for(double factor:new double[]{0,0.4,1.25,2.75,-1}){rf.set(null,with(baseline,Map.of(key,factor)));check(CombatClassDamage.scale(17,type,player)==Math.max(0,(int)(17*factor)),"signed/fractional source class arithmetic="+key+" factor="+factor);}
            var values=new HashMap<String,Double>();var defaults=new HashSet<String>();CombatClassDamage.parse(values,new Properties(),key,Path.of("fixture"),defaults);check(values.get(key)==1.0&&defaults.contains(key),"source missing class key defaults1="+key);
            var p=new Properties();p.setProperty(key,"bad");values.clear();defaults.clear();CombatClassDamage.parse(values,p,key,Path.of("fixture"),defaults);check(values.get(key)==1.0&&defaults.contains(key),"source malformed class key defaults1="+key);
            p.setProperty(key,"NaN");boolean rejected=false;try{CombatClassDamage.parse(values,p,key,Path.of("fixture"),defaults);}catch(java.io.IOException expected){rejected=true;}check(rejected,"non-finite source class control rejected="+key);
        }
        rf.set(null,baseline);var physical=copy(SourceNpcPhysicalStats.memoryFixture(45065),Map.of("armorClass",10,"damageReduction",0,"hard",false,"boss",false,"undead",0));
        check(CombatClassDamage.scale(17,-1,true)==17&&CombatClassDamage.scale(17,7,false)==17,"unknown source class falls back to multiplier1");
        AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
        var accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(ClassDamageRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
            if(m.getName().equals("repositoryForClassicSource"))return p;
            if(m.getName().equals("npcPhysicalStats"))return copy(physical,Map.of("npcId",(int)a[0]));
            try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
        });
        InventoryCatalog.Definition weapon=null;for(int id=1;id<300;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&d.sourceWeaponStatus()!=null){weapon=copy(d,Map.of("sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"hitModifier",0,"damageModifier",0,"classMask",127)),"unitWeight",1));break;}}
        check(weapon!=null,"native one-die weapon fixture");Path root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);for(int i=0;i<4;i++){var fixture=fixtures.get(i);NativeDirectoryRegression.init(fixture,"ClassDamage"+i,0,config.classicServerNames().get(0));var summary=((List<CharacterSummary>)get(fixture.session,"characters")).get(0);set(fixture.session,"characters",List.of(copy(summary,Map.of("objectId",0x38190001L+i))));set(fixture.session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());}
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var caster=f.session;var target=peer.session;
            var inventory=RuntimeInventory.fromCatalogCounts(Map.of());var item=add(inventory,0x38191001L,weapon);set(caster,"inventory",inventory);use(caster,item.objectId());
            set(caster,"sourceEquipmentMagicStats",new SourceEquipmentMagicStats(copy(SourceEquipmentPhysicalStats.none(),Map.of("itemMeleeDamage",12)),0,0));
            var companion=(BattleCompanion)get(target,"battleCompanion");set(companion,"profile",copy(get(companion,"profile"),Map.of("maxHp",5000)));
            var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");var template=copy(ClassicWorldMonsterCatalog.definitions(4).get(0).template(),Map.of("npcId",45065,"gfxId",52,"hp",5000,"armorClass",10,"damageReduction",0,"hard",false,"undead",0,"weakAttribute",0,"boss",false));
            var monster=population.spawnQuestMonsterAt(4,900001,33090,32717,0,template);check(monster!=null,"native WORLD NPC fixture spawned on empty walkable cell");
            for(boolean world:new boolean[]{false,true}) {
                for(int i=0;i<4;i++){var actor=fixtures.get(i).session;set(actor,"positionX",world?33089+i:32740+i);set(actor,"positionY",world?32717:32798);if(world&&i<2){Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,33089+i,32717,0);}}
                for(int type=0;type<7;type++) {
                    var summary=((List<CharacterSummary>)get(caster,"characters")).get(0);set(caster,"characters",List.of(copy(summary,Map.of("type",type))));rf.set(null,baseline);
                    InventoryCatalog.Definition classWeapon=null;for(int id=1;id<300;id++) {
                        var d=InventoryCatalog.find(id);if(d==null||!d.weapon()||d.sourceWeaponStatus()==null||d.weaponWalkAction()==20||d.weaponWalkAction()==62)continue;
                        if(!BattleAnimationCatalog.get().supportsTimedAction(caster.battleGfx(),d.weaponWalkAction())||!BattleAnimationCatalog.get().supportsTimedAction(caster.battleGfx(),BattleActionCodes.attackForWalk(d.weaponWalkAction())))continue;
                        classWeapon=copy(d,Map.of("sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"hitModifier",0,"damageModifier",0,"classMask",127)),"unitWeight",1));break;
                    }
                    check(classWeapon!=null,"selected-client compatible native weapon type="+type);
                    var classInventory=RuntimeInventory.fromCatalogCounts(Map.of());var classItem=add(classInventory,0x38192001L+type,classWeapon);set(caster,"inventory",classInventory);use(caster,classItem.objectId());
                    set(caster,"sourceEquipmentMagicStats",new SourceEquipmentMagicStats(copy(SourceEquipmentPhysicalStats.none(),Map.of("itemMeleeDamage",12)),0,0));
                    companion.resetForBattle(32741,32798);companion.qualifyNpcDefense(copy(physical,Map.of("npcId",companion.snapshot().npcId())));
                    int pcBase=attack(caster,target);int npcBase;
                    if(world){int before=hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();packetAttack(caster,monster.objectId(),monster.x(),monster.y());npcBase=before-hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();}
                    else npcBase=npcAttack(caster,companion);
                    check(pcBase>0&&npcBase>0,"native seven-class baseline hits type="+type+" WORLD="+world);
                    for(double factor:new double[]{0.4,1.25,2.75,0}) {
                        String pc="Other_To_is"+names[type]+"_pc",npc="Other_To_is"+names[type]+"_npc";rf.set(null,with(baseline,Map.of(pc,factor,npc,factor+0.5)));
                        check(attack(caster,target)==(int)(pcBase*factor),"actual source PC physical factor/truncation type="+type+" WORLD="+world+" factor="+factor);
                        int dealt;if(world){int before=hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();packetAttack(caster,monster.objectId(),monster.x(),monster.y());dealt=before-hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();}else dealt=npcAttack(caster,companion);
                        check(dealt==(int)(npcBase*(factor+0.5)),"actual independent source NPC physical factor type="+type+" WORLD="+world+" factor="+(factor+0.5));
                    }
                    rf.set(null,with(baseline,Map.of("Other_To_is"+names[type]+"_pc",3.0,"Other_To_is"+names[type]+"_npc",0.0)));
                    check(attack(caster,target)==pcBase*3,"source PC and NPC class multipliers stay independent type="+type+" WORLD="+world);
                    int zero;if(world){int before=hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();packetAttack(caster,monster.objectId(),monster.x(),monster.y());zero=before-hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();}else zero=npcAttack(caster,companion);
                    check(zero==0,"native NPC zero multiplier admits no damage type="+type+" WORLD="+world);
                }
            }
        } finally {rf.set(null,original);}
        System.out.println("PASS source class physical damage checks="+checks+"; all seven class/property identities, fractional/signed/zero math, loader defaults, native BATTLE/WORLD PC and NPC hit deltas; magic has a separate regression; no SQL writes/deployment");
    }
}
