package local.lito381.server;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Native awakening casts under both source modes and altered SQL abilities. */
public class AwakenCombatRegression {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Object get(Object o,String name)throws Exception{return NativeDirectoryRegression.get(o,name);}
    static void set(Object o,String name,Object value)throws Exception{NativeDirectoryRegression.set(o,name,value);}
    static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{return NativeDirectoryRegression.call(o,name,types,args);}
    static <T>T copy(T value,Map<String,Object> changes)throws Exception {
        var fields=value.getClass().getRecordComponents();var types=new Class<?>[fields.length];var values=new Object[fields.length];
        for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();var m=fields[i].getAccessor();m.setAccessible(true);values[i]=changes.getOrDefault(fields[i].getName(),m.invoke(value));}
        var ctor=value.getClass().getDeclaredConstructor(types);ctor.setAccessible(true);return (T)ctor.newInstance(values);
    }
    static void cast(ClientSession caster,int skill)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();
        byte[] payload=new Protocol.Writer().c(0x14).c((skill-1)/8).c((skill-1)%8).bytes();
        if(skill==1)payload=new Protocol.Writer().c(0x14).c(0).c(0).d(caster.playerObjectId()).bytes();
        call(caster,"useSpell",new Class[]{byte[].class},payload);
    }
    static int[] stats(ClientSession caster,CharacterSummary summary)throws Exception {
        return new int[]{(int)call(caster,"baseStrength",new Class[]{}),(int)call(caster,"baseDexterity",new Class[]{}),
            (int)call(caster,"effectiveConstitution",new Class[]{long.class},System.nanoTime()),
            (int)call(caster,"baseIntelligence",new Class[]{long.class},System.nanoTime()),
            (int)call(caster,"effectiveWisdom",new Class[]{long.class},System.nanoTime()),
            (int)call(caster,"effectiveCharisma",new Class[]{CharacterSummary.class},summary)};
    }
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(config);
        var rf=CombatRules.class.getDeclaredField("current");rf.setAccessible(true);var rules=(CombatRules.Snapshot)rf.get(null);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        check(rules.characterFile()!=null&&Files.isRegularFile(rules.characterFile())&&!CombatAwaken.classicMode(),"current source file explicitly relaxes awakening");
        var defaults=new HashSet<String>();var p=new Properties();var file=Path.of("isolated-charsettings");
        check(CombatRules.awakeClassicMode(p,file,defaults)&&defaults.size()==2,"absent new and legacy keys use both source true defaults");
        p.setProperty("DragonKnightAwakeSkillRestrict","false");defaults.clear();
        check(!CombatRules.awakeClassicMode(p,file,defaults),"legacy restrictions are combined with AND");
        p.setProperty(CombatRules.AWAKE_CLASSIC_MODE," TrUe ");
        check(CombatRules.awakeClassicMode(p,file,new HashSet<>()),"new true key overrides legacy false");
        p.setProperty(CombatRules.AWAKE_CLASSIC_MODE," false ");
        check(!CombatRules.awakeClassicMode(p,file,new HashSet<>()),"new false accepts source whitespace");
        p.setProperty(CombatRules.AWAKE_CLASSIC_MODE,"broken");defaults.clear();
        check(CombatRules.awakeClassicMode(p,file,defaults)&&defaults.equals(Set.of(CombatRules.AWAKE_CLASSIC_MODE)),"malformed explicit new key warns and uses source default true");
        p.setProperty(CombatRules.AWAKE_CLASSIC_MODE,"  ");
        check(!CombatRules.awakeClassicMode(p,file,new HashSet<>()),"blank new key falls back to legacy keys");
        Path root=Path.of(args[1]);Files.createDirectories(root);
        var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();
            check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
            var fixtures=List.of(f,peer,mate,other);
            for(int i=0;i<4;i++) {
                NativeDirectoryRegression.init(fixtures.get(i),"AwakeSource"+i,0,config.classicServerNames().get(0));
                var original=((List<CharacterSummary>)get(fixtures.get(i).session,"characters")).get(0);
                set(fixtures.get(i).session,"characters",List.of(copy(original,Map.of("objectId",0x38120001L+i))));
            }
            var summary=((List<CharacterSummary>)get(f.session,"characters")).get(0);summary=copy(summary,Map.of("type",5));
            set(f.session,"characters",List.of(summary));
            FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);
            for(var fixture:fixtures){set(fixture.session,"positionX",32740);set(fixture.session,"positionY",32798);}
            set(f.session,"learnedSkills",Set.of(1,185,190,195));
            set(f.session,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(49157L,100)));
            var awakening=(BattleAwakenStatus)get(f.session,"awakenStatus");var poly=(BattlePolymorphStatus)get(f.session,"polymorphStatus");
            for(boolean world:new boolean[]{false,true}) {
                if(world)for(var fixture:fixtures) {
                    Object phase=get(fixture.session,"phase");set(fixture.session,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));
                    set(fixture.session,"classicWorldMapId",4);set(fixture.session,"classicWorldInstanceId",-1);
                    set(fixture.session,"positionX",33089);set(fixture.session,"positionY",32717);
                    hub.enterClassicWorld(fixture.session,4,33089,32717,0);
                }
            for(boolean classic:new boolean[]{false,true})for(int skill:new int[]{185,190,195}) {
                var values=new HashMap<>(rules.values());values.put(CombatRules.AWAKE_CLASSIC_MODE,classic?1.0:0.0);
                rf.set(null,new CombatRules.Snapshot(values,rules.skillFile(),rules.damageFile(),rules.characterFile(),rules.defaultedKeys()));
                awakening.clear();poly.clear();call(f.session,"rebuildMaximumVitals",new Class[]{long.class},System.nanoTime());
                int baseHp=(int)get(f.session,"playerMaxHp"),baseAc=f.session.battleArmorClass();int[] base=stats(f.session,summary);
                int baseMr=(int)call(f.session,"magicResistanceModifier",new Class[]{long.class},System.nanoTime());
                int[] elements={1,2,4,8},baseResist=new int[4];
                for(int i=0;i<4;i++)baseResist[i]=(int)call(f.session,"effectiveSpellElementalResistance",new Class[]{int.class},elements[i]);
                set(f.session,"playerHp",baseHp);set(f.session,"playerMp",(int)get(f.session,"playerMaxMp"));
                poly.activate(System.nanoTime(),BattlePolymorphCatalog.DEATH_KNIGHT,1800);
                var before=((RuntimeInventory)get(f.session,"inventory")).catalogCounts();int hp=(int)get(f.session,"playerHp");
                cast(f.session,skill);var live=awakening.snapshot();var spell=BattleSpellCatalog.awakenSpell(skill);
                check(live.skillId()==skill,"native awakening activated "+skill+" classic="+classic);
                check((int)get(f.session,"playerHp")==hp-spell.hpCost(),"activation charges HP once "+skill);
                check(((RuntimeInventory)get(f.session,"inventory")).catalogCounts().get(49157L)==before.get(49157L)-spell.itemConsumeCount(),"activation consumes current SQL materials "+skill);
                check(poly.active(System.nanoTime())==(classic?null:BattlePolymorphCatalog.DEATH_KNIGHT),"source mode controls prior item polymorph "+skill);
                check(f.session.battleGfx()==(classic?spell.dragonGfx():BattlePolymorphCatalog.DEATH_KNIGHT.gfx()),"source mode controls actual actor gfx "+skill);
                check((int)get(f.session,"playerMaxHp")==baseHp+(skill==185?127:0)&&f.session.battleArmorClass()==baseAc+(skill==185?-12:0),"SQL awakening HP/AC on actual actor "+skill);
                int[] active=stats(f.session,summary);for(int i=0;i<6;i++)check(active[i]==base[i]+(skill==195?5:0),"SQL six ability consumers "+skill+" stat="+i);
                check((int)call(f.session,"magicResistanceModifier",new Class[]{long.class},System.nanoTime())==baseMr+(skill==190?30:0),"source Fafurion direct MR retained "+skill);
                for(int i=0;i<4;i++)check((int)call(f.session,"effectiveSpellElementalResistance",new Class[]{int.class},elements[i])==baseResist[i]+(skill==190?30:0),"source Fafurion direct elemental resistance retained "+skill+" element="+elements[i]);
                int mp=(int)get(f.session,"playerMp");long next=live.nextMpDrainAtNanos();
                call(f.session,"serverAwakeningMpDrain",new Class[]{long.class},next+1);
                check((int)get(f.session,"playerMp")==mp-(classic?Math.min(mp,spell.mpDrainAmount()):0),"actual drain respects source mode "+skill);
                if(!classic)check(awakening.snapshot().nextMpDrainAtNanos()==next,"relaxed drain does not advance a hidden timer");
                // An ordinary learned spell reaches the native dispatcher; classic
                // awakening rejects it, relaxed awakening charges and executes it.
                mp=(int)get(f.session,"playerMp");cast(f.session,1);
                check(classic?(int)get(f.session,"playerMp")==mp:(int)get(f.session,"playerMp")<mp,"ordinary spell admission in source mode "+skill);
                hp=(int)get(f.session,"playerHp");mp=(int)get(f.session,"playerMp");before=((RuntimeInventory)get(f.session,"inventory")).catalogCounts();
                cast(f.session,skill==185?190:185);
                check(awakening.snapshot().skillId()==skill&&(int)get(f.session,"playerHp")==hp&&(int)get(f.session,"playerMp")==mp
                        &&before.equals(((RuntimeInventory)get(f.session,"inventory")).catalogCounts()),"different awakening rejected before any cost "+skill);
                cast(f.session,skill);
                check(!awakening.snapshot().active()&&(int)get(f.session,"playerMaxHp")==baseHp&&f.session.battleArmorClass()==baseAc,"same spell toggles off and restores HP/AC "+skill);
                check(Arrays.equals(stats(f.session,summary),base),"toggle removes six abilities "+skill);
                check(poly.active(System.nanoTime())==(classic?null:BattlePolymorphCatalog.DEATH_KNIGHT),"toggle preserves unrelated form only in relaxed mode "+skill);
                check(!((BattleAwakenStatus)get(peer.session,"awakenStatus")).snapshot().active(),"no peer awakening state "+skill);
            }
            }
            for(var fixture:fixtures) {
                Object phase=get(fixture.session,"phase");set(fixture.session,"phase",Enum.valueOf((Class)phase.getClass(),"BATTLE"));
                set(fixture.session,"positionX",32740);set(fixture.session,"positionY",32798);
            }
            var abilities=new HashMap<>(data.controls().abilities());abilities.put(185,Map.of("AC",-17,"HP",211));
            var unequal=Map.of("STR",7,"DEX",-3,"CON",11,"INT",2,"WIS",-4,"CHA",9);abilities.put(195,unequal);
            df.set(null,new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(data.controls().probabilities(),data.controls().bossModifiers(),abilities,data.controls().npcAttributes(),data.controls().skillCoexist(),data.controls().cancellationImmune(),data.controls().eyes(),data.controls().blessings())));
            rf.set(null,rules);poly.clear();call(f.session,"rebuildMaximumVitals",new Class[]{long.class},System.nanoTime());
            int baseHp=(int)get(f.session,"playerMaxHp"),baseAc=f.session.battleArmorClass();int[] base=stats(f.session,summary);
            set(f.session,"playerHp",baseHp);cast(f.session,185);
            check((int)get(f.session,"playerMaxHp")==baseHp+211&&f.session.battleArmorClass()==baseAc-17,"altered SQL HP/AC reach actual native cast");
            set(f.session,"playerHp",baseHp+211);cast(f.session,185);
            check((int)get(f.session,"playerHp")==baseHp,"removing SQL HP buff clamps existing current HP to restored maximum");
            cast(f.session,195);int[] active=stats(f.session,summary),expected={7,-3,11,2,-4,9};
            for(int i=0;i<6;i++)check(active[i]==base[i]+expected[i],"unequal signed SQL ability reaches separate consumer "+i);
            cast(f.session,195);check(Arrays.equals(stats(f.session,summary),base),"unequal SQL abilities removed on same-spell recast");
            cast(f.session,195);call(f.session,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},100000,peer.session,false);
            check(!awakening.snapshot().active()&&(int)get(f.session,"playerHp")==0,"native fatal damage clears active awakening");
            check(Arrays.equals(stats(f.session,summary),base),"death clears signed SQL awakening abilities");
            check(((RuntimeInventory)get(f.session,"inventory")).catalogCounts().get(49157L)>0,"death retains remaining materials");
            set(f.session,"playerHp",baseHp);call(f.session,"serverAwakeningMpDrain",new Class[]{long.class},System.nanoTime()+60_000_000_000L);
            check(!awakening.snapshot().active(),"post-death tick does not resurrect awakening state");
        } finally {rf.set(null,rules);df.set(null,data);}
        System.out.println("PASS native source awakening checks="+checks+"; both modes, all3 forms, SQL abilities, admission, materials, toggle and death; memory actors; no SQL writes/deployment");
    }
}
