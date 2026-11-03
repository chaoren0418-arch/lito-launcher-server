package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Opt-in real, SELECT-only combat-source qualification. No store, migration,
 * listener, match, HP mutation or attack. Uses the actual source-bound provider;
 * the connector seam only checks SELECT DATABASE()/readOnly, never replaces the
 * source/catalog read. Output is aggregate numeric evidence, not player identity.
 */
public final class ModernCombatReadProbe {
    private ModernCombatReadProbe() {}
    public static void main(String[] args) {
        int exit = 2;
        char[] password = null;
        String stage = "CONFIGURATION";
        try {
            if (args.length != 1) throw new IllegalArgumentException("explicit configuration required");
            Path file = Path.of(args[0]);
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String secret = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (account == null || secret == null) throw new IllegalArgumentException("explicit probe credentials required");
            password = secret.toCharArray();
            try (var settings = ModernDatabaseReadProbe.load(file);
                 var config = ModernDatabaseReadProbe.modernConfig(settings, ModernCharacterDataReadProbe.catalogDatabase(file));
                 var executor = new ModernBackendExecutor(1, 1);
                 var gate = executor.openSession()) {
                try {
                    long characters = 0, items = 0, weapons = 0, equipped = 0, wielded = 0;
                    long minHp = Long.MAX_VALUE, maxHp = Long.MIN_VALUE;
                    long minAc = Long.MAX_VALUE, maxAc = Long.MIN_VALUE;
                    for (var source : config.sources()) {
                        var audit = new ModernEquipmentReadProbe.Audit();
                        var dao = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), config.dbNetworkMillis(),
                                (url, properties) -> ModernEquipmentReadProbe.inspectOwned(
                                        new com.mysql.cj.jdbc.Driver().connect(url, properties), ModernBackendScope.current(),
                                        source.database().database(), source.catalogDatabase(), config.dbNetworkMillis(), null, audit, null));
                        var verifier = ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(), dao::authenticate)), ignored -> false);
                        stage = "AUTHENTICATION";
                        var principal = ModernEquipmentRepositoryReadProbe.authenticate(gate, verifier, account, password);
                        stage = "ROSTER";
                        var rows = gate.call(5000, scope -> dao.characters(principal.accountForSource(source.id())));
                        var provider = ModernEquipmentRepository.combatJdbc(Map.of(source.id(), new ModernEquipmentRepository.Source(source, dao)));
                        for (var row : rows) {
                            stage = "SOURCE_QUALIFICATION";
                            var result = gate.call(5000, scope -> {
                                var loaded = provider.read(principal, source.id(), row.objectId());
                                var input = loaded.input();
                                if (!input.weaponsQualified() || input.equipment().source().sourceId() != source.id()
                                        || !input.equipment().source().character().equals(row)) throw new IllegalStateException("source changed");
                                var binding = new ModernEquipmentReconciliation.ExpectedBinding(
                                        new TournamentPresence.CharacterKey(source.id(), row.objectId()), loaded.ownerBinding());
                                var profile = ModernCombatProfile.qualify(input, binding, Optional.empty(), System.currentTimeMillis(),
                                        ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
                                scope.checkActive();
                                return profile;
                            });
                            stage = "VERIFY";
                            var saved = result.source().persisted();
                            var values = result.effective();
                            if (result.attackImplemented() || result.equipmentRevision() != 0
                                    || result.weapon().state() == ModernCombatProfile.WeaponState.UNQUALIFIED
                                    || values.armorClass() != saved.armorClass()
                                    || values.maximumHp() != Math.addExact(saved.maxHp(), result.equipmentEvaluation().total(ModernEquipmentFacts.Stat.MAX_HP))
                                    || values.maximumMp() != Math.addExact(saved.maxMp(), result.equipmentEvaluation().total(ModernEquipmentFacts.Stat.MAX_MP))
                                    || values.initialHp() != values.maximumHp() || values.initialMp() != values.maximumMp()) {
                                throw new IllegalStateException("effective combat input mismatch");
                            }
                            characters++;
                            items += result.source().inventory().size();
                            weapons += result.source().inventory().stream().filter(i -> i.template().kind() == ModernCharacterData.ItemKind.WEAPON).count();
                            equipped += result.equipmentEvaluation().equippedRows();
                            if (result.weapon().state() == ModernCombatProfile.WeaponState.QUALIFIED) wielded++;
                            minHp = Math.min(minHp, values.maximumHp()); maxHp = Math.max(maxHp, values.maximumHp());
                            minAc = Math.min(minAc, values.armorClass()); maxAc = Math.max(maxAc, values.armorClass());
                        }
                    }
                    if (characters == 0) throw new IllegalStateException("no character verified");
                    stage = "RETIREMENT";
                    executor.close();
                    if (!executor.awaitTermination(5000)) throw new IllegalStateException("worker not retired");
                    System.out.println("{\"phase\":\"production-combat-source\",\"status\":\"OK\",\"sources\":"+config.sources().size()
                            +",\"characters\":"+characters+",\"items\":"+items+",\"weapon_rows\":"+weapons+",\"equipped\":"+equipped
                            +",\"qualified_wielded_weapons\":"+wielded+",\"min_effective_hp\":"+minHp+",\"max_effective_hp\":"+maxHp
                            +",\"min_effective_ac\":"+minAc+",\"max_effective_ac\":"+maxAc
                            +",\"saved_ac_not_added_twice\":true,\"store_opened\":false,\"attack_implemented\":false,\"atomic_generation\":false}");
                    exit = 0;
                } finally {
                    executor.close();
                    if (!executor.awaitTermination(5000)) throw new IllegalStateException("worker not retired");
                }
            }
        } catch (Exception failure) {
            // Driver messages/causes can include credentials, SQL and paths.
            String reason = failure instanceof ModernBackendExecutor.BackendException backend ? backend.failure().name() : "INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-combat-source\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
            exit = 2;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
        if (exit != 0) System.exit(exit);
    }
}
