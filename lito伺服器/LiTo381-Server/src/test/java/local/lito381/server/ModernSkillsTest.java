package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernSkillsTest {
    @Test void appendedLightArrowSupportKeepsLegacyOrdinalsAndExactBaseCostNotEffectiveCost() {
        assertEquals(0,ModernSkills.Support.TELEPORT.ordinal());assertEquals(1,ModernSkills.Support.UNIMPLEMENTED.ordinal());
        assertEquals(2,ModernSkills.Support.UNSUPPORTED_TEMPLATE.ordinal());assertEquals(3,ModernSkills.Support.LIGHT_ARROW.ordinal());
        var arrow=new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW);
        var result=new ModernSkills.Snapshot(7,character(),List.of(arrow,teleport()));
        assertEquals(arrow,result.lightArrow().orElseThrow());assertTrue(result.teleport().isPresent());
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(5,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(4,"$1439",1,10,ModernSkills.Support.LIGHT_ARROW));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(4,"$1439",3,11,ModernSkills.Support.LIGHT_ARROW));
    }
    @Test void knownAttackWithUnsupportedTemplateOrLegacyUnimplementedNeverGrantsIt() {
        for(var support:List.of(ModernSkills.Support.UNIMPLEMENTED,ModernSkills.Support.UNSUPPORTED_TEMPLATE))
            assertTrue(new ModernSkills.Snapshot(7,character(),List.of(new ModernSkills.Skill(4,"$1439",3,10,support))).lightArrow().isEmpty());
    }
    static ModernApplicationProtocol.CharacterRow character() {
        return new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE,"$character",6,1,Long.MAX_VALUE);
    }
    static ModernSkills.Skill teleport() { return new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT); }
    @Test void immutableSortedSnapshotPreservesWideValuesAndQualifiedTeleport() {
        var list=new ArrayList<>(List.of(new ModernSkills.Skill(1,"$heal",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNIMPLEMENTED),teleport()));
        var result=new ModernSkills.Snapshot(42,character(),list);list.clear();
        assertEquals(2,result.skills().size());assertEquals(Long.MAX_VALUE,result.character().objectId());
        assertEquals(Long.MAX_VALUE,result.skills().get(0).mpCost());assertEquals(Long.MAX_VALUE,result.skills().get(0).reuseMillis());
        assertEquals(teleport(),result.teleport().orElseThrow());assertThrows(UnsupportedOperationException.class,()->result.skills().clear());
    }
    @Test void unsupportedTemplateAndEmptyLearnedListDoNotAuthorizeTeleport() {
        assertTrue(new ModernSkills.Snapshot(7,character(),List.of()).teleport().isEmpty());
        assertTrue(new ModernSkills.Snapshot(7,character(),List.of(new ModernSkills.Skill(5,"$1440",11,10,
                ModernSkills.Support.UNSUPPORTED_TEMPLATE))).teleport().isEmpty());
    }
    @Test void supportCannotFabricateDifferentSkillOrFixedTeleportCosts() {
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"name",10,10,ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(5,"name",11,10,ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(5,"name",10,11,ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(2,"name",1,1,ModernSkills.Support.UNSUPPORTED_TEMPLATE));
    }
    @Test void appendedBasicHealKeepsExactBaseCostsAndUnsupportedStateDoesNotGrantHealing() {
        assertEquals(4,ModernSkills.Support.BASIC_HEAL.ordinal());
        var heal=new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL);
        assertEquals(ModernSkills.Support.BASIC_HEAL,heal.support());
        var unsupported=new ModernSkills.Skill(1,"$1436",1,1,ModernSkills.Support.UNSUPPORTED_TEMPLATE);
        assertNotEquals(ModernSkills.Support.BASIC_HEAL,unsupported.support());
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(2,"$1436",4,10,ModernSkills.Support.BASIC_HEAL));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"$1436",3,10,ModernSkills.Support.BASIC_HEAL));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"$1436",4,11,ModernSkills.Support.BASIC_HEAL));
    }
    @Test void numericBoundsNullAndMalformedTextAreStrict() {
        for(int id:new int[]{-1,0,221,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,
                ()->new ModernSkills.Skill(id,"name",0,0,ModernSkills.Support.UNIMPLEMENTED));
        for(String name:new String[]{null,"","\u00a0","x\0y","\ud800","界".repeat(86)})
            assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,name,0,0,ModernSkills.Support.UNIMPLEMENTED));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"name",-1,0,ModernSkills.Support.UNIMPLEMENTED));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"name",0,-1,ModernSkills.Support.UNIMPLEMENTED));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(1,"name",0,0,null));
        assertEquals(256,new ModernSkills.Skill(1,"x".repeat(256),0,0,ModernSkills.Support.UNIMPLEMENTED).nameToken().length());
    }
    @Test void unorderedDuplicateAndOversizedCollectionsFailClosed() {
        var one=new ModernSkills.Skill(1,"one",0,0,ModernSkills.Support.UNIMPLEMENTED);
        for(var rows:List.of(List.of(teleport(),one),List.of(one,one),java.util.Collections.nCopies(221,one)))
            assertThrows(IllegalArgumentException.class,()->new ModernSkills.Snapshot(7,character(),rows));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Snapshot(0,character(),List.of()));
        assertThrows(NullPointerException.class,()->new ModernSkills.Snapshot(7,null,List.of()));
    }
    @Test void entire220EntryDomainListCanBeRepresentedWithoutNativeBitTruncation() {
        var rows=new ArrayList<ModernSkills.Skill>();
        for(int id=1;id<=220;id++)rows.add(id==5?teleport():new ModernSkills.Skill(id,"$"+id,0,0,ModernSkills.Support.UNIMPLEMENTED));
        assertEquals(220,new ModernSkills.Snapshot(7,character(),rows).skills().size());
    }
    @Test void diagnosticsDoNotPrintNamesOrSkillTokens() {
        assertFalse(teleport().toString().contains("$1440"));
        assertFalse(new ModernSkills.Snapshot(7,character(),List.of(teleport())).toString().contains("$character"));
    }
}
