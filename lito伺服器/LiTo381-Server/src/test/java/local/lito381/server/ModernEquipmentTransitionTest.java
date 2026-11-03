package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernCombatProfileTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModernEquipmentTransitionTest {
    @Test void maximaIncreaseDoesNotHealAndRemovingThenReequippingDoesNotRecoverClippedLoss() {
        var before = new ModernEquipmentVitals.Vitals(70, 100, 30, 50);
        var up = ModernEquipmentVitals.retain(before, 1000, 900);
        assertEquals(new ModernEquipmentVitals.Vitals(70, 1000, 30, 900), up);
        var down = ModernEquipmentVitals.retain(up, 10, 2);
        assertEquals(new ModernEquipmentVitals.Vitals(10, 10, 2, 2), down);
        assertEquals(new ModernEquipmentVitals.Vitals(10, 1000, 2, 900), ModernEquipmentVitals.retain(down, 1000, 900));
        assertEquals(new ModernEquipmentVitals.Vitals(70, 100, 30, 50), before);
    }

    @Test void zeroAndI64LimitsNeverNarrowDivideOverflowOrResurrect() {
        var dead = new ModernEquipmentVitals.Vitals(0, Long.MAX_VALUE, 0, Long.MAX_VALUE);
        assertEquals(new ModernEquipmentVitals.Vitals(0, 1, 0, 0), ModernEquipmentVitals.retain(dead, 1, 0));
        assertEquals(dead, ModernEquipmentVitals.retain(dead, Long.MAX_VALUE, Long.MAX_VALUE));
        var full = new ModernEquipmentVitals.Vitals(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(full, ModernEquipmentVitals.retain(full, Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(new ModernEquipmentVitals.Vitals(1, 1, 0, 0), ModernEquipmentVitals.retain(full, 1, 0));
    }

    @Test void invalidLiveOrNewRangeFailsBeforeAReplacementExists() {
        for (long[] row : invalidRows()) assertThrows(IllegalArgumentException.class, () -> apply(row));
    }

    @Test void sameSourceNextRevisionUsesLiveVitalsAndQualifiedAcAndAbilitiesNotSqlSavedVitals() {
        var q = withVitals(qualified(List.of(simpleArmor(1, 1, bonus(MAX_HP, 900, MAX_MP, 700, AC, -7, STR, 4)))),
                100, 50, 99999, 88888, -80);
        var before = profile(q, Optional.empty());
        var candidate = profile(q, stored(q, 1, Map.of(1L, false)));
        var after = candidate.retainMatchVitals(before, 333, 123);
        assertEquals(ModernCombatProfile.VitalPolicy.RETAINED_MATCH, after.vitalPolicy());
        assertEquals(100, after.effective().initialHp());
        assertEquals(50, after.effective().initialMp());
        assertEquals(-73, after.effective().armorClass());
        assertEquals(200, after.effective().strength());
        assertEquals(candidate.equipped(), after.equipped());
        assertSame(before.source(), after.source());
        assertEquals(1000, before.effective().initialHp());
        assertEquals(750, before.effective().initialMp());
        var reequipped = profile(q, stored(q, 2, Map.of(1L, true))).retainMatchVitals(after, 100, 50);
        assertEquals(1000, reequipped.effective().maximumHp());
        assertEquals(100, reequipped.effective().initialHp());
        assertEquals(50, reequipped.effective().initialMp());
        var dead = candidate.retainMatchVitals(before, 0, 0);
        assertEquals(0, dead.effective().initialHp());
    }

    @Test void candidateRejectsForeignSourceChangedSqlBackwardClockAndNonSuccessorRevision() {
        var items = List.of(simpleArmor(1, 1, bonus(MAX_HP, 10)));
        var q = qualified(items);
        var before = profile(q, Optional.empty());
        for (long revision : new long[] {0, 2, Long.MAX_VALUE}) {
            var stored = revision == 0 ? Optional.<ModernEquipmentOverrideStore.Stored>empty() : stored(q, revision, Map.of(1L, false));
            var candidate = profile(q, stored);
            assertThrows(IllegalArgumentException.class, () -> candidate.retainMatchVitals(before, 1, 0));
        }
        var overflowPrior = profile(q, stored(q, Long.MAX_VALUE, Map.of(1L, true)));
        assertThrows(IllegalArgumentException.class, () -> before.retainMatchVitals(overflowPrior, 1, 0));
        var foreign = qualified(42, 0, items, defaults(items, false), false, List.of(), List.of());
        var foreignCandidate = profile(foreign, stored(foreign, 1, Map.of(1L, false)));
        assertThrows(IllegalArgumentException.class, () -> foreignCandidate.retainMatchVitals(before, 1, 0));
        var changed = withVitals(q, 200, 50, 1, 0, -900);
        var changedCandidate = profile(changed, stored(changed, 1, Map.of(1L, false)));
        assertThrows(IllegalArgumentException.class, () -> changedCandidate.retainMatchVitals(before, 1, 0));
        var rewind = ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q), binding(q),
                stored(q, 1, Map.of(1L, false)), 999, ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertThrows(IllegalArgumentException.class, () -> rewind.retainMatchVitals(before, 1, 0));
        var valid = profile(q, stored(q, 1, Map.of(1L, false)));
        for (long hp : new long[] {-1, 111, Long.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> valid.retainMatchVitals(before, hp, 0));
        assertThrows(IllegalArgumentException.class, () -> valid.retainMatchVitals(before, 1, 1));
    }

    @Test void retainedMatchCannotBeSynthesizedFromSavedSqlAndSavedPolicyIsNotLiveAuthority() {
        var q = withVitals(qualified(List.of()), 100, 50, 10, 5, -80);
        assertThrows(IllegalArgumentException.class, () -> ModernCombatProfile.qualify(
                ModernCombatProfile.Input.unqualified(q), binding(q), Optional.empty(), 1000,
                ModernCombatProfile.VitalPolicy.RETAINED_MATCH));
        var saved = ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q), binding(q), Optional.empty(),
                1000, ModernCombatProfile.VitalPolicy.RETAINED_SAVED);
        var candidate = profile(q, stored(q, 1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> candidate.retainMatchVitals(saved, 10, 5));
    }

    @Test void physicalCandidateRecomputesActualDamageAndRetainsLiveHpInsteadOfFullNewMatch() {
        var weapon = ModernPhysicalCombatTest.weapon(1, 1, true, 6, bonus(MELEE_DAMAGE, 20), TemplatePhysical.zero());
        var source = ModernPhysicalCombatTest.source(7, 0, 30, List.of(weapon));
        var before = ModernPhysicalCombatTest.fighter(source);
        var q = source.input().equipment();
        var after = ModernPhysicalCombat.qualifyEquipmentChange(source, binding(q), stored(q, 1, Map.of(1L, false)),
                1, before, 23, 7);
        assertEquals(23, after.profile().effective().initialHp());
        assertEquals(7, after.profile().effective().initialMp());
        assertEquals(0, after.profile().weapon().objectId());
        assertEquals(0, after.offense().itemDamage());
        assertEquals(20, before.offense().itemDamage());
        assertEquals(100, before.profile().effective().initialHp());
        var opponent = ModernPhysicalCombatTest.fighter(ModernPhysicalCombatTest.source(42, 0, 30, List.of()));
        var oldHit = ModernPhysicalCombat.resolve(before, opponent, 100, bound -> bound - 1);
        var newHit = ModernPhysicalCombat.resolve(after, opponent, 100, bound -> bound - 1);
        assertTrue(oldHit.rawDamage() > newHit.rawDamage());
        assertEquals(ModernCombatProfile.VitalPolicy.RETAINED_MATCH, after.profile().vitalPolicy());
        // The legacy public world-entry path must not use this candidate as a new spawn.
        var owner = new TournamentPresence().connect();
        var scope = new ModernRooms.Scope(7, q.source().character().objectId(), 1, 0);
        assertDoesNotThrow(() -> new ModernBattleEngine.PreparedMember(owner, scope,
                q.source().character(), new ModernWorldMap.Cell(1, 1), 200_000_000, before, 1_000_000_000));
        assertThrows(IllegalArgumentException.class, () -> new ModernBattleEngine.PreparedMember(
                owner, scope,
                q.source().character(), new ModernWorldMap.Cell(1, 1), 200_000_000, after, 1_000_000_000));
    }

    @Test void allQualifiedDerivedProfilesUseTheSameReplacementNotStaleMagicOrRegen() {
        var armor = simpleArmor(1, 1, bonus(MAX_HP, 90, MAX_MP, 70, SP, 3, MR, 11, HP_REGEN, 4, MP_REGEN, 5));
        var plain = ModernPhysicalCombatTest.source(7, 1, 30, List.of(armor));
        var magic = ModernMagicCombatTest.enable(plain);
        var source = new ModernPhysicalCombat.Source(magic.input(),magic.growth(),magic.templates(),magic.procCatalogIds(),
                magic.magicGrowth(), Map.of(), Map.of(), ModernNaturalRegenTest.growth(7,"catalog",1),
                ModernBattleEngineHonorTest.healing(7,"catalog",0));
        var before = ModernPhysicalCombatTest.fighter(source);
        var q = source.input().equipment();
        var after = ModernPhysicalCombat.qualifyEquipmentChange(source,binding(q),stored(q,1,Map.of(1L,false)),1,before,23,7);
        assertNotNull(after.magic()); assertNotNull(after.regen()); assertNotNull(after.ranged()); assertNotNull(after.healing());
        assertTrue(after.magic().supported()); assertTrue(after.regen().supported()); assertTrue(after.healing().supported());
        assertSame(after.profile(), after.magic().profile());
        assertSame(after.profile(), after.regen().combat());
        assertEquals(0,after.magic().offense().itemSpellPower()); assertEquals(3,before.magic().offense().itemSpellPower());
        assertEquals(0,after.magic().defense().equipmentMagicResistance()); assertEquals(11,before.magic().defense().equipmentMagicResistance());
        assertEquals(0,after.regen().values().equipmentHpRegen()); assertEquals(4,before.regen().values().equipmentHpRegen());
        assertEquals(0,after.regen().values().equipmentMpRegen()); assertEquals(5,before.regen().values().equipmentMpRegen());
        assertEquals(100,after.magic().values().maximumHp()); assertEquals(100,after.regen().values().maximumHp());
        assertEquals(23,after.profile().effective().initialHp()); assertEquals(7,after.profile().effective().initialMp());
    }

    @Test void unsupportedSourceExecutorsDoNotBecomeQualifiedByUnequippingTheirVisibleItem() {
        var armor = simpleArmor(1,1,Ability.zero());
        var source = ModernPhysicalCombatTest.source(7,0,30,List.of(armor));
        var custom = new ModernPhysicalCombat.Source(source.input(),source.growth(),
                Map.of(1L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ARMOR,"Custom",null)),source.procCatalogIds());
        var before = ModernPhysicalCombatTest.fighter(custom); var q=custom.input().equipment();
        var after = ModernPhysicalCombat.qualifyEquipmentChange(custom,binding(q),stored(q,1,Map.of(1L,false)),1,before,23,7);
        assertFalse(before.supported()); assertFalse(after.supported());
        assertEquals(ModernPhysicalCombat.UnsupportedReason.CUSTOM_EXECUTOR,after.unsupportedReason());
    }

    @Test void fixedAndGeneratedWideCorpusMatchesRustContract() throws Exception {
        var rows = new ArrayList<long[]>();
        rows.add(new long[] {70,100,30,50,1000,900});
        rows.add(new long[] {70,100,30,50,10,2});
        rows.add(new long[] {0,1,0,0,Long.MAX_VALUE,Long.MAX_VALUE});
        rows.add(new long[] {Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,1,0});
        rows.add(new long[] {9007199254740993L,Long.MAX_VALUE,9007199254740993L,Long.MAX_VALUE,9007199254740992L,Long.MAX_VALUE});
        rows.addAll(invalidRows());
        long rng = 0x123456789abcdef0L;
        for (int i = 0; i < 512; i++) {
            long[] values = new long[4];
            for (int j=0;j<4;j++) { rng ^= rng << 13; rng ^= rng >>> 7; rng ^= rng << 17; values[j] = rng & Long.MAX_VALUE; }
            rows.add(new long[] {values[0],Long.MAX_VALUE,values[1],Long.MAX_VALUE,Math.max(1,values[2]),values[3]});
        }
        var digest = MessageDigest.getInstance("SHA-256");
        for (var row : rows) {
            for (long value : row) digest.update(ByteBuffer.allocate(8).putLong(value).array());
            try {
                var result = apply(row); digest.update((byte)1);
                for (long value : new long[] {result.hp(),result.maximumHp(),result.mp(),result.maximumMp()})
                    digest.update(ByteBuffer.allocate(8).putLong(value).array());
            } catch (IllegalArgumentException invalid) { digest.update((byte)0); }
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        assertEquals(526, rows.size());
        assertEquals("ab1351a9d238c53914bfd9673220c812a94e21460b4a6cab6e3c3649d01eface",hash);
        System.out.println("equipment-vitals-v1:" + hash + ":" + rows.size());
    }

    private static ModernEquipmentVitals.Vitals apply(long[] row) {
        return ModernEquipmentVitals.retain(new ModernEquipmentVitals.Vitals(row[0],row[1],row[2],row[3]),row[4],row[5]);
    }
    private static List<long[]> invalidRows() {
        return List.of(new long[] {-1,100,0,0,100,0},new long[] {101,100,0,0,100,0},new long[] {0,0,0,0,1,0},
                new long[] {1,100,-1,10,100,10},new long[] {1,100,11,10,100,10},new long[] {1,100,0,-1,100,10},
                new long[] {1,100,0,0,0,0},new long[] {1,100,0,0,-1,0},new long[] {1,100,0,0,1,-1});
    }
}
