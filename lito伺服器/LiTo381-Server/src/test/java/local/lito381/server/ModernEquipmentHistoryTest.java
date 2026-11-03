package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentHistory.*;
import static local.lito381.server.ModernTargetHistoryTest.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernEquipmentHistoryTest {
    // Frozen LHI1 has eight lanes; do not accidentally extend golden packets.
    static final List<ModernUseHistory.Lane> LEGACY_LANES=List.copyOf(java.util.EnumSet.range(
            ModernUseHistory.Lane.TELEPORT,ModernUseHistory.Lane.NPC_DISINTEGRATE));
    static final Identity ID=new Identity(WORLD,SOURCE,CASTER);
    static ModernUseHistory.Anchor anchor(ModernVitalBalance.State r) {
        return new ModernUseHistory.Anchor(r.equipmentRevision(),r.life()+1,r.vitals(),CELL.x(),CELL.y());
    }
    static List<ModernTargetHistory.Effect> impacts(ModernUseHistory.Lane lane) {
        return switch(lane) {
            case TELEPORT,GREATER_HASTE,HASTE,SLOW->List.of();
            case HEALING->List.of(new ModernTargetHistory.Effect(ModernTargetHistory.Kind.HEAL,
                    health(new ModernTargetHistory.Honor(3,0),1,0,100,100),5,0,100));
            case LIGHT_ARROW,FIRE_STORM,DISINTEGRATE->List.of(hit(new ModernTargetHistory.Participant(2,ModernParticipantId.human(42,3333))));
            case NPC_LIGHT_ARROW,NPC_DISINTEGRATE->List.of(hit(new ModernTargetHistory.Npc(new ModernNpcRoster.Id(NPC_WORLD,1))));
            case NPC_FIRE_STORM->List.of(hit(new ModernTargetHistory.Quest(new ModernQuestObjectives.Id(NPC_WORLD,1),1)));
        };
    }
    static State populated() {
        var r=ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(1000,1000,1000,1000)).damage(2,100);
        var spells=new EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
        var targets=new EnumMap<ModernUseHistory.Lane,ModernTargetHistory.History>(ModernUseHistory.Lane.class);
        for(var lane:LEGACY_LANES) {
            long rev=lane.ordinal()+3L;var spell=ModernUseHistory.Spell.initial(0,1,10).cast(rev,lane.ordinal()+1,anchor(r));
            spells.put(lane,spell);targets.put(lane,ModernTargetHistory.History.first(new ModernTargetHistory.Batch(WORLD,SOURCE,CASTER,lane,spell.last(),impacts(lane))));
            r=r.spend(rev,10);
        }
        var item=ModernUseHistory.Items.initial(0,1,5).use(11,1,40010,anchor(r),10);
        r=r.restore(11,item.last().restored());return new State(ID,11,r,spells,item,targets);
    }
    static Profile next(State s,long hp,long mp) {
        var costs=new EnumMap<ModernUseHistory.Lane,Integer>(ModernUseHistory.Lane.class);
        s.spells().keySet().forEach(l->costs.put(l,l.ordinal()));
        return new Profile(s.resources().equipmentRevision()+1,hp,mp,costs,7L);
    }
    static State copy(State s,Identity id,long revision,ModernVitalBalance.State r,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,ModernUseHistory.Items items,Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets) {
        return new State(id,revision,r,spells,items,targets);
    }
    @Test void allLanesRepriceTogetherWhileEveryPastSelfAndTargetReceiptRemainsExact() {
        var before=populated();var after=before.reprofile(12,next(before,100,50));
        assertTrue(followsReprofile(before,after));assertEquals(80,after.resources().flows().spent());assertEquals(15,after.resources().flows().restored());
        assertEquals(815,after.resources().flows().hpClipped());assertEquals(870,after.resources().flows().mpClipped());
        assertEquals(before.targets(),after.targets());assertEquals(before.items().last(),after.items().last());
        for(var lane:before.spells().keySet()) {
            var old=before.spells().get(lane);var now=after.spells().get(lane);assertEquals(old.last(),now.last());
            assertEquals(10,now.current().spent());assertEquals(lane.ordinal(),now.current().cost());assertEquals(12,now.current().profileRevision());
        }
        var up=after.reprofile(13,next(after,2000,2000));assertEquals(after.resources().vitals().hp(),up.resources().vitals().hp());
        assertEquals(after.resources().vitals().mp(),up.resources().vitals().mp());assertEquals(1000,up.resources().birthHp());
        assertTrue(followsReprofile(after,up));assertFalse(followsReprofile(before,up));assertFalse(followsReprofile(before,before));
    }
    @Test void missingOrAddedCapabilitiesAndPartialProfilesCannotBeReprofiled() {
        var a=populated();var p=next(a,100,50);var costs=new EnumMap<>(p.costs());costs.remove(ModernUseHistory.Lane.TELEPORT);
        assertThrows(IllegalArgumentException.class,()->a.reprofile(12,new Profile(1,100,50,costs,7L)));
        assertThrows(IllegalArgumentException.class,()->a.reprofile(12,new Profile(1,100,50,p.costs(),null)));
        assertThrows(IllegalArgumentException.class,()->a.reprofile(11,p));assertThrows(IllegalArgumentException.class,()->a.reprofile(12,new Profile(2,100,50,p.costs(),7L)));
        var base=new State(ID,1,ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(100,100,0,0)),Map.of(),null,Map.of());
        assertThrows(IllegalArgumentException.class,()->base.reprofile(2,new Profile(1,100,0,Map.of(ModernUseHistory.Lane.TELEPORT,10),null)));
        assertThrows(IllegalArgumentException.class,()->base.reprofile(2,new Profile(1,100,0,Map.of(),0L)));
    }
    @Test void individuallyValidComponentsCannotHideMissingSpendRecoveryOrWrongProfileEpoch() {
        var a=populated();var rebuilt=ModernVitalBalance.State.initial(11,0,a.resources().vitals());
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,11,rebuilt,a.spells(),a.items(),a.targets()));
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,11,a.resources(),a.spells(),null,a.targets()));
        var spells=new EnumMap<>(a.spells());spells.put(ModernUseHistory.Lane.TELEPORT,a.spells().get(ModernUseHistory.Lane.TELEPORT).reprice(1,12,10));
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,12,a.resources(),spells,a.items(),a.targets()));
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,10,a.resources(),a.spells(),a.items(),a.targets()));
    }
    @Test void scopeEpochChannelWorldAndTypedActorAreBoundToEveryTargetBatch() {
        var a=populated();
        for(var id:List.of(new Identity(new ModernTargetHistory.World(10,21,30),SOURCE,CASTER),
                new Identity(WORLD,new ModernRooms.Scope(7,3333,5,0),CASTER),new Identity(WORLD,new ModernRooms.Scope(7,3333,4,1),CASTER),
                new Identity(WORLD,SOURCE,new ModernTargetHistory.Participant(2,CASTER.identity()))))
            assertThrows(IllegalArgumentException.class,()->copy(a,id,11,a.resources(),a.spells(),a.items(),a.targets()));
        assertThrows(IllegalArgumentException.class,()->new Identity(WORLD,SOURCE,new ModernTargetHistory.Participant(1,ModernParticipantId.bot(7,3333))));
        assertThrows(IllegalArgumentException.class,()->new Identity(WORLD,new ModernRooms.Scope(42,3333,4,0),CASTER));
    }
    @Test void duplicateCastSequenceOrRevisionAcrossDistinctLanesIsNotTwoCasts() {
        var a=populated();var lane=ModernUseHistory.Lane.LIGHT_ARROW;var first=a.spells().get(ModernUseHistory.Lane.TELEPORT);
        var spells=new EnumMap<>(a.spells());spells.put(lane,first);var targets=new EnumMap<>(a.targets());
        targets.put(lane,ModernTargetHistory.History.first(new ModernTargetHistory.Batch(WORLD,SOURCE,CASTER,lane,first.last(),impacts(lane))));
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,11,a.resources(),spells,a.items(),targets));
    }
    @Test void currentGearCapacityCannotDisagreeWithOwnHistoricalUseButOldGearMay() {
        var a=populated();var r=a.resources();var v=r.vitals();
        var changed=new ModernVitalBalance.State(r.revision(),0,0,r.birthHp(),r.birthMp(),new ModernEquipmentVitals.Vitals(v.hp(),1001,v.mp(),1000),r.flows());
        assertThrows(IllegalArgumentException.class,()->copy(a,ID,11,changed,a.spells(),a.items(),a.targets()));
        var after=a.reprofile(12,next(a,100,50));assertTrue(after.spells().values().stream().allMatch(h->h.last().before().vitals().maximumHp()==1000));
        assertEquals(100,after.resources().vitals().maximumHp());
    }
    @Test void validButRewrittenHistoricalTargetCannotFollowAnEquipmentEvent() {
        var a=populated();var after=a.reprofile(12,next(a,100,50));var lane=ModernUseHistory.Lane.LIGHT_ARROW;
        var b=after.targets().get(lane).last();var target=new ModernTargetHistory.Effect(ModernTargetHistory.Kind.DAMAGE,
                health(new ModernTargetHistory.Participant(2,ModernParticipantId.human(42,3333)),2,0,40,100),100,40,0);
        var targets=new EnumMap<>(after.targets());targets.put(lane,ModernTargetHistory.History.first(new ModernTargetHistory.Batch(b.world(),b.source(),b.caster(),lane,b.use(),List.of(target))));
        var forged=copy(after,ID,12,after.resources(),after.spells(),after.items(),targets);assertFalse(followsReprofile(a,forged));
        assertThrows(UnsupportedOperationException.class,()->after.spells().clear());assertThrows(UnsupportedOperationException.class,()->after.profile().costs().clear());
    }
    @Test void noHistoryDoesNotPermitAValidLookingRebindOrResourceReset() {
        var a=new State(ID,1,ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(100,100,50,50)),Map.of(),null,Map.of());
        var b=a.reprofile(2,new Profile(1,10,5,Map.of(),null));
        var other=new Identity(new ModernTargetHistory.World(11,20,30),SOURCE,CASTER);
        assertFalse(followsReprofile(a,new State(other,2,b.resources(),Map.of(),null,Map.of())));
        var reset=ModernVitalBalance.State.initial(2,1,b.resources().vitals());
        assertFalse(followsReprofile(a,new State(ID,2,reset,Map.of(),null,Map.of())));
        assertEquals(90,b.resources().flows().hpClipped());assertEquals(45,b.resources().flows().mpClipped());
    }
}
