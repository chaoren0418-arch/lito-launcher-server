package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

final class AccountRepositoryTest {
    @Test void isolatedCastleWarFixtureCannotChangeTheDefaultSchedule() {
        MemoryAccountRepository ordinary =
                new MemoryAccountRepository("test", "test");
        MemoryAccountRepository fixture = new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52, 50, 110,
                false, false, 30, 1, 2);

        CastleWarSchedule normal = ordinary.castleWarSchedules().get(4);
        CastleWarSchedule shortFixture = fixture.castleWarSchedules().get(4);
        assertEquals(1_787_971_500_000L, normal.warTimeMillis());
        assertEquals(60, normal.warDurationMinutes());
        assertEquals(10_080, normal.warIntervalMinutes());
        assertTrue(shortFixture.warTimeMillis() > System.currentTimeMillis());
        assertEquals(1, shortFixture.warDurationMinutes());
        assertEquals(2, shortFixture.warIntervalMinutes());
    }

    @Test void isolatedPrimaryMaxHpFixtureDoesNotChangeTheDefaultProfile() {
        MemoryAccountRepository ordinary =
                new MemoryAccountRepository("test", "test");
        MemoryAccountRepository fixture = new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52, 100, 110,
                false, false);

        assertEquals(50, ordinary.characters("test17").get(0).maxHp());
        assertEquals(100, fixture.characters("test17").get(0).maxHp());
        assertEquals(100, fixture.characters("test2").get(0).maxHp());
    }

    @Test void memoryProfileUsesCatalogReuseFallback() throws Exception {
        MemoryAccountRepository repository =
                new MemoryAccountRepository("test", "test");
        assertTrue(repository.skillReuseDelays().isEmpty());
    }

    @Test void optInMemorySourceInventoryUsesExactUntradableQuestRow() {
        MemoryAccountRepository ordinary = new MemoryAccountRepository("test", "test");
        assertTrue(ordinary.sourceInventory(3).isEmpty());

        MemoryAccountRepository fixture = new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52, 110, true);
        SourceInventoryItem claw = fixture.sourceInventory(3).get(0);
        assertEquals(0x0F00_4059L, claw.sourceObjectId());
        assertEquals(40590, claw.catalogId());
        assertEquals("$2023", claw.definition().name());
        assertTrue(claw.sidecarImportable());
        assertFalse(claw.definition().tradable());
        assertEquals(1, fixture.inventoryWeightStats(3).rawWeight());
        assertEquals(1, fixture.inventoryWeightStats(3).itemRows());
        assertTrue(fixture.sourceInventory(1).isEmpty());
    }

    @Test void optInMemorySourceProjectileUsesMaterializedBowAndArrowRows() {
        MemoryAccountRepository fixture = new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52, 110,
                false, true);

        List<SourceInventoryItem> rows = fixture.sourceInventory(19);
        assertEquals(2, rows.size());
        SourceInventoryItem bow = rows.get(0);
        SourceInventoryItem arrow = rows.get(1);
        assertEquals(1900L, bow.sourceObjectId());
        assertEquals(172, bow.catalogId());
        assertTrue(bow.equipped());
        assertTrue(bow.sourceWeaponImportable());
        assertEquals(1901L, arrow.sourceObjectId());
        assertEquals(40743, arrow.catalogId());
        assertEquals(100, arrow.quantity());
        assertEquals("arrow", arrow.wire().useToken());
        assertEquals(0xFE, arrow.definition().useType());
        assertTrue(arrow.sourceProjectileImportable());
        assertTrue(arrow.sidecarImportable());
        assertEquals(new SourceInventoryWeightStats(33, 2, 0, 1, 1, 1.0),
                fixture.inventoryWeightStats(19));
        SourceEquipmentEvaluation equipped = fixture.equipmentEvaluation(
                19, Map.of(1900L, true));
        assertTrue(equipped.magic().physical().weapon().bow());
        assertEquals(1, equipped.magic().matchedEquipmentRows());
        SourceEquipmentEvaluation unequipped = fixture.equipmentEvaluation(
                19, Map.of(1900L, false));
        assertFalse(unequipped.magic().physical().weapon().present());
        assertEquals(0, unequipped.magic().matchedEquipmentRows());
        assertThrows(IllegalArgumentException.class,
                () -> fixture.equipmentEvaluation(19, Map.of(9999L, true)));
        assertTrue(fixture.sourceInventory(18).isEmpty());
    }

    @Test void combinedSourceFlagsExposeOnlyTheTransferAcceptanceTokenToKnight() {
        MemoryAccountRepository fixture = new MemoryAccountRepository(
                "test", "test", "", 0, 1.0, 40, 52, 110,
                true, true);

        SourceInventoryItem token = fixture.sourceInventory(1).get(0);
        assertEquals(2001L, token.sourceObjectId());
        assertEquals(65_001, token.catalogId());
        assertEquals(3, token.quantity());
        assertEquals("Source Trade Token", token.definition().name());
        assertTrue(token.definition().stackable());
        assertTrue(token.definition().tradable());
        assertTrue(token.ordinarySidecarImportable());
        assertEquals(new SourceInventoryWeightStats(3, 1, 0, 0, 1, 1.0),
                fixture.inventoryWeightStats(1));
        assertTrue(fixture.sourceInventory(3).isEmpty());
        assertEquals(new SourceInventoryWeightStats(0, 0, 0, 0, 0, 1.0),
                fixture.inventoryWeightStats(3));
    }

    @Test void memoryRepositoryCreatesAndChangesOnlyLocalAccounts() throws Exception {
        MemoryAccountRepository repository = new MemoryAccountRepository("test", "test");

        assertEquals(AccountMutationResult.SUCCESS,
                repository.createAccount("Native01", "Passcode1"));
        assertEquals(AccountMutationResult.EXISTS,
                repository.createAccount("native01", "Different1"));
        assertTrue(repository.authenticate("NATIVE01", "Passcode1").accepted());
        assertTrue(repository.characters("Native01").isEmpty());
        assertEquals(Set.of(1, 2, 4, 6, 7, 11, 15, 16, 17, 18, 25, 34, 35,
                        44, 58, 63, 77, 78, BattleSpellCatalog.WORLD_TREE_CALL),
                repository.learnedSkills(1));
        assertEquals(Set.of(1, 3, 4, BattleSpellCatalog.WIND_CUTTER,
                BattleSpellCatalog.HEAL_ALL,
                BattleSpellCatalog.HOLY_WEAPON,
                BattleSpellCatalog.CURE_POISON,
                BattleSpellCatalog.CHILL_TOUCH,
                BattleSpellCatalog.VAMPIRIC_TOUCH,
                BattleSpellCatalog.SLOW,
                BattleSpellCatalog.EARTH_JAIL,
                BattleSpellCatalog.FROZEN_CLOUD,
                BattleSpellCatalog.REVEAL_WEAKNESS,
                BattleSpellCatalog.DETECTION,
                14, 19, 20, 26, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
                42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 56, 57, 59, 60, 61, 62, 64,
                65, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
                131, BattleSpellCatalog.EARTH_BIND, 206),
                repository.learnedSkills(3));
        assertTrue(repository.learnedSkills(4).isEmpty());
        assertEquals(Set.of(BattleSpellCatalog.TRUE_TARGET,
                        BattleSpellCatalog.GLOWING_AURA,
                        BattleSpellCatalog.SHINING_AURA,
                        BattleSpellCatalog.CALL_CLAN,
                        BattleSpellCatalog.BRAVE_AURA,
                        BattleSpellCatalog.RUN_CLAN,
                        131),
                repository.learnedSkills(5));
        assertEquals(Set.of(129, 130, 131,
                        BattleSpellCatalog.ELEMENTAL_FALL_DOWN,
                        BattleSpellCatalog.COUNTER_MIRROR,
                        BattleSpellCatalog.CLEAR_MIND,
                        BattleSpellCatalog.RESIST_ELEMENTAL,
                        BattleSpellCatalog.BLOODY_SOUL,
                        BattleSpellCatalog.LESSER_ELEMENTAL,
                        BattleSpellCatalog.GREATER_ELEMENTAL,
                         BattleSpellCatalog.BURNING_WEAPON,
                         BattleSpellCatalog.NATURES_BLESSING,
                         BattleSpellCatalog.WATER_LIFE,
                         157, 169, 176),
                repository.learnedSkills(6));
        assertEquals(Set.of(49, 55, 69, 131), repository.learnedSkills(7));
        assertEquals(Set.of(27, 31, 44, 60, 88, 89, 90, 131),
                repository.learnedSkills(16));
        assertEquals(SourceEquippedFireResistance.none(),
                repository.equippedFireResistance(1));
        assertEquals(new SourceEquippedFireResistance(30, 1),
                repository.equippedFireResistance(3));
        assertEquals(new SourceBaseMagicResistance(36, 10, 26, 0, 0),
                repository.baseMagicResistance(1));
        assertEquals(new SourceBaseMagicResistance(24, 0, 23, 0, 1),
                repository.baseMagicResistance(3));
        SourceEquipmentMagicStats equipment = repository.equipmentMagicStats(3);
        assertEquals(30, equipment.totalFireResistance());
        assertEquals(1, equipment.matchedEquipmentRows());
        assertEquals(1, equipment.matchedArmorRows());
        assertEquals(11, equipment.effectiveIntelligence(11));
        assertEquals(11, equipment.magicIntelligenceTerm(11));
        assertEquals(0, equipment.totalDirectMagicResistance());
        SourceNpcPhysicalStats iceman = repository.npcPhysicalStats(45207);
        assertTrue(iceman.tamable());
        assertFalse(iceman.boss());
        assertEquals(20, iceman.maxMp());
        assertTrue(iceman.eraseEligible());
        assertEquals(17, iceman.strength());
        assertEquals(17, iceman.dexterity());
        assertTrue(repository.npcPhysicalStats(45601).boss());
        SourceNpcPhysicalStats zombie = repository.npcPhysicalStats(81186);
        assertEquals(8, zombie.armorClass());
        assertEquals(12, zombie.maxMp());
        assertEquals(16, zombie.strength());
        assertEquals(6, zombie.dexterity());
        assertTrue(zombie.turnUndeadEligible());
        assertTrue(zombie.eraseEligible());
        SourceSummonTemplate summon = repository.summonTemplate(81240);
        assertEquals("黑豹", summon.name());
        assertEquals("$2587", summon.nameId());
        assertEquals(3049, summon.gfx());
        assertEquals(80, summon.level());
        assertEquals(4000, summon.maxHp());
        assertEquals(400, summon.maxMp());
        assertEquals(-80, summon.armorClass());
        assertEquals(85, summon.strength());
        assertEquals(63, summon.dexterity());
        assertEquals(480, summon.moveIntervalMillis());
        assertEquals(440, summon.attackIntervalMillis());
        assertThrows(IllegalArgumentException.class,
                () -> repository.summonTemplate(81209));
        for (int npcId = 81210; npcId <= 81240; npcId++) {
            SourceSummonTemplate row = repository.summonTemplate(npcId);
            assertEquals(npcId, row.npcId());
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    row.gfx(), BattleActionCodes.WALK));
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    row.gfx(), BattleActionCodes.ATTACK));
        }
        SourcePet sourcePet = repository.pets(3).get(0);
        assertEquals(0x3810, sourcePet.sourceCollarObjectId());
        assertEquals(InventoryCatalog.PET_COLLAR, sourcePet.collarCatalogId());
        assertEquals(45034, sourcePet.npcId());
        assertEquals(750, sourcePet.experience());
        SourcePet evolutionFixture = repository.pets(19).get(0);
        assertEquals(0x3820, evolutionFixture.sourceCollarObjectId());
        assertEquals(30, evolutionFixture.level());
        assertEquals(120, evolutionFixture.maxHp());
        assertEquals(24, evolutionFixture.maxMp());
        assertEquals(60_000, evolutionFixture.experience());
        assertTrue(repository.pets(1).isEmpty());
        SourcePetTemplate pet = repository.petTemplate(sourcePet.npcId());
        assertEquals("$907", pet.nameId());
        assertEquals(936, pet.gfx());
        assertEquals(6, pet.charismaCost());
        assertEquals(40057, pet.tamingItemCatalogId());
        assertEquals(1500, pet.tameRate());
        assertEquals(5, pet.hpUpMin());
        assertEquals(8, pet.hpUpMax());
        assertEquals(1, pet.mpUpMin());
        assertEquals(2, pet.mpUpMax());
        assertTrue(pet.canEvolve());
        assertEquals(30, pet.minimumEvolutionLevel());
        assertEquals(InventoryCatalog.EVOLUTION_FRUIT,
                pet.evolutionItemCatalogId());
        assertEquals(45687, pet.evolutionNpcId());
        SourcePetTemplate highPet = repository.petTemplate(pet.evolutionNpcId());
        assertEquals(InventoryCatalog.HIGH_PET_COLLAR,
                highPet.collarCatalogId());
        assertEquals(0, highPet.tamingItemCatalogId());
        assertEquals(1500, highPet.tameRate());
        assertEquals(InventoryCatalog.VICTORY_FRUIT,
                highPet.evolutionItemCatalogId());
        assertEquals(46046, highPet.evolutionNpcId());
        assertFalse(repository.petTemplate(46046).canEvolve());
        SourceEquipmentMagicStats physicalFixture = repository.equipmentMagicStats(12);
        assertEquals("TestPhysical", repository.characters("test11").get(0).name());
        assertEquals(BattleActionCodes.SWORD_WALK,
                physicalFixture.physical().weapon().walkAction());
        assertEquals(19, physicalFixture.physical().totalHit(false));
        assertEquals(23, physicalFixture.physical().totalDamageBonus(false));
        assertEquals(10, physicalFixture.physical().effectiveDexterity(10));
        assertEquals(11, physicalFixture.physical().weapon().attackMaterial());
        assertEquals(1, physicalFixture.physical().weapon().attackBlessing());
        assertEquals(0, physicalFixture.physical().totalPierce());
        SourcePhysicalGrowthStats growth = repository.physicalGrowthStats();
        assertEquals(34, growth.damage(false, 127));
        assertEquals(32, growth.damage(true, 127));
        assertEquals(new SourceNpcPhysicalStats(
                        45262, -4, true, 0, true, 0, 8, 0, false, -22,
                        false, false, 10, 16, 15, true),
                repository.npcPhysicalStats(45262));
        assertEquals(new SourceInventoryWeightStats(0, 0, 0, 0, 0, 1.0),
                repository.inventoryWeightStats(3));
        assertEquals(100, repository.characters("test2").get(0).maxHp());
        assertEquals("LegacyEmblem", repository.characters("test4").get(0).clanName());
        assertEquals(2, repository.characters("test5").get(0).type());
        assertEquals("TestElf", repository.characters("test5").get(0).name());
        assertEquals(100, repository.characters("test5").get(0).maxMp());
        assertEquals("LegacyEmblem", repository.characters("test6").get(0).clanName());
        assertEquals("TestClanMate", repository.characters("test6").get(0).name());
        assertEquals("LegacyKent", repository.characters("test7").get(0).clanName());
        assertEquals("TestKentLord", repository.characters("test7").get(0).name());
        assertEquals("TestBow", repository.characters("test18").get(0).name());
        SourceWeaponPhysicalStats bow = repository.equipmentMagicStats(19)
                .physical().weapon();
        assertTrue(bow.bow());
        assertEquals(4, bow.weaponType());
        assertEquals(-2, bow.projectile().useType());
        assertEquals(66, bow.projectile().effectGfx());
        SourceDefensiveEquipment shield = repository.equipmentMagicStats(16)
                .physical().defensiveEquipment();
        assertEquals(new SourceDefensiveEquipment(
                20220, 7, 508, 1, "$1210"), shield);
        assertEquals(25, shield.useType());
        assertEquals("TestCounterBarrier",
                repository.characters("test19").get(0).name());
        assertEquals(Set.of(44, BattleSpellCatalog.COUNTER_BARRIER,
                        BattleSpellCatalog.WORLD_TREE_CALL),
                repository.learnedSkills(20));
        SourceEquipmentMagicStats counterBarrierEquipment =
                repository.equipmentMagicStats(20);
        SourceWeaponPhysicalStats twoHandSword =
                counterBarrierEquipment.physical().weapon();
        assertEquals(52, twoHandSword.itemId());
        assertEquals(3, twoHandSword.weaponType());
        assertEquals(17, twoHandSword.damageSmall());
        assertEquals(21, twoHandSword.damageLarge());
        assertEquals(1, twoHandSword.range());
        assertEquals("$3", twoHandSword.nameId());
        assertEquals(BattleActionCodes.TWO_HAND_SWORD_WALK,
                twoHandSword.walkAction());
        // Item 52 itself carries no magic-hit field; the probability engine
        // still includes any independent equipment/buff magic hit at runtime.
        assertEquals(0, counterBarrierEquipment.totalMagicHit());
        assertEquals(7, repository.characters("test").get(0).pkCount());
        ClanEmblemData emblem = repository.clanEmblem("LegacyEmblem");
        assertEquals(0x1020_3040L, emblem.emblemId());
        assertEquals(Protocol.EMBLEM_BYTES, emblem.pixels().length);
        CastleSummary castle = repository.castleForClan("legacyemblem");
        assertEquals(5, castle.castleId());
        assertEquals("TestEmblem", castle.leaderName());
        assertEquals(10, castle.taxRate());
        assertEquals(10_000, castle.publicMoney());
        CastleSummary kent = repository.castleForClan("legacykent");
        assertEquals(1, kent.castleId());
        assertEquals("TestKentLord", kent.leaderName());
        assertEquals(1_221_408_000_000L, kent.warTimeMillis());
        assertEquals(0, kent.taxRate());
        assertEquals(0, kent.publicMoney());
        assertEquals(null, repository.castleForClan("NoCastle"));
        List<CastleWarSchedule> castleSchedules = repository.castleWarSchedules();
        assertEquals(8, castleSchedules.size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), castleSchedules.stream()
                .map(CastleWarSchedule::castleId).toList());
        CastleWarSchedule heineSchedule = castleSchedules.get(4);
        assertEquals(5, heineSchedule.castleId());
        assertEquals(60, heineSchedule.warDurationMinutes());
        assertEquals(10_080, heineSchedule.warIntervalMinutes());

        assertEquals(AccountMutationResult.WRONG_PASSWORD,
                repository.changePassword("Native01", "wrong", "Changed1"));
        assertEquals(AccountMutationResult.SUCCESS,
                repository.changePassword("Native01", "Passcode1", "Changed1"));
        assertFalse(repository.authenticate("Native01", "Passcode1").accepted());
        assertTrue(repository.authenticate("Native01", "Changed1").accepted());
        assertEquals(AccountMutationResult.NOT_FOUND,
                repository.changePassword("Missing01", "Passcode1", "Changed1"));

        List<String> quiz = List.of(
                "first-question", "answer-one", "second-question", "answer-two");
        assertEquals(AccountMutationResult.WRONG_PASSWORD,
                repository.registerQuiz("Native01", "wrong", "QuizPass2", quiz));
        assertEquals(AccountMutationResult.SUCCESS,
                repository.registerQuiz("Native01", "Changed1", "QuizPass2", quiz));
        assertFalse(repository.authenticate("Native01", "Changed1").accepted());
        assertTrue(repository.authenticate("Native01", "QuizPass2").accepted());
        assertEquals(new QuizRegistrationSnapshot(quiz),
                repository.registeredQuizForTest("native01"));
        assertEquals(AccountMutationResult.NOT_FOUND,
                repository.registerQuiz("Missing01", "Changed1", "QuizPass2", quiz));

        List<String> profile = List.of("p0", "p1", "p2", "p3", "p4", "p5", "p6");
        assertEquals(AccountMutationResult.SUCCESS,
                repository.changeAccountInfo("Native01", 1, profile));
        assertEquals(new AccountInfoSnapshot(1, profile),
                repository.accountInfoForTest("native01"));
        assertEquals(AccountMutationResult.NOT_FOUND,
                repository.changeAccountInfo("Missing01", 0, profile));
    }

    @Test void optionalMemoryFailureFixtureCannotAffectOrdinaryAccounts() throws Exception {
        MemoryAccountRepository repository = new MemoryAccountRepository(
                "test", "test", "backend-failure");

        assertTrue(repository.authenticate("test", "test").accepted());
        assertThrows(java.sql.SQLException.class,
                () -> repository.authenticate("BACKEND-FAILURE", "anything"));
    }

    @Test void sourceRouterAuthenticatesAndAuthorizesEachOriginalStoreIndependently()
            throws Exception {
        MemoryAccountRepository sun = new MemoryAccountRepository("same", "sun-pass");
        MemoryAccountRepository moon = new MemoryAccountRepository("same", "moon-pass");
        Map<String, AccountRepository> sources = new LinkedHashMap<>();
        sources.put("Sun", sun);
        sources.put("Moon", moon);
        SourceRoutedAccountRepository router = new SourceRoutedAccountRepository(sources);

        AuthResult sunOnly = router.authenticate("SAME", "sun-pass");
        assertTrue(sunOnly.accepted());
        assertTrue(sunOnly.authorizesClassicSource("sun"));
        assertFalse(sunOnly.authorizesClassicSource("Moon"));
        assertEquals(Set.of("Sun"), sunOnly.authorizedClassicSources());
        assertSame(sun, router.repositoryForClassicSource("SUN"));
        assertSame(moon, router.repositoryForClassicSource("moon"));
        assertEquals(new SourceEquippedFireResistance(30, 1),
                router.repositoryForClassicSource("moon").equippedFireResistance(3));
        assertEquals(24,
                router.repositoryForClassicSource("moon").baseMagicResistance(3).value());
        assertEquals(45610,
                router.repositoryForClassicSource("moon")
                        .npcPhysicalStats(45610).npcId());
        assertEquals(81210,
                router.repositoryForClassicSource("moon")
                        .summonTemplate(81210).npcId());

        AuthResult moonOnly = router.authenticate("same", "moon-pass");
        assertTrue(moonOnly.accepted());
        assertEquals(Set.of("Moon"), moonOnly.authorizedClassicSources());
        assertFalse(router.authenticate("same", "wrong").accepted());
        assertThrows(IllegalArgumentException.class,
                () -> router.repositoryForClassicSource("unknown"));
    }

    @Test void sourceRouterFailsClosedWhenNoSourceCanCompleteAuthentication()
            throws Exception {
        Map<String, AccountRepository> sources = new LinkedHashMap<>();
        sources.put("Sun", new MemoryAccountRepository("same", "sun-pass", "same"));
        sources.put("Moon", new MemoryAccountRepository("same", "moon-pass", "same"));
        SourceRoutedAccountRepository router = new SourceRoutedAccountRepository(sources);

        assertThrows(java.sql.SQLException.class,
                () -> router.authenticate("same", "anything"));
    }

    @Test void sourceRouterCreatesChangesAndDeletesLitoAccountAcrossEveryStore()
            throws Exception {
        MemoryAccountRepository sun = new MemoryAccountRepository("base", "base-pass");
        MemoryAccountRepository moon = new MemoryAccountRepository("base", "base-pass");
        SourceRoutedAccountRepository router = new SourceRoutedAccountRepository(
                new LinkedHashMap<>(Map.of("Sun", sun, "Moon", moon)));

        assertEquals(AccountMutationResult.SUCCESS,
                router.createAccount("LiToShared", "FirstPass1"));
        assertTrue(sun.authenticate("litoshared", "FirstPass1").accepted());
        assertTrue(moon.authenticate("LITOSHARED", "FirstPass1").accepted());
        assertEquals(Set.of("Sun", "Moon"), router
                .authenticate("LiToShared", "FirstPass1").authorizedClassicSources());

        assertEquals(AccountMutationResult.SUCCESS,
                router.changePassword("LiToShared", "FirstPass1", "SecondPass2"));
        assertFalse(sun.authenticate("LiToShared", "FirstPass1").accepted());
        assertFalse(moon.authenticate("LiToShared", "FirstPass1").accepted());
        assertTrue(sun.authenticate("LiToShared", "SecondPass2").accepted());
        assertTrue(moon.authenticate("LiToShared", "SecondPass2").accepted());

        assertTrue(router.deleteLitoAccount("LiToShared"));
        assertFalse(sun.accountExists("LiToShared"));
        assertFalse(moon.accountExists("LiToShared"));
        assertFalse(router.deleteLitoAccount("base"));
        assertTrue(sun.accountExists("base"));
        assertTrue(moon.accountExists("base"));
    }

    @Test void sourceRouterDoesNotPartiallyCreateWhenAnyStoreAlreadyOwnsLogin()
            throws Exception {
        MemoryAccountRepository sun = new MemoryAccountRepository("base", "base-pass");
        MemoryAccountRepository moon = new MemoryAccountRepository("existing", "old-pass");
        SourceRoutedAccountRepository router = new SourceRoutedAccountRepository(
                new LinkedHashMap<>(Map.of("Sun", sun, "Moon", moon)));

        assertEquals(AccountMutationResult.EXISTS,
                router.createAccount("existing", "new-pass"));
        assertFalse(sun.accountExists("existing"));
        assertTrue(moon.authenticate("existing", "old-pass").accepted());
    }
}
