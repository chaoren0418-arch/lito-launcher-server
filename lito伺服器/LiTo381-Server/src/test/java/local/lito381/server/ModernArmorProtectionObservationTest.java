package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real engine/canonical publication and isolated durable store. No resource,
 * existing wire, player executable or visible UI acceptance is inferred. */
class ModernArmorProtectionObservationTest {
    @TempDir Path root;
    static ModernMatchDescriptor descriptor(ModernBattleObservation.Capture c) {
        var f=c.features();var m=c.modes();var l=c.limits();
        int flags=COMBAT|ARMOR_PROTECTION|(f.spells()?SPELLS:0)|(f.arrow()?LIGHT_ARROW:0)|(f.items()?ITEMS:0)
                |(f.weight()?WEIGHT:0)|(f.timed()?TIMED:0)|(f.regen()?REGEN:0)|(f.fire()?FIRE_STORM:0)
                |(f.disintegrate()?DISINTEGRATE:0)|(f.playerHealing()?PLAYER_HEALING:0)
                |(m.honor().enabled()?HONOR:0)|(c.npcs()!=null?NPC:0)|(m.isolatedForces()?ISOLATED_FORCES:0)
                |(m.quest().progress()!=null?QUEST_OBJECTIVES|QUEST_LIVES:0);
        return new ModernMatchDescriptor(m.honor().enabled()?Ruleset.HONOR_TOWERS:c.npcs()!=null?Ruleset.NPC_ENCOUNTER:Ruleset.PVP_SKIRMISH,
                flags,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
    }
    static ModernBattlePublication publish(ModernBattleObservation.Capture c) {
        var names=c.npcs()==null?null:new ModernNpcNames.Table(c.npcs().roster().actors().stream().map(a->
                new ModernNpcNames.Name(a.template().catalogSourceId(),a.template().npcId(),a.template().graphic(),"fixture","$1")).distinct().toList());
        var resources=names==null?null:new ModernNpcProtocol.Resources(names.rows().stream().map(ModernNpcNames.Name::graphic).distinct().sorted().toList());
        return ModernBattlePublication.armorProtection(c,descriptor(c),resources,names);
    }
    static ModernBattleObservation.CapturedActor raw(ModernBattleObservation.CapturedActor a,ModernBattleEngine.ArmorProtectionView v) {
        return new ModernBattleObservation.CapturedActor(a.pose(),a.scope(),a.loadout(),a.skills(),a.vital(),a.resources(),a.castIntervalNanos(),
                a.histories(),a.targets(),a.runtime(),a.supplies(),a.weight(),a.buffs(),a.ammo(),a.regeneration(),a.regenClock(),a.presentation(),a.materials(),a.playerHealing(),v);
    }
    static ModernBattleObservation.Capture replace(ModernBattleObservation.Capture c,int index,ModernBattleObservation.CapturedActor a) {
        var actors=new ArrayList<>(c.actors());actors.set(index,a);
        return new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),
                c.loadingOriginNanos(),actors,c.modes(),c.npcs(),c.result(),c.hasteTargets(),c.slowTargets(),c.armorWorld());
    }
    @Test void exactCommandRetainsProtectionAndHpEvenWhenClockMovesBeforePublication()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);var command=f.engine.observeCommand(()->{var hit=f.attack(0,1,2);f.elapsed(4*SECOND);return hit;});
            var c=command.observation();var a=c.actors().get(1);var p=publish(c);var v=p.audience(f.players.get(0).scope());
            assertEquals(0,c.clock().activeElapsedNanos());assertTrue(a.vital().values().hp()<10000);
            assertEquals(c.revision(),a.armorProtection().revision());assertEquals(c.revision(),a.vital().hurtRevision());
            var status=v.world().actors().get(1).armorProtection();assertEquals(3*SECOND,status.untilNanos());
            assertEquals(a.vital().values(),v.world().actors().get(1).resources().vitals());assertTrue(status.effects().get(0).appliedProtection());
            var expired=publish(f.engine.serviceObservation());assertEquals(0,expired.world().actors().get(1).armorProtection().untilNanos());
            assertTrue(expired.world().revision()>p.world().revision());assertEquals(3*SECOND,status.untilNanos());
            assertSame(v,p.audience(f.players.get(0).scope()));assertThrows(IllegalArgumentException.class,command.outcome()::snapshot);
        }
    }
    @Test void realHealingAndDurableUnequipDoNotReplayOrRefreshRetainedArmorEvent()throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                ModernBattleEngine.PreparedMember::withPlayerHealing,ModernArmorProtectionEquipmentTest::source,true)) {
            var gear=f.read();ModernArmorProtectionEquipmentTest.active(f);ModernArmorProtectionEquipmentTest.hit(f,1);
            var before=publish(f.capture());var state=before.world().actors().get(0).armorProtection();
            var heal=ModernBattlePlayerHealingTest.heal(f.world,0,1,0);assertEquals(ModernPlayerHealingCast.Reason.NONE,heal.reason());
            var healed=publish(heal.observation());assertEquals(state,healed.world().actors().get(0).armorProtection());
            assertTrue(healed.world().actors().get(0).playerHealing().incoming().restored()>0);
            var off=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,off.equipment().status());var changed=publish(off.observation());
            var a=changed.audience(f.player().scope());assertEquals(1,a.personal().equipment().revision());
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodePlayerHealing(a));
            assertEquals(state,a.world().actors().get(0).armorProtection());assertTrue(state.revision()<a.world().revision());
            assertEquals(healed.world().actors().get(0).playerHealing().incoming(),a.world().actors().get(0).playerHealing().incoming());
            assertEquals(1,f.stored().orElseThrow().revision());f.world.elapsed(3*SECOND);
            assertEquals(0,publish(f.capture()).world().actors().get(0).armorProtection().untilNanos());
        }
    }
    @Test void publicGraphCannotReachRawTriggersCatalogOrPrivateMatchBinding() {
        var forbidden=Set.of(ModernArmorProcProtection.State.class,ModernArmorProcProtection.Actor.class,ModernArmorProcProtection.Trigger.class,
                ModernArmorProcProtection.Selection.class,ModernBattleEngine.ArmorProtectionView.class,ModernArmorProtectionLoadout.class,
                ModernArmorProcs.Rule.class,ModernRooms.Scope.class,ModernBattleLoadout.class);
        var graph=ModernBattleAudienceTest.graph(ModernBattleAudience.World.class,forbidden);
        assertTrue(graph.contains(ModernArmorProtectionEffects.State.class));assertTrue(graph.contains(ModernArmorProtectionEffects.Effect.class));
        assertEquals(List.of("graphics","graphicsMode","appliedProtection","durationSeconds"),
                Arrays.stream(ModernArmorProtectionEffects.Effect.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList());
    }
    @Test void botsHaveNoInventedArmorAndOppositeSourceWithSameObjidHasDistinctPublicEntity()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,1000,SECOND,2,false,true,ModernBattleEngineArmorProtectionTest::protectHonor)) {
            active(f);var p=publish(f.engine.serviceObservation());assertEquals(4,p.world().actors().size());
            for(var a:p.world().actors())assertEquals(a.pose().member().identity().isHuman(),a.armorProtection()!=null);
            var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());assertSame(a.world(),b.world());
            assertEquals(3333,a.personal().scope().objectId());assertEquals(3333,b.personal().scope().objectId());
            assertNotEquals(a.personal().entityId(),b.personal().entityId());assertNotEquals(a.personal().scope().sourceId(),b.personal().scope().sourceId());
        }
    }
    @Test void questLaneFiltersArmorWithActorsAndDoesNotPublishOtherForceReceipts()throws Exception {
        try(var f=quest(10000)) {
            active(f);f.elapsed(100);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.step(living(f.engine,0)).reason());
            var p=publish(f.engine.serviceObservation());
            for(int i=0;i<2;i++) {
                var a=p.audience(f.players.get(i).scope());assertEquals(i,a.world().lane());assertEquals(1,a.world().actors().size());
                var armor=a.world().actors().get(0).armorProtection();assertEquals(i+1,armor.entityId());
                assertEquals(i!=0,armor.effects().isEmpty());assertEquals(2,a.world().room().members().size());
            }
        }
    }
    @Test void mixedPrivateMatchSourceEntityLifeAndFutureRevisionAreRejected()throws Exception {
        try(var f=fixture(10000,false);var other=fixture(10000,false)) {
            active(f);active(other);f.attack(0,1,2);var c=f.engine.serviceObservation();var a=c.actors().get(1);var v=a.armorProtection();var s=v.state();var id=s.actor();
            assertThrows(IllegalArgumentException.class,()->replace(c,1,raw(a,other.engine.serviceObservation().actors().get(1).armorProtection())));
            for(var wrong:List.of(new ModernArmorProcProtection.Actor(id.match(),new TournamentPresence.CharacterKey(7,3333),id.entity(),id.life()),
                    new ModernArmorProcProtection.Actor(id.match(),id.owner(),1,id.life()),new ModernArmorProcProtection.Actor(id.match(),id.owner(),id.entity(),id.life()+1))) {
                var forged=new ModernBattleEngine.ArmorProtectionView(new ModernArmorProcProtection.State(wrong,s.sequence(),s.observedNanos(),s.untilNanos()),v.revision(),v.triggers(),v.anchor());
                assertThrows(IllegalArgumentException.class,()->replace(c,1,raw(a,forged)));
            }
            assertThrows(IllegalArgumentException.class,()->replace(c,1,raw(a,new ModernBattleEngine.ArmorProtectionView(s,c.revision()+1,v.triggers(),v.anchor()))));
            assertThrows(IllegalArgumentException.class,()->replace(c,1,raw(a,null)));
        }
    }
    @Test void visibleStateRejectsExpiredFutureWrongLifeAndImpossibleEffectFacts()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);f.attack(0,1,2);var w=f.engine.serviceObservation().world();var a=w.actors().get(1);var s=a.armorProtection();
            assertThrows(IllegalArgumentException.class,()->s.validate(w.revision(),w.phase(),new ModernBattleObservation.Clock(3*SECOND,3*SECOND),a.pose(),a.resources(),true));
            assertThrows(IllegalArgumentException.class,()->s.validate(w.revision(),ModernBattleEngine.Phase.ABORTED,w.clock(),a.pose(),a.resources(),true));
            assertThrows(IllegalArgumentException.class,()->s.validate(w.revision(),w.phase(),w.clock(),a.pose(),a.resources(),false));
            var wrongLife=new ModernArmorProtectionEffects.State(2,1,s.sequence(),s.revision(),0,3*SECOND,s.effects());
            assertThrows(IllegalArgumentException.class,()->wrongLife.validate(w.revision(),w.phase(),w.clock(),a.pose(),a.resources(),true));
            var future=new ModernArmorProtectionEffects.State(2,0,s.sequence(),w.revision()+1,0,3*SECOND,s.effects());
            assertThrows(IllegalArgumentException.class,()->future.validate(w.revision(),w.phase(),w.clock(),a.pose(),a.resources(),true));
            var clockAhead=new ModernArmorProtectionEffects.State(2,0,s.sequence(),s.revision(),SECOND,4*SECOND,s.effects());
            assertThrows(IllegalArgumentException.class,()->clockAhead.validate(w.revision(),w.phase(),w.clock(),a.pose(),a.resources(),true));
            assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionEffects.State(2,0,0,1,0,3*SECOND,s.effects()));
            assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionEffects.State(2,0,1,1,0,4*SECOND,s.effects()));
            assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionEffects.State(2,0,1,1,0,3*SECOND,List.of(s.effects().get(0),s.effects().get(0))));
            assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionEffects.Effect(65536,0,true,3));
            assertThrows(IllegalArgumentException.class,()->new ModernArmorProtectionEffects.Effect(1,3,true,3));
            assertThrows(UnsupportedOperationException.class,()->s.effects().clear());
        }
    }
    @Test void noExistingWireVersionOrHealingExtensionCanSilentlyDropArmor()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);var c=f.engine.serviceObservation();var d=descriptor(c);var v=publish(c).audience(f.players.get(0).scope());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c,d,null,null));
            assertThrows(IllegalArgumentException.class,()->ModernBattlePublication.playerHealing(c,d,null,null));
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(ByteBuffer.allocate(64),d));
            for(int version=1;version<=47;version++) {
                assertFalse(d.supportedBy(version,Integer.MAX_VALUE));
                assertFalse(d.supportedBy(new ModernSessionBoundary.Negotiated(1,version,1,Integer.MAX_VALUE,3)));
            }
            for(int schema=1;schema<=7;schema++){int chosen=schema;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,chosen));}
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodePlayerHealing(v));
            var missing=new ModernMatchDescriptor(d.ruleset(),d.features()&~ARMOR_PROTECTION,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->ModernBattlePublication.armorProtection(c,missing,null,null));
        }
    }
    @Test void loadingAbortDeathRespawnAndTerminalAbortProduceValidCandidateStates()throws Exception {
        try(var f=fixture(10000,false)) {
            var loading=publish(f.engine.serviceObservation());assertEquals(-1,loading.world().clock().activeElapsedNanos());
            var stopped=publish(f.engine.abortObservation());assertEquals(ModernBattleEngine.Phase.ABORTED,stopped.world().phase());
        }
        try(var f=fixture(1,false)) {
            active(f);var dead=publish(f.cast(0,1,4,2).observation());assertEquals(0,dead.world().actors().get(1).armorProtection().untilNanos());
            f.elapsed(5*SECOND);var revived=publish(f.engine.serviceObservation());var a=revived.world().actors().get(1);
            assertEquals(a.resources().life(),a.armorProtection().life());assertEquals(1,a.armorProtection().life());assertEquals(0,a.armorProtection().sequence());
            publish(f.engine.abortObservation());
        }
    }
}
