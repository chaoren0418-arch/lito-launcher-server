package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Real owned SQL -> qualified source/catalog -> frozen room -> same engine,
 * real shared map554 -> typed NPC death + MP/lawful. No wire/UI claim. */
class ModernNpcDisintegrateEngineRealMysqlTest {
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void readOnlySqlSourcesDriveAtomicEngineCastsWithoutPersistingImportedVitals()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var f=ModernQuestSqlFixture.open(Path.of(mysql),Path.of(shared),tls,false,false,true);
            var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                ModernNpcDisintegrateRealMysqlTest.install(c,300);
                ModernNpcDisintegrateRealMysqlTest.execute(c,"INSERT INTO `"+ModernQuestSqlFixture.source(42)+"`.`character_skills` VALUES(3333,77)");
            }
            var reads=List.of(ModernNpcDisintegrateRealMysqlTest.read(gate,f.config.sources().get(0)),ModernNpcDisintegrateRealMysqlTest.read(gate,f.config.sources().get(1)));
            exercise(reads);
            for(var config:f.config.sources()) {
                var after=ModernNpcDisintegrateRealMysqlTest.read(gate,config);
                assertEquals(300,after.caster().profile().effective().initialMp());assertEquals(500,after.caster().profile().source().persisted().lawful());
                assertEquals(300,after.catalog().npcs().get(45262).get(ModernNpcCatalog.Field.HP));
            }
            assertArrayEquals(new long[]{0,0,0},f.mysql.counts());assertEquals(0,f.mysql.botCount());
            executor.close();assertTrue(executor.awaitTermination(5000));assertEquals(0,f.mysql.activeConnections());f.successful();
        }
    }
    static void exercise(List<ModernNpcDisintegrateRealMysqlTest.Read> inputs)throws Exception {
        var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,32);
        var positions=List.of(cells.get(0),cells.get(cells.size()-1));var used=new HashSet<>(positions);var targets=new ArrayList<ModernWorldMap.Cell>();
        for(var p:positions) {
            var target=cells.stream().filter(c->!used.contains(c)&&Math.abs((long)p.x()-c.x())+Math.abs((long)p.y()-c.y())==1&&map.canStep(p,c)).findFirst().orElseThrow();
            targets.add(target);used.add(target);
        }
        var binding=new ModernWorldBinding(2,554,map);var presence=new TournamentPresence();var rooms=new ModernRooms(Set.of(binding.key()));
        var owners=new ArrayList<TournamentPresence.Owner>();var scopes=new ArrayList<ModernRooms.Scope>();ModernRooms.FrozenPreparation ticket=null;
        try {
            ModernRooms.RoomSnapshot room=null;
            for(int i=0;i<inputs.size();i++) {
                var input=inputs.get(i);var row=input.caster().profile().source().character();var owner=presence.connect();owners.add(owner);
                var selection=new TournamentPresence.Selection(input.caster().key(),i==0?"太陽神":"月亮",row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var scope=new ModernRooms.Scope(input.caster().key().sourceId(),row.objectId(),1,0);scopes.add(scope);
                var command=i==0?new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(4,2,554,0),""):
                        new ModernRooms.Command(scope,ModernRooms.Action.JOIN,room.summary().id(),room.summary().revision(),0,null,"");
                var reply=rooms.execute(owner,command);ModernRoomsTest.accepted(reply);room=reply.view().joined();
            }
            for(int i=0;i<owners.size();i++) {
                var reply=rooms.execute(owners.get(i),new ModernRooms.Command(scopes.get(i),ModernRooms.Action.READY,room.summary().id(),room.summary().revision(),1,null,""));
                ModernRoomsTest.accepted(reply);room=reply.view().joined();
            }
            var captured=rooms.prepareWorld(owners.get(0),scopes.get(0),room.summary().id(),room.summary().revision());ModernRoomsTest.accepted(captured.result());ticket=captured.ticket();
            var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();var blessings=new HashMap<ModernParticipantId,ModernNpcMelee.BlessingBinding>();
            for(int i=0;i<inputs.size();i++) {
                var input=inputs.get(i);var fighter=input.caster();var row=fighter.profile().source().character();
                var skills=new ModernSkills.Snapshot(fighter.key().sourceId(),row,ModernDisintegrateSkills.qualify(input.skills(),input.skill()));
                assertTrue(skills.skills().stream().anyMatch(s->s.skillId()==77&&s.support()==ModernSkills.Support.DISINTEGRATE));
                prepared.add(new ModernBattleEngine.PreparedMember(owners.get(i),scopes.get(i),row,positions.get(i),200_000_000,fighter,700_000_000,
                        skills,1_200_000_000,false,List.of(),ModernInventoryWeight.qualify(fighter.profile(),1),false,false,true).withNpcDisintegrate(input.skill()));
                blessings.put(ModernParticipantId.human(fighter.key().sourceId(),row.objectId()),new ModernNpcMelee.BlessingBinding(fighter.key().sourceId(),fighter.catalogDatabase(),ModernNpcMelee.BlessingConvention.LEGACY_ZERO_BLESSED));
            }
            var config=new ModernNpcBattle.Config(binding.key(),inputs.get(0).catalog(),blessings,positions,8,1000,null,true).withDisintegrate(true);
            try(var engine=new ModernBattleEngine(ticket,binding,prepared,List.of(),new ModernBattleEngine.Limits(2000,0,60000),
                    ()->1_000_000_000L,System::currentTimeMillis,b->0,false,null,()->false,null,config)) {
                for(int i=0;i<owners.size();i++)engine.ackLoaded(owners.get(i),scopes.get(i),ticket.matchId(),ticket.generation(),map.digest());
                var scope=new ModernNpcRoster.Scope(room.summary().id(),ticket.matchId(),ticket.generation());var template=inputs.get(0).catalog().npcs().get(45262).rosterTemplate();
                var spawned=engine.spawnNpcs(scope,List.of(new ModernNpcRoster.Spawn(template,0,1,targets.get(0),2),new ModernNpcRoster.Spawn(template,1,1,targets.get(1),2)));
                for(int i=0;i<owners.size();i++) {
                    var npc=spawned.npcs().actors().get(i);var owner=owners.get(i);var request=new ModernNpcDisintegrateState.Request(scopes.get(i),npc.id(),1,1,1);
                    var reply=engine.disintegrateNpc(owner,request);assertEquals(ModernNpcDisintegrateState.Reason.NONE,reply.reason());
                    var w=reply.frame().world();var state=w.npcDisintegrations().get(i);
                    assertEquals(i==0?68:70,state.mpCost());assertEquals(300-state.mpCost(),w.vitals().get(i).mp());
                    assertEquals(501,state.lawful());assertEquals(i==0?328:324,state.hit().outcome().rawDamage());
                    assertEquals(300,state.hit().outcome().appliedDamage());assertEquals(11748,state.hit().outcome().effect());
                    assertEquals(0,reply.frame().npcs().actors().get(i).hp());assertEquals(ModernNpcRoster.Action.DEATH,reply.frame().npcs().actors().get(i).action());
                    assertEquals(w.revision(),state.revision());assertEquals(0,w.vitals().get(i).lastTargetEntityId());
                    assertEquals(7,npc.template().catalogSourceId());assertThrows(SecurityException.class,()->engine.disintegrateNpc(owner,request));
                }
                assertEquals(2,engine.ownerCount());assertEquals(ModernBattleEngine.Phase.ACTIVE,engine.tick().phase());
            }
        }finally {
            if(ticket!=null)rooms.releasePreparation(ticket);for(var owner:owners){rooms.disconnect(owner);presence.disconnect(owner);}
            assertEquals(0,rooms.memberCount());assertEquals(0,rooms.ownerCount());
        }
    }
}
