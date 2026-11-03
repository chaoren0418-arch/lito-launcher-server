package local.lito381.server;
import java.nio.file.*;
import java.util.*;

/** Native casts under independent source class coefficients and noncommuting reductions. */
public class ClassMagicRegression extends WeaponBuffRegression {
    static void casterType(ClientSession caster,int type)throws Exception {
        var summary=((List<CharacterSummary>)get(caster,"characters")).get(0);set(caster,"characters",List.of(copy(summary,Map.of("type",type))));
        if(type==1) {
            InventoryCatalog.Definition weapon=null;for(int id=1;id<300;id++){var d=InventoryCatalog.find(id);if(d!=null&&d.weapon()&&(d.weaponWalkAction()==50||d.weaponWalkAction()==54)){weapon=copy(d,Map.of("sourceWeaponStatus",copy(d.sourceWeaponStatus(),Map.of("classMask",127)),"unitWeight",1));break;}}
            check(weapon!=null,"native Shock Stun two-handed weapon fixture");var inv=RuntimeInventory.fromCatalogCounts(Map.of());var item=add(inv,0x381a1001L,weapon);set(caster,"inventory",inv);use(caster,item.objectId());
        }
        set(caster,"sourceEquipmentMagicStats",copy(SourceEquipmentMagicStats.none(),Map.of("itemMagicDamage",37)));
    }
    static int castHit(ClientSession caster,ClientSession target,int skill,boolean reflected)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();set(caster,"playerMp",1000);set(caster,"playerMaxMp",1000);set(caster,"playerHp",5000);set(caster,"playerMaxHp",5000);set(caster,"playerLawful",32767);
        set(target,"playerHp",5000);set(target,"playerMaxHp",5000);set(target,"playerMp",1000);set(target,"playerMaxMp",1000);
        ((BattleTimedStatus)get(target,"timedStatus")).clear();set(caster,"lastCombatTargetObjectId",target.playerObjectId());
        var w=new Protocol.Writer().c(0x14).c((skill-1)/8).c((skill-1)%8);
        var area=BattleSpellCatalog.areaAttackSpell(skill);var freeze=BattleSpellCatalog.freezeAttackSpell(skill);
        boolean selector=area!=null&&(area.maximumRange()==0||area.requestShape()==BattleSpellCatalog.RequestShape.SELECTOR_ONLY)
                ||freeze!=null&&(freeze.areaRadius()>0||freeze.lineTargeted());
        if(skill>=175)selector=false; // existing explicit-target Dragon/Illusion dispatcher forwards selector-only internally
        if(!selector)w.d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY"));
        call(caster,"useSpell",new Class[]{byte[].class},w.bytes());
        return 5000-(int)get(reflected?caster:target,"playerHp");
    }
    static int expected(int raw,double factor,boolean critical,int mr,int mode) {
        int value=(int)(raw*factor);if(critical)value*=2;
        // Independent source MR80 oracle: floor=3, coefficient=.9, original hit=0.
        double coefficient=mr==80?0.64:mr==0?1:Double.NaN;
        return Math.max(0,(int)(Math.max(value,0)*coefficient))*mode;
    }
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var rules=(CombatRules.Snapshot)rf.get(null);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        String[] names={"Crown","Knight","Elf","Wizard","Darkelf","DragonKnight","Illusionist"};
        check(CombatMagicDamage.keys().size()==7&&rules.values().size()==58,"seven source PC magic keys loaded alongside physical keys");
        var stun=new BattleShockStunEffects();long now=System.nanoTime();stun.place(42,32740,32798,now,5);check(stun.active(now+4_000_000_000L).size()==1&&stun.active(now+5_000_000_000L).isEmpty(),"source five-second Shock Stun visual exact duration");
        stun.place(42,32740,32798,now,65535);boolean rejected=false;try{stun.place(42,32740,32798,now,65536);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Shock Stun visual uses same unsigned-short bound as source control and lease");
        var ones=new HashMap<String,Double>();for(String name:names){String key="Other_ADD_is"+name+"_MagicPC";check(CombatMagicDamage.keys().contains(key)&&rules.values().get(key)==1,"seven exact class property identities="+name);ones.put(key,1.0);}
        var base=ClassDamageRegression.with(rules,ones);var skills=new HashMap<>(data.skills());
        for(String key:CombatMagicDamage.keys()) {
            var missing=CombatRules.read(rules.damageFile());missing.remove(key);var loaded=CombatRules.snapshot(CombatRules.read(rules.skillFile()),missing,rules.skillFile(),rules.damageFile());check(loaded.values().get(key)==1.0&&loaded.defaultedKeys().contains(key),"actual source magic loader missing key default1="+key);
            missing.setProperty(key,"bad");loaded=CombatRules.snapshot(CombatRules.read(rules.skillFile()),missing,rules.skillFile(),rules.damageFile());check(loaded.values().get(key)==1.0&&loaded.defaultedKeys().contains(key),"actual source magic loader malformed default1="+key);
            missing.remove(key);missing.setProperty(key,"Infinity");boolean invalid=false;try{CombatRules.snapshot(CombatRules.read(rules.skillFile()),missing,rules.skillFile(),rules.damageFile());}catch(java.io.IOException expected){invalid=true;}check(invalid,"actual source magic loader nonfinite rejected="+key);
        }
        var ids=Set.of(4,17,25,53,50,80,194,77,87,108,203,207,208,213);
        for(int id:ids){var old=skills.get(id);skills.put(id,new CombatDatabase.Skill(old.mp(),old.hp(),old.reuse(),old.duration(),0,1,0,0,0,old.probabilityDice(),old.probabilityValue()));}
        df.set(null,data.withSkills(skills));Path root=Path.of(args[1]);Files.createDirectories(root);
        var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"native fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);for(int n=0;n<4;n++) {
                var actor=fixtures.get(n).session;NativeDirectoryRegression.init(fixtures.get(n),"ClassMagic"+n,0,config.classicServerNames().get(0));
                var summary=((List<CharacterSummary>)get(actor,"characters")).get(0);set(actor,"characters",List.of(copy(summary,Map.of("objectId",0x381a0001L+n,"intelligence",9,"originalIntelligence",9,"wisdom",9,"originalWisdom",9))));
                set(actor,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());set(actor,"sourceBaseMagicResistance",new SourceBaseMagicResistance(0,0,0,0,0));set(actor,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());
            }
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);var caster=f.session;var target=peer.session;
            set(caster,"learnedSkills",ids);set(caster,"sourceEquipmentMagicStats",copy(SourceEquipmentMagicStats.none(),Map.of("itemMagicDamage",37)));
            set(target,"sourceEquipmentMagicStats",copy(SourceEquipmentMagicStats.none(),Map.of("itemMagicResistance",80,"armorTemplateDamageReduction",5)));
            check((int)call(target,"effectiveBaseMagicResistance",new Class[]{})==80,"qualified actual MR80, armor5 fixture");
            for(boolean world:new boolean[]{false,true}) {
                for(int n=0;n<4;n++){var actor=fixtures.get(n).session;set(actor,"positionX",world?33089+n:32740+n);set(actor,"positionY",world?32717:32798);if(world&&n<2){Object phase=get(actor,"phase");set(actor,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));set(actor,"classicWorldMapId",4);set(actor,"classicWorldInstanceId",-1);hub.enterClassicWorld(actor,4,33089+n,32717,0);}}
                for(int type=0;type<7;type++) {
                    var summary=((List<CharacterSummary>)get(caster,"characters")).get(0);set(caster,"characters",List.of(copy(summary,Map.of("type",type))));
                    check((int)call(caster,"originalMagicHit",new Class[]{})==0,"original hit fixture class="+type);
                    for(double factor:new double[]{1,0.4,1.25,2.75,0,-1}) {
                        rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_is"+names[type]+"_MagicPC",factor)));
                        int dealt=castHit(caster,target,4,false);check(dealt==expected(32,factor,false,80,1),"native class magic cast/truncation type="+type+" WORLD="+world+" factor="+factor+" actual="+dealt);
                    }
                }
                var summary=((List<CharacterSummary>)get(caster,"characters")).get(0);set(caster,"characters",List.of(copy(summary,Map.of("type",3))));
                for(int id:new int[]{17,25,53,50,80,194,87,203,207,208,213}) {
                    casterType(caster,id==87?1:3);
                    rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.4,"Other_ADD_isKnight_MagicPC",0.4)));
                    int actual=castHit(caster,target,id,false),want=expected(32,0.4,false,80,id==207?4:1);
                    check(actual==want,"native family coefficient/MR order skill="+id+" WORLD="+world+" expected="+want+" actual="+actual);
                }
                for(boolean critical:new boolean[]{false,true}) {
                    casterType(caster,3);
                    rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.4,"DISINTEGRATE_CRITICAL_RND",critical?100.0:0.0)));
                    check(castHit(caster,target,77,false)==expected(32,0.4,critical,80,1),"source class cast before Disintegrate double before MR WORLD="+world+" critical="+critical);
                }
                ((BattleAbilityStatus)get(target,"abilityStatus")).activateImmuneToHarm(System.nanoTime(),64,2);
                rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.4,"DISINTEGRATE_CRITICAL_RND",100.0)));
                check(castHit(caster,target,77,false)==expected(16,0.4,true,80,1),"source immune before class and critical and MR WORLD="+world);
                check(castHit(caster,target,207,false)==expected(16,0.4,false,80,4),"Mind Break reduces before class/MR then mode4, no duplicate reduction WORLD="+world);
                check(castHit(caster,target,108,false)==expected(497,0.4,false,80,1),"Final Burn source MP1000 minus armor5, immune, class then MR WORLD="+world);
                check((int)get(caster,"playerHp")==1&&(int)get(caster,"playerMp")==0,"Final Burn native HP/MP commit retained WORLD="+world);
                var trigger=new SourceMagicDollPower(1,"fixture","ACTIVE_SKILL",0,100,0,0,0,0,0,0,0,25,0,0,0);
                int proc=(int)call(caster,"nativeMagicDollTriggeredSkillDamage",new Class[]{SourceMagicDollPower.class,ClientSession.class,SourceNpcPhysicalStats.class},trigger,target,null);
                check(proc==expected(16,0.4,false,80,1),"native magic-doll calculation includes immunity before class/MR WORLD="+world);
                int npcProc=(int)call(caster,"nativeMagicDollTriggeredSkillDamage",new Class[]{SourceMagicDollPower.class,ClientSession.class,SourceNpcPhysicalStats.class},trigger,null,SourceNpcPhysicalStats.memoryFixture(45065));
                rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.0)));
                check((int)call(caster,"nativeMagicDollTriggeredSkillDamage",new Class[]{SourceMagicDollPower.class,ClientSession.class,SourceNpcPhysicalStats.class},trigger,null,SourceNpcPhysicalStats.memoryFixture(45065))==npcProc,"PC magic multiplier does not change native NPC doll damage WORLD="+world);
                ((BattleAbilityStatus)get(target,"abilityStatus")).clear();
                var targetSummary=((List<CharacterSummary>)get(target,"characters")).get(0);set(target,"characters",List.of(copy(targetSummary,Map.of("wisdom",255))));
                for(int id:new int[]{4,77,87}) {
                    casterType(caster,id==87?1:3);
                    ((BattleCounterMirrorStatus)get(target,"counterMirrorStatus")).activate(System.nanoTime(),32);
                    rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.0,"Other_ADD_isKnight_MagicPC",0.0,"DISINTEGRATE_CRITICAL_RND",100.0)));
                    check(castHit(caster,target,id,true)==32,"reflection retains pre-class/pre-critical/pre-MR damage with zero coefficient skill="+id+" WORLD="+world);
                    check((int)get(target,"playerHp")==5000,"reflected spell does not also damage target skill="+id);
                }
                casterType(caster,3);((BattleCounterMirrorStatus)get(target,"counterMirrorStatus")).activate(System.nanoTime(),32);set(caster,"playerHp",5000);
                check((int)call(caster,"nativeMagicDollTriggeredSkillDamage",new Class[]{SourceMagicDollPower.class,ClientSession.class,SourceNpcPhysicalStats.class},trigger,target,null)==0&&(int)get(caster,"playerHp")==4968,"native doll mirror uses pre-coefficient/pre-MR32 WORLD="+world);
                set(target,"characters",List.of(targetSummary));
                ((BattleCounterMirrorStatus)get(target,"counterMirrorStatus")).activate(System.nanoTime(),32);set(caster,"playerMp",0);set(target,"playerHp",5000);((Map<?,?>)get(caster,"spellReadyNanos")).clear();
                call(caster,"useSpell",new Class[]{byte[].class},new Protocol.Writer().c(0x14).c(0).c(3).d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY")).bytes());
                check((int)get(target,"playerHp")==5000&&((BattleCounterMirrorStatus)get(target,"counterMirrorStatus")).active(System.nanoTime())&&(int)get(caster,"playerMp")==0,"failed MP admission leaves target mirror and HP intact WORLD="+world);((BattleCounterMirrorStatus)get(target,"counterMirrorStatus")).clear();
            }
            rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",0.4)));
            casterType(caster,3);
            var snapshot=CombatMagicDamage.attempt(32,80,0);check(CombatMagicDamage.finish(32,snapshot,caster,false)==7,"immutable MR attempt independent of subsequent target changes");
            rf.set(null,ClassDamageRegression.with(base,Map.of("Other_ADD_isWizard_MagicPC",Double.MAX_VALUE)));
            check(CombatMagicDamage.finish(32,snapshot,caster,false)==(int)(Integer.MAX_VALUE*0.64)&&CombatMagicDamage.finish(32,snapshot,caster,true)==0,"source double-to-int saturation and subsequent int critical overflow");
            for(var component:CombatMagicDamage.Attempt.class.getRecordComponents())check(component.getType().isPrimitive()||component.getType()==BattleFinalBurn.Result.class,"attempt contains values only="+component.getName());
        } finally {rf.set(null,rules);df.set(null,data);}
        System.out.println("PASS source class PC magic checks="+checks+"; seven classes, native BATTLE/WORLD casts, damage families, mirror/class/critical/MR order, Mind Break reduction; NPC and full source parity remain separately tracked; no SQL writes/deployment");
    }
}
