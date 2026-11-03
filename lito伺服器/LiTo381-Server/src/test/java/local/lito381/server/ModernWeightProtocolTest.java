package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernInventoryWeightTest.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernWeightProtocolTest {
    static final String GOLDEN="000000000000000100000000000000020000000000000003406e0000000000000003000000010000"
            +"0000000000009c4a00000000000007d0";
    static ModernInventoryWeight.Snapshot snapshot() {
        return weight(1,240,List.of(new ModernInventoryWeight.Carried(40010,1,2000)));
    }
    @Test void independentHeaderAndUnitLiteralRetainsBinary64AndOriginalWeight() throws Exception {
        var out=ByteBuffer.allocate(56);ModernWeightProtocol.put(out,snapshot());assertEquals(56,out.position());
        assertEquals(GOLDEN,HexFormat.of().formatHex(out.array()));
        var in=ByteBuffer.wrap(HexFormat.of().parseHex(GOLDEN));assertEquals(snapshot(),ModernWeightProtocol.get(in));assertFalse(in.hasRemaining());
        var wide=weight(4_000_000_000L,8_000_000_000.5,List.of());out=ByteBuffer.allocate(40);ModernWeightProtocol.put(out,wide);
        assertEquals(wide,ModernWeightProtocol.get(ByteBuffer.wrap(out.array())));
    }
    @Test void optionalAndActorPrefixesAreExactlyEightBytesIncludingCanonicalBot() throws Exception {
        var out=ByteBuffer.allocate(8);ModernWeightProtocol.putOptional(out,null);assertArrayEquals(new byte[8],out.array());
        assertNull(ModernWeightProtocol.getOptional(ByteBuffer.wrap(out.array())));
        var bot=ByteBuffer.allocate(40);ModernWeightProtocol.put(bot,ModernInventoryWeight.notApplicable());assertArrayEquals(new byte[40],bot.array());
        var states=List.of(new ModernWeightProtocol.State(1,snapshot()),new ModernWeightProtocol.State(2,ModernInventoryWeight.notApplicable()));
        out=ByteBuffer.allocate(120);ModernWeightProtocol.putStates(out,states);assertEquals(120,out.position());
        assertEquals(states,ModernWeightProtocol.getStates(ByteBuffer.wrap(out.array()),2));
        final byte[] encoded=out.array();assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernWeightProtocol.getStates(ByteBuffer.wrap(encoded),1));
    }
    @Test void reservedPenaltyImpossibleTotalsAndNonfiniteCapacityCannotPassDecoder() {
        for(int offset:new int[]{34,35,38,39}) {
            var bytes=HexFormat.of().parseHex(GOLDEN);bytes[offset]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernWeightProtocol.get(ByteBuffer.wrap(bytes)));
        }
        for(long bits:new long[]{Long.MIN_VALUE,0x7ff0000000000000L,0x7ff8000000000000L,0}) {
            var bytes=HexFormat.of().parseHex(GOLDEN);ByteBuffer.wrap(bytes).putLong(24,bits);
            assertThrows(IllegalArgumentException.class,()->ModernWeightProtocol.get(ByteBuffer.wrap(bytes)));
        }
        var bytes=HexFormat.of().parseHex(GOLDEN);ByteBuffer.wrap(bytes).putLong(16,4);
        assertThrows(IllegalArgumentException.class,()->ModernWeightProtocol.get(ByteBuffer.wrap(bytes)));
    }
    @Test void equipmentAppendsAfterUnchangedV8BodyAndRejectsEveryTruncationOrOldVersion() throws Exception {
        var gear=equipment(List.of(item(1,ModernCharacterData.ItemKind.ARMOR,1,2000,false)));
        var basis=ModernInventoryWeight.qualify(gear,1);var weighted=gear.withWeight(ModernInventoryWeight.evaluate(basis,List.of()));
        var request=new ModernEquipmentAuthority.Request(1,3333,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO);
        var before=new ModernApplicationProtocol.EquipmentResult(8,request,new ModernEquipmentAuthority.Result(0,false,0,0,gear));
        var after=new ModernApplicationProtocol.EquipmentResult(8,request,new ModernEquipmentAuthority.Result(0,false,0,0,weighted));
        byte[] old=ModernApplicationProtocol.encode(before,8),current=ModernApplicationProtocol.encode(after,9);
        assertEquals(old.length+48,current.length);assertArrayEquals(Arrays.copyOfRange(old,8,old.length),Arrays.copyOfRange(current,8,old.length));
        assertEquals(after,ModernApplicationProtocol.decode(current,9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(after,8));
        for(int size=0;size<current.length;size++){var cut=Arrays.copyOf(current,size);if(size>=8)ByteBuffer.wrap(cut).putInt(4,size);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,9));}
        var none=ModernApplicationProtocol.encode(before,9);assertEquals(old.length+8,none.length);
        assertArrayEquals(new byte[8],Arrays.copyOfRange(none,old.length,none.length));
    }
    @Test void equipmentCannotClaimSuppliesOrWrongCarriedGear() {
        var gear=equipment(List.of(item(1,ModernCharacterData.ItemKind.ARMOR,1,2000,false)));
        assertThrows(IllegalArgumentException.class,()->gear.withWeight(snapshot()));
        assertThrows(IllegalArgumentException.class,()->gear.withWeight(weight(3,3150,List.of())));
        assertThrows(IllegalArgumentException.class,()->gear.withWeight(ModernInventoryWeight.notApplicable()));
    }
    @Test void shopUsesOwnedUnitFactsAndNotCurrentOfferWeights() throws Exception {
        var f=new ModernRoomsTest.Fixture();var p=f.player(7,3333,80,0);f.create(p);
        var rooms=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var summary=rooms.view().joined().summary();var echo=new ModernShopProtocol.Request(p.scope(),ModernShopAuthority.Action.READ,
                summary.id(),summary.revision(),0,0,ModernEquipmentAuthority.ZERO,0,0);
        // Current catalog may have changed; the owned stack retains its own unit weight.
        var offers=List.of(new ModernShopAuthority.Product(40010,0,40,40,"$26",80,45,9000,ModernShopCatalog.Support.HEALING,
                new ModernShopCatalog.Healing(10,20,189,2,500)));
        var owned=List.of(new ModernShopAuthority.Owned(40010,1,40));
        var snap=new ModernShopAuthority.Snapshot(1,0,100,60,ModernEquipmentAuthority.ZERO,offers,owned,snapshot());
        var message=new ModernApplicationProtocol.ShopResult(1,echo,new ModernShopProtocol.Reply(
                new ModernShopAuthority.Outcome(ModernShopAuthority.Status.OK,ModernShopAuthority.Reason.NONE,snap),rooms));
        byte[] encoded=ModernApplicationProtocol.encode(message,9);assertEquals(message,ModernApplicationProtocol.decode(encoded,9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,8));
        var denied=new ModernApplicationProtocol.ShopResult(1,echo,new ModernShopProtocol.Reply(
                new ModernShopAuthority.Outcome(ModernShopAuthority.Status.DENIED,ModernShopAuthority.Reason.TOO_HEAVY,snap),rooms));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(denied,9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(denied,8));
        assertThrows(IllegalArgumentException.class,()->ModernWeightProtocol.validateShop(snapshot(),List.of(new ModernShopAuthority.Owned(40010,2,40))));
        assertThrows(IllegalArgumentException.class,()->ModernWeightProtocol.validateShop(snapshot(),List.of(new ModernShopAuthority.Owned(40011,1,40))));
    }
    @Test void atomicCombatWeightMatchesEveryActorAndStockWithoutChangingV8() throws Exception {
        var old=ModernCombatProtocolTest.frame();
        var items=List.of(ModernItemProtocol.State.initial(1,0,List.of(new ModernItemProtocol.Stock(40010,1,1))));
        var weights=List.of(new ModernWeightProtocol.State(1,snapshot()));
        var frame=new ModernCombatProtocol.Frame(old.world(),old.vitals(),List.of(),items,weights);
        var message=new ModernApplicationProtocol.CombatResult(1,ModernCombatProtocolTest.read(),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
        byte[] encoded=ModernApplicationProtocol.encode(message,9);assertEquals(message,ModernApplicationProtocol.decode(encoded,9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,8));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(old.world(),old.vitals(),List.of(),items,
                List.of(new ModernWeightProtocol.State(1,weight(1,240,List.of(new ModernInventoryWeight.Carried(40010,2,2000)))))));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(old.world(),old.vitals(),List.of(),items,
                List.of(new ModernWeightProtocol.State(1,ModernInventoryWeight.notApplicable()))));
        for(int size=0;size<encoded.length;size++){var cut=Arrays.copyOf(encoded,size);if(size>=8)ByteBuffer.wrap(cut).putInt(4,size);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,9));}
    }
    @Test void newRejectionCodesAreNeverEncodedForOldPeers() {
        var message=new ModernApplicationProtocol.CombatResult(1,ModernCombatProtocolTest.read(),new ModernCombatProtocol.Reply(
                ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.TOO_HEAVY,ModernCombatProtocolTest.frame()));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,8));
    }

    @Test void combatTooHeavyRequiresAttackAndTheRequestingActorsConfirmedHeavyWeight() throws Exception {
        var scope=ModernCombatProtocolTest.read().scope();
        var attack=new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.ATTACK,9,6,1,2);
        var heavy=denialFrame(197,0,true,ModernWorldProtocol.Phase.ACTIVE,1);
        var valid=new ModernApplicationProtocol.CombatResult(1,attack,new ModernCombatProtocol.Reply(
                ModernCombatProtocol.Status.DENIED,ModernCombatProtocol.Reason.TOO_HEAVY,heavy));
        assertEquals(valid,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(valid,9),9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(valid,8));
        rejectHeavyCombat(ModernCombatProtocolTest.read(),heavy,ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,denialFrame(196,197,true,ModernWorldProtocol.Phase.ACTIVE,1),ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,denialFrame(197,0,false,ModernWorldProtocol.Phase.ACTIVE,1),ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,denialFrame(197,0,true,ModernWorldProtocol.Phase.LOADING,1),ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,denialFrame(197,0,true,ModernWorldProtocol.Phase.ACTIVE,0),ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,deadOwn(heavy),ModernCombatProtocol.Status.DENIED);
        rejectHeavyCombat(attack,heavy,ModernCombatProtocol.Status.BUSY);
    }

    @Test void castTooHeavyRequiresOwnActiveWeightWithoutInventingALearnedSkillGrant() throws Exception {
        var scope=ModernCombatProtocolTest.read().scope();
        var heavy=denialFrame(197,0,true,ModernWorldProtocol.Phase.ACTIVE,1);
        // Weight is checked before learned/template support, for both known
        // actions and other structurally valid source skill IDs.
        for(int skill:new int[]{4,5,6}) {
            var request=new ModernSpellProtocol.Request(scope,9,6,1,skill,skill==4?2:0);
            var valid=new ModernApplicationProtocol.CastResult(1,request,new ModernSpellProtocol.Reply(
                    ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.TOO_HEAVY,heavy));
            assertEquals(valid,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(valid,9),9));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(valid,8));
            rejectHeavyCast(request,denialFrame(196,197,true,ModernWorldProtocol.Phase.ACTIVE,1),ModernSpellProtocol.Status.DENIED);
            rejectHeavyCast(request,denialFrame(197,0,false,ModernWorldProtocol.Phase.ACTIVE,1),ModernSpellProtocol.Status.DENIED);
            rejectHeavyCast(request,denialFrame(197,0,true,ModernWorldProtocol.Phase.LOADING,1),ModernSpellProtocol.Status.DENIED);
            rejectHeavyCast(request,deadOwn(heavy),ModernSpellProtocol.Status.DENIED);
            rejectHeavyCast(request,heavy,ModernSpellProtocol.Status.BUSY);
        }
    }

    @Test void shopTooHeavyIsBuyOnlyAndUsesFreshBuyUnitRatherThanOwnedUnit() throws Exception {
        var fixture=new ShopFixture();
        // Existing unit=2000 gives total232; fresh unit=9000 projects242
        // (reject), whereas incorrectly reusing the owned unit projects235.
        var snapshot=shopSnapshot(230,9000,2000,true);
        var buy=fixture.request(ModernShopAuthority.Action.BUY,snapshot,40010);
        assertFalse(ModernInventoryWeight.permitsAdd(snapshot.weight().view(),1,9000));
        assertTrue(ModernInventoryWeight.permitsAdd(snapshot.weight().view(),1,2000));
        var valid=fixture.result(buy,snapshot,ModernShopAuthority.Reason.TOO_HEAVY);
        assertEquals(valid,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(valid,9),9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(valid,8));
        rejectHeavyShop(fixture,fixture.request(ModernShopAuthority.Action.READ,snapshot,0),snapshot);
        rejectHeavyShop(fixture,fixture.request(ModernShopAuthority.Action.SELL,snapshot,40010),snapshot);
        rejectHeavyShop(fixture,buy,shopSnapshot(230,9000,2000,false));
        rejectHeavyShop(fixture,fixture.request(ModernShopAuthority.Action.BUY,snapshot,40011),snapshot);
        var original=snapshot.offers().get(0);
        var unsupported=new ModernShopAuthority.Product(original.itemId(),original.sortOrder(),original.buyPrice(),original.sellPrice(),
                original.nameToken(),original.inventoryGraphic(),original.groundGraphic(),original.weight(),ModernShopCatalog.Support.UNIMPLEMENTED,null);
        rejectHeavyShop(fixture,buy,new ModernShopAuthority.Snapshot(snapshot.roundId(),snapshot.revision(),snapshot.allowance(),snapshot.balance(),
                snapshot.token(),List.of(unsupported),snapshot.stacks(),snapshot.weight()));
    }

    @Test void shopTooHeavyCannotForgeAdmissionFailureOrChangeItsCasReceipt() throws Exception {
        var fixture=new ShopFixture();
        // Inverse case: old unit9000 gives total231, but the current buyunit
        // is2000 and projects234. The old unit would incorrectly reject241.
        var light=shopSnapshot(222,2000,9000,true);
        assertTrue(ModernInventoryWeight.permitsAdd(light.weight().view(),1,2000));
        assertFalse(ModernInventoryWeight.permitsAdd(light.weight().view(),1,9000));
        rejectHeavyShop(fixture,fixture.request(ModernShopAuthority.Action.BUY,light,40010),light);
        var heavy=shopSnapshot(230,9000,2000,true);var echo=fixture.request(ModernShopAuthority.Action.BUY,heavy,40010);
        byte[] changedToken=new byte[32];changedToken[0]=1;
        for(int field=0;field<3;field++) {
            var stale=new ModernShopProtocol.Request(echo.scope(),echo.action(),echo.roomId(),echo.expectedRoomRevision(),
                    echo.roundId()+(field==0?1:0),echo.expectedRevision()+(field==1?1:0),
                    field==2?new ModernEquipmentOverrideStore.Fingerprint(changedToken):echo.token(),echo.itemId(),echo.quantity());
            rejectHeavyShop(fixture,stale,heavy);
        }
    }

    private static ModernCombatProtocol.Frame denialFrame(long ownWeight,long otherWeight,boolean includeWeight,
                                                           ModernWorldProtocol.Phase phase,long attackSequence) {
        var base=ModernWorldProtocolTest.frame();var first=base.room().members().get(0);
        var second=new ModernRooms.Member(new TournamentPresence.CharacterKey(42,3333),"M","Moon",80,0,0,1,true);
        var room=new ModernRooms.RoomSnapshot(new ModernRooms.Summary(5,6,new ModernRooms.Config("R",2,1,514,0),2,2,true),
                first.key(),List.of(first,second));
        var actors=List.of(new ModernBattleEngine.Actor(1,first,new ModernWorldMap.Cell(32724,32784),2,true,0,50_000_000),
                new ModernBattleEngine.Actor(2,second,new ModernWorldMap.Cell(32725,32784),6,true,0,50_000_000));
        var casts=List.of(new ModernSpellProtocol.CastState(1,1,0,0,0,0,0,0,0,0,1200),ModernSpellProtocol.CastState.initial(2,1200));
        var world=new ModernWorldProtocol.Frame(9,6,2,phase,ModernWorldProtocol.AbortReason.NONE,room,base.manifest(),actors,null,casts);
        var vitals=List.of(new ModernCombatProtocol.Vital(1,100,100,100,100,0,0,attackSequence,0,0,0,0,50_000_000),
                new ModernCombatProtocol.Vital(2,100,100,100,100,0,0,0,0,0,0,0,50_000_000));
        var items=List.of(ModernItemProtocol.State.initial(1,0,List.of()),ModernItemProtocol.State.initial(2,0,List.of()));
        var weights=includeWeight?List.of(new ModernWeightProtocol.State(1,weight(ownWeight,240,List.of())),
                new ModernWeightProtocol.State(2,weight(otherWeight,240,List.of()))):List.<ModernWeightProtocol.State>of();
        return new ModernCombatProtocol.Frame(world,vitals,List.of(),items,weights);
    }

    private static ModernCombatProtocol.Frame deadOwn(ModernCombatProtocol.Frame frame) {
        var own=frame.vitals().get(0);
        var dead=new ModernCombatProtocol.Vital(own.entityId(),0,own.maxHp(),own.mp(),own.maxMp(),own.ac(),own.weaponType(),
                own.attackSequence(),own.attackRevision(),frame.world().revision(),frame.world().revision(),own.lastTargetEntityId(),own.attackIntervalNanos());
        return new ModernCombatProtocol.Frame(frame.world(),List.of(dead,frame.vitals().get(1)),frame.damages(),frame.items(),frame.weights());
    }

    private static void rejectHeavyCombat(ModernCombatProtocol.Request request,ModernCombatProtocol.Frame frame,
                                           ModernCombatProtocol.Status status)throws Exception {
        var invalid=new ModernApplicationProtocol.CombatResult(1,request,new ModernCombatProtocol.Reply(status,ModernCombatProtocol.Reason.TOO_HEAVY,frame));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(invalid,9));
        var base=new ModernApplicationProtocol.CombatResult(1,request,new ModernCombatProtocol.Reply(status,ModernCombatProtocol.Reason.NOT_ACTIVE,frame));
        byte[] bytes=ModernApplicationProtocol.encode(base,9);bytes[16+ModernCombatProtocol.REQUEST_BYTES+1]=(byte)ModernCombatProtocol.Reason.TOO_HEAVY.ordinal();
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,9));
    }
    private static void rejectHeavyCast(ModernSpellProtocol.Request request,ModernCombatProtocol.Frame frame,
                                        ModernSpellProtocol.Status status)throws Exception {
        var invalid=new ModernApplicationProtocol.CastResult(1,request,new ModernSpellProtocol.Reply(status,ModernSpellProtocol.Reason.TOO_HEAVY,frame));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(invalid,9));
        var base=new ModernApplicationProtocol.CastResult(1,request,new ModernSpellProtocol.Reply(status,ModernSpellProtocol.Reason.NOT_ACTIVE,frame));
        byte[] bytes=ModernApplicationProtocol.encode(base,9);bytes[16+ModernSpellProtocol.CAST_REQUEST_BYTES+8+1]=(byte)ModernSpellProtocol.Reason.TOO_HEAVY.ordinal();
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,9));
    }
    private static ModernShopAuthority.Snapshot shopSnapshot(long gearWeight,long currentUnit,long oldUnit,boolean includeWeight) {
        var product=new ModernShopAuthority.Product(40010,0,40,40,"$26",80,45,currentUnit,ModernShopCatalog.Support.HEALING,
                new ModernShopCatalog.Healing(10,20,189,2,500));
        return new ModernShopAuthority.Snapshot(1,2,100,60,ModernEquipmentAuthority.ZERO,List.of(product),
                List.of(new ModernShopAuthority.Owned(40010,1,40)),includeWeight?weight(gearWeight,240,List.of(new ModernInventoryWeight.Carried(40010,1,oldUnit))):null);
    }
    private static void rejectHeavyShop(ShopFixture fixture,ModernShopProtocol.Request request,ModernShopAuthority.Snapshot snapshot)throws Exception {
        var invalid=fixture.result(request,snapshot,ModernShopAuthority.Reason.TOO_HEAVY);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(invalid,9));
        var base=fixture.result(request,snapshot,ModernShopAuthority.Reason.UNSUPPORTED_ITEM);
        byte[] bytes=ModernApplicationProtocol.encode(base,9);bytes[16+ModernShopProtocol.REQUEST_BYTES+1]=(byte)ModernShopAuthority.Reason.TOO_HEAVY.ordinal();
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,9));
    }
    private static final class ShopFixture {
        final ModernRoomsTest.Fixture fixture=new ModernRoomsTest.Fixture();
        final ModernRoomsTest.Player player=fixture.player(7,3333,80,0);
        final ModernRooms.Result rooms=fixture.create(player);
        ModernShopProtocol.Request request(ModernShopAuthority.Action action,ModernShopAuthority.Snapshot snapshot,long item) {
            var room=rooms.view().joined().summary();boolean read=action==ModernShopAuthority.Action.READ;
            return new ModernShopProtocol.Request(player.scope(),action,room.id(),room.revision(),read?0:snapshot.roundId(),
                    read?0:snapshot.revision(),snapshot.token(),read?0:item,read?0:1);
        }
        ModernApplicationProtocol.ShopResult result(ModernShopProtocol.Request request,ModernShopAuthority.Snapshot snapshot,ModernShopAuthority.Reason reason) {
            return new ModernApplicationProtocol.ShopResult(1,request,new ModernShopProtocol.Reply(
                    new ModernShopAuthority.Outcome(ModernShopAuthority.Status.DENIED,reason,snapshot),rooms));
        }
    }
}
