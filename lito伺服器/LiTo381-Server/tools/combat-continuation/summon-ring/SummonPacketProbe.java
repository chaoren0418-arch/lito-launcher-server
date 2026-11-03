package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Packet boundaries and the two actual Source control-ring item ids. */
public final class SummonPacketProbe extends SummonMaterialProbe {
    static void exercise(ClientSession pc,ClientSession peer,GameHub hub,int mode)throws Exception {
        reset(pc,peer,hub,true);
        var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
        set(pc,"characters",List.of(copy(c,Map.of("type",3,"level",mode>=6?55:99,"charisma",45,"originalIntelligence",10))));
        set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);
        set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(51)));
        if(mode>=4){
            int ringId=mode==4?120284:20284;
            // The memory fixture lacks an import for the alternate id. Supply only its wire view;
            // this checks Source's id-based equipped predicate, not the general item grant/import path.
            if(mode==4)need(pc.nativeDeInventory(0).add(copy(InventoryCatalog.require(20284),Map.of("catalogId",120284L)),1),"fixture alternate-id wire view");
            else NativeDeItemPublication.grant(pc,ringId,1);
            var inv=pc.nativeDeInventory(0);var ring=inv.items().stream().filter(i->i.definition().catalogId()==ringId).findFirst().orElseThrow();
            var changed=ring.definition().sourceArmor()?inv.toggleNativeArmor(ring.objectId()):inv.toggleRing(ring.objectId());
            need(!changed.isEmpty(),"actual Source ring equipped id="+ringId);
            need(inv.controlAbilityEnabled(5),"Source control ring recognized id="+ringId);
        }
        if(mode==6)NativePlayerCommit.scene(pc,350,"classicWorldMapId");
        var row=BattleSpellCatalog.summonSpell(51);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();
        NativeDeItemPublication.grant(pc,item,count+3);
        selection=mode==0?0:mode==1?-1:mode==5?999:mode>=4?274:13;
        byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(2).d(selection).bytes();
        if(mode==2)packet=Arrays.copyOf(packet,6);if(mode==3)packet=Arrays.copyOf(packet,8);
        try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
            boolean malformed=false;
            if(mode==2||mode==3){try{Protocol.summonSpellRequest(packet);}catch(IllegalArgumentException expected){malformed=true;}}
            try{call(pc,"handle",new Class[]{byte[].class},packet);}
            catch(Exception failure){Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();need((mode==2||mode==3)&&cause instanceof IllegalArgumentException,"only malformed packet can reject parsing");malformed=true;}
            boolean accepted=mode==0||mode==1||mode==4||mode>=6;
            need((int)get(pc,"playerMp")==1000-(accepted?cost(pc,51,row):0)&&(int)get(pc,"playerHp")==4000-(accepted?row.hpCost():0),"Source accepted resources mode="+mode);
            need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"material once or untouched mode="+mode);
            var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");
            if(mode==0||mode==1||mode==4){
                need(pets.size()==(mode==4?1:8),"authoritative capacity mode="+mode);
                for(var pet:List.copyOf(pets))need(pet.snapshot().npcId()==(mode==4?81240:81228),"authoritative actor ignores no-ring DWORD");
                call(pc,"cleanupClassicWorldTransientCompanionsOnDisconnect",new Class[]{});
                need(pets.isEmpty(),"native disconnect releases actual summons");
            }else need(pets.isEmpty(),"rejected or map/level-disabled cast creates no actor");
            var self=collect(wire);collect(other);
            String map=HexFormat.of().formatHex(Protocol.messageCode(353)),level=HexFormat.of().formatHex(Protocol.messageCode(743));
            if(mode==6)need(self.contains(map)&&!self.contains(level),"Source no-recall failure precedes level check");
            if(mode==7)need(self.contains(level)&&!self.contains(map),"Source level failure visible");
            if(mode==2||mode==3)need(malformed&&self.isEmpty(),"malformed lengths rejected without output");
            need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"unlocked output and drained receipts");
        }cases++;
    }
    public static void main(String[] args)throws Exception {
        var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate PC");
        var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);
        var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
        var root=Path.of(args[1]);Throwable primary=null;
        try{for(int mode=0;mode<8;mode++){
            var r=root.resolve("mode-"+mode);Files.createDirectories(r);
            var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));
            var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
            try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
                var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
                var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
                var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);
                NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,mode);
            }need(!Files.exists(r.resolve("must-not-write.properties")),"memory only");
        }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
        need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
        Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
    }
}
