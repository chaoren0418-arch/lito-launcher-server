package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.reflect.*;

/** Actual repository delay between accepted cost and real NPC construction. */
public final class ElementalClockProbe extends ElementalMaterialProbe {
    static int shortened;
    static final List<String> goldens = new ArrayList<>();
    static AccountRepository delayed(AtomicBoolean active, AtomicLong completed, AtomicInteger reads) {
        var base = new MemoryAccountRepository("fixture", "fixture", "", 0, 1, 40, 52);
        return (AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),
                new Class[]{AccountRepository.class, NativeCastMaterialPolicy.Source.class}, (proxy, method, args) -> {
                    if (method.getName().equals("characterAccessLevel")) return 0;
                    if (method.getName().equals("repositoryForClassicSource")) return proxy;
                    try {
                        method.setAccessible(true); Object result = method.invoke(base, args);
                        if (active.get() && method.getName().equals("npcPhysicalStats")) {
                            reads.incrementAndGet(); Thread.sleep(250); completed.set(System.nanoTime());
                        }
                        return result;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
    static void exercise(ClientSession pc, ClientSession peer, GameHub hub, boolean world,
                         int skill, AtomicBoolean active, AtomicLong completed, AtomicInteger reads) throws Exception {
        reset(pc, peer, hub, world);
        var c = ((List<CharacterSummary>)get(pc, "characters")).get(0);
        set(pc, "characters", List.of(copy(c, Map.of("type", 2, "originalIntelligence", 10))));
        set(pc, "playerMp", 1000); set(pc, "playerMaxMp", 1000); set(pc, "observer", false);
        set(pc, "elfAttribute", 1); set(pc, "learnedSkills", new HashSet<>(Set.of(skill)));
        if (!world) {
            var arena = ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc, "currentRoomId"), pc));
            need(arena.allowsPetRecall(), "actual battle map supports summons");
            var point = arena.spawns().get(0);
            set(pc, "positionX", point.x()); set(pc, "positionY", point.y());
            set(peer, "positionX", point.x()+2); set(peer, "positionY", point.y());
        }
        var spell = BattleSpellCatalog.elementalSummonSpell(skill);
        int item = Math.toIntExact(spell.itemConsumeId()), count = spell.itemConsumeCount();
        NativeDeItemPublication.grant(pc, item, count+3);
        try (var wire = new SourceActionPairProbe.Wire(pc, peer); var other = new SourceActionPairProbe.Wire(peer, pc)) {
            active.set(true);
            try { cast(pc, skill, spell); } finally { active.set(false); }
            var pets = (List<BattleCompanion>)get(pc, "tamedCompanions");
            need(reads.get()==1 && completed.get()>0 && pets.size()==1, "actual delayed source query and actual created NPC");
            var pet = pets.get(0);
            long started = pet.snapshot().expiresAtNanos()-TimeUnit.SECONDS.toNanos(spell.lifetimeSeconds());
            if (started < completed.get()) shortened++;
            need(before ? started < completed.get()-TimeUnit.MILLISECONDS.toNanos(200)
                            : started >= completed.get() && started <= System.nanoTime(),
                    "NPC lifetime begins at creation after source query, stage="+before);
            need((int)get(pc,"playerMp")==1000-cost(pc,skill,spell)
                    && pc.nativeDeInventory(0).countCatalog(item)==3, "clock correction leaves accepted costs intact");
            ElementalLifecycleProbe.expire(pc,hub,world,pet);
            var self=collect(wire); var peers=collect(other);
            goldens.add("world="+world+" skill="+skill+" self="+self+" peer="+peers);
            need(wire.locked+other.locked==0 && NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,
                    "outputs unlocked, retained receipt released");
        }
        cases++;
    }
    public static void main(String[] args) throws Exception {
        before=args[4].equals("before"); var jar=Path.of(args[3]).toRealPath();
        need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "actual requested PC");
        var base=ServerConfig.load(Path.of(args[0]),"standard"); ResourceDatabase.load(base);
        var templates=new NativeDeItemTemplatesResources.Owner().reload(base);
        var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
        var root=Path.of(args[1]); Throwable primary=null;
        try {
            for(boolean world:new boolean[]{false,true}) for(boolean app:new boolean[]{false,true}) for(int skill:SKILLS) {
                var r=root.resolve(world+"-"+app+"-"+skill); Files.createDirectories(r);
                var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,
                        "statePersistenceEnabled",true,"stateFile",r.resolve("must-not-write.properties")));
                var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);
                var active=new AtomicBoolean(); var completed=new AtomicLong(); var reads=new AtomicInteger();
                var accounts=delayed(active,completed,reads);
                try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
                    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                    var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                    var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
                    var clock=(ScheduledExecutorService)get(hub,"battleClock"); clock.shutdownNow();
                    need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
                    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);
                    if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);
                    exercise(p.session,m.session,hub,world,skill,active,completed,reads);
                }
                need(!Files.exists(r.resolve("must-not-write.properties")),"no source/disk persistence");
            }
        } catch(Throwable e) {primary=e;throw e;}
        finally {try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
        need(NativePlayerLifetime.allIdle(),"all native lifetimes idle");
        var output=Path.of(args[2]);Files.writeString(output.resolveSibling("goldens.txt"),String.join("\n",goldens)+"\n");
        String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"shortened_lifetimes\":"+shortened
                +",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}";
        Files.writeString(output,result+"\n");System.out.println(result);
    }
}
