package local.lito381.server;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Source player-owned enchantments through native casts, equipment and damage. */
public class WeaponBuffRegression {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Object get(Object o,String n)throws Exception{return NativeDirectoryRegression.get(o,n);}
    static void set(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
    static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{return NativeDirectoryRegression.call(o,n,types,args);}
    static <T>T copy(T value,Map<String,Object> changes)throws Exception{return AwakenCombatRegression.copy(value,changes);}
    static CombatDatabase.Snapshot controls(CombatDatabase.Snapshot data,Map<Integer,Map<String,Integer>> abilities,boolean coexist,Set<Integer> immune) {
        var c=data.controls();return new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(c.probabilities(),c.bossModifiers(),abilities,c.npcAttributes(),coexist,immune,c.eyes(),c.blessings()));
    }
    static void cast(ClientSession caster,int skill,long target)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();set(caster,"playerMp",1000);set(caster,"playerMaxMp",1000);
        call(caster,"useSpell",new Class[]{byte[].class},new Protocol.Writer().c(0x14).c((skill-1)/8).c((skill-1)%8).d(target).bytes());
    }
    static void use(ClientSession caster,long item)throws Exception {
        call(caster,"useInventoryItem",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_ITEM).d(item).s("").bytes());
    }
    static int attack(ClientSession caster,ClientSession target)throws Exception {
        set(target,"playerHp",5000);set(target,"playerMaxHp",5000);
        call(caster,"handle",new Class[]{byte[].class},new Protocol.Writer().c(0x17).d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY")).bytes());
        return 5000-(int)get(target,"playerHp");
    }
    static RuntimeInventory.Item add(RuntimeInventory inv,long object,InventoryCatalog.Definition definition)throws Exception {
        var item=new RuntimeInventory.Item(object,definition,1,false);check(inv.add(item,1,()->object),"add isolated native item "+object);return inv.item(object);
    }
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,
                "uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1));ResourceDatabase.load(config);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        check(data.controls().cancellationImmune().size()==12&&data.controls().cancellationImmune().containsAll(Set.of(12,21,107)),"source cancellation immunity snapshot has actual12 rows");
        boolean immutable=false;try{data.controls().cancellationImmune().clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"source immunity set is immutable");
        try {
            long now=System.nanoTime();var status=new BattleAbilityStatus();var leases=CombatBuffLeases.owned(status);
            for(int id:new int[]{8,12,48,107,21})leases.put(id,now,20);
            var weapon=leases.weapon(now);check(weapon.meleeDamage()==10&&weapon.meleeHit()==3&&weapon.rangedHit()==2&&weapon.rangedDamage()==0,"source4 weapon buffs use melee/ranged SQL columns exactly once");
            check(leases.armor(now)==-3,"source player armor BUFF");leases.put(12,now,30);check(leases.weapon(now).meleeDamage()==10,"refresh cannot duplicate one weapon skill");
            status.cancelMagicEffects();weapon=leases.weapon(now);
            check(weapon.meleeDamage()==7&&weapon.meleeHit()==0&&weapon.rangedHit()==0&&leases.armor(now)==-3,"ordinary Cancellation preserves source12/107/21 only");
            status.clear();check(leases.weapon(now).meleeDamage()==0&&leases.armor(now)==0,"full native lifecycle clear removes immune player buffs too");
            df.set(null,controls(data,data.controls().abilities(),false,data.controls().cancellationImmune()));
            for(int id:new int[]{8,12,48,107})leases.put(id,now,20);
            check(leases.weapon(now).meleeDamage()==7&&leases.weapon(now).meleeHit()==2,"source non-coexist mode replaces8/12/48 while Fang stays separate");
            status.clear();df.set(null,controls(data,data.controls().abilities(),true,Set.of(8)));
            for(int id:new int[]{8,12,48,107,21})leases.put(id,now,20);status.cancelMagicEffects();
            check(leases.weapon(now).meleeDamage()==1&&leases.weapon(now).meleeHit()==1&&leases.armor(now)==0,"altered immunity snapshot changes actual cancellation consumer");
            status.clear();df.set(null,data);leases.put(12,now,2);leases.put(107,now,4);
            check(leases.weapon(now+2_000_000_000L).meleeDamage()==5&&leases.weapon(now+4_000_000_000L).meleeDamage()==0,"weapon buff IDs expire independently at exact boundaries");
            var armLease=new BattleArmBreakerStatus();check(armLease.activateIfAbsent(now,7).accepted()&&armLease.meleeDamageModifier(now)==-2,"canonical213 alias binds SQL DMG and accepts source SQL duration");
            check(armLease.activateIfAbsent(now+1,9).alreadyActive()&&armLease.active(now+6_999_999_999L)&&!armLease.active(now+7_000_000_000L),"Arm Breaker retains source no-refresh and exact configured expiry");
            check(!new BattleArmBreakerStatus().activateIfAbsent(now,0).accepted()&&!new BattleArmBreakerStatus().activateIfAbsent(now,65536).accepted(),"Arm Breaker SQL duration boundaries");
            var definitions=new ArrayList<InventoryCatalog.Definition>();
            for(int id=1;id<300;id++) {
                var d=InventoryCatalog.find(id);if(d==null||!d.weapon()||d.sourceWeaponStatus()==null)continue;
                if(!definitions.isEmpty()&&d.weaponWalkAction()!=definitions.get(0).weaponWalkAction())continue;
                var nativeWeapon=copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"hitModifier",0,"damageModifier",0,"classMask",127));
                definitions.add(copy(d,Map.of("sourceWeaponStatus",nativeWeapon,"unitWeight",1)));
                if(definitions.size()==2)break;
            }
            InventoryCatalog.Definition body=null;
            for(int id=20000;id<22000;id++) {
                var d=InventoryCatalog.find(id);if(d==null||!d.sourceArmor()||d.sourceArmorStatus()==null||d.sourceArmorStatus().armorType()!=2)continue;
                body=copy(d,Map.of("sourceArmorStatus",copy(d.sourceArmorStatus(),Map.of("classMask",127)),"unitWeight",1));break;
            }
            check(definitions.size()==2&&body!=null,"local native catalog provides two compatible weapons and body armor");
            Path root=Path.of(args[1]);Files.createDirectories(root);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
            try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
                var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
                var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
                var fixtures=List.of(f,peer,mate,other);
                for(int i=0;i<4;i++) {
                    NativeDirectoryRegression.init(fixtures.get(i),"WeaponBuff"+i,0,config.classicServerNames().get(0));
                    var summary=((List<CharacterSummary>)get(fixtures.get(i).session,"characters")).get(0);
                    set(fixtures.get(i).session,"characters",List.of(copy(summary,Map.of("objectId",0x38140001L+i))));
                    set(fixtures.get(i).session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
                }
                FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);
                var live=(BattleAbilityStatus)get(f.session,"abilityStatus");var current=CombatBuffLeases.owned(live);
                for(boolean world:new boolean[]{false,true}) {
                    df.set(null,data);live.clear();set(f.session,"sourceWeaponSuppressed",false);
                    var equipment=copy(SourceEquipmentPhysicalStats.none(),Map.of("itemMeleeDamage",3));set(f.session,"sourceEquipmentMagicStats",new SourceEquipmentMagicStats(equipment,0,0));
                    set(f.session,"learnedSkills",Set.of(8,12,21,44,48,107));set(f.session,"playerHp",1000);set(f.session,"playerMaxHp",1000);set(f.session,"playerMp",1000);set(f.session,"playerMaxMp",1000);
                    for(int i=0;i<fixtures.size();i++){set(fixtures.get(i).session,"positionX",32740+i);set(fixtures.get(i).session,"positionY",32798);}
                    if(world)for(int i=0;i<2;i++) {
                        var session=fixtures.get(i).session;Object phase=get(session,"phase");set(session,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));
                        set(session,"classicWorldMapId",4);set(session,"classicWorldInstanceId",-1);set(session,"positionX",33089+i);set(session,"positionY",32717);
                        hub.enterClassicWorld(session,4,33089+i,32717,0);
                    }
                    var inv=RuntimeInventory.fromCatalogCounts(Map.of(InventoryCatalog.SECOND_LEVEL_DARK_STONE,30,40318L,30));
                    var first=add(inv,0x38141001L,definitions.get(0));var second=add(inv,0x38141002L,definitions.get(1));var armor=add(inv,0x38141003L,body);set(f.session,"inventory",inv);
                    use(f.session,first.objectId());check(((RuntimeInventory)get(f.session,"inventory")).hasEquippedWeapon(),"native first weapon equipped world="+world);
                    int ordinary=attack(f.session,peer.session);check(ordinary>0,"deterministic ordinary hit world="+world);
                    for(int id:new int[]{8,12,48,107})cast(f.session,id,id==8||id==48?f.session.playerObjectId():0x7ffffffeL);
                    check(current.weapon(System.nanoTime()).meleeDamage()==10&&current.weapon(System.nanoTime()).rangedDamage()==0,"native four buff casts produce source player effects world="+world);
                    check(attack(f.session,peer.session)==ordinary+10,"actual attack adds all SQL weapon damage once world="+world);
                    check(((BattleItemWeaponEnchantStatus)get(f.session,"itemWeaponEnchantStatus")).activeLeaseCount(System.nanoTime())==0,"source cast creates no item-owned weapon lease");
                    use(f.session,second.objectId());check(((RuntimeInventory)get(f.session,"inventory")).equippedWeaponItem().objectId()==second.objectId(),"native C_USE_ITEM switches weapon");
                    check(attack(f.session,peer.session)==ordinary+10,"native switched weapon retains same player buff damage world="+world);
                    use(f.session,second.objectId());check(!((RuntimeInventory)get(f.session,"inventory")).hasEquippedWeapon(),"native weapon unequip");
                    long lease=((Map<Integer,Long>)get(current,"deadlines")).get(12);cast(f.session,12,first.objectId());
                    check(((Map<Integer,Long>)get(current,"deadlines")).get(12)==lease&&(int)get(f.session,"playerMp")<1000,"unequipped inventory weapon cannot admit source enchant; cost still charged");
                    check(current.weapon(System.nanoTime()).meleeDamage()==10,"unequipping does not remove player BUFF");
                    use(f.session,first.objectId());int bareAc=f.session.battleArmorClass();cast(f.session,21,armor.objectId());
                    check(!current.contains(21,System.nanoTime()),"unequipped body armor cannot admit source armor BUFF");
                    use(f.session,armor.objectId());int equippedAc=f.session.battleArmorClass();cast(f.session,21,first.objectId());
                    check(current.contains(21,System.nanoTime())&&f.session.battleArmorClass()==equippedAc-3,"source armor buff uses equipped body despite unrelated inventory selection");
                    use(f.session,armor.objectId());check(f.session.battleArmorClass()==bareAc-3,"armor buff remains on player after native unequip");
                    cast(f.session,44,f.session.playerObjectId());
                    check(current.weapon(System.nanoTime()).meleeDamage()==7&&current.weapon(System.nanoTime()).meleeHit()==0&&current.armor(System.nanoTime())==-3,"actual Cancellation keeps SQL-immune12/107/21 only");
                    check(attack(f.session,peer.session)==ordinary+7,"actual post-Cancellation attack uses retained source buffs world="+world);
                    var abilities=new HashMap<>(data.controls().abilities());abilities.put(12,Map.of("DMG",4));abilities.put(107,Map.of("DMG",9));abilities.put(21,Map.of("AC",6));
                    df.set(null,controls(data,abilities,true,data.controls().cancellationImmune()));
                    check(attack(f.session,peer.session)==ordinary+13&&f.session.battleArmorClass()==bareAc+6,"altered signed SQL player buffs reach real physical damage and AC");
                    current.put(12,System.nanoTime()-3_000_000_000L,1);check(attack(f.session,peer.session)==ordinary+9,"actual physical attack excludes expired enchant while Fang remains");
                    var timed=(BattleTimedStatus)get(f.session,"timedStatus");timed.activateWeakness(System.nanoTime(),60);timed.activateDisease(System.nanoTime(),60);
                    check(attack(f.session,peer.session)==ordinary+9-11&&f.session.battleArmorClass()==bareAc+6+12,"actual Weakness/Disease SQL damage and AC, applied once world="+world);
                    var burning=(BattleBurningWeaponStatus)get(f.session,"burningWeaponStatus");burning.activate(System.nanoTime(),60,3,6);
                    var breaker=(BattleArmBreakerStatus)get(f.session,"armBreakerStatus");breaker.activateIfAbsent(System.nanoTime(),7);
                    check(attack(f.session,peer.session)==ordinary+9-11+6-2,"actual Burning Weapon and canonical Arm Breaker SQL damage world="+world);
                    abilities.put(47,Map.of("DMG",2,"HIT",3));abilities.put(56,Map.of("DMG",-1,"AC",-5));abilities.put(163,Map.of("DMG",10,"HIT",-2));abilities.put(213,Map.of("DMG",-4));
                    df.set(null,controls(data,abilities,true,data.controls().cancellationImmune()));
                    check(attack(f.session,peer.session)==ordinary+9+2-1+10-4&&f.session.battleArmorClass()==bareAc+6-5,"altered remaining SQL ability rows reach actual damage/AC world="+world);
                    check(burning.snapshot(System.nanoTime()).meleeHit()==-2&&CombatWeaponBuffs.weaponModifier((BattleWeaponBuffStatus)get(f.session,"weaponBuffStatus"),System.nanoTime(),f.session).meleeHit()==3,"altered signed HIT rows reach real hit projections");
                    df.set(null,controls(data,abilities,true,Set.of()));cast(f.session,44,f.session.playerObjectId());
                    check(current.weapon(System.nanoTime()).meleeDamage()==0&&current.armor(System.nanoTime())==0,"altered immunity table reaches actual Cancellation cast");
                    df.set(null,data);cast(f.session,12,0x7ffffffeL);cast(f.session,107,0x7ffffffeL);
                }
                Object phase=get(f.session,"phase");set(f.session,"phase",Enum.valueOf((Class)phase.getClass(),"BATTLE"));
                set(peer.session,"phase",get(f.session,"phase"));var before=((RuntimeInventory)get(f.session,"inventory")).catalogCounts();
                call(f.session,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},100000,peer.session,false);
                check(current.weapon(System.nanoTime()).meleeDamage()==0&&((Map<?,?>)get(current,"deadlines")).isEmpty(),"native death removes even normally immune player weapon buffs");
                check(before.equals(((RuntimeInventory)get(f.session,"inventory")).catalogCounts()),"native death retains material/inventory counts");
                check(!CombatBuffLeases.owned((BattleAbilityStatus)get(peer.session,"abilityStatus")).contains(107,System.nanoTime()),"player weapon BUFF state cannot leak to opponent");
            }
        } finally {df.set(null,data);}
        System.out.println("PASS source player weapon BUFF checks="+checks+"; native BATTLE/WORLD casts, real equipment changes/damage, SQL abilities/immunity, independent expiry, cancellation and death; no SQL writes/deployment");
    }
}
