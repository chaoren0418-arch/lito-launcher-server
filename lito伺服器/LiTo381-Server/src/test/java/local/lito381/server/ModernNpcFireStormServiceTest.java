package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcFireStormState.Reason.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual WorldService scheduler, preparation readers and owner-scoped dispatch. */
class ModernNpcFireStormServiceTest {
    @TempDir Path temporary;
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldBinding.Key key=new ModernWorldBinding.Key(2,554);
        final ModernRooms rooms=new ModernRooms(Set.of(key),Set.of(key));
        final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final AtomicInteger physicalReads=new AtomicInteger(),skillReads=new AtomicInteger();
        final ModernWorldService service;
        final ModernWorldService.Mapping mapping;
        Fixture(boolean bothCap,int authority)throws Exception {this(bothCap,authority,true);}
        Fixture(boolean bothCap,int authority,boolean populate)throws Exception {this(bothCap,authority,populate,31);}
        Fixture(boolean bothCap,int authority,boolean populate,int version)throws Exception {
            var map=ModernWorldMap.fromSectorBytes(554,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(2,554,map);var lanes=List.of(ModernNpcServiceTest.Fixture.cell(10,10),ModernNpcServiceTest.Fixture.cell(20,10));
            var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,10000L,ModernNpcCatalog.Field.MR,0L),true,0);
            var encounter=new ModernNpcEncounter(key,catalog,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7),42,ModernNpcServiceTest.Fixture.blessing(42)),lanes,8,1000,
                    List.of(new ModernNpcRoster.Spawn(catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,ModernNpcServiceTest.Fixture.cell(11,10),2)),16,()->false).withFireStorm(true);
            mapping=new ModernWorldService.Mapping(binding,lanes.get(0),1_000_000,10_000_000_000L,1_200_000_000L,false,false,true,true,false,false,true,null,false,null,lanes,null,encounter);
            service=new ModernWorldService(rooms,Map.of(key,mapping),new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000)),bound->0,null,Map.of(7,100.0,42,100.0));
            if(!populate)return;
            for(int source:List.of(7,42)) {
                var owner=presence.connect();var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"source-"+source,"human",80,1,0);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selected));rooms.enterLobby(owner,selected,1,0,true);
                var p=new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,3333,1,0));players.add(p);
                var row=new ModernApplicationProtocol.CharacterRow(3333,"human",1,0,80);
                var fighter=ModernPhysicalCombatTest.fighter(ModernBattleEngineNpcDisintegrateTest.source(ModernBattleEngineHonorTest.source(source,row,10000,1000),1000,500));
                var learned=List.of(ModernSkillsTest.teleport(),new ModernSkills.Skill(70,"$1478",48,2000,authority==4?ModernSkills.Support.UNSUPPORTED_TEMPLATE:ModernSkills.Support.FIRE_STORM));
                boolean enabled=source==7||bothCap;int caps=ModernSessionProtocol.RUNTIME_CAPABILITIES|ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_NPC_FIRE_STORM;
                if(version>=37)caps|=ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_BATTLE_DATA;
                if(!enabled)caps&=~ModernSessionProtocol.CAP_NPC_FIRE_STORM;
                service.register(owner,p.scope(),deadline->{throw new AssertionError("no physical fallback");},deadline->{physicalReads.incrementAndGet();return fighter;},false,
                        deadline->{skillReads.incrementAndGet();assertTrue(System.nanoTime()<deadline);if(authority==2)throw new java.io.IOException("fixture qualification unavailable");return new ModernSkills.Snapshot(authority==1?99:source,row,authority==3?List.of(ModernSkillsTest.teleport()):learned);},false,new ModernWorldService.MatchItems(){
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}
                            public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}
                            public void terminal(long m,long g){}
                        },true,false,false,true,false,false,new ModernSessionBoundary.Negotiated(1,enabled?version:30,1,caps));
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
        ModernNpcFireStormState.Request request(ModernNpcProtocol.Frame f,long seq){return new ModernNpcFireStormState.Request(players.get(0).scope(),f.scope(),seq,1);}
        public void close()throws Exception {service.close();assertTrue(service.awaitTermination(4000));assertTrue(service.isQuiescent());for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.memberCount());assertEquals(0,presence.ownerCount());}
    }
    @Test void negotiatedServiceCapturesSourceSkillsOnceAndReturnsExactAtomicReceipt()throws Exception {
        try(var f=new Fixture(true,0)) {
            var before=f.active();assertTrue(before.combat().world().descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM));
            assertFalse(before.combat().world().descriptor().has(ModernMatchDescriptor.FIRE_STORM));
            assertEquals(2,f.skillReads.get());assertEquals(2,f.physicalReads.get());var r=f.request(before,1);
            var hit=f.service.fireStormNpc(f.players.get(0).owner(),r);assertEquals(NONE,hit.reason());
            var state=hit.frame().combat().npcFireStorms().get(0);
            assertEquals(1,state.castCount());assertEquals(1000-state.mpCost(),hit.frame().combat().vital(1).mp());
            assertFalse(state.targets().isEmpty());assertTrue(state.targets().get(0).outcome().appliedDamage()>0);
            var message=new ModernApplicationProtocol.NpcFireStormResult(91,r,hit);ModernNpcFireStormProtocolTest.roundtrip(message);
            var peer=new ModernSessionBoundary.Negotiated(1,31,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_GREATER_HASTE));
            assertEquals(message,ModernMatchDescriptor.forPeer(message,peer));
            var denied=f.service.fireStormNpc(f.players.get(0).owner(),f.request(hit.frame(),2));
            assertEquals(TOO_FAST,denied.reason());assertEquals(hit.frame().combat().npcFireStorms(),denied.frame().combat().npcFireStorms());
            ModernNpcFireStormProtocolTest.roundtrip(new ModernApplicationProtocol.NpcFireStormResult(92,f.request(hit.frame(),2),denied));
            assertThrows(SecurityException.class,()->f.service.fireStormNpc(f.players.get(0).owner(),r));
            assertEquals(2,f.skillReads.get());assertEquals(2,f.physicalReads.get());
        }
    }
    @Test void oldPeerCannotPrepareNewMatchOrCauseSourceReads()throws Exception {
        try(var f=new Fixture(false,0)){assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,f.prepare().status());assertEquals(0,f.physicalReads.get());assertEquals(0,f.skillReads.get());}
    }
    @Test void wrongSourceAndUnavailableAuthorityAbortWithoutDefaultSkillPermission()throws Exception {
        for(int kind:List.of(1,2))try(var f=new Fixture(true,kind)){assertEquals(ModernWorldProtocol.Phase.ABORTED,f.loading().phase());assertTrue(f.skillReads.get()>0);}
    }
    @Test void unlearnedAndUnsupportedSourceTemplatesCannotSpendManaOrDamageTargets()throws Exception {
        for(int kind:List.of(3,4))try(var f=new Fixture(true,kind)){
            var before=f.active();var r=f.request(before,1);var denied=f.service.fireStormNpc(f.players.get(0).owner(),r);
            assertEquals(kind==3?UNLEARNED:UNSUPPORTED,denied.reason());
            assertEquals(before.combat().npcFireStorms(),denied.frame().combat().npcFireStorms());
            assertEquals(1000,denied.frame().combat().vital(1).mp());
            ModernNpcFireStormProtocolTest.roundtrip(new ModernApplicationProtocol.NpcFireStormResult(91,r,denied));
        }
    }
    @Test void wrongOwnerSourceEpochChannelMatchGenerationAndLifeCannotConsumeSequence()throws Exception {
        try(var f=new Fixture(true,0)) {
            var before=f.active();var r=f.request(before,1);
            assertThrows(SecurityException.class,()->f.service.fireStormNpc(f.players.get(1).owner(),r));
            for(int kind=0;kind<7;kind++){
                var p=r.player();var w=r.world();
                var scope=new ModernRooms.Scope(kind==0?42:p.sourceId(),p.objectId(),kind==1?p.epoch()+1:p.epoch(),kind==2?1:p.channelId());
                var world=new ModernNpcRoster.Scope(kind==3?w.roomId()+1:w.roomId(),kind==4?w.matchId()+1:w.matchId(),kind==5?w.generation()+1:w.generation());
                var bad=new ModernNpcFireStormState.Request(scope,world,1,kind==6?2:1);
                assertThrows(SecurityException.class,()->f.service.fireStormNpc(f.players.get(0).owner(),bad));
            }
            var after=f.read(f.players.get(0));
            assertEquals(0,after.combat().world().casts().get(0).requestSequence());assertEquals(before.combat().npcFireStorms(),after.combat().npcFireStorms());
            assertEquals(NONE,f.service.fireStormNpc(f.players.get(0).owner(),r).reason());
        }
    }
    @Test void configuredRulesRequireRegenAndSpellsPreserveOverridesAndSeparateQuestOutbox()throws Exception {
        var p=ModernQuestConfigurationTest.world(temporary);var prefix=ModernNpcConfigurationTest.PREFIX;
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");
        byte[] old;try(var c=ModernQuestConfigurationTest.config(temporary,p)){old=ModernSettlementCoordinator.binding(c);}
        p.setProperty(prefix+"npcFireStormRules",ModernNpcFireStorm.RULESET);
        try(var c=ModernQuestConfigurationTest.config(temporary,p)) {
            assertTrue(c.worlds().npcFireStormEnabled());assertFalse(c.worlds().fireStormEnabled());assertTrue(c.worlds().magicQualificationEnabled());assertFalse(c.worlds().damageMagicEnabled());
            var encounters=ModernNpcConfigurationTest.encounter(c.worlds());var mapping=c.worlds().load(null,()->false,encounters).get(ModernNpcConfigurationTest.KEY);
            assertTrue(mapping.npcFireStormEnabled());assertTrue(mapping.withClockOverrides(mapping.clockOverrides()).npcFireStormEnabled());
            var combined=mapping.npc().withLightArrow(true).withDisintegrate(true);assertTrue(combined.fireStorm());assertTrue(combined.lightArrow());assertTrue(combined.disintegrate());
            assertTrue(combined.withLightArrow(false).withDisintegrate(false).fireStorm());assertFalse(combined.withFireStorm(false).fireStorm());
            assertTrue(ModernMatchDescriptor.of(mapping,new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,60000)),true).has(ModernMatchDescriptor.NPC_FIRE_STORM));
            assertThrows(java.io.IOException.class,()->c.worlds().load(null,()->false,Map.of(mapping.binding().key(),mapping.npc().withFireStorm(false))));
            assertFalse(Arrays.equals(old,ModernSettlementCoordinator.binding(c)));
        }
        for(String key:List.of("spellRules","regenRules","castMillis")){var bad=(Properties)p.clone();bad.remove(prefix+key);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));}
        p.setProperty(prefix+"npcFireStormRules","true");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(p));
    }
    @Test void timeAttackNpc70PolicyHasSeparateResultNamespaceAndPreservesCooperativeRules()throws Exception {
        var p=ModernTimeAttackConfigurationTest.world(temporary);String prefix=ModernTimeAttackConfigurationTest.PREFIX;
        for(var row:Map.of("itemRules",ModernWorldConfiguration.ITEM_RULESET,"weightRules",ModernWorldConfiguration.WEIGHT_RULESET,"regenRules",ModernWorldConfiguration.REGEN_RULESET,"spellRules",ModernWorldConfiguration.SPELL_RULESET,"castMillis","1200").entrySet())p.setProperty(prefix+row.getKey(),row.getValue());
        byte[] old;try(var c=ModernQuestConfigurationTest.config(temporary,p)){old=ModernSettlementCoordinator.binding(c);}
        p.setProperty(prefix+"npcFireStormRules",ModernNpcFireStorm.RULESET);
        try(var c=ModernQuestConfigurationTest.config(temporary,p)) {
            var npc=ModernTimeAttackConfigurationTest.encounter(c.worlds());var m=c.worlds().load(null,()->false,Map.of(ModernTimeAttackConfigurationTest.KEY,npc)).get(ModernTimeAttackConfigurationTest.KEY);
            assertTrue(m.npcFireStormEnabled());assertNotNull(m.timeAttack());assertFalse(m.npc().lightArrow());assertFalse(Arrays.equals(old,ModernSettlementCoordinator.binding(c)));
        }
    }
    @Test void liveTimeAttackPreparationAndTowerCastUseTheNegotiatedNpc70Projection()throws Exception {
        var p=ModernTimeAttackConfigurationTest.world(temporary);String prefix=ModernTimeAttackConfigurationTest.PREFIX;
        for(var row:Map.of("itemRules",ModernWorldConfiguration.ITEM_RULESET,"weightRules",ModernWorldConfiguration.WEIGHT_RULESET,
                "regenRules",ModernWorldConfiguration.REGEN_RULESET,"spellRules",ModernWorldConfiguration.SPELL_RULESET,"castMillis","1200",
                "npcFireStormRules",ModernNpcFireStorm.RULESET).entrySet())p.setProperty(prefix+row.getKey(),row.getValue());
        ModernWorldService.Mapping mapping;
        try(var config=ModernQuestConfigurationTest.config(temporary,p)) {
            var encounter=ModernTimeAttackConfigurationTest.encounter(config.worlds());
            mapping=config.worlds().load(null,()->false,Map.of(ModernTimeAttackConfigurationTest.KEY,encounter)).get(ModernTimeAttackConfigurationTest.KEY);
        }
        var key=mapping.binding().key();
        var presence=new TournamentPresence();var owner=presence.connect();
        var rooms=new ModernRooms(Set.of(key),Set.of(),Map.of(key,60));
        var journal=new ModernTimeAttackServiceTest.Journal();
        var service=new ModernWorldService(rooms,Map.of(key,mapping),new ModernWorldService.Limits(2,1,1,2000,1000,
                new ModernBattleEngine.Limits(2000,0,60000)),bound->0,journal,Map.of(7,100.0));
        try {
            var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"Sun","human",80,1,0);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selected));rooms.enterLobby(owner,selected,1,0,true);
            var scope=new ModernRooms.Scope(7,3333,1,0);var row=new ModernApplicationProtocol.CharacterRow(3333,"human",1,0,80);
            var fighter=ModernPhysicalCombatTest.fighter(ModernBattleEngineNpcDisintegrateTest.source(ModernBattleEngineHonorTest.source(7,row,10000,1000),1000,500));
            int caps=ModernSessionProtocol.RUNTIME_CAPABILITIES|ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_NPC_FIRE_STORM;
            service.register(owner,scope,deadline->{throw new AssertionError("no roster fallback");},deadline->fighter,true,
                    deadline->new ModernSkills.Snapshot(7,row,List.of(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM))),false,
                    new ModernWorldService.MatchItems(){
                        public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}
                        public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}
                        public void terminal(long m,long g){}
                    },true,false,false,true,false,false,new ModernSessionBoundary.Negotiated(1,31,1,caps));
            var created=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(60,4,540,0),""));
            ModernRoomsTest.accepted(created);var room=created.view().joined().summary();
            var ready=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READY,room.id(),room.revision(),1,null,""));
            ModernRoomsTest.accepted(ready);room=ready.view().joined().summary();
            var admitted=service.handle(owner,new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.PREPARE,0,room.id(),room.revision(),0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));
            assertEquals(ModernWorldProtocol.Status.OK,admitted.status());
            var read=new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST);
            ModernNpcServiceTest.await(()->service.handle(owner,read).frame().phase()!=ModernWorldProtocol.Phase.PREPARING);
            var loading=service.handle(owner,read).frame();
            assertEquals(ModernWorldProtocol.Phase.LOADING,loading.phase(),"live projection must not abort: "+loading.abortReason());
            assertTrue(loading.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM));
            assertEquals(ModernWorldProtocol.Status.OK,service.handle(owner,new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.LOADED,0,0,0,loading.matchId(),loading.generation(),0,0,0,loading.manifest().digest())).status());
            var request=new ModernNpcFireStormState.Request(scope,new ModernNpcRoster.Scope(room.id(),loading.matchId(),loading.generation()),1,1);
            var result=service.fireStormNpc(owner,request);assertEquals(NONE,result.reason());
            assertEquals(0,result.frame().combat().world().timeAttack().towerHp());
            assertEquals(1,result.frame().combat().npcFireStorms().get(0).castCount());
            assertTrue(result.frame().combat().npcFireStorms().get(0).structure().hit().outcome().death());
            ModernNpcFireStormProtocolTest.roundtrip(new ModernApplicationProtocol.NpcFireStormResult(93,request,result));
        } finally {
            service.close();assertTrue(service.awaitTermination(5000));assertTrue(service.isQuiescent());
            rooms.disconnect(owner);presence.disconnect(owner);assertEquals(0,presence.ownerCount());
        }
    }
}
