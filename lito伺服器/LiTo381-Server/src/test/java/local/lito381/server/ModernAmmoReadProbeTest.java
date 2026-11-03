package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernAmmoReadProbeTest {
    @Test void qualifiedReportRetainsSourceArrowWidthAndConsumesNoSourceInventory() {
        var base=ModernCombatTestPeer.arrow(40744,0,20).template();
        var template=new ModernShopCatalog.Template(base.kind(),base.nameToken(),base.inventoryGraphic(),base.groundGraphic(),base.weight(),
                base.etc(),new ModernShopCatalog.Arrow(4_294_967_301L,4_294_967_302L,-1,1));
        String json=ModernAmmoReadProbe.sourceJson(7,4,List.of(ModernShopCatalog.Offer.fromSource(40744,0,20,20,template)));
        assertTrue(json.contains("AMMO"));assertTrue(json.contains("4294967301"));assertTrue(json.contains("4294967302"));
        assertTrue(json.contains("arrow_sprite"));assertTrue(json.contains("consume"));assertFalse(json.contains("password"));
        assertFalse(json.contains("owner"));assertFalse(json.contains("executor"));assertFalse(json.contains("object_id"));
    }
    @Test void reportDoesNotPromoteUnimplementedOfferOrExposeSourceExecutor() {
        var template=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,"public",1,2,3,null);
        String json=ModernAmmoReadProbe.sourceJson(7,1,List.of(ModernShopCatalog.Offer.fromSource(1,0,1,1,template)));
        assertTrue(json.contains("UNIMPLEMENTED"));assertFalse(json.contains("arrow_small"));
        assertFalse(json.contains("password"));assertFalse(json.contains("owner"));assertFalse(json.contains("executor"));
        assertThrows(IllegalArgumentException.class,()->ModernAmmoReadProbe.sourceJson(0,1,List.of()));
        assertThrows(IllegalArgumentException.class,()->ModernAmmoReadProbe.sourceJson(7,65,List.of()));
    }
}
