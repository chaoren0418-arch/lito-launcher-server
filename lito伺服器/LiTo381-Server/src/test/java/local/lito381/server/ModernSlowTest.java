package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModernSlowTest {
    @Test void rawSelectedSqlColumnOrderNotTheOlderHandwrittenModelOwnsTheContract() throws Exception {
        String sql = Files.readString(Path.of("../1-伺服器/db/380_db.sql"));
        int table = sql.indexOf("CREATE TABLE `skills`  (");
        String definition = sql.substring(table, sql.indexOf(") ENGINE", table));
        var columns = new ArrayList<String>();
        var fields = Pattern.compile("(?m)^  `([^`]+)`").matcher(definition);
        while (fields.find()) columns.add(fields.group(1));
        var row = Pattern.compile("(?m)^INSERT INTO `skills` VALUES \\((29, .*?)\\);").matcher(sql);
        assertTrue(row.find());
        String[] values = row.group(1).split(", ", -1);
        assertEquals(columns.size(), values.length);
        var actual = new HashMap<String,String>();
        for (int i = 0; i < columns.size(); i++) actual.put(columns.get(i), values[i]);
        assertEquals("-1", actual.get("ranged"));
        assertEquals("8", actual.get("attr"));
        assertEquals("277", actual.get("sysmsgID_happen"));
        assertEquals("0", actual.get("sysmsgID_stop"));
        assertEquals(ModernSlow.SOURCE_RANGE, Integer.parseInt(actual.get("ranged")));
        assertEquals(ModernSlow.HAPPEN_MESSAGE, Integer.parseInt(actual.get("sysmsgID_happen")));
        assertEquals(ModernSlow.STOP_MESSAGE, Integer.parseInt(actual.get("sysmsgID_stop")));
        assertEquals(ModernSlow.MATERIAL_ID, Long.parseLong(actual.get("itemConsumeId")));
        assertEquals(ModernSlow.MATERIAL_COUNT, Integer.parseInt(actual.get("itemConsumeCount")));
        assertEquals(ModernSlow.REUSE_MILLIS, Integer.parseInt(actual.get("reuseDelay")));
        assertEquals(ModernSlow.DURATION_SECONDS, Integer.parseInt(actual.get("buffDuration")));
        assertEquals(ModernSlow.ACTION, Integer.parseInt(actual.get("action_id")));
        assertEquals(ModernSlow.EFFECT, Integer.parseInt(actual.get("castgfx")));
        String use = Files.readString(Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("this._skill.getRanged() != -1"));
        assertTrue(use.contains("this._user.getLocation().isInScreen(this._target.getLocation())"));
        // Disagreement is explicit; this test must not bless a handwritten row
        // simply because another handwritten test uses the same wrong fields.
        assertNotEquals(BattleSpellCatalog.entangleSpell(29).maximumRange(), ModernSlow.SOURCE_RANGE);
        assertNotEquals(BattleSpellCatalog.entangleSpell(29).stopMessage(), ModernSlow.STOP_MESSAGE);
    }

    @Test void nativeCostsAndBinary64ProbabilityOrderRemainExactWithWideSourceInputs() {
        var nativeSpell = BattleSpellCatalog.entangleSpell(29);
        for (int intelligence = 1; intelligence <= 254; intelligence++) {
            for (int original = 0; original <= 2; original++)
                assertEquals(nativeSpell.effectiveMpCost(intelligence, original), ModernSlow.effectiveMpCost(intelligence, original));
            for (int level : new int[]{79,80,81}) for (int mr = 0; mr <= 300; mr++)
                for (boolean boss : new boolean[]{false,true})
                    assertEquals(nativeSpell.probabilityForTarget(level,80,intelligence,7,2,mr,boss),
                            ModernSlow.probability(level,80,intelligence,2,mr,boss));
        }
        // Do not silently replace the selected floating operation order by a
        // rational formula: this specific native boundary floors 61, not 62.
        assertEquals(61, ModernSlow.probability(80,80,57,0,2,false));
        assertEquals(12, ModernSlow.probability(80,80,57,0,2,true));
        for (long intelligence : new long[]{15,254,255,65536,5_000_000_000L,Long.MAX_VALUE}) {
            assertEquals(17, ModernSlow.effectiveMpCost(intelligence,0));
            assertEquals(15, ModernSlow.effectiveMpCost(intelligence,2));
        }
        assertEquals(90,ModernSlow.probability(Long.MAX_VALUE,80,5_000_000_000L,0,0,false));
        assertEquals(18,ModernSlow.probability(80,Long.MAX_VALUE,5_000_000_000L,0,0,true));
        assertEquals(1,ModernSlow.probability(80,80,15,0,Long.MAX_VALUE,false));
        assertEquals(90,ModernSlow.probability(80,80,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,false));
        for(long bad:new long[]{Long.MIN_VALUE,-1,0}) assertThrows(IllegalArgumentException.class,()->ModernSlow.effectiveMpCost(bad,0));
        for(long bad:new long[]{-1,3,Long.MAX_VALUE}) assertThrows(IllegalArgumentException.class,()->ModernSlow.effectiveMpCost(15,bad));
        assertThrows(IllegalArgumentException.class,()->ModernSlow.probability(0,80,15,0,0,false));
        assertThrows(IllegalArgumentException.class,()->ModernSlow.probability(80,0,15,0,0,false));
        assertThrows(IllegalArgumentException.class,()->ModernSlow.probability(80,80,-1,0,0,false));
        assertEquals(48,ModernSlow.probability(80,80,15,-1,0,false));
        assertThrows(IllegalArgumentException.class,()->ModernSlow.probability(80,80,15,0,-1,false));
    }

    @Test void signedMagicHitUsesTheRetainedSumAndNeverNarrowsWidePenalties() throws Exception {
        String pc=Files.readString(Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertTrue(pc.contains("return _magicHit + _setAbility.get(com.lineage.server.model.item.L1ItemAbility.AbilityType.MAGIC_HIT);"));
        String magic=Files.readString(Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"));
        assertTrue(magic.contains("_pc.getMagicHit()"));
        for(long hit:new long[]{Long.MIN_VALUE,-5_000_000_000L,-90,-1,0,1,5_000_000_000L,Long.MAX_VALUE})
            for(long mr:new long[]{0,100,Long.MAX_VALUE})for(long intelligence:new long[]{0,15,Long.MAX_VALUE})
                for(boolean boss:new boolean[]{false,true}) {
                    double nativeOrder=45;
                    nativeOrder+=intelligence*0.3d;nativeOrder+=hit;nativeOrder-=mr*0.05d;
                    int expected=(int)Math.max(1,Math.min(100,nativeOrder));
                    expected=Math.max(1,Math.min(90,expected));
                    if(boss)expected=Math.max(1,expected/5);
                    assertEquals(expected,ModernSlow.probability(80,80,intelligence,hit,mr,boss));
                }
        assertEquals(1,ModernSlow.probability(80,80,15,Long.MIN_VALUE,0,false));
        assertEquals(90,ModernSlow.probability(80,80,15,Long.MAX_VALUE,0,false));
    }

    @Test void sourceEquipmentProofDoesNotBorrowHasteHelmetDiscountOrGrantCast() {
        for (int source : new int[]{7,42}) for (long helm : new long[]{20008,20013,20023,20024}) {
            var fighter = ModernPhysicalCombatTest.fighter(ModernHasteTest.source(source,1,5_000_000_000L,helm));
            var slow = ModernSlow.qualify(fighter);
            assertEquals(fighter.key(),slow.key()); assertEquals(0,slow.equipmentRevision());
            assertEquals(17,slow.mpCost()); assertFalse(slow.toString().contains("3333"));
        }
        assertThrows(IllegalArgumentException.class,()->ModernSlow.qualify(ModernPhysicalCombatTest.fighter(
                ModernPhysicalCombatTest.source(7,1,80,List.of()))));
        var key = new TournamentPresence.CharacterKey(7,3333);
        assertThrows(IllegalArgumentException.class,()->new ModernSlow.Profile(key,-1,20));
        for(int cost:new int[]{0,14,21,Integer.MAX_VALUE}) assertThrows(IllegalArgumentException.class,()->new ModernSlow.Profile(key,0,cost));
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,new ModernSkills.Skill(29,"$1455",20,500,ModernSkills.Support.UNIMPLEMENTED).support());
    }

    @Test void screenRangeKeepsSelectedMinusOneSeparateFromNativeClickRangeAndLos() {
        var from = new ModernWorldMap.Cell(0,0);
        assertTrue(ModernSlow.inSourceRange(from,new ModernWorldMap.Cell(9,0)));
        assertTrue(ModernSlow.inSourceRange(from,new ModernWorldMap.Cell(10,8)));
        assertFalse(ModernSlow.inSourceRange(from,new ModernWorldMap.Cell(20,0)));
        assertFalse(ModernSlow.inSourceRange(new ModernWorldMap.Cell(Integer.MIN_VALUE,0),new ModernWorldMap.Cell(Integer.MAX_VALUE,0)));
        assertEquals(10,ModernSlow.CLIENT_CLICK_RANGE);
    }
}
