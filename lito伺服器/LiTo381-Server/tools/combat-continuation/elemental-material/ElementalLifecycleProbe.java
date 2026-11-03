package local.lito381.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real packet dispatch, actual summon objects and native expiry/disconnect removal. */
public final class ElementalLifecycleProbe extends ElementalMaterialProbe {
    static final int[] ATTRIBUTES = {1, 2, 4, 8, 3, 6, 12, 15};

    static int expectedNpc(int skill, int attribute) {
        int offset = (attribute & 1) != 0 ? 0 : (attribute & 2) != 0 ? 1
                : (attribute & 4) != 0 ? 2 : 3;
        return (skill == 154 ? new int[]{45306, 45303, 45304, 45305}
                : new int[]{81053, 81050, 81051, 81052})[offset];
    }

    static void expire(ClientSession pc, GameHub hub, boolean world,
                       BattleCompanion pet) throws Exception {
        var snapshot = pet.snapshot();
        long end = snapshot.expiresAtNanos();
        need(!pet.expired(end - 1) && pet.expired(end), "exact expiry boundary");
        if (world) pc.serverClassicWorldCompanionTick((int)get(pc, "classicWorldMapId"), end);
        else pc.serverCompanionTick((long)get(pc, "currentRoomId"), end);
        need(!pet.snapshot().active(), "native expiry deactivates the actual summoned actor");
        need(((List<?>)get(pc, "tamedCompanions")).isEmpty(), "native expiry releases owner reference");
    }

    static void exercise(ClientSession pc, ClientSession peer, GameHub hub,
                         boolean world, int skill, String scenario) throws Exception {
        reset(pc, peer, hub, world);
        var chars = (List<CharacterSummary>)get(pc, "characters");
        var caster = copy(chars.get(0), Map.of("type", 2, "originalIntelligence", 10));
        set(pc, "characters", List.of(caster));
        set(pc, "playerMp", 1000); set(pc, "playerMaxMp", 1000);
        set(pc, "observer", false); set(pc, "learnedSkills", new HashSet<>(Set.of(skill)));
        call(pc, "clearInvisibilityStateSilently", new Class[]{});
        int attribute = scenario.startsWith("attr-") ? Integer.parseInt(scenario.substring(5)) : 1;
        set(pc, "elfAttribute", attribute);
        if (!world) {
            var arena = ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc, "currentRoomId"), pc));
            need(arena.allowsPetRecall(), "battle fixture map permits actual summons");
            var point = arena.spawns().get(0);
            set(pc, "positionX", point.x()); set(pc, "positionY", point.y());
            set(peer, "positionX", point.x() + 2); set(peer, "positionY", point.y());
        }
        if (scenario.equals("blocked-map")) {
            need(world && !ClassicWorldMapCatalog.forMap(350).allowsPetRecall(), "retained map350 denies recall");
            NativePlayerCommit.scene(pc, 350, "classicWorldMapId");
        }
        var spell = BattleSpellCatalog.elementalSummonSpell(skill);
        int item = Math.toIntExact(spell.itemConsumeId()), count = spell.itemConsumeCount();
        NativeDeItemPublication.grant(pc, item, count * 3 + 3);
        int mpCost = cost(pc, skill, spell);
        var pets = (List<BattleCompanion>)get(pc, "tamedCompanions");
        need(pets.isEmpty(), "no prior companion in fresh scenario");
        try (var wire = new SourceActionPairProbe.Wire(pc, peer);
             var other = new SourceActionPairProbe.Wire(peer, pc)) {
            Socket original = (Socket)get(pc, "socket");
            var failed = new AtomicBoolean();
            if (scenario.equals("socket-failure")) set(pc, "socket", new Socket() {
                public OutputStream getOutputStream() throws IOException {
                    return new FilterOutputStream(original.getOutputStream()) {
                        public void write(byte[] b, int o, int n) throws IOException {
                            if (failed.compareAndSet(false, true)) throw new IOException("elemental-real-output-failure");
                            out.write(b, o, n);
                        }
                        public void write(int b) throws IOException { write(new byte[]{(byte)b}, 0, 1); }
                    };
                }
                public void close() {}
            });
            try {
                cast(pc, skill, spell);
            } catch (Exception error) {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                need(scenario.equals("socket-failure") && cause instanceof IOException
                        && "elemental-real-output-failure".equals(cause.getMessage()), "preserve actual socket failure");
            } finally { set(pc, "socket", original); }
            need((int)get(pc, "playerMp") == 1000 - mpCost, "source MP cost commits exactly once");
            need(pc.nativeDeInventory(0).countCatalog(item) == count * 2 + 3, "material commits exactly once");
            if (scenario.equals("blocked-map")) {
                need(pets.isEmpty(), "no-recall map creates no actor after accepted cost");
                need(collect(wire).contains(HexFormat.of().formatHex(Protocol.messageCode(353))), "visible original map failure353");
            } else {
                need(pets.size() == 1, "one real actor created");
                var pet = pets.get(0); var snapshot = pet.snapshot();
                need(snapshot.active() && snapshot.npcId() == expectedNpc(skill, attribute), "source lowest attribute bit selects correct NPC");
                need(snapshot.petCost() == Math.max(0, caster.charisma()) + 7, "source CHA+7 pet cost");
                need(snapshot.moveSpeed() == 1 && snapshot.expiresAtNanos() > System.nanoTime(), "haste and lifetime");
                if (scenario.equals("duplicate")) {
                    long ready = ((Map<Integer,Long>)get(pc, "spellReadyNanos")).get(skill);
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (System.nanoTime() < ready || !pc.nativeCastTimelineState().timeline.canReserveSkillAction()) {
                        need(System.nanoTime() < deadline, "source action and reuse finish within fixture bound");
                        Thread.sleep(2);
                    }
                    cast(pc, skill, spell);
                    need(pets.size() == 1 && pets.get(0) == pet, "existing pet cost prevents second summon");
                    need((int)get(pc, "playerMp") == 1000 - 2 * mpCost
                            && pc.nativeDeInventory(0).countCatalog(item) == count + 3,
                            "source no-op outcome retains second accepted cast cost");
                }
                if (scenario.equals("disconnect")) {
                    call(pc, "cleanupClassicWorldTransientCompanionsOnDisconnect", new Class[]{});
                    need(pets.isEmpty() && !pet.snapshot().active(), "native world disconnect releases actual summon");
                } else expire(pc, hub, world, pet);
                if (scenario.equals("socket-failure")) need(failed.get(), "actual socket failure reached");
                collect(wire);
            }
            collect(other);
            need(wire.locked + other.locked == 0, "no cast/expiry/disconnect packet under source action lock");
            need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc)) == 0
                    && SourceActionPairProbe.state(pc).queue.isEmpty(), "all accepted receipts and queues drained");
        }
        cases++;
    }

    public static void main(String[] args) throws Exception {
        var jar = Path.of(args[3]).toRealPath();
        for (var type : List.of(ClientSession.class, BattleCompanion.class, NativeElementalCastAction.class))
            need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "real candidate handlers and actors");
        var base = ServerConfig.load(Path.of(args[0]), "standard"); ResourceDatabase.load(base);
        var templates = new NativeDeItemTemplatesResources.Owner().reload(base);
        var definitions = new NativeDeItemDefinitionsResources.Owner().reload(base);
        var root = Path.of(args[1]); Throwable primary = null;
        try {
            for (boolean world : new boolean[]{false, true}) for (int skill : SKILLS) {
                var scenarios = new ArrayList<String>();
                for (int attribute : ATTRIBUTES) scenarios.add("attr-" + attribute);
                scenarios.add("duplicate"); scenarios.add("socket-failure");
                if (world) { scenarios.add("blocked-map"); scenarios.add("disconnect"); }
                for (var scenario : scenarios) {
                    var r = root.resolve(world + "-" + skill + "-" + scenario); Files.createDirectories(r);
                    var cfg = copy(base, Map.of("battleNaturalHpRegenEnabled", false, "battleNaturalMpRegenEnabled", false,
                            "statePersistenceEnabled", true, "stateFile", r.resolve("must-not-write.properties")));
                    var state = new GameStateStore(r.resolve("must-not-write.properties"), false, true);
                    var accounts = new MemoryAccountRepository("fixture", "fixture", "", 0, 1, 40, 52);
                    try (var capture = new CaptureLog(r.resolve("capture")); var hub = new GameHub(cfg, state, accounts);
                         var f = new SourceLoginRegression.Fixture(cfg, accounts, state, capture, hub, 0, ClientSession.Endpoint.GAME);
                         var p = new SourceLoginRegression.Fixture(cfg, accounts, state, capture, hub, 0, ClientSession.Endpoint.GAME);
                         var m = new SourceLoginRegression.Fixture(cfg, accounts, state, capture, hub, 0, ClientSession.Endpoint.GAME);
                         var o = new SourceLoginRegression.Fixture(cfg, accounts, state, capture, hub, 0, ClientSession.Endpoint.GAME)) {
                        var clock = (ScheduledExecutorService)get(hub, "battleClock"); clock.shutdownNow();
                        need(clock.awaitTermination(2, TimeUnit.SECONDS), "unrelated battle clock stopped");
                        var fixtures = List.of(f, p, m, o); init(fixtures, cfg);
                        FlowRegression.setup(hub, f.session, p.session, m.session, o.session);
                        if (world) world(fixtures, hub);
                        NativeDeItemApplication.install(hub, templates, definitions);
                        exercise(p.session, m.session, hub, world, skill, scenario);
                    }
                    need(!Files.exists(r.resolve("must-not-write.properties")), "all mutations remain memory-only");
                }
            }
        } catch (Throwable failure) { primary = failure; throw failure; }
        finally { try { NativePlayerCommit.closeFixture(); } catch (Throwable e) { if (primary != null) primary.addSuppressed(e); else throw e; } }
        need(NativePlayerLifetime.allIdle(), "all lifetimes idle");
        String result = "{\"cases\":" + cases + ",\"checks\":" + ownChecks + ",\"frames\":" + frames
                + ",\"locked_writes\":" + lockedWrites + ",\"full_goal_achieved\":false}";
        Files.writeString(Path.of(args[2]), result + "\n"); System.out.println(result);
    }
}
