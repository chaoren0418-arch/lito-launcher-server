package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.NONE;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Real authenticated, SELECT-only SQL -> shared514 engine -> durable gear.
 * Internal admission only: not WorldService negotiation, transport or UI. */
class ModernPlayerHealingRealMysqlTest {
    static final long SECOND=1_000_000_000L;
    record Read(ModernPhysicalCombat.Fighter fighter,ModernSkills.Snapshot skills) {}
    static Read read(ModernBackendExecutor.SessionGate gate,ModernEquipmentAuthority.PhysicalProvider provider,
            ModernAccountAuthentication.Principal principal,ModernJdbcBackend dao,int id)throws Exception {
        return gate.call(10000,scope->{
            var loaded=provider.read(principal,id,3333);var raw=loaded.source();
            var binding=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(id,3333),loaded.ownerBinding());
            var fighter=ModernPhysicalCombat.qualify(raw,binding,Optional.empty(),System.currentTimeMillis());
            var skills=dao.skillsCharacter(id,principal.accountForSource(id),3333,ModernQuestSqlFixture.CATALOG).orElseThrow();
            return new Read(fighter,skills);
        });
    }
    @Test @Timeout(value=180,unit=TimeUnit.SECONDS)
    void nonHonorSqlSourceToEngineAndDurableGearKeepZeroCostReceiptsAndReadonlyCharacters()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null,"explicit owned MySQL and shared root required");
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.openTargetHaste(Path.of(mysql),Path.of(shared),tls);
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            install(f);assertFalse(f.config.worlds().honorEnabled());
            var sun=new ModernJdbcBackend(f.config.sources().get(0).database(),1000,5000);
            var moon=new ModernJdbcBackend(f.config.sources().get(1).database(),1000,5000);
            var sources=Map.of(7,new ModernEquipmentRepository.Source(f.config.sources().get(0),sun),42,new ModernEquipmentRepository.Source(f.config.sources().get(1),moon));
            var physical=ModernEquipmentRepository.physicalJdbc(sources,true,false,false,false,false,true);
            assertTrue(physical.playerHealingSource(7));assertTrue(physical.playerHealingSource(42));assertFalse(physical.playerHealingSource(99));
            var legacyPhysical=ModernEquipmentRepository.physicalJdbc(sources,true,false,false,true,false);
            assertFalse(legacyPhysical.playerHealingSource(7),"Honor configuration does not opt into player healing");
            ModernEquipmentAuthority.PhysicalProvider legacyLambda=(p,s,id)->{throw new AssertionError("configuration check must not read a character");};
            assertFalse(legacyLambda.playerHealingSource(7));
            var verifier=ModernAccountAuthentication.repositories(Map.of(
                    7,ModernAccountAuthentication.SourceBinding.direct("太陽神",sun::authenticate),
                    42,ModernAccountAuthentication.SourceBinding.direct("月亮神",moon::authenticate)),account->false);
            var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
            assertEquals("fixture-user",principal.accountForSource(7));assertEquals("FIXTURE-USER",principal.accountForSource(42));
            var first=read(gate,physical,principal,sun,7);var second=read(gate,physical,principal,moon,42);
            assertNotEquals(first.fighter().key(),second.fighter().key());assertEquals(3333,first.fighter().key().objectId());
            assertEquals(5_000_000_000L,first.fighter().profile().effective().intelligence());
            assertEquals(0,ModernPlayerHealing.qualify(first.fighter()).mpCost());assertEquals(4,ModernPlayerHealing.qualify(second.fighter()).mpCost());
            assertTrue(ModernPlayerHealing.qualify(first.fighter()).accepts(first.skills()));assertTrue(ModernPlayerHealing.qualify(second.fighter()).accepts(second.skills()));
            assertFalse(ModernPlayerHealing.qualify(first.fighter()).accepts(second.skills()));
            assertTrue(gate.call(10000,scope->moon.physicalCharacter(42,"fixture-user",3333,ModernQuestSqlFixture.CATALOG,true,false,false,false,false,true)).isEmpty());
            assertThrows(SecurityException.class,()->physical.read(principal,99,3333));
            var original=ModernSlowMainSqlTest.sourceRows(f);
            var mapping=ModernMain.loadMappings(f.config).get(new ModernWorldBinding.Key(1,514));assertNotNull(mapping);assertNull(mapping.honor());
            var equipmentDirectory=f.directory.resolve("equipment");
            try(var arena=new Arena(mapping,List.of(first,second));var store=new ModernEquipmentOverrideStore(equipmentDirectory)) {
                var authority=new ModernEquipmentAuthority(ModernEquipmentRepository.jdbc(sources),null,physical,store);
                var player=arena.players.get(0);
                try(var lease=new ModernEquipmentLease.Issuer().open(player.owner(),first.fighter().key(),1,()->{})) {
                    var gear=gate.call(10000,scope->{try(var op=lease.operation(scope)) {
                        return authority.execute(principal,player.row(),op,new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO)).snapshot();
                    }});assertNotNull(gear);
                    arena.active();var initial=arena.capture();
                    assertEquals(NONE,arena.heal(0,1,0).reason());var full=arena.capture();
                    assertEquals(initial.actors().get(0).resources().vitals().mp(),full.actors().get(0).resources().vitals().mp());
                    assertEquals(1,full.actors().get(0).playerHealing().incoming().received());assertEquals(0,full.actors().get(0).playerHealing().incoming().restored());
                    var enemy=arena.players.get(1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,
                            arena.engine.attack(enemy.owner(),enemy.scope(),arena.ticket.matchId(),arena.ticket.generation(),1,1).status());
                    var hurt=arena.capture();assertTrue(hurt.actors().get(0).vital().values().hp()<100);assertTrue(hurt.actors().get(0).vital().values().hp()>0);
                    arena.clock.addAndGet(2*SECOND);assertEquals(NONE,arena.heal(1,1,0).reason());var healed=arena.capture();
                    assertEquals(2,healed.actors().get(0).playerHealing().incoming().received());assertTrue(healed.actors().get(0).playerHealing().incoming().restored()>0);
                    assertEquals(4,healed.actors().get(1).resources().flows().spent());
                    var c=arena.capture();var proof=new ModernBattleCommandLife.Proof(player.scope(),c.identity(),1,c.actors().get(0).resources().life()+1,c.revision());
                    var changed=gate.call(10000,scope->{try(var op=lease.operation(scope)) {
                        return authority.executeInMatch(principal,player.row(),op,new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.SET,false,1003,gear.revision(),gear.viewToken()),
                                arena.engine.beginCanonicalEquipmentChange(player.owner(),proof));
                    }});
                    assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
                    var repriced=changed.observation().actors().get(0);assertEquals(2,repriced.playerHealing().outgoing().spending().current().cost());
                    assertEquals(0,repriced.playerHealing().outgoing().spending().current().spent());assertEquals(full.actors().get(0).runtime(),repriced.runtime());
                    assertEquals(healed.actors().get(0).playerHealing().incoming(),repriced.playerHealing().incoming());
                    assertEquals(NONE,arena.heal(0,2,0).reason());var finalState=arena.capture();
                    assertEquals(2,finalState.actors().get(0).playerHealing().outgoing().spending().current().casts());
                    assertEquals(2,finalState.actors().get(0).resources().flows().spent());assertEquals(3,finalState.actors().get(0).playerHealing().incoming().received());
                    assertEquals(2,finalState.ownerViews(finalState.world()).size());
                    assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(finalState));
                }
            }
            assertEquals(original,ModernSlowMainSqlTest.sourceRows(f));
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                execute(c,"DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`角色_智力成長` WHERE id=127");
            }
            var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(gate,physical,principal,sun,7));
            assertEquals(ModernBackendExecutor.Failure.BACKEND,error.failure());assertEquals("modern backend operation failed: BACKEND",error.getMessage());
            assertNull(error.getCause());assertTrue(ModernPlayerHealing.qualify(first.fighter()).accepts(first.skills()),"previous immutable read remains qualified");
            var old=ModernEquipmentRepository.physicalJdbc(sources,true,false,false,false,false);
            assertNotNull(gate.call(10000,scope->old.read(principal,7,3333)));assertNull(gate.call(10000,scope->old.read(principal,7,3333)).source().healingGrowth());
            var connections=new ModernSettlementConnections(f.config.sources().get(0).database(),1000,3000,1);
            try {try(var c=connections.open(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement("UPDATE `"+ModernQuestSqlFixture.source(7)+"`.characters SET CurMp=0 WHERE objid=3333")) {
                c.setReadOnly(false);s.setQueryTimeout(2);assertEquals(1142,assertThrows(SQLException.class,s::executeUpdate).getErrorCode());
            }}finally{connections.close();assertTrue(connections.awaitTermination(5000));}
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());assertFalse(f.mysql.forcedTermination());f.successful();
        }
        System.err.println("PLAYER_HEAL_SQL: non-Honor source7/source42, shared514, full-HP zero-cost receipt, damage/cross-heal, durable gear reprice, SELECT-only source preservation and fixture cleanup verified; no service/wire/UI route");
    }
    static final class Arena implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernRooms rooms;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();final AtomicLong clock=new AtomicLong(SECOND);
        ModernRooms.FrozenPreparation ticket;ModernBattleEngine engine;final ModernWorldService.Mapping mapping;
        Arena(ModernWorldService.Mapping mapping,List<Read> input)throws Exception {
            this.mapping=mapping;rooms=new ModernRooms(Set.of(mapping.binding().key()));
            try {
                var positions=mapping.binding().map().serverChosenSpawns(mapping.anchor(),input.size());var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
                for(int i=0;i<input.size();i++) {
                    var data=input.get(i);var row=data.skills().character();int id=data.skills().sourceId();var owner=presence.connect();
                    var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(id,row.objectId()),id==7?"太陽神":"月亮神",row.name(),row.level(),row.characterClass(),row.sex());
                    assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                    var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(id,row.objectId(),1,0),row);players.add(p);
                    var summary=i==0?null:room().summary();ModernRoomsTest.accepted(rooms.execute(owner,i==0?
                            new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(2,1,514,0),""):
                            new ModernRooms.Command(p.scope(),ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")));
                    prepared.add(new ModernBattleEngine.PreparedMember(owner,p.scope(),row,positions.get(i),200_000_000,data.fighter(),700_000_000,
                            data.skills(),700_000_000,false,List.of(),ModernInventoryWeight.qualify(data.fighter().profile(),100),false,false,false).withPlayerHealing());
                }
                for(var p:players){var s=room().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}
                var s=room().summary();var result=rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),s.id(),s.revision());ModernRoomsTest.accepted(result.result());ticket=result.ticket();
                engine=new ModernBattleEngine(ticket,mapping.binding(),prepared,new ModernBattleEngine.Limits(2000,0,120000),clock::get,System::currentTimeMillis,b->b-1);
            }catch(Throwable failure){close();throw failure;}
        }
        ModernRooms.RoomSnapshot room(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        void active(){for(var p:players)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),mapping.binding().map().digest());assertEquals(ModernBattleEngine.Phase.ACTIVE,capture().phase());}
        ModernBattleObservation.Capture capture(){var c=engine.serviceObservation();c.ownerViews(c.world());return c;}
        ModernBattleEngine.PlayerHealingResult heal(int who,long seq,int target){return engine.playerHeal(players.get(who).owner(),ModernBattlePlayerHealingTest.request(capture(),who,seq,target));}
        @Override public void close(){if(engine!=null)engine.close();if(ticket!=null)rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.ownerCount());}
    }
    static void install(ModernQuestSqlFixture f)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            execute(c,"CREATE TABLE `"+ModernQuestSqlFixture.CATALOG+"`.`角色_智力成長` (id BIGINT PRIMARY KEY,`治癒骰加成` BIGINT NOT NULL)");
            ModernQuestSqlFixture.insertZeroGrowthRows(c,"角色_智力成長",127);
            execute(c,"INSERT INTO `"+ModernQuestSqlFixture.CATALOG+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx,target_to,damage_value,damage_dice,damage_dice_count,attr,ranged) VALUES(1,'$1436','初級治癒術','buff',1,0,4,10,16,19,11722,3,2,4,0,4,-1)");
            execute(c,"INSERT INTO `"+ModernQuestSqlFixture.CATALOG+"`.`armor`(item_id,name_id,type,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist) VALUES(20014,'$healing-helm','helm',1,1,1,1,1,1,1)");
            execute(c,"UPDATE `"+ModernQuestSqlFixture.source(7)+"`.`character_items` SET item_id=20014 WHERE id=1003");
            for(int id:List.of(7,42)) {
                execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(id)+"`.`character_skills` VALUES(3333,1)");
                execute(c,"UPDATE `"+ModernQuestSqlFixture.source(id)+"`.`characters` SET MaxHp=100,CurHp=100");
            }
        }
    }
    private static void execute(Connection c,String sql)throws Exception {try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}}
}
