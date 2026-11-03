package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;
import org.junit.jupiter.api.Test;

class ModernShopCatalogTest {
    static ModernShopCatalog.Template template(long id) {
        long min=id==40010?10:id==40011?30:60,max=id==40010?20:id==40011?50:80;
        int effect=id==40010?189:id==40011?194:197;
        return new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,id==40010?"$26":id==40011?"$235":"$238",
                id==40010?80:id==40011?78:81,id==40010?45:id==40011?43:46,id==40010?3900:id==40011?4900:5850,
                new ModernShopCatalog.Etc("hp.UserAddHp "+min+" "+max+" "+effect+" true"+(id==40012?"":" 0 0"),
                        "potion","hpr","glass",1,0,0,0,0,0,0,2,id==40010?500:1000,0,0,0,127));
    }
    static ModernShopCatalog.Offer offer(long id,int order) {return ModernShopCatalog.Offer.fromSource(id,order,40,20,template(id));}
    private static ModernApplicationProtocol.CharacterRow character() {return new ModernApplicationProtocol.CharacterRow(3333,"角色",1,0,80);}
    private static ModernShopCatalog.Snapshot snapshot(List<ModernShopCatalog.Offer> offers) {return new ModernShopCatalog.Snapshot(7,"owner",character(),offers);}
    private static ModernShopCatalog.Template edit(ModernShopCatalog.Template t,ModernShopCatalog.Etc e) {
        return new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),e);
    }
    private static ModernShopCatalog.Etc executor(ModernShopCatalog.Etc e,String value) {
        return new ModernShopCatalog.Etc(value,e.itemType(),e.useType(),e.material(),e.stackable(),e.maxChargeCount(),e.minLevel(),
                e.maxLevel(),e.blessing(),e.trade(),e.cantDelete(),e.delayGroup(),e.delayMillis(),e.delayEffect(),e.foodVolume(),e.saveAtOnce(),e.limitClass());
    }
    @Test void threeExactHealingTuplesRetainRealNamesGraphicsAndDelay() {
        for(long id:new long[]{40010,40011,40012}) {
            var offer=offer(id,0);assertEquals(ModernShopCatalog.Support.HEALING,offer.support());assertNotNull(offer.healing());
            assertSame(offer.evidence(),offer.template());assertEquals(id==40010?500:1000,offer.healing().delayMillis());
            assertEquals(2,offer.healing().delayGroup());assertEquals(template(id),offer.template());
        }
    }
    @Test void onlyCompleteEquivalentConsumptionSyntaxQualifiesWithoutParserDefaults() {
        var t=template(40010);
        for(String value:List.of("hp.UserAddHp 10 20 189 true","hp.UserAddHp 10 20 189 TRUE 0 0","hp.UserAddHp 10 20 189 1"))
            assertEquals(ModernShopCatalog.Support.HEALING,ModernShopCatalog.Offer.fromSource(40010,0,1,1,edit(t,executor(t.etc(),value))).support());
        for(String value:new String[]{null,"0","hp.UserAddHp","hp.UserAddHp 10 20 189 false","hp.UserAddHp 10 20 189 true 40308 10",
                "hp.UserAddHp 10 20 189 true 0","hp.UserAddHp 10 20 189 true 0 0 0","hp.UserAddHp 10 21 189 true","hp.UserAddHp 10 20 190 true",
                "hp.UserAddHp 10 20 189 maybe","other.UserAddHp 10 20 189 true"," hp.UserAddHp 10 20 189 true"})
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(40010,0,1,1,edit(t,executor(t.etc(),value))).support());
    }
    @Test void itemIdIsNeverSufficientAndUnsupportedGoodsRemainDisplayOnly() {
        var t=template(40010);
        for(var changed:List.of(new ModernShopCatalog.Template(t.kind(),"$27",80,45,3900,t.etc()),
                new ModernShopCatalog.Template(t.kind(),"$26",81,45,3900,t.etc()),
                new ModernShopCatalog.Template(t.kind(),"$26",80,46,3900,t.etc()),
                new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,"$26",80,45,3900,null))) {
            var offer=ModernShopCatalog.Offer.fromSource(40010,0,1,1,changed);
            assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,offer.support());assertNull(offer.healing());
        }
        var unsupported=ModernShopCatalog.Offer.fromSource(999999,0,Long.MAX_VALUE,Long.MAX_VALUE,t);
        assertEquals(ModernShopCatalog.Support.UNIMPLEMENTED,unsupported.support());assertNull(unsupported.healing());
        assertThrows(IllegalArgumentException.class,()->new ModernShopCatalog.Offer(999999,0,1,1,t.nameToken(),80,45,3900,
                ModernShopCatalog.Support.HEALING,offer(40010,0).healing(),t));
    }
    @Test void fingerprintCoversIdentityCharacterEveryOfferAndRawTemplateEvidence() {
        var a=offer(40010,0);var original=snapshot(List.of(a));
        assertEquals(original.token(),snapshot(List.of(offer(40010,0))).token());
        assertNotEquals(original.token(),new ModernShopCatalog.Snapshot(42,"owner",character(),List.of(a)).token());
        assertNotEquals(original.token(),new ModernShopCatalog.Snapshot(7,"alias",character(),List.of(a)).token());
        assertNotEquals(original.token(),new ModernShopCatalog.Snapshot(7,"owner",new ModernApplicationProtocol.CharacterRow(3333,"角色",1,0,81),List.of(a)).token());
        for(var changed:List.of(ModernShopCatalog.Offer.fromSource(40010,1,40,20,a.template()),
                ModernShopCatalog.Offer.fromSource(40010,0,41,20,a.template()),ModernShopCatalog.Offer.fromSource(40010,0,40,21,a.template()),
                ModernShopCatalog.Offer.fromSource(40010,0,40,20,new ModernShopCatalog.Template(a.template().kind(),"$26",80,45,3901,a.template().etc())),
                ModernShopCatalog.Offer.fromSource(40010,0,40,20,edit(a.template(),executor(a.template().etc(),"hp.UserAddHp 10 20 189 true")))))
            assertNotEquals(original.token(),snapshot(List.of(changed)).token());
        byte[] bytes=original.token().bytes();bytes[0]^=1;assertNotEquals(new Fingerprint(bytes),original.token());
        assertThrows(IllegalArgumentException.class,()->new ModernShopCatalog.Snapshot(7,"owner",character(),new Fingerprint(bytes),List.of(a)));
        assertFalse(original.toString().contains("owner"));
    }
    @Test void immutableOrderingDuplicateAndMaximumBoundCannotBeBypassed() {
        var mutable=new ArrayList<>(List.of(offer(40010,0)));var value=snapshot(mutable);mutable.clear();assertEquals(1,value.offers().size());
        assertThrows(UnsupportedOperationException.class,()->value.offers().clear());
        assertThrows(IllegalArgumentException.class,()->snapshot(List.of(offer(40010,0),offer(40010,1))));
        assertThrows(IllegalArgumentException.class,()->snapshot(List.of(offer(40010,0),offer(40011,0))));
        assertThrows(IllegalArgumentException.class,()->snapshot(List.of(offer(40011,1),offer(40010,0))));
        var offers=new ArrayList<ModernShopCatalog.Offer>();for(int i=0;i<256;i++)offers.add(ModernShopCatalog.Offer.fromSource(100000+i,i,1,1,template(40010)));
        assertEquals(256,snapshot(offers).offers().size());offers.add(ModernShopCatalog.Offer.fromSource(100256,256,1,1,template(40010)));
        assertThrows(IllegalArgumentException.class,()->snapshot(offers));
    }
    @Test void strictUnicodeAndExactI64ValuesDoNotClampOrNormalize() {
        for(String value:new String[]{null,"","\u00a0","\u2007","\u202f","\ud800","x\0y","x".repeat(257)})
            assertThrows(IllegalArgumentException.class,()->new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,value,0,0,0,null));
        var t=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,"plain",Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,null);
        var o=ModernShopCatalog.Offer.fromSource(Long.MAX_VALUE,65535,Long.MAX_VALUE,Long.MAX_VALUE,t);
        assertEquals(Long.MAX_VALUE,snapshot(List.of(o)).offers().get(0).inventoryGraphic());
        assertEquals(65535,o.sortOrder());
        for(int invalid:new int[]{-1,65536,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->ModernShopCatalog.Offer.fromSource(1,invalid,1,1,t));
        assertThrows(IllegalArgumentException.class,()->ModernShopCatalog.Offer.fromSource(1,0,0,1,t));
        assertThrows(IllegalArgumentException.class,()->new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,"x",1,1,1,null));
    }
}
