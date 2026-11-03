package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Current-source Kiringku through actual native attacks; isolated immutable inputs. */
public class KiringkuRegression extends WeaponBuffRegression {
    static SourceEquipmentMagicStats stats(int intelligence,int sp,int hit)throws Exception {
        return copy(SourceEquipmentMagicStats.none(),Map.of("itemIntelligence",intelligence,"itemSpellPower",sp,"itemMagicHit",hit,"physical",copy(SourceEquipmentPhysicalStats.none(),Map.of("itemMeleeDamage",200))));
    }
    static void equip(ClientSession caster,InventoryCatalog.Definition weapon,int enchant)throws Exception {
        var inv=RuntimeInventory.fromCatalogCounts(Map.of());var item=new RuntimeInventory.Item(0x381b1001L,weapon,1,true,0,enchant,false,0,true);check(inv.add(item,1,()->item.objectId()),"isolated Kiringku source-bound native inventory");inv.qualifySourceWeapon(item.objectId(),new SourceWeaponPhysicalStats(270,17,1,1,1,0,77,enchant,0));set(caster,"inventory",inv);set(caster,"sourceEquipmentMagicStats",stats(0,3,4));check(inv.equippedPhysicalWeapon().weaponType()==17,"per-instance source Kiringku type is not generic claw walk type11");
    }
    static int npcHit(ClientSession caster,BattleCompanion npc)throws Exception {int before=npc.snapshot().hp();ClassDamageRegression.packetAttack(caster,npc.snapshot().objectId(),npc.snapshot().x(),npc.snapshot().y());return before-npc.snapshot().hp();}
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,"uncannyDodgeHitFixtureD20",1,"uncannyDodgeHitFixtureD100",100,"shadowFangDamageFixtureWeaponRoll",1));ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var rules=(CombatRules.Snapshot)rf.get(null);
        check(rules.values().size()==58&&rules.values().get(CombatKiringku.MR_KEY)==1.5,"current source MR extra control loaded");
        var missing=CombatRules.read(rules.damageFile());missing.remove(CombatKiringku.MR_KEY);var defaults=CombatRules.snapshot(CombatRules.read(rules.skillFile()),missing,rules.skillFile(),rules.damageFile());check(defaults.values().get(CombatKiringku.MR_KEY)==3&&defaults.defaultedKeys().contains(CombatKiringku.MR_KEY),"source missing MR control default3");
        check(CombatKiringku.magicBonus(43)==15&&CombatKiringku.magicBonus(44)==16&&CombatKiringku.magicBonus(100)==72,"current source high-INT magic bonus is not old15 cap");
        check(Math.abs(CombatKiringku.prefix(20,14,4,1,0,100,true,1.5)-52.785)<1e-9,"source current prefix69 * .9 * .85 retains fraction");
        check(Math.abs(CombatKiringku.prefix(20,14,4,1,0,1000,false,100)-62.1)<1e-9,"source NPC Kiringku has no inherited MR formula");
        var physical=copy(SourceNpcPhysicalStats.memoryFixture(45065),Map.of("armorClass",10,"damageReduction",5,"hard",false,"boss",false,"magicResistance",1000));
        AccountRepository memory=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
        var accounts=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(KiringkuRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
            if(m.getName().equals("repositoryForClassicSource"))return p;if(m.getName().equals("npcPhysicalStats"))return copy(physical,Map.of("npcId",(int)a[0]));
            try{return m.invoke(memory,a);}catch(java.lang.reflect.InvocationTargetException fail){throw fail.getCause();}
        });
        InventoryCatalog.Definition weapon=null;for(int id=1;id<1000;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&d.sourceWeaponStatus()!=null&&d.weaponWalkAction()==BattleActionCodes.CLAW_WALK){weapon=copy(d,Map.of("catalogId",270L,"unitWeight",1,"sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"damageModifier",77,"classMask",64))));break;}}
        check(weapon!=null,"native claw-action carrier for source Kiringku270");Path root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");var fixtures=List.of(f,peer,mate,other);
            for(int i=0;i<4;i++){var actor=fixtures.get(i).session;NativeDirectoryRegression.init(fixtures.get(i),"Kiringku"+i,0,config.classicServerNames().get(0));var summary=((List<CharacterSummary>)get(actor,"characters")).get(0);set(actor,"characters",List.of(copy(summary,Map.of("objectId",0x381b0001L+i,"type",i==0?6:1,"intelligence",20,"originalIntelligence",9,"wisdom",9,"originalWisdom",9))));set(actor,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(actor,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(actor,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));}
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var caster=f.session;var target=peer.session;
            var companion=(BattleCompanion)get(target,"battleCompanion");set(companion,"profile",copy(get(companion,"profile"),Map.of("maxHp",50000)));
            var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");var template=copy(ClassicWorldMonsterCatalog.definitions(4).get(0).template(),Map.of("npcId",45065,"gfxId",52,"hp",50000,"armorClass",10,"damageReduction",5,"hard",false,"undead",0,"weakAttribute",0,"magicResistance",1000,"boss",false));var monster=population.spawnQuestMonsterAt(4,900002,33090,32717,0,template);check(monster!=null,"actual WORLD NPC fixture");
            for(boolean world:new boolean[]{false,true}) {
                for(int i=0;i<4;i++){var actor=fixtures.get(i).session;set(actor,"positionX",world?33089+i:32740+i);set(actor,"positionY",world?32717:32798);if(world&&i<2){Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,33089+i,32717,0);}}
                companion.resetForBattle(32741,32798);companion.qualifyNpcDefense(copy(physical,Map.of("npcId",companion.snapshot().npcId())));equip(caster,weapon,0);
                set(target,"sourceEquipmentMagicStats",copy(SourceEquipmentMagicStats.none(),Map.of("armorTemplateDamageReduction",5,"itemMagicResistance",100)));
                for(double perTen:new double[]{0,1.5,3,100,-2})for(double factor:new double[]{0.4,1.25,2.75,0}) {
                    rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatKiringku.MR_KEY,perTen,"Other_To_isIllusionist_pc",factor,"Other_To_isIllusionist_npc",1.25)));
                    double prefix=62.1*(1-Math.min(1,perTen/10));int want=Math.max(0,(int)((prefix+3-5)*factor));
                    int dealt=attack(caster,target);check(dealt==want,"actual current Kiringku MR/class/fraction WORLD="+world+" MR-rate="+perTen+" class="+factor+" expected="+want+" actual="+dealt);
                    int npc;if(world){int before=hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();ClassDamageRegression.packetAttack(caster,monster.objectId(),monster.x(),monster.y());npc=before-hub.classicWorldMonster(4,-1,monster.objectId()).currentHp();}else npc=npcHit(caster,companion);
                    check(npc==75,"Kiringku NPC ignores MR1000, PC MR control and ordinary melee bonus200 WORLD="+world+" actual="+npc);
                }
                rf.set(null,ClassDamageRegression.with(rules,Map.of(CombatKiringku.MR_KEY,1.5,"Other_To_isIllusionist_pc",1.25)));
                ((BattleAbilityStatus)get(target,"abilityStatus")).activateImmuneToHarm(System.nanoTime(),64,2);check(attack(caster,target)==31,"Kiringku preserves fraction through defense/immune/class WORLD="+world);((BattleAbilityStatus)get(target,"abilityStatus")).clear();
                ((BattleEraseMagicStatus)get(target,"eraseMagicStatus")).activate(System.nanoTime(),64);check(attack(caster,target)==63,"Kiringku MR uses raw player MR100 rather than Erase quarter25 WORLD="+world);((BattleEraseMagicStatus)get(target,"eraseMagicStatus")).clear();
                set(caster,"sourceEquipmentMagicStats",stats(0,3,7));check(attack(caster,target)==68,"equipment magic hit participates before 1.8 WORLD="+world);
                set(caster,"sourceEquipmentMagicStats",stats(6,3,4));check(attack(caster,target)==82,"INT and resulting true SP magic bonus both participate WORLD="+world);
                equip(caster,weapon,2);check(attack(caster,target)==65,"weapon enchant added before .9 and extra MR WORLD="+world);
                ((BattleTimedStatus)get(target,"timedStatus")).activateAbsoluteBarrier(System.nanoTime(),8);check(attack(caster,target)==0,"native absolute barrier remains after Kiringku calculation WORLD="+world);((BattleTimedStatus)get(target,"timedStatus")).clear();
            }
        } finally{rf.set(null,rules);}
        System.out.println("PASS current-source Kiringku checks="+checks+"; native BATTLE/WORLD PC and NPC, INT/SP/magic-hit/weapon/enchant, raw MR control, float defense/class order, auto-hit and barrier; other source combat controls/paths remain tracked; no SQL writes/deployment");
    }
}
