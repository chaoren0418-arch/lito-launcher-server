package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only real TLS/Auth/Navigation/Rooms/WorldService NPC provider. Default
 * geometry is flat; shared mode uses the pinned full Lineage map554. Catalog,
 * profiles, day/night and RNG remain explicit fixtures, never production SQL. */
public final class ModernNpcServiceTestPeer {
    private ModernNpcServiceTestPeer(){}
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!Set.of("npc-service-v18","npc-service-shared-v19","quest-stages-shared-v20","quest-objectives-shared-v21","quest-lives-shared-v22","quest-lives-shared-v37","quest-revival-shared-v40","quest-revival-shared-v43","quest-cadence-shared-v43","quest-pursuit-shared-v43","quest-dynamic-shared-v43","quest-dynamic-low-quota-shared-v43","npc77-service-v29","npc77-service-shared-v29","npc70-service-v31","npc70-service-shared-v31","npc70-service-shared-v32-chat","npc-actions-service-shared-v29").contains(args[2]))throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);if(lifetime<5000||lifetime>60_000)throw new IllegalArgumentException();
            boolean lifeActions=args[2].equals("quest-lives-shared-v37");
            boolean pursuit=args[2].equals("quest-pursuit-shared-v43");
            boolean lowQuota=args[2].equals("quest-dynamic-low-quota-shared-v43");
            boolean dynamic=lowQuota||args[2].equals("quest-dynamic-shared-v43");
            boolean cadence=pursuit||dynamic||args[2].equals("quest-cadence-shared-v43");
            boolean canonicalRevival=cadence||args[2].startsWith("quest-revival-shared-v");
            boolean lives=canonicalRevival||lifeActions||args[2].equals("quest-lives-shared-v22");
            boolean actions=args[2].equals("npc-actions-service-shared-v29");
            boolean npc77=actions||args[2].startsWith("npc77-service-");
            boolean npc70=args[2].startsWith("npc70-service-");
            run(Path.of(args[1]),lifetime,!args[2].equals("npc77-service-v29")&&!args[2].equals("npc70-service-v31")&&!args[2].equals("npc-service-v18"),args[2].startsWith("quest-"),lives||args[2].equals("quest-objectives-shared-v21"),lives,npc77,npc70,actions,args[2].endsWith("-chat"),lifeActions,canonicalRevival,cadence||args[2].equals("quest-revival-shared-v43"),cadence,pursuit,dynamic,lowQuota);
        }catch(Exception failure){System.err.println("MODERN_NPC_SERVICE_PEER_FAILED "+failure.getClass().getSimpleName());
            // Method/line diagnostics only; never expose source/account messages.
            java.util.Arrays.stream(failure.getStackTrace()).limit(6).forEach(System.err::println);System.exit(2);}
    }
    private static void run(Path fixtures,int lifetime,boolean shared,boolean quest,boolean objectives,boolean lives,boolean npc77,boolean npc70,boolean actions,boolean chat,boolean lifeActions,boolean canonicalRevival,boolean motion,boolean cadence,boolean pursuit,boolean dynamic,boolean lowQuota)throws Exception {
        boolean magic=npc77||npc70;
        var directory=Files.createTempDirectory("modern-npc-service-peer-").toAbsolutePath().normalize();
        var properties=new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12=").append(fixtures.resolve("server.p12").toString().replace('\\','/')).append("\ntls.password.env=T\nsources=7,42\n");
        for(int id:List.of(7,42))properties.append("source.").append(id).append(".name=Source").append(id).append('\n')
                .append("source.").append(id).append(".mysql.host=127.0.0.1\nsource.").append(id).append(".mysql.port=3306\nsource.").append(id).append(".mysql.database=fixture_").append(id)
                .append("\nsource.").append(id).append(".catalog.database=catalog\nsource.").append(id).append(".mysql.username.env=U\nsource.").append(id).append(".mysql.password.env=P\nsource.")
                .append(id).append(".mysql.passwordScheme=plain\nsource.").append(id).append(".mysql.tls=disabled\n");
        try(var config=ModernServerConfig.parse(properties.toString().getBytes(StandardCharsets.UTF_8),name->"fixture-only");var store=new ModernEquipmentOverrideStore(directory)) {
            var fixture=new ModernApplicationTestBackend("auth",80);var physicalReads=new AtomicInteger();
            ModernEquipmentAuthority.PhysicalProvider physical=(principal,sourceId,objectId)->{
                if(!principal.authorizesSource(sourceId)||objectId!=3333||!(sourceId==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT).equals(principal.accountForSource(sourceId)))throw new SecurityException();
                var source=config.sources().stream().filter(s->s.id()==sourceId).findFirst().orElseThrow();
                var row=fixture.characters.characters(principal,sourceId).stream().filter(c->c.objectId()==objectId).findFirst().orElseThrow();physicalReads.incrementAndGet();
                return new ModernEquipmentAuthority.PhysicalLoaded(magic?ModernBattleEngineNpcDisintegrateTest.source(ModernBattleEngineHonorTest.source(sourceId,row,10000,1000),1000,500):lives?ModernNaturalRegenTest.source(sourceId,row,cadence?10_000:1,30,0,0,225):ModernPhysicalCombatTest.fixtureSource(sourceId,row,ModernNpcServiceTest.HUGE,10,1),ModernEquipmentBinding.ownerBinding(principal,source));
            };
            var authority=new ModernEquipmentAuthority((principal,source,object)->{var loaded=physical.read(principal,source,object);return new ModernEquipmentAuthority.Loaded(loaded.source().input().equipment(),loaded.ownerBinding());},null,physical,store);
            int playerCapacity=quest?4:48;
            var presence=new TournamentPresence();var key=new ModernWorldBinding.Key(2,554);var rooms=new ModernRooms(Set.of(key),Set.of(),Map.of(key,playerCapacity));
            var map=shared?ModernQuestMapQualificationTest.shared():ModernWorldMap.fromSectorBytes(554,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(2,554,map);var catalog=magic?ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,shared?500L:10000L,ModernNpcCatalog.Field.MR,0L),true,0):quest?ModernQuestProgressTest.encounterCatalog():ModernNpcProtocolTest.futureCatalog();
            if(actions) {
                // Explicit arbitration fixture: a durable, stationary NPC keeps
                // movement, death and hurt from masquerading as melee priority.
                catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1_000_000L,ModernNpcCatalog.Field.MR,0L,
                        ModernNpcCatalog.Field.WALK_MILLIS,60_000L,ModernNpcCatalog.Field.ATTACK_MILLIS,60_000L),true,0);
            }
            if(lives) {
                // Test-only warmup lets both real renderers reach ACTIVE before
                // a one-hit death. Long repeat cooldown is not production balance.
                var rows=new java.util.HashMap<Integer,ModernNpcCatalog.Npc>();
                for(var row:catalog.npcs().values()) {
                    var facts=new java.util.HashMap<>(row.facts().values());
                    facts.put(ModernNpcCatalog.Field.WALK_MILLIS,cadence?100L:4000L);facts.put(ModernNpcCatalog.Field.ATTACK_MILLIS,cadence?750L:60_000L);
                    rows.put(row.key().npcId(),new ModernNpcCatalog.Npc(row.key(),row.name(),row.nameId(),row.implementation(),row.large(),new ModernNpcCatalog.Facts(facts),false));
                }
                catalog=new ModernNpcCatalog.Catalog(catalog.sourceId(),catalog.database(),rows,catalog.growth());
            }
            var sharedSpawns=shared?map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,cadence?100:quest||actions?8:3):List.<ModernWorldMap.Cell>of();
            // Cadence fixture gives each force a real shared-map approach path;
            // high HP prevents a one-hit death from hiding the motion/audio test.
            var cadencePath=cadence&&!pursuit&&!dynamic?qualifyCadencePath(map,catalog,sharedSpawns):null;
            int playerIndex=pursuit||dynamic?qualifyPursuitPath(map,catalog,sharedSpawns).playerIndex():cadence?cadencePath.playerIndex():0;
            var lanes=shared?List.of(sharedSpawns.get(playerIndex),sharedSpawns.get(quest?playerIndex:2)):List.of(ModernNpcServiceTest.Fixture.cell(10,10),ModernNpcServiceTest.Fixture.cell(20,10));
            ModernPhysicalCombat.BoundedRandom entropy=bound->bound-1;
            if(actions) {
                // The real engine still selects/validates the destination. Only
                // entropy is fixed, to a free shared-map cell next to the NPC;
                // no movement request or rewritten receipt can cancel the cast.
                var target=sharedSpawns.get(1);
                var candidates=map.teleportCells().stream().filter(c->!lanes.contains(c)&&!c.equals(target)).toList();
                var landing=sharedSpawns.stream().filter(c->!lanes.contains(c)&&!c.equals(target)&&map.canStep(c,target)).findFirst().orElseThrow();
                int index=candidates.indexOf(landing);
                if(index<0||candidates.size()!=976)throw new IllegalStateException("arbitration teleport fixture invalid");
                entropy=bound->bound==candidates.size()?index:0;
            }
            // Both Quest forces deliberately occupy identical coordinates in
            // separate spaces; the ordinary NPC v19 fixture remains shared.
            var plan=quest?ModernQuestProgressTest.encounterPlan(key,catalog,List.of(sharedSpawns.get(1),sharedSpawns.get(1))):null;
            if(objectives) {
                // Explicit shared-geometry fixture, not a claim of native Quest
                // map layout. Both isolated camps use the same qualified cell.
                var target=sharedSpawns.stream().filter(c->!c.equals(lanes.get(0))&&!c.equals(sharedSpawns.get(1))
                        &&Math.max(Math.abs(c.x()-lanes.get(0).x()),Math.abs(c.y()-lanes.get(0).y()))==1).findFirst().orElseThrow();
                plan=new ModernQuestProgress.Plan(key,plan.waves(),new ModernQuestObjectives.Config(List.of(
                    new ModernQuestObjectives.Target(0,4,target,2665,1),new ModernQuestObjectives.Target(1,4,target,2669,1))));
                if(lives)plan=plan.withParticipantLives();
            }
            var npc=new ModernNpcEncounter(key,catalog,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7),42,ModernNpcServiceTest.Fixture.blessing(42)),lanes,8,1000,
                    plan==null?List.of(new ModernNpcRoster.Spawn(catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,shared?sharedSpawns.get(1):ModernNpcServiceTest.Fixture.cell(11,10),2)):plan.firstWaves(),pursuit||lowQuota?PURSUIT_TEST_QUOTA:cadence?64:16,()->false,playerCapacity,plan).withDisintegrate(npc77).withFireStorm(npc70).withLightArrow(actions);
            var mapping=new ModernWorldService.Mapping(binding,lanes.get(0),200_000_000L,actions?200_000_000L:quest?700_000_000L:10_000_000_000L,magic?1_200_000_000L:0,false,false,lives||magic,lives||magic,false,false,lives||magic,null,false,null,lanes,null,npc);
            if(lives) {
                var input=ModernQuestConfigurationTest.properties(mapping,Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")));
                var parsed=ModernWorldConfiguration.parse(input);var spec=parsed.npcSpecs().get(key);
                var loaded=spec.qualify(catalog,npc.sources(),()->ClassicLineageClock.REAL_BASE_EPOCH_MILLIS);
                mapping=parsed.load(null,()->false,Map.of(key,loaded)).get(key);
                if(!mapping.npc().questPlan().equals(plan)||mapping.npc()==npc)throw new IllegalStateException("configured Quest plan mismatch");
            }
            ModernShopCatalogRepository shop=(principal,sourceId,objectId)-> {
                var loaded=physical.read(principal,sourceId,objectId);
                return Optional.of(new ModernShopCatalog.Snapshot(sourceId,principal.accountForSource(sourceId),loaded.source().input().equipment().source().character(),List.of()));
            };
            if(quest)ModernWorldPolicy.qualifyLayout(mapping,playerCapacity,()->false);
            var saved=new java.util.concurrent.atomic.AtomicReference<ModernMatchSettlement.Event>();
            try(var journal=objectives?new ModernSettlementCoordinator(new ModernMatchOutbox(directory,new byte[32]),(e,d)->{
                    if(e.mode()!=2||!saved.compareAndSet(null,e)&&!saved.get().equals(e))throw new IllegalStateException("fixture result conflict");
                },2000,null):null;
                var worlds=new ModernWorldService(rooms,Map.of(key,mapping),new ModernWorldService.Limits(2,1,1,3000,50,new ModernBattleEngine.Limits(shared?30000:15000,0,45_000)),entropy,journal,magic?Map.of(7,100.0,42,100.0):lives?Map.of(7,1.0,42,1.0):Map.of())) {
                ModernServer server=null;
                try {
                    server=new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),ModernTlsTestMaterial.serverContext(fixtures,false),
                            new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10000),fixture.verifier,fixture.characters,presence,
                            (principal,source,object)->Optional.of(physical.read(principal,source,object).source().input().equipment().source()),authority,store,rooms,worlds,canonicalRevival?(motion?43:40):lifeActions?37:chat?32:npc70?31:npc77?29:lives?22:objectives?21:quest?20:shared?19:18,
                            magic?(principal,sourceId,objectId)->{
                                var row=physical.read(principal,sourceId,objectId).source().input().equipment().source().character();
                                return Optional.of(new ModernSkills.Snapshot(sourceId,row,actions?List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),ModernSkillsTest.teleport(),new ModernSkills.Skill(77,"$1485",70,5000,ModernSkills.Support.DISINTEGRATE)):npc70&&sourceId==42?List.of(ModernSkillsTest.teleport()):List.of(ModernSkillsTest.teleport(),npc70?new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM):new ModernSkills.Skill(77,"$1485",70,5000,ModernSkills.Support.DISINTEGRATE))));
                            }:ModernSkillsRepository.unavailable(),ModernCharacterRecordRepository.unavailable(),null,ModernGmCommandRepository.unavailable(),lives||magic?shop:ModernShopCatalogRepository.unavailable(),lives||magic?20000:0,
                            npc77?(principal,sourceId,objectId)->{
                                physical.read(principal,sourceId,objectId);
                                return Optional.of(new ModernDisintegrateCatalog.Snapshot(new TournamentPresence.CharacterKey(sourceId,objectId),ModernDisintegrateCatalog.Status.QUALIFIED));
                            }:null);
                    System.out.println("READY "+server.address().getPort());System.out.flush();long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                    for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                        if(state.accepted()>2||server.listenerFailed()||System.nanoTime()>=deadline)throw new IllegalStateException();Thread.sleep(10);}
                    var completed=server.snapshot();
                    int authenticationCount=fixture.authenticationCalls.get(),physicalCount=physicalReads.get();
                    if(authenticationCount!=4||physicalCount<2||completed.failed()!=0) {
                        // Bounded, numeric-only test diagnostics: never print accounts,
                        // credentials, packet contents or backend exception messages.
                        System.err.println("MODERN_NPC_SERVICE_FLOW auth="+authenticationCount+" physical="+physicalCount
                                +" failed="+completed.failed()+" accepted="+completed.accepted()+" sessions="+completed.sessions());
                        System.err.println("MODERN_NPC_SERVICE_FAILURE_COUNTS "+server.failureCounts());
                        throw new IllegalStateException("NPC authenticated flow incomplete");
                    }
                    // This additional mode confirms death/respawn then explicit
                    // cancellation. Revival must NOT manufacture a victory.
                    // All existing full-Quest modes still require persistence.
                    if(canonicalRevival) {if(saved.get()!=null)throw new IllegalStateException("revival created a fabricated result");}
                    else if(objectives&&(saved.get()==null||saved.get().participants().size()!=2))throw new IllegalStateException("Quest result not persisted");
                }finally {
                    if(server!=null){server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("NPC listener retained");}
                    worlds.close();if(!worlds.awaitTermination(5000)||!worlds.isQuiescent()||worlds.registrationCount()!=0||presence.ownerCount()!=0||rooms.ownerCount()!=0||rooms.memberCount()!=0||rooms.chatOwnerCount()!=0||rooms.directChatOwnerCount()!=0||!store.isQuiescent())throw new IllegalStateException("NPC owners retained");
                }
            }
        }finally {
            // Exact freshly-created test directory, after every writer retires.
            try(var children=Files.list(directory)){for(var child:children.toList()){
                if(!child.toAbsolutePath().normalize().getParent().equals(directory)||!Files.isRegularFile(child,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("unexpected test artifact");Files.delete(child);}}
            Files.delete(directory);
        }
    }
    record CadencePath(int playerIndex,int moves,int maximumVisited) {}
    record PursuitPath(int playerIndex,int moves,int maximumVisited,int budgetYields) {}
    static final int PURSUIT_TEST_QUOTA=1;
    /** Keep distant spawn99 fixed. A* finishes this geometry within 64, so
     * explicitly reduce this fixture's legal quota to exercise resumption. */
    static PursuitPath qualifyPursuitPath(ModernWorldMap map,ModernNpcCatalog.Catalog catalog,List<ModernWorldMap.Cell> cells) {
        var scope=new ModernNpcRoster.Scope(1,1,1);var player=cells.get(99);var origin=cells.get(1);
        var objective=cells.stream().filter(c->!c.equals(player)&&!c.equals(origin)
                &&Math.max(Math.abs(c.x()-player.x()),Math.abs(c.y()-player.y()))==1).findFirst().orElseThrow();
        var occupied=Set.of(player,objective);
        var target=new ModernNpcRoster.Target(ModernParticipantId.human(7,3333),0,player,true);
        var template=catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        try(var roster=new ModernNpcRoster(scope,map,List.of(player,player),8,1000,true)) {
            var npc=roster.spawn(scope,List.of(new ModernNpcRoster.Spawn(template,0,1,origin,2)),occupied).get(0);
            int moves=0,visited=0,yields=0;
            for(int tick=1;tick<=80;tick++) {
                roster.advance(scope,(long)tick*template.walkMillis());
                var plan=roster.pursue(scope,npc.id(),npc.life(),List.of(target),occupied,PURSUIT_TEST_QUOTA,()->false);
                visited=Math.max(visited,plan.visited());
                if(plan.visited()>PURSUIT_TEST_QUOTA||tick==1&&plan.kind()!=ModernNpcRoster.PlanKind.BUDGET)throw new IllegalStateException("distant quota fixture no longer requires resumption");
                if(plan.kind()==ModernNpcRoster.PlanKind.BUDGET){yields++;continue;}
                if(plan.kind()==ModernNpcRoster.PlanKind.IN_RANGE) {
                    if(moves<10||yields<1)throw new IllegalStateException("distant fixture did not exercise pursuit");
                    return new PursuitPath(99,moves,visited,yields);
                }
                if(plan.kind()!=ModernNpcRoster.PlanKind.MOVE)throw new IllegalStateException("distant pursuit stalled");
                npc=roster.move(scope,plan,occupied);moves++;
            }
        }
        throw new IllegalStateException("distant pursuit never reached its target");
    }
    /** Preserve R247's actual spawn73 geometry and nine-step cadence case;
     * a faster pathfinder must not silently select a different fixture. */
    static CadencePath qualifyCadencePath(ModernWorldMap map,ModernNpcCatalog.Catalog catalog,List<ModernWorldMap.Cell> cells) {
        var scope=new ModernNpcRoster.Scope(1,1,1);
        var template=catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        for(int index:List.of(73)) {
            var player=cells.get(index);var origin=cells.get(1);
            var objective=cells.stream().filter(c->!c.equals(player)&&!c.equals(origin)
                    &&Math.max(Math.abs(c.x()-player.x()),Math.abs(c.y()-player.y()))==1).findFirst().orElse(null);
            if(objective==null)continue;
            var occupied=Set.of(player,objective);
            var target=new ModernNpcRoster.Target(ModernParticipantId.human(7,3333),0,player,true);
            try(var roster=new ModernNpcRoster(scope,map,List.of(player,player),8,1000,true)) {
                var npc=roster.spawn(scope,List.of(new ModernNpcRoster.Spawn(template,0,1,origin,2)),occupied).get(0);
                int moves=0,visited=0;
                for(int tick=1;tick<=32;tick++) {
                    roster.advance(scope,(long)tick*template.walkMillis());
                    var plan=roster.pursue(scope,npc.id(),npc.life(),List.of(target),occupied,64,()->false);
                    visited=Math.max(visited,plan.visited());
                    if(plan.kind()==ModernNpcRoster.PlanKind.IN_RANGE) {
                        if(moves==9)return new CadencePath(index,moves,visited);
                        break;
                    }
                    if(plan.kind()!=ModernNpcRoster.PlanKind.MOVE)break;
                    npc=roster.move(scope,plan,occupied);moves++;
                }
            }
        }
        throw new IllegalStateException("no six-step cadence fixture within the service search bound");
    }
}
