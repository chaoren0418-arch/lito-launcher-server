// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import local.lito381.server.AccountRepository;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.BattleAnimationCatalog;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.ClientSession;
import local.lito381.server.GameHub;
import local.lito381.server.LiToGmCommands;
import local.lito381.server.Protocol;

final class LiToGmNpcs {
    private static final Map<BattleTimeAttackEncounter, BattleRules.RuleSet> GM = Collections.synchronizedMap(new WeakHashMap());

    LiToGmNpcs() {
    }

    static boolean active(BattleTimeAttackEncounter battleTimeAttackEncounter) {
        return GM.containsKey(battleTimeAttackEncounter);
    }

    static void clear(BattleTimeAttackEncounter battleTimeAttackEncounter) {
        GM.remove(battleTimeAttackEncounter);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static BattleRules.Mode combatMode(BattleRules.RuleSet ruleSet) {
        Map<BattleTimeAttackEncounter, BattleRules.RuleSet> map = GM;
        synchronized (map) {
            for (BattleRules.RuleSet ruleSet2 : GM.values()) {
                if (ruleSet2 != ruleSet) continue;
                return BattleRules.Mode.TIME_ATTACK;
            }
        }
        return ruleSet.mode();
    }

    static int number(Map<String, String> map, String string, int n) {
        String string2 = map.get(string);
        return string2 == null || string2.isBlank() ? n : Integer.parseInt(string2);
    }

    /*
     * Exception decompiling
     */
    static Map<String, String> npc(AccountRepository var0, String var1_1) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void spawn(ClientSession clientSession, AccountRepository accountRepository, String string, int n, int n2) throws Exception {
        List<ClientSession> list;
        if (n > 100 || n2 < 0 || n2 > 20) {
            throw new IllegalArgumentException("NPC \u6578\u91cf1\uff5e100\uff0c\u7bc4\u570d0\uff5e20\uff1b\u6bcf\u623f\u6700\u591a100\u500b GM NPC\u3002");
        }
        if (!LiToGmCommands.get(clientSession, "phase").toString().equals("BATTLE") || ((Boolean)LiToGmCommands.get(clientSession, "observer")).booleanValue() || (Integer)LiToGmCommands.get(clientSession, "playerHp") <= 0) {
            throw new IllegalArgumentException(".spawn \u8acb\u5728\u9032\u5165\u6230\u9b25\u5730\u5716\u5f8c\u4f7f\u7528\uff0c\u89c0\u6230\u6216\u6b7b\u4ea1\u72c0\u614b\u7121\u6cd5\u5efa\u7acb\u3002");
        }
        GameHub gameHub = (GameHub)LiToGmCommands.get(clientSession, "hub");
        long l = (Long)LiToGmCommands.get(clientSession, "currentRoomId");
        Map<String, String> map = LiToGmNpcs.npc(accountRepository, string);
        String string2 = map.getOrDefault("impl", "");
        if (!string2.equals("L1Monster")) {
            throw new IllegalArgumentException("\u76ee\u524d .spawn \u652f\u63f4\u6230\u9b25\u602a\u7269\uff08L1Monster\uff09\uff1b\u5546\u4eba\u3001\u5b88\u885b\u53ca\u7279\u6b8a\u529f\u80fd NPC \u5c1a\u672a\u958b\u653e\u3002");
        }
        int n3 = LiToGmNpcs.number(map, "npcid", 0);
        int n4 = LiToGmNpcs.number(map, "gfxid", 0);
        BattleAnimationCatalog.get().requireBattleCapableSprite(n4, 0);
        BattleAnimationCatalog.get().requireTimedAction(n4, 1);
        BattleTimeAttackEncounter.Template template = new BattleTimeAttackEncounter.Template(n3, n4, map.getOrDefault("nameid", map.getOrDefault("name", "NPC")), LiToGmNpcs.number(map, "lvl", 1), LiToGmNpcs.number(map, "hp", 1), LiToGmNpcs.number(map, "mp", 0), LiToGmNpcs.number(map, "int", 10));
        ArrayList<BattleTimeAttackEncounter.Snapshot> arrayList = new ArrayList<BattleTimeAttackEncounter.Snapshot>();
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            Object object = ((Map)LiToGmCommands.get(gameHub, "rooms")).get(l);
            if (object == null || !((Boolean)LiToGmCommands.get(object, "battleStarted")).booleanValue()) {
                throw new IllegalArgumentException("\u6230\u9b25\u5df2\u7d50\u675f\uff0c\u6c92\u6709\u5efa\u7acb NPC\u3002");
            }
            BattleRules.RuleSet ruleSet = gameHub.roomRules(l);
            if (ruleSet.mode() == BattleRules.Mode.TIME_ATTACK || ruleSet.mode() == BattleRules.Mode.QUEST_BATTLE || ruleSet.mode() == BattleRules.Mode.PET_RACE) {
                throw new IllegalArgumentException("\u8acb\u5728\u4e00\u822c\u6230\u9b25\u6a21\u5f0f\u4f7f\u7528 .spawn\uff1b\u7af6\u901f\uff0f\u95d6\u95dc\uff0f\u5bf5\u7269\u8cfd\u7684\u56fa\u5b9a\u6ce2\u6b21\u4e0d\u63a5\u53d7\u984d\u5916 NPC\u3002");
            }
            BattleTimeAttackEncounter battleTimeAttackEncounter = (BattleTimeAttackEncounter)LiToGmCommands.get(object, "timeAttackEncounter");
            List list2 = (List)LiToGmCommands.get(battleTimeAttackEncounter, "actors");
            if (list2.size() + n > 100) {
                throw new IllegalArgumentException("\u672c\u5834 GM NPC \u5df2\u9054100\u500b\u4e0a\u9650\uff08\u542b\u5c4d\u9ad4\uff09\u3002");
            }
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(ruleSet.mapId());
            int n5 = (Integer)LiToGmCommands.get(clientSession, "positionX");
            int n6 = (Integer)LiToGmCommands.get(clientSession, "positionY");
            ArrayList<ArenaMapCatalog.Point> arrayList2 = new ArrayList<ArenaMapCatalog.Point>();
            int n7 = n2 == 0 ? 3 : n2;
            for (int i = n6 - n7; i <= n6 + n7; ++i) {
                for (int j = n5 - n7; j <= n5 + n7; ++j) {
                    if (!arena.nativeStandingFloor(j, i) || battleTimeAttackEncounter.blocks(j, i) || j == n5 && i == n6) continue;
                    boolean bl = false;
                    for (ClientSession clientSession2 : gameHub.roomMembers(l)) {
                        if (clientSession2.battleX() != j || clientSession2.battleY() != i) continue;
                        bl = true;
                        break;
                    }
                    if (bl) continue;
                    arrayList2.add(new ArenaMapCatalog.Point(j, i));
                }
            }
            if (arrayList2.isEmpty()) {
                throw new IllegalArgumentException("\u9644\u8fd1\u6c92\u6709\u53ef\u653e\u7f6e NPC \u7684\u7a7a\u5730\u3002");
            }
            Collections.shuffle(arrayList2);
            Class<?> clazz = Class.forName("local.lito381.server.BattleTimeAttackEncounter$Monster");
            Constructor<?> constructor = clazz.getDeclaredConstructor(BattleTimeAttackEncounter.Template.class, Integer.TYPE, Integer.TYPE, String.class, String.class);
            constructor.setAccessible(true);
            ArrayList arrayList3 = new ArrayList();
            for (int i = 0; i < Math.min(n, arrayList2.size()); ++i) {
                ArenaMapCatalog.Point point = (ArenaMapCatalog.Point)arrayList2.get(i);
                Object obj = constructor.newInstance(template, point.x(), point.y(), "selected-source-npc GM", "GM one-shot, no wave progression");
                arrayList3.add(obj);
                arrayList.add((BattleTimeAttackEncounter.Snapshot)LiToGmCommands.call(obj, "snapshot", new Class[0], new Object[0]));
            }
            LiToGmCommands.set(battleTimeAttackEncounter, "arena", arena);
            LiToGmCommands.set(battleTimeAttackEncounter, "phase", (Object)BattleTimeAttackEncounter.Phase.FINAL_BOSSES);
            GM.put(battleTimeAttackEncounter, ruleSet);
            list2.addAll(arrayList3);
            LiToGmCommands.set(object, "pveAiPublishAfterNanos", System.nanoTime() + 2000000000L);
            list = gameHub.roomMembers(l);
        }
        for (ClientSession clientSession3 : list) {
            for (BattleTimeAttackEncounter.Snapshot snapshot : arrayList) {
                LiToGmCommands.call(clientSession3, "sendEncrypted", new Class[]{byte[].class, String.class}, Protocol.timeAttackActor(snapshot), "GM NPC spawn=" + snapshot.objectId());
            }
        }
        LiToGmCommands.reply(clientSession, "\u5df2\u5efa\u7acb NPC " + map.getOrDefault("name", string) + "\uff08" + n3 + "\uff09\u00d7" + arrayList.size() + (arrayList.size() < n ? "\uff1b\u9644\u8fd1\u7a7a\u5730\u4e0d\u8db3\uff0c\u5df2\u4f9d\u53ef\u7528\u4f4d\u7f6e\u6e1b\u5c11\u6578\u91cf\u3002" : "\u3002"));
        System.out.println("[LiTo] GM spawn room=" + l + " npc=" + n3 + " count=" + arrayList.size());
    }

    static BattleTimeAttackEncounter.Hit damage(BattleTimeAttackEncounter battleTimeAttackEncounter, Object object, int n, boolean bl) {
        try {
            BattleTimeAttackEncounter.Damage damage = (BattleTimeAttackEncounter.Damage)LiToGmCommands.call(object, bl ? "poisonDamage" : "damage", new Class[]{Integer.TYPE}, n);
            if (damage != null && damage.diedNow()) {
                LiToGmCommands.call(battleTimeAttackEncounter, "rememberCorpse", new Class[]{BattleTimeAttackEncounter.Snapshot.class, Long.TYPE}, damage.after(), System.nanoTime());
            }
            return new BattleTimeAttackEncounter.Hit(damage, false, battleTimeAttackEncounter.snapshots().size(), List.of());
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static BattleTimeAttackEncounter.Capture capture(BattleTimeAttackEncounter battleTimeAttackEncounter, long l) {
        try {
            BattleTimeAttackEncounter.Snapshot snapshot = battleTimeAttackEncounter.find(l);
            if (snapshot == null) {
                return null;
            }
            List list = (List)LiToGmCommands.get(battleTimeAttackEncounter, "actors");
            list.removeIf(object -> {
                try {
                    return (Long)LiToGmCommands.get(object, "objectId") == l;
                }
                catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            return new BattleTimeAttackEncounter.Capture(snapshot, battleTimeAttackEncounter.snapshots().size(), List.of());
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
