package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleNpcHasteTest.active;
import static local.lito381.server.ModernBattleNpcSlowTest.cast;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Explicit retained NPC prerequisite failures through the actual engine,
 * never a fake probability roll or a free unsupported-command response. */
class ModernSlowNativeRulesTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/L1AttackList.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
    }

    static ModernNpcCatalog.Catalog catalog(int id) {
        var c=ModernNpcMeleeTest.catalog();var original=c.npcs().get(ModernNpcMeleeTest.NPC);
        var rows=new HashMap<>(c.npcs());rows.put(id,new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(c.sourceId(),c.database(),id),
                "ward-fixture","$1498",original.implementation(),original.large(),original.facts(),original.hasMobskills()));
        return new ModernNpcCatalog.Catalog(c.sourceId(),c.database(),rows,c.growth());
    }
    static ModernNpcRoster.Actor spawn(ModernBattleEngineNpcTest.Fixture f,int id,int x) {
        var c=f.engine.spawnNpcsObservation(f.scope,List.of(new ModernNpcRoster.Spawn(
                f.config.catalog().npcs().get(id).rosterTemplate(),0,1,ModernBattleEngineNpcTest.cell(x,10),2)));
        return c.npcs().roster().actors().get(c.npcs().roster().actors().size()-1);
    }
    @Test void requiredEffectIdsMatchTheRetainedTableAndMagicChecksBeforeProbability()throws Exception {
        var root=Path.of("../1-伺服器/src/com/lineage/server/model");
        var ids=LocalResources.readString(root.resolve("skill/L1SkillId.java"));var names=new HashMap<String,Integer>();
        var constants=Pattern.compile("public static final int (\\w+) = (\\d+);").matcher(ids);
        while(constants.find())names.put(constants.group(1),Integer.parseInt(constants.group(2)));
        var retained=new HashMap<Integer,Integer>();
        var entries=Pattern.compile("SKNPC\\.put\\((\\d+), (\\w+)\\);").matcher(LocalResources.readString(root.resolve("L1AttackList.java")));
        while(entries.find())retained.put(Integer.parseInt(entries.group(1)),Objects.requireNonNull(names.get(entries.group(2))));
        assertEquals(16,retained.size());assertEquals(retained,ModernSlowNativeRules.requiredEffects());
        String magic=LocalResources.readString(root.resolve("L1MagicPc.java"));int guard=magic.indexOf("L1AttackList.SKNPC.get(npcId)");
        assertTrue(guard>0&&guard<magic.indexOf("probability = calcProbability(skillId);"));
        assertTrue(magic.substring(guard,magic.indexOf("// NPC指定外型",guard)).contains("if (!_pc.hasSkillEffect(tgskill))"));
        String use=LocalResources.readString(root.resolve("skill/L1SkillUse.java"));int type=use.indexOf("case TYPE_NORMAL ->");
        assertTrue(type>0&&use.indexOf("commitMaterialAndAcquireCastLocks()",type)<use.indexOf("runSkill();",type));
        assertTrue(use.contains("applySideEffectsAfterNormalCast();"));assertTrue(use.contains("useConsume();"));
    }
    @Test void everyQualifiedPrerequisiteNpcPaysOnceWithoutRngOrNpcMutation()throws Exception {
        for(var required:ModernSlowNativeRules.requiredEffects().entrySet())try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,catalog(required.getKey()))) {
            active(f);var n=spawn(f,required.getKey(),11);var before=f.engine.serviceObservation();f.invalidRandom=true;
            var r=cast(f,0,1,n);assertEquals(ModernSlowCast.Reason.NONE,r.reason());var after=r.observation();var a=after.actors().get(0);
            var detail=(ModernSlowReceipt)a.runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();
            assertEquals(ModernSlowReceipt.Outcome.NATIVE_IMMUNE,detail.outcome());assertEquals(0,detail.probability());assertEquals(0,detail.roll());
            assertEquals(new ModernSlowNativeRules.MissingEffect(required.getKey(),required.getValue()),detail.missingEffect());
            assertEquals(20,a.resources().flows().spent());assertEquals(1,a.materials().uses());assertEquals(5,a.materials().stocks().get(0).remaining());
            assertEquals(before.npcs(),after.npcs());assertEquals(0,f.draws.get());assertTrue(after.slowTargets().targets().isEmpty());
            assertEquals(ModernSlowCast.Reason.TOO_FAST,cast(f,0,2,n).reason());
            assertThrows(SecurityException.class,()->cast(f,0,2,n));assertEquals(1,f.engine.serviceObservation().actors().get(0).materials().uses());
            ModernBattleSlowTest.consistent(after);
        }
    }
    @Test void immuneLastAttemptDoesNotEraseEarlierOrdinaryNpcSlowAndMaterialsCannotGoBelowZero()throws Exception {
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,catalog(45912))) {
            active(f);var ordinary=spawn(f,ModernNpcMeleeTest.NPC,11);var ward=spawn(f,45912,12);
            assertEquals(ModernSlowCast.Reason.NONE,cast(f,0,1,ordinary).reason());assertEquals(1,f.draws.get());
            for(int i=1;i<6;i++){f.elapsed(i*2000L);assertEquals(ModernSlowCast.Reason.NONE,cast(f,0,i+1,ward).reason());}
            var paid=f.engine.serviceObservation();assertTrue(paid.npcs().roster().actors().get(0).speed().slowed());
            assertEquals(6,paid.actors().get(0).materials().uses());assertEquals(120,paid.actors().get(0).resources().flows().spent());assertEquals(1,f.draws.get());
            assertEquals(1,paid.slowTargets().targets().size());ModernBattleSlowTest.consistent(paid);
            f.elapsed(12000);assertEquals(ModernSlowCast.Reason.NO_MATERIAL,cast(f,0,7,ward).reason());
            assertEquals(6,f.engine.serviceObservation().actors().get(0).materials().uses());
        }
    }
    @Test void immunityProofCannotBeMovedToAnotherTemplateAndSurvivesTerminalRetirement()throws Exception {
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,catalog(45912))) {
            active(f);var ward=spawn(f,45912,11);var c=cast(f,0,1,ward).observation();
            var old=c.npcs().roster();var a=old.actors().get(0);var t=a.template();
            var other=new ModernNpcRoster.Template(t.catalogSourceId(),t.catalogDatabase(),45916,t.graphic(),t.maximumHp(),t.maximumMp(),t.walkMillis(),t.attackMillis());
            var changed=new ModernNpcRoster.Actor(a.id(),other,a.lane(),a.stage(),a.cell(),a.heading(),a.hp(),a.mp(),a.life(),a.action(),
                    a.bornRevision(),a.changedRevision(),a.corpseUntilMillis(),a.nextMoveMillis(),a.nextAttackMillis(),a.walk(),a.speed(),a.walkSpeed());
            var forged=new ModernBattleObservation.Npcs(new ModernNpcRoster.Snapshot(old.scope(),old.manifest(),old.revision(),old.elapsedMillis(),
                    old.spawned(),old.closed(),List.of(changed),old.isolatedForces()),c.npcs().exchanges());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),
                    c.room(),c.manifest(),c.features(),c.clock(),c.limits(),c.loadingOriginNanos(),c.actors(),c.modes(),forged,c.result(),c.hasteTargets(),c.slowTargets()));
            var w=c.world();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),
                    w.room(),w.manifest(),w.features(),w.clock(),w.actors(),w.modes(),forged,w.result(),w.hasteEffects(),w.speedEffects()));
            var terminal=f.engine.abortObservation();assertTrue(terminal.npcs().roster().closed());
            assertEquals(1,terminal.actors().get(0).materials().uses());ModernBattleSlowTest.consistent(terminal);
        }
    }
    @Test void missingPrerequisiteCannotDisguiseAFakeRollOrPlayerTargetAsNativeImmunity()throws Exception {
        assertThrows(IllegalArgumentException.class,()->new ModernSlowNativeRules.MissingEffect(45912,1014));
        assertThrows(IllegalArgumentException.class,()->new ModernSlowNativeRules.MissingEffect(45262,1013));
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,catalog(45912))) {
            active(f);var ward=spawn(f,45912,11);var c=cast(f,0,1,ward).observation();
            var d=(ModernSlowReceipt)c.actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();
            for(int[] bad:new int[][]{{1,0},{0,1},{1,1},{90,100}})
                assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(d.activeElapsedNanos(),bad[0],bad[1],0,d.target(),d.missingEffect()));
            assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(d.activeElapsedNanos(),0,0,0,d.target()));
            var a=c.actors().get(0);var p=new ModernSlowReceipt.Player(new ModernTargetHistory.Participant(1,a.pose().member().identity()),1,0,a.pose().position(),a.buffs(),a.buffs());
            assertThrows(IllegalArgumentException.class,()->new ModernSlowReceipt(d.activeElapsedNanos(),0,0,0,p,d.missingEffect()));
        }
    }
}
