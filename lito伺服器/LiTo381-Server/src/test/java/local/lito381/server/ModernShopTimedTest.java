package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;
import org.junit.jupiter.api.Test;

/** Source qualification, selection projection and v10 wire; no SQL or listener. */
class ModernShopTimedTest {
    static ModernShopCatalog.Template template(ModernTimedEffects.Kind kind) {
        String name,executor,material="glass";long inventory,ground,weight;
        switch(kind) {
            case HASTE -> {name="$234";inventory=77;ground=42;weight=3900;executor="brave.StatusHaste 300 191 true 40308 10";}
            case BRAVE -> {name="$943";inventory=79;ground=44;weight=4500;executor="brave.StatusBrave 300 751 true";}
            case ELF_BRAVE -> {name="$110";inventory=130;ground=81;weight=5000;material="vegetation";executor="brave.StatusElfBrave 480 751 true";}
            case WISDOM -> {name="$944";inventory=1529;ground=3836;weight=4500;executor="brave.StatusWisdomPotion 300 750";}
            default -> throw new AssertionError();
        }
        return new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,name,inventory,ground,weight,
                new ModernShopCatalog.Etc(executor,"potion","power",material,1,0,0,0,0,0,0,0,0,0,0,0,kind.classMask()));
    }
    static ModernShopCatalog.Template executor(ModernShopCatalog.Template t,String executor) {
        var e=t.etc();return new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),
                new ModernShopCatalog.Etc(executor,e.itemType(),e.useType(),e.material(),e.stackable(),e.maxChargeCount(),e.minLevel(),e.maxLevel(),
                        e.blessing(),e.trade(),e.cantDelete(),e.delayGroup(),e.delayMillis(),e.delayEffect(),e.foodVolume(),e.saveAtOnce(),e.limitClass()));
    }
    static ModernShopCatalog.Offer offer(ModernTimedEffects.Kind kind,int order) {
        return ModernShopCatalog.Offer.fromSource(kind.itemId(),order,40,20,template(kind));
    }
    static ModernApplicationProtocol.CharacterRow row(int type) {return new ModernApplicationProtocol.CharacterRow(3333,"fixture",type,0,80);}
    static ModernShopCatalog.Snapshot catalog(int type) {
        var list=new ArrayList<ModernShopCatalog.Offer>();
        for(long id:new long[]{40010,40011,40012})list.add(ModernShopCatalogTest.offer(id,list.size()));
        for(var kind:List.of(ModernTimedEffects.Kind.HASTE,ModernTimedEffects.Kind.BRAVE,ModernTimedEffects.Kind.WISDOM,ModernTimedEffects.Kind.ELF_BRAVE))
            list.add(offer(kind,list.size()));
        return new ModernShopCatalog.Snapshot(7,"sun",row(type),list);
    }
    static Snapshot read(ModernShopAuthority a,ModernShopCatalog.Snapshot c){return a.apply(Action.READ,0,0,null,0,0,c).snapshot();}
    static Outcome mutate(ModernShopAuthority a,ModernShopCatalog.Snapshot c,Snapshot s,Action action,long id,long qty) {
        return a.apply(action,s.roundId(),s.revision(),s.token(),id,qty,c);
    }
    static long deadline(){return System.nanoTime()+TimeUnit.SECONDS.toNanos(3);}
    @Test void fourExactSourceTuplesAndClassEligibilityAreQualifiedNotGuessed() {
        assertEquals(3,ModernShopCatalog.Support.TIMED.ordinal());
        for(var kind:ModernTimedEffects.Kind.values()) {
            var offer=offer(kind,0);assertEquals(ModernShopCatalog.Support.TIMED,offer.support());assertNull(offer.healing());
            assertEquals(kind,offer.timed().kind());assertEquals(template(kind),offer.template());
            for(int type=0;type<7;type++)assertEquals((kind.classMask()&(1<<type))!=0,offer.timed().eligible(type));
            assertFalse(offer.timed().eligible(-1));assertFalse(offer.timed().eligible(7));
            var t=offer.template();var huge=new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),Long.MAX_VALUE,t.etc());
            assertEquals(ModernShopCatalog.Support.TIMED,ModernShopCatalog.Offer.fromSource(kind.itemId(),0,1,1,huge).support());
        }
    }
    @Test void consumptionTrueCurrencyOperandsAreParsedButNotChargedAndNoFallbackQualifies() {
        var kind=ModernTimedEffects.Kind.HASTE;var t=template(kind);
        for(String text:List.of("brave.StatusHaste 300 191 true","brave.StatusHaste 300 191 TRUE 40308 10",
                "brave.StatusHaste 300 191 1 0 0","brave.StatusHaste +300 0191 true +2147483647 01"))
            assertEquals(ModernShopCatalog.Support.TIMED,ModernShopCatalog.Offer.fromSource(40013,0,1,1,executor(t,text)).support());
        for(String text:new String[]{null,"brave.StatusHaste","brave.StatusHaste 0 191 true","brave.StatusHaste 301 191 true",
                "brave.StatusHaste 300 190 true","brave.StatusHaste 300 191 false 40308 10","brave.StatusHaste 300 191 127",
                "brave.StatusHaste 300 191 maybe","brave.StatusHaste 300 191 true 40308","brave.StatusHaste 300 191 true 0 0 0",
                "brave.StatusHaste 300 191 true -1 0","brave.StatusHaste 300 191 true 2147483648 0",
                "brave.StatusHaste 300 191 true x 0","brave.StatusHaste  300 191 true","brave.StatusHaste 300 191 true "})
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(40013,0,1,1,executor(t,text)).support());
        var w=template(ModernTimedEffects.Kind.WISDOM);
        for(String text:List.of("brave.StatusWisdomPotion 300 750 true","brave.StatusWisdomPotion 300","brave.StatusWisdomPotion 0 750"))
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(40016,0,1,1,executor(w,text)).support());
    }
    @Test void templatesCannotForgeSupportOrLaunderClassAndGraphics() {
        for(var kind:ModernTimedEffects.Kind.values()) {
            var t=template(kind);var e=t.etc();
            var wrongClass=new ModernShopCatalog.Etc(e.executor(),e.itemType(),e.useType(),e.material(),1,0,0,0,0,0,0,0,0,0,0,0,0);
            for(var wrong:List.of(new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),wrongClass),
                    new ModernShopCatalog.Template(t.kind(),"$1",t.inventoryGraphic(),t.groundGraphic(),t.weight(),e),
                    new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic()+1,t.groundGraphic(),t.weight(),e),
                    new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ARMOR,t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),null)))
                assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(kind.itemId(),0,1,1,wrong).support());
            assertThrows(IllegalArgumentException.class,()->new ModernShopCatalog.Offer(kind.itemId(),0,1,1,t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),
                    ModernShopCatalog.Support.TIMED,null,t)); // old constructor cannot grant new support
        }
    }
    @Test void timedQualificationRejectsEveryChangedBehaviorFieldWithoutRepairingIt() {
        var t=template(ModernTimedEffects.Kind.HASTE);var e=t.etc();
        for(int field=0;field<13;field++) {
            long[] n={1,0,0,0,0,0,0,0,0,0,0,0,127};
            n[field]=field==0||field==12?0:1;
            var changed=new ModernShopCatalog.Etc(e.executor(),e.itemType(),e.useType(),e.material(),n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],n[11],n[12]);
            var altered=new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),changed);
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(40013,0,1,1,altered).support(),"field "+field);
        }
    }
    @Test void legacyProjectionUsesItsOwnStableTokenAndStillBuysHealing() {
        var c=catalog(1);var a=new ModernShopAuthority(20000,7,row(1),"sun");var before=read(a,c);
        assertNull(before.timed());assertNotEquals(c.token(),before.token());assertEquals(before,read(a,c));
        assertEquals(4,before.offers().stream().filter(p->p.support()==ModernShopCatalog.Support.UNIMPLEMENTED).count());
        assertEquals(3,before.offers().stream().filter(p->p.support()==ModernShopCatalog.Support.HEALING).count());
        assertEquals(Reason.CATALOG_CHANGED,a.apply(Action.BUY,before.roundId(),before.revision(),c.token(),40010,1,c).reason());
        assertEquals(Reason.UNSUPPORTED_ITEM,mutate(a,c,before,Action.BUY,40013,1).reason());
        var bought=mutate(a,c,before,Action.BUY,40010,1);assertEquals(Status.OK,bought.status());assertEquals(19960,bought.snapshot().balance());
        assertEquals(before.token(),bought.snapshot().token());assertEquals(ModernShopCatalog.Support.TIMED,c.offers().get(3).support());
    }
    @Test void explicitTimedScopeBuysAndSellsWrongClassWithoutPretendingUsable() {
        var c=catalog(1);var a=new ModernShopAuthority(20000,7,row(1),"sun",true);var before=read(a,c);
        assertEquals(c.token(),before.token());assertEquals(1,before.timed().ownerClass());
        assertFalse(before.offers().stream().filter(p->p.itemId()==40016).findFirst().orElseThrow().eligible());
        var bought=mutate(a,c,before,Action.BUY,40016,2);assertEquals(Status.OK,bought.status());assertEquals(19920,bought.snapshot().balance());
        var sold=mutate(a,c,bought.snapshot(),Action.SELL,40016,1);assertEquals(Status.OK,sold.status());assertEquals(19940,sold.snapshot().balance());
        var captured=a.capture(1,1,deadline(),c);assertEquals(1,captured.size());assertEquals(ModernTimedEffects.Kind.WISDOM,captured.get(0).offer().timed().kind());
    }
    @Test void sevenStacksCaptureOnlyAfterFreshOwnerAndFullTemplateRequalification() {
        var c=catalog(2);var a=new ModernShopAuthority(20000,7,row(2),"sun",true);var view=read(a,c);
        for(var offer:c.offers())view=mutate(a,c,view,Action.BUY,offer.itemId(),1).snapshot();
        var changed=new ArrayList<>(c.offers());var old=changed.get(3);var t=old.template();
        changed.set(3,ModernShopCatalog.Offer.fromSource(old.itemId(),old.sortOrder(),40,20,executor(t,"brave.StatusHaste 301 191 true")));
        var invalid=new ModernShopCatalog.Snapshot(7,"sun",row(2),changed);
        assertThrows(IllegalArgumentException.class,()->a.capture(1,1,deadline(),invalid));assertFalse(a.frozen());
        assertThrows(SecurityException.class,()->a.capture(1,1,deadline(),new ModernShopCatalog.Snapshot(42,"sun",row(2),c.offers())));
        assertEquals(7,a.capture(1,1,deadline(),c).size());assertEquals(Status.FROZEN,a.apply(Action.READ,0,0,null,0,0,c).status());
        a.terminal(1,1);assertEquals(20000,read(a,c).balance());assertTrue(read(a,c).stacks().isEmpty());
    }
    @Test void timedTemplateEvidenceAndOwnerClassEnterFingerprint() {
        var c=catalog(1);assertNotEquals(c.token(),catalog(2).token());
        var offers=new ArrayList<>(c.offers());var h=offers.get(3);offers.set(3,ModernShopCatalog.Offer.fromSource(h.itemId(),h.sortOrder(),40,20,
                executor(h.template(),"brave.StatusHaste 300 191 true 0 0")));
        assertNotEquals(c.token(),new ModernShopCatalog.Snapshot(7,"sun",row(1),offers).token());
    }
    @Test void timedBuyUsesUnchangedNativeWeightAdmissionAndTooHeavyWireReason() throws Exception {
        var c=catalog(1);var a=new ModernShopAuthority(20000,7,row(1),"sun",true);var basis=ModernShopWeightTest.basis(0,0,8);
        var before=a.apply(Action.READ,0,0,null,0,0,c,basis).snapshot();
        var denied=a.apply(Action.BUY,before.roundId(),before.revision(),before.token(),40013,2,c,basis);
        assertEquals(Reason.TOO_HEAVY,denied.reason());assertEquals(before,denied.snapshot());
        var base=reply(true,false);var echo=new ModernShopProtocol.Request(base.echo().scope(),Action.BUY,0,0,
                before.roundId(),before.revision(),before.token(),40013,2);
        var message=new ModernApplicationProtocol.ShopResult(1,echo,new ModernShopProtocol.Reply(denied,base.reply().rooms()));
        assertEquals(message,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message,10),10));
        var bought=a.apply(Action.BUY,before.roundId(),before.revision(),before.token(),40013,1,c,basis);
        assertEquals(Status.OK,bought.status());assertEquals(3,bought.snapshot().weight().view().suppliesWeight());
        assertEquals(List.of(new ModernInventoryWeight.Unit(40013,3900)),bought.snapshot().weight().units());
    }
    static ModernApplicationProtocol.ShopResult reply(boolean enabled,boolean empty) {
        var fixture=new ModernRoomsTest.Fixture();var player=fixture.player(7,3333,80,0);
        var rooms=fixture.rooms.execute(player.owner(),new ModernRooms.Command(player.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var c=empty?new ModernShopCatalog.Snapshot(7,"sun",row(1),List.of()):catalog(1);
        var a=new ModernShopAuthority(20000,7,row(1),"sun",enabled);
        var result=a.apply(Action.READ,0,0,null,0,0,c);
        var echo=new ModernShopProtocol.Request(player.scope(),Action.READ,0,0,0,0,new Fingerprint(new byte[32]),0,0);
        return new ModernApplicationProtocol.ShopResult(0xfedcba98,echo,new ModernShopProtocol.Reply(result,rooms));
    }
    static void rejects(byte[] bytes,int version){assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,version));}
    @Test void v10AppendixLiteralGoldenCarriesSortedExactTuplesAndOwnerEligibility() throws Exception {
        var msg=reply(true,false);byte[] bytes=ModernApplicationProtocol.encode(msg,10);
        String golden="0101000000040000"
                +"0000000000009c4d"+"00017f00"+"00000000000493e0"+"000000bf"
                +"0000000000009c4e"+"01017f00"+"00000000000493e0"+"000002ef"
                +"0000000000009c50"+"03000800"+"00000000000493e0"+"000002ee"
                +"0000000000009c84"+"02000400"+"0000000000075300"+"000002ef";
        assertEquals(golden,HexFormat.of().formatHex(Arrays.copyOfRange(bytes,bytes.length-104,bytes.length)));
        assertEquals(msg,ModernApplicationProtocol.decode(bytes,10));
        for(int version:new int[]{8,9}) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(msg,version));rejects(bytes,version);
        }
    }
    @Test void v10EnabledEmptyIsDistinctFromDisabledAndLegacyBytesStayExact() throws Exception {
        for(boolean enabled:new boolean[]{false,true}) {
            var msg=reply(enabled,true);byte[] bytes=ModernApplicationProtocol.encode(msg,10);
            assertEquals(enabled?"0101000000000000":"0000000000000000",HexFormat.of().formatHex(Arrays.copyOfRange(bytes,bytes.length-8,bytes.length)));
            assertEquals(msg,ModernApplicationProtocol.decode(bytes,10));
        }
        var old=reply(false,false);byte[] v9=ModernApplicationProtocol.encode(old,9),v10=ModernApplicationProtocol.encode(old,10);
        assertEquals(v9.length+8,v10.length);var common=Arrays.copyOf(v10,v9.length);ByteBuffer.wrap(common).putInt(4,v9.length);
        assertArrayEquals(v9,common);assertEquals(old,ModernApplicationProtocol.decode(v9,9));
        assertArrayEquals(HexFormat.of().parseHex(ModernShopProtocolTest.BUY_HEX),ModernApplicationProtocol.encode(
                new ModernApplicationProtocol.ShopRequest(0xfedcba98,ModernShopProtocolTest.golden()),8));
    }
    @Test void absentSnapshotFailuresStayExactly128BytesWithoutWeightOrTimedAppendicesInEveryShopVersion() throws Exception {
        var statuses=new Status[]{Status.BUSY,Status.UNAVAILABLE,Status.FROZEN};
        var reasons=new Reason[]{Reason.BUSY,Reason.BACKEND,Reason.FROZEN};
        // Same literal header/request body as the cross-language BUSY golden.
        // status + reason + absent snapshot + reserved; nothing follows it.
        var suffixes=new String[]{"030b0000","040a0000","05090000"};
        for(int i=0;i<statuses.length;i++) {
            var message=new ModernApplicationProtocol.ShopResult(0xfedcba98,ModernShopProtocolTest.golden(),
                    ModernShopProtocol.Reply.exceptional(statuses[i],reasons[i]));
            byte[] expected=HexFormat.of().parseHex("4c544d3100000080fedcba9800210000"
                    +ModernShopProtocolTest.BODY+suffixes[i]);
            assertEquals(128,expected.length);
            assertNull(message.reply().outcome().snapshot());assertNull(message.reply().rooms());
            for(int version:new int[]{8,9,10}) {
                byte[] encoded=ModernApplicationProtocol.encode(message,version);
                assertEquals(128,encoded.length);
                assertArrayEquals(expected,encoded);
                assertEquals(message,ModernApplicationProtocol.decode(expected,version));
                for(int extra:new int[]{8,16}) {
                    byte[] trailing=Arrays.copyOf(expected,expected.length+extra);
                    // Keep the header length consistent: rejection must come from unused body bytes.
                    ByteBuffer.wrap(trailing).putInt(4,trailing.length);
                    rejects(trailing,version);
                }
            }
        }
    }
    @Test void everyAppendixTruncationTailReservedTupleAndFalseEligibilityIsRejected() throws Exception {
        byte[] valid=ModernApplicationProtocol.encode(reply(true,false),10);int start=valid.length-104;
        for(int n=start;n<valid.length;n++){byte[] b=Arrays.copyOf(valid,n);ByteBuffer.wrap(b).putInt(4,n);rejects(b,10);}
        byte[] tail=Arrays.copyOf(valid,valid.length+1);ByteBuffer.wrap(tail).putInt(4,tail.length);rejects(tail,10);
        for(int at:new int[]{start+2,start+3,start+6,start+7,start+8+11}){byte[] b=valid.clone();b[at]=1;rejects(b,10);}
        for(int[] mutation:new int[][]{{0,2},{1,7},{5,5},{8+7,0},{8+8,4},{8+9,2},{8+9,0},{8+10,4},{8+23,190}}) {
            byte[] b=valid.clone();b[start+mutation[0]]=(byte)mutation[1];rejects(b,10);
        }
        byte[] b=valid.clone();ByteBuffer.wrap(b).putLong(start+8+12,300001);rejects(b,10);
        b=valid.clone();System.arraycopy(b,start+8,b,start+32,24);rejects(b,10);
        b=valid.clone();b[start]=0;rejects(b,10);
    }
}
