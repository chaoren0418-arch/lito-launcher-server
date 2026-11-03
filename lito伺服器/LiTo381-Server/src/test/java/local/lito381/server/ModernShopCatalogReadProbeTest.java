package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernShopCatalogReadProbeTest {
    @Test void reportContainsPublicItemMetadataNotCanonicalOwnerOrExecutor() {
        String json=ModernShopCatalogReadProbe.sourceJson(7,4,List.of(ModernShopCatalogTest.offer(40010,0)));
        assertTrue(json.contains("\"source_id\":7"));assertTrue(json.contains("\"item_id\":40010"));
        assertTrue(json.contains("\"name_token\":\"$26\""));assertTrue(json.contains("\"effect\":189"));
        assertFalse(json.contains("UserAddHp"));assertFalse(json.contains("owner"));assertFalse(json.contains("password"));
    }
    @Test void displayOnlyNamesAreEscapedAndProbeValuesBounded() {
        var template=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.WEAPON,"name\"\\token",1,2,3,null);
        String json=ModernShopCatalogReadProbe.sourceJson(7,1,List.of(ModernShopCatalog.Offer.fromSource(1,0,1,1,template)));
        assertTrue(json.contains("name\\\"\\\\token"));assertTrue(json.contains("UNIMPLEMENTED"));assertFalse(json.contains("heal_min"));
        assertThrows(IllegalArgumentException.class,()->ModernShopCatalogReadProbe.sourceJson(0,1,List.of()));
        assertThrows(IllegalArgumentException.class,()->ModernShopCatalogReadProbe.sourceJson(7,65,List.of()));
    }
    @Test void qualifiedTimedReportShowsActualTupleWithoutExecutorOrAnEligibilityClaim() {
        String json=ModernShopCatalogReadProbe.sourceJson(7,4,List.of(ModernShopTimedTest.offer(ModernTimedEffects.Kind.WISDOM,0)));
        assertTrue(json.contains("\"support\":\"TIMED\""));assertTrue(json.contains("\"timed_kind\":\"WISDOM\""));
        assertTrue(json.contains("\"effect\":750"));assertTrue(json.contains("\"duration_ms\":300000"));
        assertTrue(json.contains("\"class_mask\":8"));assertFalse(json.contains("StatusWisdomPotion"));
        assertFalse(json.contains("eligible"));assertFalse(json.contains("heal_min"));assertFalse(json.contains("owner"));
    }
}
