// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.io.IOException;
import java.io.Reader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

record ServerConfig(
   Path source,
   String bind,
   String gameBind,
   int port,
   List<Integer> channelPorts,
   String advertisedHost,
   int advertisedPort,
   String serverName,
   String originSourceId,
   String originSourceName,
   List<String> classicServerNames,
   List<Integer> classicServerIds,
   int maxFrame,
   int versionResponseDelayMs,
   int loginBootstrapInitialDelayMs,
   int loginBootstrapStepDelayMs,
   List<String> loginBootstrapSequence,
   List<Integer> loginNoticeMessageIds,
   String loginCommonNews,
   int serverSelectResponseDelayMs,
   boolean enterChannelResponseEnabled,
   int enterChannelResponseDelayMs,
   boolean characterListEnabled,
   boolean rentalTownDirectoryEnabled,
   long rentalTownDirectoryRoomId,
   String rentalTownDirectoryName,
   String rentalTownDirectoryHost,
   int rentalTownDirectoryPort,
   int rentalTownMapId,
   List<NativeLitoRentalTeleportEndpointCatalog.Endpoint> rentalTeleportEndpoints,
   Path rentalBattleHandoffFile,
   Path captureDirectory,
   Path mapDirectory,
   String databaseMode,
   Path legacySqlProperties,
   String legacyLoginDatabase,
   List<String> legacySourceDatabases,
   List<String> legacySourceResourceDatabases,
   List<Double> legacySourceWeightRates,
   boolean databaseReadOnly,
   String memoryAccount,
   String memoryPassword,
   String memoryAuthenticationFailureAccount,
   int memorySourceInventoryWeight,
   double memorySourceWeightRate,
   int memorySourceFood,
   int memorySourcePrimaryLevel,
   int memorySourcePrimaryMaxHp,
   int memorySourcePrimaryMaxMp,
   int memorySourcePrimaryArmorClass,
   int memorySourcePrimaryStrength,
   int memorySourcePrimaryConstitution,
   int memorySourcePrimaryDexterity,
   int memorySourcePrimaryCharisma,
   int memorySourcePrimaryIntelligence,
   int memorySourcePrimaryWisdom,
   int memorySourceSecondaryMaxMp,
   boolean memorySourceInventoryFixtureEnabled,
   boolean memorySourceProjectileFixtureEnabled,
   int memoryCastleWarFixtureStartDelaySeconds,
   int memoryCastleWarFixtureDurationMinutes,
   int memoryCastleWarFixtureIntervalMinutes,
   Path stateFile,
   boolean statePersistenceEnabled,
   boolean battleAiEnabled,
   boolean battleNaturalHpRegenEnabled,
   boolean battleNaturalMpRegenEnabled,
   boolean battlePhysicalHitEnabled,
   boolean battleCompatibilityServiceActorsEnabled,
   boolean classicWorldCompatibilityEnabled,
   boolean classicWorldMonsterAiEnabled,
   int classicWorldMonsterAiTickMs,
   boolean classicWorldMonsterAggressiveAiEnabled,
   int classicWorldPirateShipFixtureSecondOfDay,
   int classicWorldShipFixtureSecondOfDay,
   boolean classicWorldShipFixtureWindowOpening,
   int classicWorldMonsterPoisonFixtureRoll,
   int classicWorldMonsterPhysicalSkillFixtureBranch,
   int classicWorldMonsterPhysicalSkillFixtureTriggerRoll,
   int classicWorldMonsterPhysicalSkillFixtureLevelRoll,
   int classicWorldMonsterPhysicalSkillFixtureCriticalRoll,
   int classicWorldMonsterMagicFixtureBranch,
   int classicWorldMonsterMagicFixtureTriggerRoll,
   int classicWorldMonsterMagicFixtureDieRoll,
   int timeAttackDocumentedPatternFixtureIndex,
   int timeAttackDragonOutcomeFixtureIndex,
   boolean timeAttackDragonCombatFixtureOnly,
   int timeAttackDragonCombatFixtureMinimumTargets,
   int timeAttackMonsterActNoFixture,
   int timeAttackMonsterMagicFixtureTriggerRoll,
   int timeAttackMonsterPhysicalFixtureTriggerRoll,
   int timeAttackMonsterCandidateFixtureSelector,
   int timeAttackMonsterPhysicalFixtureLevelRoll,
   int timeAttackMonsterPhysicalFixtureCriticalRoll,
   int timeAttackMonsterPhysicalFixtureAcRoll,
   int timeAttackPlayerPhysicalDamageFixture,
   int classicWorldQuestDropFixtureRoll,
   int classicWorldDeathItemChanceFixtureRoll,
   int classicWorldDeathItemCountFixtureRoll,
   int classicWorldDeathItemSelectorFixtureIndex,
   int classicWorldDeathSkillChanceFixtureRoll,
   int classicWorldDeathSkillCountFixtureRoll,
   int classicWorldDeathSkillSelectorFixtureIndex,
   int classicWorldDropAutoLoot,
   int classicWorldDropLootingRange,
   double classicWorldDropItemRate,
   double classicWorldDropAdenaRate,
   boolean nativePlayerDropEnabled,
   double classicWorldExperienceRate,
   double classicWorldLawfulRate,
   int classicWorldScarecrowMaxLevel,
   int battleAiTickMs,
   int battleAiDamage,
   int deathmatchFatalityFixtureSlot,
   int battleWinReward,
   int battleLossReward,
   boolean nativePetRaceMinimumPlayersEnforced,
   boolean nativeSiegeRosterEnforced,
   boolean nativeTournamentEconomyEnabled,
   int nativeTournamentAllowance,
   boolean nativeIntegrityPreWorldProbe,
   boolean nativeTransferStoneProbeEnabled,
   boolean nativeCharacterEventProbeEnabled,
   boolean nativeSourceSelectionProbeEnabled,
   boolean attackAllProbeEnabled,
   boolean bookListProbeEnabled,
   boolean cloneAppearanceProbeEnabled,
   int repairFixtureWeaponDurability,
   boolean curseRemovalFixtureEnabled,
   boolean controlRingFixtureEnabled,
   boolean spellMaterialFixtureEnabled,
   boolean awakenValakasFixtureEnabled,
   boolean worldCancellationFixtureEnabled,
   boolean worldTamingLifecycleFixtureEnabled,
   boolean cursePoisonSpellFixtureEnabled,
   boolean sleepSpellFixtureEnabled,
   boolean freezeSpellFixtureEnabled,
   boolean blindHidingSpellFixtureEnabled,
   boolean enchantVenomSpellFixtureEnabled,
   boolean shadowArmorSpellFixtureEnabled,
   boolean bringStoneSpellFixtureEnabled,
   boolean movingAccelerationSpellFixtureEnabled,
   boolean burningSpiritSpellFixtureEnabled,
   boolean darkBlindSpellFixtureEnabled,
   boolean weaknessSpellFixtureEnabled,
   boolean silenceSpellFixtureEnabled,
   boolean decayPotionSpellFixtureEnabled,
   boolean venomResistSpellFixtureEnabled,
   boolean doubleBrakeSpellFixtureEnabled,
   boolean uncannyDodgeSpellFixtureEnabled,
   boolean mirrorImageSpellFixtureEnabled,
   boolean confusionSpellFixtureEnabled,
   boolean smashSpellFixtureEnabled,
   boolean mindBreakSpellFixtureEnabled,
   boolean boneBreakSpellFixtureEnabled,
   boolean illusionOgreSpellFixtureEnabled,
   boolean illusionLichSpellFixtureEnabled,
   boolean cubeIgnitionSpellFixtureEnabled,
   boolean cubeQuakeSpellFixtureEnabled,
   boolean cubeShockSpellFixtureEnabled,
   boolean cubeHarmonySpellFixtureEnabled,
   boolean insightSpellFixtureEnabled,
   boolean panicSpellFixtureEnabled,
   boolean joyOfPainSpellFixtureEnabled,
   boolean illusionAvatarSpellFixtureEnabled,
   boolean patienceSpellFixtureEnabled,
   boolean phantasmSpellFixtureEnabled,
   boolean shadowFangSpellFixtureEnabled,
   boolean finalBurnSpellFixtureEnabled,
   boolean dressMightySpellFixtureEnabled,
   boolean dressDexteritySpellFixtureEnabled,
   boolean dressEvasionSpellFixtureEnabled,
   boolean armorBreakSpellFixtureEnabled,
   boolean weaponBreakSpellFixtureEnabled,
   boolean chainswordWeaknessAdvanceFixtureEnabled,
   int enchantVenomProcFixtureRoll,
   int bringStoneFixtureRoll,
   int burningSpiritProcFixtureRoll,
   int elementalFireProcFixtureRoll,
   int darkBlindFixtureRoll,
   int weaknessFixtureDieRoll,
   int weaknessFixturePercentileRoll,
   int massSlowFixtureDieRoll,
   int massSlowFixturePercentileRoll,
   int sleepFixturePercentileRoll,
   int phantasmFixturePercentileRoll,
   int earthBindFixturePercentileRoll,
   int silenceFixturePercentileRoll,
   int decayPotionFixtureDieRoll,
   int decayPotionFixturePercentileRoll,
   int cursePoisonFixtureDieRoll,
   int cursePoisonFixturePercentileRoll,
   int curseParalysisFixturePercentileRoll,
   int tamingMonsterFixturePercentileRoll,
   int returnToNatureFixturePercentileRoll,
   int turnUndeadFixtureDieRoll,
   int turnUndeadFixturePercentileRoll,
   int manaDrainFixtureDieRoll,
   int manaDrainFixturePercentileRoll,
   int manaDrainFixtureTransferRoll,
   int doubleBrakeProcFixtureRoll,
   int uncannyDodgeHitFixtureD20,
   int uncannyDodgeHitFixtureD100,
   int rangedEvasionFixtureD100,
   int counterBarrierFixtureD100,
   int mortalBodyFixtureD100,
   int thunderGrabFixtureD100,
   int thunderGrabFixtureDurationOffset,
   int confusionFixtureD100,
   int mindBreakFixtureMpDrain,
   int boneBreakFixtureD1000,
   int shadowFangDamageFixtureWeaponRoll,
   int armorBreakFixtureD100,
   int guardBrakeFixtureD100,
   int horrorOfDeathFixtureD100,
   int freezingBreathFixtureD100,
   int resistFearFixtureD100,
   int weaponBreakFixtureD100,
   int weaponBreakFixtureDamageRoll,
   boolean warehouseFixtureEnabled,
   boolean letterFixtureEnabled,
   String repairNpcAppearanceSpeech,
   Set<String> administrationAccounts
) {
   static ServerConfig load(Path path) throws IOException {
      return load(path, null);
   }

   static ServerConfig load(Path path, String serviceMode) throws IOException {
      Path source = path.toAbsolutePath().normalize();
      Properties p = new Properties();

      try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
         p.load(reader);
         UnifiedConfiguration.loaded(p, path);
      }

      Path base = source.getParent();
      applyExternalSourceServers(p, base);
      applyServiceMode(p, serviceMode);
      String bind = value(p, "bind", "127.0.0.1");
      String gameBind = value(p, "game.bind", "127.0.0.2");
      requireLoopback("bind", bind);
      requireLoopback("game.bind", gameBind);
      if (bind.equals(gameBind)) {
         throw new IllegalArgumentException("bind and game.bind must differ");
      } else {
         int port = integer(p, "port", 32000, 1, 65535);
         String legacyChannelPorts = optional(p, "lito.channel.ports");
         if (legacyChannelPorts != null && !legacyChannelPorts.isBlank()) {
            throw new IllegalArgumentException("lito.channel.ports is no longer supported; all four Tournament lobbies share port=" + port);
         } else {
            List<Integer> channelPorts = List.of(port);
            String advertisedHost = value(p, "advertised.host", "127.0.0.1");
            int advertisedPort = integer(p, "advertised.port", port, 1, 65535);
            String rentalTownDirectoryHost = value(p, "lito.rental.town.directory.host", advertisedHost);
            int rentalTownDirectoryPort = integer(p, "lito.rental.town.directory.port", advertisedPort, 1, 65535);
            int maxFrame = integer(p, "max.frame", 8192, 64, 65535);
            Path capture = resolve(base, value(p, "capture.directory", "../runtime/captures"));
            List<String> loginBootstrapSequence = parseLoginBootstrapSequence(
               value(p, "login.bootstrap.sequence", "login_ok,time_limit,classic_server_list,lito_server_list")
            );
            List<Integer> loginNoticeMessageIds = parseLoginNoticeMessageIds(optional(p, "login.notice.message.ids"));
            String loginCommonNews = parseLoginCommonNews(optional(p, "login.common.news"));
            String serverName = value(p, "server.name", "LiTo 3.81C Local");
            String originSourceId = value(p, "origin.source.id", "local-development");
            if (!originSourceId.matches("[A-Za-z0-9._-]+")) {
               throw new IllegalArgumentException("origin.source.id contains unsupported characters");
            } else {
               String originSourceName = value(p, "origin.source.name", serverName);
               List<String> classicServerNames = parseServerNames(value(p, "login.classic.server.names", serverName));
               List<Integer> classicServerIds = parseClassicServerIds(optional(p, "login.classic.server.ids"), classicServerNames);
               String databaseMode = value(p, "database.mode", "legacy_mysql").toLowerCase(Locale.ROOT);
               if (!databaseMode.equals("memory") && !databaseMode.equals("legacy_mysql") && !databaseMode.equals("lito_mysql")) {
                  throw new IllegalArgumentException("unsupported database.mode: " + databaseMode);
               } else {
                  Path legacy = resolve(
                     base,
                     value(
                        p,
                        databaseMode.equals("lito_mysql") ? "database.connection.properties" : "database.legacy.properties",
                        "../../1-伺服器/config/sql.properties"
                     )
                  );
                  boolean databaseReadOnly = Boolean.parseBoolean(value(p, "database.read.only", "true"));
                  boolean statePersistenceEnabled = Boolean.parseBoolean(value(p, "state.persistence.enabled", "true"));
                  boolean nativeTransferStoneProbeEnabled = Boolean.parseBoolean(value(p, "native.transfer.stone.probe.enabled", "false"));
                  if (!nativeTransferStoneProbeEnabled || "memory".equals(databaseMode) && port == 32100) {
                     boolean nativeCharacterEventProbeEnabled = Boolean.parseBoolean(value(p, "native.character.event.probe.enabled", "false"));
                     if (nativeCharacterEventProbeEnabled) {
                        requireLoopback("advertised.host", advertisedHost);
                        if (!"memory".equals(databaseMode)
                           || port != 32100
                           || !databaseReadOnly
                           || statePersistenceEnabled
                           || !loginBootstrapSequence.contains("classic_server_list")) {
                           throw new IllegalArgumentException(
                              "native.character.event.probe.enabled requires a read-only, non-persistent memory fixture on port 32100 with classic_server_list bootstrap"
                           );
                        }
                     }

                     boolean nativeSourceSelectionProbeEnabled = Boolean.parseBoolean(value(p, "native.source.selection.probe.enabled", "false"));
                     if (nativeSourceSelectionProbeEnabled) {
                        requireLoopback("advertised.host", advertisedHost);
                        if (!"memory".equals(databaseMode)
                           || port != 32100
                           || !databaseReadOnly
                           || statePersistenceEnabled
                           || !loginBootstrapSequence.contains("classic_server_list")
                           || nativeCharacterEventProbeEnabled) {
                           throw new IllegalArgumentException(
                              "native.source.selection.probe.enabled requires an isolated read-only source-list fixture without S_04"
                           );
                        }
                     }

                     boolean attackAllProbeEnabled = Boolean.parseBoolean(value(p, "attack.all.probe.enabled", "false"));
                     if (attackAllProbeEnabled) {
                        requireLoopback("bind", bind);
                        requireLoopback("game.bind", gameBind);
                        requireLoopback("advertised.host", advertisedHost);
                        if (!"memory".equals(databaseMode)
                           || port != 32100
                           || !databaseReadOnly
                           || statePersistenceEnabled
                           || !loginBootstrapSequence.contains("classic_server_list")) {
                           throw new IllegalArgumentException(
                              "attack.all.probe.enabled requires a loopback-only, read-only, non-persistent memory fixture on port 32100 with classic_server_list bootstrap"
                           );
                        }
                     }

                     boolean bookListProbeEnabled = Boolean.parseBoolean(value(p, "book.list.probe.enabled", "false"));
                     if (bookListProbeEnabled) {
                        requireLoopback("bind", bind);
                        requireLoopback("game.bind", gameBind);
                        requireLoopback("advertised.host", advertisedHost);
                        if (!"memory".equals(databaseMode)
                           || port != 32100
                           || !databaseReadOnly
                           || statePersistenceEnabled
                           || !loginBootstrapSequence.contains("classic_server_list")) {
                           throw new IllegalArgumentException(
                              "book.list.probe.enabled requires a loopback-only, read-only, non-persistent memory fixture on port 32100 with classic_server_list bootstrap"
                           );
                        }
                     }

                     boolean cloneAppearanceProbeEnabled = Boolean.parseBoolean(value(p, "clone.appearance.probe.enabled", "false"));
                     if (cloneAppearanceProbeEnabled) {
                        requireLoopback("advertised.host", advertisedHost);
                        if (!"memory".equals(databaseMode)
                           || port != 32100
                           || !databaseReadOnly
                           || statePersistenceEnabled
                           || !loginBootstrapSequence.contains("classic_server_list")) {
                           throw new IllegalArgumentException(
                              "clone.appearance.probe.enabled requires a read-only, non-persistent memory fixture on port 32100 with classic_server_list bootstrap"
                           );
                        }
                     }

                     String legacyLoginDatabase = databaseMode.equals("lito_mysql") ? "" : value(p, "database.legacy.login.database", "");
                     String sourceDatabaseProperty = databaseMode.equals("lito_mysql")
                        ? optional(p, "database.dynamic.databases")
                        : optional(p, "database.legacy.source.databases");
                     List<String> legacySourceDatabases = parseLegacySourceDatabases(
                        sourceDatabaseProperty, databaseMode, classicServerNames, legacyLoginDatabase
                     );
                     if (databaseMode.equals("lito_mysql")) {
                        legacyLoginDatabase = legacySourceDatabases.get(0);
                     }

                     String resourceDatabaseProperty;
                     if (databaseMode.equals("lito_mysql")) {
                        String staticDatabase = optional(p, "database.static.database");
                        if (!staticDatabase.matches("[A-Za-z0-9_]+")) {
                           throw new IllegalArgumentException("database.static.database must be a database identifier");
                        }

                        resourceDatabaseProperty = Collections.nCopies(classicServerNames.size(), staticDatabase).stream().collect(Collectors.joining(","));
                     } else {
                        resourceDatabaseProperty = optional(p, "database.legacy.source.resource.databases");
                     }

                     List<String> legacySourceResourceDatabases = parseLegacyResourceDatabases(resourceDatabaseProperty, databaseMode, classicServerNames);
                     List<Double> legacySourceWeightRates = parseLegacyWeightRates(
                        optional(p, databaseMode.equals("lito_mysql") ? "database.source.weight.rates" : "database.legacy.source.weight.rates"),
                        databaseMode,
                        classicServerNames
                     );
                     int rentalTownMapId = integer(p, "lito.rental.town.map", 500, 500, 560);
                     if (rentalTownMapId != 500 && rentalTownMapId != 560) {
                        throw new IllegalArgumentException("lito.rental.town.map must be exact native map 500 or 560");
                     } else {
                        List<NativeLitoRentalTeleportEndpointCatalog.Endpoint> rentalTeleportEndpoints = NativeLitoRentalTeleportEndpointCatalog.parse(
                           optional(p, "lito.rental.teleport.routes")
                        );
                        String rentalBattleHandoffFileValue = optional(p, "lito.rental.handoff.file");
                        Path rentalBattleHandoffFile = rentalBattleHandoffFileValue.isBlank() ? null : resolve(base, rentalBattleHandoffFileValue);
                        return new ServerConfig(
                           source,
                           bind,
                           gameBind,
                           port,
                           channelPorts,
                           advertisedHost,
                           advertisedPort,
                           serverName,
                           originSourceId,
                           originSourceName,
                           classicServerNames,
                           classicServerIds,
                           maxFrame,
                           integer(p, "version.response.delay.ms", 750, 0, 5000),
                           integer(p, "login.bootstrap.initial.delay.ms", 0, 0, 5000),
                           integer(p, "login.bootstrap.step.delay.ms", 750, 0, 5000),
                           loginBootstrapSequence,
                           loginNoticeMessageIds,
                           loginCommonNews,
                           integer(p, "lobby.server.select.response.delay.ms", 750, 0, 5000),
                           Boolean.parseBoolean(value(p, "lobby.enter.channel.response.enabled", "true")),
                           integer(p, "lobby.enter.channel.response.delay.ms", 500, 0, 5000),
                           Boolean.parseBoolean(value(p, "lobby.character.list.enabled", "true")),
                           Boolean.parseBoolean(value(p, "lito.rental.town.directory.enabled", "false")),
                           integer(p, "lito.rental.town.room.id", 1342177281, 1, Integer.MAX_VALUE),
                           value(p, "lito.rental.town.directory.name", "LiTo Rental Village"),
                           rentalTownDirectoryHost,
                           rentalTownDirectoryPort,
                           rentalTownMapId,
                           rentalTeleportEndpoints,
                           rentalBattleHandoffFile,
                           capture,
                           resolve(base, value(p, "maps.directory", "../maps")),
                           databaseMode,
                           legacy,
                           legacyLoginDatabase,
                           legacySourceDatabases,
                           legacySourceResourceDatabases,
                           legacySourceWeightRates,
                           databaseReadOnly,
                           value(p, "memory.account", "test"),
                           value(p, "memory.password", "test"),
                           optional(p, "memory.authentication.failure.account"),
                           integer(p, "memory.source.inventory.weight", 0, 0, Integer.MAX_VALUE),
                           decimal(p, "memory.source.weight.rate", 1.0, 1.0E-6, 1000.0),
                           integer(p, "memory.source.food", 40, 0, 255),
                           integer(p, "memory.source.primary.level", 52, 1, 999999),
                           integer(p, "memory.source.primary.max.hp", 50, 1, 999999),
                           integer(p, "memory.source.primary.max.mp", 100, 1, 999999),
                           integer(p, "memory.source.primary.armor.class", 10, -999999, 999999),
                           integer(p, "memory.source.primary.strength", 13, 0, 999999),
                           integer(p, "memory.source.primary.constitution", 10, 0, 999999),
                           integer(p, "memory.source.primary.dexterity", 10, 0, 999999),
                           integer(p, "memory.source.primary.charisma", 9, 0, 999999),
                           integer(p, "memory.source.primary.intelligence", 8, 0, 999999),
                           integer(p, "memory.source.primary.wisdom", 11, 0, 999999),
                           integer(p, "memory.source.secondary.max.mp", 110, 1, 999999),
                           Boolean.parseBoolean(value(p, "memory.source.inventory.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "memory.source.projectile.fixture.enabled", "false")),
                           integer(p, "memory.source.castle.war.fixture.start.delay.seconds", -1, -1, 3600),
                           integer(p, "memory.source.castle.war.fixture.duration.minutes", 60, 1, 10080),
                           integer(p, "memory.source.castle.war.fixture.interval.minutes", 10080, 2, 525600),
                           resolve(base, value(p, "state.file", "../runtime/server-state.properties")),
                           statePersistenceEnabled,
                           Boolean.parseBoolean(value(p, "battle.ai.enabled", "false")),
                           Boolean.parseBoolean(value(p, "battle.natural.hp.regen.enabled", "true")),
                           Boolean.parseBoolean(value(p, "battle.natural.mp.regen.enabled", "false")),
                           Boolean.parseBoolean(value(p, "battle.physical.hit.enabled", "false")),
                           Boolean.parseBoolean(value(p, "battle.compatibility.service.actors.enabled", "true")),
                           Boolean.parseBoolean(value(p, "classic.world.compatibility.enabled", databaseMode.equals("memory") ? "true" : "false")),
                           Boolean.parseBoolean(value(p, "classic.world.monster.ai.enabled", "false")),
                           integer(p, "classic.world.monster.ai.tick.ms", 100, 50, 10000),
                           Boolean.parseBoolean(value(p, "classic.world.monster.aggressive.ai.enabled", "true")),
                           integer(p, "classic.world.pirate.ship.fixture.second.of.day", -1, -1, 86399),
                           integer(p, "classic.world.ship.fixture.second.of.day", -1, -1, 86399),
                           Boolean.parseBoolean(value(p, "classic.world.ship.fixture.window.opening", "false")),
                           integer(p, "classic.world.monster.poison.fixture.roll", 0, 0, 100),
                           integer(p, "classic.world.monster.physical.skill.fixture.branch", 0, 0, 2),
                           integer(p, "classic.world.monster.physical.skill.fixture.trigger.roll", 0, 0, 100),
                           integer(p, "classic.world.monster.physical.skill.fixture.level.roll", 0, 0, 27),
                           integer(p, "classic.world.monster.physical.skill.fixture.critical.roll", 0, 0, 99),
                           integer(p, "classic.world.monster.magic.fixture.branch", 0, 0, 2),
                           integer(p, "classic.world.monster.magic.fixture.trigger.roll", 0, 0, 100),
                           integer(p, "classic.world.monster.magic.fixture.die.roll", 0, 0, 6),
                           integer(p, "time.attack.documented.pattern.fixture.index", -1, -1, 1),
                           integer(p, "time.attack.dragon.outcome.fixture.index", -1, -1, 3),
                           Boolean.parseBoolean(value(p, "time.attack.dragon.combat.fixture.only", "false")),
                           integer(p, "time.attack.dragon.combat.fixture.minimum.targets", 1, 1, 8),
                           integer(p, "time.attack.monster.act.no.fixture", -1, -1, 255),
                           integer(p, "time.attack.monster.magic.fixture.trigger.roll", 0, 0, 100),
                           integer(p, "time.attack.monster.physical.fixture.trigger.roll", 0, 0, 100),
                           integer(p, "time.attack.monster.candidate.fixture.selector", 0, 0, Integer.MAX_VALUE),
                           integer(p, "time.attack.monster.physical.fixture.level.roll", -1, -1, 55),
                           integer(p, "time.attack.monster.physical.fixture.critical.roll", -1, -1, 99),
                           integer(p, "time.attack.monster.physical.fixture.ac.roll", -1, -1, 255),
                           integer(p, "time.attack.player.physical.damage.fixture", 0, 0, Integer.MAX_VALUE),
                           integer(p, "classic.world.quest.drop.fixture.roll", 0, 0, 100),
                           integer(p, "classic.world.death.item.chance.fixture.roll", 0, 0, 100),
                           integer(p, "classic.world.death.item.count.fixture.roll", 0, 0, 3),
                           integer(p, "classic.world.death.item.selector.fixture.index", -1, -1, 255),
                           integer(p, "classic.world.death.skill.chance.fixture.roll", 0, 0, 100),
                           integer(p, "classic.world.death.skill.count.fixture.roll", 0, 0, 3),
                           integer(p, "classic.world.death.skill.selector.fixture.index", -1, -1, 255),
                           integer(p, "classic.world.drop.auto-loot", 2, 0, 2),
                           integer(p, "classic.world.drop.looting-range", 15, 0, 127),
                           decimal(p, "classic.world.drop.item-rate", 1.0, 0.0, 1000.0),
                           decimal(p, "classic.world.drop.adena-rate", 1.0, 0.0, 1000.0),
                           Boolean.parseBoolean(value(p, "native.player.drop.enabled", Boolean.toString(false))),
                           decimal(p, "classic.world.experience.rate", 1.0, 1.0, 32767.0),
                           decimal(p, "classic.world.lawful.rate", 1.0, 1.0, 32767.0),
                           integer(p, "classic.world.scarecrow.max-level", 14, 1, 99),
                           integer(p, "battle.ai.tick.ms", 900, 100, 10000),
                           integer(p, "battle.ai.damage", 5, 1, 100),
                           integer(p, "deathmatch.fatality.fixture.slot", -1, -2, 8),
                           integer(p, "battle.reward.win", 250, 0, 1000000),
                           integer(p, "battle.reward.loss", 50, 0, 1000000),
                           Boolean.parseBoolean(value(p, "native.pet.race.minimum.players.enforced", "true")),
                           Boolean.parseBoolean(value(p, "native.siege.roster.enforced", "true")),
                           Boolean.parseBoolean(value(p, "native.tournament.economy.enabled", "false")),
                           integer(p, "native.tournament.allowance", 20000, 0, 1000000),
                           Boolean.parseBoolean(value(p, "native.integrity.pre-world.probe", "false")),
                           nativeTransferStoneProbeEnabled,
                           nativeCharacterEventProbeEnabled,
                           nativeSourceSelectionProbeEnabled,
                           attackAllProbeEnabled,
                           bookListProbeEnabled,
                           cloneAppearanceProbeEnabled,
                           integer(p, "repair.fixture.weapon.durability", 0, 0, 127),
                           Boolean.parseBoolean(value(p, "curse.removal.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "control.ring.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "spell.material.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "awaken.valakas.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "world.cancellation.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "world.taming.lifecycle.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "curse.poison.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "sleep.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "freeze.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "blind.hiding.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "enchant.venom.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "shadow.armor.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "bring.stone.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "moving.acceleration.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "burning.spirit.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "dark.blind.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "weakness.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "silence.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "decay.potion.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "venom.resist.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "double.brake.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "uncanny.dodge.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "mirror.image.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "confusion.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "smash.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "mind.break.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "bone.break.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "illusion.ogre.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "illusion.lich.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "cube.ignition.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "cube.quake.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "cube.shock.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "cube.harmony.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "insight.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "panic.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "joy.of.pain.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "illusion.avatar.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "patience.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "phantasm.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "shadow.fang.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "final.burn.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "dress.mighty.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "dress.dexterity.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "dress.evasion.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "armor.break.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "weapon.break.spell.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "chainsword.weakness.fixture.advance.enabled", "false")),
                           integer(p, "enchant.venom.proc.fixture.roll", 0, 0, 100),
                           integer(p, "bring.stone.fixture.roll", 0, 0, 100),
                           integer(p, "burning.spirit.proc.fixture.roll", 0, 0, 100),
                           integer(p, "elemental.fire.proc.fixture.roll", 0, 0, 100),
                           integer(p, "dark.blind.fixture.roll", 0, 0, 100),
                           integer(p, "weakness.fixture.die.roll", 0, 0, 12),
                           integer(p, "weakness.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "mass.slow.fixture.die.roll", 0, 0, 7),
                           integer(p, "mass.slow.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "sleep.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "phantasm.spell.fixture.percentile.roll", 1, 1, 100),
                           integer(p, "earth.bind.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "silence.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "decay.potion.fixture.die.roll", 0, 0, 8),
                           integer(p, "decay.potion.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "curse.poison.fixture.die.roll", 0, 0, 12),
                           integer(p, "curse.poison.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "curse.paralysis.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "taming.monster.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "return.to.nature.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "turn.undead.fixture.die.roll", 0, 0, 7),
                           integer(p, "turn.undead.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "mana.drain.fixture.die.roll", 0, 0, 12),
                           integer(p, "mana.drain.fixture.percentile.roll", 0, 0, 100),
                           integer(p, "mana.drain.fixture.transfer.roll", 0, 0, 10),
                           integer(p, "double.brake.proc.fixture.roll", 0, 0, 100),
                           integer(p, "uncanny.dodge.hit.fixture.d20", 0, 0, 20),
                           integer(p, "uncanny.dodge.hit.fixture.d100", 0, 0, 100),
                           integer(p, "ranged.evasion.fixture.d100", 0, 0, 100),
                           integer(p, "counter.barrier.fixture.d100", 0, 0, 100),
                           integer(p, "mortal.body.fixture.d100", 0, 0, 100),
                           integer(p, "thunder.grab.fixture.d100", 0, 0, 100),
                           integer(p, "thunder.grab.fixture.duration.offset", -1, -1, 2),
                           integer(p, "confusion.fixture.d100", 0, 0, 100),
                           mindBreakFixtureMpDrain(p),
                           integer(p, "bone.break.fixture.d1000", 0, 0, 1000),
                           integer(p, "shadow.fang.damage.fixture.weapon.roll", 0, 0, 127),
                           integer(p, "armor.break.fixture.d100", 0, 0, 100),
                           integer(p, "guard.brake.fixture.d100", 0, 0, 100),
                           integer(p, "horror.of.death.fixture.d100", 0, 0, 100),
                           integer(p, "freezing.breath.fixture.d100", 0, 0, 100),
                           integer(p, "resist.fear.fixture.d100", 0, 0, 100),
                           integer(p, "weapon.break.fixture.d100", 0, 0, 100),
                           integer(p, "weapon.break.fixture.damage.roll", 0, 0, 127),
                           Boolean.parseBoolean(value(p, "warehouse.fixture.enabled", "false")),
                           Boolean.parseBoolean(value(p, "letter.fixture.enabled", "false")),
                           parseRepairNpcAppearanceSpeech(optional(p, "repair.npc.appearance.speech")),
                           parseAdministrationAccounts(value(p, "administration.accounts", ""))
                        );
                     }
                  } else {
                     throw new IllegalArgumentException("native.transfer.stone.probe.enabled requires memory mode on port 32100");
                  }
               }
            }
         }
      }
   }

   private static void applyServiceMode(Properties properties, String requestedMode) {
      if (requestedMode != null && !requestedMode.isBlank()) {
         String mode = requestedMode.trim().toLowerCase(Locale.ROOT);
         switch (mode) {
            case "standard":
               break;
            case "rental-source":
               properties.remove("lito.channel.ports");
               set(properties, "bind", "127.0.0.1");
               set(properties, "game.bind", "127.0.0.2");
               set(properties, "port", "32000");
               set(properties, "advertised.host", "127.0.0.2");
               set(properties, "advertised.port", "32000");
               set(properties, "server.name", "LiTo Shared Rental Town");
               set(properties, "lito.rental.town.directory.enabled", "true");
               set(properties, "lito.rental.town.room.id", "1342177281");
               set(properties, "lito.rental.town.directory.name", "LiTo 租賃村莊");
               set(properties, "lito.rental.town.directory.host", "127.0.0.1");
               set(properties, "lito.rental.town.directory.port", "32000");
               set(properties, "lito.rental.town.map", "500");
               set(
                  properties,
                  "lito.rental.teleport.routes",
                  "TeleportMe|LT_TELEPORT_B|LiTo 租賃戰鬥|127.0.0.3|32001,TeleportMe|LT_TELEPORT_C|LiTo 租賃戰鬥|127.0.0.3|32001"
               );
               set(properties, "lito.rental.handoff.file", "../runtime/rental-handoff.state");
               set(properties, "capture.directory", "../runtime/captures-rental-source");
               set(properties, "state.file", "../runtime/server-rental-source.state");
               set(properties, "state.persistence.enabled", "false");
               set(properties, "classic.world.compatibility.enabled", "false");
               set(properties, "classic.world.monster.ai.enabled", "false");
               break;
            case "rental-battle":
               properties.remove("lito.channel.ports");
               set(properties, "bind", "127.0.0.3");
               set(properties, "game.bind", "127.0.0.4");
               set(properties, "port", "32001");
               set(properties, "advertised.host", "127.0.0.4");
               set(properties, "advertised.port", "32001");
               set(properties, "server.name", "LiTo Shared Rental Battle");
               set(properties, "lito.rental.town.directory.enabled", "false");
               set(properties, "lito.rental.teleport.routes", "");
               set(properties, "lito.rental.handoff.file", "../runtime/rental-handoff.state");
               set(properties, "capture.directory", "../runtime/captures-rental-battle");
               set(properties, "state.file", "../runtime/server-rental-battle.state");
               set(properties, "state.persistence.enabled", "true");
               set(properties, "classic.world.compatibility.enabled", "false");
               set(properties, "classic.world.monster.ai.enabled", "false");
               break;
            default:
               throw new IllegalArgumentException("unsupported service mode: " + requestedMode + " (expected standard, rental-source, or rental-battle)");
         }
      }
   }

   private static void set(Properties properties, String key, String value) {
      properties.setProperty(key, value);
   }

   private static int mindBreakFixtureMpDrain(Properties properties) {
      int value = integer(properties, "mind.break.fixture.mp.drain", 0, 0, 50);
      if (value > 0 && value < 5) {
         throw new IllegalArgumentException("mind.break.fixture.mp.drain must be 0 or 5..50");
      } else {
         return value;
      }
   }

   private static List<Integer> parseClassicServerIds(String source, List<String> classicServerNames) {
      if (source.isBlank()) {
         return IntStream.rangeClosed(1, classicServerNames.size()).boxed().toList();
      } else {
         List<Integer> ids;
         try {
            ids = Arrays.stream(source.split(",", -1)).map(String::trim).map(Integer::parseInt).toList();
         } catch (RuntimeException var6) {
            throw new IllegalArgumentException("login.classic.server.ids contains an invalid number", var6);
         }

         if (ids.size() != classicServerNames.size()) {
            throw new IllegalArgumentException("login.classic.server.ids count must match login.classic.server.names");
         } else {
            LinkedHashSet<Integer> unique = new LinkedHashSet<>();

            for (int id : ids) {
               if (id < 1 || id > 255) {
                  throw new IllegalArgumentException("login.classic.server.ids must contain only 1..255");
               }

               if (!unique.add(id)) {
                  throw new IllegalArgumentException("login.classic.server.ids must be unique");
               }
            }

            return List.copyOf(ids);
         }
      }
   }

   private static List<String> parseLegacySourceDatabases(String source, String databaseMode, List<String> classicServerNames, String legacyLoginDatabase) {
      if (source.isBlank()) {
         if (isMysqlMode(databaseMode) && classicServerNames.size() > 1) {
            throw new IllegalArgumentException("dynamic databases must map every advertised classic source");
         } else {
            return legacyLoginDatabase.isBlank() ? List.of() : List.of(legacyLoginDatabase.trim());
         }
      } else {
         List<String> databases = Arrays.stream(source.split(",", -1)).map(String::trim).toList();
         if (!isMysqlMode(databaseMode)) {
            throw new IllegalArgumentException("database routes require a MySQL database.mode");
         } else if (databases.size() != classicServerNames.size()) {
            throw new IllegalArgumentException("database.legacy.source.databases count must match login.classic.server.names");
         } else {
            for (String database : databases) {
               if (!database.matches("[A-Za-z0-9_]+")) {
                  throw new IllegalArgumentException("database.legacy.source.databases contains an invalid database identifier");
               }
            }

            return List.copyOf(databases);
         }
      }
   }

   private static List<String> parseLegacyResourceDatabases(String source, String databaseMode, List<String> classicServerNames) {
      if (source.isBlank()) {
         if (isMysqlMode(databaseMode)) {
            throw new IllegalArgumentException("static databases must map every advertised classic source");
         } else {
            return List.of();
         }
      } else if (!isMysqlMode(databaseMode)) {
         throw new IllegalArgumentException("resource database routes require a MySQL database.mode");
      } else {
         List<String> databases = Arrays.stream(source.split(",", -1)).map(String::trim).toList();
         if (databases.size() != classicServerNames.size()) {
            throw new IllegalArgumentException("database.legacy.source.resource.databases count must match login.classic.server.names");
         } else {
            for (String database : databases) {
               if (!database.matches("[A-Za-z0-9_]+")) {
                  throw new IllegalArgumentException("database.legacy.source.resource.databases contains an invalid database identifier");
               }
            }

            return List.copyOf(databases);
         }
      }
   }

   private static List<Double> parseLegacyWeightRates(String source, String databaseMode, List<String> classicServerNames) {
      if (source.isBlank()) {
         if (isMysqlMode(databaseMode)) {
            throw new IllegalArgumentException("database weight rates must map every advertised classic source");
         } else {
            return List.of();
         }
      } else if (!isMysqlMode(databaseMode)) {
         throw new IllegalArgumentException("database weight rates require a MySQL database.mode");
      } else {
         List<Double> rates;
         try {
            rates = Arrays.stream(source.split(",", -1)).map(String::trim).map(Double::parseDouble).toList();
         } catch (RuntimeException var7) {
            throw new IllegalArgumentException("database.legacy.source.weight.rates contains an invalid number", var7);
         }

         if (rates.size() != classicServerNames.size()) {
            throw new IllegalArgumentException("database.legacy.source.weight.rates count must match login.classic.server.names");
         } else {
            for (double rate : rates) {
               if (!Double.isFinite(rate) || rate <= 0.0 || rate > 1000.0) {
                  throw new IllegalArgumentException("database.legacy.source.weight.rates is outside 0..1000");
               }
            }

            return List.copyOf(rates);
         }
      }
   }

   private static boolean isMysqlMode(String databaseMode) {
      return databaseMode.equals("legacy_mysql") || databaseMode.equals("lito_mysql");
   }

   private static void applyExternalSourceServers(Properties target, Path base) throws IOException {
      try {
         if (SharedDatabase.applySources(target, base)) {
            return;
         }
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException(e);
      }
      {
         String configured = optional(target, "source.servers.file");
         if (!configured.isEmpty()) {
            Path source = resolve(base, configured);
            if (Files.isRegularFile(source)) {
               Properties external = new Properties();

               try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
                  external.load(reader);
               }

               int count = integer(external, "source.count", 0, 1, 16);
               List<String> names = new ArrayList<>(count);
               List<String> ids = new ArrayList<>(count);
               List<String> databases = new ArrayList<>(count);
               List<String> resourceDatabases = new ArrayList<>(count);
               List<String> weightRates = new ArrayList<>(count);
               boolean hasResourceDatabase = false;

               for (int index = 1; index <= count; index++) {
                  String prefix = "source." + index + ".";
                  names.add(requiredExternal(external, prefix + "name"));
                  ids.add(requiredExternal(external, prefix + "id"));
                  databases.add(requiredExternal(external, prefix + "database"));
                  String resource = optional(external, prefix + "resource.database");
                  resourceDatabases.add(resource);
                  hasResourceDatabase |= !resource.isEmpty();
                  weightRates.add(value(external, prefix + "weight.rate", "100.0"));
               }

               if (hasResourceDatabase && resourceDatabases.stream().anyMatch(String::isEmpty)) {
                  throw new IllegalArgumentException("every external source must define resource.database when one does");
               } else {
                  target.setProperty("login.classic.server.names", String.join(",", names));
                  target.setProperty("login.classic.server.ids", String.join(",", ids));
                  target.setProperty("database.dynamic.databases", String.join(",", databases));
                  target.setProperty("database.source.weight.rates", String.join(",", weightRates));
                  if (hasResourceDatabase) {
                     target.setProperty("database.source.resource.databases", String.join(",", resourceDatabases));
                  }
               }
            }
         }
      }
   }

   private static String requiredExternal(Properties p, String key) {
      String result = optional(p, key);
      if (result.isEmpty()) {
         throw new IllegalArgumentException("missing external source property: " + key);
      } else if (result.indexOf(44) >= 0) {
         throw new IllegalArgumentException("external source property cannot contain comma: " + key);
      } else {
         return result;
      }
   }

   boolean ephemeralSourceProfiles() {
      return this.databaseReadOnly && this.databaseMode.equals("lito_mysql");
   }

   private static List<String> parseLoginBootstrapSequence(String source) {
      List<String> sequence = Arrays.stream(source.split(","))
         .map(String::trim)
         .filter(value -> !value.isEmpty())
         .map(value -> value.toLowerCase(Locale.ROOT))
         .toList();

      for (String token : sequence) {
         if (!token.equals("login_check")
            && !token.equals("login_ok")
            && !token.equals("time_limit")
            && !token.equals("classic_server_list")
            && !token.equals("lito_server_list")) {
            throw new IllegalArgumentException("unknown login.bootstrap.sequence token: " + token);
         }
      }

      return List.copyOf(sequence);
   }

   private static List<String> parseServerNames(String source) {
      List<String> names = Arrays.stream(source.split(",")).map(String::trim).filter(value -> !value.isEmpty()).distinct().limit(16L).toList();
      if (names.isEmpty()) {
         throw new IllegalArgumentException("login.classic.server.names is empty");
      } else {
         Charset ms950 = Charset.forName("MS950");

         for (String name : names) {
            byte[] encoded = name.getBytes(ms950);
            if (encoded.length > 63) {
               throw new IllegalArgumentException("login.classic.server.names exceeds native 63-byte source storage");
            }

            if (!new String(encoded, ms950).equals(name) || name.chars().anyMatch(Character::isISOControl)) {
               throw new IllegalArgumentException("login.classic.server.names is not a valid native MS950 source name");
            }
         }

         return List.copyOf(names);
      }
   }

   private static List<Integer> parseLoginNoticeMessageIds(String source) {
      if (source.isBlank()) {
         return List.of();
      } else {
         List<Integer> ids = Arrays.stream(source.split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(Integer::decode).toList();
         if (ids.size() > 32) {
            throw new IllegalArgumentException("login.notice.message.ids exceeds 32 entries");
         } else {
            for (int index = 0; index < ids.size(); index++) {
               int id = ids.get(index);
               if (id < 0 || id > 32767) {
                  throw new IllegalArgumentException("login.notice.message.ids is outside signed 16-bit range: " + id);
               }

               if (id == 438 && index + 1 != ids.size()) {
                  throw new IllegalArgumentException("native login notice 438 must be terminal");
               }
            }

            return List.copyOf(ids);
         }
      }
   }

   private static String parseLoginCommonNews(String source) {
      if (source.indexOf(0) >= 0) {
         throw new IllegalArgumentException("login.common.news contains NUL");
      } else {
         int bytes = source.getBytes(Charset.forName("MS950")).length;
         if (bytes > 255) {
            throw new IllegalArgumentException("login.common.news exceeds native 255-byte storage");
         } else {
            return source;
         }
      }
   }

   private static String parseRepairNpcAppearanceSpeech(String source) {
      if (source.indexOf(0) >= 0) {
         throw new IllegalArgumentException("repair.npc.appearance.speech contains NUL");
      } else {
         String rendered = "$478: " + source;
         int bytes = rendered.getBytes(Charset.forName("MS950")).length;
         if (!source.isEmpty() && bytes > 511) {
            throw new IllegalArgumentException("repair.npc.appearance.speech exceeds native 511-byte S_SAY_CODE storage");
         } else {
            return source;
         }
      }
   }

   private static Set<String> parseAdministrationAccounts(String source) {
      return Arrays.stream(source.split(","))
         .map(String::trim)
         .filter(value -> !value.isEmpty())
         .map(value -> value.toLowerCase(Locale.ROOT))
         .collect(Collectors.toUnmodifiableSet());
   }

   private static List<Integer> parseChannelPorts(String source, int primaryPort) {
      if (source != null && !source.isBlank()) {
         List<Integer> ports = Arrays.stream(source.split(",", -1)).map(String::trim).map(value -> {
            if (value.isEmpty()) {
               throw new IllegalArgumentException("lito.channel.ports contains an empty value");
            } else {
               int port = Integer.parseInt(value);
               if (port >= 1 && port <= 65535) {
                  return port;
               } else {
                  throw new IllegalArgumentException("lito.channel.ports is outside 1..65535");
               }
            }
         }).toList();
         if (ports.size() == GameHub.CHANNEL_COUNT && new LinkedHashSet<>(ports).size() == ports.size() && ports.get(0) == primaryPort) {
            return List.copyOf(ports);
         } else {
            throw new IllegalArgumentException("lito.channel.ports must contain four unique ports and start with port=" + primaryPort);
         }
      } else {
         return List.of(primaryPort);
      }
   }

   boolean dedicatedChannelPortsEnabled() {
      return this.channelPorts.size() == GameHub.CHANNEL_COUNT;
   }

   int channelPort(int channelId) {
      if (channelId >= 0 && channelId < GameHub.CHANNEL_COUNT) {
         return this.dedicatedChannelPortsEnabled() ? this.channelPorts.get(channelId) : this.port;
      } else {
         throw new IllegalArgumentException("invalid channel id: " + channelId);
      }
   }

   private static String value(Properties p, String key, String fallback) {
      String value = p.getProperty(key);
      return value != null && !value.isBlank() ? value.trim() : fallback;
   }

   private static String optional(Properties p, String key) {
      String value = p.getProperty(key);
      return value == null ? "" : value.trim();
   }

   private static void requireLoopback(String key, String value) throws IOException {
      InetAddress address = InetAddress.getByName(value);
      if (!(address instanceof Inet4Address) || !address.isLoopbackAddress()) {
         throw new IllegalArgumentException(key + " must be an IPv4 loopback address");
      }
   }

   private static int integer(Properties p, String key, int fallback, int min, int max) {
      int value = Integer.parseInt(value(p, key, Integer.toString(fallback)));
      if (value >= min && value <= max) {
         return value;
      } else {
         throw new IllegalArgumentException(key + " is outside " + min + ".." + max);
      }
   }

   private static double decimal(Properties p, String key, double fallback, double min, double max) {
      double result = Double.parseDouble(value(p, key, Double.toString(fallback)));
      if (Double.isFinite(result) && !(result < min) && !(result > max)) {
         return result;
      } else {
         throw new IllegalArgumentException(key + " is outside " + min + ".." + max);
      }
   }

   private static Path resolve(Path base, String value) {
      Path candidate = Path.of(value);
      return (candidate.isAbsolute() ? candidate : base.resolve(candidate)).normalize().toAbsolutePath();
   }
}
