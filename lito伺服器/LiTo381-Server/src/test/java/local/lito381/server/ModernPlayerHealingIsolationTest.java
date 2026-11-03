package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernPlayerHealingIsolationTest {
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source s) {
        var magic=ModernBattleEngineNpcMagicTest.magic(s);
        return new ModernPhysicalCombat.Source(magic.input(),magic.growth(),magic.templates(),magic.procCatalogIds(),magic.magicGrowth(),
                magic.magicBows(),magic.weaponArrowSprites(),magic.regenGrowth(),
                ModernBattleEngineHonorTest.healing(magic.growth().sourceId(),magic.growth().database(),0));
    }
    private static List<ModernBattleEngine.PreparedMember> prepared(ModernBattleEngineNpcTest.Fixture f) {
        return f.prepared.stream().map(p->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),
                p.fighter(),p.attackIntervalNanos(),new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),
                    List.of(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL),ModernSkillsTest.teleport())),
                1200*ModernBattleEngineNpcTest.MS,false,p.matchItems(),p.weightBasis(),false,false,true).withPlayerHealing()).toList();
    }
    @Test void npcClockAdvancingDuringHealingCannotInvalidateUnchangedSelfOrBot()throws Exception {
        for(int target:List.of(0,2))try(var f=new ModernBattleEngineNpcTest.Fixture(2,1,true,100,0,ModernPlayerHealingIsolationTest::source,ModernNpcMeleeTest.catalog(),false,
                ModernWorldMap.fromSectorBytes(554,java.util.Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false),
                List.of(ModernBattleEngineNpcTest.cell(10,10),ModernBattleEngineNpcTest.cell(20,10),ModernBattleEngineNpcTest.cell(12,10)))) {
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan,true),true,prepared(f));
            for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
            var before=f.engine.serviceObservation();long draws=f.draws.get();
            f.duringRandom=()->f.clock.addAndGet(ModernBattleEngineNpcTest.MS);
            var result=f.engine.playerHeal(f.players.get(0).owner(),request(before,0,1,target));
            assertEquals(NONE,result.reason());verify(result.observation());var after=result.observation();
            assertTrue(after.revision()>before.revision()+1,"NPC clock and healing are separate revisions");
            assertTrue(f.draws.get()>draws);assertEquals(4,after.actors().get(0).resources().flows().spent());
            assertEquals(1,after.actors().get(0).playerHealing().outgoing().spending().current().casts());
            assertEquals(1,after.actors().get(target).playerHealing().incoming().received());
            assertEquals(after.revision(),after.actors().get(target).playerHealing().incoming().last().use().revision());
            assertEquals(before.actors().get(1).resources(),after.actors().get(1).resources());
        }
    }
    @Test void nearbyPlayersInDifferentQuestLanesCannotExchangeHealingOrMana()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,ModernPlayerHealingIsolationTest::source,ModernNpcMeleeTest.catalog(),false)) {
            var prepared=prepared(f);
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            f.engine=f.build(new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan,true),true,prepared);
            for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
            var before=f.engine.serviceObservation();verify(before);assertEquals(ModernBattleEngine.Phase.ACTIVE,before.phase());
            assertNotEquals(before.actors().get(0).pose().member().team(),before.actors().get(1).pose().member().team());
            var denied=f.engine.playerHeal(f.players.get(0).owner(),request(before,0,1,1));assertEquals(WRONG_INSTANCE,denied.reason());verify(denied.observation());
            assertEquals(0,f.draws.get());for(int i=0;i<2;i++) {
                assertEquals(before.actors().get(i).resources(),denied.observation().actors().get(i).resources());
                assertEquals(before.actors().get(i).playerHealing(),denied.observation().actors().get(i).playerHealing());
            }
            var self=f.engine.playerHeal(f.players.get(0).owner(),request(denied.observation(),0,2,0));assertEquals(NONE,self.reason());verify(self.observation());
            assertEquals(1,self.observation().actors().get(0).playerHealing().incoming().received());
            assertEquals(0,self.observation().actors().get(1).playerHealing().incoming().received());
            var publication=ModernBattleHealingWireTest.publication(self.observation(),f.config.catalog());
            for(var player:f.players) {
                var view=publication.audience(player.scope());var sections=ModernBattleHealingWireTest.sections(ModernBattleHealingWireTest.check(view));
                assertEquals(1,view.world().actors().size());assertEquals(1,java.nio.ByteBuffer.wrap(sections.get(15)).getInt());
                assertEquals(player.scope().sourceId(),view.world().actors().get(0).pose().member().identity().sourceId());
            }
        }
    }
}
