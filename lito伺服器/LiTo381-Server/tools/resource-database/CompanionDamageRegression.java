package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Source PC-to-pet coefficients through owned native actor targets and real spell packets. */
public class CompanionDamageRegression extends WeaponBuffRegression {
    static int serial;
    static List<BattleCompanion> owned(ClientSession owner)throws Exception{return (List<BattleCompanion>)get(owner,"tamedCompanions");}
    static BattleCompanion target(ClientSession owner,BattleCompanion.Kind kind,SourceNpcPhysicalStats physical,int x,int y)throws Exception {
        owned(owner).clear();long id=0x381c8000L+(++serial);BattleCompanion target;
        if(kind==BattleCompanion.Kind.SUMMON)target=BattleCompanion.zombie(id,new BattleSpellCatalog.ZombieTemplate(45065,52,"$57",6,50000),copy(physical,Map.of("npcId",45065)),6,x,y);
        else if(kind==BattleCompanion.Kind.TAMED)target=BattleCompanion.tamed(id,45065,52,"$57",6,50000,50000,1000,10,10,10,x,y);
        else if(kind==BattleCompanion.Kind.PET) {
            var template=copy(SourcePetTemplate.memoryFixture(45034),Map.of("armorClass",10,"magicResistance",80,"damageReduction",5));
            var state=new GameStateStore.PetState("",0x381c1000L+serial,0x90010000L+serial,(int)InventoryCatalog.PET_COLLAR,45034,"Pet",6,50000,1000,0,0);
            target=BattleCompanion.pet(state,template,x,y);
        } else {target=(BattleCompanion)get(owner,"battleCompanion");set(target,"profile",copy(get(target,"profile"),Map.of("maxHp",50000,"maxMp",1000,"armorClass",10)));target.resetForBattle(x,y);}
        target.qualifyNpcDefense(copy(physical,Map.of("npcId",target.snapshot().npcId())));if(kind!=BattleCompanion.Kind.ARENA)owned(owner).add(target);return target;
    }
    static int spell(ClientSession caster,BattleCompanion target,int skill)throws Exception {
        var t=target.snapshot();target.resetForBattle(t.x(),t.y());set(caster,"playerHp",5000);set(caster,"playerMaxHp",5000);set(caster,"playerMp",1000);set(caster,"playerMaxMp",1000);set(caster,"playerLawful",32767);((Map<?,?>)get(caster,"spellReadyNanos")).clear();set(caster,"lastCombatTargetObjectId",t.objectId());
        cast(caster,target,skill);return 50000-target.snapshot().hp();
    }
    static void cast(ClientSession caster,BattleCompanion target,int skill)throws Exception {
        var t=target.snapshot();var area=BattleSpellCatalog.areaAttackSpell(skill);var freeze=BattleSpellCatalog.freezeAttackSpell(skill);boolean selector=area!=null&&(area.maximumRange()==0||area.requestShape()==BattleSpellCatalog.RequestShape.SELECTOR_ONLY)||freeze!=null&&(freeze.areaRadius()>0||freeze.lineTargeted());if(skill>=175)selector=false;
        var packet=new Protocol.Writer().c(0x14).c((skill-1)/8).c((skill-1)%8);if(!selector)packet.d(t.objectId()).h(t.x()).h(t.y());call(caster,"useSpell",new Class[]{byte[].class},packet.bytes());
    }
    static int physical(ClientSession caster,BattleCompanion target)throws Exception {
        var t=target.snapshot();target.resetForBattle(t.x(),t.y());set(caster,"playerHp",5000);set(caster,"playerMaxHp",5000);ClassDamageRegression.packetAttack(caster,t.objectId(),t.x(),t.y());return 50000-target.snapshot().hp();
    }
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,"uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1));ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var rules=(CombatRules.Snapshot)rf.get(null);var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        check(rules.values().size()==58&&rules.values().get(CombatCompanionDamage.KEY)==1,"current PC-to-pet source multiplier loaded");
        var properties=new Properties();properties.putAll(CombatRules.read(rules.damageFile()));properties.remove(CombatCompanionDamage.KEY);var loaded=CombatRules.snapshot(CombatRules.read(rules.skillFile()),properties,rules.skillFile(),rules.damageFile());check(loaded.values().get(CombatCompanionDamage.KEY)==0.125&&loaded.defaultedKeys().contains(CombatCompanionDamage.KEY),"source absent control default .125");
        for(String bad:new String[]{"","garbage"}){properties.setProperty(CombatCompanionDamage.KEY,bad);loaded=CombatRules.snapshot(CombatRules.read(rules.skillFile()),properties,rules.skillFile(),rules.damageFile());check(loaded.values().get(CombatCompanionDamage.KEY)==0.125&&loaded.defaultedKeys().contains(CombatCompanionDamage.KEY),"source malformed control uses .125="+bad);}
        for(String bad:new String[]{"NaN","Infinity","-Infinity"}){properties.setProperty(CombatCompanionDamage.KEY,bad);boolean rejected=false;try{CombatRules.snapshot(CombatRules.read(rules.skillFile()),properties,rules.skillFile(),rules.damageFile());}catch(java.io.IOException e){rejected=true;}check(rejected,"nonfinite companion control rejected="+bad);}
        var ids=Set.of(4,17,25,53,50,80,194,77,87,203,207,208,213,108);var skills=new HashMap<>(data.skills());for(int id:ids){var old=skills.get(id);skills.put(id,new CombatDatabase.Skill(old.mp(),old.hp(),old.reuse(),old.duration(),0,1,0,0,0,old.probabilityDice(),old.probabilityValue()));}df.set(null,data.withSkills(skills));
        var physical=copy(SourceNpcPhysicalStats.memoryFixture(45065),Map.of("armorClass",10,"damageReduction",5,"magicResistance",80,"hard",false,"boss",false,"maxMp",1000));
        var sourceMr=new java.util.concurrent.atomic.AtomicInteger(80);AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(CompanionDamageRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
            if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats"))return copy(physical,Map.of("npcId",(int)a[0],"magicResistance",sourceMr.get()));try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
        });
        InventoryCatalog.Definition dagger=null;for(int id=1;id<300;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&d.sourceWeaponStatus()!=null&&d.weaponWalkAction()==BattleActionCodes.DAGGER_WALK){dagger=copy(d,Map.of("unitWeight",1,"sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"damageModifier",0,"hitModifier",0,"classMask",127))));break;}}check(dagger!=null,"native deterministic dagger fixture");
        Path root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");var fixtures=List.of(f,peer,mate,other);
            for(int n=0;n<4;n++){var actor=fixtures.get(n).session;NativeDirectoryRegression.init(fixtures.get(n),"CompanionDamage"+n,0,config.classicServerNames().get(0));var old=((List<CharacterSummary>)get(actor,"characters")).get(0);set(actor,"characters",List.of(copy(old,Map.of("objectId",0x381c0001L+n,"intelligence",9,"originalIntelligence",9))));set(actor,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());}
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var caster=f.session;var owner=peer.session;set(caster,"learnedSkills",ids);
            var inv=RuntimeInventory.fromCatalogCounts(Map.of());var item=add(inv,0x381c2001L,dagger);set(caster,"inventory",inv);use(caster,item.objectId());inv=(RuntimeInventory)get(caster,"inventory");check(inv.equippedWeaponItem()!=null,"native dagger equipped");
            var equipment=copy(SourceEquipmentMagicStats.none(),Map.of("itemMagicDamage",37,"physical",copy(SourceEquipmentPhysicalStats.none(),Map.of("itemMeleeDamage",12))));set(caster,"sourceEquipmentMagicStats",equipment);
            for(boolean world:new boolean[]{false,true}) {
                int x=world?33089:32740,y=world?32717:32798;for(int n=0;n<4;n++){var actor=fixtures.get(n).session;set(actor,"positionX",x+n);set(actor,"positionY",y);if(world&&n<2){Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,x+n,y,0);}}
                for(var kind:List.of(BattleCompanion.Kind.SUMMON,BattleCompanion.Kind.TAMED,BattleCompanion.Kind.PET)) {
                    var companion=target(owner,kind,physical,x+1,y);check(CombatCompanionDamage.eligible(companion),"source actor classification="+kind);
                    for(double factor:new double[]{0.4,1.25,2.75,0,-1}) {
                        rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,factor,"DISINTEGRATE_CRITICAL_RND",0.0,"Other_ADD_isCrown_MagicPC",9.0)));
                        int actual=spell(caster,companion,4),want=Math.max(0,(int)((int)(37*factor)*0.64));check(actual==want,"native pet magic before MR kind="+kind+" WORLD="+world+" factor="+factor+" expected="+want+" actual="+actual);
                    }
                    sourceMr.set(600);companion.qualifyNpcDefense(copy(physical,Map.of("npcId",companion.snapshot().npcId(),"magicResistance",600)));
                    rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,-1.0)));check(spell(caster,companion,4)==1,"source signed NPC damage survives until high-MR calculation kind="+kind+" WORLD="+world);
                    sourceMr.set(80);companion.qualifyNpcDefense(copy(physical,Map.of("npcId",companion.snapshot().npcId())));
                    companion.resetForBattle(x+1,y);companion.activateEraseMagic(System.nanoTime(),60);set(caster,"playerMp",0);((Map<?,?>)get(caster,"spellReadyNanos")).clear();cast(caster,companion,4);check(companion.snapshot().hp()==50000&&companion.captureEraseMagic(System.nanoTime()).present(),"rejected cast preserves HP and Erase kind="+kind+" WORLD="+world);
                    rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,0.4,"DISINTEGRATE_CRITICAL_RND",100.0)));
                    for(int id:world?new int[]{17,25,53,50,80,194,77,203,207,208,213,108}:new int[]{25,53,50,77,203,207,208,213,108}) {
                        int source=id==108?1000:37;int want=(int)(((int)(source*0.4))*(id==77?2:1)*0.64)*(id==207?4:1);int actual=spell(caster,companion,id);check(actual==want,"native owned-target spell family kind="+kind+" WORLD="+world+" skill="+id+" expected="+want+" actual="+actual);
                    }
                    if(!world) {
                        rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,1.0,"Other_To_isCrown_npc",1.0)));int baseline=physical(caster,companion);check(baseline>0,"native owned physical baseline="+kind);
                        for(double factor:new double[]{0.4,1.25,2.75,0,-1}){rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,factor,"Other_To_isCrown_npc",1.25)));int actual=physical(caster,companion),want=Math.max(0,(int)(baseline*factor*1.25));check(actual==want,"owned physical factor before NPC class with fraction kind="+kind+" factor="+factor+" expected="+want+" actual="+actual);}
                        var sourceWeapon=inv.equippedPhysicalWeapon();var criticalInventory=RuntimeInventory.fromCatalogCounts(Map.of());var criticalItem=new RuntimeInventory.Item(item.objectId(),dagger,1,true,0,0,false,0,true);check(criticalInventory.add(criticalItem,1,()->criticalItem.objectId()),"isolated per-instance critical source weapon");criticalInventory.qualifySourceWeapon(item.objectId(),sourceWeapon.withCombatTraits(copy(sourceWeapon.combatTraits(),Map.of("doubleDamageChance",100,"doubleDamageMultiplier",1.5))));set(caster,"inventory",criticalInventory);
                        rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,1.25,"Other_To_isCrown_npc",1.25)));int actual=physical(caster,companion),want=(int)(((baseline+5)*1.5-5)*1.25*1.25);check(actual==want,"native critical fraction retained through companion and class factors kind="+kind+" expected="+want+" actual="+actual);
                        var summaries=(List<CharacterSummary>)get(caster,"characters");set(caster,"characters",List.of(copy(summaries.get(0),Map.of("type",6,"intelligence",20))));InventoryCatalog.Definition carrier=null;for(int id=1;id<1000;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&d.sourceWeaponStatus()!=null&&d.weaponWalkAction()==BattleActionCodes.CLAW_WALK){carrier=copy(d,Map.of("catalogId",270L,"unitWeight",1,"sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"classMask",64))));break;}}check(carrier!=null,"native Kiringku carrier");KiringkuRegression.equip(caster,carrier,0);
                        for(double factor:new double[]{0.4,1.25,0,-1}){rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,factor,"Other_To_isIllusionist_npc",1.25)));actual=physical(caster,companion);want=Math.max(0,(int)((62.1+3-5)*factor*1.25));check(actual==want,"Kiringku pet factor before class, without MR kind="+kind+" factor="+factor+" expected="+want+" actual="+actual);}
                        set(caster,"characters",summaries);set(caster,"inventory",inv);set(caster,"sourceEquipmentMagicStats",equipment);
                    }
                }
                if(!world) {
                    var arena=target(owner,BattleCompanion.Kind.ARENA,physical,x+1,y);check(!CombatCompanionDamage.eligible(arena),"ARENA actor is not source pet/summon");rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,1.0)));int basePhysical=physical(caster,arena),baseMagic=spell(caster,arena,4);rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatCompanionDamage.KEY,0.0)));check(physical(caster,arena)==basePhysical&&spell(caster,arena,4)==baseMagic,"ARENA physical/magic unaffected by zero PC-to-pet control");
                }
            }
            for(var c:CombatCompanionDamage.Physical.class.getRecordComponents())check(c.getType().isPrimitive(),"physical attempt has no actor reference="+c.getName());
        }finally{rf.set(null,rules);df.set(null,data);}
        System.out.println("PASS source owned companion damage checks="+checks+"; native BATTLE physical and BATTLE/WORLD magic for SUMMON/TAMED/PET, NPC class/fraction and MR/critical/mode order, ARENA exclusion; missing target admission paths remain tracked; no SQL writes/deployment");
    }
}
