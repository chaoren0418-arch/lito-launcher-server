package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import static local.lito381.server.ModernArmorProtectionObservationTest.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real engine/publication plus synthetic rules. No resource files, LOAD,
 * schema negotiation or player UI are certified by these producer tests. */
class ModernArmorPresentationTest {
    @TempDir Path root;
    static ModernArmorPresentation.Candidate own(ModernBattleObservation.Capture c,int who) {
        return publish(c).armorPresentation(c.actors().get(who).scope());
    }
    static ModernBattleEngineHonorTest.Fixture variant(int graphic,int mode,int rows)throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(10000,10000,1000,SECOND,0,false,true,p->{
            var v=p.fighter().profile().source().persisted();
            var seed=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.character(),v.maxHp(),v.maxMp(),0);
            var rules=new HashMap<Long,List<ModernArmorProcs.Rule>>();
            for(var item:seed.input().equipment().source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR) {
                var list=new ArrayList<ModernArmorProcs.Rule>();
                for(int n=0;n<rows;n++)list.add(new ModernArmorProcs.Rule(n+1,item.template().catalogId(),-1,88,
                        0,100,0,0,0,0,0,0,0,68,3,1,graphic,mode));
                rules.put(item.template().catalogId(),list);
            }
            var fighter=ModernArmorProtectionLoadoutTest.qualify(ModernArmorProtectionLoadoutTest.withRules(seed,Set.of(),rules));
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,
                    p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),
                    ModernInventoryWeight.qualify(fighter.profile(),p.weightBasis().sourceRate()),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),
                    p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),p.greaterHaste(),p.haste(),p.slow(),p.playerHealing());
        });
    }
    @Test void loadingDeclaresMissingProcSeparatelyFromSkillAndIconAndDoesNotInventAnEvent()throws Exception {
        try(var f=fixture(10000,false)) {
            var c=f.engine.serviceObservation();var p=publish(c);
            for(int who=0;who<2;who++) {
                var candidate=p.armorPresentation(f.players.get(who).scope());
                assertEquals(who+1,candidate.entityId());assertEquals(0,candidate.life());assertNull(candidate.batch());
                assertEquals(List.of(228,11101),candidate.required().graphics());assertEquals(List.of(1562),candidate.required().icons());
                assertEquals(c.identity(),candidate.world());assertEquals(c.revision(),candidate.worldRevision());
                assertSame(candidate,p.armorPresentation(f.players.get(who).scope()));
            }
            assertEquals(3333,f.players.get(0).scope().objectId());assertEquals(3333,f.players.get(1).scope().objectId());
            assertThrows(SecurityException.class,()->p.armorPresentation(new ModernRooms.Scope(99,3333,1,0)));
            assertThrows(UnsupportedOperationException.class,()->own(c,0).required().graphics().clear());
        }
    }
    @Test void acceptedAnchorAndTimeSurviveMovementTeleportDelayedPublicationAndExpiry()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);var command=f.engine.observeCommand(()->{var hit=f.attack(0,1,2);f.elapsed(SECOND);return hit;});
            var c=command.observation();var initial=own(c,1);var batch=initial.batch();
            assertNull(own(c,0).batch(),"self proc is not an attacker/broadcast visual");
            assertEquals(0,batch.observedNanos());assertEquals(c.revision(),batch.revision());
            assertEquals(c.actors().get(1).pose().position(),batch.anchor().cell());assertEquals(0,batch.anchor().moveSequence());
            assertEquals(List.of(11101,228),batch.visuals().stream().map(ModernArmorPresentation.Visual::graphic).toList());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,1,2).status());
            var moved=f.engine.serviceObservation();assertNotEquals(batch.anchor().cell(),moved.actors().get(1).pose().position());
            assertEquals(batch,own(moved,1).batch());
            f.elapsed(2*SECOND); // Respect movement/cast action spacing.
            var teleported=f.cast(1,1,5,0);assertEquals(ModernSpellProtocol.Reason.NONE,teleported.reason());
            var t=teleported.observation();assertEquals(1,t.actors().get(1).pose().lastMoveSequence());
            assertNotEquals(moved.actors().get(1).pose().position(),t.actors().get(1).pose().position());assertEquals(batch,own(t,1).batch());
            f.elapsed(4*SECOND);assertNull(own(f.engine.serviceObservation(),1).batch());
            assertEquals(batch,own(c,1).batch(),"old command must not be reconstructed from current engine time");
            assertThrows(UnsupportedOperationException.class,()->batch.visuals().clear());
        }
        try(var f=fixture(10000,false)) {
            active(f);var batch=own(f.attack(0,1,2).observation(),1).batch();f.elapsed(SECOND);
            // Teleport changes cell even when the anchor/current MOVE sequences
            // are identical. Treating that as corruption would break teleport.
            var teleported=f.cast(1,1,5,0);assertEquals(ModernSpellProtocol.Reason.NONE,teleported.reason());
            var c=teleported.observation();assertEquals(batch.anchor().moveSequence(),c.actors().get(1).pose().lastMoveSequence());
            assertNotEquals(batch.anchor().cell(),c.actors().get(1).pose().position());assertEquals(batch,own(c,1).batch());
        }
    }
    @Test void modesKeepNativeOrderingMultiplicityAndDoNotRefreshSkillVisual()throws Exception {
        for(int mode=0;mode<=2;mode++)try(var f=variant(11101,mode,2)) {
            active(f);var first=own(f.attack(0,1,2).observation(),1).batch();
            var expected=new ArrayList<ModernArmorPresentation.Role>();
            for(int i=0;i<2;i++) {
                if(mode!=1)expected.add(ModernArmorPresentation.Role.PROC_TARGET);
                if(mode!=0)expected.add(ModernArmorPresentation.Role.PROC_ACTOR);
                if(i==0)expected.add(ModernArmorPresentation.Role.SKILL_CAST);
            }
            assertEquals(expected,first.visuals().stream().map(ModernArmorPresentation.Visual::role).toList());
            for(int i=0;i<first.visuals().size();i++)assertEquals(i,first.visuals().get(i).ordinal());
            f.elapsed(SECOND);var second=own(f.attack(0,2,2).observation(),1).batch();
            assertEquals(mode==2?4:2,second.visuals().size());assertTrue(second.visuals().stream().allMatch(v->v.graphic()==11101));
            assertEquals(first.sequence()+1,second.sequence());assertEquals(3*SECOND,armor(f).state().untilNanos());
            assertEquals(List.of(228,11101),own(f.engine.serviceObservation(),1).required().graphics(),"only assets, not visual events, are deduplicated");
        }
    }
    @Test void zeroProcGraphicDoesNotSuppressGrantedSkillAndMaximumSiblingBatchIsBounded()throws Exception {
        try(var f=variant(0,2,1)) {
            active(f);var c=own(f.attack(0,1,2).observation(),1);
            assertEquals(List.of(228),c.required().graphics());assertEquals(1,c.batch().visuals().size());
            assertEquals(ModernArmorPresentation.Role.SKILL_CAST,c.batch().visuals().get(0).role());
            f.elapsed(SECOND);assertTrue(own(f.attack(0,2,2).observation(),1).batch().visuals().isEmpty());
        }
        try(var f=variant(11101,2,ModernArmorProcs.MAX_ROWS)) {
            active(f);var c=own(f.attack(0,1,2).observation(),1);
            assertEquals(ModernArmorPresentation.MAX_VISUALS,c.batch().visuals().size());
            assertEquals(1,c.batch().visuals().stream().filter(v->v.role()==ModernArmorPresentation.Role.SKILL_CAST).count());
        }
    }
    @Test void exactDisabledRowsSuppressWildcardResourcesAndInactiveCatalogsAreNotPreloaded() {
        var seed=ModernArmorProtectionCatalogTest.source(7);long catalog=88889;
        long enchant=seed.input().equipment().source().inventory().stream().filter(i->i.template().catalogId()==catalog).findFirst().orElseThrow().enchantment();
        var wildcard=ModernArmorProcsTest.rule(1,catalog,-1,88,0,100);
        var disabled=ModernArmorProcsTest.rule(2,catalog,enchant,88,0,0);
        var s=ModernArmorProtectionLoadoutTest.withRules(seed,Set.of(),Map.of(catalog,List.of(wildcard,disabled)));
        var required=ModernArmorProtectionLoadoutTest.qualify(s).armorProtection().presentationRequirements();
        assertTrue(required.graphics().isEmpty());assertTrue(required.icons().isEmpty());
    }
    @Test void durableUnequipKeepsReequipRequirementsAndReceiptButNextNonProcHitHasNoAnchor()throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                ModernBattleEngine.PreparedMember::withPlayerHealing,ModernArmorProtectionEquipmentTest::source,true)) {
            var gear=f.read();ModernArmorProtectionEquipmentTest.active(f);ModernArmorProtectionEquipmentTest.hit(f,1);
            var before=own(f.capture(),0);var off=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,off.equipment().status());
            var after=own(off.observation(),0);assertEquals(before.batch(),after.batch());assertEquals(before.required(),after.required());
            assertEquals(before.equipmentRevision()+1,after.equipmentRevision());
            var heal=ModernBattlePlayerHealingTest.heal(f.world,0,1,0);assertEquals(ModernPlayerHealingCast.Reason.NONE,heal.reason());
            assertEquals(before.batch(),own(heal.observation(),0).batch());
            f.world.elapsed(SECOND);ModernArmorProtectionEquipmentTest.hit(f,2);assertNull(own(f.capture(),0).batch());
            assertNull(f.capture().actors().get(0).armorProtection().anchor());
            assertEquals(before.required(),own(f.capture(),0).required());assertEquals(3*SECOND,f.capture().actors().get(0).armorProtection().state().untilNanos());
        }
    }
    @Test void invalidAnchorsOrMixedAudienceAreRejectedWithoutChangingLiveState()throws Exception {
        try(var f=fixture(10000,false);var other=fixture(10000,false)) {
            active(f);active(other);var c=f.attack(0,1,2).observation();var a=c.actors().get(1);var v=a.armorProtection();
            for(var anchor:List.of(new ModernArmorPresentation.Anchor(1,v.anchor().cell()),
                    new ModernArmorPresentation.Anchor(0,new ModernWorldMap.Cell(Integer.MAX_VALUE,Integer.MIN_VALUE)),
                    new ModernArmorPresentation.Anchor(0,c.actors().get(0).pose().position())))
                assertThrows(IllegalArgumentException.class,()->replace(c,1,raw(a,new ModernBattleEngine.ArmorProtectionView(v.state(),v.revision(),v.triggers(),anchor))));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.ArmorProtectionView(v.state(),v.revision(),v.triggers(),null));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.ArmorProtectionView(v.state(),v.revision(),List.of(),v.anchor()));
            var foreign=publish(other.engine.serviceObservation()).audience(other.players.get(1).scope());
            assertThrows(IllegalArgumentException.class,()->ModernArmorPresentation.from(c,foreign));assertEquals(c,f.engine.serviceObservation());
        }
    }
    @Test void questOtherForceHasNoSelfProcBatchAndDeathRespawnAbortClearAnchors()throws Exception {
        try(var f=quest(10000)) {
            active(f);f.elapsed(100);var c=f.step(living(f.engine,0)).observation();
            assertNotNull(own(c,0).batch());assertNull(own(c,1).batch());
            var prior=publish(c);var aborted=f.engine.abortObservation();
            var stopped=ModernBattlePublication.armorProtection(aborted,descriptor(aborted),prior.resources(),prior.names());
            assertNull(stopped.armorPresentation(f.players.get(0).scope()).batch());
        }
        try(var f=fixture(1,false)) {
            active(f);var dead=f.cast(0,1,4,2).observation();assertNull(own(dead,1).batch());
            f.elapsed(5*SECOND);var revived=own(f.engine.serviceObservation(),1);assertNull(revived.batch());assertEquals(1,revived.life());
        }
    }
    @Test void candidateGraphHasNoPrivateSourceRowsOrBodyActionAndOldSchemaStaysUnchanged()throws Exception {
        var forbidden=Set.of(ModernArmorProcProtection.Trigger.class,ModernArmorProcProtection.Actor.class,ModernArmorProcs.Rule.class,
                ModernArmorProtectionLoadout.class,ModernRooms.Scope.class,ModernBattleLoadout.class,ModernPhysicalCombat.Fighter.class);
        var graph=ModernBattleAudienceTest.graph(ModernArmorPresentation.Candidate.class,forbidden);
        assertTrue(graph.contains(ModernArmorPresentation.Anchor.class));assertTrue(graph.contains(ModernArmorPresentation.Requirements.class));
        assertEquals(List.of("ordinal","graphic","role"),Arrays.stream(ModernArmorPresentation.Visual.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList());
        try(var f=fixture(10000,false)) {
            active(f);var c=f.attack(0,1,2).observation();var p=publish(c);var view=p.audience(f.players.get(1).scope());
            var before=ModernBattleWire.encodeArmorProtection(view);p.armorPresentation(f.players.get(1).scope());
            assertArrayEquals(before,ModernBattleWire.encodeArmorProtection(view));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodePlayerHealing(view));
        }
    }
}
