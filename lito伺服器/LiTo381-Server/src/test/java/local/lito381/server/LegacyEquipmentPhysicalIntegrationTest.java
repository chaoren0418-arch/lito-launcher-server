package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Explicitly opted-in, read-only probe against the configured legacy sources. */
final class LegacyEquipmentPhysicalIntegrationTest {
    private static final int[] ENCOUNTER_NPC_IDS = {
            45262, 45207, 45289, 45259, 45278, 45322, 45390,
            45380, 45601, 45650, 45161, 45182, 45812, 45584, 45610,
            45065, 81183, 81184, 81185, 81186, 81187, 81188
    };
    private static final SourceNpcPhysicalStats[] EXPECTED_NPCS = {
            new SourceNpcPhysicalStats(45262, -4, true, 0, true, 0, 8, 0,
                    false, -22, false, false, 10, 16, 15, true),
            new SourceNpcPhysicalStats(45207, -20, true, 0, false, 0, 8, 15,
                    false, -25, true, false, 20, 17, 17, true),
            new SourceNpcPhysicalStats(45289, -12, false, 0, false, 0, 2, 35,
                    false, -40, true, false, 50, 17, 16, true),
            new SourceNpcPhysicalStats(45259, -15, true, 0, false, 0, 4, 5,
                    false, -18, true, false, 30, 14, 12, true),
            new SourceNpcPhysicalStats(45278, -18, true, 0, false, 0, 0, 20,
                    false, -34, true, false, 20, 20, 12, true),
            new SourceNpcPhysicalStats(45322, -25, false, 0, true, 2, 2, 40,
                    false, -35, true, false, 50, 18, 12, true),
            new SourceNpcPhysicalStats(45390, -20, false, 0, false, 0, 0, 0,
                    false, -20, false, false, 20, 20, 10, true),
            new SourceNpcPhysicalStats(45380, -16, false, 0, false, 0, 0, 50,
                    false, -39, false, false, 120, 17, 16, true),
            new SourceNpcPhysicalStats(45601, -75, false, 0, true, 3, 2, 150,
                    false, -120, false, true, 250, 77, 17, false, 1, 0),
            new SourceNpcPhysicalStats(45650, -62, true, 0, true, 3, 0, 50,
                    false, -180, false, true, 100, 67, 62, false, 2, 0),
            new SourceNpcPhysicalStats(45161, -3, false, 0, false, 1, 2, 18,
                    true, -18, true, false, 30, 17, 16, true, 1, 0),
            new SourceNpcPhysicalStats(45182, -8, true, 0, true, 0, 8, 7,
                    false, -14, true, false, 0, 46, 10, true),
            new SourceNpcPhysicalStats(45812, -23, false, 0, false, 0, 8, 35,
                    false, -20, false, false, 500, 13, 13, true),
            new SourceNpcPhysicalStats(45584, -69, true, 0, true, 0, 2, 60,
                    false, -110, false, true, 200, 72, 12, false),
            new SourceNpcPhysicalStats(45610, -67, true, 0, true, 0, 2, 70,
                    false, -80, false, true, 350, 22, 18, false),
            new SourceNpcPhysicalStats(45065, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 14, 6, true),
            new SourceNpcPhysicalStats(81183, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
            new SourceNpcPhysicalStats(81184, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
            new SourceNpcPhysicalStats(81185, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
            new SourceNpcPhysicalStats(81186, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
            new SourceNpcPhysicalStats(81187, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
            new SourceNpcPhysicalStats(81188, 8, false, 0, false, 1, 2, 5,
                    true, -8, true, false, 12, 16, 6, true),
    };

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void loadsEveryPhysicalEquipmentLayerForTheProbeAccount() throws Exception {
        String configPath = System.getProperty(
                "lito.legacy.config", "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int probedCharacters = 0;
            int projectileResourceSources = 0;
            int defensiveProjectionSources = 0;
            int attributeResourceSources = 0;
            int pierceTemplateRows = 0;
            int npcPhysicalRows = 0;
            int summonTemplateRows = 0;
            int petTemplateRows = 0;
            int holyWalkSkillRows = 0;
            int tornadoSkillRows = 0;
            int greaterHasteSkillRows = 0;
            int berserkersSkillRows = 0;
            int diseaseSkillRows = 0;
            int fullHealSkillRows = 0;
            int blizzardSkillRows = 0;
            int earthquakeSkillRows = 0;
            int silenceSkillRows = 0;
            int lightningStormSkillRows = 0;
            int meteorStrikeSkillRows = 0;
            int greaterResurrectionSkillRows = 0;
            int massSlowSkillRows = 0;
            int disintegrateSkillRows = 0;
            int absoluteBarrierSkillRows = 0;
            int advanceSpiritSkillRows = 0;
            int shapeChangeSkillRows = 0;
            int shapeChangeMenuRows = 0;
            int immuneToHarmSkillRows = 0;
            int massTeleportSkillRows = 0;
            int fireStormSkillRows = 0;
            int decayPotionSkillRows = 0;
            int counterDetectionSkillRows = 0;
            int specialWeaponRows = 0;
            int magicHitTables = 0;
            int magicHitResourceRows = 0;
            int magicHitDarkRows = 0;
            int blindResistanceTables = 0;
            int blindResistanceResourceRows = 0;
            int blindResistanceDarkRows = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                SourcePhysicalGrowthStats growth = repository.physicalGrowthStats();
                assertTrue(growth.hit(false, 127) >= growth.hit(false, 1));
                assertTrue(growth.damage(true, 127) >= growth.damage(true, 1));
                for (int index = 0; index < ENCOUNTER_NPC_IDS.length; index++) {
                    int npcId = ENCOUNTER_NPC_IDS[index];
                    SourceNpcPhysicalStats npc = repository.npcPhysicalStats(npcId);
                    assertEquals(EXPECTED_NPCS[index], npc);
                    System.out.printf(
                            "[LiTo-test] source=%s npc=%d ac=%d large=%s reduction=%d hard=%s"
                                    + " undead=%d weak=%d mr=%d isTU=%s lawful=%d%n",
                            source, npc.npcId(), npc.armorClass(), npc.large(),
                            npc.damageReduction(), npc.hard(), npc.undead(),
                            npc.weakAttribute(), npc.magicResistance(),
                            npc.turnUndeadEligible(), npc.lawful());
                    npcPhysicalRows++;
                }
                for (int npcId = 81210; npcId <= 81240; npcId++) {
                    SourceSummonTemplate summon = repository.summonTemplate(npcId);
                    assertEquals(SourceSummonTemplate.memoryFixture(npcId), summon,
                            "source summon template differs for NPC " + npcId);
                    summonTemplateRows++;
                }
                System.out.printf(
                        "[LiTo-test] source=%s summon-monster-template-rows=%d%n",
                        source, 31);
                assertEquals(SourcePetTemplate.memoryFixture(45034),
                        repository.petTemplate(45034));
                assertEquals(SourcePetTemplate.memoryFixture(45687),
                        repository.petTemplate(45687));
                assertEquals(SourcePetTemplate.memoryFixture(46046),
                        repository.petTemplate(46046));
                petTemplateRows++;
                HolyWalkSourceEvidence holyWalk = holyWalkSkillRow(repository);
                HolyWalkSourceEvidence expectedHolyWalk = switch (holyWalk.database()) {
                    case "380_db" -> new HolyWalkSourceEvidence("380_db", true, 0, 10);
                    case "380yiwei_db" ->
                            new HolyWalkSourceEvidence("380yiwei_db", false, 0, 0);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: " + holyWalk.database());
                };
                assertEquals(expectedHolyWalk, holyWalk,
                        "source skill 52 Holy Walk row differs");
                holyWalkSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s holy-walk-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d%n",
                        source, holyWalk.database(), holyWalk.effectSkillIdColumn(),
                        holyWalk.reuseDelayMillis());
                TornadoSourceEvidence tornado = tornadoSkillRow(repository);
                TornadoSourceEvidence expectedTornado = switch (tornado.database()) {
                    case "380_db" -> new TornadoSourceEvidence("380_db", true, 0, 1_500);
                    case "380yiwei_db" ->
                            new TornadoSourceEvidence("380yiwei_db", false, 0, 1_000);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: " + tornado.database());
                };
                assertEquals(expectedTornado, tornado,
                        "source skill 53 Tornado row differs");
                tornadoSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s tornado-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d%n",
                        source, tornado.database(), tornado.effectSkillIdColumn(),
                        tornado.reuseDelayMillis());
                GreaterHasteSourceEvidence greaterHaste =
                        greaterHasteSkillRow(repository);
                GreaterHasteSourceEvidence expectedGreaterHaste =
                        switch (greaterHaste.database()) {
                            case "380_db" -> new GreaterHasteSourceEvidence(
                                    "380_db", true, 0, 10);
                            case "380yiwei_db" -> new GreaterHasteSourceEvidence(
                                    "380yiwei_db", false, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + greaterHaste.database());
                        };
                assertEquals(expectedGreaterHaste, greaterHaste,
                        "source skill 54 Greater Haste row differs");
                greaterHasteSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s greater-haste-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d%n",
                        source, greaterHaste.database(),
                        greaterHaste.effectSkillIdColumn(),
                        greaterHaste.reuseDelayMillis());
                BerserkersSourceEvidence berserkers = berserkersSkillRow(repository);
                BerserkersSourceEvidence expectedBerserkers =
                        switch (berserkers.database()) {
                            case "380_db" -> new BerserkersSourceEvidence(
                                    "380_db", true, 0, 10, 3, 3);
                            case "380yiwei_db" -> new BerserkersSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 3, 3);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + berserkers.database());
                        };
                assertEquals(expectedBerserkers, berserkers,
                        "source skill 55 Berserkers row or ability rows differ");
                berserkersSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s berserkers-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " ability-rows=%d%n",
                        source, berserkers.database(),
                        berserkers.effectSkillIdColumn(),
                        berserkers.reuseDelayMillis(), berserkers.abilityRows());
                DiseaseSourceEvidence disease = diseaseSkillRow(repository);
                DiseaseSourceEvidence expectedDisease = switch (disease.database()) {
                    case "380_db" -> new DiseaseSourceEvidence(
                            "380_db", true, 0, 500, 2, 2, 1, 1, 0);
                    case "380yiwei_db" -> new DiseaseSourceEvidence(
                            "380yiwei_db", false, 0, 0, 2, 2, 1, 1, 0);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + disease.database());
                };
                assertEquals(expectedDisease, disease,
                        "source skill 56 Disease row or supporting rows differ");
                diseaseSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s disease-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " ability-rows=%d boss-rows=%d item-rows=%d"
                                + " probability-overrides=%d%n",
                        source, disease.database(), disease.effectSkillIdColumn(),
                        disease.reuseDelayMillis(), disease.abilityRows(),
                        disease.bossRows(), disease.itemRows(),
                        disease.probabilityOverrideRows());
                FullHealSourceEvidence fullHeal = fullHealSkillRow(repository);
                FullHealSourceEvidence expectedFullHeal = switch (fullHeal.database()) {
                    case "380_db" -> new FullHealSourceEvidence(
                            "380_db", true, 0, 500, 0, 0, 1, 0);
                    case "380yiwei_db" -> new FullHealSourceEvidence(
                            "380yiwei_db", false, 0, 500, 0, 0, 1, 0);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + fullHeal.database());
                };
                assertEquals(expectedFullHeal, fullHeal,
                        "source skill 57 Full Heal row or supporting rows differ");
                fullHealSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s full-heal-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " ability-rows=%d boss-rows=%d item-rows=%d"
                                + " probability-overrides=%d%n",
                        source, fullHeal.database(), fullHeal.effectSkillIdColumn(),
                        fullHeal.reuseDelayMillis(), fullHeal.abilityRows(),
                        fullHeal.bossRows(), fullHeal.itemRows(),
                        fullHeal.probabilityOverrideRows());
                BlizzardSourceEvidence blizzard = blizzardSkillRow(repository);
                BlizzardSourceEvidence expectedBlizzard = switch (blizzard.database()) {
                    case "380_db" -> new BlizzardSourceEvidence(
                            "380_db", true, 0, 1_500);
                    case "380yiwei_db" -> new BlizzardSourceEvidence(
                            "380yiwei_db", false, 0, 1_500);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + blizzard.database());
                };
                assertEquals(expectedBlizzard, blizzard,
                        "source skill 59 Blizzard row differs");
                blizzardSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s blizzard-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d%n",
                        source, blizzard.database(), blizzard.effectSkillIdColumn(),
                        blizzard.reuseDelayMillis());
                EarthquakeSourceEvidence earthquake = earthquakeSkillRow(repository);
                EarthquakeSourceEvidence expectedEarthquake = switch (earthquake.database()) {
                    case "380_db" -> new EarthquakeSourceEvidence(
                            "380_db", true, 0, 1_500);
                    case "380yiwei_db" -> new EarthquakeSourceEvidence(
                            "380yiwei_db", false, 0, 500);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + earthquake.database());
                };
                assertEquals(expectedEarthquake, earthquake,
                        "source skill 62 Earthquake row differs");
                earthquakeSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s earthquake-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d%n",
                        source, earthquake.database(), earthquake.effectSkillIdColumn(),
                        earthquake.reuseDelayMillis());
                SilenceSourceEvidence silence = silenceSkillRow(repository);
                SilenceSourceEvidence expectedSilence = switch (silence.database()) {
                    case "380_db" -> new SilenceSourceEvidence(
                            "380_db", true, 0, 500, 1, 1, 1);
                    case "380yiwei_db" -> new SilenceSourceEvidence(
                            "380yiwei_db", false, 0, 0, 1, 1, 1);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + silence.database());
                };
                assertEquals(expectedSilence, silence,
                        "source skill 64 Silence row or supporting rows differ");
                silenceSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s silence-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " boss-rows=%d item-rows=%d probability-rows=%d%n",
                        source, silence.database(), silence.effectSkillIdColumn(),
                        silence.reuseDelayMillis(), silence.bossRows(),
                        silence.itemRows(), silence.probabilityRows());
                LightningStormSourceEvidence lightningStorm =
                        lightningStormSkillRow(repository);
                LightningStormSourceEvidence expectedLightningStorm =
                        switch (lightningStorm.database()) {
                            case "380_db" -> new LightningStormSourceEvidence(
                                    "380_db", true, 0, 2_000, 1, 0, 0, 0);
                            case "380yiwei_db" -> new LightningStormSourceEvidence(
                                    "380yiwei_db", false, 0, 500, 1, 0, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + lightningStorm.database());
                        };
                assertEquals(expectedLightningStorm, lightningStorm,
                        "source skill 65 Lightning Storm row or supporting rows differ");
                lightningStormSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s lightning-storm-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d%n",
                        source, lightningStorm.database(),
                        lightningStorm.effectSkillIdColumn(),
                        lightningStorm.reuseDelayMillis(), lightningStorm.itemRows(),
                        lightningStorm.abilityRows(), lightningStorm.bossRows(),
                        lightningStorm.probabilityRows());
                LightningStormSourceEvidence meteorStrike =
                        meteorStrikeSkillRow(repository);
                LightningStormSourceEvidence expectedMeteorStrike =
                        switch (meteorStrike.database()) {
                            case "380_db" -> new LightningStormSourceEvidence(
                                    "380_db", true, 0, 3_000, 1, 0, 0, 0);
                            case "380yiwei_db" -> new LightningStormSourceEvidence(
                                    "380yiwei_db", false, 0, 1_500, 1, 0, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + meteorStrike.database());
                        };
                assertEquals(expectedMeteorStrike, meteorStrike,
                        "source skill 74 Meteor Strike row or supporting rows differ");
                meteorStrikeSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s meteor-strike-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d%n",
                        source, meteorStrike.database(),
                        meteorStrike.effectSkillIdColumn(),
                        meteorStrike.reuseDelayMillis(), meteorStrike.itemRows(),
                        meteorStrike.abilityRows(), meteorStrike.bossRows(),
                        meteorStrike.probabilityRows());
                LightningStormSourceEvidence greaterResurrection =
                        greaterResurrectionSkillRow(repository);
                LightningStormSourceEvidence expectedGreaterResurrection =
                        switch (greaterResurrection.database()) {
                            case "380_db" -> new LightningStormSourceEvidence(
                                    "380_db", true, 0, 10, 1, 0, 0, 0);
                            case "380yiwei_db" -> new LightningStormSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 1, 0, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + greaterResurrection.database());
                        };
                assertEquals(expectedGreaterResurrection, greaterResurrection,
                        "source skill 75 Greater Resurrection row or supporting rows differ");
                greaterResurrectionSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s greater-resurrection-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d%n",
                        source, greaterResurrection.database(),
                        greaterResurrection.effectSkillIdColumn(),
                        greaterResurrection.reuseDelayMillis(),
                        greaterResurrection.itemRows(),
                        greaterResurrection.abilityRows(),
                        greaterResurrection.bossRows(),
                        greaterResurrection.probabilityRows());
                LightningStormSourceEvidence massSlow = massSlowSkillRow(repository);
                LightningStormSourceEvidence expectedMassSlow =
                        switch (massSlow.database()) {
                            case "380_db" -> new LightningStormSourceEvidence(
                                    "380_db", true, 0, 3_000, 1, 0, 1, 0);
                            case "380yiwei_db" -> new LightningStormSourceEvidence(
                                    "380yiwei_db", false, 0, 3_000, 1, 0, 1, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + massSlow.database());
                        };
                assertEquals(expectedMassSlow, massSlow,
                        "source skill 76 Mass Slow row or supporting rows differ");
                massSlowSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s mass-slow-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-modifier3-rows=%d"
                                + " probability-rows=%d%n",
                        source, massSlow.database(), massSlow.effectSkillIdColumn(),
                        massSlow.reuseDelayMillis(), massSlow.itemRows(),
                        massSlow.abilityRows(), massSlow.bossRows(),
                        massSlow.probabilityRows());
                LightningStormSourceEvidence disintegrate =
                        disintegrateSkillRow(repository);
                LightningStormSourceEvidence expectedDisintegrate =
                        switch (disintegrate.database()) {
                            case "380_db" -> new LightningStormSourceEvidence(
                                    "380_db", true, 0, 5_000, 1, 0, 0, 0);
                            case "380yiwei_db" -> new LightningStormSourceEvidence(
                                    "380yiwei_db", false, 0, 5_000, 1, 0, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + disintegrate.database());
                        };
                assertEquals(expectedDisintegrate, disintegrate,
                        "source skill 77 Disintegrate row or supporting rows differ");
                disintegrateSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s disintegrate-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d%n",
                        source, disintegrate.database(),
                        disintegrate.effectSkillIdColumn(),
                        disintegrate.reuseDelayMillis(), disintegrate.itemRows(),
                        disintegrate.abilityRows(), disintegrate.bossRows(),
                        disintegrate.probabilityRows());
                AbsoluteBarrierSourceEvidence absoluteBarrier =
                        absoluteBarrierSkillRow(repository);
                AbsoluteBarrierSourceEvidence expectedAbsoluteBarrier =
                        switch (absoluteBarrier.database()) {
                            case "380_db" -> new AbsoluteBarrierSourceEvidence(
                                    "380_db", true, 0, 12_000, 1, 0, 0, 0, 1);
                            case "380yiwei_db" -> new AbsoluteBarrierSourceEvidence(
                                    "380yiwei_db", false, 0, 12_000, 1, 0, 0, 0, 1);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + absoluteBarrier.database());
                        };
                assertEquals(expectedAbsoluteBarrier, absoluteBarrier,
                        "source skill 78 Absolute Barrier row or supporting rows differ");
                absoluteBarrierSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s absolute-barrier-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d cancellation-immune-rows=%d%n",
                        source, absoluteBarrier.database(),
                        absoluteBarrier.effectSkillIdColumn(),
                        absoluteBarrier.reuseDelayMillis(), absoluteBarrier.itemRows(),
                        absoluteBarrier.abilityRows(), absoluteBarrier.bossRows(),
                        absoluteBarrier.probabilityRows(),
                        absoluteBarrier.cancellationImmuneRows());
                AdvanceSpiritSourceEvidence advanceSpirit =
                        advanceSpiritSkillRow(repository);
                AdvanceSpiritSourceEvidence expectedAdvanceSpirit =
                        switch (advanceSpirit.database()) {
                            case "380_db" -> new AdvanceSpiritSourceEvidence(
                                    "380_db", true, 0, 10, 1, 0, 0, 0, 1);
                            case "380yiwei_db" -> new AdvanceSpiritSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 1, 0, 0, 0, 1);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + advanceSpirit.database());
                        };
                assertEquals(expectedAdvanceSpirit, advanceSpirit,
                        "source skill 79 Advance Spirit row or supporting rows differ");
                advanceSpiritSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s advance-spirit-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-rows=%d cancellation-immune-rows=%d%n",
                        source, advanceSpirit.database(),
                        advanceSpirit.effectSkillIdColumn(),
                        advanceSpirit.reuseDelayMillis(), advanceSpirit.itemRows(),
                        advanceSpirit.abilityRows(), advanceSpirit.bossRows(),
                        advanceSpirit.probabilityRows(),
                        advanceSpirit.cancellationImmuneRows());
                ShapeChangeSourceEvidence shapeChange =
                        shapeChangeSkillRow(repository);
                ShapeChangeSourceEvidence expectedShapeChange =
                        switch (shapeChange.database()) {
                            case "380_db" -> new ShapeChangeSourceEvidence(
                                    "380_db", true, 0, 10, 1, 0, 96);
                            case "380yiwei_db" -> new ShapeChangeSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 1, 0, 96);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + shapeChange.database());
                        };
                assertEquals(expectedShapeChange, shapeChange,
                        "source skill 67 Shape Change row or polymorph menu differs");
                shapeChangeSkillRows++;
                shapeChangeMenuRows += shapeChange.menuRows();
                System.out.printf(
                        "[LiTo-test] source=%s shape-change-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d menu-rows=%d%n",
                        source, shapeChange.database(),
                        shapeChange.effectSkillIdColumn(),
                        shapeChange.reuseDelayMillis(), shapeChange.itemRows(),
                        shapeChange.abilityRows(), shapeChange.menuRows());
                ImmuneToHarmSourceEvidence immuneToHarm =
                        immuneToHarmSkillRow(repository);
                ImmuneToHarmSourceEvidence expectedImmuneToHarm =
                        switch (immuneToHarm.database()) {
                            case "380_db" -> new ImmuneToHarmSourceEvidence(
                                    "380_db", true, 0, 1_500, 1, 0);
                            case "380yiwei_db" -> new ImmuneToHarmSourceEvidence(
                                    "380yiwei_db", false, 0, 1_500, 1, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + immuneToHarm.database());
                        };
                assertEquals(expectedImmuneToHarm, immuneToHarm,
                        "source skill 68 Immune to Harm row differs");
                immuneToHarmSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s immune-to-harm-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d divisor=2%n",
                        source, immuneToHarm.database(),
                        immuneToHarm.effectSkillIdColumn(),
                        immuneToHarm.reuseDelayMillis(), immuneToHarm.itemRows(),
                        immuneToHarm.abilityRows());
                MassTeleportSourceEvidence massTeleport =
                        massTeleportSkillRow(repository);
                MassTeleportSourceEvidence expectedMassTeleport =
                        switch (massTeleport.database()) {
                            case "380_db" -> new MassTeleportSourceEvidence(
                                    "380_db", true, 0, 10, 1, 0);
                            case "380yiwei_db" -> new MassTeleportSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 1, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + massTeleport.database());
                        };
                assertEquals(expectedMassTeleport, massTeleport,
                        "source skill 69 Mass Teleport row differs");
                massTeleportSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s mass-teleport-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d%n",
                        source, massTeleport.database(),
                        massTeleport.effectSkillIdColumn(),
                        massTeleport.reuseDelayMillis(), massTeleport.itemRows(),
                        massTeleport.abilityRows());
                FireStormSourceEvidence fireStorm = fireStormSkillRow(repository);
                FireStormSourceEvidence expectedFireStorm = switch (fireStorm.database()) {
                    case "380_db" -> new FireStormSourceEvidence(
                            "380_db", true, 0, 2_000, 1, 0);
                    case "380yiwei_db" -> new FireStormSourceEvidence(
                            "380yiwei_db", false, 0, 400, 1, 0);
                    default -> throw new IllegalArgumentException(
                            "unexpected retained resource database: "
                                    + fireStorm.database());
                };
                assertEquals(expectedFireStorm, fireStorm,
                        "source skill 70 Fire Storm row differs");
                fireStormSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s fire-storm-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d%n",
                        source, fireStorm.database(),
                        fireStorm.effectSkillIdColumn(), fireStorm.reuseDelayMillis(),
                        fireStorm.itemRows(), fireStorm.abilityRows());
                DecayPotionSourceEvidence decayPotion =
                        decayPotionSkillRow(repository);
                DecayPotionSourceEvidence expectedDecayPotion =
                        switch (decayPotion.database()) {
                            case "380_db" -> new DecayPotionSourceEvidence(
                                    "380_db", true, 0, 500, 1, 0, 1, 0);
                            case "380yiwei_db" -> new DecayPotionSourceEvidence(
                                    "380yiwei_db", false, 0, 500, 1, 0, 1, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + decayPotion.database());
                        };
                assertEquals(expectedDecayPotion, decayPotion,
                        "source skill 71 Decay Potion row differs");
                decayPotionSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s decay-potion-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-override-rows=%d%n",
                        source, decayPotion.database(),
                        decayPotion.effectSkillIdColumn(),
                        decayPotion.reuseDelayMillis(), decayPotion.itemRows(),
                        decayPotion.abilityRows(), decayPotion.bossRows(),
                        decayPotion.probabilityOverrideRows());
                CounterDetectionSourceEvidence counterDetection =
                        counterDetectionSkillRow(repository);
                CounterDetectionSourceEvidence expectedCounterDetection =
                        switch (counterDetection.database()) {
                            case "380_db" -> new CounterDetectionSourceEvidence(
                                    "380_db", true, 0, 500, 1, 0, 0, 0);
                            case "380yiwei_db" -> new CounterDetectionSourceEvidence(
                                    "380yiwei_db", false, 0, 0, 1, 0, 0, 0);
                            default -> throw new IllegalArgumentException(
                                    "unexpected retained resource database: "
                                            + counterDetection.database());
                        };
                assertEquals(expectedCounterDetection, counterDetection,
                        "source skill 72 Counter Detection row differs");
                counterDetectionSkillRows++;
                System.out.printf(
                        "[LiTo-test] source=%s counter-detection-skill-row=exact"
                                + " schema=%s effect-skill-id-column=%s reuse-ms=%d"
                                + " item-rows=%d ability-rows=%d boss-rows=%d"
                                + " probability-override-rows=%d%n",
                        source, counterDetection.database(),
                        counterDetection.effectSkillIdColumn(),
                        counterDetection.reuseDelayMillis(),
                        counterDetection.itemRows(), counterDetection.abilityRows(),
                        counterDetection.bossRows(),
                        counterDetection.probabilityOverrideRows());
                boolean hasProjectileResources = hasProjectileResources(repository);
                if (hasProjectileResources) projectileResourceSources++;
                if (hasAttributeResources(repository)) attributeResourceSources++;
                int sourcePierceRows = pierceWeaponTemplateRows(repository);
                pierceTemplateRows += sourcePierceRows;
                System.out.printf("[LiTo-test] source=%s weapon-pierce-template-rows=%d%n",
                        source, sourcePierceRows);
                MagicHitConfigurationEvidence magicHit =
                        magicHitConfigurationEvidence(repository);
                assertTrue(magicHit.tablesWithColumn() >= 4,
                        "source magic-hit pipeline lacks qualified columns");
                assertTrue(magicHit.nonzeroRows() > 0,
                        "source has no nonzero retained magic-hit configuration rows");
                magicHitTables += magicHit.tablesWithColumn();
                magicHitResourceRows += magicHit.nonzeroResourceRows();
                magicHitDarkRows += magicHit.darkRows();
                System.out.printf(
                        "[LiTo-test] source=%s magic-hit-tables=%d"
                                + " nonzero-resource-rows=%d dark-rows=%d%n",
                        source, magicHit.tablesWithColumn(),
                        magicHit.nonzeroResourceRows(), magicHit.darkRows());
                MagicHitConfigurationEvidence blind =
                        blindResistanceConfigurationEvidence(repository);
                System.out.printf(
                        "[LiTo-test] source=%s blind-resistance-tables=%d"
                                + " nonzero-resource-rows=%d dark-rows=%d%n",
                        source, blind.tablesWithColumn(),
                        blind.nonzeroResourceRows(), blind.darkRows());
                assertTrue(blind.tablesWithColumn() >= 1,
                        "source blind-resistance pipeline lacks qualified columns");
                assertTrue(blind.nonzeroRows() > 0,
                        "source has no nonzero retained blind-resistance rows");
                blindResistanceTables += blind.tablesWithColumn();
                blindResistanceResourceRows += blind.nonzeroResourceRows();
                blindResistanceDarkRows += blind.darkRows();
                SpecialWeaponEvidence special = specialWeaponEvidence(repository);
                specialWeaponRows += special.total();
                assertEquals(new SpecialWeaponEvidence(69, 24, 28, 9, 8), special);
                System.out.printf(
                        "[LiTo-test] source=%s special-weapon-rows=%d"
                                + " claw=%d edoryu=%d kiringku=%d chainsword=%d%n",
                        source, special.total(), special.claw(), special.edoryu(),
                        special.kiringku(), special.chainsword());
                KiringkuEffectEvidence kiringku = kiringkuEffectEvidence(repository);
                assertEquals(new KiringkuEffectEvidence(9, 1, 8), kiringku);
                System.out.printf(
                        "[LiTo-test] source=%s kiringku-rows=%d"
                                + " effect-6983=%d effect-7049=%d%n",
                        source, kiringku.total(), kiringku.special3018(),
                        kiringku.defaultOther());
                ChainswordEvidence chainsword = chainswordEvidence(repository);
                assertEquals(new ChainswordEvidence(8, 8, 1), chainsword);
                System.out.printf(
                        "[LiTo-test] source=%s chainsword-rows=%d range-two=%d"
                                + " item-272-exact=%d%n",
                        source, chainsword.total(), chainsword.rangeTwo(),
                        chainsword.item272Exact());
                long defensiveObjectId = firstDefensiveCharacter(repository);
                if (defensiveObjectId != 0) {
                    SourceEquipmentPhysicalStats defensive = repository
                            .equipmentMagicStats(defensiveObjectId).physical();
                    SourceDefensiveEquipment projected = defensive.defensiveEquipment();
                    assertTrue(projected.present());
                    assertEquals(25, projected.useType());
                    assertTrue(projected.icon() > 0);
                    assertTrue(!projected.nameId().isBlank());
                    assertEquals(projected.armorType() == 7,
                            defensive.shieldEquipped());
                    assertEquals(projected.armorType() == 13,
                            defensive.guarderEquipped());
                    defensiveProjectionSources++;
                }
                long objectId = firstEquippedCharacter(repository);
                // An installed source schema can legitimately be empty. Its
                // metadata was still validated by healthCheck; exercise every
                // populated source and require at least one real replay.
                if (objectId == 0) continue;
                SourceEquipmentMagicStats equipment =
                        repository.equipmentMagicStats(objectId);
                probedCharacters++;
                assertNotNull(equipment.physical());
                assertNotNull(equipment.physical().weapon());
                assertTrue(equipment.matchedEquipmentRows() > 0);
                assertEquals(hasEquippedItem(repository, objectId, 20014),
                        equipment.healingHelmEquipped());
                assertEquals(hasEquippedItem(repository, objectId, 20008)
                                || hasEquippedItem(repository, objectId, 20013),
                        equipment.hasteCostHalvingHelmEquipped());
                assertEquals(hasEquippedItem(repository, objectId, 20023),
                        equipment.greaterHasteCostHalvingHelmEquipped());
                System.out.printf(
                        "[LiTo-test] source=%s character=%d item-ac=%d set-ac=%d"
                                + " ac-modifier=%d armor-reduction-base=%d"
                                + " item-magic-hit=%d set-magic-hit=%d magic-hit=%d"
                                + " item-blind=%d set-blind=%d blind=%d"
                                + " item-sleep=%d set-sleep=%d sleep=%d"
                                + " item-freeze=%d set-freeze=%d freeze=%d"
                                + " item-stone=%d set-stone=%d stone=%d"
                                + " item-sustain=%d set-sustain=%d sustain=%d"
                                + " ignore-reduction=%d melee-reduction=%d"
                                + " ranged-reduction=%d healing-helm=%s%n",
                        source, objectId,
                        equipment.physical().itemArmorClassModifier(),
                        equipment.physical().setArmorClassValue(),
                        equipment.physical().armorClassModifier(),
                        equipment.armorReductionBase(),
                        equipment.itemMagicHit(), equipment.setMagicHit(),
                        equipment.totalMagicHit(),
                        equipment.itemBlindResistance(), equipment.setBlindResistance(),
                        equipment.totalBlindResistance(),
                        equipment.itemSleepResistance(), equipment.setSleepResistance(),
                        equipment.totalSleepResistance(),
                        equipment.itemFreezeResistance(), equipment.setFreezeResistance(),
                        equipment.totalFreezeResistance(),
                        equipment.itemStoneResistance(), equipment.setStoneResistance(),
                        equipment.totalStoneResistance(),
                        equipment.itemSustainResistance(), equipment.setSustainResistance(),
                        equipment.totalSustainResistance(),
                        equipment.physical().totalIgnoreReduction(),
                        equipment.physical().directionalReduction(false),
                        equipment.physical().directionalReduction(true),
                        equipment.healingHelmEquipped());
                assertEquals(0, equipment.totalSleepResistance(),
                        "selected fixture character sleep resistance changed");
                assertEquals(0, equipment.totalFreezeResistance(),
                        "selected fixture character freeze resistance changed");
                assertEquals(0, equipment.totalStoneResistance(),
                        "selected fixture character stone resistance changed");
                assertEquals(0, equipment.totalSustainResistance(),
                        "selected fixture character sustain resistance changed");
                if (equipment.physical().weapon().present()) {
                    SourceWeaponPhysicalStats weapon = equipment.physical().weapon();
                    assertTrue(!weapon.nameId().isBlank(),
                            "equipped legacy weapon lost its native name_id");
                    assertTrue(weapon.damageSmall() >= 0);
                    assertTrue(weapon.combatTraits().material() >= 0);
                    assertTrue(weapon.combatTraits().blessing() <= 3);
                    System.out.printf(
                            "[LiTo-test] source=%s character=%d weapon=%d material=%d"
                                    + " blessing=%d attribute=%d/%d/%d pierce=%d"
                                    + " enchant=%d durability=%d%n",
                            source, objectId, weapon.itemId(),
                            weapon.combatTraits().material(),
                            weapon.combatTraits().blessing(),
                            weapon.combatTraits().attributeKind(),
                            weapon.combatTraits().attributeLevel(),
                            weapon.combatTraits().attributeDamage(),
                            equipment.physical().totalPierce(),
                            weapon.enchantLevel(), weapon.durability());
                }
                long attributeObjectId = firstAttributeWeaponCharacter(repository);
                if (attributeObjectId != 0) {
                    SourceWeaponPhysicalStats attributeWeapon = repository
                            .equipmentMagicStats(attributeObjectId).physical().weapon();
                    assertTrue(attributeWeapon.combatTraits().attributeKind() > 0);
                    assertTrue(attributeWeapon.combatTraits().attributeDamage() > 0);
                    System.out.printf(
                            "[LiTo-test] source=%s attribute-character=%d weapon=%d"
                                    + " attribute=%d/%d/%d%n",
                            source, attributeObjectId, attributeWeapon.itemId(),
                            attributeWeapon.combatTraits().attributeKind(),
                            attributeWeapon.combatTraits().attributeLevel(),
                            attributeWeapon.combatTraits().attributeDamage());
                }
                long rangedObjectId = hasProjectileResources
                        ? firstRangedCharacterWithProjectile(repository) : 0;
                if (rangedObjectId != 0) {
                    SourceWeaponPhysicalStats rangedWeapon = repository
                            .equipmentMagicStats(rangedObjectId).physical().weapon();
                    assertTrue(rangedWeapon.ranged());
                    assertTrue(rangedWeapon.projectile().present());
                    assertTrue(rangedWeapon.projectile().objectId() > 0);
                    assertTrue(rangedWeapon.projectile().count() > 0);
                }
            }
            assertTrue(probedCharacters > 0,
                    "configured legacy sources contain no equipped character to probe");
            assertTrue(projectileResourceSources > 0,
                    "configured legacy sources contain no etcitem_arrow resource rows");
            assertTrue(defensiveProjectionSources > 0,
                    "configured legacy sources contain no equipped shield/guarder projection");
            assertTrue(attributeResourceSources > 0,
                    "configured legacy sources contain no attribute weapon rows");
            assertTrue(pierceTemplateRows > 0,
                    "configured legacy sources contain no weapon pierce template rows");
            assertEquals(config.classicServerNames().size() * 69, specialWeaponRows,
                    "not every source has the qualified special-weapon rows");
            assertEquals(config.classicServerNames().size() * ENCOUNTER_NPC_IDS.length,
                    npcPhysicalRows, "not every encounter NPC has a qualified physical row");
            assertEquals(config.classicServerNames().size() * 31,
                    summonTemplateRows,
                    "not every source has all 31 qualified summon-monster rows");
            assertEquals(config.classicServerNames().size(), petTemplateRows,
                    "not every source has the qualified shepherd-dog pet row");
            assertEquals(config.classicServerNames().size(), holyWalkSkillRows,
                    "not every source has the exact skill 52 Holy Walk row");
            assertEquals(config.classicServerNames().size(), tornadoSkillRows,
                    "not every source has the exact skill 53 Tornado row");
            assertEquals(config.classicServerNames().size(), greaterHasteSkillRows,
                    "not every source has the exact skill 54 Greater Haste row");
            assertEquals(config.classicServerNames().size(), berserkersSkillRows,
                    "not every source has the exact skill 55 Berserkers rows");
            assertEquals(config.classicServerNames().size(), diseaseSkillRows,
                    "not every source has the exact skill 56 Disease rows");
            assertEquals(config.classicServerNames().size(), fullHealSkillRows,
                    "not every source has the exact skill 57 Full Heal rows");
            assertEquals(config.classicServerNames().size(), blizzardSkillRows,
                    "not every source has the exact skill 59 Blizzard row");
            assertEquals(config.classicServerNames().size(), earthquakeSkillRows,
                    "not every source has the exact skill 62 Earthquake row");
            assertEquals(config.classicServerNames().size(), silenceSkillRows,
                    "not every source has the exact skill 64 Silence rows");
            assertEquals(config.classicServerNames().size(), lightningStormSkillRows,
                    "not every source has the exact skill 65 Lightning Storm rows");
            assertEquals(config.classicServerNames().size(), meteorStrikeSkillRows,
                    "not every source has the exact skill 74 Meteor Strike rows");
            assertEquals(config.classicServerNames().size(), greaterResurrectionSkillRows,
                    "not every source has the exact skill 75 Greater Resurrection rows");
            assertEquals(config.classicServerNames().size(), massSlowSkillRows,
                    "not every source has the exact skill 76 Mass Slow rows");
            assertEquals(config.classicServerNames().size(), disintegrateSkillRows,
                    "not every source has the exact skill 77 Disintegrate rows");
            assertEquals(config.classicServerNames().size(), absoluteBarrierSkillRows,
                    "not every source has the exact skill 78 Absolute Barrier rows");
            assertEquals(config.classicServerNames().size(), advanceSpiritSkillRows,
                    "not every source has the exact skill 79 Advance Spirit rows");
            assertEquals(config.classicServerNames().size(), shapeChangeSkillRows,
                    "not every source has the exact skill 67 Shape Change rows");
            assertEquals(config.classicServerNames().size() * 96, shapeChangeMenuRows,
                    "not every source matches all 96 native Shape Change menu rows");
            assertEquals(config.classicServerNames().size(), immuneToHarmSkillRows,
                    "not every source has the exact skill 68 Immune to Harm row");
            assertEquals(config.classicServerNames().size(), massTeleportSkillRows,
                    "not every source has the exact skill 69 Mass Teleport row");
            assertEquals(config.classicServerNames().size(), fireStormSkillRows,
                    "not every source has the exact skill 70 Fire Storm row");
            assertEquals(config.classicServerNames().size(), decayPotionSkillRows,
                    "not every source has the exact skill 71 Decay Potion rows");
            assertEquals(config.classicServerNames().size(), counterDetectionSkillRows,
                    "not every source has the exact skill 72 Counter Detection rows");
            assertEquals(config.classicServerNames().size() * 9, magicHitTables,
                    "selected source magic-hit column count changed");
            assertEquals(3, magicHitResourceRows,
                    "selected source nonzero magic-hit row count changed");
            assertEquals(0, magicHitDarkRows,
                    "selected account dark magic-hit rows changed");
            assertEquals(7, blindResistanceTables,
                    "selected source blind-resistance column count changed");
            assertEquals(10, blindResistanceResourceRows,
                    "selected source nonzero blind-resistance rows changed");
            assertEquals(0, blindResistanceDarkRows,
                    "selected account dark blind-resistance rows changed");
        }
    }

    private static long firstEquippedCharacter(AccountRepository repository) throws Exception {
        String url = stringField(repository, "accountUrl");
        String login = stringField(repository, "accountLogin");
        String password = stringField(repository, "accountPassword");
        try (Connection connection = DriverManager.getConnection(url, login, password)) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT c.objid
                      FROM characters c
                      JOIN character_items i ON i.char_id=c.objid AND i.is_equipped=1
                     WHERE c.Banned=0
                     ORDER BY c.objid
                     LIMIT 1
                    """); ResultSet row = statement.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        }
    }

    private static boolean hasProjectileResources(AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA=? AND TABLE_NAME='etcitem_arrow' LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return false;
            }
        }
        String rowSql = "SELECT 1 FROM " + LegacyEquipmentMagicLoader.qualified(
                resourceDatabase, "etcitem_arrow") + " LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(rowSql);
             ResultSet row = statement.executeQuery()) {
            return row.next();
        }
    }

    private static long firstDefensiveCharacter(AccountRepository repository)
            throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String armor = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "armor");
        String sql = "SELECT c.objid FROM characters c "
                + "JOIN character_items i ON i.char_id=c.objid AND i.is_equipped=1 "
                + "JOIN " + armor + " a ON a.item_id=i.item_id "
                + "WHERE c.Banned=0 AND a.type IN ('shield','guarder') "
                + "ORDER BY c.objid LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getLong(1) : 0;
        }
    }

    private static HolyWalkSourceEvidence holyWalkSkillRow(AccountRepository repository)
            throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        // Keep the production equipment-table allowlist closed. This explicit
        // integration probe fixes the table name and validates the schema
        // identifier independently before constructing the read-only query.
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=52"
                + " AND skill_level=7 AND skill_number=3"
                + " AND mpConsume=15 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=32"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=8 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=8 AND nameid='$1469'"
                + " AND action_id=19 AND castgfx=3936 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0 AND sysmsgID_fail=0";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 52 Holy Walk source row");
            int reuseDelay = row.getInt("reuseDelay");
            int effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 52 Holy Walk source row");
            return new HolyWalkSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay);
        }
    }

    private static TornadoSourceEvidence tornadoSkillRow(AccountRepository repository)
            throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=53"
                + " AND skill_level=7 AND skill_number=4"
                + " AND mpConsume=40 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='none' AND target_to=3"
                + " AND damage_value=16 AND damage_dice=10 AND damage_dice_count=5"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=8 AND type=64 AND lawful=0"
                + " AND ranged=0 AND area=4 AND through=0"
                + " AND id=16 AND nameid='$1470'"
                + " AND action_id=18 AND castgfx=758 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0 AND sysmsgID_fail=0";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 53 Tornado source row");
            int reuseDelay = row.getInt("reuseDelay");
            int effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 53 Tornado source row");
            return new TornadoSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay);
        }
    }

    private static BlizzardSourceEvidence blizzardSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=59"
                + " AND skill_level=8 AND skill_number=2"
                + " AND mpConsume=60 AND hpConsume=10"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='none' AND target_to=3"
                + " AND damage_value=2 AND damage_dice=10 AND damage_dice_count=3"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=4 AND type=64 AND lawful=-10"
                + " AND ranged=0 AND area=10 AND through=0"
                + " AND id=4 AND nameid='$1473'"
                + " AND action_id=18 AND castgfx=757 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0 AND sysmsgID_fail=0";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 59 Blizzard source row");
            int reuseDelay = row.getInt("reuseDelay");
            int effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 59 Blizzard source row");
            return new BlizzardSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay);
        }
    }

    private static EarthquakeSourceEvidence earthquakeSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT skill_level,skill_number,mpConsume,hpConsume,"
                + "itemConsumeId,itemConsumeCount,reuseDelay,buffDuration,target,target_to,"
                + "damage_value,damage_dice,damage_dice_count,probability_value,"
                + "probability_dice,attr,type,lawful,ranged,area,through,id,nameid,"
                + "action_id,castgfx,castgfx2,sysmsgID_happen,sysmsgID_stop,sysmsgID_fail"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills + " WHERE skill_id=62";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 62 Earthquake source row");
            assertEquals(8, row.getInt("skill_level"));
            assertEquals(5, row.getInt("skill_number"));
            assertEquals(40, row.getInt("mpConsume"));
            assertEquals(0, row.getInt("hpConsume"));
            assertEquals(0, row.getInt("itemConsumeId"));
            assertEquals(0, row.getInt("itemConsumeCount"));
            assertEquals(0, row.getInt("buffDuration"));
            assertEquals("none", row.getString("target"));
            assertEquals(3, row.getInt("target_to"));
            assertEquals(32, row.getInt("damage_value"));
            assertEquals(10, row.getInt("damage_dice"));
            assertEquals(6, row.getInt("damage_dice_count"));
            assertEquals(0, row.getInt("probability_value"));
            assertEquals(0, row.getInt("probability_dice"));
            assertEquals(1, row.getInt("attr"));
            assertEquals(64, row.getInt("type"));
            assertEquals(0, row.getInt("lawful"));
            assertEquals(0, row.getInt("ranged"));
            assertEquals(3, row.getInt("area"));
            assertEquals(0, row.getInt("through"));
            assertEquals(32, row.getInt("id"));
            assertEquals("$1974", row.getString("nameid"));
            assertEquals(18, row.getInt("action_id"));
            assertEquals(1812, row.getInt("castgfx"));
            assertEquals(0, row.getInt("castgfx2"));
            assertEquals(0, row.getInt("sysmsgID_happen"));
            assertEquals(0, row.getInt("sysmsgID_stop"));
            assertEquals(0, row.getInt("sysmsgID_fail"));
            int reuseDelay = row.getInt("reuseDelay");
            int effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 62 Earthquake source row");
            return new EarthquakeSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay);
        }
    }

    private static SilenceSourceEvidence silenceSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=64"
                + " AND skill_level=8 AND skill_number=7"
                + " AND mpConsume=50 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=1"
                + " AND buffDuration=16"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=10"
                + " AND attr=0 AND type=1 AND lawful=0"
                + " AND ranged=3 AND area=0 AND through=0"
                + " AND id=128 AND nameid='$1989'"
                + " AND action_id=19 AND castgfx=2177 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 64 Silence source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 64 Silence source row");
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫`"
                             + " WHERE skill_id=64 AND modifier=0");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=64 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        String probabilityTable = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','prob_high','prob_equal','prob_low')"
                             + " GROUP BY TABLE_NAME HAVING COUNT(DISTINCT COLUMN_NAME)=4")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) probabilityTable = row.getString(1);
                assertTrue(!row.next(), "multiple retained skill probability tables");
            }
        }
        assertNotNull(probabilityTable, "missing retained skill probability table");
        String trustedProbabilityTable = probabilityTable.replace("`", "``");
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedProbabilityTable + "`"
                             + " WHERE skill_id=64"
                             + " AND prob_high=55 AND prob_equal=45 AND prob_low=35"
                             + " AND bonus_str=0 AND bonus_dex=0 AND bonus_int=0.35"
                             + " AND check_resist_type=0 AND check_magic_level=0"
                             + " AND check_magic_hit=1 AND mr_multiplier=0.05"
                             + " AND con_multiplier=0 AND wis_multiplier=0");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new SilenceSourceEvidence(resourceDatabase, effectSkillIdColumn,
                effectSkillId, reuseDelay, bossRows, itemRows, probabilityRows);
    }

    private static LightningStormSourceEvidence lightningStormSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=65"
                + " AND skill_level=9 AND skill_number=0"
                + " AND mpConsume=48 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='attack' AND target_to=3"
                + " AND damage_value=20 AND damage_dice=10 AND damage_dice_count=5"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=8 AND type=64 AND lawful=0"
                + " AND ranged=10 AND area=3 AND through=0"
                + " AND id=1 AND nameid='$1476'"
                + " AND action_id=18 AND castgfx=3924 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 65 Lightning Storm source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 65 Lightning Storm source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=65 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=65");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫` WHERE skill_id=65");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面技能控制` WHERE skill_id=65");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new LightningStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, bossRows, probabilityRows);
    }

    private static LightningStormSourceEvidence meteorStrikeSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=74"
                + " AND skill_level=10 AND skill_number=1"
                + " AND mpConsume=60 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='attack' AND target_to=3"
                + " AND damage_value=110 AND damage_dice=15 AND damage_dice_count=8"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=2 AND type=64 AND lawful=0"
                + " AND ranged=10 AND area=3 AND through=0"
                + " AND id=2 AND nameid='$1482'"
                + " AND action_id=18 AND castgfx=762 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 74 Meteor Strike source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 74 Meteor Strike source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=74 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=74");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫` WHERE skill_id=74");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面技能控制` WHERE skill_id=74");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new LightningStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, bossRows, probabilityRows);
    }

    private static LightningStormSourceEvidence greaterResurrectionSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=75"
                + " AND skill_level=10 AND skill_number=2"
                + " AND mpConsume=100 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=1"
                + " AND buffDuration=0"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=32 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=4 AND nameid='$1483'"
                + " AND action_id=19 AND castgfx=3944 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(),
                    "missing exact skill 75 Greater Resurrection source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(),
                    "duplicate exact skill 75 Greater Resurrection source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=75 AND name='終極返生術'"
                             + " AND itemids IS NULL AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=75");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫` WHERE skill_id=75");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面技能控制` WHERE skill_id=75");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new LightningStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, bossRows, probabilityRows);
    }

    private static LightningStormSourceEvidence massSlowSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=76"
                + " AND skill_level=10 AND skill_number=3"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=2"
                + " AND buffDuration=64"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=7"
                + " AND attr=1 AND type=1 AND lawful=0"
                + " AND ranged=-1 AND area=3 AND through=0"
                + " AND id=8 AND nameid='$1484'"
                + " AND action_id=19 AND castgfx=3932 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 76 Mass Slow source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 76 Mass Slow source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=76 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=76");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫`"
                             + " WHERE skill_id=76 AND modifier=3");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面技能控制` WHERE skill_id=76");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new LightningStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, bossRows, probabilityRows);
    }

    private static LightningStormSourceEvidence disintegrateSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=77"
                + " AND skill_level=10 AND skill_number=4"
                + " AND mpConsume=70 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='attack' AND target_to=3"
                + " AND damage_value=190 AND damage_dice=15"
                + " AND damage_dice_count=8"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=16 AND type=64 AND lawful=1"
                + " AND ranged=10 AND area=0 AND through=0"
                + " AND id=16 AND nameid='$1485'"
                + " AND action_id=18 AND castgfx=11746 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 77 Disintegrate source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 77 Disintegrate source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=77 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=77");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫` WHERE skill_id=77");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int probabilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面技能控制` WHERE skill_id=77");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityRows = row.getInt(1);
        }
        return new LightningStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, bossRows, probabilityRows);
    }

    private static AbsoluteBarrierSourceEvidence absoluteBarrierSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=78"
                + " AND skill_level=10 AND skill_number=5"
                + " AND mpConsume=30 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=2"
                + " AND buffDuration=12"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0"
                + " AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=32 AND nameid='$1992'"
                + " AND action_id=19 AND castgfx=2234 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 78 Absolute Barrier source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 78 Absolute Barrier source row");
        }
        int itemRows = countSkillRows(repository, resourceDatabase,
                "skills_item", "skill_id=78 AND itemids IS NULL AND counts IS NULL");
        int abilityRows = countSkillRows(repository, resourceDatabase,
                "skills_buff_ability", "skill_id=78");
        int bossRows = countSkillRows(repository, resourceDatabase,
                "skills_負面boss免疫", "skill_id=78");
        int probabilityRows = countSkillRows(repository, resourceDatabase,
                "skills_負面技能控制", "skill_id=78");
        int cancellationImmuneRows = countSkillRows(repository, resourceDatabase,
                "skills_相消免疫技能", "skill_id=78 AND note='絕對屏障'");
        return new AbsoluteBarrierSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay, itemRows,
                abilityRows, bossRows, probabilityRows, cancellationImmuneRows);
    }

    private static AdvanceSpiritSourceEvidence advanceSpiritSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=79"
                + " AND skill_level=10 AND skill_number=6"
                + " AND mpConsume=20 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=1200"
                + " AND target='buff' AND target_to=1"
                + " AND damage_value=0 AND damage_dice=0"
                + " AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=64 AND nameid='$1993'"
                + " AND action_id=19 AND castgfx=3935 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 79 Advance Spirit source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 79 Advance Spirit source row");
        }
        int itemRows = countSkillRows(repository, resourceDatabase,
                "skills_item", "skill_id=79 AND itemids IS NULL AND counts IS NULL");
        int abilityRows = countSkillRows(repository, resourceDatabase,
                "skills_buff_ability", "skill_id=79");
        int bossRows = countSkillRows(repository, resourceDatabase,
                "skills_負面boss免疫", "skill_id=79");
        int probabilityRows = countSkillRows(repository, resourceDatabase,
                "skills_負面技能控制", "skill_id=79");
        int cancellationImmuneRows = countSkillRows(repository, resourceDatabase,
                "skills_相消免疫技能", "skill_id=79 AND note='靈魂昇華'");
        return new AdvanceSpiritSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay, itemRows,
                abilityRows, bossRows, probabilityRows, cancellationImmuneRows);
    }

    private static int countSkillRows(AccountRepository repository, String database,
                                      String table, String condition) throws Exception {
        if (!table.matches("[A-Za-z0-9_\\p{IsHan}]+")
                || !condition.matches("[A-Za-z0-9_='絕對屏障靈魂昇華 ]+")) {
            throw new IllegalArgumentException("invalid retained skill row query");
        }
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + database + ".`" + table
                             + "` WHERE " + condition);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            return row.getInt(1);
        }
    }

    private static ShapeChangeSourceEvidence shapeChangeSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=67"
                + " AND skill_level=9 AND skill_number=2"
                + " AND mpConsume=80 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=1"
                + " AND buffDuration=7200"
                + " AND target='buff' AND target_to=1"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=4 AND nameid='$1478'"
                + " AND action_id=19 AND castgfx=230 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 67 Shape Change source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 67 Shape Change source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=67 AND name='變形術'"
                             + " AND itemids IS NULL AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=67");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int menuRows = 0;
        String polymorphSql = "SELECT polyid,minlevel,weaponequip,armorequip,"
                + "isSkillUse,cause FROM " + resourceDatabase
                + ".`polymorphs` WHERE name=?";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(polymorphSql)) {
            for (BattlePolymorphCatalog.Form form
                    : BattlePolymorphCatalog.standardMenuForms()) {
                statement.setString(1, form.selection());
                try (ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing native polymorph row: "
                            + form.selection());
                    assertEquals(form.gfx(), row.getInt("polyid"), form.selection());
                    assertEquals(form.minimumLevel(), row.getInt("minlevel"),
                            form.selection());
                    assertEquals(form.weaponEquipMask(), row.getInt("weaponequip"),
                            form.selection());
                    assertEquals(form.armorEquipMask(), row.getInt("armorequip"),
                            form.selection());
                    assertEquals(form.skillUsable() ? 1 : 0,
                            row.getInt("isSkillUse"), form.selection());
                    assertEquals(form.causeMask(), row.getInt("cause"),
                            form.selection());
                    assertTrue(!row.next(), "duplicate native polymorph row: "
                            + form.selection());
                    menuRows++;
                }
            }
        }
        return new ShapeChangeSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows, menuRows);
    }

    private static ImmuneToHarmSourceEvidence immuneToHarmSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=68"
                + " AND skill_level=9 AND skill_number=3"
                + " AND mpConsume=30 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=2"
                + " AND buffDuration=32"
                + " AND target='buff' AND target_to=4"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=8 AND nameid='$1479'"
                + " AND action_id=19 AND castgfx=228 AND castgfx2=0"
                + " AND sysmsgID_happen=314 AND sysmsgID_stop=315"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 68 Immune to Harm source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 68 Immune to Harm source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=68 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=68");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        return new ImmuneToHarmSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows);
    }

    private static MassTeleportSourceEvidence massTeleportSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=69"
                + " AND skill_level=9 AND skill_number=4"
                + " AND mpConsume=60 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=1"
                + " AND buffDuration=0"
                + " AND target='none' AND target_to=4"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=128 AND lawful=0"
                + " AND ranged=0 AND area=2 AND through=0"
                + " AND id=16 AND nameid='$1480'"
                + " AND action_id=19 AND castgfx=2236 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 69 Mass Teleport source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 69 Mass Teleport source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=69 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=69");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        return new MassTeleportSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows);
    }

    private static FireStormSourceEvidence fireStormSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=70"
                + " AND skill_level=9 AND skill_number=5"
                + " AND mpConsume=48 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='none' AND target_to=3"
                + " AND damage_value=24 AND damage_dice=15 AND damage_dice_count=8"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=2 AND type=64 AND lawful=0"
                + " AND ranged=0 AND area=3 AND through=0"
                + " AND id=32 AND nameid='$1975'"
                + " AND action_id=18 AND castgfx=1819 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 70 Fire Storm source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 70 Fire Storm source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=70 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=70");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        return new FireStormSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay,
                itemRows, abilityRows);
    }

    private static DecayPotionSourceEvidence decayPotionSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=71"
                + " AND skill_level=9 AND skill_number=6"
                + " AND mpConsume=80 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=2"
                + " AND reuseDelay=500 AND buffDuration=16"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=8"
                + " AND attr=0 AND type=1 AND lawful=0"
                + " AND ranged=3 AND area=0 AND through=0"
                + " AND id=64 AND nameid='$1990'"
                + " AND action_id=19 AND castgfx=2232 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 71 Decay Potion source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 71 Decay Potion source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=71 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=71");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        String bossImmunityTable = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','modifier') AND TABLE_NAME LIKE 'skills%'"
                             + " GROUP BY TABLE_NAME"
                             + " HAVING COUNT(DISTINCT COLUMN_NAME)=2")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) bossImmunityTable = row.getString(1);
                assertTrue(!row.next(), "multiple retained boss-immunity tables");
            }
        }
        assertNotNull(bossImmunityTable,
                "missing retained skill boss-immunity table");
        String trustedBossImmunityTable = bossImmunityTable.replace("`", "``");
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedBossImmunityTable + "`"
                             + " WHERE skill_id=71 AND modifier=0");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        String probabilityTable = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','prob_high','prob_equal','prob_low')"
                             + " GROUP BY TABLE_NAME"
                             + " HAVING COUNT(DISTINCT COLUMN_NAME)=4")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) probabilityTable = row.getString(1);
                assertTrue(!row.next(), "multiple retained skill probability tables");
            }
        }
        assertNotNull(probabilityTable, "missing retained skill probability table");
        String trustedProbabilityTable = probabilityTable.replace("`", "``");
        int probabilityOverrideRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedProbabilityTable + "` WHERE skill_id=71");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityOverrideRows = row.getInt(1);
        }
        return new DecayPotionSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay, itemRows,
                abilityRows, bossRows, probabilityOverrideRows);
    }

    private static CounterDetectionSourceEvidence counterDetectionSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String skillSql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=72"
                + " AND skill_level=9 AND skill_number=7"
                + " AND mpConsume=30 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='none' AND target_to=3"
                + " AND damage_value=24 AND damage_dice=10 AND damage_dice_count=4"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=64 AND lawful=0"
                + " AND ranged=0 AND area=-1 AND through=0"
                + " AND id=128 AND nameid='$1991'"
                + " AND action_id=19 AND castgfx=3934 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=0";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(skillSql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(),
                    "missing exact skill 72 Counter Detection source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(),
                    "duplicate exact skill 72 Counter Detection source row");
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=72 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=72");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        String bossImmunityTable = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','modifier') AND TABLE_NAME LIKE 'skills%'"
                             + " GROUP BY TABLE_NAME"
                             + " HAVING COUNT(DISTINCT COLUMN_NAME)=2")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) bossImmunityTable = row.getString(1);
                assertTrue(!row.next(), "multiple retained boss-immunity tables");
            }
        }
        assertNotNull(bossImmunityTable,
                "missing retained skill boss-immunity table");
        String trustedBossImmunityTable = bossImmunityTable.replace("`", "``");
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedBossImmunityTable + "` WHERE skill_id=72");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        String probabilityTable = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','prob_high','prob_equal','prob_low')"
                             + " GROUP BY TABLE_NAME"
                             + " HAVING COUNT(DISTINCT COLUMN_NAME)=4")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) probabilityTable = row.getString(1);
                assertTrue(!row.next(), "multiple retained skill probability tables");
            }
        }
        assertNotNull(probabilityTable,
                "missing retained skill probability table");
        String trustedProbabilityTable = probabilityTable.replace("`", "``");
        int probabilityOverrideRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedProbabilityTable + "` WHERE skill_id=72");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            probabilityOverrideRows = row.getInt(1);
        }
        return new CounterDetectionSourceEvidence(resourceDatabase,
                effectSkillIdColumn, effectSkillId, reuseDelay, itemRows,
                abilityRows, bossRows, probabilityOverrideRows);
    }

    private static GreaterHasteSourceEvidence greaterHasteSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=54"
                + " AND skill_level=7 AND skill_number=5"
                + " AND mpConsume=60 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=2400"
                + " AND target='none' AND target_to=0"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=8 AND type=2 AND lawful=0"
                + " AND ranged=0 AND area=0 AND through=0"
                + " AND id=32 AND nameid='$1976'"
                + " AND action_id=19 AND castgfx=3104 AND castgfx2=0"
                + " AND sysmsgID_happen=184 AND sysmsgID_stop=185"
                + " AND sysmsgID_fail=0";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 54 Greater Haste source row");
            int reuseDelay = row.getInt("reuseDelay");
            int effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 54 Greater Haste source row");
            return new GreaterHasteSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay);
        }
    }

    private static BerserkersSourceEvidence berserkersSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        String abilities = resourceDatabase + ".`skills_buff_ability`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=55"
                + " AND skill_level=7 AND skill_number=6"
                + " AND mpConsume=40 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=320"
                + " AND target='buff' AND target_to=12"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=0 AND type=2 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=64 AND nameid='$1986'"
                + " AND action_id=19 AND castgfx=3943 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 55 Berserkers source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 55 Berserkers source row");
        }
        String abilitySql = "SELECT COUNT(*) total, SUM((ability_type='AC'"
                + " AND ability_value=10) OR (ability_type='DMG'"
                + " AND ability_value=5) OR (ability_type='HIT'"
                + " AND ability_value=2)) exact FROM " + abilities
                + " WHERE skill_id=55";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(abilitySql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing skill 55 ability source rows");
            int abilityRows = row.getInt("total");
            int exactAbilityRows = row.getInt("exact");
            assertTrue(!row.next(), "duplicate Berserkers aggregate row");
            return new BerserkersSourceEvidence(resourceDatabase,
                    effectSkillIdColumn, effectSkillId, reuseDelay,
                    abilityRows, exactAbilityRows);
        }
    }

    private static DiseaseSourceEvidence diseaseSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        String abilities = resourceDatabase + ".`skills_buff_ability`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=56"
                + " AND skill_level=7 AND skill_number=7"
                + " AND mpConsume=10 AND hpConsume=0"
                + " AND itemConsumeId=40318 AND itemConsumeCount=1"
                + " AND buffDuration=64"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=0 AND damage_dice=0 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=10"
                + " AND attr=0 AND type=4 AND lawful=-10"
                + " AND ranged=3 AND area=0 AND through=0"
                + " AND id=128 AND nameid='$1987'"
                + " AND action_id=19 AND castgfx=2230 AND castgfx2=0"
                + " AND sysmsgID_happen=0 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 56 Disease source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 56 Disease source row");
        }
        int abilityRows;
        int exactAbilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) total, SUM((ability_type='AC'"
                             + " AND ability_value=12) OR (ability_type='DMG'"
                             + " AND ability_value=-6)) exact FROM " + abilities
                             + " WHERE skill_id=56");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing skill 56 ability source rows");
            abilityRows = row.getInt("total");
            exactAbilityRows = row.getInt("exact");
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫`"
                             + " WHERE skill_id=56 AND modifier=5");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=56 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int probabilityOverrideRows = probabilityOverrideRows(
                repository, resourceDatabase, 56);
        return new DiseaseSourceEvidence(resourceDatabase, effectSkillIdColumn,
                effectSkillId, reuseDelay, abilityRows, exactAbilityRows,
                bossRows, itemRows, probabilityOverrideRows);
    }

    private static int probabilityOverrideRows(AccountRepository repository,
                                               String resourceDatabase,
                                               int skillId) throws Exception {
        String tableName = null;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND COLUMN_NAME IN"
                             + " ('skill_id','prob_high','prob_equal','prob_low')"
                             + " GROUP BY TABLE_NAME HAVING COUNT(DISTINCT COLUMN_NAME)=4")) {
            statement.setString(1, resourceDatabase);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) tableName = row.getString(1);
                assertTrue(!row.next(), "multiple retained skill probability tables");
            }
        }
        if (tableName == null) return 0;
        String trustedTable = tableName.replace("`", "``");
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`"
                             + trustedTable + "` WHERE skill_id=?")) {
            statement.setInt(1, skillId);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                return row.getInt(1);
            }
        }
    }

    private static FullHealSourceEvidence fullHealSkillRow(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        if (!resourceDatabase.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid retained resource database");
        }
        String skills = resourceDatabase + ".`skills`";
        boolean effectSkillIdColumn;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement column = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='skills'"
                             + " AND COLUMN_NAME='effect_skill_id'")) {
            column.setString(1, resourceDatabase);
            try (ResultSet row = column.executeQuery()) {
                effectSkillIdColumn = row.next() && row.getInt(1) == 1;
            }
        }
        String sql = "SELECT reuseDelay"
                + (effectSkillIdColumn ? ",effect_skill_id" : "")
                + " FROM " + skills
                + " WHERE skill_id=57"
                + " AND skill_level=8 AND skill_number=0"
                + " AND mpConsume=48 AND hpConsume=0"
                + " AND itemConsumeId=0 AND itemConsumeCount=0"
                + " AND buffDuration=0"
                + " AND target='buff' AND target_to=3"
                + " AND damage_value=15 AND damage_dice=17 AND damage_dice_count=0"
                + " AND probability_value=0 AND probability_dice=0"
                + " AND attr=12 AND type=16 AND lawful=0"
                + " AND ranged=-1 AND area=0 AND through=0"
                + " AND id=1 AND nameid='$1471'"
                + " AND action_id=19 AND castgfx=11725 AND castgfx2=0"
                + " AND sysmsgID_happen=77 AND sysmsgID_stop=0"
                + " AND sysmsgID_fail=280";
        int reuseDelay;
        int effectSkillId;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next(), "missing exact skill 57 Full Heal source row");
            reuseDelay = row.getInt("reuseDelay");
            effectSkillId = effectSkillIdColumn ? row.getInt("effect_skill_id") : 0;
            assertTrue(!row.next(), "duplicate exact skill 57 Full Heal source row");
        }
        int abilityRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_buff_ability` WHERE skill_id=57");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            abilityRows = row.getInt(1);
        }
        int bossRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase
                             + ".`skills_負面boss免疫` WHERE skill_id=57");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            bossRows = row.getInt(1);
        }
        int itemRows;
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + resourceDatabase + ".`skills_item`"
                             + " WHERE skill_id=57 AND itemids IS NULL"
                             + " AND counts IS NULL");
             ResultSet row = statement.executeQuery()) {
            assertTrue(row.next());
            itemRows = row.getInt(1);
        }
        int probabilityOverrideRows = probabilityOverrideRows(
                repository, resourceDatabase, 57);
        return new FullHealSourceEvidence(resourceDatabase, effectSkillIdColumn,
                effectSkillId, reuseDelay, abilityRows, bossRows, itemRows,
                probabilityOverrideRows);
    }

    private static boolean hasEquippedItem(AccountRepository repository,
                                           long characterObjectId, int itemId)
            throws Exception {
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM character_items"
                             + " WHERE char_id=? AND item_id=? AND is_equipped=1 LIMIT 1")) {
            statement.setLong(1, characterObjectId);
            statement.setInt(2, itemId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    private static boolean hasAttributeResources(AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String sql = "SELECT 1 FROM " + LegacyEquipmentMagicLoader.qualified(
                resourceDatabase, "裝備_weapon_屬性") + " LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next();
        }
    }

    private static int pierceWeaponTemplateRows(AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String sql = "SELECT COUNT(*) FROM " + LegacyEquipmentMagicLoader.qualified(
                resourceDatabase, "weapon") + " WHERE add_pierce<>0";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getInt(1) : 0;
        }
    }

    private static MagicHitConfigurationEvidence magicHitConfigurationEvidence(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String[] tables = {
                "armor", "weapon",
                "裝備_weapon_greater_能力", "裝備_armor_greater_能力",
                "裝備_weapon_bless_能力", "裝備_armor_bless_能力",
                "裝備_weapon_bless_指定", "裝備_armor_bless_指定",
                "裝備_weapon_enchant_指定", "裝備_armor_enchant_指定",
                "裝備_weapon_enchant_能力", "裝備_armor_enchant_能力",
                "裝備_特殊_卡片_能力", "裝備_特殊_套裝_能力",
                "裝備_特殊_卡片_組合"
        };
        int tablesWithColumn = 0;
        int nonzeroResourceRows = 0;
        try (Connection connection = repositoryConnection(repository)) {
            for (String table : tables) {
                try (PreparedStatement column = connection.prepareStatement(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                                + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
                                + " AND COLUMN_NAME='add_magic_hit'")) {
                    column.setString(1, resourceDatabase);
                    column.setString(2, table);
                    try (ResultSet row = column.executeQuery()) {
                        if (!row.next() || row.getInt(1) == 0) continue;
                    }
                }
                tablesWithColumn++;
                String sql = "SELECT COUNT(*) FROM "
                        + LegacyEquipmentMagicLoader.qualified(resourceDatabase, table)
                        + " WHERE add_magic_hit<>0";
                try (PreparedStatement count = connection.prepareStatement(sql);
                     ResultSet row = count.executeQuery()) {
                    int nonzero = row.next() ? row.getInt(1) : 0;
                    nonzeroResourceRows += nonzero;
                    if (nonzero > 0) {
                        System.out.printf(
                                "[LiTo-test] resource=%s magic-hit-table=%s nonzero=%d%n",
                                resourceDatabase, table, nonzero);
                    }
                }
            }
            int darkRows;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM character_items_dark WHERE add_magic_hit<>0");
                 ResultSet row = count.executeQuery()) {
                darkRows = row.next() ? row.getInt(1) : 0;
            }
            return new MagicHitConfigurationEvidence(
                    tablesWithColumn, nonzeroResourceRows, darkRows);
        }
    }

    private static MagicHitConfigurationEvidence blindResistanceConfigurationEvidence(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String[] tables = {
                "armor", "weapon",
                "鋆?_weapon_greater_?賢?", "鋆?_armor_greater_?賢?",
                "鋆?_weapon_bless_?賢?", "鋆?_armor_bless_?賢?",
                "鋆?_weapon_bless_??", "鋆?_armor_bless_??",
                "鋆?_weapon_enchant_??", "鋆?_armor_enchant_??",
                "鋆?_weapon_enchant_?賢?", "鋆?_armor_enchant_?賢?",
                "鋆?_?寞?_?∠?_?賢?", "鋆?_?寞?_憟?_?賢?",
                "鋆?_?寞?_?∠?_蝯?",
                "裝備_特殊_套裝_能力", "裝備_特殊_卡片_組合"
        };
        int tablesWithColumn = 0;
        int nonzeroResourceRows = 0;
        try (Connection connection = repositoryConnection(repository)) {
            for (String table : tables) {
                try (PreparedStatement column = connection.prepareStatement(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                                + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
                                + " AND COLUMN_NAME='regist_blind'")) {
                    column.setString(1, resourceDatabase);
                    column.setString(2, table);
                    try (ResultSet row = column.executeQuery()) {
                        if (!row.next() || row.getInt(1) == 0) continue;
                    }
                }
                tablesWithColumn++;
                String sql = "SELECT COUNT(*) FROM "
                        + LegacyEquipmentMagicLoader.qualified(resourceDatabase, table)
                        + " WHERE regist_blind<>0";
                try (PreparedStatement count = connection.prepareStatement(sql);
                     ResultSet row = count.executeQuery()) {
                    nonzeroResourceRows += row.next() ? row.getInt(1) : 0;
                }
            }
            try (PreparedStatement column = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                            + " WHERE TABLE_SCHEMA=? AND TABLE_NAME='道具_自訂穿脫能力'"
                            + " AND COLUMN_NAME='暗黑耐性'")) {
                column.setString(1, resourceDatabase);
                try (ResultSet row = column.executeQuery()) {
                    if (row.next() && row.getInt(1) > 0) {
                        tablesWithColumn++;
                        String vipSql = "SELECT COUNT(*) FROM "
                                + LegacyEquipmentMagicLoader.qualified(
                                        resourceDatabase, "道具_自訂穿脫能力")
                                + " WHERE `暗黑耐性`<>0";
                        try (PreparedStatement count = connection.prepareStatement(vipSql);
                             ResultSet vip = count.executeQuery()) {
                            nonzeroResourceRows += vip.next() ? vip.getInt(1) : 0;
                        }
                    }
                }
            }
            int darkRows;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM character_items_dark"
                            + " WHERE add_regist_blind<>0");
                 ResultSet row = count.executeQuery()) {
                darkRows = row.next() ? row.getInt(1) : 0;
            }
            return new MagicHitConfigurationEvidence(
                    tablesWithColumn, nonzeroResourceRows, darkRows);
        }
    }

    private static SpecialWeaponEvidence specialWeaponEvidence(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String weapon = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon");
        String sql = "SELECT COUNT(*) total,"
                + "SUM(type='claw' AND dmg_cri_chance=0 AND dmg_cri=0"
                + " AND dmg_max_chance=33) claw,"
                + "SUM(type='edoryu' AND dmg_cri_chance=10 AND dmg_cri=1.2"
                + " AND dmg_max_chance=0) edoryu,"
                + "SUM(type='kiringku' AND dmg_cri_chance=0 AND dmg_cri=0"
                + " AND dmg_max_chance=0) kiringku,"
                + "SUM(type='chainsword' AND dmg_cri_chance=0 AND dmg_cri=0"
                + " AND dmg_max_chance=0) chainsword FROM " + weapon
                + " WHERE type IN ('claw','edoryu','kiringku','chainsword')";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) return new SpecialWeaponEvidence(0, 0, 0, 0, 0);
            return new SpecialWeaponEvidence(
                    row.getInt("total"), row.getInt("claw"),
                    row.getInt("edoryu"), row.getInt("kiringku"),
                    row.getInt("chainsword"));
        }
    }

    private static KiringkuEffectEvidence kiringkuEffectEvidence(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String weapon = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon");
        String sql = "SELECT COUNT(*) total,"
                + "SUM(invgfx=3018) special_3018,"
                + "SUM(invgfx<>3018) default_other FROM " + weapon
                + " WHERE type='kiringku'";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) return new KiringkuEffectEvidence(0, 0, 0);
            return new KiringkuEffectEvidence(
                    row.getInt("total"), row.getInt("special_3018"),
                    row.getInt("default_other"));
        }
    }

    private static ChainswordEvidence chainswordEvidence(
            AccountRepository repository) throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String weapon = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon");
        String sql = "SELECT COUNT(*) total,"
                + "SUM(`range`=2) range_two,"
                + "SUM(item_id=272 AND dmg_small=16 AND dmg_large=12"
                + " AND `range`=2 AND hitmodifier=2 AND dmgmodifier=5"
                + " AND invgfx=3033 AND material='silver' AND bless=0) item_272_exact"
                + " FROM " + weapon + " WHERE type='chainsword'";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) return new ChainswordEvidence(0, 0, 0);
            return new ChainswordEvidence(
                    row.getInt("total"), row.getInt("range_two"),
                    row.getInt("item_272_exact"));
        }
    }

    private static long firstAttributeWeaponCharacter(AccountRepository repository)
            throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String weapon = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon");
        String sql = "SELECT c.objid FROM characters c "
                + "JOIN character_items i ON i.char_id=c.objid AND i.is_equipped=1 "
                + "JOIN " + weapon + " w ON w.item_id=i.item_id "
                + "WHERE c.Banned=0 AND i.attr_enchant_kind>0 "
                + "AND i.attr_enchant_level>0 ORDER BY c.objid LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getLong(1) : 0;
        }
    }

    private static long firstRangedCharacterWithProjectile(AccountRepository repository)
            throws Exception {
        String resourceDatabase = stringField(repository, "resourceDatabase");
        String weapon = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "weapon");
        String etcitem = LegacyEquipmentMagicLoader.qualified(resourceDatabase, "etcitem");
        String arrow = LegacyEquipmentMagicLoader.qualified(
                resourceDatabase, "etcitem_arrow");
        String sql = "SELECT c.objid FROM characters c "
                + "JOIN character_items w ON w.char_id=c.objid AND w.is_equipped=1 "
                + "JOIN " + weapon + " wt ON wt.item_id=w.item_id "
                + "JOIN character_items p ON p.char_id=c.objid "
                + "AND p.is_equipped=0 AND p.count>0 "
                + "JOIN " + etcitem + " e ON e.item_id=p.item_id "
                + "JOIN " + arrow + " a ON a.item_id=p.item_id "
                + "WHERE c.Banned=0 AND ((wt.type IN ('bow','singlebow') "
                + "AND e.use_type='arrow') OR (wt.type='gauntlet' "
                + "AND e.use_type='sting')) ORDER BY c.objid,p.id LIMIT 1";
        try (Connection connection = repositoryConnection(repository);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getLong(1) : 0;
        }
    }

    private static Connection repositoryConnection(AccountRepository repository)
            throws Exception {
        Connection connection = DriverManager.getConnection(
                stringField(repository, "accountUrl"),
                stringField(repository, "accountLogin"),
                stringField(repository, "accountPassword"));
        connection.setReadOnly(true);
        return connection;
    }

    private static String stringField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }

    private record SpecialWeaponEvidence(
            int total, int claw, int edoryu, int kiringku, int chainsword) {}

    private record KiringkuEffectEvidence(
            int total, int special3018, int defaultOther) {}

    private record ChainswordEvidence(int total, int rangeTwo, int item272Exact) {}

    private record MagicHitConfigurationEvidence(
            int tablesWithColumn, int nonzeroResourceRows, int darkRows) {
        int nonzeroRows() {
            return nonzeroResourceRows + darkRows;
        }
    }

    private record HolyWalkSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis) {}

    private record TornadoSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis) {}

    private record BlizzardSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis) {}

    private record EarthquakeSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis) {}

    private record SilenceSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int bossRows, int itemRows, int probabilityRows) {}

    private record LightningStormSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
             int itemRows, int abilityRows, int bossRows, int probabilityRows) {}

    private record AbsoluteBarrierSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows, int bossRows, int probabilityRows,
            int cancellationImmuneRows) {}

    private record AdvanceSpiritSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows, int bossRows, int probabilityRows,
            int cancellationImmuneRows) {}

    private record ShapeChangeSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows, int menuRows) {}

    private record ImmuneToHarmSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows) {}

    private record MassTeleportSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows) {}

    private record FireStormSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows) {}

    private record DecayPotionSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows, int bossRows,
            int probabilityOverrideRows) {}

    private record CounterDetectionSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int itemRows, int abilityRows, int bossRows,
            int probabilityOverrideRows) {}

    private record GreaterHasteSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis) {}

    private record BerserkersSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int abilityRows, int exactAbilityRows) {}

    private record DiseaseSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int abilityRows, int exactAbilityRows,
            int bossRows, int itemRows, int probabilityOverrideRows) {}

    private record FullHealSourceEvidence(
            String database, boolean effectSkillIdColumn,
            int effectSkillId, int reuseDelayMillis,
            int abilityRows, int bossRows, int itemRows,
            int probabilityOverrideRows) {}
}
