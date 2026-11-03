package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real skill51 packet and unchanged Source grade/cost/map branches. */
public class SummonMaterialProbe extends ElementalMaterialProbe {
    static final int[] SKILLS={51};
    static int selection=13, gaps;
    static final List<String> goldens=new ArrayList<>();
    static Object spell(int skill){return BattleSpellCatalog.summonSpell(skill);}
    static void cast(ClientSession pc,int skill,Object spell)throws Exception {
        call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c((skill-1)>>3).c((skill-1)&7).d(selection).bytes());
    }
    static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception {
        reset(pc,peer,hub,world);
        var c=((List<CharacterSummary>)get(pc,"characters")).get(0);
        set(pc,"characters",List.of(copy(c,Map.of("type",3,"level",mode==6?31:99,"originalIntelligence",10))));
        set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);
        set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(2,51)));
        call(pc,"clearInvisibilityStateSilently",new Class[]{});
        var row=BattleSpellCatalog.summonSpell(51);need(row!=null,"loaded shared skill51 row");
        selection=mode==5?999:row.directGrade(mode==6?31:99);
        if(mode==3)set(pc,"playerMp",0);if(mode==4)set(pc,"playerHp",row.hpCost());
        if(!world){
            var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));
            need(arena.allowsPetRecall(),"fixture map allows summons");var point=arena.spawns().get(0);
            set(pc,"positionX",point.x());set(pc,"positionY",point.y());set(peer,"positionX",point.x()+2);set(peer,"positionY",point.y());
        }
        if(mode==7){need(world&&!ClassicWorldMapCatalog.forMap(350).allowsPetRecall(),"real no-recall map");NativePlayerCommit.scene(pc,350,"classicWorldMapId");}
        int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();
        if(mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
        try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)) {
            if(mode==1){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);
                while(System.nanoTime()-ready<=0)Thread.sleep(1);
                need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine Light action window still closed");}
            int hp=(int)get(pc,"playerHp"),mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();
            boolean accepted=mode==0||mode==6||mode==7||mode==1&&before;
            cast(pc,51,row);
            need((int)get(pc,"playerHp")==hp-(accepted?row.hpCost():0),"actual HP cost or original rejection");
            need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,51,row):0),"independent source MP formula");
            need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"material once or unchanged");
            need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(51)==accepted,"reuse only after accepted cast");
            if(mode==1){need((wire.raw.size()>bytes)==before,"closed source action window gates actual summon");if(before)gaps++;}
            var pets=(List<BattleCompanion>)get(pc,"tamedCompanions");
            if(accepted&&mode!=7){
                int expectedCount=Math.min(8,(c.charisma()+6)/6);
                need(pets.size()==expectedCount,"source CHA+6, cost6, available landing produces correct count");
                int expectedNpc=mode==6?81210:81228;
                for(var pet:List.copyOf(pets)){
                    var snapshot=pet.snapshot();need(snapshot.npcId()==expectedNpc&&snapshot.petCost()==6&&snapshot.active(),"correct direct grade NPC identity and cost");
                    need(snapshot.expiresAtNanos()>System.nanoTime(),"real summon lifetime");
                }
            }else need(pets.isEmpty(),"rejected or map-disabled summon creates no actor");
            var self=collect(wire);var peers=collect(other);
            if(mode==7)need(self.contains(HexFormat.of().formatHex(Protocol.messageCode(353))),"accepted no-recall reports353");
            if(mode==2&&!before)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material reports299 only");
            need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0
                    &&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and all receipts drained");
            // Object ids differ if the parent accepted an extra cast. Compare stable no-actor branches only.
            if(mode==2||mode==3||mode==4||mode==5||mode==7)
                goldens.add("world="+world+" mode="+mode+" hp="+get(pc,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
        }
        cases++;
    }
    public static void main(String[] args)throws Exception {
        before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();
        for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeCastMaterial.class))
            need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native handlers from requested JAR");
        var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);
        var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
        var root=Path.of(args[1]);Throwable primary=null;
        try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<8;mode++){
            if(mode==7&&!world)continue;
            var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);
            var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
            var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
            try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
                var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
                var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
                var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);
                if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
                exercise(p.session,m.session,hub,world,mode);
            }need(!Files.exists(r.resolve("must-not-write.properties")),"both settings remain memory-only");
        }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
        need(NativePlayerLifetime.allIdle(),"all native lifetimes idle");var output=Path.of(args[2]);
        Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
        String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"Source_window_bypasses\":"+gaps+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
        Files.writeString(output,result+"\n");System.out.println(result);
    }
}
