package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Actual shared engine ownership: teleport is not a native cursor/portal wait. */
class ModernNpcTeleportTest {
    static ModernBattleEngineNpcTest.Fixture fixture(boolean learned)throws Exception {
        return fixture(learned,true);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean learned,boolean quest)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false);
        try {
            var prepared=f.prepared.stream().map(p->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),learned?List.of(new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT)):List.of()),
                    1200*ModernBattleEngineNpcTest.MS,false,p.matchItems(),p.weightBasis(),false,false,true)).toList();
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            f.engine=f.build(quest?new ModernNpcBattle.Config(f.config.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan):f.config,quest,prepared);
            return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernBattleEngine.CastResult cast(ModernBattleEngineNpcTest.Fixture f,int player,long sequence) {
        var p=f.players.get(player);return f.engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,5));
    }
    static ModernMatchDescriptor descriptor() {
        return new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.NPC_ENCOUNTER,
                ModernMatchDescriptor.NPC|ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT|
                ModernMatchDescriptor.REGEN|ModernMatchDescriptor.ISOLATED_FORCES|ModernMatchDescriptor.QUEST_OBJECTIVES|ModernMatchDescriptor.QUEST_LIVES,2000,2000,0,60000);
    }
    @Test void castsProjectSparseIdentityAndRequireV23IncludingPreparingAndNpcReplies()throws Exception {
        try(var f=fixture(true)) {
            var before=ModernNpcProtocol.Frame.from(f.view(),descriptor()).withResources(ModernNpcProtocol.Resources.from(f.config.catalog()));
            for(int i=0;i<2;i++) {
                var p=f.players.get(i);var projected=ModernQuestVisibility.npc(before,p.scope());
                assertEquals(1,projected.combat().world().casts().size());assertEquals(i+1,projected.combat().world().casts().get(0).entityId());
                var w=projected.combat().world();new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision(),ModernWorldProtocol.Phase.PREPARING,
                        w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest());
                var request=new ModernNpcProtocol.Request(p.scope(),ModernNpcProtocol.Action.READ,projected.scope(),0,0,0);
                var message=new ModernApplicationProtocol.NpcResult(1,request,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,projected));
                var bytes=ModernApplicationProtocol.encode(message,23);assertEquals(message,ModernApplicationProtocol.decode(bytes,23));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,22));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,22));
            }
            assertFalse(descriptor().supportedBy(22,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertTrue(descriptor().supportedBy(23,ModernSessionProtocol.KNOWN_CAPABILITIES));
        }
    }
    @Test void teleportDeductsMpOnceKeepsNpcReceiptsAndFutureBirthsClearAndAllowsMovement()throws Exception {
        try(var f=fixture(true)) {
            f.active();f.elapsed(100);var npc=f.view().npcs().actors().stream().filter(n->n.lane()==1).findFirst().orElseThrow();
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.step(npc).reason());
            var before=f.view();assertFalse(before.exchanges().isEmpty());var p=f.players.get(1);var result=cast(f,1,1);assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());
            var after=f.view();var actor=after.world().actors().get(1);var receipt=after.world().casts().get(1);
            assertNotEquals(before.world().actors().get(1).position(),actor.position());assertTrue(f.map.standing(actor.position()));
            assertEquals(20,after.world().vitals().get(1).mp());assertEquals(30,after.world().vitals().get(0).mp());
            assertEquals(1,receipt.castCount());assertEquals(10,receipt.mpSpent());assertEquals(before.exchanges(),after.exchanges());
            var plan=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            assertTrue(plan.waves().stream().filter(w->w.lane()==1).flatMap(w->w.spawns().stream()).noneMatch(s->s.cell().equals(actor.position())));
            assertTrue(ModernQuestObjectivesTest.objectives(554,1,false).targets().stream().noneMatch(t->t.lane()==1&&t.cell().equals(actor.position())));
            var visible=ModernQuestVisibility.combat(ModernNpcProtocol.Frame.from(after,descriptor()).combat(),p.scope());
            var request=new ModernSpellProtocol.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),1,5);
            var message=new ModernApplicationProtocol.CastResult(3,request,new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.OK,ModernSpellProtocol.Reason.NONE,visible));
            assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,23),23));
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,cast(f,1,2).reason());assertEquals(20,f.view().world().vitals().get(1).mp());
            assertThrows(SecurityException.class,()->cast(f,1,2));
            f.elapsed(1300);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,
                    actor.position().x(),actor.position().y(),2).status());
        }
    }
    @Test void oppositeLaneDoesNotBlockDestinationAndLiveSameLaneNpcDoes()throws Exception {
        try(var f=fixture(true)) {
            f.active();var target=f.view().world().actors().get(1).position();
            var blocked=new HashSet<ModernWorldMap.Cell>();blocked.add(f.view().world().actors().get(0).position());
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            base.waves().stream().filter(w->w.lane()==0).flatMap(w->w.spawns().stream()).forEach(s->blocked.add(s.cell()));
            ModernQuestObjectivesTest.objectives(554,1,false).targets().stream().filter(t->t.lane()==0).forEach(t->blocked.add(t.cell()));
            f.randomValue=f.map.teleportCells().stream().filter(c->!blocked.contains(c)).toList().indexOf(target);
            assertTrue(f.randomValue>=0);assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1).reason());assertEquals(target,f.view().world().actors().get(0).position());
        }
        try(var f=fixture(true,false)) {
            f.active();var first=f.map.teleportCells().get(0);f.spawn(0,first);f.randomValue=0;
            assertEquals(ModernSpellProtocol.Reason.NONE,cast(f,0,1).reason());assertNotEquals(first,f.view().world().actors().get(0).position());
        }
    }
    @Test void unlearnedAndCancellationDoNotSpendMpOrTeleport()throws Exception {
        try(var f=fixture(false)) {var before=f.active();assertEquals(ModernSpellProtocol.Reason.UNLEARNED,cast(f,0,1).reason());
            assertEquals(before.world().actors(),f.view().world().actors());assertEquals(30,f.view().world().vitals().get(0).mp());}
        try(var f=fixture(true)) {var before=f.active();f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,cast(f,0,1).reason());assertEquals(before.world().actors(),f.view().world().actors());assertEquals(30,f.view().world().vitals().get(0).mp());}
    }
}
