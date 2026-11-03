package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernFireStormServiceTest {
    @Test void everyHumanMustNegotiateFireBeforeReadersAndDisabledBindingStillPublishesEmptyFireVector()throws Exception{
        for(int scenario=0;scenario<3;scenario++){
            boolean enabled=scenario!=2,secondCap=scenario!=0;var rooms=new ModernRoomsTest.Fixture();
            var a=rooms.player(7,3333,80,0);var b=rooms.player(42,3333,80,0);var reads=new AtomicInteger();
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);var other=new ModernWorldBinding(1,509,map);
            var cell=new ModernWorldMap.Cell(32778,32778);
            var selected=new ModernWorldService.Mapping(binding,cell,200_000_000,1_000_000_000,700_000_000,true,false,true,true,false,false,true,null,enabled);
            var available=new ModernWorldService.Mapping(other,cell,200_000_000,1_000_000_000,700_000_000,true,false,true,true,false,false,true,null,true);
            try(var service=new ModernWorldService(rooms.rooms,Map.of(binding.key(),selected,other.key(),available),
                    new ModernWorldService.Limits(2,1,1,2000,5,new ModernBattleEngine.Limits(2000,0,10000)),bound->0,null,Map.of(7,100.0,42,100.0))){
                ModernRoomsTest.accepted(rooms.create(a,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(rooms.join(b,rooms.view(a).joined().summary(),""));
                for(var p:List.of(a,b)){
                    var selection=rooms.presence.selected(p.owner());var row=new ModernApplicationProtocol.CharacterRow(3333,selection.characterName(),selection.characterClass(),selection.sex(),80);
                    var fighter=ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(p.scope().sourceId(),row,100,300));
                    service.register(p.owner(),p.scope(),deadline->{throw new AssertionError("physical exact source bundle required");},deadline->{reads.incrementAndGet();return fighter;},true,
                            deadline->new ModernSkills.Snapshot(p.scope().sourceId(),row,List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM))),true,
                            new ModernWorldService.MatchItems(){
                                public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline){throw new AssertionError("weighted capture must carry its fresh basis");}
                                public List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline,ModernInventoryWeight.Basis basis){
                                    assertNotNull(basis);assertTrue(basis.sameFacts(ModernInventoryWeight.qualify(fighter.profile(),100.0)));return List.of();
                                }
                                public void terminal(long match,long generation){}
                            },
                            true,false,false,true,false,p==a||secondCap);
                    ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
                }
                var summary=rooms.view(a).joined().summary();var reply=service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,summary.id(),summary.revision(),0,0,0,0,0,"0".repeat(64)));
                if(!secondCap){assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,reply.status());assertNull(reply.frame());assertEquals(0,reads.get());assertFalse(rooms.rooms.isPreparing(a.owner()));}
                else {
                    assertEquals(ModernWorldProtocol.Status.OK,reply.status());long until=System.nanoTime()+3_000_000_000L;ModernWorldProtocol.Frame world;
                    do{world=service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,"0".repeat(64))).frame();
                        assertTrue(System.nanoTime()-until<0);if(world.phase()==ModernWorldProtocol.Phase.PREPARING)Thread.sleep(2);
                    }while(world.phase()==ModernWorldProtocol.Phase.PREPARING);
                    assertEquals(ModernWorldProtocol.Phase.LOADING,world.phase(),"scenario="+scenario+" abort="+world.abortReason());assertEquals(2,reads.get());
                    var frame=service.combat(a.owner(),new ModernCombatProtocol.Request(a.scope(),ModernCombatProtocol.Action.READ,world.matchId(),world.generation(),0,0)).frame();
                    assertEquals(enabled?2:0,frame.fireStorms().size());
                }
                service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());
            }finally{for(var p:List.of(a,b)){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
            assertEquals(0,rooms.rooms.memberCount());assertEquals(0,rooms.presence.ownerCount());
        }
    }
}
