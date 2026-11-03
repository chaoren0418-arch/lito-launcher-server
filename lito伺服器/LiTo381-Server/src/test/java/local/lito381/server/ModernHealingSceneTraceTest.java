package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual engine + durable gear; synthetic map fixture, not resource/UI admission. */
class ModernHealingSceneTraceTest {
    @TempDir Path root;
    @FunctionalInterface interface Sink {
        void accept(String label,ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog)throws Exception;
    }
    static void trace(Path root,Sink sink)throws Exception {
        var factory=new ModernPlayerHealingEquipmentTest();factory.root=Files.createDirectory(root.resolve("gear"));
        try(var f=factory.fixture()) {
            var gear=f.read();sink.accept("gear-active",active(f.world),null);
            var first=heal(f.world,0,1,0);assertEquals(NONE,first.reason());sink.accept("gear-self",first.observation(),null);
            var changed=f.set(gear,false);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
            sink.accept("gear-unequip",changed.observation(),null);
            f.world.elapsed(2*SECOND);var cross=heal(f.world,0,2,1);assertEquals(NONE,cross.reason());sink.accept("gear-cross",cross.observation(),null);
            f.killAndRevive();var revived=f.capture();assertTrue(revived.actors().get(0).resources().life()>0);
            sink.accept("gear-respawn",revived,null);
            var next=heal(f.world,0,3,0);assertEquals(NONE,next.reason());sink.accept("gear-heal-revived",next.observation(),null);
        }
        factory.root=Files.createDirectory(root.resolve("regen"));
        try(var f=factory.fixture()) {
            sink.accept("regen-active",active(f.world),null);
            var hurt=f.world.cast(1,1,4,1);assertEquals(ModernSpellProtocol.Reason.NONE,hurt.reason());sink.accept("regen-hurt",hurt.observation(),null);
            var healed=heal(f.world,0,1,0);assertEquals(NONE,healed.reason());assertTrue(healed.observation().actors().get(0).playerHealing().incoming().restored()>0);
            sink.accept("regen-heal",healed.observation(),null);
            var potion=f.use(1);assertEquals(ModernItemProtocol.Reason.NONE,potion.reason());assertTrue(potion.observation().actors().get(0).regeneration().itemGain()>0);
            sink.accept("regen-potion",potion.observation(),null);
            f.world.elapsed(60*SECOND);var regenerated=f.capture();assertTrue(regenerated.actors().get(0).resources().flows().mpGain()>0);
            sink.accept("regen-tick",regenerated,null);
        }
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,ModernPlayerHealingIsolationTest::source,ModernNpcMeleeTest.catalog(),false)) {
            var prepared=f.prepared.stream().map(p->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),
                    p.fighter(),p.attackIntervalNanos(),new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),
                        List.of(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL),ModernSkillsTest.teleport())),
                    1200*ModernBattleEngineNpcTest.MS,false,p.matchItems(),p.weightBasis(),false,false,true).withPlayerHealing()).toList();
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan,true),true,prepared);
            sink.accept("npc-loading",f.engine.serviceObservation(),f.config.catalog());
            for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
            var active=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.ACTIVE,active.phase());sink.accept("npc-active",active,f.config.catalog());
            var denied=f.engine.playerHeal(f.players.get(0).owner(),request(active,0,1,1));assertEquals(WRONG_INSTANCE,denied.reason());
            sink.accept("npc-denied",denied.observation(),f.config.catalog());
            var self=f.engine.playerHeal(f.players.get(0).owner(),request(denied.observation(),0,2,0));assertEquals(NONE,self.reason());
            sink.accept("npc-self",self.observation(),f.config.catalog());
            f.elapsed(2000);var moved=f.move(0,1,10,10,6);assertEquals(ModernBattleEngine.MoveStatus.MOVED,moved.status());
            sink.accept("npc-player-move",f.engine.serviceObservation(),f.config.catalog());
            var npc=f.engine.serviceObservation().npcs().roster().actors().stream().filter(n->n.lane()==0).findFirst().orElseThrow();
            f.elapsed(2200);f.step(npc);var motion=f.engine.serviceObservation();
            assertTrue(motion.npcs().roster().actors().stream().anyMatch(n->n.walk()!=null));sink.accept("npc-motion",motion,f.config.catalog());
            f.rooms.releasePreparation(f.ticket);sink.accept("npc-abort",f.engine.serviceObservation(),f.config.catalog());
        }
    }
    @Test void fullSceneTraceRetainsGearLivesRegenNpcMovementAndLanePrivacy()throws Exception {
        var count=new int[1];
        trace(root,(label,c,catalog)->{
            verify(c);var p=ModernBattleHealingWireTest.publication(c,catalog);
            for(var scope:c.ownerViews(c.world()).keySet()) {
                var v=p.audience(scope);ModernBattleHealingWireTest.check(v);
                if(label.startsWith("npc-")){assertEquals(1,v.world().actors().size());assertEquals(scope.sourceId(),v.world().actors().get(0).pose().member().identity().sourceId());}
                count[0]++;
            }
        });
        assertEquals(36,count[0]);
    }
}
