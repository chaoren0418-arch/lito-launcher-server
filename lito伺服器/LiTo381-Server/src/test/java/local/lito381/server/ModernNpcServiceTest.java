package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Exercises the real WorldService admission, owned scheduler and terminal
 * publication, not a manually driven stand-alone BattleEngine. */
class ModernNpcServiceTest {
    static final String ZERO="0".repeat(64);
    static final long HUGE=9_007_199_254_740_993L;
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldBinding.Key key;
        final ModernRooms rooms;
        final ModernRoomsTest.Player a,b;
        final AtomicInteger physicalReads=new AtomicInteger(),nightReads=new AtomicInteger();
        final ModernWorldService.Mapping mapping;
        final ModernWorldService service;
        boolean learned=true;
        Fixture()throws Exception {this(100);}
        Fixture(int playerCapacity)throws Exception {this(playerCapacity,false);}
        Fixture(int playerCapacity,boolean quest)throws Exception {this(playerCapacity,quest,false);}
        Fixture(int playerCapacity,boolean quest,boolean sameCamp)throws Exception {
            this(playerCapacity,quest,sameCamp,null,554);
        }
        Fixture(int playerCapacity,boolean quest,boolean sameCamp,ModernMatchSettlement.JournaledStore journal,int logical)throws Exception {
            this(playerCapacity,quest,sameCamp,journal,logical,false);
        }
        Fixture(int playerCapacity,boolean quest,boolean sameCamp,ModernMatchSettlement.JournaledStore journal,int logical,boolean lightArrow)throws Exception {
            this(playerCapacity,quest,sameCamp,journal,logical,lightArrow,null);
        }
        Fixture(int playerCapacity,boolean quest,boolean sameCamp,ModernMatchSettlement.JournaledStore journal,int logical,boolean lightArrow,
                ModernPhysicalCombat.BoundedRandom random)throws Exception {
            key=new ModernWorldBinding.Key(2,logical);rooms=new ModernRooms(Set.of(key),Set.of(key));a=player(7);b=player(42);
            var map=ModernWorldMap.fromSectorBytes(554,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(2,logical,map);
            var catalog=quest?ModernQuestProgressTest.encounterCatalog():ModernNpcProtocolTest.futureCatalog();
            var lanes=List.of(cell(10,10),cell(sameCamp?10:20,10));
            var plan=quest?ModernQuestProgressTest.encounterPlan(key,catalog,List.of(cell(11,10),cell(sameCamp?11:21,10))):null;
            if(journal!=null)plan=new ModernQuestProgress.Plan(key,plan.waves(),ModernQuestObjectivesTest.objectives(logical,1,false));
            var setup=new ModernNpcEncounter(key,catalog,Map.of(7,blessing(7),42,blessing(42)),lanes,8,1000,
                    plan==null?List.of(new ModernNpcRoster.Spawn(catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,cell(11,10),2)):plan.firstWaves(),
                    16,()->{nightReads.incrementAndGet();return false;},playerCapacity,plan,lightArrow);
            mapping=new ModernWorldService.Mapping(binding,lanes.get(0),1_000_000,quest?1_000_000:10_000_000_000L,lightArrow?(quest?10_000_000:10_000_000_000L):0,false,false,false,false,false,false,false,null,false,null,lanes,null,setup);
            var limits=new ModernWorldService.Limits(2,1,1,2000,quest?20:1000,new ModernBattleEngine.Limits(2000,0,60_000));
            service=random!=null||quest?new ModernWorldService(rooms,Map.of(key,mapping),limits,random!=null?random:bound->bound-1,journal):new ModernWorldService(rooms,Map.of(key,mapping),limits);
            ModernRoomsTest.accepted(command(a,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,2,logical,0)));
            var summary=view().summary();ModernRoomsTest.accepted(command(b,ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null));
            for(var p:List.of(a,b)){register(p,lightArrow?24:journal!=null?21:quest?20:18,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)),null);ready(p);}
        }
        static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(32768+x,32768+y);}
        static ModernNpcMelee.BlessingBinding blessing(int source){return new ModernNpcMelee.BlessingBinding(source,"catalog",ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED);}
        ModernRoomsTest.Player player(int source) {
            var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"server-"+source,"human",80,1,0);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
            return new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,3333,1,0));
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p){var s=presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(s.key().objectId(),s.characterName(),s.characterClass(),s.sex(),s.level());}
        ModernPhysicalCombat.Fighter fighter(ModernRoomsTest.Player p){physicalReads.incrementAndGet();var source=ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(),row(p),HUGE,10,1);
            return ModernPhysicalCombatTest.fighter(mapping.npc().lightArrow()?ModernBattleEngineNpcMagicTest.magic(source):source);}
        void register(ModernRoomsTest.Player p,int version,int capabilities,ModernWorldService.PhysicalReader custom){service.register(p.owner(),p.scope(),deadline->{fail("no roster fallback");return null;},
                custom==null?deadline->fighter(p):custom,mapping.npc().questPlan()!=null&&mapping.npc().questPlan().objectives()!=null,
                mapping.spellsEnabled()?deadline->new ModernSkills.Snapshot(p.scope().sourceId(),row(p),learned?List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),ModernSkillsTest.teleport()):List.of(ModernSkillsTest.teleport())):null,
                false,null,false,false,false,false,false,false,new ModernSessionBoundary.Negotiated(1,version,1,capabilities));}
        ModernRooms.Result command(ModernRoomsTest.Player p,ModernRooms.Action action,long id,long revision,int value,ModernRooms.Draft draft){return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,id,revision,value,draft,""));}
        ModernRooms.RoomSnapshot view(){return command(a,ModernRooms.Action.READ,0,0,0,null).view().joined();}
        void ready(ModernRoomsTest.Player p){var s=view().summary();ModernRoomsTest.accepted(command(p,ModernRooms.Action.READY,s.id(),s.revision(),1,null));}
        ModernWorldProtocol.Reply prepare(){var s=view().summary();return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ZERO));}
        ModernWorldProtocol.Frame world(){return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ZERO)).frame();}
        ModernNpcProtocol.Request request(ModernRoomsTest.Player p,ModernWorldProtocol.Frame w,long sequence,long serial,long life){return new ModernNpcProtocol.Request(p.scope(),sequence==0?ModernNpcProtocol.Action.READ:ModernNpcProtocol.Action.ATTACK,
                new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation()),sequence,serial,life);}
        ModernNpcProtocol.Reply read(ModernRoomsTest.Player p,ModernWorldProtocol.Frame w){return service.npc(p.owner(),request(p,w,0,0,0));}
        ModernWorldProtocol.Frame loading()throws Exception {assertEquals(ModernWorldProtocol.Status.OK,prepare().status());await(()->world().phase()!=ModernWorldProtocol.Phase.PREPARING);var w=world();assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase());return w;}
        void ack(ModernRoomsTest.Player p,ModernWorldProtocol.Frame w){assertEquals(ModernWorldProtocol.Status.OK,service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,w.matchId(),w.generation(),0,0,0,w.manifest().digest())).status());}
        ModernNpcProtocol.Frame active()throws Exception {var w=loading();ack(a,w);ack(b,w);var n=read(a,w).frame();assertEquals(ModernWorldProtocol.Phase.ACTIVE,n.combat().world().phase());return n;}
        @Override public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));for(var p:List.of(a,b)){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
    }
    static void await(BooleanSupplier condition)throws Exception {long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!condition.getAsBoolean()){assertTrue(System.nanoTime()<deadline);Thread.sleep(2);}}
    static void wire(ModernNpcProtocol.Request request,ModernNpcProtocol.Reply reply)throws Exception {var message=new ModernApplicationProtocol.NpcResult(91,request,reply);int version=reply.frame()!=null&&reply.frame().combat().world().quest().enabled()?21:reply.frame()!=null&&reply.frame().combat().world().isolatedForces()?20:18;wireVersions(message,version);}
    static void wireVersions(ModernApplicationProtocol.Message message,int version)throws Exception {
        var peer=new ModernSessionBoundary.Negotiated(1,version,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))&~ModernSessionProtocol.CAP_DISINTEGRATE);
        var projected=ModernMatchDescriptor.forPeer(message,peer);
        assertEquals(projected,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(projected,version),version));
        assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,28),28));
    }

    @Test void stagedQuestUsesActualServiceSchedulerAndFrozenFutureResourcesWithoutPretendingObjectiveCompletion()throws Exception {
        try(var f=new Fixture(100,true)) {
            assertNotNull(f.mapping.npc().questPlan());
            assertSame(f.mapping.npc().questPlan(),f.mapping.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60_000)).npc().questPlan());
            var w=f.loading();var loading=f.read(f.a,w);var resources=loading.frame().resources();
            assertEquals(List.of(1649,2158),resources.graphics());assertTrue(loading.frame().actors().isEmpty());
            f.ack(f.a,w);assertEquals(0,f.read(f.a,w).frame().spawned());f.ack(f.b,w);
            var ids=new java.util.HashSet<Long>();
            for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++) {
                final int currentLane=lane,currentStage=stage;
                var p=lane==0?f.a:f.b;
                await(()->f.read(p,w).frame().actors().stream().anyMatch(a->a.hp()>0&&a.lane()==currentLane&&a.stage()==currentStage));
                var before=f.read(p,w).frame();var target=before.actors().stream().filter(a->a.hp()>0&&a.lane()==currentLane).findFirst().orElseThrow();
                assertEquals(stage,target.stage());assertTrue(ids.add(target.serial()));
                assertEquals(stage%2==1?1649:2158,target.graphic());
                var request=f.request(p,w,stage,target.serial(),target.life());
                var reply=f.service.npc(p.owner(),request);wire(request,reply);assertEquals(ModernNpcProtocol.Status.OK,reply.status());assertEquals(ModernNpcProtocol.Reason.NONE,reply.reason());
                assertTrue(reply.frame().exchanges().stream().anyMatch(e->e.direction()==ModernNpcBattle.Direction.PLAYER_TO_NPC
                        &&e.participant().equals(ModernParticipantId.human(p.scope().sourceId(),3333))&&e.requestSequence()==currentStage&&e.outcome().death()));
                assertEquals(resources,reply.frame().resources());assertEquals(ModernWorldProtocol.Phase.ACTIVE,reply.frame().combat().world().phase());
                assertNull(reply.frame().combat().world().completion());
                assertTrue(reply.frame().actors().stream().allMatch(a->a.lane()==currentLane));
                assertEquals(currentLane,reply.frame().combat().world().audienceLane());
                assertEquals(List.of((long)lane+1),reply.frame().combat().vitals().stream().map(ModernCombatProtocol.Vital::entityId).toList());
                if(lane==0)assertTrue(f.read(f.b,w).frame().actors().stream().anyMatch(a->a.hp()>0&&a.lane()==1&&a.stage()==1));
                // Let the real monotonic attack clock, not a client timestamp, expire.
                Thread.sleep(3);
            }
            await(()->f.read(f.a,w).frame().actors().isEmpty());
            for(int i=0;i<5;i++){var after=f.read(f.a,w);wire(f.request(f.a,w,0,0,0),after);assertEquals(8,after.frame().spawned());
                assertEquals(resources,after.frame().resources());assertEquals(ModernWorldProtocol.Phase.ACTIVE,after.frame().combat().world().phase());assertNull(after.frame().combat().world().completion());}
            f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ZERO));
            var terminal=f.read(f.b,w);assertTrue(terminal.frame().closed());assertEquals(ModernWorldProtocol.Phase.ABORTED,terminal.frame().combat().world().phase());
            assertNull(terminal.frame().combat().world().completion());assertThrows(SecurityException.class,()->f.read(f.a,w));
        }
    }

    @Test void worldAdmissionIndependentlyRejectsOverLimitDraftBeforeReadingCharactersAndReleasesRoom()throws Exception {
        try(var f=new Fixture(48)) {
            var reply=f.prepare();assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,reply.status());
            assertEquals(ModernWorldProtocol.Reason.CAPACITY,reply.reason());assertNull(reply.frame());
            assertEquals(0,f.physicalReads.get());assertEquals(0,f.nightReads.get());assertEquals(0,f.service.active());
            var summary=f.view().summary();
            ModernRoomsTest.accepted(f.command(f.a,ModernRooms.Action.CONFIGURE,summary.id(),summary.revision(),0,ModernRoomsTest.draft(48,2,554,0)));
            f.ready(f.a);f.ready(f.b);assertNotNull(f.loading());
        }
    }

    @Test void resourcesPrecedeEveryLoadedAckAndServerTickSpawnsExactlyOnce()throws Exception {
        try(var f=new Fixture()) {
            var w=f.loading();var before=f.read(f.a,w);wire(f.request(f.a,w,0,0,0),before);
            assertEquals(List.of(1649,2158),before.frame().resources().graphics());assertTrue(before.frame().actors().isEmpty());assertEquals(0,f.nightReads.get());
            assertEquals(ModernMatchDescriptor.Ruleset.NPC_ENCOUNTER,w.descriptor().ruleset());assertEquals(2,f.physicalReads.get());
            f.ack(f.a,w);assertTrue(f.read(f.a,w).frame().actors().isEmpty());assertEquals(0,f.nightReads.get());
            f.ack(f.b,w);await(()->f.read(f.a,w).frame().spawned()==1);
            for(int i=0;i<5;i++){var r=f.read(f.a,w);wire(f.request(f.a,w,0,0,0),r);assertEquals(1,r.frame().spawned());assertEquals(before.frame().resources(),r.frame().resources());}
            assertTrue(f.nightReads.get()>0);var frame=f.read(f.a,w).frame();
            assertEquals(2,frame.combat().world().actors().size());assertEquals(1,frame.actors().size());
            assertTrue(frame.combat().world().actors().stream().noneMatch(a->a.position().equals(frame.actors().get(0).cell())));
        }
    }
    @Test void typedAttackAndCooldownUseRealServiceAndSourceQualifiedWideHealth()throws Exception {
        try(var f=new Fixture()) {
            var initial=f.active();var w=initial.combat().world();var target=initial.actors().get(0);
            assertEquals(HUGE,initial.combat().vitals().get(0).maxHp());
            var request=f.request(f.a,w,1,target.serial(),target.life());var struck=f.service.npc(f.a.owner(),request);wire(request,struck);
            assertEquals(ModernNpcProtocol.Status.OK,struck.status());assertEquals(1,struck.frame().combat().vitals().get(0).attackSequence());
            assertEquals(0,struck.frame().combat().vitals().get(0).lastTargetEntityId());assertEquals(0,struck.frame().combat().vitals().get(0).attackRevision());
            assertTrue(struck.frame().exchanges().stream().anyMatch(e->e.participant().equals(ModernParticipantId.human(7,3333))&&e.requestSequence()==1));
            var again=f.request(f.a,w,2,target.serial(),target.life());var denied=f.service.npc(f.a.owner(),again);wire(again,denied);assertEquals(ModernNpcProtocol.Reason.TOO_FAST,denied.reason());
            assertEquals(0,denied.frame().combat().vitals().get(1).attackSequence());
            assertThrows(SecurityException.class,()->f.service.npc(f.b.owner(),request));
            var wrong=new ModernNpcProtocol.Request(f.a.scope(),ModernNpcProtocol.Action.READ,new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation()+1),0,0,0);
            assertThrows(SecurityException.class,()->f.service.npc(f.a.owner(),wrong));
        }
    }
    @Test void leavePublishesClosedRosterAndReentryGetsFreshGenerationAndPlan()throws Exception {
        try(var f=new Fixture()) {
            var initial=f.active();var w=initial.combat().world();
            assertNull(f.service.handle(f.a.owner(),new ModernWorldProtocol.Request(f.a.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ZERO)).frame());
            var terminal=f.read(f.b,w);wire(f.request(f.b,w,0,0,0),terminal);assertTrue(terminal.frame().closed());assertTrue(terminal.frame().actors().isEmpty());
            assertEquals(initial.resources(),terminal.frame().resources());assertEquals(ModernWorldProtocol.Phase.ABORTED,terminal.frame().combat().world().phase());
            assertThrows(SecurityException.class,()->f.read(f.a,w));
            f.service.handle(f.b.owner(),new ModernWorldProtocol.Request(f.b.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,w.matchId(),w.generation(),0,0,0,ZERO));
            f.ready(f.a);f.ready(f.b);var next=f.loading();assertNotEquals(w.matchId(),next.matchId());assertTrue(f.read(f.a,next).frame().actors().isEmpty());
            assertThrows(SecurityException.class,()->f.read(f.a,w));
        }
    }
    @Test void legacyAndUnnegotiatedClientsCannotReserveNpcWorldOrReadProfiles()throws Exception {
        for(int version:new int[]{17,18})try(var f=new Fixture()) {
            f.register(f.b,version,version==17?(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)):ModernSessionProtocol.RUNTIME_CAPABILITIES,null);
            var result=f.prepare();assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,result.status());assertNull(result.frame());assertEquals(0,f.physicalReads.get());assertFalse(f.view().summary().readySatisfied());
        }
    }
    @Test void cancelledFreshReadCannotPublishLateNpcRosterOrRunAi()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture()) {
            f.register(f.a,18,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM)),deadline->{entered.countDown();boolean interrupted=false;
                for(;;)try{assertTrue(release.await(3,TimeUnit.SECONDS));break;}catch(InterruptedException e){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();return f.fighter(f.a);});
            var w=f.prepare().frame();assertTrue(entered.await(1,TimeUnit.SECONDS));var unavailable=f.read(f.a,w);wire(f.request(f.a,w,0,0,0),unavailable);assertNull(unavailable.frame());
            f.service.detach(f.a.owner());release.countDown();await(()->f.service.active()==0);assertNull(f.read(f.b,w).frame());assertEquals(0,f.nightReads.get());
        }finally{release.countDown();}
    }
    @Test void clockOverridesPreserveNpcAdmissionAndPolicyRequiresExplicitProvider()throws Exception {
        try(var f=new Fixture()) {
            assertSame(f.mapping.npc(),f.mapping.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60_000)).npc());
            assertEquals(0,ModernSessionBoundary.Policy.world(true,false,false,false,false,false,false,false,false,false).supportedCapabilities()&ModernSessionProtocol.CAP_NPC);
            assertThrows(IllegalArgumentException.class,()->ModernSessionBoundary.Policy.world(true,false,false,false,false,false,false,false,false,false,true));
            assertNotEquals(0,ModernSessionBoundary.Policy.world(true,true,false,false,false,false,false,false,false,false,true).supportedCapabilities()&ModernSessionProtocol.CAP_NPC);
        }
    }
}
