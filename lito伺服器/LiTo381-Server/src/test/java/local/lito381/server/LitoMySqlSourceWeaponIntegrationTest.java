package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Read-only acceptance against the operator's retained 3333 source character. */
final class LitoMySqlSourceWeaponIntegrationTest {
    private static final String ACCOUNT = "3333";
    private static final String PASSWORD = "3333";
    private static final String CHARACTER = "3332";
    private static final long CHARACTER_OBJECT_ID = 10_275L;

    @Test
    void princeCanQualifyEveryRetainedSamuraiSwordIncludingExtendedEnchant(
            @TempDir Path temporary) throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        ServerConfig config = ServerConfig.load(Path.of("config", "server.properties"));
        String source = config.classicServerNames().get(0);
        try (AccountRepository routed = AccountRepository.open(config)) {
            AuthResult authentication = routed.authenticate(ACCOUNT, PASSWORD);
            assertTrue(authentication.accepted());
            assertTrue(authentication.authorizesClassicSource(source));
            AccountRepository repository = routed.repositoryForClassicSource(source);
            CharacterSummary character = repository.characters(ACCOUNT).stream()
                    .filter(candidate -> candidate.name().equals(CHARACTER))
                    .findFirst().orElseThrow();
            assertEquals(CHARACTER_OBJECT_ID, character.objectId());
            assertEquals(0, character.type(), "3332 must remain a Prince");

            List<SourceInventoryItem> sourceInventory =
                    repository.sourceInventory(character.objectId());
            Map<Long, ExpectedWeapon> expected = Map.of(
                    10_295L, new ExpectedWeapon(10, 2, 9),
                    10_296L, new ExpectedWeapon(10, 2, 999),
                    10_303L, new ExpectedWeapon(41, 1, 0));
            for (Map.Entry<Long, ExpectedWeapon> entry : expected.entrySet()) {
                SourceInventoryItem sourceItem = sourceInventory.stream()
                        .filter(item -> item.sourceObjectId() == entry.getKey())
                        .findFirst().orElseThrow();
                ExpectedWeapon expectedWeapon = entry.getValue();
                assertEquals(expectedWeapon.itemId(), sourceItem.catalogId());
                assertEquals(expectedWeapon.enchantLevel(),
                        sourceItem.sourceEnchantLevel());
                assertEquals("weapon", sourceItem.wire().kind());
                assertTrue((sourceItem.wire().classMask()
                        & (1 << character.type())) != 0,
                        "source class mask must allow this Prince");

                Map<Long, Boolean> overrides = new LinkedHashMap<>();
                sourceInventory.stream()
                        .filter(item -> item.wire().kind().equals("weapon"))
                        .forEach(item -> overrides.put(item.sourceObjectId(), false));
                overrides.put(sourceItem.sourceObjectId(), true);
                SourceWeaponPhysicalStats physical = repository.equipmentEvaluation(
                        character.objectId(), overrides).magic().physical().weapon();
                assertTrue(physical.present());
                assertEquals(expectedWeapon.itemId(), physical.itemId());
                assertEquals(expectedWeapon.weaponType(), physical.weaponType());
                assertEquals(expectedWeapon.enchantLevel(), physical.enchantLevel());

                String identity = GameStateStore.sourceCharacter(source, CHARACTER);
                GameStateStore state = new GameStateStore(
                        temporary.resolve("weapon-" + entry.getKey() + ".properties"), true);
                GameStateStore.SourceInventoryReconciliation imported =
                        state.reconcileSourceInventory(identity, sourceInventory,
                                repository.equipmentEvaluation(character.objectId()).magic());
                RuntimeInventory runtime = RuntimeInventory.fromDurableState(
                        imported.economy().inventory(), imported.economy().itemStates(),
                        imported.definitions(), imported.sourceWeapons());
                long localObjectId = state.sourceInventoryBindings(identity).stream()
                        .filter(binding -> binding.sourceObjectId() == entry.getKey())
                        .findFirst().orElseThrow().localObjectId();
                RuntimeInventory candidate = runtime.copy();
                assertFalse(candidate.hasQualifiedPhysicalWeapon(localObjectId));
                assertFalse(candidate.toggleSourceWeaponForEvaluation(
                        localObjectId).isEmpty());
                candidate.qualifySourceWeapon(localObjectId, physical);
                assertNotNull(candidate.equippedWeaponItem());
                assertEquals(localObjectId, candidate.equippedWeaponItem().objectId());
                assertEquals(expectedWeapon.enchantLevel(),
                        candidate.equippedPhysicalWeapon().enchantLevel());
                assertEquals(physical.walkAction(), candidate.equippedWalkAction());
            }
        }
    }

    private record ExpectedWeapon(int itemId, int weaponType, int enchantLevel) {}
}
