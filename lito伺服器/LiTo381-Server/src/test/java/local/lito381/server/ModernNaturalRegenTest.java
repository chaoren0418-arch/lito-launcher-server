package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNaturalRegenTest {
    static ModernNaturalRegen.Growth growth(int source,String database,long maximum) {
        var rows=new ArrayList<ModernNaturalRegen.ConRow>();for(int id=1;id<=50;id++)rows.add(new ModernNaturalRegen.ConRow(id,maximum));
        return new ModernNaturalRegen.Growth(source,database,rows);
    }
    static ModernPhysicalCombat.Source source(int id,ModernApplicationProtocol.CharacterRow row,long hp,long mp,long hpr,long mpr,long food) {
        var armor=ModernEquipmentEvaluatorTest.simpleArmor(2,2,ModernEquipmentEvaluatorTest.bonus(HP_REGEN,hpr,MP_REGEN,mpr));
        var old=ModernPhysicalCombatTest.source(id,row.characterClass(),row.level(),List.of(armor));var q=old.input().equipment();var p=q.source().persisted();
        var saved=new ModernCharacterData.PersistedValues(hp,mp,hp,mp,10,12,12,12,12,14,12,0,0,12,12,12,0,0,food);
        var snapshot=new ModernCharacterData.Snapshot(id,row,saved,q.source().inventory());
        var qualified=new Qualified(snapshot,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,Map.of()),old.growth(),old.templates(),Set.of(),
                ModernMagicCombatTest.growth(id),Map.of(),Map.of(),growth(id,q.catalog().resourceDatabase(),1));
    }
    @Test void completeSameSourceProfileIncludesActualSignedEquipmentAndFoodWithoutMeleePermissionDependency() {
        var row=new ModernApplicationProtocol.CharacterRow(0x100000001L,"fixture",1,0,80);var s=source(42,row,100,30,-4,-2,2);
        var fighter=ModernPhysicalCombatTest.fighter(s);var regen=fighter.regen();assertSame(fighter.profile(),regen.combat());
        assertTrue(regen.supported());assertEquals(-3,ModernNaturalRegen.hpAmount(regen.values(),1));assertEquals(-1,ModernNaturalRegen.mpAmount(regen.values()));
        assertEquals(2,regen.values().food());assertFalse(regen.values().fed());assertEquals(12,regen.values().originalConstitution());
        assertFalse(regen.toString().contains(row.name()));assertEquals(42,regen.combat().source().sourceId());
    }
    @Test void growthCannotBeMissingDuplicatedForeignOrZeroAndLegacyDoesNotPretendQualified() {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);var s=source(7,row,100,30,0,0,225);
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),s.magicGrowth(),Map.of(),Map.of(),growth(42,"catalog",1)));
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(),s.magicGrowth(),Map.of(),Map.of(),growth(7,"other",1)));
        assertNull(ModernPhysicalCombatTest.fighter(new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of())).regen());
        var values=new ArrayList<>(s.regenGrowth().constitution());values.remove(49);assertThrows(IllegalArgumentException.class,()->new ModernNaturalRegen.Growth(7,"catalog",values));
        assertThrows(IllegalArgumentException.class,()->new ModernNaturalRegen.ConRow(1,0));
    }
    @Test void unknownCustomOrProcCannotBeHiddenByAQualifiedGrowthTable() {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);var s=source(7,row,100,30,0,0,225);
        var custom=new ModernPhysicalCombat.Source(s.input(),s.growth(),Map.of(2L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ARMOR,"add.VIP",null)),
                Set.of(),s.magicGrowth(),Map.of(),Map.of(),s.regenGrowth());
        assertEquals(ModernNaturalRegen.UnsupportedReason.CUSTOM_EXECUTOR,ModernPhysicalCombatTest.fighter(custom).regen().unsupportedReason());
        var proc=new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),Set.of(2L),s.magicGrowth(),Map.of(),Map.of(),s.regenGrowth());
        assertEquals(ModernNaturalRegen.UnsupportedReason.EQUIPMENT_PROC,ModernPhysicalCombatTest.fighter(proc).regen().unsupportedReason());
    }
    @Test void mpCatchupRecordsLastNonzeroTickAtBothSaturationEdges() {
        var v=new ModernNaturalRegen.Values(80,1,12,12,14,0,225,1,0,2,100,108);
        var due=new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),0,4);
        var up=ModernNaturalRegen.resolve(v,due,50,100,b->{throw new AssertionError("no HPR roll");});
        assertEquals(108,up.mp());assertEquals(8,up.mpGain());assertEquals(106,up.mpBefore());assertEquals(108,up.mpAfter());
        var neg=new ModernNaturalRegen.Values(80,1,12,12,14,0,225,1,0,-4,100,108);
        var down=ModernNaturalRegen.resolve(neg,due,50,8,b->{throw new AssertionError();});
        assertEquals(0,down.mp());assertEquals(8,down.mpLoss());assertEquals(2,down.mpBefore());assertEquals(0,down.mpAfter());
    }
}
