package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real canonical preparation, scheduler, target command and owner publication. */
class ModernHasteNpcServiceTest {
    static final String ZERO="0".repeat(64);
    static final long HUGE=9_007_199_254_740_993L;
    @Test void preparesNpcHasteWorldCastsAndLeavesWithoutLegacyProjection()throws Exception {
        try(var f=new Fixture()) {
            f.active();var v=f.read(f.a).view();var n=v.world().npcs().actors().get(0);
            var id=new ModernNpcRoster.Id(new ModernNpcRoster.Scope(f.world.roomId(),f.world.matchId(),f.world.generation()),n.serial());
            var target=new ModernHasteCast.Npc(id,n.life(),n.changedRevision(),n.cell());
            var result=f.service.haste(f.a.owner(),f.intent(f.a,1,target));
            assertEquals(ModernHasteCast.Reason.NONE,result.reason());assertEquals(260,result.view().personal().history().resources().vitals().mp());
            assertEquals(1,result.view().world().hasteEffects().targets().size());
            assertTrue(result.view().world().hasteEffects().npcs().actors().get(0).speed().hasteUntilMillis()>0);
            assertDoesNotThrow(()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.HasteResult(result),44));
            assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.HasteResult(result),43));
            assertEquals(ModernHasteCast.Reason.STALE_TARGET,f.service.haste(f.a.owner(),f.intent(f.a,2,target)).reason());
            for(var p:List.of(f.a,f.b))assertEquals(ModernWorldProtocol.Status.OK,f.service.battleWorld(p.owner(),f.request(p,ModernBattleWorldProtocol.Action.LEAVE)).status());
            ModernWorldServiceTest.awaitNoMatches(f.service);
        }
    }
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
            mapping=new ModernWorldService.Mapping(binding,lanes.get(0),1_000_000,10_000_000_000L,2_000_000_000L,false,false,true,true,true,false,false,null,false,null,lanes,null,setup,null,false,100,false,true);
            var limits=new ModernWorldService.Limits(2,1,1,2000,quest?20:1000,new ModernBattleEngine.Limits(2000,0,60_000));
            service=new ModernWorldService(rooms,Map.of(key,mapping),limits,bound->0,null,Map.of(7,100.0,42,100.0));
            ModernRoomsTest.accepted(command(a,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,2,logical,0)));
            var summary=view().summary();ModernRoomsTest.accepted(command(b,ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null));
            for(var p:List.of(a,b)){register(p);ready(p);}
        }
        static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(32768+x,32768+y);}
        static ModernNpcMelee.BlessingBinding blessing(int source){return new ModernNpcMelee.BlessingBinding(source,"catalog",ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED);}
        ModernRoomsTest.Player player(int source) {
            var owner=presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"server-"+source,"human",80,1,0);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
            return new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,3333,1,0));
        }
        ModernApplicationProtocol.CharacterRow row(ModernRoomsTest.Player p){var s=presence.selected(p.owner());return new ModernApplicationProtocol.CharacterRow(s.key().objectId(),s.characterName(),s.characterClass(),s.sex(),s.level());}
        void register(ModernRoomsTest.Player p) {
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),row(p),HUGE,300,12,0);
            service.register(p.owner(),p.scope(),d->row(p),d->fighter,false,
                    d->new ModernSkills.Snapshot(p.scope().sourceId(),row(p),List.of(new ModernSkills.Skill(43,"$1463",40,10,ModernSkills.Support.UNIMPLEMENTED))),
                    false,new ModernWorldService.MatchItems(){
                        public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}
                        public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){return List.of();}
                        public void terminal(long m,long g){}
                    },true,true,false,false,false,false,
                    new ModernSessionBoundary.Negotiated(1,44,1,ModernHasteServiceTest.CAPS|ModernSessionProtocol.CAP_NPC),null,null,
                    d->new ModernHasteCatalog.Snapshot(new TournamentPresence.CharacterKey(p.scope().sourceId(),3333),ModernHasteCatalog.Status.QUALIFIED));
        }
        ModernRooms.Result command(ModernRoomsTest.Player p,ModernRooms.Action action,long id,long revision,int value,ModernRooms.Draft draft){return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,id,revision,value,draft,""));}
        ModernRooms.RoomSnapshot view(){return command(a,ModernRooms.Action.READ,0,0,0,null).view().joined();}
        void ready(ModernRoomsTest.Player p){var s=view().summary();ModernRoomsTest.accepted(command(p,ModernRooms.Action.READY,s.id(),s.revision(),1,null));}
        ModernTargetHistory.World world;
        ModernBattleWorldProtocol.Request request(ModernRoomsTest.Player p,ModernBattleWorldProtocol.Action action) {
            return new ModernBattleWorldProtocol.Request(p.scope(),action,world.roomId(),0,world.matchId(),world.generation(),
                    action==ModernBattleWorldProtocol.Action.LOADED?mapping.binding().map().digest():ZERO);
        }
        ModernBattleWorldProtocol.Reply read(ModernRoomsTest.Player p){return service.battleWorld(p.owner(),request(p,ModernBattleWorldProtocol.Action.READ));}
        void active()throws Exception {
            var summary=view().summary();
            var q=new ModernBattleWorldProtocol.Request(a.scope(),ModernBattleWorldProtocol.Action.PREPARE,summary.id(),summary.revision(),0,0,ZERO);
            var prepared=service.battleWorld(a.owner(),q);assertEquals(ModernWorldProtocol.Status.OK,prepared.status());world=prepared.world();
            ModernNpcServiceTest.await(()->read(a).view()!=null);
            for(var p:List.of(a,b))assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(p.owner(),request(p,ModernBattleWorldProtocol.Action.LOADED)).status());
            ModernNpcServiceTest.await(()->!read(a).view().world().npcs().actors().isEmpty());
        }
        ModernHasteProtocol.Request intent(ModernRoomsTest.Player p,long sequence,ModernHasteCast.Target target){
            var v=read(p).view();var actor=v.world().actors().stream().filter(x->x.pose().entityId()==v.personal().entityId()).findFirst().orElseThrow();
            return new ModernHasteProtocol.Request(1,new ModernBattleCommandLife.Proof(p.scope(),world,actor.pose().entityId(),actor.resources().life()+1,v.world().revision()),
                    sequence,actor.resources().equipmentRevision(),actor.pose().position().x(),actor.pose().position().y(),actor.buffs().revision(),target);
        }
        @Override public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));for(var p:List.of(a,b)){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
    }
}
