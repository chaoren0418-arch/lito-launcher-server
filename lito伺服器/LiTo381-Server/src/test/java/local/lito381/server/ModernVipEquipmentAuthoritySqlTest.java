package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernVipEquipmentAuthority.Status.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Owned MySQL only. Real SELECT-only provider, source-specific authenticated
 * grants and owner binding, worker scope, engine admission, durable store and
 * publication. Artificial open map; no Main/TLS/native UI activation claim. */
class ModernVipEquipmentAuthoritySqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void sourceSelectedReloadRemovalAndReequipPersistWithoutSourceCharacterWrites()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls)) {
            ModernExpiredVipSqlFixture.seed(fixture);
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                s.setQueryTimeout(3);
                for(int id:List.of(7,42)) {
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.characters SET MaxHp=100,CurHp=100 WHERE objid=3333");
                    s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items_time SET usertime='2999-01-01 00:00:00' WHERE itemr_obj_id="+ModernExpiredVipSqlFixture.VIP_OBJECT);
                }
            }
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession();
                    var store=new ModernEquipmentOverrideStore(fixture.directory.resolve("equipment"))) {
                try {
                    var sources=new TreeMap<Integer,ModernEquipmentRepository.Source>();
                    var grants=new TreeMap<Integer,ModernAccountAuthentication.SourceBinding>();
                    for(var config:fixture.config.sources()) {
                        var dao=new ModernJdbcBackend(config.database(),1000,5000);
                        sources.put(config.id(),new ModernEquipmentRepository.Source(config,dao));
                        grants.put(config.id(),ModernAccountAuthentication.SourceBinding.direct("source-"+config.id(),dao::authenticate));
                    }
                    var verifier=ModernAccountAuthentication.repositories(grants,account->false);
                    var principal=gate.call(10000,scope->ModernAccountAuthentication.verify(verifier,ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()));
                    var provider=ModernEquipmentRepository.fullVipJdbc(sources,false,false,false,false,false,false,false);
                    var strict=ModernEquipmentRepository.physicalJdbc(sources);
                    var inputs=new ArrayList<ModernEquipmentAuthority.PhysicalLoaded>();
                    for(int id:List.of(7,42))inputs.add(gate.call(5000,scope->provider.read(principal,id,3333)));
                    try(var arena=new Arena(inputs,gate,principal,provider,store)) {
                        var sun=arena.inspect(0);var moon=arena.inspect(1);
                        assertEquals(22,sun.view().personal().equipment().vipAbilities().get(CHA));
                        assertNotEquals(sun.token(),moon.token());assertTrue(arena.stored(0).isEmpty());assertTrue(arena.stored(1).isEmpty());
                        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                            s.setQueryTimeout(3);
                            s.execute("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`道具_自訂穿脫能力` SET `魅力`=9000,`血量`=4294967299 WHERE `道具編號`=300086");
                            s.execute("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`poly_變身賦予能力` SET addCha=3000 WHERE gfxId=5641");
                        }
                        assertEquals(CONFLICT,arena.set(sun,false).status());assertTrue(arena.stored(0).isEmpty());
                        var fresh=arena.inspect(0);assertEquals(22,fresh.view().personal().equipment().vipAbilities().get(CHA));
                        var removed=arena.set(fresh,false);assertEquals(APPLIED,removed.status());assertEquals(1,removed.publication().equipmentRevision());
                        var reequipped=arena.set(removed.quote(),true);assertEquals(APPLIED,reequipped.status());
                        assertEquals(12000,reequipped.quote().view().personal().equipment().vipAbilities().get(CHA));
                        assertEquals(4_294_967_399L,reequipped.quote().view().personal().equipment().effective().maximumHp());
                        assertEquals(100,reequipped.quote().view().personal().history().resources().vitals().hp());
                        assertEquals(2,arena.stored(0).orElseThrow().revision());assertTrue(arena.stored(1).isEmpty());
                        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.createStatement()) {
                            s.setQueryTimeout(3);
                            s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`道具_自訂穿脫能力` WHERE `道具編號`=300086");
                            s.execute("DELETE FROM `"+ModernQuestSqlFixture.CATALOG+"`.`poly_變身賦予能力` WHERE gfxId=5641");
                        }
                        assertThrows(Exception.class,()->gate.call(5000,scope->strict.read(principal,7,3333)));
                        var absent=arena.inspect(0);assertEquals(12000,absent.view().personal().equipment().vipAbilities().get(CHA));
                        var last=arena.set(absent,false);assertEquals(APPLIED,last.status());assertEquals(3,last.publication().equipmentRevision());
                        assertEquals(DENIED,arena.set(last.quote(),true).status());assertEquals(3,arena.stored(0).orElseThrow().revision());
                        for(int i=0;i<inputs.size();i++) {
                            int id=List.of(7,42).get(i);var source=gate.call(5000,scope->provider.read(principal,id,3333));
                            assertEquals(java.util.Set.of(300086L),source.source().vipCatalog().absentVips());
                            assertEquals(inputs.get(i).source().input().equipment().source(),source.source().input().equipment().source());
                            assertEquals(inputs.get(i).source().input().equipment().lifetimes().items(),source.source().input().equipment().lifetimes().items());
                        }
                        assertTrue(arena.stored(1).isEmpty());assertEquals(ModernBattleEngine.Phase.ACTIVE,arena.engine.serviceObservation().phase());
                    }
                } finally {executor.close();assertTrue(executor.awaitTermination(5000));}
            }
            System.err.println("FULL_VIP_AUTHORITY_SQL sources=2 sameObjid=3333 authenticatedProvider=true durableRevisions=1,2,3 fullI64=true reloadPinnedUntilReequip=true verifiedAbsenceRemoval=true strictReaderUnchanged=true sourceCharactersItemsLifetimesPreserved=true");
            fixture.successful();
        }
    }
    @FunctionalInterface interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    static final class Arena implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final List<ModernEquipmentAuthority.PhysicalLoaded> inputs;final ModernBackendExecutor.SessionGate gate;
        final ModernAccountAuthentication.Principal principal;final ModernEquipmentOverrideStore store;
        final ModernVipEquipmentAuthority authority;final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;
        Arena(List<ModernEquipmentAuthority.PhysicalLoaded> inputs,ModernBackendExecutor.SessionGate gate,ModernAccountAuthentication.Principal principal,
                ModernEquipmentAuthority.PhysicalProvider provider,ModernEquipmentOverrideStore store)throws Exception {
            this.inputs=inputs;this.gate=gate;this.principal=principal;this.store=store;authority=new ModernVipEquipmentAuthority(provider,store,System::currentTimeMillis);
            for(var input:inputs) {
                var snapshot=input.source().input().equipment().source();var row=snapshot.character();var owner=rooms.presence.connect();
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(snapshot.sourceId(),row.objectId()),"source-"+snapshot.sourceId(),row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,rooms.presence.select(owner,selection));
                rooms.rooms.enterLobby(owner,selection,1,0);players.add(new ModernRoomsTest.Player(owner,new ModernRooms.Scope(snapshot.sourceId(),row.objectId(),1,0)));
            }
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(100,1,514,0)));
            ModernRoomsTest.accepted(rooms.join(players.get(1),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();var preparation=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(preparation.result());ticket=preparation.ticket();
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var members=new ArrayList<ModernBattleEngine.PreparedMember>();long now=System.currentTimeMillis();
            for(int i=0;i<inputs.size();i++) {
                var input=inputs.get(i);var source=input.source();var p=players.get(i);var key=new TournamentPresence.CharacterKey(p.scope().sourceId(),p.scope().objectId());
                var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,input.ownerBinding());
                var old=ModernPhysicalCombat.qualify(source,binding,Optional.empty(),now);
                var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),key,1,0),source,binding,Optional.empty(),now,5641);
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row(i),new ModernWorldMap.Cell(32770+i,32770),200_000_000,old,700_000_000)
                        .withVip(ModernPhysicalCombat.qualifyVip(state,now)));
            }
            engine=new ModernBattleEngine(ticket,new ModernWorldBinding(1,514,map),members,new ModernBattleEngine.Limits(10000,0,60000),System::nanoTime,System::currentTimeMillis,bound->0,false);
            for(var p:players)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,engine.serviceObservation().phase());
        }
        ModernApplicationProtocol.CharacterRow row(int index){return inputs.get(index).source().input().equipment().source().character();}
        <T>T call(int index,Work<T> work)throws Exception {
            var p=players.get(index);var lease=new ModernEquipmentLease.Issuer().open(p.owner(),new TournamentPresence.CharacterKey(p.scope().sourceId(),p.scope().objectId()),p.scope().epoch(),()->{});
            try {return gate.call(10000,scope->{try(var operation=lease.operation(scope)){return work.run(operation);}});}finally{lease.invalidate();}
        }
        ModernBattleEngine.EquipmentChange reserve(int i,ModernBattleVipCommandState.Proof proof) {
            var admission=engine.admitVipEquipmentChange(players.get(i).owner(),proof);assertEquals(ModernBattleVipCommandState.Rejection.NONE,admission.rejection());return admission.change();
        }
        ModernVipEquipmentAuthority.Quote inspect(int i)throws Exception {
            var proof=ModernBattleVipCommandState.from(ModernBattleVipObservation.from(engine.serviceObservation()).forOwner(players.get(i).scope()));
            return call(i,op->authority.inspect(principal,row(i),op,proof,reserve(i,proof)));
        }
        ModernVipEquipmentAuthority.Result set(ModernVipEquipmentAuthority.Quote quote,boolean equipped)throws Exception {
            return call(0,op->authority.execute(principal,row(0),op,new ModernVipEquipmentAuthority.Request(quote.proof(),ModernExpiredVipSqlFixture.VIP_OBJECT,equipped,quote.token()),reserve(0,quote.proof())));
        }
        Optional<ModernEquipmentOverrideStore.Stored> stored(int i)throws Exception{return call(i,op->store.read(op.key(),op.scope()));}
        @Override public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
    }
}
