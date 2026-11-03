package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Source's 31 frozen selection rows exercise real control-ring cast and level rejection. */
public final class SummonChoicesProbe extends SummonMaterialProbe {
    static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int[] source,boolean low)throws Exception {
        reset(pc,peer,hub,true);
        int select=source[0],npc=source[1],level=source[2],petCost=source[3];
        var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
        set(pc,"characters",List.of(copy(c,Map.of("type",3,"level",level-(low?1:0),"charisma",petCost-6,"originalIntelligence",10))));
        set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);
        set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(51)));
        NativeDeItemPublication.grant(pc,20284,1);
        var inv=pc.nativeDeInventory(0);
        var ring=inv.items().stream().filter(i->i.definition().catalogId()==20284).findFirst().orElseThrow();
        need(!inv.toggleNativeArmor(ring.objectId()).isEmpty()&&inv.controlAbilityEnabled(5),"real equipped summon control ring");
        var row=BattleSpellCatalog.summonSpell(51);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();
        NativeDeItemPublication.grant(pc,item,count+3);selection=select;
        try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)) {
            cast(pc,51,row);
            var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");
            if(low){
                need(pets.isEmpty()&&(int)get(pc,"playerMp")==1000&&(int)get(pc,"playerHp")==4000
                        &&pc.nativeDeInventory(0).countCatalog(item)==count+3,"existing LiTo selection preflight rejects before any accepted cost");
                need(collect(wire).contains(HexFormat.of().formatHex(Protocol.messageCode(743))),"visible Source level failure743");
            }else{
                need(pets.size()==1,"Source CHA+6 equals one selection capacity");
                var pet=pets.get(0);var snapshot=pet.snapshot();
                need(snapshot.npcId()==npc&&snapshot.active(),"actual actor matches Source selection row");
                need(snapshot.petCost()==petCost+(npc>=81238?7:0),"Source special summon stored cost adjustment");
                need((int)get(pc,"playerMp")==1000-cost(pc,51,row)&&(int)get(pc,"playerHp")==4000-row.hpCost()
                        &&pc.nativeDeInventory(0).countCatalog(item)==3,"actual one-cast resources and material");
                ElementalLifecycleProbe.expire(pc,hub,true,pet);collect(wire);
            }
            collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked packets and drained cast receipts");
        }cases++;
    }
    public static void main(String[] args)throws Exception {
        var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate PC");
        var rows=new ArrayList<int[]>();for(var line:Files.readAllLines(Path.of(args[4])))rows.add(Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray());
        need(rows.size()==31,"all Source control-ring rows");
        var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);
        var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
        var root=Path.of(args[1]);Throwable primary=null;
        try{for(var source:rows)for(boolean low:new boolean[]{false,true}){
            var r=root.resolve(source[0]+"-"+low);Files.createDirectories(r);
            var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));
            var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
            try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
                var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
                var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
                var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);
                NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,source,low);
            }need(!Files.exists(r.resolve("must-not-write.properties")),"memory-only source profile");
        }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
        need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
        Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
    }
}
