package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #356（T125）：customer-gaps G09–G12、G16 收尾。
 *
 * <p>G09 是純函式，直接測行為（含本 Issue 對照 1-伺服器 types/Point.java:157-177 補上的第 19 格反向邊界）。
 * G10–G12 的接受後生命週期需要真 GameHub／連線（CI 沒有 config/server.properties），這裡測不需連線就能跑的
 * 退化路徑，並以 src/main 原始碼鎖住「先等 HP 被接受、再發狀態／特效」的接線與守門條件；
 * G16 測 metadata 前提與政策本體。CI 的工作目錄是模組根目錄，所以原始碼路徑以 src/main/java 開頭。</p>
 */
final class SkillTimingRangeGapsTest {
    private static final Path SRC = Path.of("src/main/java/local/lito381/server");

    /** 380_db.skills（server1/skills-targeting-181.tsv）的 29／44／76／50／80／87／1／4／26／42／167 列。 */
    private static final Map<Integer, NativeSkillTargetMetadata.Row> ROWS = Map.ofEntries(
            Map.entry(1, new NativeSkillTargetMetadata.Row("buff", 3, -1, 0, false, 16, 4, 1)),
            Map.entry(4, new NativeSkillTargetMetadata.Row("attack", 3, 10, 0, false, 64, 16, 8)),
            Map.entry(26, new NativeSkillTargetMetadata.Row("buff", 4, -1, 0, false, 2, 8, 2)),
            Map.entry(29, new NativeSkillTargetMetadata.Row("buff", 3, -1, 0, false, 1, 8, 16)),
            Map.entry(42, new NativeSkillTargetMetadata.Row("buff", 4, -1, 0, false, 2, 2, 2)),
            Map.entry(44, new NativeSkillTargetMetadata.Row("buff", 3, -1, 0, false, 1, 8, 8)),
            Map.entry(50, new NativeSkillTargetMetadata.Row("attack", 3, 6, 0, false, 64, 4, 2)),
            Map.entry(76, new NativeSkillTargetMetadata.Row("buff", 3, -1, 3, false, 1, 1, 8)),
            Map.entry(80, new NativeSkillTargetMetadata.Row("none", 3, 0, 10, false, 64, 4, 128)),
            Map.entry(87, new NativeSkillTargetMetadata.Row("attack", 3, 1, 0, false, 64, 0, 64)),
            Map.entry(167, new NativeSkillTargetMetadata.Row("buff", 3, -1, 0, false, 1, 8, 64)));

    private Map<Integer, NativeSkillTargetMetadata.Row> prior;

    @BeforeEach void installRows() {
        prior = NativeSkillTargetMetadata.replaceForTesting(ROWS);
    }

    @AfterEach void restoreRows() {
        NativeSkillTargetMetadata.replaceForTesting(prior == null ? Map.of() : prior);
    }

    private static String source(String file) {
        try {
            return Files.readString(SRC.resolve(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void contains(String text, String snippet) {
        assertTrue(text.contains(snippet), () -> "missing: " + snippet);
    }

    private static int count(String text, String snippet) {
        int n = 0;
        for (int at = text.indexOf(snippet); at >= 0; at = text.indexOf(snippet, at + 1)) n++;
        return n;
    }

    // ---- G09：29／44／76 ranged=−1 視野射程 ----

    /** Source ranged=−1 的三個技能，目錄射程在 Source 列載入後投影成螢幕外框 19；沒載入時保留目錄原值。 */
    @Test void g09CatalogRangeProjectsSourceScreen() {
        assertEquals(19, BattleSpellCatalog.entangleSpell(29).maximumRange());
        assertEquals(19, BattleSpellCatalog.cancellationSpell(44).maximumRange());
        assertEquals(19, BattleSpellCatalog.massSlowSpell(76).maximumRange());
        for (int skill : new int[] {29, 44, 76}) assertEquals(19, NativeSourceVisibilityRange.value(skill, 18));
        assertEquals(6, NativeSourceVisibilityRange.value(50, 99), "ranged>=0 follows the Source row");
        NativeSkillTargetMetadata.replaceForTesting(Map.of());
        assertEquals(18, NativeSourceVisibilityRange.value(29, 18), "no Source rows: original catalog value");
        assertEquals(25, NativeSourceVisibilityRange.signedDistance29(25, 0), "no Source rows: plain tile distance");
    }

    /** Source Point.isInScreen = LiTo 正向外框 ∪ 反向外框；兩者只差 (19,0)／(0,19)／(18,-1)／(-1,18) 四格。 */
    @Test void g09SourceScreenIsSymmetricUnionOfNativeBounds() {
        Set<String> extra = new TreeSet<>();
        for (int dx = -25; dx <= 25; dx++) {
            for (int dy = -25; dy <= 25; dy++) {
                boolean source = NativeSourceVisibilityRange.inSourceScreen(dx, dy);
                assertEquals(source, NativeSourceVisibilityRange.inSourceScreen(-dx, -dy), dx + "," + dy);
                boolean forward = BattleSpellCatalog.isInNativeScreen(0, 0, dx, dy);
                boolean reverse = BattleSpellCatalog.isInNativeScreen(0, 0, -dx, -dy);
                assertEquals(forward || reverse, source, dx + "," + dy);
                if (source && !forward) extra.add(dx + "," + dy);
            }
        }
        assertEquals(new TreeSet<>(List.of("-1,18", "0,19", "18,-1", "19,0")), extra);
    }

    /** 呼叫端的有號差值（施法者−目標）與座標版本在第 19 格都採 Source 判定；超出螢幕回 MAX。 */
    @Test void g09BoundaryDistances() {
        for (int skill : new int[] {29, 44, 76}) {
            assertEquals(18, NativeSourceVisibilityRange.signedDistance(skill, 18, 0));
            assertEquals(19, NativeSourceVisibilityRange.signedDistance(skill, 19, 0));
            assertEquals(19, NativeSourceVisibilityRange.signedDistance(skill, -19, 0));
            assertEquals(19, NativeSourceVisibilityRange.signedDistance(skill, 0, 19));
            assertEquals(19, NativeSourceVisibilityRange.signedDistance(skill, 0, -19));
            assertEquals(10, NativeSourceVisibilityRange.signedDistance(skill, 10, -9));
            assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.signedDistance(skill, 10, 9));
            assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.signedDistance(skill, -10, -9));
            assertEquals(18, NativeSourceVisibilityRange.signedDistance(skill, 18, -1));
            assertEquals(18, NativeSourceVisibilityRange.signedDistance(skill, -18, 1));
            assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.signedDistance(skill, 20, 0));
        }
        assertEquals(19, NativeSourceVisibilityRange.distance29(100, 100, 119, 100));
        assertEquals(19, NativeSourceVisibilityRange.distance44(100, 100, 81, 100));
        assertEquals(19, NativeSourceVisibilityRange.distance76(100, 100, 100, 119));
        assertEquals(Integer.MAX_VALUE, NativeSourceVisibilityRange.distance44(100, 100, 110, 109));
        assertEquals(10, NativeSourceVisibilityRange.signedDistance(50, 10, 9), "ranged>=0 skills never use the screen");
    }

    /** 三個技能的 ClientSession 射程判定都經過 NativeSourceVisibilityRange（沒有繞過的舊 helper）。 */
    @Test void g09CallSitesUseSourceVisibility() {
        String session = source("ClientSession.java");
        assertTrue(count(session, "NativeSourceVisibilityRange.signedDistance29(") >= 1);
        assertTrue(count(session, "NativeSourceVisibilityRange.signedDistance44(") >= 5);
        assertTrue(count(session, "NativeSourceVisibilityRange.signedDistance76(") >= 6);
        String range = source("NativeSourceVisibilityRange.java");
        assertFalse(range.contains("isInNativeScreen("), "skill range uses the Source screen, not the broadcast bound");
        String slowNpc = source("NativeSourceSlowFailureMetadata.java");
        contains(slowNpc, "row.ranged()==-1&&!NativeSourceVisibilityRange.inSourceScreen(dx,dy)?Integer.MAX_VALUE");
        assertFalse(slowNpc.contains("isInNativeScreen("), "76 NPC selection shares the same Source screen bound");
    }

    // ---- G10：87 衝擊之暈先扣 HP 再暈 ----

    @Test void g10ShockStunWaitsForAcceptedHp() {
        assertFalse(NativeAcceptedShockMutation.plan(null, BattleSpellCatalog.shockStunSpell(87), null, null, null),
                "without an admitted event scope the original path runs and no plan is held");
        String shock = source("NativeAcceptedShockMutation.java");
        contains(shock, "if(target==null||!frame.accepted||frame.fatal||frame.after<=0||frame.pc!=target)return;");
        contains(shock, "(int)CombatAccess.field(target,\"playerHp\")<=0)return;p.done=true;");
        contains(shock, "!=5)return NativeAreaMaterialAction.nativeAcceptedShockOriginalCaptureOutput(");
        contains(shock, "p.outputs.add(new Output(receiver,binding,packet.clone(),note));return true;");
        contains(shock, "if(p!=null){apply(p,frame);emit(p);}");
        String session = source("ClientSession.java");
        assertEquals(2, count(session, "NativeAcceptedShockMutation.plan(this,"), "BATTLE + WORLD");
        String completion = source("NativeAcceptedHpCompletion.java");
        int tail = completion.indexOf("NativePlayerCommit.acceptedTail(this.frame);");
        int accepted = completion.indexOf("NativeAcceptedFreezeMutation.completed(this.frame);");
        assertTrue(tail >= 0 && accepted > tail, "HP tail is published before accepted status/effect");
        contains(source("NativeAcceptedFreezeMutation.java"), "else NativeAcceptedShockMutation.completed(frame);}");
        contains(source("NativeAreaMaterialAction.java"), "NativeAcceptedShockMutation.captureOutput(");
    }

    // ---- G11：50 冰矛圍籬接受後才消魔／冰凍，期限重載讀擷取值 ----

    @Test void g11IceLanceAcceptedFreeze() {
        var spell = BattleSpellCatalog.freezeAttackSpell(BattleSpellCatalog.ICE_LANCE);
        assertNull(NativeAcceptedFreezeMutation.current());
        assertEquals(spell.durationSeconds(), NativeAcceptedFreezeMutation.duration(spell),
                "outside a replay the catalog duration is used");
        assertTrue(NativeSourceFreezeTarget.applies(spell));
        String freeze = source("NativeAcceptedFreezeMutation.java");
        contains(freeze, "return b!=null&&b.replay&&b.spell==spell?b.seconds:spell.durationSeconds();");
        contains(freeze, "b.erase=lease;return false;");
        contains(freeze, "b.freeze=true;b.seconds=seconds;");
        contains(freeze, "if(frame!=null&&frame.accepted&&!frame.fatal&&frame.after>0&&frame.event.damage()>0)");
        contains(freeze, "boolean accepted=queue.nativeFreezeAcceptedOriginalOffer(event);if(!accepted)abandon(event);");
        contains(freeze, "b.cancelled=true;");
        String session = source("ClientSession.java");
        contains(session, "NativeAcceptedFreezeMutation.battle(this, byArray, freezeAttackSpell);");
        contains(session, "NativeAcceptedFreezeMutation.world(this, byArray, freezeAttackSpell);");
        assertTrue(count(session, "NativeAcceptedFreezeMutation.duration(") >= 2);
        String commit = source("NativePlayerCommit.java");
        contains(commit, "NativeAcceptedFreezeMutation.offer(");
        contains(commit, "NativeAcceptedFreezeMutation.clearEvents(");
    }

    // ---- G12：80 冰雪颶風動畫先於 HP、過期不扣 MP ----

    @Test void g12FreezingBlizzardAreaOrdering() {
        List<ClientSession> values = new ArrayList<>();
        assertSame(values, NativeAcceptedAreaFreezeMutation.players(values), "no area group: membership untouched");
        assertNull(NativeAcceptedAreaFreezeMutation.selected(null));
        var spell = BattleSpellCatalog.freezeAttackSpell(BattleSpellCatalog.FREEZING_BLIZZARD);
        assertFalse(NativeSourceFreezeTarget.applies(spell), "80 uses the area group, not the single-target batch");
        assertEquals(spell.durationSeconds(), NativeAcceptedFreezeMutation.duration(spell));
        String area = source("NativeAcceptedAreaFreezeMutation.java");
        contains(area, "if(spell.skillId()!=80){original(pc,packet,spell,world);return;}");
        contains(area, "NativePlayerLifetime.of(hub).mode!=NativePlayerLifetime.Mode.OPEN)return;");
        contains(area, "if(g.scene()&&NativePlayerLifetime.of(hub).mode==NativePlayerLifetime.Mode.OPEN)NativeAreaMaterialAction.owned(");
        contains(area, "if(b.eligible){g.pending.add(new Player(b,amount,caster,false));return;}");
        contains(area, "if(b.eligible){g.pending.add(new Player(b,amount,caster,flag));return;}");
        int emit = area.indexOf("emit(g);for(var p:g.pending)publish(p);");
        assertTrue(emit >= 0, "cast animation/output is emitted before deferred HP publication");
        contains(area, "if(!b.valid(false)||b.lifetime.mode==NativePlayerLifetime.Mode.STOPPED){b.cancelled=true;return;}");
        String session = source("ClientSession.java");
        contains(session, "NativeAcceptedAreaFreezeMutation.battle(this, byArray, freezeAttackSpell);");
        contains(session, "NativeAcceptedAreaFreezeMutation.world(this, byArray, freezeAttackSpell);");
    }

    // ---- G16：7 技能選取玩家的明確 Ctrl 政策（與 G02 的伺服器側） ----

    @Test void g16SelectedPlayerIntentPolicy() {
        for (int skill : new int[] {1, 4, 26, 42, 50, 76, 167}) {
            assertTrue((NativeSkillTargetMetadata.row(skill).targetTo() & 13) != 0,
                    "skill" + skill + " targets PCs, so the explicit-intent gate applies");
        }
        NativeSkillTargetMetadata.replaceForTesting(Map.of());
        assertTrue(NativeSelectedPlayerIntent.allowed(null, 26, 1L), "no Source rows: original admission");
        assertTrue(NativeSelectedPlayerIntent.object(null, null));
        assertTrue(NativeSelectedPlayerIntent.attack(null, null));
        String intent = source("NativeSelectedPlayerIntent.java");
        contains(intent, "if(NativeSkillTargetMetadata.size()==0||!CombatAccess.field(pc,\"phase\").toString().equals(\"BATTLE\"))return true;");
        contains(intent, "if(row==null||((row.targetTo()&13)==0&&skill!=68))return true;");
        contains(intent, "if(target==pc||ForcedCombat.nativeSelectedObjectIntent(pc,id))return true;");
        contains(intent, "return !hub.canAttack(room,pc,target);");
        String session = source("ClientSession.java");
        assertEquals(33, count(session, "NativeSelectedPlayerIntent.object("));
        assertEquals(48, count(session, "NativeSelectedPlayerIntent.attack("));
    }
}
