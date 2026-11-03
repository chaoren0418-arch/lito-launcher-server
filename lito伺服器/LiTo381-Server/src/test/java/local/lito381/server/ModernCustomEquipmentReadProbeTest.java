package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ModernCustomEquipmentReadProbeTest {
    @Test void rawVipIdentityRemainsPresentAfterProductionExpiryGuardDisablesIt() {
        var equipped = vip(1,true,300086);
        var original = ModernEquipmentEvaluatorTest.qualified(7,0,List.of(equipped),
                ModernEquipmentEvaluatorTest.defaults(List.of(equipped),true),true,List.of(),List.of());
        var lifetime = new ModernItemLifetime.Observation(7,3333,"2026-09-30 00:00:00",1000,0,
                Map.of(1L,new ModernItemLifetime.Fact(1,300086,ModernCharacterData.ItemKind.ETC,true,
                        OptionalLong.of(0),true,OptionalLong.of(720),Optional.of("2026-09-29 00:00:00"),0,false)));
        var expired = new Qualified(original.source(),original.items(),original.catalog(),lifetime);
        assertEquals(0,ModernEquipmentEvaluator.evaluate(expired,Map.of(),1000).equippedRows());
        assertEquals(List.of(1L),ModernCustomEquipmentReadProbe.sqlEquippedVipObjects(expired.source()));
        assertTrue(expired.source().inventory().get(0).equipped());
    }

    @Test void rawVipInspectionExcludesUnwornAndOtherCatalogsAndSortsObjectIds() {
        var source = ModernEquipmentEvaluatorTest.snapshot(7,0,List.of(
                vip(9,true,300086),vip(2,false,300086),vip(3,true,100),vip(1,true,300086)));
        assertEquals(List.of(1L,9L),ModernCustomEquipmentReadProbe.sqlEquippedVipObjects(source));
    }

    private static Instance vip(long objectId,boolean equipped,long catalogId) {
        return ModernEquipmentEvaluatorTest.instance(objectId,catalogId,ModernCharacterData.ItemKind.ETC,equipped,0,
                "private-test-label",List.of(),Ability.zero(),Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),
                Stone.none(),TemplatePhysical.zero());
    }
}
