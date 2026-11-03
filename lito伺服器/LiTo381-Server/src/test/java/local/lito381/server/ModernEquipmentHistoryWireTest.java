package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentHistoryTest.*;
import static local.lito381.server.ModernTargetHistoryTest.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernEquipmentHistoryWireTest {
    static ModernEquipmentHistory.State roundTrip(ModernEquipmentHistory.State state) {
        var bytes=ModernEquipmentHistoryWire.encode(state);
        var decoded=ModernEquipmentHistoryWire.decodeBound(bytes,state.identity(),state.worldRevision());
        assertEquals(state,decoded);assertArrayEquals(bytes,ModernEquipmentHistoryWire.encode(decoded));return decoded;
    }
    static ModernEquipmentHistory.State empty(long epoch,long value) {
        var id=new ModernEquipmentHistory.Identity(WORLD,new ModernRooms.Scope(7,3333,epoch,3),CASTER);
        return new ModernEquipmentHistory.State(id,1,ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(value,value,value,value)),Map.of(),null,Map.of());
    }
    @Test void completeHeaderAndAllLanesRoundTripBeforeRepriceAndAfterMixedPriceRecasts() {
        assertEquals(256,ModernEquipmentHistoryWire.encode(empty(4,Long.MAX_VALUE)).length);
        roundTrip(empty(Long.MIN_VALUE,Long.MAX_VALUE));roundTrip(empty(-1,Long.MAX_VALUE));
        var previous=roundTrip(populated());var state=roundTrip(previous.reprofile(12,next(previous,100,50)));
        assertTrue(ModernEquipmentHistory.followsReprofile(previous,state));
        for(var lane:LEGACY_LANES) {
            var before=state;state=roundTrip(state.cast(lane,state.worldRevision()+1,9+lane.ordinal(),CELL,impacts(lane)));
            assertTrue(ModernEquipmentHistory.followsCast(before,state,lane));assertFalse(ModernEquipmentHistory.followsReprofile(before,state));
            var current=state.spells().get(lane).current();assertEquals(2,current.casts());assertEquals(10+lane.ordinal(),current.spent());
            assertNotEquals(current.casts()*current.cost(),current.spent(),"past cast must retain cost10");
            assertEquals(1,state.spells().get(lane).last().before().equipmentRevision());
        }
        assertEquals(108,state.resources().flows().spent());assertEquals(22,state.resources().vitals().mp());
        assertEquals(ModernEquipmentHistoryWire.SCHEMA,1);assertEquals(47,ModernSessionProtocol.MAX_VERSION);
    }
    @Test void singleCastCannotRepriceOrResetOtherLanesItemsAndResources() {
        var before=populated();var after=before.cast(ModernUseHistory.Lane.LIGHT_ARROW,12,9,CELL,impacts(ModernUseHistory.Lane.LIGHT_ARROW));
        assertTrue(ModernEquipmentHistory.followsCast(before,after,ModernUseHistory.Lane.LIGHT_ARROW));
        assertFalse(ModernEquipmentHistory.followsCast(before,before,ModernUseHistory.Lane.LIGHT_ARROW));
        assertFalse(ModernEquipmentHistory.followsCast(before,after,ModernUseHistory.Lane.FIRE_STORM));
        assertEquals(before.items(),after.items());assertEquals(before.resources().flows().damaged(),after.resources().flows().damaged());
        for(var lane:LEGACY_LANES)if(lane!=ModernUseHistory.Lane.LIGHT_ARROW){assertEquals(before.spells().get(lane),after.spells().get(lane));assertEquals(before.targets().get(lane),after.targets().get(lane));}
        var repriced=after.reprofile(13,next(after,100,50));assertFalse(ModernEquipmentHistory.followsCast(before,repriced,ModernUseHistory.Lane.LIGHT_ARROW));
        var mixed=new ModernEquipmentHistory.State(after.identity(),13,after.resources().damage(13,1),after.spells(),after.items(),after.targets());
        assertFalse(ModernEquipmentHistory.followsCast(before,mixed,ModernUseHistory.Lane.LIGHT_ARROW));
    }
    @Test void rejectedCastCannotPublishPartialSpendOrTargetsAndGlobalSequenceCannotGoBackwards() {
        var s=populated();var encoded=ModernEquipmentHistoryWire.encode(s);
        for(var lane:LEGACY_LANES) {
            assertThrows(IllegalArgumentException.class,()->s.cast(lane,12,8,CELL,impacts(lane)));
            assertThrows(IllegalArgumentException.class,()->s.cast(lane,11,9,CELL,impacts(lane)));
        }
        assertThrows(IllegalArgumentException.class,()->s.cast(ModernUseHistory.Lane.LIGHT_ARROW,12,9,CELL,List.of()));
        var poor=s.reprofile(12,next(s,100,0));assertThrows(IllegalArgumentException.class,()->poor.cast(ModernUseHistory.Lane.FIRE_STORM,13,9,CELL,impacts(ModernUseHistory.Lane.FIRE_STORM)));
        var free=poor.cast(ModernUseHistory.Lane.TELEPORT,13,9,CELL,List.of());assertEquals(0,free.resources().vitals().mp());assertEquals(2,free.spells().get(ModernUseHistory.Lane.TELEPORT).current().casts());
        assertArrayEquals(encoded,ModernEquipmentHistoryWire.encode(s));
    }
    @Test void everyTruncationAndTrailingByteIsRejectedEvenWithForgedDeclaredLength() {
        var bytes=ModernEquipmentHistoryWire.encode(populated());
        for(int n=0;n<bytes.length;n++) {var truncated=Arrays.copyOf(bytes,n);if(n>=12)ByteBuffer.wrap(truncated).putInt(8,n);assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(truncated),"prefix "+n);}
        var extra=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(extra).putInt(8,extra.length);
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(extra));
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(new byte[ModernEquipmentHistoryWire.MAX_BYTES+1]));
    }
    @Test void unknownTagsNonzeroPaddingDuplicateLanesAndOversizedTargetCountsAreRejected() {
        var bytes=ModernEquipmentHistoryWire.encode(populated());
        for(int offset:new int[]{0,5,6,7,8,57,61,69,70,71,254,255,257,258,259,280,281,282,283,334,335,412,413,414,415,426,427,602,603,604,605,606,607,617,618,619,632,647}) {
            var changed=bytes.clone();changed[offset]^=0x40;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(changed),"offset "+offset);
        }
        for(int[] edit:new int[][]{{252,9},{253,2},{256,8},{332,2},{333,3},{428,0},{600,5},{601,2}}) {
            var changed=bytes.clone();changed[edit[0]]=(byte)edit[1];assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(changed),"tag "+edit[0]);
        }
        var tooMany=bytes.clone();ByteBuffer.wrap(tooMany).putShort(596,(short)65535);assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(tooMany));
    }
    @Test void boundDecodeRejectsOtherWorldSourceEpochActorChannelAndRevision() {
        var s=populated();var bytes=ModernEquipmentHistoryWire.encode(s);
        for(var id:List.of(new ModernEquipmentHistory.Identity(new ModernTargetHistory.World(10,21,30),SOURCE,CASTER),
                new ModernEquipmentHistory.Identity(WORLD,new ModernRooms.Scope(7,3333,-1,0),CASTER),
                new ModernEquipmentHistory.Identity(WORLD,new ModernRooms.Scope(7,3333,4,3),CASTER),
                new ModernEquipmentHistory.Identity(WORLD,SOURCE,new ModernTargetHistory.Participant(2,CASTER.identity())),
                new ModernEquipmentHistory.Identity(WORLD,new ModernRooms.Scope(42,3333,4,0),new ModernTargetHistory.Participant(1,ModernParticipantId.human(42,3333)))))
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decodeBound(bytes,id,s.worldRevision()));
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decodeBound(bytes,s.identity(),s.worldRevision()+1));
        var invalid=bytes.clone();ByteBuffer.wrap(invalid).putLong(220,79);assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(invalid));
    }
    @Test void retainedStructureUsesOneCanonicalRepresentationAndCannotBeSilentlyDropped() {
        var bytes=ModernEquipmentHistoryWire.encode(populated());
        assertEquals(2572,bytes.length);assertEquals(6,bytes[1848]);assertEquals(1,bytes[1925]);
        var dropped=bytes.clone();dropped[1925]=0;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(dropped));
        var fabricated=bytes.clone();fabricated[333]=1;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(fabricated));
        // Same latest structure encoded a second time would be semantically
        // equal, but schema1 requires retained=1 without duplicate bytes.
        var duplicate=new byte[bytes.length+204];System.arraycopy(bytes,0,duplicate,0,2132);
        System.arraycopy(bytes,1928,duplicate,2132,204);System.arraycopy(bytes,2132,duplicate,2336,bytes.length-2132);
        duplicate[1925]=2;ByteBuffer.wrap(duplicate).putInt(8,duplicate.length);
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(duplicate));
    }
    static ModernEquipmentHistory.State maximum() {
        var s=populated();var fire=new ArrayList<ModernTargetHistory.Effect>();
        for(int i=2;i<=100;i++)fire.add(hit(new ModernTargetHistory.Participant(i,i==2?ModernParticipantId.human(42,3333):ModernParticipantId.bot(42,i))));
        fire.add(hit(new ModernTargetHistory.Honor(101,0)));fire.add(hit(new ModernTargetHistory.Honor(102,1)));
        s=s.cast(ModernUseHistory.Lane.FIRE_STORM,12,9,CELL,fire);
        var npc=new ArrayList<ModernTargetHistory.Effect>();for(int i=1;i<=47;i++)npc.add(hit(new ModernTargetHistory.Npc(new ModernNpcRoster.Id(NPC_WORLD,i))));
        npc.add(hit(new ModernTargetHistory.TimeTower(new ModernTimeAttack.TowerId(NPC_WORLD))));
        s=s.cast(ModernUseHistory.Lane.NPC_FIRE_STORM,13,10,CELL,npc);
        npc.set(47,hit(new ModernTargetHistory.Npc(new ModernNpcRoster.Id(NPC_WORLD,48))));
        return s.cast(ModernUseHistory.Lane.NPC_FIRE_STORM,14,11,CELL,npc);
    }
    @Test void maximumAoeAndRetainedTimeTowerStayBoundedAndLosslessAcrossReprice() {
        var s=roundTrip(maximum());var bytes=ModernEquipmentHistoryWire.encode(s);assertTrue(bytes.length>22000&&bytes.length<ModernEquipmentHistoryWire.MAX_BYTES);
        assertEquals(101,s.targets().get(ModernUseHistory.Lane.FIRE_STORM).last().targets().size());
        var h=s.targets().get(ModernUseHistory.Lane.NPC_FIRE_STORM);assertEquals(48,h.last().targets().size());assertEquals(48,h.structure().targets().size());
        assertTrue(h.structure().targets().get(47).before().target() instanceof ModernTargetHistory.TimeTower);
        var repriced=roundTrip(s.reprofile(15,next(s,100,50)));assertEquals(s.targets(),repriced.targets());
    }
    @Test void actualOwnerEngineHistoryUsesCodecWithoutPublishingAnAdvancedSet()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);var before=roundTrip(ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            var after=roundTrip(ModernBattleEquipmentHistoryTest.read(f.engine,f.ticket,p));
            assertTrue(ModernEquipmentHistory.followsCast(before,after,ModernUseHistory.Lane.LIGHT_ARROW));
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void everyAcceptedSingleByteMutationStillHasAnExactCanonicalReencoding() {
        var bytes=ModernEquipmentHistoryWire.encode(populated());int accepted=0,rejected=0;
        for(int offset=0;offset<bytes.length;offset++) {
            var mutation=bytes.clone();mutation[offset]^=1;
            ModernEquipmentHistory.State decoded;
            try{decoded=ModernEquipmentHistoryWire.decode(mutation);}catch(IllegalArgumentException expected){rejected++;continue;}
            accepted++;assertArrayEquals(mutation,ModernEquipmentHistoryWire.encode(decoded),"offset "+offset);
        }
        assertTrue(accepted>0);assertTrue(rejected>2000);
    }
}
