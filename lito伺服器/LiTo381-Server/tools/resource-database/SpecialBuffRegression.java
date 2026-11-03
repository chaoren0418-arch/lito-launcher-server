package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Source blessing and eye data through native actor and item paths. */
public class SpecialBuffRegression extends WeaponBuffRegression {
    static SourceEyeControls.Bundle readRows(List<Map<String,Object>> configs,List<Map<String,Object>> abilities)throws Exception {
        var connection=(java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(SpecialBuffRegression.class.getClassLoader(),new Class[]{java.sql.Connection.class},(p,m,a)->{
            if(!m.getName().equals("createStatement"))throw new AssertionError("unexpected SQL connection operation="+m.getName());
            return java.lang.reflect.Proxy.newProxyInstance(SpecialBuffRegression.class.getClassLoader(),new Class[]{java.sql.Statement.class},(p2,m2,a2)->{
                if(m2.getName().equals("close"))return null;if(!m2.getName().equals("executeQuery")||!((String)a2[0]).startsWith("SELECT "))throw new AssertionError("non-read-only eye loader");
                var rows=((String)a2[0]).contains("設置")?configs:abilities;var cursor=new int[]{-1};
                return java.lang.reflect.Proxy.newProxyInstance(SpecialBuffRegression.class.getClassLoader(),new Class[]{java.sql.ResultSet.class},(p3,m3,a3)->{
                    if(m3.getName().equals("close"))return null;if(m3.getName().equals("next"))return ++cursor[0]<rows.size();
                    var value=rows.get(cursor[0]).get((String)a3[0]);return switch(m3.getName()){
                        case "getLong"->value==null?0L:((Number)value).longValue();case "getInt"->value==null?0:((Number)value).intValue();
                        case "getBoolean"->value!=null&&((Number)value).intValue()!=0;case "getString"->value==null?null:value.toString();default->throw new AssertionError("unexpected eye result read");};
                });
            });
        });return SourceEyeControls.read(connection);
    }
    static void loaderVectors(CombatDatabase.Snapshot data)throws Exception {
        var rows=new ArrayList<Map<String,Object>>();var wide=new ArrayList<Map<String,Object>>();
        for(long item=42518;item<=42524;item++) {
            var c=data.controls().eyes().get(item);var row=new HashMap<String,Object>();
            row.put("item_id",item);row.put("buff_id",c.buff());row.put("buff_time_sec",0);row.put("cast_gfx",c.gfx());row.put("status_icon",c.icon());row.put("group_check","FIRST, dragon_eye");
            row.put("delay_id",8);row.put("delay_time",1250);row.put("delay_effect",2300);rows.add(row);wide.add(new HashMap<>(Map.of("item_id",item,"dmg",2)));
        }
        var read=readRows(rows,wide);check(read.configs().get(42518L).seconds()==600&&read.configs().get(42518L).sharedMillis()==1250&&read.configs().get(42518L).itemMillis()==2300,"source loader zero timer fallback and raw millisecond units");
        check(read.abilities().get(6683).get("DMG")==2,"source loader binds wide item row to configured skill ID");
        for(var change:Map.<String,Object>of("buff_time_sec",-1,"consume_count",1,"use_custom_packet",1,"group_check","DISJOINT").entrySet()) {
            var altered=new ArrayList<Map<String,Object>>(rows);var row=new HashMap<>(altered.get(0));row.put(change.getKey(),change.getValue());altered.set(0,row);boolean rejected=false;
            try{readRows(altered,wide);}catch(java.sql.SQLException expected){rejected=true;}check(rejected,"source profile rejects unimplemented condition="+change.getKey());
        }
        var badWide=new ArrayList<Map<String,Object>>(wide);badWide.set(0,new HashMap<>(Map.of("item_id",42518L,"dodge",1)));boolean rejected=false;
        try{readRows(rows,badWide);}catch(java.sql.SQLException expected){rejected=true;}check(rejected,"unimplemented nonzero item ability rejects partial application");
        rejected=false;try{readRows(rows.subList(0,6),wide);}catch(java.sql.SQLException expected){rejected=true;}check(rejected,"missing eye profile is diagnosed");
    }
    static int scalar(ClientSession actor,String method,long now)throws Exception{return (int)call(actor,method,new Class[]{long.class},now);}
    static CombatDatabase.Snapshot snapshot(CombatDatabase.Snapshot data,Map<Integer,Map<String,Integer>> abilities,Map<Long,SourceEyeControls.Config> eyes) {
        var c=data.controls();return new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(c.probabilities(),c.bossModifiers(),abilities,c.npcAttributes(),c.skillCoexist(),c.cancellationImmune(),eyes,c.blessings()));
    }
    static void rebuild(ClientSession actor)throws Exception{call(actor,"rebuildMaximumVitals",new Class[]{long.class},System.nanoTime());}
    public static void main(String[] args)throws Exception {
        var config=copy(ServerConfig.load(Path.of(args[0]),"standard"),Map.of("battlePhysicalHitEnabled",true,"uncannyDodgeHitFixtureD20",20,"uncannyDodgeHitFixtureD100",1));
        ResourceDatabase.load(config);var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        loaderVectors(data);
        check(data.controls().eyes().size()==7&&data.controls().abilities().size()==45,"seven source profiles and forty-five combined ability IDs");
        check(SourceSkillControls.ability(data.controls(),6683,"DMG")==4&&SourceSkillControls.ability(data.controls(),6687,"DMG")==4,"source skill and wide item DMG rows add rather than replace");
        var merged=SourceEyeControls.merge(Map.of(1,Map.of("DMG",-2)),Map.of(1,Map.of("DMG",5,"HIT",-3)));
        check(merged.get(1).equals(Map.of("DMG",3,"HIT",-3)),"independent signed two-table merge");
        boolean immutable=false;try{data.controls().eyes().clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"eye profiles immutable");
        int[] buffs={6684,6685,6686,6683,6688,6689,6687};
        for(int i=0;i<7;i++) {
            var eye=BattleDragonEyeCatalog.require(42518L+i);var sql=data.controls().eyes().get(eye.itemId());
            check(eye.buffId()==buffs[i]&&eye.durationSeconds()==600&&eye.castGfx()==sql.gfx()&&eye.statusIcon()==46+i,"source eye IDs/timer/wire settings item="+eye.itemId());
            check(eye.sharedDelayId()==8&&eye.sharedDelayMillis()==600&&eye.itemDelayMillis()==60000&&!eye.canOverwrite(),"source item delays retain milliseconds item="+eye.itemId());
            var lease=new BattleDragonEyeStatus();long t=1_000_000_000L;
            check(lease.activate(100+i,eye,t).applied()&&lease.snapshot(t).active()&&!lease.snapshot(t+600_000_000_000L).active(),"exact source eye expiry item="+eye.itemId());
            check(lease.activate(100+i,eye,t+59_999_999_999L).outcome()==BattleDragonEyeStatus.Outcome.ITEM_COOLDOWN,"per object source delay item="+eye.itemId());
            check(lease.activate(200+i,eye,t+600_000_000L).outcome()==BattleDragonEyeStatus.Outcome.GROUP_CONFLICT,"source non-overwrite group item="+eye.itemId());
        }
        check(BattleDragonEyeCatalog.require(42518).evasionRate()==10&&BattleDragonEyeCatalog.require(42518).stoneResistance()==3,"earth eye wide abilities");
        check(BattleDragonEyeCatalog.require(42519).damageReduction()==3&&BattleDragonEyeCatalog.require(42519).freezeResistance()==3,"water eye wide abilities");
        check(BattleDragonEyeCatalog.require(42520).magicHit()==3&&BattleDragonEyeCatalog.require(42520).sleepResistance()==3,"wind eye wide abilities");
        check(BattleDragonEyeCatalog.require(42524).poisonResistance()==1&&BattleDragonEyeCatalog.require(42524).damageReduction()==50,"life eye wide abilities");
        Path root=Path.of(args[1]);Files.createDirectories(root);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);
            for(int i=0;i<4;i++) {
                NativeDirectoryRegression.init(fixtures.get(i),"SpecialBuff"+i,0,config.classicServerNames().get(0));
                var summary=((List<CharacterSummary>)get(fixtures.get(i).session,"characters")).get(0);
                set(fixtures.get(i).session,"characters",List.of(copy(summary,Map.of("objectId",0x38160001L+i))));
                set(fixtures.get(i).session,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
            }
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);
            var caster=f.session;var story=CombatSpecialBuffs.story(caster);var eye=(BattleDragonEyeStatus)get(caster,"dragonEyeStatus");
            var inv=RuntimeInventory.fromCatalogCounts(Map.of(42521L,1,42524L,1,42520L,1,40318L,20));
            InventoryCatalog.Definition weapon=null;
            for(int id=1;id<300;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&d.sourceWeaponStatus()!=null){weapon=copy(d,Map.of("sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("damageSmall",1,"damageLarge",1,"hitModifier",0,"damageModifier",0,"classMask",127)),"unitWeight",1));break;}}
            var item=add(inv,0x38161001L,weapon);set(caster,"inventory",inv);use(caster,item.objectId());
            for(boolean world:new boolean[]{false,true}) {
                if(world)for(int i=0;i<2;i++){var actor=fixtures.get(i).session;Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,33089+i,32717,0);}
                else for(int i=0;i<4;i++){set(fixtures.get(i).session,"positionX",32740+i);set(fixtures.get(i).session,"positionY",32798);}
                story.clear();eye.clear();rebuild(caster);int ordinary=attack(caster,peer.session);check(ordinary>0,"fixed native ordinary hit world="+world);
                int hp=(int)get(caster,"playerMaxHp"),mp=(int)get(caster,"playerMaxMp"),ac=caster.battleArmorClass();long now=System.nanoTime();
                int er=scalar(caster,"effectiveRangedEvasionRate",now),sp=scalar(caster,"spellPowerModifier",now);
                for(int id:new int[]{4009,4010,4016}) {
                    story.clear();check(CombatSpecialBuffs.startBlessing(caster,id,30),"source skill mode admits blessing="+id);
                    int bonusHp=id==4009?100:id==4010?80:150,bonusMp=id==4010?10:50;
                    check((int)get(caster,"playerMaxHp")==hp+bonusHp&&(int)get(caster,"playerMaxMp")==mp+bonusMp,"native maximum vitals source blessing="+id+" world="+world);
                    check(caster.battleArmorClass()==ac+(id==4010?8:id==4016?10:0),"native signed AC source blessing="+id);
                    check(attack(caster,peer.session)==ordinary+(id==4010?0:1),"native melee damage source blessing once="+id);
                    now=System.nanoTime();check(scalar(caster,"waterDragonBlessingHpRegen",now)==(id==4016?0:3)&&scalar(caster,"waterDragonBlessingMpRegen",now)==(id==4016?0:3),"native regen source blessing="+id);
                    check(scalar(caster,"effectiveRangedEvasionRate",now)==er+(id==4009?30:id==4010?15:0),"source direct ER preserved blessing="+id);
                    check(!CombatSpecialBuffs.startBlessing(caster,id,60),"source blessing cannot refresh existing="+id);
                    for(int kind:new int[]{1,2,4,8}) {
                        long resistance=((Number)call(caster,"effectiveSpellElementalResistance",new Class[]{int.class,long.class},kind,now)).longValue();
                        story.clear();long baseline=((Number)call(caster,"effectiveSpellElementalResistance",new Class[]{int.class,long.class},kind,now)).longValue();
                        story.put(id,now+30_000_000_000L);
                        check(resistance-baseline==(id==4016||id==4009&&kind==1||id==4010&&kind==4?30:0),"source direct elemental effect="+id+" kind="+kind);
                    }
                    story.put(id,now-1);check(scalar(caster,"waterDragonBlessingHpRegen",now)==0&&story.containsKey(id),"numeric read excludes expiry without swallowing refresh event");
                    check((boolean)call(caster,"expireWaterDragonBlessing",new Class[]{long.class},now)&&!story.containsKey(id),"native expiry signal covers blessing="+id);rebuild(caster);
                    check((int)get(caster,"playerMaxHp")==hp&&(int)get(caster,"playerMaxMp")==mp,"expiry restores maximum vitals="+id);
                }
                story.clear();rebuild(caster);
                for(long itemId:new long[]{42521,42524,42520}) {
                    eye.clear();var before=inv.catalogCounts();var eyeItem=inv.items().stream().filter(v->v.definition().catalogId()==itemId).findFirst().orElseThrow();
                    use(caster,eyeItem.objectId());now=System.nanoTime();
                    check(eye.snapshot(now).active()&&eye.snapshot(now).eye().itemId()==itemId,"actual C_USE_ITEM eye admits world="+world+" item="+itemId);
                    check(inv.catalogCounts().equals(before),"source eye reusable item remains in ephemeral inventory");
                    check(attack(caster,peer.session)==ordinary+(itemId==42520?0:4),"actual melee adds merged source damage once world="+world+" item="+itemId);
                    check(scalar(caster,"spellPowerModifier",now)==sp+(itemId==42521?0:1),"source direct eye SP preserved item="+itemId);
                    check(CombatWeaponBuffs.weaponModifier((BattleWeaponBuffStatus)get(caster,"weaponBuffStatus"),now,caster).meleeHit()==(itemId==42520?0:1),"source skills eye HIT reaches actor projection");
                }
                eye.clear();use(caster,inv.items().stream().filter(v->v.definition().catalogId()==42521L).findFirst().orElseThrow().objectId());CombatSpecialBuffs.startBlessing(caster,4009,30);
                set(caster,"learnedSkills",Set.of(44));cast(caster,44,caster.playerObjectId());
                check(eye.snapshot(System.nanoTime()).active()&&story.containsKey(4009),"native ordinary Cancellation preserves out-of-range story and eye buffs world="+world);
                check(attack(caster,peer.session)==ordinary+5,"preserved source eye/blessing damage after actual Cancellation");
                story.clear();eye.clear();
            }
            var npc=ClassicWorldNpcCatalog.all(1011).stream().filter(n->n.npcId()==70689).findFirst().orElseThrow();
            set(caster,"classicWorldMapId",1011);set(caster,"positionX",npc.x());set(caster,"positionY",npc.y());
            long now=System.nanoTime();story.put(4009,now+30_000_000_000L);story.put(4016,now+30_000_000_000L);story.put(4018,now+30_000_000_000L);
            call(caster,"applyClassicWorldWaterDragonSaellBlessing",new Class[]{ClassicWorldNpcCatalog.StaticNpc.class},npc);
            check(story.containsKey(4010)&&!story.containsKey(4009)&&!story.containsKey(4016)&&story.containsKey(4018),"native Saell NPC uses source 4016 exclusion and retains unrelated4018");
            long deadline=story.get(4010);call(caster,"applyClassicWorldWaterDragonSaellBlessing",new Class[]{ClassicWorldNpcCatalog.StaticNpc.class},npc);check(story.get(4010)==deadline,"native Saell has no refresh");
            story.clear();eye.clear();rebuild(caster);int hp=(int)get(caster,"playerMaxHp"),ac=caster.battleArmorClass();
            var abilities=new HashMap<>(data.controls().abilities());abilities.put(4010,Map.of("HP",-7,"AC",-4,"HP_REGEN",-2,"DMG",6));abilities.put(6683,Map.of("DMG",9,"HIT",-2,"MAGIC_HIT",8,"SP",3));
            var profiles=new HashMap<>(data.controls().eyes());var old=profiles.get(42521L);profiles.put(42521L,new SourceEyeControls.Config(old.buff(),7,1234,55,9,1250,2300,"DRAGON_EYE",true,"source expiry"));df.set(null,snapshot(data,abilities,profiles));
            check(CombatSpecialBuffs.startBlessing(caster,4010,20)&&(int)get(caster,"playerMaxHp")==hp-7&&caster.battleArmorClass()==ac-4,"altered signed source vitals and AC reach native consumers");
            check(scalar(caster,"waterDragonBlessingHpRegen",System.nanoTime())==-2,"altered signed source regen");
            var fire=BattleDragonEyeCatalog.require(42521);check(fire.durationSeconds()==7&&fire.castGfx()==1234&&fire.statusIcon()==55&&fire.sharedDelayId()==9&&fire.sharedDelayMillis()==1250&&fire.itemDelayMillis()==2300&&fire.canOverwrite()&&fire.endMessage().equals("source expiry"),"altered source item settings reach existing catalog getters");
            use(caster,inv.items().stream().filter(v->v.definition().catalogId()==42521L).findFirst().orElseThrow().objectId());now=System.nanoTime();check(eye.snapshot(now).meleeDamage()==9&&eye.snapshot(now).magicHit()==8,"altered source eye ability snapshot");
            check(scalar(caster,"spellPowerModifier",now)==3,"altered source eye SP reaches native magic modifier");
            check(eye.snapshot(now+7_000_000_000L).active()==false,"altered source eye exact expiry");
            check(eye.activate(999,fire,now+1_250_000_000L).applied(),"altered source overwrite and shared delay");
            df.set(null,data);eye.clear();story.clear();set(caster,"playerHp",1000);CombatSpecialBuffs.startBlessing(caster,4009,30);use(caster,inv.items().stream().filter(v->v.definition().catalogId()==42521L).findFirst().orElseThrow().objectId());
            var retained=inv.catalogCounts();call(caster,"takeClassicWorldDirectDamage",new Class[]{double.class,long.class},100000d,peer.session.playerObjectId());
            check((int)get(caster,"playerHp")==0&&eye.snapshot(System.nanoTime()).active()&&story.containsKey(4009),"native WORLD death retains source out-of-cancellation-range blessing and eye");
            check(inv.catalogCounts().equals(retained),"native WORLD death preserves eye inventory and materials");
            set(caster,"playerHp",1000);Object phase=get(caster,"phase");set(caster,"phase",Enum.valueOf((Class)phase.getClass(),"BATTLE"));
            call(caster,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},100000,peer.session,false);
            check((int)get(caster,"playerHp")==0&&eye.snapshot(System.nanoTime()).active()&&story.containsKey(4009),"native BATTLE death retains source out-of-cancellation-range blessing and eye");
            check(inv.catalogCounts().equals(retained),"native BATTLE death preserves eye inventory and materials");
        } finally {df.set(null,data);}
        System.out.println("PASS source special BUFF checks="+checks+"; native BATTLE/WORLD item/damage/vitals/resists, source two-table additive eyes, Saell exclusion, cancellation, expiry and changed immutable profiles; other blessing NPC producers pending; no SQL writes/deployment");
    }
}
