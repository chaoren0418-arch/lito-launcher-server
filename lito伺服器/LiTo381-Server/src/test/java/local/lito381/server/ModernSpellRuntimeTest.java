package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentHistoryTest.*;
import static local.lito381.server.ModernTargetHistoryTest.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernSpellRuntimeTest {
    static ModernEquipmentHistory.State empty() {
        var spells=new EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
        for(var lane:LEGACY_LANES)spells.put(lane,ModernUseHistory.Spell.initial(0,1,10));
        return new ModernEquipmentHistory.State(ID,1,ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(1000,1000,1000,1000)),spells,null,Map.of());
    }
    static ModernSpellRuntime.State runtime(ModernEquipmentHistory.State state) {
        return ModernSpellRuntime.State.initial(state.identity(),state.spells().keySet(),state.worldRevision(),500);
    }
    static ModernSpellRuntime.Detail detail(ModernUseHistory.Lane lane,long lawful) {
        return switch(lane) {
            case TELEPORT->new ModernSpellRuntime.Teleport(new ModernWorldMap.Cell(CELL.x()+1,CELL.y()));
            case DISINTEGRATE->new ModernSpellRuntime.Disintegrate(true,lawful,lawful+1,impacts(lane).get(0).applied());
            case NPC_DISINTEGRATE->new ModernSpellRuntime.Disintegrate(true,lawful,lawful+1);
            case LIGHT_ARROW,NPC_LIGHT_ARROW->new ModernSpellRuntime.Arrow(false);
            case FIRE_STORM->new ModernSpellRuntime.FireStorm(impacts(lane).stream().map(ModernTargetHistory.Effect::applied).toList());
            case NPC_FIRE_STORM->new ModernSpellRuntime.NpcFireStorm(ModernQuestObjectives.Kind.ROOM_SYMBOL);
            default->ModernSpellRuntime.Ordinary.INSTANCE;
        };
    }
    @Test void allLanesRetainSpecialOutcomesAndMixedPricesAcrossReprofile() {
        var h=empty();var r=runtime(h);
        for(var lane:LEGACY_LANES) {
            var next=h.cast(lane,h.worldRevision()+1,r.sequence()+1,CELL,impacts(lane));
            r=r.cast(h,next,lane,10+lane.ordinal(),detail(lane,r.lawful()));h=next;
        }
        var retained=r.receipts();assertEquals(502,r.lawful());assertEquals(8,r.sequence());
        var costs=new EnumMap<ModernUseHistory.Lane,Integer>(ModernUseHistory.Lane.class);
        for(var lane:LEGACY_LANES)costs.put(lane,lane.ordinal());
        h=h.reprofile(10,new ModernEquipmentHistory.Profile(1,1000,1000,costs,null));r.validate(h);
        assertEquals(retained,r.receipts());
        for(var lane:LEGACY_LANES) {
            var next=h.cast(lane,h.worldRevision()+1,r.sequence()+1,CELL,impacts(lane));
            r=r.cast(h,next,lane,20+lane.ordinal(),detail(lane,r.lawful()));h=next;
            assertEquals(10+lane.ordinal(),r.receipts().get(lane).spent());assertEquals(2,r.receipts().get(lane).casts());
        }
        assertEquals(504,r.lawful());assertEquals(16,r.sequence());assertEquals(108,h.resources().flows().spent());
        assertEquals(r,new ModernSpellRuntime.Snapshot(h,r).spells());
    }
    @Test void deniedCommandsConsumeExactlyOneSequenceAndNeverReplayOrEraseSuccess() {
        var h=empty();var start=runtime(h);var denied=start.denied(2,1);
        assertTrue(denied.receipts().isEmpty());assertEquals(500,denied.lawful());assertFalse(denied.cooling(0,10));
        var after=h.cast(ModernUseHistory.Lane.DISINTEGRATE,3,2,CELL,impacts(ModernUseHistory.Lane.DISINTEGRATE));
        // The owner history is a later read at command revision2, with unchanged resources.
        var before=new ModernEquipmentHistory.State(h.identity(),2,h.resources(),h.spells(),h.items(),h.targets());
        var cast=denied.cast(before,after,ModernUseHistory.Lane.DISINTEGRATE,100,detail(ModernUseHistory.Lane.DISINTEGRATE,500));
        var rejected=cast.denied(4,3);assertEquals(cast.receipts(),rejected.receipts());assertEquals(501,rejected.lawful());
        assertTrue(rejected.cooling(101,10));assertTrue(rejected.cooling(ModernUseHistory.Lane.DISINTEGRATE,101,10));
        assertFalse(rejected.cooling(ModernUseHistory.Lane.NPC_DISINTEGRATE,101,10));assertFalse(rejected.cooling(110,10));
        assertThrows(IllegalArgumentException.class,()->rejected.denied(5,3));assertThrows(IllegalArgumentException.class,()->rejected.denied(5,5));
        assertThrows(IllegalArgumentException.class,()->rejected.denied(4,4));
        assertThrows(IllegalArgumentException.class,()->start.cast(h,after,ModernUseHistory.Lane.DISINTEGRATE,100,detail(ModernUseHistory.Lane.DISINTEGRATE,500)));
    }
    @Test void typedMetadataCannotBeOmittedOrAppliedToAnotherSkill() {
        var h=empty();var r=runtime(h);
        for(var lane:LEGACY_LANES) {
            var after=h.cast(lane,2,1,CELL,impacts(lane));
            var wrong=lane==ModernUseHistory.Lane.TELEPORT||lane==ModernUseHistory.Lane.DISINTEGRATE||lane==ModernUseHistory.Lane.NPC_DISINTEGRATE
                    ?ModernSpellRuntime.Ordinary.INSTANCE:new ModernSpellRuntime.Teleport(new ModernWorldMap.Cell(1,2));
            assertThrows(IllegalArgumentException.class,()->r.cast(h,after,lane,0,wrong));
        }
        var after=h.cast(ModernUseHistory.Lane.TELEPORT,2,1,CELL,List.of());
        assertThrows(IllegalArgumentException.class,()->r.cast(h,after,ModernUseHistory.Lane.TELEPORT,0,new ModernSpellRuntime.Teleport(CELL)));
        var dis=h.cast(ModernUseHistory.Lane.DISINTEGRATE,2,1,CELL,impacts(ModernUseHistory.Lane.DISINTEGRATE));
        assertThrows(IllegalArgumentException.class,()->r.cast(h,dis,ModernUseHistory.Lane.DISINTEGRATE,0,new ModernSpellRuntime.Disintegrate(false,600,601)));
        assertEquals(ModernDisintegrate.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernDisintegrate.CombatException.class,
                ()->new ModernSpellRuntime.Disintegrate(true,Long.MAX_VALUE,Long.MIN_VALUE)).failure());
    }
    @Test void arbitraryDamageItemReprofileOrOtherLaneCannotPiggybackOnCast() {
        var h=empty();var r=runtime(h);var lane=ModernUseHistory.Lane.LIGHT_ARROW;
        var after=h.cast(lane,2,1,CELL,impacts(lane));
        var forged=new ModernEquipmentHistory.State(after.identity(),3,after.resources().damage(3,1),after.spells(),null,after.targets());
        assertThrows(IllegalArgumentException.class,()->r.cast(h,forged,lane,0,ModernSpellRuntime.Ordinary.INSTANCE));
        assertThrows(IllegalArgumentException.class,()->r.cast(h,after,ModernUseHistory.Lane.NPC_LIGHT_ARROW,0,ModernSpellRuntime.Ordinary.INSTANCE));
        assertTrue(r.receipts().isEmpty());assertEquals(1000,h.resources().vitals().mp());
    }
    @Test void exactOwnerBindingAndLaneSetCannotBeReplaced() {
        var h=empty();var r=runtime(h);
        var identity=new ModernEquipmentHistory.Identity(ID.world(),new ModernRooms.Scope(SOURCE.sourceId(),SOURCE.objectId(),-1,SOURCE.channelId()),CASTER);
        var foreign=new ModernEquipmentHistory.State(identity,1,h.resources(),h.spells(),null,Map.of());
        assertThrows(IllegalArgumentException.class,()->r.validate(foreign));
        var noLanes=new ModernEquipmentHistory.State(ID,1,h.resources(),Map.of(),null,Map.of());
        assertThrows(IllegalArgumentException.class,()->r.validate(noLanes));
        assertThrows(UnsupportedOperationException.class,()->r.lanes().clear());
        assertThrows(UnsupportedOperationException.class,()->r.receipts().clear());
    }
    @Test void runtimeRetainsReceiptsAcrossDamageAndRespawnButDoesNotMistakeThemForCasts() {
        var h=empty();var r=runtime(h);var lane=ModernUseHistory.Lane.LIGHT_ARROW;
        var cast=h.cast(lane,2,1,CELL,impacts(lane));r=r.cast(h,cast,lane,0,detail(lane,r.lawful()));
        var hurt=new ModernEquipmentHistory.State(ID,3,cast.resources().damage(3,1000),cast.spells(),null,cast.targets());r.validate(hurt);
        var respawn=new ModernEquipmentHistory.State(ID,4,hurt.resources().respawn(4,1000,0),hurt.spells(),null,hurt.targets());r.validate(respawn);
        assertEquals(cast.targets().get(lane).last(),r.receipts().get(lane).batch());assertEquals(1,r.sequence());
    }
    @Test void backwardsClocksRejectButSignedNanoClockWrapKeepsCooldown() {
        var h=empty();var lane=ModernUseHistory.Lane.LIGHT_ARROW;var cast=h.cast(lane,2,1,CELL,impacts(lane));
        var r=runtime(h).cast(h,cast,lane,Long.MAX_VALUE-5,detail(lane,500));
        var next=cast.cast(lane,3,2,CELL,impacts(lane));
        assertTrue(r.cooling(Long.MIN_VALUE+4,11));assertFalse(r.cooling(Long.MIN_VALUE+4,10));
        assertThrows(IllegalArgumentException.class,()->r.cast(cast,next,lane,Long.MAX_VALUE-6,detail(lane,r.lawful())));
        assertEquals(2,r.cast(cast,next,lane,Long.MIN_VALUE+4,detail(lane,r.lawful())).sequence());
    }
    @Test void fabricatedSuccessCountsAndLawfulTotalsCannotFitADeniedRequestBudget() {
        var h=empty();var lane=ModernUseHistory.Lane.DISINTEGRATE;var cast=h.cast(lane,2,1,CELL,impacts(lane));
        var r=runtime(h).cast(h,cast,lane,0,detail(lane,500));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.State(ID,r.lanes(),1,0,0,500,501,r.receipts()));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.State(ID,r.lanes(),1,1,2,500,502,r.receipts()));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.State(ID,r.lanes(),1,2,2,500,501,r.receipts()));
        var erased=new ModernSpellRuntime.State(ID,r.lanes(),1,1,2,500,500,Map.of());
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.Snapshot(cast,erased));
    }
    @Test void rawValidationCannotSubstituteADifferentAnchorWithMatchingCounters() {
        var h=empty();var lane=ModernUseHistory.Lane.LIGHT_ARROW;var cast=h.cast(lane,2,1,CELL,impacts(lane));
        var r=runtime(h).cast(h,cast,lane,0,detail(lane,500));var use=cast.spells().get(lane).last();var a=use.before();
        var forged=new ModernUseHistory.Cast(use.revision(),use.sequence(),new ModernUseHistory.Anchor(a.equipmentRevision(),a.life(),a.vitals(),a.x()+1,a.y()),use.cost(),use.mpAfter());
        var spells=new EnumMap<>(cast.spells());spells.put(lane,new ModernUseHistory.Spell(spells.get(lane).current(),forged));
        assertThrows(IllegalArgumentException.class,()->r.validate(cast.worldRevision(),spells,cast.targets()));
    }
    @Test void cumulativeLossAndStructureKindMustMatchTheirHistoricalLane() {
        var h=empty();var r=runtime(h);var lane=ModernUseHistory.Lane.FIRE_STORM;var fire=h.cast(lane,2,1,CELL,impacts(lane));
        assertThrows(IllegalArgumentException.class,()->r.cast(h,fire,lane,0,new ModernSpellRuntime.FireStorm(List.of())));
        assertThrows(IllegalArgumentException.class,()->r.cast(h,fire,lane,0,new ModernSpellRuntime.FireStorm(List.of(49L))));
        var quest=h.cast(ModernUseHistory.Lane.NPC_FIRE_STORM,2,1,CELL,impacts(ModernUseHistory.Lane.NPC_FIRE_STORM));
        assertThrows(IllegalArgumentException.class,()->r.cast(h,quest,ModernUseHistory.Lane.NPC_FIRE_STORM,0,new ModernSpellRuntime.NpcFireStorm(null)));
        var after=r.cast(h,quest,ModernUseHistory.Lane.NPC_FIRE_STORM,0,detail(ModernUseHistory.Lane.NPC_FIRE_STORM,500));
        assertNotNull(after.structure());
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.State(ID,after.lanes(),1,1,2,500,500,after.receipts(),null));
        var target=impacts(ModernUseHistory.Lane.NPC_LIGHT_ARROW);
        var npcOnly=quest.cast(ModernUseHistory.Lane.NPC_FIRE_STORM,3,2,CELL,target);
        var retained=after.cast(quest,npcOnly,ModernUseHistory.Lane.NPC_FIRE_STORM,1,new ModernSpellRuntime.NpcFireStorm(null));
        assertEquals(after.structure(),retained.structure());assertEquals(1,retained.structure().casts());
        var erased=new ModernSpellRuntime.State(ID,retained.lanes(),1,2,3,500,500,retained.receipts(),null);
        assertThrows(IllegalArgumentException.class,()->erased.validate(npcOnly));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.Structure(2,after.structure().batch(),after.structure().questKind()));
    }
}
