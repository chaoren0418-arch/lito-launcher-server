package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual WorldService scheduler, preparation readers and owner-scoped dispatch. */
class ModernNpcDisintegrateServiceTest {
    @TempDir Path temporary;
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldBinding.Key key=new ModernWorldBinding.Key(2,554);
        final ModernRooms rooms=new ModernRooms(Set.of(key),Set.of(key));
        final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final AtomicInteger physicalReads=new AtomicInteger(),skillReads=new AtomicInteger();
        final ModernWorldService service;
        final ModernWorldService.Mapping mapping;
        Fixture(boolean bothCap,int authority)throws Exception {this(bothCap,authority,29);}
        Fixture(boolean bothCap,int authority,int version)throws Exception {
            var map=ModernWorldMap.fromSectorBytes(554,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(2,554,map);var lanes=List.of(ModernNpcServiceTest.Fixture.cell(10,10),ModernNpcServiceTest.Fixture.cell(20,10));
            var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,10000L,ModernNpcCatalog.Field.MR,0L),true,0);
            var encounter=new ModernNpcEncounter(key,catalog,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7),42,ModernNpcServiceTest.Fixture.blessing(42)),lanes,8,1000,
                    List.of(new ModernNpcRoster.Spawn(catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,ModernNpcServiceTest.Fixture.cell(11,10),2)),16,()->false).withDisintegrate(true);
            mapping=new ModernWorldService.Mapping(binding,lanes.get(0),1_000_000,10_000_000_000L,1_200_000_000L,false,false,true,true,false,false,true,null,false,null,lanes,null,encounter);
            service=new ModernWorldService(rooms,Map.of(key,mapping),new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000)),bound->0,null,Map.of(7,100.0,42,100.0));
            for(int source:List.of(7,42)) {
                var owner=presence.connect();var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"source-"+source,"human",80,1,0);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selected));rooms.enterLobby(owner,selected,1,0,true);
                var p=new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,3333,1,0));players.add(p);
                var row=new ModernApplicationProtocol.CharacterRow(3333,"human",1,0,80);
                var fighter=ModernPhysicalCombatTest.fighter(ModernBattleEngineNpcDisintegrateTest.source(ModernBattleEngineHonorTest.source(source,row,10000,1000),1000,500));
                var learned=List.of(ModernSkillsTest.teleport(),new ModernSkills.Skill(77,"$1485",70,5000,ModernSkills.Support.DISINTEGRATE));
                boolean enabled=source==7||bothCap;int caps=(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_NPC_FIRE_STORM))&~ModernSessionProtocol.CAP_DISINTEGRATE;
                if(!enabled)caps&=~ModernSessionProtocol.CAP_NPC_DISINTEGRATE;
                service.register(owner,p.scope(),deadline->{throw new AssertionError("no physical fallback");},deadline->{physicalReads.incrementAndGet();return fighter;},false,
                        deadline->new ModernSkills.Snapshot(source,row,learned),false,new ModernWorldService.MatchItems(){
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}
                            public void terminal(long m,long g){}
                        },true,false,false,true,false,false,new ModernSessionBoundary.Negotiated(1,enabled?version:28,1,caps),enabled?deadline->{
                            skillReads.incrementAndGet();assertTrue(System.nanoTime()<deadline);if(authority==2)throw new java.io.IOException("fixture qualification unavailable");
                            return new ModernDisintegrateCatalog.Snapshot(authority==1?new TournamentPresence.CharacterKey(99,3333):fighter.key(),ModernDisintegrateCatalog.Status.QUALIFIED);
                        }:null);
            }
            var a=players.get(0);accepted(a,ModernRooms.Action.CREATE,ModernRoomsTest.draft(2,2,554,0),0);
            accepted(players.get(1),ModernRooms.Action.JOIN,null,0);for(var p:players)accepted(p,ModernRooms.Action.READY,null,1);
        }
        ModernRooms.RoomSnapshot room(){return rooms.execute(players.get(0).owner(),new ModernRooms.Command(players.get(0).scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        void accepted(ModernRoomsTest.Player p,ModernRooms.Action action,ModernRooms.Draft draft,int value){var r=action==ModernRooms.Action.CREATE?null:room();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),action,r==null?0:r.summary().id(),r==null?0:r.summary().revision(),value,draft,"")));}
        ModernWorldProtocol.Reply prepare(){var a=players.get(0);var s=room().summary();return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.PREPARE,0,s.id(),s.revision(),0,0,0,0,0,ModernNpcServiceTest.ZERO));}
        ModernWorldProtocol.Frame world(){var a=players.get(0);return service.handle(a.owner(),new ModernWorldProtocol.Request(a.scope(),ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ModernNpcServiceTest.ZERO)).frame();}
        ModernWorldProtocol.Frame loading()throws Exception{assertEquals(ModernWorldProtocol.Status.OK,prepare().status());ModernNpcServiceTest.await(()->world().phase()!=ModernWorldProtocol.Phase.PREPARING);return world();}
        ModernNpcProtocol.Frame active()throws Exception {
            var w=loading();assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase(),"abort "+w.abortReason());
            for(var p:players)assertEquals(ModernWorldProtocol.Status.OK,service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LOADED,0,0,0,w.matchId(),w.generation(),0,0,0,w.manifest().digest())).status());
            return read(players.get(0));
        }
        ModernNpcProtocol.Frame read(ModernRoomsTest.Player p){var w=world();return service.npc(p.owner(),new ModernNpcProtocol.Request(p.scope(),ModernNpcProtocol.Action.READ,new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation()),0,0,0)).frame();}
        ModernNpcDisintegrateState.Request request(ModernNpcProtocol.Frame f,long seq){var n=f.actors().stream().filter(a->a.hp()>0).findFirst().orElseThrow();return new ModernNpcDisintegrateState.Request(players.get(0).scope(),new ModernNpcRoster.Id(f.scope(),n.serial()),seq,1,n.life());}
        public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.memberCount());assertEquals(0,presence.ownerCount());}
    }
    @Test void negotiatedServiceReadsFreshSkillAuthorityOnceAndDispatchDoesNotReadAgain()throws Exception {
        try(var f=new Fixture(true,0)) {
            var before=f.active();assertEquals(2,f.skillReads.get());assertEquals(2,f.physicalReads.get());var r=f.request(before,1);
            var hit=f.service.disintegrateNpc(f.players.get(0).owner(),r);assertEquals(NONE,hit.reason());
            assertEquals(930,hit.frame().combat().vital(1).mp());assertEquals(501,hit.frame().combat().npcDisintegrations().get(0).lawful());
            var message=new ModernApplicationProtocol.NpcDisintegrateResult(91,r,hit);
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,29),29));
            var denied=f.service.disintegrateNpc(f.players.get(0).owner(),f.request(hit.frame(),2));assertEquals(TOO_FAST,denied.reason());assertEquals(hit.frame().combat().npcDisintegrations(),denied.frame().combat().npcDisintegrations());
            assertThrows(SecurityException.class,()->f.service.disintegrateNpc(f.players.get(0).owner(),r));assertEquals(2,f.skillReads.get());
        }
    }
    @Test void missingPeerCapabilityRejectsPreparationBeforeAnySourceReads()throws Exception {
        try(var f=new Fixture(false,0)){assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());assertEquals(0,f.skillReads.get());}
    }
    @Test void wrongSourceAndUnavailableAuthorityAbortWithoutDefaultSkillPermission()throws Exception {
        for(int kind:List.of(1,2))try(var f=new Fixture(true,kind)){assertEquals(ModernWorldProtocol.Phase.ABORTED,f.loading().phase());assertTrue(f.skillReads.get()>0);}
    }
    @Test void wrongOwnerEpochChannelGenerationAndLivesCannotConsumeCastSequence()throws Exception {
        try(var f=new Fixture(true,0)) {
            var before=f.active();var r=f.request(before,1);assertThrows(SecurityException.class,()->f.service.disintegrateNpc(f.players.get(1).owner(),r));
            for(int kind=0;kind<5;kind++) {
                var p=r.player();var scope=kind==0?new ModernRooms.Scope(p.sourceId(),p.objectId(),p.epoch()+1,p.channelId()):kind==1?new ModernRooms.Scope(p.sourceId(),p.objectId(),p.epoch(),1):p;
                var id=kind==2?new ModernNpcRoster.Id(new ModernNpcRoster.Scope(before.scope().roomId(),before.scope().matchId(),before.scope().generation()+1),r.target().serial()):r.target();
                var bad=new ModernNpcDisintegrateState.Request(scope,id,1,kind==3?2:1,kind==4?2:1);assertThrows(SecurityException.class,()->f.service.disintegrateNpc(f.players.get(0).owner(),bad));
            }
            var after=f.read(f.players.get(0));assertEquals(0,after.combat().world().casts().get(0).requestSequence());assertEquals(before.combat().npcDisintegrations(),after.combat().npcDisintegrations());
        }
    }
    @Test void configuredRulesRequireRegenAndSpellsPreserveOverridesAndSeparateQuestOutbox()throws Exception {
        var p=ModernQuestConfigurationTest.world(temporary);var prefix=ModernNpcConfigurationTest.PREFIX;
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");
        byte[] old;try(var c=ModernQuestConfigurationTest.config(temporary,p)){old=ModernSettlementCoordinator.binding(c);}
        p.setProperty(prefix+"npcDisintegrateRules",ModernDisintegrate.RULESET);
        try(var c=ModernQuestConfigurationTest.config(temporary,p)) {
            assertTrue(c.worlds().npcDisintegrateEnabled());assertFalse(c.worlds().disintegrateEnabled());assertTrue(c.worlds().magicQualificationEnabled());assertFalse(c.worlds().damageMagicEnabled());
            var encounters=ModernNpcConfigurationTest.encounter(c.worlds());var mapping=c.worlds().load(null,()->false,encounters).get(ModernNpcConfigurationTest.KEY);
            assertTrue(mapping.npcDisintegrateEnabled());assertTrue(mapping.withClockOverrides(mapping.clockOverrides()).npcDisintegrateEnabled());
            assertTrue(ModernMatchDescriptor.of(mapping,new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,60000)),true).has(ModernMatchDescriptor.NPC_DISINTEGRATE));
            assertThrows(java.io.IOException.class,()->c.worlds().load(null,()->false,Map.of(mapping.binding().key(),mapping.npc().withDisintegrate(false))));
            assertFalse(Arrays.equals(old,ModernSettlementCoordinator.binding(c)));
        }
        for(String key:List.of("spellRules","regenRules","castMillis")){var bad=(Properties)p.clone();bad.remove(prefix+key);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));}
        p.setProperty(prefix+"npcDisintegrateRules","true");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(p));
    }
    @Test void timeAttackNpc77PolicyHasSeparateResultNamespaceAndPreservesCooperativeRules()throws Exception {
        var p=ModernTimeAttackConfigurationTest.world(temporary);String prefix=ModernTimeAttackConfigurationTest.PREFIX;
        for(var row:Map.of("itemRules",ModernWorldConfiguration.ITEM_RULESET,"weightRules",ModernWorldConfiguration.WEIGHT_RULESET,"regenRules",ModernWorldConfiguration.REGEN_RULESET,"spellRules",ModernWorldConfiguration.SPELL_RULESET,"castMillis","1200").entrySet())p.setProperty(prefix+row.getKey(),row.getValue());
        byte[] old;try(var c=ModernQuestConfigurationTest.config(temporary,p)){old=ModernSettlementCoordinator.binding(c);}
        p.setProperty(prefix+"npcDisintegrateRules",ModernDisintegrate.RULESET);
        try(var c=ModernQuestConfigurationTest.config(temporary,p)) {
            var npc=ModernTimeAttackConfigurationTest.encounter(c.worlds());var m=c.worlds().load(null,()->false,Map.of(ModernTimeAttackConfigurationTest.KEY,npc)).get(ModernTimeAttackConfigurationTest.KEY);
            assertTrue(m.npcDisintegrateEnabled());assertNotNull(m.timeAttack());assertFalse(m.npc().lightArrow());assertFalse(Arrays.equals(old,ModernSettlementCoordinator.binding(c)));
        }
    }
}
