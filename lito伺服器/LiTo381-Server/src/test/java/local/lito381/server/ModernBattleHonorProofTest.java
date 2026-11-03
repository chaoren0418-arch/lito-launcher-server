package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBattleHonorProofTest {
    static ModernBattleObservation.Capture objectives(ModernBattleObservation.Capture c,List<ModernHonor.Objective> objectives) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),new ModernBattleObservation.Honor(objectives,m.honor().lives()),
                m.quest(),m.timeAttack(),m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernBattleObservation.Capture result(ModernBattleObservation.Capture c,ModernMatchSettlement.Event event) {
        return new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),c.loadingOriginNanos(),c.actors(),c.modes(),c.npcs(),event);
    }
    static ModernHonor.Objective owner(ModernHonor.Objective o,long actor) {
        return new ModernHonor.Objective(o.entityId(),o.team(),o.action(),o.graphic(),o.x(),o.y(),o.hp(),o.maximumHp(),o.revision(),o.hurtRevision(),o.deathRevision(),o.damageLoss(),o.healGain(),actor,o.lastKind());
    }
    @Test void terminalVerdictsMustBelongToTheTeamThatActuallyDestroyedTheTower()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1,100,100,1_000_000_000L)) {
            f.active();var c=f.cast(0,1,4,4).observation();assertEquals(ModernBattleEngine.Phase.FINISHED,c.world().phase());var e=c.result();
            var swapped=e.participants().stream().map(p->new ModernMatchSettlement.Participant(p.entityId(),p.identity(),p.team(),
                    p.verdict()==ModernMatchSettlement.Verdict.WIN?ModernMatchSettlement.Verdict.LOSS:ModernMatchSettlement.Verdict.WIN)).toList();
            var forged=new ModernMatchSettlement.Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),swapped);
            assertThrows(IllegalArgumentException.class,()->result(c,forged).world());
        }
    }
    @Test void finishedCannotRetainAResultWhileBothObjectivesAreAlive()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1,100,100,1_000_000_000L)) {
            f.active();var c=f.cast(0,1,4,4).observation();
            var initial=f.honor.towers().stream().map(t->ModernHonor.Objective.initial(2,t)).toList();
            assertThrows(IllegalArgumentException.class,()->objectives(c,initial).world());
        }
    }
    @Test void healingCannotBeAttributedToAnEnemyWithNoMatchingCast()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var c=f.cast(0,1,1,3).observation();var rows=c.modes().honor().objectives();
            assertThrows(IllegalArgumentException.class,()->objectives(c,ModernBattleLifeProofTest.replace(rows,0,owner(rows.get(0),2))).world());
        }
    }
    static ModernHonor.Objective geometry(ModernHonor.Objective o,long entity,ModernWorldMap.Cell cell) {
        return new ModernHonor.Objective(entity,o.team(),o.action(),o.graphic(),cell.x(),cell.y(),o.hp(),o.maximumHp(),o.revision(),o.hurtRevision(),o.deathRevision(),o.damageLoss(),o.healGain(),o.lastActorEntityId(),o.lastKind());
    }
    static ModernBattleObservation.World receipt(ModernBattleObservation.World w,int index,ModernUseHistory.Lane lane,
            java.util.function.UnaryOperator<ModernBattleObservation.SpellReceipt> change) {
        var a=w.actors().get(index);var s=a.spells();var receipts=new java.util.EnumMap<ModernUseHistory.Lane,ModernBattleObservation.SpellReceipt>(ModernUseHistory.Lane.class);
        receipts.putAll(s.receipts());receipts.put(lane,change.apply(receipts.get(lane)));
        var spells=new ModernBattleObservation.Spells(s.sequence(),s.requestRevision(),s.initialLawful(),s.lawful(),s.histories(),receipts,s.structure());
        var next=new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),spells,a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
        return ModernBattleLifeProofTest.actors(w,ModernBattleLifeProofTest.replace(w.actors(),index,next));
    }
    static ModernBattleObservation.SpellReceipt targets(ModernBattleObservation.SpellReceipt r,List<ModernTargetHistory.Effect> effects) {
        return new ModernBattleObservation.SpellReceipt(new ModernBattleObservation.Impact(r.impact().lane(),r.impact().use(),effects),r.casts(),r.spent(),r.loadingElapsedNanos(),r.detail());
    }
    @Test void towerIdentityAndGeometryCannotCollideWithPlayersOrAnotherObjective()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var c=f.engine.serviceObservation();var rows=c.modes().honor().objectives();var o=rows.get(0);
            for(var cell:List.of(c.actors().get(0).pose().position(),rows.get(1).cell(),new ModernWorldMap.Cell(0,0)))
                assertThrows(IllegalArgumentException.class,()->objectives(c,ModernBattleLifeProofTest.replace(rows,0,geometry(o,o.entityId(),cell))).world());
            assertThrows(IllegalArgumentException.class,()->objectives(c,ModernBattleLifeProofTest.replace(rows,0,geometry(o,102,o.cell()))).world());
            assertEquals(c.world(),f.engine.serviceObservation().world());
        }
    }
    @Test void actualMeleeHealArrowAndFireShareTowerAccountingWithoutReinterpretingOldReceipts()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,500,1_000_000_000L,1,true,true)) {
            f.active();long tower=f.engine.serviceObservation().modes().honor().objectives().get(1).entityId();
            var melee=f.attack(0,1,tower).observation();assertEquals(1,melee.world().modes().honor().objectives().get(1).lastKind());
            f.elapsed(2_000_000_000L);var heal=f.cast(1,1,1,tower).observation();assertEquals(3,heal.world().modes().honor().objectives().get(1).lastKind());
            f.elapsed(4_000_000_000L);var arrow=f.cast(0,1,4,tower).observation();assertEquals(2,arrow.world().modes().honor().objectives().get(1).lastKind());
            f.elapsed(6_000_000_000L);var fire=f.cast(0,2,70,0).observation();assertEquals(4,fire.world().modes().honor().objectives().get(1).lastKind());
            assertTrue(fire.world().modes().honor().objectives().get(1).damageLoss()>melee.world().modes().honor().objectives().get(1).damageLoss());
            assertEquals(3,fire.world().actors().size());assertEquals(1,melee.world().modes().honor().objectives().get(1).lastKind());
            for(var p:f.players)assertNotNull(fire.forOwner(p.scope()).owner().history());
        }
    }
    @Test void physicalLastTargetCannotBeReboundAndLaterDeniedRequestKeepsTheRealHit()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,1_000_000_000L)) {
            f.active();var c=f.attack(0,1,4).observation();var w=c.world();var a=w.actors().get(0);var v=a.vital();
            var changed=new ModernBattleObservation.Vital(v.values(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),v.hurtRevision(),v.deathRevision(),3,v.attackIntervalNanos());
            var actor=new ModernBattleObservation.Actor(a.pose(),changed,a.resources(),a.spells(),a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
            assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProofTest.actors(w,ModernBattleLifeProofTest.replace(w.actors(),0,actor)));
            var denied=f.attack(0,2,3);assertEquals(ModernBattleEngine.AttackStatus.SAME_TEAM,denied.status());
            assertEquals(w.modes().honor().objectives(),denied.observation().world().modes().honor().objectives());
            assertEquals(v.attackRevision(),denied.observation().world().actors().get(0).vital().attackRevision());
        }
    }
    @Test void canonicalHealingTargetsMustBindExactTowerPositionAndIdentity()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var w=f.cast(0,1,1,3).observation().world();var lane=ModernUseHistory.Lane.HEALING;
            var original=w.actors().get(0).spells().receipts().get(lane);var e=original.impact().targets().get(0);var h=e.before();
            var wrongPlace=new ModernTargetHistory.Health(h.target(),h.life(),h.equipmentRevision(),new ModernWorldMap.Cell(h.cell().x()+1,h.cell().y()),h.hp(),h.maximumHp());
            var wrongId=new ModernTargetHistory.Health(new ModernTargetHistory.Honor(102,0),h.life(),h.equipmentRevision(),h.cell(),h.hp(),h.maximumHp());
            for(var invalid:List.of(wrongPlace,wrongId)) {
                var effect=new ModernTargetHistory.Effect(e.kind(),invalid,e.raw(),e.applied(),e.hpAfter());
                assertThrows(IllegalArgumentException.class,()->receipt(w,0,lane,r->targets(r,List.of(effect))));
            }
            assertThrows(IllegalArgumentException.class,()->receipt(w,0,lane,r->targets(r,List.of(e,e))));
        }
    }
    @Test void fireStormDamageTotalMustBelongToTheMatchingTowerImpact()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,500,1_000_000_000L,0,true,true)) {
            f.active();var w=f.cast(0,1,70,0).observation().world();var lane=ModernUseHistory.Lane.FIRE_STORM;
            var r=w.actors().get(0).spells().receipts().get(lane);var totals=new java.util.ArrayList<>(((ModernSpellRuntime.FireStorm)r.detail()).damageLossAfter());
            int target=-1;for(int i=0;i<r.impact().targets().size();i++)if(r.impact().targets().get(i).before().target() instanceof ModernTargetHistory.Honor)target=i;
            assertTrue(target>=0);totals.set(target,totals.get(target)+1);
            assertThrows(IllegalArgumentException.class,()->receipt(w,0,lane,x->new ModernBattleObservation.SpellReceipt(x.impact(),x.casts(),x.spent(),x.loadingElapsedNanos(),new ModernSpellRuntime.FireStorm(totals))));
        }
    }
    @Test void fatalAreaCastPublishesPlayerDeathTowerDestructionAndAllTypedVerdictsTogether()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1,1,500,1_000_000_000L,1,true,true)) {
            f.active();var c=f.cast(0,1,70,0).observation();var w=c.world();assertEquals(ModernBattleEngine.Phase.FINISHED,w.phase());
            assertEquals(0,w.actors().get(1).vital().values().hp());assertEquals(0,w.modes().honor().lives().get(1).pendingDueMillis());
            assertEquals(0,w.modes().honor().objectives().get(1).hp());assertEquals(w.revision(),w.modes().honor().objectives().get(1).deathRevision());
            assertEquals(3,w.result().participants().size());assertEquals(1,w.result().participants().stream().filter(p->!p.identity().isHuman()).count());
            var d=ModernBattlePublicationTest.honor(c);d=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.FIRE_STORM,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertEquals(w.result(),new ModernBattlePublication(c,d,null,null).audience(f.players.get(0).scope()).world().result());
        }
    }
    @Test void laterEmptyAreaCastDoesNotRequireAnOldTowerToAppearInTheNewestTargets()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,500,1_000_000_000L,0,true,true)) {
            f.active();var first=f.cast(0,1,70,0).observation().world();f.elapsed(3_000_000_000L);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,4).status());
            for(int n=1;n<=10;n++){f.elapsed((3L+n)*1_000_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,n+1,2).status());}
            f.elapsed(15_000_000_000L);var empty=f.cast(0,2,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,empty.reason());
            var last=empty.observation().world();assertTrue(last.actors().get(0).spells().receipts().get(ModernUseHistory.Lane.FIRE_STORM).impact().targets().isEmpty());
            assertEquals(first.modes().honor().objectives(),last.modes().honor().objectives());
        }
    }
}
