package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Retained pet/summon-to-player authority plus selected LiTo wire boundary. */
final class NativeClassicWorldCompanionPlayerSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECT_TARGET = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_SelectTarget.java");
    private static final Path PC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path PET = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PetInstance.java");
    private static final Path SUMMON = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1SummonInstance.java");
    private static final Path ATTACK_NPC = RETAINED.resolve(
            "src/com/lineage/server/model/L1AttackNpc.java");
    private static final Path ATTACK_MODE = RETAINED.resolve(
            "src/com/lineage/server/model/L1AttackMode.java");
    private static final Path NPC_PHYSICAL_FLOW = RETAINED.resolve(
            "src/com/lineage/server/model/combat/NpcPhysicalAttackFlow.java");
    private static final Path USE_ARROW = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_UseArrowSkill.java");
    private static final Path PRIMARY_DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path SELECTED_CLIENT =
            Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path C_PRODUCERS = Path.of("..", "LiTo381-開發",
            "analysis", "NATIVE-C-PRODUCERS-ALL.json")
            .toAbsolutePath().normalize();
    private static final Path S_DISPATCH = Path.of("..", "LiTo381-開發",
            "analysis", "NATIVE-S-DISPATCH-ALL.json")
            .toAbsolutePath().normalize();
    private static final Path HYPERTEXT_EVIDENCE = Path.of("..", "LiTo381-開發",
            "analysis", "NATIVE-HYPERTEXT-ACTION-EVIDENCE.json")
            .toAbsolutePath().normalize();

    @Test void retainedFilesRemainTheReviewedCompanionPlayerAuthority()
            throws Exception {
        assertEquals("D6AC219EE87D0A89762DB9EFB799D23E22D65374BFBF82B91D245C91510EC141",
                sha256(SELECT_TARGET));
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PC));
        assertEquals("C1AFF14740C290FB91E5C0F5840818372E61F88D2FE8129ADC5712452A899CA3",
                sha256(PET));
        assertEquals("BC0DAB6D576CF7AC91830E49A543792814AA69893364EF5CD2952856966AEC9D",
                sha256(SUMMON));
        assertEquals("7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8",
                sha256(ATTACK_NPC));
        assertEquals("F29C4D0BCC85CA3E6399497FA45B97B14047D4161E32A02525940E14290AE2C2",
                sha256(ATTACK_MODE));
        assertEquals("FFF7ABCE3436AE282F08BFB3D3D72E8ADCD70F033A631AB4AEB8049B686A507B",
                sha256(NPC_PHYSICAL_FLOW));
        assertEquals("870D1757E281E346A635C52E6F3B4CF8B5DC22A06959FF5C88A1029BC6A0A769",
                sha256(USE_ARROW));
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY_DATABASE));
    }

    @Test void retainedSelectionAuthorizesPlayerAndRejectsSafetyOrForgery()
            throws Exception {
        String source = normalized(SELECT_TARGET);
        assertTrue(source.contains("final int petId = this.readD();"));
        assertTrue(source.contains("final int targetId = this.readD();"));
        assertTrue(source.contains("pet.getMaster(), pet.getMapId(), pet.get_showId()"));
        assertTrue(source.contains("if (target instanceof L1PcInstance)"));
        assertTrue(source.contains("tgpc.checkNonPvP(tgpc, pet)"));
        assertTrue(source.contains("if (target.isSafetyZone())"));
        assertTrue(source.contains("pet.setMasterSelectTarget(target)"));
        assertTrue(source.contains("petMaster != requester"));
        assertTrue(source.contains("target.getMapId() != requester.getMapId()"));
        assertTrue(source.contains("!requester.knownsObject(target)"));
    }

    @Test void retainedMasterHitAndDeathBothResolveThroughCompanionOwner()
            throws Exception {
        String source = normalized(PC);
        assertTrue(source.contains("public void setPetTarget(final L1Character target)"));
        assertTrue(source.contains("pets.setMasterTarget(target)"));
        assertTrue(source.contains("summon.setMasterTarget(target)"));
        assertTrue(source.contains("lastAttacker instanceof L1PetInstance pet"));
        assertTrue(source.contains("attacker = (L1PcInstance) pet.getMaster()"));
        assertTrue(source.contains("lastAttacker instanceof L1SummonInstance summon"));
        assertTrue(source.contains("attacker = (L1PcInstance) summon.getMaster()"));

        String pet = normalized(PET);
        String summon = normalized(SUMMON);
        assertTrue(pet.contains("void setMasterTarget"));
        assertTrue(pet.contains("void setMasterSelectTarget"));
        assertTrue(summon.contains("void setMasterTarget"));
    }

    @Test void retainedNpcAttackContainsPlayerHitDamageAndCommitBranches()
            throws Exception {
        String source = normalized(ATTACK_NPC);
        assertTrue(source.contains("calcPcHit"));
        assertTrue(source.contains("calcPcDamage"));
        assertTrue(source.contains("ConfigPcDamage"));
        assertTrue(source.contains("receiveDamage"));

        String flow = normalized(NPC_PHYSICAL_FLOW);
        int hit = flow.indexOf("if (attack.calcHit())");
        int counter = flow.indexOf("targetPc.hasSkillEffect(COUNTER_BARRIER)");
        int ordinaryDamage = flow.indexOf("attack.calcDamage()", counter);
        int reflected = flow.indexOf("attack.commitCounterBarrier()", ordinaryDamage);
        assertTrue(hit >= 0 && counter > hit && ordinaryDamage > counter
                && reflected > ordinaryDamage);
        assertTrue(flow.contains("isProbability && attack.isShortDistance()"));

        String mode = normalized(ATTACK_MODE);
        assertTrue(mode.contains("COUNTER_BARRIER_TRIGGER_GFX = 10710"));
        assertTrue(mode.contains("weapon.getItem().getDmgLarge()"));
        assertTrue(mode.contains("weapon.getEnchantLevel()"));
        assertTrue(mode.contains("weapon.getItem().getDmgModifier()) << 1"));
    }

    @Test void retainedRangedCompanionUsesNpcRangeBowPacketAndShortDistanceGate()
            throws Exception {
        String attack = normalized(ATTACK_NPC);
        assertTrue(attack.contains("getLocation().getTileLineDistance"));
        assertTrue(attack.contains("int bowActId = _npc.getBowActId()"));
        assertTrue(attack.contains("isLongRange && (bowActId > 0)"));
        assertTrue(attack.contains("new S_UseArrowSkill"));
        assertTrue(attack.contains("return false;"));
        assertTrue(attack.contains("ConfigSkill.STRIKER_DMG2"));

        String packet = normalized(USE_ARROW);
        assertTrue(packet.contains("this.writeD(0x00000152)"));
        assertTrue(packet.contains("this.writeH(spellgfx)"));
        assertTrue(packet.contains("this.writeC(0x7f)"));

        String database = normalized(PRIMARY_DATABASE);
        assertTrue(database.contains("INSERT INTO `npc` VALUES (81215, '妖魔巡守'"));
        assertTrue(database.contains("INSERT INTO `npc` VALUES (81227, '火焰弓箭手'"));
        assertTrue(database.lines().filter(line -> line.startsWith(
                "INSERT INTO `npc` VALUES (81215,")).findFirst().orElseThrow()
                .contains(", 10, 1, 640, 1520"));
        assertTrue(database.lines().filter(line -> line.startsWith(
                "INSERT INTO `npc` VALUES (81227,")).findFirst().orElseThrow()
                .contains(", 10, 0, 960, 2000"));
    }

    @Test void retainedCompanionPoisonUsesNpcTemplateRollAndExactSourceRows()
            throws Exception {
        String attack = normalized(ATTACK_NPC);
        assertTrue(attack.contains("addNpcPoisonAttack(_targetPc)"));
        assertTrue(attack.contains("addNpcPoisonAttack(_targetNpc)"));
        assertTrue(attack.contains("switch (_npc.getNpcTemplate().get_poisonatk())"));
        assertTrue(attack.contains("15 >= ThreadLocalRandom.current().nextInt(100) + 1"));
        assertTrue(attack.contains("L1DamagePoison.doInfection(_npc, target, 3000, 5)"));
        assertTrue(attack.contains("L1SilencePoison.doInfection(target)"));
        assertTrue(attack.contains("L1ParalysisPoison.doInfection(target, 20000, 45000)"));
        assertTrue(attack.contains("get_paralysisatk() != 0) { // 具麻痺攻擊\n\t\t}"));

        String database = normalized(PRIMARY_DATABASE);
        String damagePoison = database.lines().filter(line -> line.startsWith(
                "INSERT INTO `npc` VALUES (81217,")).findFirst().orElseThrow();
        String silencePoison = database.lines().filter(line -> line.startsWith(
                "INSERT INTO `npc` VALUES (81236,")).findFirst().orElseThrow();
        String paralysisPoison = database.lines().filter(line -> line.startsWith(
                "INSERT INTO `npc` VALUES (45157,")).findFirst().orElseThrow();
        assertTrue(damagePoison.contains(", 680, 680, 680, 0, 1, 0, 1,"));
        assertTrue(silencePoison.contains(", 1120, 1120, 1120, 2, 0, 0, 1,"));
        assertTrue(paralysisPoison.contains(
                ", 1280, 720, 720, 720, 1, 4, 0, 1,"));

        String current = normalized(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(current.contains("applyClassicWorldNpcPoison("));
        assertTrue(current.contains("controlled.poisonAttack()"));
        assertTrue(current.contains("controlled.paralysisAttack()"));
        assertTrue(current.contains("ClassicWorldMonsterPoisonAttack.resolve("));
        assertTrue(current.contains("applyClassicWorldCompanionPoisonToMonster("));
        assertTrue(current.contains("hub.poisonClassicWorldMonsterFromNpc("));
        assertTrue(current.contains("reason=companion-poison-"));
        assertTrue(current.contains("retained-paralysis-atk-handler=absent"));

        String population = normalized(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"));
        assertTrue(population.contains("synchronized NpcPoison npcPoison("));
        assertTrue(population.contains("case DAMAGE -> actor.damagePoisonStatus.activatePoison("));
        assertTrue(population.contains("case SILENCE -> actor.damagePoisonStatus"));
        assertTrue(population.contains("case PARALYSIS -> actor.damagePoisonStatus.activateParalysis("));
        assertTrue(population.contains("poisonContributorKind"));
        assertTrue(population.contains("poisonPlayerObjectId"));
    }

    @Test void retainedNpcToNpcPhysicalFormulaAndPoisonOrderingStayLocked()
            throws Exception {
        String attack = normalized(ATTACK_NPC);
        String mode = normalized(ATTACK_MODE);
        assertTrue(attack.contains("_hitRate += _npc.getLevel() + 3"));
        assertTrue(attack.contains("((L1PetInstance) _npc).getHitByWeapon()"));
        assertTrue(attack.contains(
                "nextInt(_npc.getNpcTemplate().get_level()) + (_npc.getStr() / 2) + 1"));
        assertTrue(attack.contains("dmg += (lvl / 14)"));
        assertTrue(attack.contains("nextInt(lvl) + _npc.getStr() / 2 + dmgStr"));
        assertTrue(attack.contains("dmg *= 1.80"));
        assertTrue(attack.contains("dmg += _npc.getDmgup()"));
        assertTrue(attack.contains("dmg *= 1.20"));
        assertTrue(attack.contains("_npc.getWeaponBreakStack()"));
        assertTrue(mode.contains("final int acDefMax = ac / 7"));

        int defense = attack.indexOf("dmg -= calcNpcDamageReduction()");
        int poison = attack.indexOf("addNpcPoisonAttack(_targetNpc)", defense);
        int immune = attack.indexOf("_targetNpc.hasSkillEffect(IMMUNE_TO_HARM)", poison);
        int zeroDamage = attack.indexOf("if (dmg <= 0)", immune);
        assertTrue(defense >= 0 && poison > defense && immune > poison
                && zeroDamage > immune);

        String current = normalized(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldCompanionNpcPhysicalAttack.java"));
        assertTrue(current.contains(
                ClassicWorldCompanionNpcPhysicalAttack.L1_ATTACK_NPC_SOURCE_SHA256));
        assertTrue(current.contains(
                ClassicWorldCompanionNpcPhysicalAttack.L1_ATTACK_MODE_SOURCE_SHA256));
        assertTrue(current.contains("level + 3"));
        assertTrue(current.contains("runtimeLevel / 14"));
        assertTrue(current.contains("BattleWeaponBreak.PERCENT_PER_STACK"));

        String world = normalized(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int nativeDamage = world.indexOf(
                "ClassicWorldCompanionNpcPhysicalAttack.damage(");
        int nativePoison = world.indexOf(
                "applyClassicWorldCompanionPoisonToMonster(", nativeDamage);
        int finalMiss = world.indexOf("if (!hit) return;", nativePoison);
        assertTrue(nativeDamage >= 0 && nativePoison > nativeDamage
                && finalMiss > nativePoison);
    }

    @Test void selectedLiToReceiversAndProducerRemainExact() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals("C0121DF9BF5BC02656691CD16B62BE0321E4FCCF5BB95B54059071D2FB8ECE47",
                sha256(C_PRODUCERS));
        assertEquals("67AD2A6593D31F821DEC4F1EF34916D2186B3841F7B1AD1578A76DADD229960E",
                sha256(S_DISPATCH));
        assertEquals("884C4D99C045F2F3909ED4538CD9441E2AC01E9718659D44281B0C5AEBD88BA5",
                sha256(HYPERTEXT_EVIDENCE));
        String c = normalized(C_PRODUCERS);
        String s = normalized(S_DISPATCH);
        String evidence = normalized(HYPERTEXT_EVIDENCE);
        assertTrue(c.contains("\"opcode\": \"0x4F\""));
        assertTrue(c.contains("\"name\": \"C_SLAVE_CONTROL\""));
        assertTrue(s.contains("\"opcode\": \"0x57\""));
        assertTrue(s.contains("\"name\": \"S_SLAVE_CONTROL\""));
        assertTrue(s.contains("\"opcode\": \"0x23\""));
        assertTrue(s.contains("\"name\": \"S_ATTACK\""));
        assertTrue(evidence.contains(
                "C_SLAVE_CONTROL 0x4F = c d(controller_object_id) c(mode) d(target_object_id)"));
    }

    @Test void currentWorldImplementationKeepsSelectionAiAndLiabilityTogether()
            throws Exception {
        String source = normalized(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(source.contains("classicWorldCompanionPlayerTarget("));
        assertTrue(source.contains("target-kind=\"\n"
                + "                    + (playerTarget == null ? \"monster\" : \"player\")"));
        assertTrue(source.contains("controlled.selectMasterTarget(target.playerObjectId())"));
        assertTrue(source.contains("takeClassicWorldCompanionDamage("));
        assertTrue(source.contains("applyClassicWorldPlayerLiability(attacker, fatalContext)"));
        assertTrue(source.contains("companion.objectId() == attackerObjectId"));
        assertTrue(source.contains("target-kind=player hit="));
        int playerBranch = source.indexOf("if (playerTarget != null)");
        int decision = source.indexOf("playerTarget.counterBarrierDecision(", playerBranch);
        int attackPacket = source.indexOf(
                "byte[] attack = rangedAttack", decision);
        assertTrue(playerBranch >= 0 && decision > playerBranch
                && attackPacket > decision);
        String branch = source.substring(playerBranch, attackPacket);
        assertTrue(branch.contains("controlled.magicResistance()"));
        assertTrue(branch.contains("controlled.boss()"));
        assertTrue(branch.contains("publishNpcCounterBarrierTrigger("));
        assertTrue(branch.contains("applyClassicWorldMonsterCompanionDamage("));
        assertTrue(branch.contains("return;"));
        assertTrue(source.contains("controlled.attackRange()"));
        assertTrue(source.contains("controlled.projectileAtDistance(range)"));
        assertTrue(source.contains("Protocol.rangedPhysicalAttack("));
        assertTrue(source.contains("BattleStrikerGale.damageAfterStatus("));
        assertTrue(source.contains("BattleRangedEvasion.hitsAfterEvasion("));
        assertTrue(source.contains("applyCompanionDefenseFeedbackDamage("));
        assertTrue(source.contains("owner.applyClassicWorldMonsterCompanionDamage("));
        assertEquals(0x4F, Protocol.C_SLAVE_CONTROL);
        assertEquals(0x57, Protocol.S_SLAVE_CONTROL);
        assertEquals(0x23, Protocol.S_ATTACK);
    }

    private static String normalized(Path path) throws Exception {
        return Files.readString(path).replace("\r\n", "\n");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
