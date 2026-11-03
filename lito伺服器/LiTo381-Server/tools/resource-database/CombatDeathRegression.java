package local.lito381.server;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Fatal BATTLE/WORLD damage: no death inventory, skill or XP penalty. */
public class CombatDeathRegression {
    static void check(boolean v,String name) { if(!v)throw new AssertionError(name); }
    static Object get(Object o,String n)throws Exception { return NativeDirectoryRegression.get(o,n); }
    static void set(Object o,String n,Object v)throws Exception { NativeDirectoryRegression.set(o,n,v); }
    static CharacterSummary character(CharacterSummary original,int type)throws Exception {
        var fields=CharacterSummary.class.getRecordComponents();
        var types=new Class<?>[fields.length];var args=new Object[fields.length];
        for(int i=0;i<fields.length;i++) {
            types[i]=fields[i].getType();args[i]=fields[i].getAccessor().invoke(original);
            switch(fields[i].getName()) {
                case "type" -> args[i]=type;
                case "experience" -> args[i]=1234567L;
                case "lawful" -> args[i]=-32768L;
                case "pkCount" -> args[i]=100;
            }
        }
        var constructor=CharacterSummary.class.getDeclaredConstructor(types);
        constructor.setAccessible(true);return constructor.newInstance(args);
    }
    public static void main(String[] a)throws Exception {
        var config=ServerConfig.load(Path.of(a[0]),"standard");ResourceDatabase.load(config);
        var dataField=CombatDatabase.class.getDeclaredField("current");dataField.setAccessible(true);
        var data=(CombatDatabase.Snapshot)dataField.get(null);var altered=new HashMap<>(data.skills());
        var immune=altered.get(68);altered.put(68,new CombatDatabase.Skill(immune.mp(),immune.hp(),immune.reuse(),immune.duration(),
                immune.damage(),immune.die(),immune.dice(),0,0));dataField.set(null,data.withSkills(altered));
        Path root=Path.of(a[1]);Files.createDirectories(root);
        var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
        var state=new GameStateStore(root.resolve("unused"),false,true);
        try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
            var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
            var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
            var fixtures=List.of(f,peer,mate,other);
            for(int i=0;i<fixtures.size();i++)NativeDirectoryRegression.init(fixtures.get(i),"Death"+i,0,config.classicServerNames().get(0));
            var current=((List<CharacterSummary>)get(f.session,"characters")).get(0);
            var damage=ClientSession.class.getDeclaredMethod("takePlayerDamage",int.class,ClientSession.class,boolean.class);
            damage.setAccessible(true);
            for(int type=0;type<7;type++) {
                var summary=character(current,type);set(f.session,"characters",List.of(summary));
                long room=FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);
                for(var fixture:fixtures) {
                    set(fixture.session,"playerHp",1000);set(fixture.session,"playerMaxHp",1000);
                    set(fixture.session,"playerMp",1000);set(fixture.session,"playerMaxMp",1000);
                    set(fixture.session,"positionX",32740);set(fixture.session,"positionY",32798);
                }
                var inventory=RuntimeInventory.fromCatalogCounts(Map.of(40318L,7,40308L,1500));
                set(f.session,"inventory",inventory);
                var before=List.copyOf(inventory.items());
                set(f.session,"learnedSkills",Set.of(68));((Map<?,?>)get(f.session,"spellReadyNanos")).clear();
                NativeDirectoryRegression.call(f.session,"useSpell",new Class[]{byte[].class},
                        new Protocol.Writer().c(0x14).c(8).c(3).d(f.session.playerObjectId()).bytes());
                check((int)get(f.session,"playerMp")<1000,"zero-reagent SQL spell did not cast for class "+type);
                check(((BattleAbilityStatus)get(f.session,"abilityStatus")).immuneToHarm(System.nanoTime()).divisor()!=0,
                        "zero-reagent SQL protection effect missing for class "+type);
                check(((RuntimeInventory)get(f.session,"inventory")).items().equals(before),"zero-reagent spell debited inventory");
                Object phase=get(f.session,"phase");
                set(f.session,"phase",Enum.valueOf((Class)phase.getClass(),"WORLD"));
                set(f.session,"classicWorldMapId",4);set(f.session,"positionX",33089);set(f.session,"positionY",32717);
                set(f.session,"playerLawful",-32768);set(f.session,"playerClassicExperience",1234567L);
                var learned=Set.of(1,68,100,181);set(f.session,"learnedSkills",learned);
                NativeDirectoryRegression.call(f.session,"takeClassicWorldDirectDamage",new Class[]{double.class,long.class},100000d,0x7ffffffeL);
                check((int)get(f.session,"playerHp")==0,"WORLD fatal damage not lethal for class "+type);
                check(((RuntimeInventory)get(f.session,"inventory")).items().equals(before),"WORLD death lost inventory for class "+type);
                check((long)get(f.session,"playerClassicExperience")==1234567L&&get(f.session,"learnedSkills").equals(learned),"WORLD death lost runtime experience/skills for class "+type);
                var worldAfter=((List<CharacterSummary>)get(f.session,"characters")).get(0);
                check(worldAfter.experience()==1234567L&&worldAfter.level()==summary.level(),"WORLD death changed character experience/level for class "+type);
                check((int)get(f.session,"playerLawful")==-32768,"WORLD death altered lawful for non-guard attacker");
                set(f.session,"phase",phase);set(f.session,"playerHp",1000);set(f.session,"positionX",32740);set(f.session,"positionY",32798);
                damage.invoke(f.session,100000,peer.session,false);
                check((int)get(f.session,"playerHp")==0,"fatal damage not lethal for class "+type);
                check(((RuntimeInventory)get(f.session,"inventory")).items().equals(before),"death lost inventory for class "+type);
                var after=((List<CharacterSummary>)get(f.session,"characters")).get(0);
                check(after.experience()==1234567L && after.level()==summary.level(),"death lost experience/level for class "+type);
                check(after.lawful()==-32768L&&after.pkCount()==100,"arena death altered source lawful/PK for class "+type);
                check((boolean)get(FlowRegression.room(hub,room),"battleStarted"),"death ended match despite surviving teammate");
                check(!(boolean)get(f.session,"resultRecorded"),"death settled prematurely for class "+type);
                hub.endBattle(room);
                for(var fixture:fixtures)hub.leaveRoom(room,fixture.session);
            }
            System.out.println("PASS altered SQL itemConsumeId=0/Count=0 actual protection casts across all 7 classes: MP charged, buff applied, no inventory debit; fatal BATTLE PvP and WORLD NPC death at lawful=-32768/PK=100 retain inventory, adena, materials, learned skills, runtime/character experience and level; PK/lawful unchanged; surviving teammate continues; no premature settlement; memory repository only");
        }
    }
}
