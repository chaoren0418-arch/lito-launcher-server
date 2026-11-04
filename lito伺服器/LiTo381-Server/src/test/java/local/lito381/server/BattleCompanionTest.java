package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class BattleCompanionTest {
    @Test
    void retainedDamagePoisonAcceptsOnlyCapturedMonsterAndTicksUntilThirtySeconds() {
        long now = 1_000_000L;
        long attacker = 0x6100_0001L;
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6000_0301L, 81217, 4205, "$3378", 20,
                100, 100, 20, 0, 14, 14, 100, 120);
        BattleCompanion.NpcPoisonInstallation installed =
                tamed.activateNpcPoison(
                        ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                        attacker, now);
        assertTrue(installed.nativeDamageTarget());
        assertTrue(installed.applied());
        assertEquals(1, tamed.pollNpcPoison(now + 3_000_000_000L)
                .damageTicks());
        assertEquals(8, tamed.pollNpcPoison(now + 27_000_000_000L)
                .damageTicks());
        BattleCompanion.NpcPoisonPoll expired =
                tamed.pollNpcPoison(now + 30_000_000_000L);
        assertTrue(expired.expired());
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                expired.kind());
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                tamed.pollNpcPoison(now + 30_000_000_001L).kind());

        BattleCompanion summon = BattleCompanion.summoned(
                0x6000_0302L, BattleSpellCatalog.summonChoice(11),
                SourceSummonTemplate.memoryFixture(81222), 6,
                1_800, 100, 120, now);
        BattleCompanion.NpcPoisonInstallation summonRejected =
                summon.activateNpcPoison(
                        ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                        attacker, now);
        assertFalse(summonRejected.nativeDamageTarget());
        assertFalse(summonRejected.applied());

        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3901L, 0x9000_0303L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120);
        BattleCompanion.NpcPoisonInstallation petRejected =
                pet.activateNpcPoison(
                        ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                        attacker, now);
        assertFalse(petRejected.nativeDamageTarget());
        assertFalse(petRejected.applied());
    }

    @Test
    void retainedSilencePoisonHasNoTimerAndCancellationCuresIt() {
        long now = 2_000_000L;
        BattleCompanion summon = BattleCompanion.summoned(
                0x6000_0310L, BattleSpellCatalog.summonChoice(11),
                SourceSummonTemplate.memoryFixture(81222), 6,
                1_800, 100, 120, now);
        BattleCompanion.NpcPoisonInstallation installed =
                summon.activateNpcPoison(
                        ClassicWorldMonsterPoisonAttack.Kind.SILENCE,
                        0x6100_0010L, now);
        assertTrue(installed.applied());
        assertFalse(summon.pollNpcPoison(Long.MAX_VALUE).expired());

        BattleCompanion.MagicCancellation cancellation =
                summon.cancelMagicEffects();
        assertTrue(cancellation.poison());
        assertFalse(cancellation.paralysis());
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                summon.pollNpcPoison(now + 1).kind());
    }

    @Test
    void retainedParalysisPoisonTransitionsPaletteAndLocksOnlyActivePhase() {
        long now = 3_000_000L;
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6000_0320L, 81217, 4205, "$3378", 20,
                100, 100, 20, 0, 14, 14, 100, 120);
        assertTrue(tamed.selectTarget(0x6200_0320L));
        assertTrue(tamed.activateNpcPoison(
                ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS,
                0x6100_0020L, now).applied());
        assertTrue(tamed.ready(now + 19_999_999_999L));

        BattleCompanion.NpcPoisonPoll activated =
                tamed.pollNpcPoison(now + 20_000_000_000L);
        assertTrue(activated.activated());
        assertFalse(tamed.ready(now + 20_000_000_000L));

        BattleCompanion.NpcPoisonPoll expired =
                tamed.pollNpcPoison(now + 65_000_000_000L);
        assertTrue(expired.expired());
        assertTrue(tamed.ready(now + 65_000_000_000L));

        assertTrue(tamed.activateNpcPoison(
                ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS,
                0x6100_0021L, now + 66_000_000_000L).applied());
        BattleCompanion.NpcPoisonCancellation removed =
                tamed.removeCurseNpcPoison(false);
        assertFalse(removed.poison());
        assertTrue(removed.paralysis());
    }

    @Test
    void retainedCurseParalysisImmediatelyLocksOnlyCapturedMonsterTarget() {
        long now = 4_000_000L;
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6000_0330L, 81217, 4205, "$3378", 20,
                100, 100, 20, 0, 14, 14, 100, 120);
        assertTrue(tamed.selectTarget(0x6200_0330L));
        BattleCompanion.NpcCurseParalysisInstallation installed =
                tamed.activateNpcCurseParalysis(now, 10_000);
        assertTrue(installed.nativeMonsterTarget());
        assertTrue(installed.applied());
        assertTrue(tamed.npcCurseParalysisActive(now));
        assertFalse(tamed.ready(now));
        assertFalse(tamed.pollNpcCurseParalysis(
                now + 9_999_999_999L).expired());
        assertTrue(tamed.pollNpcCurseParalysis(
                now + 10_000_000_000L).expired());
        assertFalse(tamed.npcCurseParalysisActive(
                now + 10_000_000_000L));
        assertTrue(tamed.ready(now + 10_000_000_000L));

        BattleCompanion summon = BattleCompanion.summoned(
                0x6000_0331L, BattleSpellCatalog.summonChoice(11),
                SourceSummonTemplate.memoryFixture(81222), 6,
                1_800, 100, 120, now);
        BattleCompanion.NpcCurseParalysisInstallation summonRejected =
                summon.activateNpcCurseParalysis(now, 10_000);
        assertFalse(summonRejected.nativeMonsterTarget());
        assertFalse(summonRejected.applied());

        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3931L, 0x9000_0332L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120);
        BattleCompanion.NpcCurseParalysisInstallation petRejected =
                pet.activateNpcCurseParalysis(now, 10_000);
        assertFalse(petRejected.nativeMonsterTarget());
        assertFalse(petRejected.applied());
    }

    @Test
    void poisonAndCurseParalysisKeepIndependentNativeOwnershipDomains() {
        long now = 5_000_000L;
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6000_0340L, 81217, 4205, "$3378", 20,
                100, 100, 20, 0, 14, 14, 100, 120);
        assertTrue(tamed.selectTarget(0x6200_0340L));
        assertTrue(tamed.activateNpcPoison(
                ClassicWorldMonsterPoisonAttack.Kind.DAMAGE,
                0x6100_0340L, now).applied());
        assertTrue(tamed.activateNpcCurseParalysis(now, 10_000).applied());
        assertEquals(1, tamed.pollNpcPoison(
                now + 3_000_000_000L).damageTicks());
        assertTrue(tamed.npcCurseParalysisActive(now + 3_000_000_000L));

        BattleCompanion.NpcPoisonCancellation curePoison =
                tamed.removeCurseNpcPoison(false);
        assertTrue(curePoison.poison());
        assertFalse(curePoison.paralysis());
        assertEquals(ClassicWorldMonsterPoisonAttack.Kind.NONE,
                tamed.pollNpcPoison(now + 3_000_000_001L).kind());
        assertTrue(tamed.npcCurseParalysisActive(now + 3_000_000_001L));
        assertFalse(tamed.ready(now + 3_000_000_001L));

        BattleCompanion.NpcPoisonCancellation removeCurse =
                tamed.removeCurseNpcPoison(true);
        assertFalse(removeCurse.poison());
        assertTrue(removeCurse.paralysis());
        assertFalse(tamed.npcCurseParalysisActive(now + 3_000_000_002L));
        assertTrue(tamed.ready(now + 3_000_000_002L));
    }

    @Test
    void npcCombatQualificationRetainsTemplateLevelUndeadAndWeaponBreakLifecycle() {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_01F0L, 81217, 4205, "$3378", 28,
                240, 240, 80, 0, 16, 14, 100, 120);
        SourceNpcPhysicalStats source = new SourceNpcPhysicalStats(
                81217, 0, false, 0, false,
                3, 0, 5, false, 0, true, false,
                80, 16, 14, true, 1, 0, 1, 0);
        companion.qualifyNpcDefense(source);

        assertEquals(28, companion.sourceTemplateLevel());
        assertEquals(3, companion.undead());
        for (int expected = 1; expected <= BattleWeaponBreak.MAX_STACKS; expected++) {
            assertEquals(expected, companion.addWeaponBreakStack());
        }
        assertEquals(BattleWeaponBreak.MAX_STACKS,
                companion.addWeaponBreakStack());
        assertEquals(BattleWeaponBreak.MAX_STACKS,
                companion.weaponBreakStacks());

        companion.cancelMagicEffects();
        assertEquals(0, companion.weaponBreakStacks());
        assertEquals(1, companion.addWeaponBreakStack());
        companion.applyDamage(240);
        assertEquals(0, companion.weaponBreakStacks());
    }

    @Test
    void selectedNpcCombatQualificationRetainsNativeRangeAndBowAction() {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0200L, 81227, 1832, "$1572", 40,
                220, 220, 110, -5, 13, 16, 100, 120);
        companion.qualifyNpcDefense(
                SourceNpcPhysicalStats.memoryFixture(81227));

        assertEquals(10, companion.attackRange());
        assertEquals(66, companion.bowActionId());
        assertFalse(companion.projectileAtDistance(1));
        assertTrue(companion.projectileAtDistance(2));
        assertEquals(BattleActionCodes.ATTACK,
                companion.projectileAttackAction());
    }

    @Test
    void selectedNpcCombatQualificationRetainsNativePoisonFields() {
        BattleCompanion damagePoison = BattleCompanion.tamed(
                0x6000_0201L, 81217, 4205, "$3378", 28,
                240, 240, 80, 0, 16, 14, 100, 120);
        damagePoison.qualifyNpcDefense(
                SourceNpcPhysicalStats.memoryFixture(81217));
        assertEquals(1, damagePoison.poisonAttack());
        assertEquals(0, damagePoison.paralysisAttack());

        BattleCompanion silencePoison = BattleCompanion.tamed(
                0x6000_0202L, 81236, 5442, "$4306", 52,
                280, 280, 140, -50, 15, 20, 100, 120);
        silencePoison.qualifyNpcDefense(
                SourceNpcPhysicalStats.memoryFixture(81236));
        assertEquals(2, silencePoison.poisonAttack());
        assertEquals(0, silencePoison.paralysisAttack());

        SourceNpcPhysicalStats paralysisPoison =
                SourceNpcPhysicalStats.memoryFixture(45157);
        assertEquals(4, paralysisPoison.poisonAttack());
        assertEquals(0, paralysisPoison.paralysisAttack());
        assertTrue(paralysisPoison.tamable());
    }

    @Test
    void givenHealingPotionTriggersHighestPriorityPetBagPotionImmediately()
            throws Exception {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0781L, 45207, 2158, "$1791", 26,
                100, 200, 20, -20, 17, 17, 100, 120);
        assertTrue(companion.receiveItem(sourceItem(40012), 1));

        BattleCompanion.GivenItemReceipt receipt =
                companion.receiveItemAndOnGetItemPersisted(
                        sourceItem(40010), 1, 1_000L, () -> {});

        assertEquals(BattleCompanion.GivenItemUseKind.HEAL, receipt.useKind());
        assertEquals(40012, receipt.consumedCatalogId());
        assertEquals(197, receipt.effectGfx());
        assertEquals(75, receipt.appliedHp());
        assertEquals(175, receipt.afterUse().hp());
        assertEquals(1, companion.carriedItems().size());
        assertEquals(40010, companion.carriedItems().get(0)
                .definition().catalogId());
    }

    @Test
    void givenHastePotionUsesPriorityDurationOnlyWhenPetIsNotAlreadyHasted()
            throws Exception {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0782L, 45207, 2158, "$1791", 26,
                200, 200, 20, -20, 17, 17, 100, 120);
        assertTrue(companion.receiveItem(sourceItem(40018), 1));
        long now = 2_000L;

        BattleCompanion.GivenItemReceipt first =
                companion.receiveItemAndOnGetItemPersisted(
                        sourceItem(40013), 1, now, () -> {});
        assertEquals(BattleCompanion.GivenItemUseKind.HASTE, first.useKind());
        assertEquals(40018, first.consumedCatalogId());
        assertEquals(1800, first.durationSeconds());
        assertEquals(191, first.effectGfx());
        assertEquals(BattleTimedStatus.HASTE, first.afterUse().moveSpeed());

        BattleCompanion.GivenItemReceipt second =
                companion.receiveItemAndOnGetItemPersisted(
                        sourceItem(40013), 1, now + 1, () -> {});
        assertEquals(BattleCompanion.GivenItemUseKind.NONE, second.useKind());
        assertEquals(2, companion.carriedItems().get(0).quantity());
    }

    @Test
    void litLightCanBeGivenWithoutLosingNativeLightingState() throws Exception {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0783L, 45207, 2158, "$1791", 26,
                200, 200, 20, -20, 17, 17, 100, 120);
        RuntimeInventory source = RuntimeInventory.fromCatalogCounts(
                java.util.Map.of(InventoryCatalog.MAGIC_LANTERN, 1));
        long objectId = source.items().get(0).objectId();
        source.toggleLight(objectId);
        RuntimeInventory.Item lit = source.item(objectId);
        assertTrue(lit.equipped());
        assertFalse(lit.blocksNativeTransferAsEquipped());

        BattleCompanion.GivenItemReceipt receipt =
                companion.receiveItemAndOnGetItemPersisted(
                        lit, 1, 3_000L, () -> {});

        assertEquals(BattleCompanion.GivenItemUseKind.NONE, receipt.useKind());
        assertEquals(0x16, receipt.lightRange());
        assertTrue(companion.carriedItems().get(0).equipped());
        assertEquals(0x16, companion.lightRange());
    }

    @Test
    void failedGivenItemDurableCommitPublishesNoPetBagOrVitalMutation() {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0784L, 45207, 2158, "$1791", 26,
                100, 200, 20, -20, 17, 17, 100, 120);
        assertThrows(IOException.class,
                () -> companion.receiveItemAndOnGetItemPersisted(
                        sourceItem(40010), 1, 4_000L,
                        () -> { throw new IOException("forced"); }));
        assertTrue(companion.carriedItems().isEmpty());
        assertEquals(100, companion.snapshot().hp());
    }

    private static RuntimeInventory.Item sourceItem(long catalogId) {
        InventoryCatalog.Definition definition =
                ClassicWorldDropCatalog.inventoryDefinition((int) catalogId);
        if (definition == null) definition = InventoryCatalog.builtIn(catalogId);
        if (definition == null) {
            throw new IllegalArgumentException("missing source item " + catalogId);
        }
        RuntimeInventory source = new RuntimeInventory();
        assertTrue(source.add(definition, 1));
        return source.items().get(0);
    }

    @Test
    void strictManaDebitIsAtomicAndRejectsEqualBalance() {
        BattleCompanion companion = new BattleCompanion(0x6000_0207L);
        companion.resetForBattle(100, 120);
        BattleCompanion.ManaDebit first =
                companion.debitManaIfStrictlyAbove(4);
        assertTrue(first.debited());
        assertEquals(5, first.before().mp());
        assertEquals(1, first.after().mp());
        BattleCompanion.ManaDebit equal =
                companion.debitManaIfStrictlyAbove(1);
        assertFalse(equal.debited());
        assertEquals(1, equal.after().mp());
    }

    @Test
    void horrorOfDeathLeaseIsCancelableWithoutChangingNpcAttributes() {
        BattleCompanion companion = new BattleCompanion(0x6000_0193L);
        companion.resetForBattle(100, 120);
        long now = 1_000_000_000L;
        int strength = companion.snapshot().strength();

        assertFalse(companion.activateHorrorOfDeath(now, 60));
        assertTrue(companion.horrorOfDeathActive(now));
        assertTrue(companion.activateHorrorOfDeath(now + 1, 60));
        assertEquals(strength, companion.snapshot().strength());
        assertFalse(companion.expireHorrorOfDeath(
                now + 59_999_999_999L));
        assertTrue(companion.expireHorrorOfDeath(
                now + 60_000_000_000L));
        assertFalse(companion.horrorOfDeathActive(
                now + 60_000_000_000L));
        assertFalse(companion.activateHorrorOfDeath(
                now + 60_000_000_001L, 60));

        BattleCompanion.MagicCancellation cancelled =
                companion.cancelMagicEffects();
        assertTrue(cancelled.horrorOfDeath());
        assertFalse(companion.horrorOfDeathActive(now + 2));
    }

    @Test
    void nativeHasteLeaseAcceleratesOnlyNormalSpeedCompanionsAndRestoresBase() {
        BattleCompanion companion = new BattleCompanion(0x6000_0041L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 1_000_000_000L;
        assertTrue(companion.activateHaste(
                castAt, 2, BattleSpellCatalog.HASTE));
        assertEquals(BattleTimedStatus.HASTE, companion.snapshot().moveSpeed());

        BattleCompanion.Movement movement = companion.moveOneTile(
                101, 120, 2, castAt);
        assertEquals(101, movement.destinationX());
        long hastedMoveNanos = (long) (BattleCompanion.MOVE_INTERVAL_MILLIS
                * BattleActionInterval.HASTE_MULTIPLIER) * 1_000_000L;
        assertFalse(companion.ready(castAt + hastedMoveNanos - 1));
        assertTrue(companion.ready(castAt + hastedMoveNanos));

        assertEquals(0, companion.expireHaste(castAt + 1_999_999_999L));
        assertEquals(BattleSpellCatalog.HASTE,
                companion.expireHaste(castAt + 2_000_000_000L));
        assertEquals(BattleTimedStatus.NORMAL, companion.snapshot().moveSpeed());
        assertEquals(0, companion.cancelHaste());
    }

    @Test
    void cancellationAtomicallyClearsCompanionHasteAndEraseMagic() {
        BattleCompanion companion = new BattleCompanion(0x6000_0047L);
        companion.resetForBattle(100, 120);
        long now = 2_000_000_000L;
        assertTrue(companion.activateHaste(
                now, 1_200, BattleSpellCatalog.HASTE));
        assertTrue(companion.activateEraseMagic(now, 32));
        assertEquals(BattleTimedStatus.HASTE,
                companion.snapshot().moveSpeed());
        assertEquals(25, companion.effectiveMagicResistance(103, now));

        BattleCompanion.MagicCancellation cancelled =
                companion.cancelMagicEffects();
        assertTrue(cancelled.haste());
        assertFalse(cancelled.slow());
        assertFalse(cancelled.freeze());
        assertTrue(cancelled.eraseMagic());
        assertEquals(companion.snapshot().objectId(),
                cancelled.target().objectId());
        assertEquals(BattleTimedStatus.NORMAL,
                companion.snapshot().moveSpeed());
        assertEquals(103, companion.effectiveMagicResistance(103, now));

        BattleCompanion.MagicCancellation empty =
                companion.cancelMagicEffects();
        assertFalse(empty.haste());
        assertFalse(empty.slow());
        assertFalse(empty.freeze());
        assertFalse(empty.eraseMagic());
        companion.applyDamage(Integer.MAX_VALUE);
        assertEquals(null, companion.cancelMagicEffects());

        GameStateStore.PetState durable = new GameStateStore.PetState(
                "cancellation\u001fpet", 0x7000_0047L, 0x9000_0047L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120);
        assertEquals(BattleCompanion.Kind.PET, pet.snapshot().kind());
        assertTrue(pet.activateHaste(now, 1_200, BattleSpellCatalog.HASTE));
        assertTrue(pet.activateEraseMagic(now, 32));
        BattleCompanion.MagicCancellation petCancelled =
                pet.cancelMagicEffects();
        assertTrue(petCancelled.haste());
        assertFalse(petCancelled.slow());
        assertFalse(petCancelled.freeze());
        assertTrue(petCancelled.eraseMagic());
        assertEquals(BattleCompanion.Kind.PET, petCancelled.target().kind());
        assertEquals(BattleTimedStatus.NORMAL, pet.snapshot().moveSpeed());
    }

    @Test
    void nativeMassSlowDoublesCompanionIntervalsExpiresAndCancels() {
        BattleCompanion companion = new BattleCompanion(0x6000_0048L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 3_000_000_000L;
        BattleTimedStatus.SpeedChange change = companion.activateSlow(
                castAt, 64, BattleSpellCatalog.MASS_SLOW);
        assertEquals(BattleTimedStatus.SLOW, change.type());
        assertEquals(64, change.durationSeconds());
        assertEquals(BattleTimedStatus.SLOW, companion.snapshot().moveSpeed());

        BattleCompanion.Movement movement = companion.moveOneTile(
                101, 120, 2, castAt);
        assertEquals(101, movement.destinationX());
        long slowedMoveNanos = (long) (BattleCompanion.MOVE_INTERVAL_MILLIS
                * BattleActionInterval.SLOW_MULTIPLIER) * 1_000_000L;
        assertFalse(companion.ready(castAt + slowedMoveNanos - 1));
        assertTrue(companion.ready(castAt + slowedMoveNanos));
        assertEquals(0, companion.expireSlow(
                castAt + 63_999_999_999L));
        assertEquals(BattleSpellCatalog.MASS_SLOW,
                companion.expireSlow(castAt + 64_000_000_000L));
        assertEquals(BattleTimedStatus.NORMAL, companion.snapshot().moveSpeed());

        companion.activateSlow(castAt + 65_000_000_000L,
                64, BattleSpellCatalog.MASS_SLOW);
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertFalse(cancellation.haste());
        assertTrue(cancellation.slow());
        assertEquals(BattleTimedStatus.NORMAL, companion.snapshot().moveSpeed());
    }

    @Test
    void freezingBlizzardLocksCompanionActionsUntilExactExpiryOrCancellation() {
        BattleCompanion companion = new BattleCompanion(0x6000_0480L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 4_800_000_000L;

        assertTrue(companion.activateFreeze(castAt, 32));
        assertTrue(companion.freezeActive(castAt));
        assertFalse(companion.ready(castAt));
        assertFalse(companion.expireFreeze(castAt + 31_999_999_999L));
        assertFalse(companion.ready(castAt + 31_999_999_999L));
        assertTrue(companion.expireFreeze(castAt + 32_000_000_000L));
        assertFalse(companion.freezeActive(castAt + 32_000_000_000L));
        assertTrue(companion.ready(castAt + 32_000_000_000L));

        assertTrue(companion.activateFreeze(castAt + 33_000_000_000L, 32));
        companion.applyDamage(1);
        assertTrue(companion.freezeActive(castAt + 33_000_000_001L));
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertTrue(cancellation.freeze());
        assertFalse(companion.freezeActive(castAt + 33_000_000_001L));

        assertTrue(companion.activateFreeze(castAt + 34_000_000_000L, 32));
        companion.applyDamage(Integer.MAX_VALUE);
        assertFalse(companion.freezeActive(castAt + 34_000_000_001L));
    }

    @Test
    void shockStunLocksCompanionMovementAndAttackAndCancellationCannotRemoveIt() {
        BattleCompanion companion = new BattleCompanion(0x6000_0487L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 8_700_000_000L;

        BattleTimedStatus.ShockStunActivation activation =
                companion.activateShockStun(castAt, 4, 4);
        assertEquals(4, activation.durationSeconds());
        assertFalse(activation.refreshed());
        assertTrue(companion.shockStunActive(castAt));
        assertFalse(companion.ready(castAt));
        assertEquals(null, companion.moveOneTile(101, 120, 2, castAt));
        assertEquals(null, companion.beginStrike(101, 120, castAt));

        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertFalse(cancellation.freeze());
        assertTrue(companion.shockStunActive(castAt + 1));
        assertFalse(companion.expireShockStun(castAt + 3_999_999_999L));
        assertTrue(companion.expireShockStun(castAt + 4_000_000_000L));
        assertFalse(companion.shockStunActive(castAt + 4_000_000_000L));
        assertTrue(companion.ready(castAt + 4_000_000_000L));
    }

    @Test
    void thunderGrabLocksCompanionAndCancellationRemovesIt() {
        BattleCompanion companion = new BattleCompanion(0x6000_0492L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 9_200_000_000L;

        assertTrue(companion.activateThunderGrab(castAt, 4));
        assertTrue(companion.thunderGrabActive(castAt));
        assertFalse(companion.ready(castAt));
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertTrue(cancellation.thunderGrab());
        assertFalse(companion.thunderGrabActive(castAt + 1));
        assertTrue(companion.ready(castAt + 1));
    }

    @Test
    void boneBreakLocksCompanionForTwoSecondsAndCancellationRemovesIt() {
        BattleCompanion companion = new BattleCompanion(0x6000_0508L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.selectTarget(3));
        long castAt = 10_800_000_000L;

        assertTrue(companion.activateBoneBreak(castAt, 2));
        assertTrue(companion.boneBreakActive(castAt));
        assertFalse(companion.activateBoneBreak(castAt + 1, 2));
        assertFalse(companion.ready(castAt));
        assertEquals(null, companion.moveOneTile(101, 120, 2, castAt));
        assertEquals(null, companion.beginStrike(101, 120, castAt));
        BattleCompanion.MagicCancellation cancellation =
                companion.cancelMagicEffects();
        assertTrue(cancellation.boneBreak());
        assertFalse(companion.boneBreakActive(castAt + 1));
        assertTrue(companion.ready(castAt + 1));

        assertTrue(companion.activateBoneBreak(castAt + 2, 2));
        assertFalse(companion.expireBoneBreak(castAt + 2_000_000_001L));
        assertTrue(companion.expireBoneBreak(castAt + 2_000_000_002L));
    }

    @Test
    void ownsBattleScopedPetStateAndMapsNativeStaticActions() {
        BattleCompanion companion = new BattleCompanion(0x6000_0042L);
        assertFalse(companion.snapshot().active());

        companion.resetForBattle(100, 120);
        BattleCompanion.Snapshot initial = companion.snapshot();
        assertTrue(initial.active());
        assertEquals(BattleCompanion.Posture.STAY, initial.posture());
        assertEquals(100, initial.x());
        assertEquals(120, initial.y());
        assertEquals("$471", initial.anicomArguments().get(0));
        assertEquals(10, initial.anicomArguments().size());

        assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED,
                companion.apply("aggressive"));
        assertEquals(BattleCompanion.Posture.AGGRESSIVE, companion.snapshot().posture());
        assertEquals("$469", companion.snapshot().anicomArguments().get(0));
        assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED,
                companion.apply("defensive"));
        assertEquals("$470", companion.snapshot().anicomArguments().get(0));
        assertEquals(BattleCompanion.CommandResult.TARGET_SELECTION_PENDING,
                companion.apply("attackchr"));
        assertTrue(companion.selectTarget(3));
        assertEquals(3, companion.snapshot().targetObjectId());
        BattleCompanion.Movement movement = companion.moveOneTile(101, 120, 2, 10);
        assertEquals(100, movement.sourceX());
        assertEquals(101, movement.destinationX());
        assertEquals(2, movement.heading());
        assertFalse(companion.ready(10 + BattleCompanion.MOVE_INTERVAL_MILLIS * 1_000_000L - 1));
        assertTrue(companion.ready(10 + BattleCompanion.MOVE_INTERVAL_MILLIS * 1_000_000L));
        BattleCompanion.Strike strike = companion.beginStrike(102, 120,
                10 + BattleCompanion.MOVE_INTERVAL_MILLIS * 1_000_000L);
        assertEquals(2, strike.heading());
        assertEquals(BattleCompanion.CommandResult.POSTURE_CHANGED,
                companion.apply("stay"));
        assertEquals(0, companion.snapshot().targetObjectId());
        assertEquals(BattleCompanion.CommandResult.COLLECTION_REQUESTED,
                companion.apply("getitem"));
        assertEquals(BattleCompanion.CommandResult.RENAME_PENDING,
                companion.apply("changename"));

        assertEquals(BattleCompanion.CommandResult.DISMISSED,
                companion.apply("dismiss"));
        assertFalse(companion.snapshot().active());
        assertEquals(0, companion.snapshot().targetObjectId());
        assertFalse(companion.selectTarget(3));
        assertEquals(BattleCompanion.CommandResult.REJECTED,
                companion.apply("aggressive"));
    }

    @Test
    void rejectsNonNativeCaseAndInvalidWorldIds() {
        BattleCompanion companion = new BattleCompanion(1);
        companion.resetForBattle(100, 120);
        assertEquals(BattleCompanion.CommandResult.REJECTED,
                companion.apply("Aggressive"));
        assertEquals(BattleCompanion.CommandResult.REJECTED,
                companion.apply("unknown"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new BattleCompanion(0));
    }

    @Test
    void reproducesClassicShepherdDogBaseHitAndDamageRules() {
        assertEquals(15, ClientSession.companionHitChance(10, 3, 0));
        assertEquals(100, ClientSession.companionHitChance(10, 4, 0));
        assertEquals(15, ClientSession.companionHitChance(-10, 4, 10));

        assertEquals(4, ClientSession.companionPhysicalDamage(0, 10, 0, 99, 0));
        assertEquals(5, ClientSession.companionPhysicalDamage(0, 10, 4, 99, 0));
        assertEquals(7, ClientSession.companionPhysicalDamage(0, 10, 0, 0, 0));
    }

    @Test
    void carriesGivenItemsUntilNativeGetitemCollection() {
        BattleCompanion companion = new BattleCompanion(0x6000_0043L);
        companion.resetForBattle(100, 120);
        InventoryCatalog.Definition whistle = InventoryCatalog.require(InventoryCatalog.WHISTLE);

        assertTrue(companion.receiveItem(whistle, 1));
        assertEquals(1, companion.carriedItems().size());
        RuntimeInventory.Item carried = companion.carriedItems().get(0);
        assertEquals(InventoryCatalog.WHISTLE, carried.definition().catalogId());

        // A battle rebuild recreates the pet actor but classic collect(false)
        // ownership remains until getitem transfers or drops the item.
        companion.resetForBattle(101, 121);
        assertEquals(1, companion.carriedItems().size());
        assertEquals(BattleCompanion.CommandResult.COLLECTION_REQUESTED,
                companion.apply("getitem"));
        assertTrue(companion.removeCarriedItem(carried.objectId(), 1));
        assertTrue(companion.carriedItems().isEmpty());
    }

    @Test
    void givenItemRetainsMutableStateUntilNativeCollection() {
        BattleCompanion companion = new BattleCompanion(0x6000_0046L);
        companion.resetForBattle(100, 120);
        RuntimeInventory.Item sword = new RuntimeInventory.Item(
                0x7000_0456L, InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                1, false, 2, 1, false, RuntimeInventory.BLESSING_CURSED);

        assertTrue(companion.receiveItem(sword, 1));
        RuntimeInventory.Item carried = companion.carriedItems().get(0);
        assertEquals(1, carried.enchantLevel());
        assertEquals(2, carried.durability());
        assertFalse(carried.identified());
        assertEquals(RuntimeInventory.BLESSING_CURSED, carried.blessing());
    }

    @Test
    void durableGiveFailureDoesNotDuplicateDynamicSourceItem() throws Exception {
        BattleCompanion companion = new BattleCompanion(0x6000_0457L);
        companion.resetForBattle(100, 120);
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                65_001, 0, 321, 322, 1, true,
                "Source Companion Token", "retained source detail", 17, 9,
                true, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 87, 1_250, 3, null, null);
        RuntimeInventory.Item source = new RuntimeInventory.Item(
                0x7000_0457L, definition, 2, false, 0, 0,
                true, RuntimeInventory.BLESSING_NORMAL, true);

        assertThrows(IOException.class, () -> companion.receiveItemPersisted(
                source, 1, () -> { throw new IOException("write failed"); }));
        assertTrue(companion.carriedItems().isEmpty());
        assertTrue(companion.receiveItemPersisted(source, 1, () -> {}));
        RuntimeInventory.Item carried = companion.carriedItems().get(0);
        assertEquals(definition, carried.definition());
        assertTrue(carried.sourceBound());
    }

    @Test
    void preservesTheClassicShepherdDogPetEquipmentExclusion() {
        BattleCompanion companion = new BattleCompanion(0x6000_0045L);
        companion.resetForBattle(100, 120);
        assertFalse(companion.nativeEquipmentEligible());
        assertFalse(BattleCompanion.nativeEquipmentEligibleNpc(45034));
        assertFalse(BattleCompanion.nativeEquipmentEligibleNpc(46044));
        assertTrue(BattleCompanion.nativeEquipmentEligibleNpc(45686));
    }

    @Test
    void appliesClassicOneTimePetRenameRule() {
        BattleCompanion companion = new BattleCompanion(0x6000_0044L);
        companion.resetForBattle(100, 120);
        assertTrue(companion.renameOnce("K9"));
        assertEquals("K9", companion.snapshot().name());
        assertFalse(companion.renameOnce("K10"));
        assertEquals("K9", companion.snapshot().name());
    }

    @Test
    void tamedProfilePreservesCapturedIdentityVitalsAndDynamicCombatStats() {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0046L, 45207, 2158, "$1791", 26,
                137, 200, 20, -20, 17, 17, 100, 120);
        BattleCompanion.Snapshot snapshot = companion.snapshot();
        assertTrue(snapshot.active());
        assertEquals(BattleCompanion.Kind.TAMED, snapshot.kind());
        assertTrue(snapshot.kind().classicWorldEligible());
        assertEquals(45207, snapshot.npcId());
        assertEquals(2158, snapshot.gfx());
        assertEquals("$1791", snapshot.name());
        assertEquals(137, snapshot.hp());
        assertEquals(200, snapshot.maxHp());
        assertEquals(20, snapshot.mp());
        assertEquals(-20, snapshot.armorClass());
        assertEquals(17, snapshot.strength());
        assertEquals(17, snapshot.dexterity());
        assertEquals(6, snapshot.petCost());
        assertTrue(companion.nativeEquipmentEligible());
    }

    @Test
    void groupHealingRestoresOnlyLivingActivePetVitalsAndCapsAtMaximum() {
        BattleCompanion companion = BattleCompanion.tamed(
                0x6000_0048L, 45207, 2158, "$1791", 26,
                137, 200, 20, -20, 17, 17, 100, 120);
        assertEquals(177, companion.heal(40));
        assertEquals(200, companion.heal(80));
        assertEquals(200, companion.snapshot().hp());
        companion.deactivate();
        assertEquals(200, companion.heal(40));
        assertFalse(companion.snapshot().active());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> companion.heal(-1));
    }

    @Test
    void createdZombieUsesLevelSelectedTemplateAtFullVitals() {
        BattleSpellCatalog.ZombieTemplate template =
                BattleSpellCatalog.zombieTemplate(1, 47);
        SourceNpcPhysicalStats stats = SourceNpcPhysicalStats.memoryFixture(
                template.npcId());
        BattleCompanion companion = BattleCompanion.zombie(
                0x6000_0047L, template, stats, 6, 100, 120);

        BattleCompanion.Snapshot snapshot = companion.snapshot();
        assertTrue(snapshot.active());
        assertEquals(81186, snapshot.npcId());
        assertEquals(52, snapshot.gfx());
        assertEquals("$57", snapshot.name());
        assertEquals(16, snapshot.level());
        assertEquals(500, snapshot.hp());
        assertEquals(500, snapshot.maxHp());
        assertEquals(12, snapshot.mp());
        assertEquals(12, snapshot.maxMp());
        assertEquals(8, snapshot.armorClass());
        assertEquals(16, snapshot.strength());
        assertEquals(6, snapshot.dexterity());
        assertEquals(6, snapshot.petCost());
    }

    @Test
    void summonedProfileUsesExactSourceIdentityHasteCostAndLifetime() {
        BattleSpellCatalog.SummonChoice choice =
                BattleSpellCatalog.summonChoice(17);
        SourceSummonTemplate template = SourceSummonTemplate.memoryFixture(81238);
        long nowNanos = 123_456L;
        BattleCompanion companion = BattleCompanion.summoned(
                0x6000_0049L, choice, template, choice.effectivePetCost(),
                1_800, 100, 120, nowNanos);

        BattleCompanion.Snapshot snapshot = companion.snapshot();
        assertTrue(snapshot.active());
        assertEquals(BattleCompanion.Kind.SUMMON, snapshot.kind());
        assertTrue(snapshot.kind().classicWorldEligible());
        assertFalse(snapshot.kind().earnsExperience());
        assertEquals(BattleCompanion.Posture.AGGRESSIVE, snapshot.posture());
        assertEquals(81238, snapshot.npcId());
        assertEquals(2389, snapshot.gfx());
        assertEquals("$1554", snapshot.name());
        assertEquals(58, snapshot.level());
        assertEquals(1000, snapshot.hp());
        assertEquals(100, snapshot.mp());
        assertEquals(-30, snapshot.armorClass());
        assertEquals(20, snapshot.strength());
        assertEquals(18, snapshot.dexterity());
        assertEquals(BattleTimedStatus.HASTE, snapshot.moveSpeed());
        assertEquals(49, snapshot.petCost());
        assertEquals(nowNanos + 1_800_000_000_000L,
                snapshot.expiresAtNanos());
        assertFalse(companion.expired(snapshot.expiresAtNanos() - 1));
        assertTrue(companion.expired(snapshot.expiresAtNanos()));
    }

    @Test
    void persistentPetKeepsCollarProgressAndCorpseUntilResurrection() {
        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3810L, 0x9000_0001L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120);

        BattleCompanion.Snapshot initial = pet.snapshot();
        assertEquals(BattleCompanion.Kind.PET, initial.kind());
        assertTrue(initial.kind().earnsExperience());
        assertEquals(durable.collarObjectId(), initial.collarObjectId());
        assertEquals(21_375, initial.experience());
        assertEquals("0", initial.anicomArguments().get(9));

        BattleCompanion.Damage death = pet.applyDamage(Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        assertTrue(death.after().active());
        assertEquals(0, death.after().hp());
        assertFalse(pet.selectTarget(123));
        assertTrue(pet.resurrect(39));
        assertEquals(39, pet.snapshot().hp());

        pet.updatePetProgress(13, 84, 19, 24_000);
        GameStateStore.PetState progressed = pet.durablePetState();
        assertEquals(13, progressed.level());
        assertEquals(84, progressed.maxHp());
        assertEquals(19, progressed.maxMp());
        assertEquals(24_000, progressed.experience());
        assertEquals(durable.collarObjectId(), progressed.collarObjectId());
    }

    @Test
    void persistentPetEvolutionReplacesTemplateCollarAndClearsBag() {
        GameStateStore.PetState durable = new GameStateStore.PetState(
                "legacy\u001f10231", 0x7000_3810L, 0x9000_0001L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 30, 120, 24, 60_000, 0);
        BattleCompanion pet = BattleCompanion.pet(
                durable, SourcePetTemplate.memoryFixture(45034), 100, 120);
        RuntimeInventory.Item fruit = new RuntimeInventory.Item(
                0x7000_7777L, InventoryCatalog.require(
                        InventoryCatalog.EVOLUTION_FRUIT), 2, false);
        assertTrue(pet.receiveItem(fruit, 2));

        GameStateStore.PetState evolved = new GameStateStore.PetState(
                durable.sourceKey(), 0x7000_3812L, durable.objectId(),
                (int) InventoryCatalog.HIGH_PET_COLLAR, 45687,
                durable.name(), 1, 60, 12, 0, durable.lawful());
        BattleCompanion.Snapshot after = pet.evolve(
                evolved, SourcePetTemplate.memoryFixture(45687));

        assertEquals(45687, after.npcId());
        assertEquals(3184, after.gfx());
        assertEquals(1, after.level());
        assertEquals(60, after.hp());
        assertEquals(12, after.mp());
        assertEquals(evolved.collarObjectId(), after.collarObjectId());
        assertEquals(InventoryCatalog.HIGH_PET_COLLAR,
                after.collarCatalogId());
        assertTrue(pet.carriedItems().isEmpty());
        assertEquals(evolved, pet.durablePetState());
    }

    @Test
    void authoritativeDamageClaimsSummonDeathExactlyOnce() {
        BattleCompanion companion = BattleCompanion.summoned(
                0x6000_0050L, BattleSpellCatalog.summonChoice(11),
                SourceSummonTemplate.memoryFixture(81222), 6,
                1_800, 100, 120, 123_456L);
        BattleCompanion.Damage first = companion.applyDamage(7);
        assertEquals(7, first.appliedDamage());
        assertFalse(first.diedNow());
        assertTrue(first.after().active());

        BattleCompanion.Damage death = companion.applyDamage(Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        assertEquals(0, death.after().hp());
        assertFalse(death.after().active());
        assertEquals(0, death.after().targetObjectId());

        BattleCompanion.Damage replay = companion.applyDamage(1);
        assertEquals(0, replay.appliedDamage());
        assertFalse(replay.diedNow());
    }

    @Test
    void eraseMagicUsesL1CharacterQuarterMrLeaseAndClearsOnLifecycle() {
        BattleCompanion companion = new BattleCompanion(0x6000_0153L);
        long now = 10_000L;
        companion.resetForBattle(100, 120);

        assertTrue(companion.activateEraseMagic(now, 32));
        assertEquals(25, companion.effectiveMagicResistance(103, now));
        BattleEraseMagicStatus.Lease first = companion.captureEraseMagic(now);
        assertTrue(first.present());

        // A refresh replaces the exact timer identity; the stale hit cannot
        // delete the later status (retained L1SkillTimer ABA behaviour).
        assertTrue(companion.activateEraseMagic(now + 1, 32));
        assertFalse(companion.consumeEraseMagic(first));
        assertEquals(25, companion.effectiveMagicResistance(103, now + 1));
        assertTrue(companion.consumeEraseMagic(now + 1));
        assertEquals(103, companion.effectiveMagicResistance(103, now + 1));

        assertTrue(companion.activateEraseMagic(now + 2, 32));
        companion.resetForBattle(101, 121);
        assertEquals(103, companion.effectiveMagicResistance(103, now + 2));

        assertTrue(companion.activateEraseMagic(now + 3, 32));
        companion.applyDamage(Integer.MAX_VALUE);
        assertEquals(103, companion.effectiveMagicResistance(103, now + 3));
    }

    @Test
    void lesserElementalUsesSelectedNpcHastePetCostAndSummonLifetime() {
        long now = 123_456_789L;
        SourceSummonTemplate earth = SourceSummonTemplate.memoryFixture(45306);
        BattleCompanion companion = BattleCompanion.lesserElemental(
                0x6000_0154L, earth, 25, 1_800, 100, 120, now);

        BattleCompanion.Snapshot snapshot = companion.snapshot();
        assertEquals(BattleCompanion.Kind.SUMMON, snapshot.kind());
        assertEquals(45306, snapshot.npcId());
        assertEquals(2273, snapshot.gfx());
        assertEquals("$2007", snapshot.name());
        assertEquals(30, snapshot.level());
        assertEquals(400, snapshot.hp());
        assertEquals(0, snapshot.mp());
        assertEquals(-15, snapshot.armorClass());
        assertEquals(16, snapshot.strength());
        assertEquals(18, snapshot.dexterity());
        assertEquals(BattleTimedStatus.HASTE, snapshot.moveSpeed());
        assertEquals(25, snapshot.petCost());
        assertEquals(now + 1_800_000_000_000L,
                snapshot.expiresAtNanos());
        assertEquals(BattleCompanion.Posture.AGGRESSIVE, snapshot.posture());
        assertFalse(companion.expired(snapshot.expiresAtNanos() - 1));
        assertTrue(companion.expired(snapshot.expiresAtNanos()));

        assertEquals(45303, SourceSummonTemplate.memoryFixture(45303).npcId());
        assertEquals(45304, SourceSummonTemplate.memoryFixture(45304).npcId());
        assertEquals(45305, SourceSummonTemplate.memoryFixture(45305).npcId());
    }

    @Test
    void greaterElementalUsesSelectedNpcHastePetCostAndSummonLifetime() {
        long now = 223_456_789L;
        SourceSummonTemplate earth = SourceSummonTemplate.memoryFixture(81053);
        BattleCompanion companion = BattleCompanion.greaterElemental(
                0x6000_0162L, earth, 25, 1_800, 100, 120, now);

        BattleCompanion.Snapshot snapshot = companion.snapshot();
        assertEquals(BattleCompanion.Kind.SUMMON, snapshot.kind());
        assertEquals(81053, snapshot.npcId());
        assertEquals(2267, snapshot.gfx());
        assertEquals("$2011", snapshot.name());
        assertEquals(45, snapshot.level());
        assertEquals(650, snapshot.hp());
        assertEquals(60, snapshot.mp());
        assertEquals(-18, snapshot.armorClass());
        assertEquals(19, snapshot.strength());
        assertEquals(18, snapshot.dexterity());
        assertEquals(BattleTimedStatus.HASTE, snapshot.moveSpeed());
        assertEquals(25, snapshot.petCost());
        assertEquals(now + 1_800_000_000_000L,
                snapshot.expiresAtNanos());
        assertEquals(BattleCompanion.Posture.AGGRESSIVE, snapshot.posture());
        assertFalse(companion.expired(snapshot.expiresAtNanos() - 1));
        assertTrue(companion.expired(snapshot.expiresAtNanos()));

        assertEquals(81050, SourceSummonTemplate.memoryFixture(81050).npcId());
        assertEquals(81051, SourceSummonTemplate.memoryFixture(81051).npcId());
        assertEquals(81052, SourceSummonTemplate.memoryFixture(81052).npcId());
    }
}
