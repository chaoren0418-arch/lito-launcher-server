package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateCharacterTest {
    @TempDir Path temporary;

    @Test void persistsCreatedCharactersOutsideTheLegacyRepository() throws Exception {
        Path file = temporary.resolve("state.properties");
        Protocol.CustomCharacterRequest request = new Protocol.CustomCharacterRequest(
                "NativeHero", 0, 4, List.of(12, 15, 8, 10, 9, 11), 0, 0);

        GameStateStore first = new GameStateStore(file, true);
        GameStateStore.CharacterCreation created = first.createCharacter("test", request, 1);
        assertTrue(created.accepted());
        assertEquals(0x6000_0000L, created.character().objectId());
        assertTrue(file.toFile().isFile());

        GameStateStore reloaded = new GameStateStore(file, true);
        List<CharacterSummary> characters = reloaded.createdCharacters("TEST");
        assertEquals(1, characters.size());
        assertEquals("NativeHero", characters.get(0).name());
        assertEquals(0x6000_0000L, characters.get(0).objectId());

        reloaded.saveBookmark("NativeHero",
                new GameStateStore.Bookmark("Arena Gate", 32730, 32798, 509));
        GameStateStore withBookmark = new GameStateStore(file, true);
        assertEquals(List.of(new GameStateStore.Bookmark("Arena Gate", 32730, 32798, 509)),
                withBookmark.bookmarks("NativeHero"));
        withBookmark.deleteBookmark("NativeHero", "arena gate");
        assertTrue(new GameStateStore(file, true).bookmarks("NativeHero").isEmpty());

        GameStateStore.CharacterCreation duplicate = withBookmark.createCharacter(
                "test2", request, 1);
        assertFalse(duplicate.accepted());
        assertEquals(GameStateStore.CreationFailure.DUPLICATE_NAME, duplicate.failure());
    }

    @Test void atomicallyDeletesOnlyOwnedSidecarCharacterAndItsProfile() throws Exception {
        Path file = temporary.resolve("delete-state.properties");
        Protocol.CustomCharacterRequest request = new Protocol.CustomCharacterRequest(
                "DeleteHero", 0, 4, List.of(12, 15, 8, 10, 9, 11), 0, 0);
        GameStateStore state = new GameStateStore(file, true);
        assertTrue(state.createCharacter("owner", request, 0).accepted());
        state.saveBookmark("DeleteHero",
                new GameStateStore.Bookmark("Delete Me", 32730, 32798, 509));

        GameStateStore.CharacterDeletion foreign =
                state.deleteCreatedCharacter("intruder", "DeleteHero");
        assertEquals(GameStateStore.DeletionStatus.NOT_OWNED, foreign.status());
        assertEquals(1, state.createdCharacters("owner").size());

        GameStateStore.CharacterDeletion deleted =
                state.deleteCreatedCharacter("OWNER", "deletehero");
        assertTrue(deleted.deleted());
        assertEquals("DeleteHero", deleted.character().name());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertTrue(reloaded.createdCharacters("owner").isEmpty());
        assertFalse(reloaded.createdCharacterExists("DeleteHero"));
        assertTrue(reloaded.bookmarks("DeleteHero").isEmpty());

        GameStateStore.CharacterDeletion legacy =
                reloaded.deleteCreatedCharacter("owner", "TestKnight");
        assertEquals(GameStateStore.DeletionStatus.NOT_FOUND, legacy.status());
    }

    @Test void sqlBookmarkReconciliationReplacesTheSidecarSnapshotExactly() throws Exception {
        Path file = temporary.resolve("sql-bookmarks.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.saveBookmark("SharedHero",
                new GameStateStore.Bookmark("Old Sidecar", 32730, 32798, 4));

        List<GameStateStore.Bookmark> sql = List.of(
                new GameStateStore.Bookmark("Aden", 33970, 33245, 4),
                new GameStateStore.Bookmark("Kent", 33080, 32764, 4));
        state.reconcileSqlBookmarks("SharedHero", sql);
        assertEquals(sql, state.bookmarks("SharedHero"));
        assertEquals(sql, new GameStateStore(file, true).bookmarks("SharedHero"));

        assertThrows(IllegalArgumentException.class, () -> state.reconcileSqlBookmarks(
                "SharedHero", List.of(
                        new GameStateStore.Bookmark("Aden", 1, 1, 4),
                        new GameStateStore.Bookmark("ADEN", 2, 2, 4))));
        assertEquals(sql, state.bookmarks("SharedHero"));
    }

    @Test void sqlTimedBuffHookPublishesWholeSnapshotAndSuppressesReadonlySidecar()
            throws Exception {
        Path file = temporary.resolve("sql-timed-buffs.properties");
        String character = GameStateStore.sourceCharacter("380 route", "BuffHero");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.TimedBuffSnapshot initial = new GameStateStore.TimedBuffSnapshot(
                new GameStateStore.TimedExperienceBuff(3_000, 500),
                new GameStateStore.TimedFireWeaponBuff(900),
                GameStateStore.TimedBurningWeaponBuff.none(),
                GameStateStore.TimedWindShotBuff.none(),
                GameStateStore.TimedFireBlessBuff.none(),
                GameStateStore.TimedStormEyeBuff.none(),
                GameStateStore.TimedStormShotBuff.none(),
                GameStateStore.TimedEarthBlessBuff.none(),
                GameStateStore.TimedIronSkinBuff.none(),
                GameStateStore.TimedResistFearBuff.none(),
                GameStateStore.TimedMovementBuff.none(),
                GameStateStore.TimedArmorProtection.none());
        state.reconcileSqlTimedBuffs(character, initial);

        List<GameStateStore.TimedBuffSnapshot> published = new ArrayList<>();
        GameStateStore.TimedBuffPersistenceHook writable = (ignored, replacement) -> {
            published.add(replacement);
            return true;
        };
        state.registerTimedBuffPersistence(character, writable);
        state.saveTimedFireWeaponBuff(
                character, new GameStateStore.TimedFireWeaponBuff(800));
        assertEquals(1, published.size());
        assertEquals(3_000, published.get(0).experience().bonusExperience());
        assertEquals(800, published.get(0).fireWeapon().remainingSeconds());
        assertEquals(800, state.timedFireWeaponBuff(character).remainingSeconds());

        GameStateStore.TimedBuffPersistenceHook readOnly =
                (ignored, replacement) -> false;
        state.registerTimedBuffPersistence(character, readOnly);
        state.saveTimedFireWeaponBuff(
                character, new GameStateStore.TimedFireWeaponBuff(700));
        assertEquals(800, state.timedFireWeaponBuff(character).remainingSeconds());

        state.unregisterTimedBuffPersistence(character, readOnly);
        state.saveTimedFireWeaponBuff(
                character, new GameStateStore.TimedFireWeaponBuff(700));
        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(3_000, reloaded.timedExperienceBuff(character).bonusExperience());
        assertEquals(700, reloaded.timedFireWeaponBuff(character).remainingSeconds());
    }

    @Test void tournamentResultsNeverMutateCharacterLawfulSourceState() throws Exception {
        Path file = temporary.resolve("result-lawful-boundary.properties");
        Protocol.CustomCharacterRequest request = new Protocol.CustomCharacterRequest(
                "LawfulHero", 0, 4, List.of(12, 15, 8, 10, 9, 11), 0, 0);
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.CharacterCreation created = state.createCharacter("owner", request, 0);
        assertTrue(created.accepted());
        assertEquals(0, created.character().lawful());
        assertEquals(0, state.reconcileLawful("LawfulHero", 0).effective());
        assertEquals(22, state.addLawful("LawfulHero", 0, 22).after());

        GameStateStore.Progress win = state.recordResult("LawfulHero", true, 10);
        GameStateStore.Progress loss = state.recordResult("LawfulHero", false, 5);
        GameStateStore.Progress disconnected =
                state.recordResult("LawfulHero", false, true, 5);
        assertEquals(1, win.wins());
        assertEquals(1, loss.losses());
        assertEquals(2, disconnected.losses());
        assertEquals(1, disconnected.disconnects());

        GameStateStore.TournamentStatistics statistics =
                new GameStateStore(file, true).tournamentStatistics("LawfulHero");
        assertEquals(3, statistics.winRateDenominator());
        assertEquals(1, statistics.wins());
        assertEquals(2, statistics.losses());
        assertEquals(1, statistics.disconnects());
        assertEquals(22, new GameStateStore(file, true)
                .reconcileLawful("LawfulHero", 0).effective());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("disconnects=1"));
        assertThrows(IllegalArgumentException.class,
                () -> state.recordResult("LawfulHero", true, true, 10));

        CharacterSummary reloaded = new GameStateStore(file, true)
                .createdCharacters("owner").get(0);
        assertEquals(0, reloaded.lawful());
    }

    @Test void lawfulOverlayIsDurableClampedAndReconcilesExternalSourceChanges()
            throws Exception {
        Path file = temporary.resolve("lawful-overlay.properties");
        String identity = GameStateStore.sourceCharacter("Sun", "NativeLawful");
        GameStateStore state = new GameStateStore(file, true);

        GameStateStore.LawfulBalance initial = state.reconcileLawful(identity, -100);
        assertEquals(-100, initial.observedSource());
        assertEquals(-100, initial.effective());
        GameStateStore.LawfulMutation gain = state.addLawful(identity, -100, 25);
        assertEquals(-100, gain.before());
        assertEquals(-75, gain.after());
        assertEquals(25, gain.appliedDelta());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(-75, reloaded.reconcileLawful(identity, -100).effective());
        // A changed read-only source row is authoritative and resets the overlay.
        assertEquals(300, reloaded.reconcileLawful(identity, 300).effective());
        GameStateStore.LawfulMutation upper = reloaded.addLawful(
                identity, 300, Integer.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, upper.after());
        GameStateStore.LawfulMutation lower = reloaded.addLawful(
                identity, 300, Integer.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, lower.after());

        GameStateStore finalReload = new GameStateStore(file, true);
        assertEquals(Short.MIN_VALUE,
                finalReload.reconcileLawful(identity, 300).effective());
        assertThrows(IllegalArgumentException.class,
                () -> finalReload.reconcileLawful(identity, Short.MAX_VALUE + 1));
        String durable = Files.readString(file);
        assertTrue(durable.contains("lawful.present=true"));
        assertTrue(durable.contains("lawful.observedSource=300"));
        assertTrue(durable.contains("lawful.effective=-32768"));
    }

    @Test void classicExperienceOverlayIsDurablePenalizedAndSourceReconciled()
            throws Exception {
        Path file = temporary.resolve("classic-experience-overlay.properties");
        String identity = GameStateStore.sourceCharacter("Sun", "NativeExp");
        GameStateStore state = new GameStateStore(file, true);

        GameStateStore.ClassicExperienceBalance initial =
                state.reconcileClassicExperience(identity, 234_276, 22);
        assertEquals(234_276, initial.effectiveExperience());
        assertEquals(22, initial.effectiveLevel());
        GameStateStore.ClassicExperienceMutation gain =
                state.addClassicExperience(identity, 234_276, 22, 5);
        assertEquals(234_281, gain.afterExperience());
        assertEquals(22, gain.afterLevel());
        assertEquals(5, gain.adjustedExperience());

        GameStateStore.ClassicDeathPenaltyMutation death =
                state.applyClassicDeathPenalty(identity, 234_276, 22);
        assertEquals(234_281, death.beforeExperience());
        assertEquals(229_723, death.afterExperience());
        assertEquals(4_558, death.requestedPenalty());
        assertEquals(4_558, death.appliedPenalty());
        assertEquals(21, death.afterLevel());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(229_723, reloaded.reconcileClassicExperience(
                identity, 234_276, 22).effectiveExperience());
        // An externally changed read-only character row resets the overlay.
        assertEquals(279_861, reloaded.reconcileClassicExperience(
                identity, 279_861, 23).effectiveExperience());
        GameStateStore.ClassicExperienceMutation levelUp =
                reloaded.addClassicExperience(identity, 279_861, 23,
                        ClassicExperienceCatalog.minimumExperienceForLevel(24) - 279_861);
        assertEquals(24, levelUp.afterLevel());

        // Retain synthetic memory-fixture level when its old EXP=0 is inconsistent.
        GameStateStore.ClassicExperienceMutation fixture = reloaded.addClassicExperience(
                GameStateStore.sourceCharacter("Memory", "Fixture"), 0, 52, 5);
        assertEquals(52, fixture.afterLevel());
        assertEquals(2, fixture.adjustedExperience());
        // CalcExp keeps RateXp and penalty in one double expression before
        // truncation: 1 * 3.0 * 0.5 is one, not a pre-truncated zero.
        GameStateStore.ClassicExperienceMutation ratedFixture =
                reloaded.addClassicExperience(
                        GameStateStore.sourceCharacter("Memory", "RateFixture"),
                        0, 52, 1, 3.0);
        assertEquals(1, ratedFixture.adjustedExperience());
        GameStateStore.ClassicDeathPenaltyMutation novice =
                reloaded.applyClassicDeathPenalty(
                        GameStateStore.sourceCharacter("Memory", "Novice"),
                        ClassicExperienceCatalog.minimumExperienceForLevel(10), 10);
        assertFalse(novice.changed());
        assertEquals(0, novice.requestedPenalty());
        String durable = Files.readString(file);
        assertTrue(durable.contains("classicExperience.present=true"));
        assertTrue(durable.contains("classicExperience.observedSourceLevel=23"));
    }

    @Test void sidecarCharacterNamesAreUniquePerOriginalServerNamespace() throws Exception {
        Path file = temporary.resolve("source-characters.properties");
        Protocol.CustomCharacterRequest request = new Protocol.CustomCharacterRequest(
                "SameHero", 0, 4, List.of(12, 15, 8, 10, 9, 11), 0, 0);
        String sunOwner = GameStateStore.sourceCharacter("Sun", "owner1");
        String sunOther = GameStateStore.sourceCharacter("Sun", "owner2");
        String moonOwner = GameStateStore.sourceCharacter("Moon", "owner1");
        GameStateStore state = new GameStateStore(file, true);

        assertTrue(state.createCharacter(sunOwner, request, 0).accepted());
        assertFalse(state.createCharacter(sunOther, request, 0).accepted());
        assertTrue(state.createCharacter(moonOwner, request, 0).accepted());
        assertTrue(state.createdCharacterExists(sunOwner, "SameHero"));
        assertTrue(state.createdCharacterExists(moonOwner, "SameHero"));

        state = new GameStateStore(file, true);
        assertEquals(1, state.createdCharacters(sunOwner).size());
        assertEquals(1, state.createdCharacters(moonOwner).size());

        assertTrue(state.deleteCreatedCharacter(sunOwner, "SameHero").deleted());
        assertFalse(state.createdCharacterExists(sunOwner, "SameHero"));
        assertTrue(state.createdCharacterExists(moonOwner, "SameHero"));
        state = new GameStateStore(file, true);
        assertEquals(0, state.createdCharacters(sunOwner).size());
        assertEquals(1, state.createdCharacters(moonOwner).size());
    }
}
