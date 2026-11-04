package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModernSpellMaterialsTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    static ModernShopCatalog.Template template(long weight) {
        return new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,"$508",235,773,weight,
                new ModernShopCatalog.Etc("0","material","none","gemstone",1,0,0,0,0,0,0,0,0,0,0,0,127));
    }
    static ModernShopCatalog.Offer offer(long weight) {return ModernShopCatalog.Offer.fromSource(40318,1,100,50,template(weight));}
    static ModernInventoryWeight.Basis basis(int source,long gear,double maximum) {
        var s=ModernInventoryWeightTest.source(List.of());
        return new ModernInventoryWeight.Basis(new ModernCharacterData.Snapshot(source,s.character(),s.persisted(),s.inventory()),0,gear,maximum,1,-1);
    }
    static ModernShopCatalog.Snapshot catalog(int source,long weight) {
        return new ModernShopCatalog.Snapshot(source,"owner",basis(source,0,100).source().character(),List.of(ModernShopAuthorityTest.offer(40,20),offer(weight)));
    }
    static ModernShopAuthority shop(boolean materials) {return new ModernShopAuthority(2000,7,catalog(7,2000).character(),"owner",true,false,materials);}
    static Snapshot read(ModernShopAuthority a,ModernShopCatalog.Snapshot c,ModernInventoryWeight.Basis b) {return a.apply(Action.READ,0,0,null,0,0,c,b).snapshot();}
    static Outcome buy(ModernShopAuthority a,Snapshot v,long id,long count,ModernShopCatalog.Snapshot c,ModernInventoryWeight.Basis b) {
        return a.apply(Action.BUY,v.roundId(),v.revision(),v.token(),id,count,c,b);
    }
    static ModernSpellMaterials.State state(long quantity) {
        return ModernSpellMaterials.State.initial(new ModernTargetHistory.World(1,2,3),new ModernRooms.Scope(7,3333,9,0),
                new ModernTargetHistory.Participant(1,ModernParticipantId.human(7,3333)),List.of(new ModernShopAuthority.Stack(offer(2000),quantity)));
    }
    static ModernSlow.Profile profile(int source,long gear) {return new ModernSlow.Profile(new TournamentPresence.CharacterKey(source,3333),gear,15);}
    static ModernSlowCatalog.Snapshot learned(int source) {return new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(source,3333),ModernSlowCatalog.Status.QUALIFIED);}

    @Test void selectedRawEtcColumnOrderQualifiesWithoutGuessingResourceIds() throws Exception {
        String sql=Files.readString(Path.of("../1-伺服器/db/380_db.sql")),q=Character.toString(96);
        int start=sql.indexOf("CREATE TABLE "+q+"etcitem"+q+"  (");
        String definition=sql.substring(start,sql.indexOf(") ENGINE",start));
        var columns=new ArrayList<String>();var fields=Pattern.compile("(?m)^  "+q+"([^"+q+"]+)"+q).matcher(definition);
        while(fields.find())columns.add(fields.group(1));
        var row=Pattern.compile("(?m)^INSERT INTO "+q+"etcitem"+q+" VALUES \\((NULL, 40318, .*?)\\);").matcher(sql);
        assertTrue(row.find());String[] values=row.group(1).split(", ",-1);assertEquals(columns.size(),values.length);
        var actual=new HashMap<String,String>();for(int i=0;i<values.length;i++)actual.put(columns.get(i),values[i]);
        assertEquals("'0'",actual.get("classname"));assertEquals("'$508'",actual.get("name_id"));
        assertEquals("'material'",actual.get("item_type"));assertEquals("'none'",actual.get("use_type"));assertEquals("'gemstone'",actual.get("material"));
        assertEquals("2000",actual.get("weight"));assertEquals("235",actual.get("invgfx"));assertEquals("773",actual.get("grdgfx"));
        assertEquals("1",actual.get("stackable"));assertEquals("127",actual.get("limit_class"));
        for(String field:List.of("max_charge_count","min_lvl","max_lvl","bless","trade","cant_delete","delay_id","delay_time","delay_effect","food_volume","save_at_once"))
            assertEquals("0",actual.get(field),field);
        assertEquals(new ModernSpellMaterials.Facts(40318,2000),offer(2000).material());assertEquals(ModernShopCatalog.Support.MATERIAL,offer(2000).support());
    }
    @Test void everyEtcSemanticFieldAndResourceIdentityIsCheckedButWeightStaysWide() throws Exception {
        var good=template(2000);var e=good.etc();var components=ModernShopCatalog.Etc.class.getRecordComponents();
        var types=Arrays.stream(components).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);
        var ctor=ModernShopCatalog.Etc.class.getDeclaredConstructor(types);
        for(int i=0;i<components.length;i++) {
            Object[] args=new Object[components.length];for(int n=0;n<args.length;n++)args[n]=components[n].getAccessor().invoke(e);
            if(args[i] instanceof String)args[i]="other";else args[i]=((Long)args[i])==0?1L:0L;
            var changed=(ModernShopCatalog.Etc)ctor.newInstance(args);
            var t=new ModernShopCatalog.Template(good.kind(),good.nameToken(),235,773,2000,changed);
            assertNull(ModernSpellMaterials.qualify(40318,t),components[i].getName());
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(40318,1,100,50,t).support());
        }
        for(var t:List.of(new ModernShopCatalog.Template(good.kind(),"$509",235,773,2000,e),
                new ModernShopCatalog.Template(good.kind(),"$508",236,773,2000,e),new ModernShopCatalog.Template(good.kind(),"$508",235,774,2000,e),
                new ModernShopCatalog.Template(good.kind(),"$508",235,773,2000,e,new ModernShopCatalog.Arrow(1,1,66,1)),
                new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,"$508",235,773,2000,null)))assertNull(ModernSpellMaterials.qualify(40318,t));
        for(long id:new long[]{0,40319,0x100009d7eL})assertNull(ModernSpellMaterials.qualify(id,good));
        for(long weight:new long[]{0,1,5_000_000_000L,Long.MAX_VALUE})assertEquals(weight,offer(weight).material().unitWeight());
    }
    @Test void explicitInternalShopBuysSellsFreezesAndResetsWithoutGrantingLegacyMaterialUse() {
        var c=catalog(7,2000);var b=basis(7,1,1000);
        try(var legacy=shop(false);var a=shop(true)) {
            var old=read(legacy,c,b);var v=read(a,c,b);
            assertEquals(ModernShopCatalog.Support.UNIMPLEMENTED,old.offers().get(1).support());assertNotEquals(old.token(),v.token());
            assertEquals(Reason.UNSUPPORTED_ITEM,buy(legacy,old,40318,1,c,b).reason());assertEquals(Reason.CATALOG_CHANGED,buy(a,old,40318,1,c,b).reason());
            var bought=buy(a,v,40318,3,c,b).snapshot();assertEquals(1700,bought.balance());assertEquals(6,bought.weight().view().suppliesWeight());
            assertEquals(Reason.STALE_REVISION,buy(a,v,40318,3,c,b).reason());
            var sold=a.apply(Action.SELL,bought.roundId(),bought.revision(),bought.token(),40318,1,c,b).snapshot();
            assertEquals(1750,sold.balance());assertEquals(2,sold.stacks().get(0).quantity());
            assertThrows(SecurityException.class,()->read(a,catalog(42,2000),basis(42,1,1000)));assertThrows(IllegalArgumentException.class,()->read(a,c,null));
            var descriptor=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,
                    ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT|ModernMatchDescriptor.TIMED,1000,1000,0,10000);
            assertThrows(IllegalArgumentException.class,()->a.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,descriptor));
            assertFalse(a.frozen());
            var captured=a.capture(2,3,ModernShopAuthorityTest.deadline(),c,b);
            assertEquals(2,captured.get(0).quantity());assertEquals(Status.FROZEN,buy(a,sold,40318,1,c,b).status());
            a.terminal(2,3);var next=read(a,c,b);assertEquals(2000,next.balance());assertTrue(next.stacks().isEmpty());assertEquals(2,captured.get(0).quantity());
        }
        assertThrows(IllegalArgumentException.class,()->new ModernItemProtocol.Stock(40318,1,1));
    }
    @Test void descriptorCaptureRequiresMaterialOptInAndCannotFreezeLegacyOrWrongSourceShops() {
        var c=catalog(7,2000);var b=basis(7,1,1000);
        int common=ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.ITEMS
                |ModernMatchDescriptor.WEIGHT|ModernMatchDescriptor.TIMED;
        var speed=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,common|ModernMatchDescriptor.SLOW,1000,1000,0,10000);
        var old=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,common,1000,1000,0,10000);
        try(var legacy=shop(false);var enabled=shop(true)) {
            assertThrows(IllegalArgumentException.class,()->legacy.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,speed));
            assertFalse(legacy.frozen());
            var initial=read(enabled,c,b);
            var bought=buy(enabled,initial,40318,2,c,b).snapshot();
            assertThrows(IllegalArgumentException.class,()->enabled.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,old));
            assertThrows(SecurityException.class,()->enabled.capture(2,3,ModernShopAuthorityTest.deadline(),catalog(42,2000),basis(42,1,1000),speed));
            assertFalse(enabled.frozen());assertEquals(bought,read(enabled,c,b));
            var captured=enabled.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,speed);
            assertTrue(enabled.frozen());assertEquals(2,captured.get(0).quantity());assertNotNull(captured.get(0).offer().material());
            assertEquals(captured,enabled.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,speed));
            enabled.terminal(2,3);assertFalse(enabled.frozen());assertTrue(read(enabled,c,b).stacks().isEmpty());
        }
    }
    @Test void debitCandidateConservesStockChecksLearnedSourceAndRejectsReplayWithoutMutatingOldState() {
        var initial=state(2);
        assertThrows(IllegalArgumentException.class,()->initial.slow(1,1,1,profile(42,0),learned(42)));
        assertThrows(IllegalArgumentException.class,()->initial.slow(1,1,1,profile(7,0),new ModernSlowCatalog.Snapshot(profile(7,0).key(),ModernSlowCatalog.Status.UNLEARNED)));
        var miss=initial.slow(3,2,1,profile(7,0),learned(7));var hit=miss.slow(4,3,2,profile(7,1),learned(7));
        assertEquals(2,initial.stocks().get(0).remaining());assertEquals(1,miss.stocks().get(0).remaining());
        assertFalse(hit.hasSlowMaterial());assertEquals(2,hit.uses());assertEquals(3,hit.lastSequence());
        assertThrows(IllegalArgumentException.class,()->miss.slow(4,2,1,profile(7,0),learned(7)));
        assertThrows(IllegalArgumentException.class,()->miss.slow(3,3,1,profile(7,0),learned(7)));
        assertThrows(IllegalArgumentException.class,()->hit.slow(5,4,2,profile(7,1),learned(7)));
        var fresh=state(3).slow(2,1,2,profile(7,2),learned(7));
        assertThrows(IllegalArgumentException.class,()->fresh.slow(3,2,1,profile(7,2),learned(7)));assertThrows(IllegalArgumentException.class,()->fresh.slow(3,2,2,profile(7,1),learned(7)));
        assertThrows(IllegalArgumentException.class,()->new ModernSpellMaterials.State(initial.world(),initial.scope(),initial.caster(),0,0,0,0,0,miss.stocks()));
        assertThrows(IllegalArgumentException.class,()->ModernSpellMaterials.State.initial(initial.world(),initial.scope(),new ModernTargetHistory.Participant(1,ModernParticipantId.bot(7,3333)),List.of()));
        assertThrows(IllegalArgumentException.class,()->ModernSpellMaterials.State.initial(initial.world(),initial.scope(),initial.caster(),List.of(new ModernShopAuthority.Stack(offer(2000),1),new ModernShopAuthority.Stack(offer(2000),1))));
    }
    @Test void materialsReweightAlongsidePotionsAndArrowsAndRetainZeroStockUnitEvidence() {
        var b=basis(7,1,1000);var m=state(1);var items=List.of(new ModernItemProtocol.Stock(40010,2,1));
        var ammo=List.of(new ModernAmmo.Stock(ModernAmmoTest.facts(40744),3,2));
        var units=List.of(new ModernInventoryWeight.Unit(40010,3900),new ModernInventoryWeight.Unit(40318,2000),new ModernInventoryWeight.Unit(40744,500));
        assertEquals(7,m.reweight(b,items,ammo,units).view().totalWeight());
        var after=m.slow(1,1,1,profile(7,0),learned(7)).reweight(b,items,ammo,units);assertEquals(5,after.view().totalWeight());assertEquals(units,after.units());
        assertThrows(IllegalArgumentException.class,()->ModernInventoryWeight.world(b,items,ammo,units));
        assertThrows(IllegalArgumentException.class,()->m.reweight(basis(42,1,1000),items,ammo,units));
        assertThrows(IllegalArgumentException.class,()->m.reweight(b,items,ammo,List.of(units.get(0),new ModernInventoryWeight.Unit(40318,1),units.get(2))));
        var huge=new ModernSpellMaterials.Stock(new ModernSpellMaterials.Facts(40318,Long.MAX_VALUE),1000,1000);
        assertEquals(Long.MAX_VALUE,ModernInventoryWeight.world(basis(7,0,Double.MAX_VALUE),List.of(),List.of(),List.of(huge),List.of(new ModernInventoryWeight.Unit(40318,Long.MAX_VALUE))).view().totalWeight());
        assertThrows(ArithmeticException.class,()->ModernInventoryWeight.world(b,List.of(),List.of(),List.of(new ModernSpellMaterials.Stock(huge.facts(),1001,1001)),List.of(new ModernInventoryWeight.Unit(40318,Long.MAX_VALUE))));
    }
    @Test void weightAdmissionAndTemplateChangeRejectBeforeInventoryMutation() {
        var c=catalog(7,2000);var b=basis(7,0,10);
        try(var a=shop(true)) {
            var v=read(a,c,b);assertEquals(Reason.TOO_HEAVY,buy(a,v,40318,5,c,b).reason());assertEquals(v,read(a,c,b));
            var bought=buy(a,v,40318,2,c,b).snapshot();var changed=catalog(7,3000);var newer=read(a,changed,b);
            assertEquals(4,newer.weight().view().suppliesWeight());assertEquals(bought.balance(),newer.balance());
            assertEquals(Reason.CATALOG_CHANGED,buy(a,newer,40318,1,changed,b).reason());
            assertThrows(IllegalArgumentException.class,()->a.capture(2,3,ModernShopAuthorityTest.deadline(),changed,b));assertFalse(a.frozen());assertEquals(newer,read(a,changed,b));
        }
    }
    @Test void pre47WireVersionsRejectMaterialsButRoundTripLegacyProjection() throws Exception {
        var f=new ModernRoomsTest.Fixture();var p=f.player(7,3333,80,0);
        var rooms=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var request=new ModernShopProtocol.Request(p.scope(),Action.READ,0,0,0,0,new ModernEquipmentOverrideStore.Fingerprint(new byte[32]),0,0);
        var c=catalog(7,2000);var b=basis(7,0,1000);
        try(var a=shop(true);var legacy=new ModernShopAuthority(2000,7,c.character(),"owner")) {
            var internalMessage=new ModernApplicationProtocol.ShopResult(1,request,new ModernShopProtocol.Reply(a.apply(Action.READ,0,0,null,0,0,c,b),rooms));
            for(int version=8;version<47;version++) {
                var oldMessage=new ModernApplicationProtocol.ShopResult(1,request,new ModernShopProtocol.Reply(legacy.apply(Action.READ,0,0,null,0,0,c,version<9?null:b),rooms));
                final int v=version;assertThrows(RuntimeException.class,()->ModernShopProtocol.validate(internalMessage,v));
                byte[] wire=ModernApplicationProtocol.encode(oldMessage,v);assertEquals(oldMessage,ModernApplicationProtocol.decode(wire,v));
                byte[] marker=ByteBuffer.allocate(12).putLong(40318).putShort((short)1).put((byte)ModernShopCatalog.Support.UNIMPLEMENTED.ordinal()).put((byte)0).array();
                int offset=-1;for(int i=0;i<=wire.length-marker.length;i++)if(Arrays.equals(marker,Arrays.copyOfRange(wire,i,i+marker.length))){assertEquals(-1,offset);offset=i;}
                assertTrue(offset>0);wire[offset+10]=(byte)ModernShopCatalog.Support.MATERIAL.ordinal();assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(wire,v));
            }
        }
    }
}
