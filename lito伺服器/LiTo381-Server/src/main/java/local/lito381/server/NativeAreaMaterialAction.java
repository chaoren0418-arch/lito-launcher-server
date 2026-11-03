// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationNpcMaterialAction;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerActionScope;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.NativeWorldMaterialPolicy;
import local.lito381.server.Protocol;
import local.lito381.server.SourceNpcPhysicalStats;

final class NativeAreaMaterialAction {
    static final String P = "local/lito381/server/";
    static final ThreadLocal<Context> current = new ThreadLocal();
    private static final ThreadLocal<Object[]> massUnlock = new ThreadLocal();

    static boolean companionSlowEligible(SourceNpcPhysicalStats sourceNpcPhysicalStats) {
        return sourceNpcPhysicalStats != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void massTeleport(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MassTeleportSpell massTeleportSpell) throws IOException {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        Object[] objectArray = massUnlock.get();
        Object[] objectArray2 = new Object[]{clientSession, Boolean.FALSE};
        massUnlock.set(objectArray2);
        try {
            NativeAreaMaterialAction.run(clientSession, massTeleportSpell.skillId(), () -> clientSession.nativeMaterialMassTeleportBody(byArray, massTeleportSpell));
        }
        finally {
            try {
                if (!((Boolean)objectArray2[1]).booleanValue() && binding.matches(clientSession) && CombatAccess.field(clientSession, "pendingPortal") == null) {
                    NativeRolePublication.completion(clientSession, () -> {
                        if (binding.matches(clientSession)) {
                            try {
                                clientSession.nativeSelfOriginalSend(Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport admission-unlock");
                            }
                            catch (IOException iOException) {
                                throw new UncheckedIOException(iOException);
                            }
                        }
                    }).run();
                }
            }
            finally {
                if (objectArray == null) {
                    massUnlock.remove();
                } else {
                    massUnlock.set(objectArray);
                }
            }
        }
    }

    static List<ClientSession> hubWorldMembers(GameHub gameHub, int n, int n2) {
        return NativeAreaMaterialAction.filter(gameHub.classicWorldMembers(n, n2));
    }

    static void worldSleep(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.SleepSpell sleepSpell) throws IOException {
        NativeAreaMaterialAction.run(clientSession, sleepSpell.skillId(), () -> clientSession.nativeAreaWorldSleepBody(byArray, sleepSpell));
    }

    static void battleSleep(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.SleepSpell sleepSpell) throws IOException {
        NativeAreaMaterialAction.run(clientSession, sleepSpell.skillId(), () -> clientSession.nativeAreaBattleSleepBody(byArray, sleepSpell));
    }

    static void worldSlow(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MassSlowSpell massSlowSpell) throws IOException {
        NativeAreaMaterialAction.run(clientSession, massSlowSpell.skillId(), () -> clientSession.nativeAreaWorldSlowBody(byArray, massSlowSpell));
    }

    static void battleSlow(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MassSlowSpell massSlowSpell) throws IOException {
        NativeAreaMaterialAction.run(clientSession, massSlowSpell.skillId(), () -> clientSession.nativeAreaBattleSlowBody(byArray, massSlowSpell));
    }

    static List<ClientSession> roomCombatants(GameHub gameHub, long l) {
        return NativeAreaMaterialAction.filter(gameHub.roomCombatants(l));
    }

    static ClassicWorldMonsterPopulation.Snapshot worldNpc(GameHub gameHub, int n, int n2, long l) {
        Context context = current.get();
        return context.npc(l) ? gameHub.classicWorldMonster(n, n2, l) : null;
    }

    static BattleQuestEncounter.Snapshot questNpc(GameHub gameHub, long l, long l2) {
        Context context = current.get();
        return context.npc(l2) ? gameHub.questMonster(l, l2) : null;
    }

    static BattleTimeAttackEncounter.Snapshot timeNpc(GameHub gameHub, long l, long l2) {
        Context context = current.get();
        return context.npc(l2) ? gameHub.timeAttackActor(l, l2) : null;
    }

    static List<BattleCompanion.Snapshot> companions(ClientSession clientSession) {
        Context context = current.get();
        return clientSession.classicWorldCompanionSnapshots().stream().filter(snapshot -> context.npc(snapshot.objectId())).toList();
    }

    static BattleCompanion companion(ClientSession clientSession, long l) {
        Context context = current.get();
        Npc npc = context.npcs.get(l);
        return npc != null && npc.kind == 3 && npc.owner == clientSession && context.npc(l) ? (BattleCompanion)npc.actor : null;
    }

    static Map<Long, Object> phantasms(ClientSession clientSession, Object object, long l, int n, int n2, int n3, int n4, int n5, long l2) {
        Map map = (Map)CombatAccess.call(clientSession, "phantasmTargets", "(Llocal/lito381/server/ClientSession$Phase;JIIIIIJ)Ljava/util/Map;", object, l, n, n2, n3, n4, n5, l2);
        if (map == null) {
            return null;
        }
        Context context = current.get();
        LinkedHashMap<Long, Object> linkedHashMap = new LinkedHashMap<Long, Object>();
        for (Map.Entry entry : map.entrySet()) {
            Object v = entry.getValue();
            ClientSession clientSession2 = (ClientSession)CombatAccess.call(v, "player", "()Llocal/lito381/server/ClientSession;", new Object[0]);
            String string = String.valueOf(CombatAccess.call(v, "kind", "()Llocal/lito381/server/BattleTrueTarget$Kind;", new Object[0]));
            if (!(string.equals("PLAYER") ? clientSession2 != null && context.player(clientSession2) : context.npc((Long)entry.getKey()))) continue;
            linkedHashMap.put((Long)entry.getKey(), v);
        }
        return linkedHashMap;
    }

    static void silence(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.AreaSilenceSpell areaSilenceSpell, int n) throws IOException {
        NativeAreaMaterialAction.run(clientSession, areaSilenceSpell.skillId(), () -> clientSession.nativeMaterialAreaSilenceBody(byArray, areaSilenceSpell, n));
    }

    static void wind(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.WindShackleSpell windShackleSpell, int n) throws IOException {
        NativeAreaMaterialAction.run(clientSession, windShackleSpell.skillId(), () -> clientSession.nativeMaterialAreaWindBody(byArray, windShackleSpell, n));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void run(ClientSession clientSession, int n, NativeSelfCastAction.Task task) throws IOException {
        Context context = new Context(clientSession);
        if (!context.phase.equals("WORLD") && !context.phase.equals("BATTLE")) {
            return;
        }
        List<ReentrantLock> list = context.players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();
        int n2 = 0;
        for (int i = 0; i < 32; ++i) {
            while (n2 < list.size() && list.get(n2).tryLock()) {
                ++n2;
            }
            if (n2 == list.size()) break;
            while (n2 > 0) {
                list.get(--n2).unlock();
            }
            if (Thread.currentThread().isInterrupted() || !context.caster.matches(clientSession)) {
                return;
            }
            LockSupport.parkNanos(1000000L);
        }
        if (n2 != list.size()) {
            return;
        }
        NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter((ClientSession[])context.players.toArray(ClientSession[]::new));
        try {
            if (!context.scene()) {
                return;
            }
            NativeLightAction.commitPair(clientSession, clientSession, () -> NativeWorldMaterialPolicy.run(clientSession, context.phase.equals("WORLD"), n, () -> {
                GameHub gameHub = context.hub;
                synchronized (gameHub) {
                    if (!context.scene()) {
                        return;
                    }
                    if (context.population != null) {
                        ClassicWorldMonsterPopulation classicWorldMonsterPopulation = context.population;
                        synchronized (classicWorldMonsterPopulation) {
                            NativeAreaMaterialAction.owned(context, task);
                        }
                    } else {
                        NativeAreaMaterialAction.owned(context, task);
                    }
                }
            }));
        }
        finally {
            while (n2 > 0) {
                list.get(--n2).unlock();
            }
            for (ClientSession clientSession2 : context.players) {
                NativePlayerActionScope.unlocked(clientSession2);
            }
            ticket.close();
        }
    }

    static void owned(Context context, NativeSelfCastAction.Task task) throws IOException {
        Context context2 = current.get();
        current.set(context);
        try {
            task.run();
        }
        finally {
            if (context2 == null) {
                current.remove();
            } else {
                current.set(context2);
            }
        }
    }

    static List<ClientSession> filter(List<ClientSession> list) {
        Context context = current.get();
        if (context == null) {
            throw new IllegalStateException("area membership outside cast");
        }
        return list.stream().filter(context::player).toList();
    }

    static List<ClientSession> worldMembers(ClientSession clientSession, int n) {
        Context context = current.get();
        return NativeAreaMaterialAction.filter(context.hub.classicWorldMembers(n, context.instance));
    }

    static List<ClientSession> battleMembers(GameHub gameHub, long l) {
        return NativeAreaMaterialAction.filter(gameHub.roomMembers(l));
    }

    static List<ClientSession> battleCombatants(GameHub gameHub, long l, ClientSession clientSession) {
        return NativeAreaMaterialAction.filter(gameHub.visibleBattleCombatants(l, clientSession));
    }

    static List<ClassicWorldMonsterPopulation.Snapshot> worldNpcs(GameHub gameHub, int n, int n2, int n3, int n4) {
        Context context = current.get();
        return gameHub.classicWorldMonstersInScreen(n, n2, n3, n4).stream().filter(snapshot -> context.npc(snapshot.objectId())).toList();
    }

    static List<BattleQuestEncounter.Snapshot> questNpcs(GameHub gameHub, long l, ClientSession clientSession) {
        Context context = current.get();
        return gameHub.visibleQuestMonsters(l, clientSession).stream().filter(snapshot -> context.npc(snapshot.objectId())).toList();
    }

    static List<BattleTimeAttackEncounter.Snapshot> timeNpcs(GameHub gameHub, long l, ClientSession clientSession) {
        Context context = current.get();
        return gameHub.visibleTimeAttackActors(l, clientSession).stream().filter(snapshot -> context.npc(snapshot.objectId())).toList();
    }

    static boolean admit(ClientSession clientSession, int n, int n2, int n3) {
        if (n != 161 && n != 167 && n != 66 && n != 76 && n != 69) {
            throw new IllegalStateException("unreviewed area skill");
        }
        boolean bl = NativeLightAction.admitPair(clientSession, clientSession, n, n2, n3);
        if (!bl && n == 69) {
            Object[] objectArray = massUnlock.get();
            if (objectArray != null) {
                objectArray[1] = Boolean.TRUE;
            }
            NativeLightAction.deferScoped(() -> clientSession.nativeSelfOriginalSend(Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport material-unlock"));
        }
        return bl;
    }

    static boolean captureOutput(ClientSession clientSession2, byte[] byArray, String string) {
        Context context;
        Object[] objectArray = massUnlock.get();
        if (objectArray != null && objectArray[0] == clientSession2 && Arrays.equals(byArray, Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK))) {
            objectArray[1] = Boolean.TRUE;
        }
        if ((context = current.get()) == null) {
            current.remove();
            return false;
        }
        NativePlayerCommit.Binding binding = context.bindings.get(clientSession2);
        if (binding == null) {
            return true;
        }
        byte[] byArray2 = (byte[])byArray.clone();
        long l = byArray2.length < 5 ? -1L : (long)(byArray2[1] & 0xFF) | (long)(byArray2[2] & 0xFF) << 8 | (long)(byArray2[3] & 0xFF) << 16 | (long)(byArray2[4] & 0xFF) << 24;
        List<ClientSession> list = context.players.stream().filter(clientSession -> clientSession.playerObjectId() == l).toList();
        boolean bl2 = context.npcs.containsKey(l);
        Runnable runnable = NativeRolePublication.completion(clientSession2, () -> {
            boolean bl2;
            GameHub gameHub = context.hub;
            synchronized (gameHub) {
                if (context.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = context.population;
                    synchronized (classicWorldMonsterPopulation) {
                        if (!binding.matches(clientSession2)) return;
                        if (!context.player(clientSession2)) return;
                        if (!list.stream().allMatch(context::player)) return;
                        if (bl2) {
                            if (!context.npc(l)) return;
                        }
                        boolean bl3 = true;
                        bl2 = bl3;
                    }
                } else {
                    if (!binding.matches(clientSession2)) return;
                    if (!context.player(clientSession2)) return;
                    if (!list.stream().allMatch(context::player)) return;
                    if (bl2) {
                        if (!context.npc(l)) return;
                    }
                    boolean bl4 = true;
                    bl2 = bl4;
                }
            }
            if (!bl2) return;
            try {
                clientSession2.nativeSelfOriginalSend(byArray2, string);
                return;
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        });
        NativeLightAction.finishScoped((bl, throwable) -> runnable.run());
        return true;
    }

    private NativeAreaMaterialAction() {
    }

    static final class Context {
        final ClientSession pc;
        final GameHub hub;
        final NativePlayerCommit.Binding caster;
        final String phase;
        final int map;
        final int instance;
        final long room;
        final List<ClientSession> players;
        final IdentityHashMap<ClientSession, NativePlayerCommit.Binding> bindings = new IdentityHashMap();
        final Map<Long, Npc> npcs = new HashMap<Long, Npc>();
        final ClassicWorldMonsterPopulation population;

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        Context(ClientSession clientSession) {
            this.pc = clientSession;
            this.hub = (GameHub)CombatAccess.field(clientSession, "hub");
            this.caster = NativePlayerCommit.Binding.capture(clientSession);
            this.phase = String.valueOf(CombatAccess.field(clientSession, "phase"));
            this.map = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
            this.instance = (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId");
            this.room = (Long)CombatAccess.field(clientSession, "currentRoomId");
            GameHub gameHub = this.hub;
            synchronized (gameHub) {
                ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>(this.phase.equals("WORLD") ? this.hub.classicWorldMembers(this.map, this.instance) : this.hub.roomMembers(this.room));
                if (!arrayList.contains(clientSession)) {
                    arrayList.add(clientSession);
                }
                this.players = arrayList.stream().distinct().sorted(Comparator.comparingInt(NativeSourcePlayerActionLock::signedId).thenComparingInt(System::identityHashCode)).toList();
                for (ClientSession object : this.players) {
                    this.bindings.put(object, NativePlayerCommit.Binding.capture(object));
                    for (BattleCompanion.Snapshot snapshot : this.phase.equals("WORLD") ? object.classicWorldCompanionSnapshots() : object.controlledCompanionSnapshots()) {
                        BattleCompanion battleCompanion = (BattleCompanion)CombatAccess.call(object, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", snapshot.objectId());
                        if (battleCompanion == null || !snapshot.active() || snapshot.hp() <= 0) continue;
                        this.npcs.put(snapshot.objectId(), new Npc(3, snapshot.objectId(), object, battleCompanion, NativeCombatAttacker.companion(object, battleCompanion)));
                    }
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population = this.phase.equals("WORLD") ? NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) : null;
                if (this.population != null) {
                    Iterator<Record> iterator = this.population;
                    synchronized (iterator) {
                        for (ClassicWorldMonsterPopulation.Snapshot snapshot : this.population.inScreen(this.map, (Integer)CombatAccess.field(clientSession, "positionX"), (Integer)CombatAccess.field(clientSession, "positionY"))) {
                            Object object = NativeCancellationNpcMaterialAction.actor(this.population, snapshot.objectId());
                            this.npcs.put(snapshot.objectId(), new Npc(0, snapshot.objectId(), this.population, object, NativeCancellationNpcMaterialAction.epoch(object, true)));
                        }
                    }
                }
                if (this.phase.equals("BATTLE")) {
                    for (BattleQuestEncounter.Snapshot snapshot : this.hub.visibleQuestMonsters(this.room, clientSession)) {
                        this.captureBattle(snapshot.objectId(), 1);
                    }
                    for (BattleTimeAttackEncounter.Snapshot snapshot : this.hub.visibleTimeAttackActors(this.room, clientSession)) {
                        this.captureBattle(snapshot.objectId(), 2);
                    }
                }
            }
        }

        Object encounter(int n) {
            Object v = ((Map)CombatAccess.field(this.hub, "rooms")).get(this.room);
            return v == null ? null : CombatAccess.field(v, n == 1 ? "questEncounter" : "timeAttackEncounter");
        }

        Object monster(Object object, int n, long l) {
            return object == null ? null : CombatAccess.call(object, "monster", "(J)Llocal/lito381/server/" + (n == 1 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter") + "$Monster;", l);
        }

        void captureBattle(long l, int n) {
            Object object = this.encounter(n);
            Object object2 = this.monster(object, n, l);
            if (object2 != null) {
                this.npcs.put(l, new Npc(n, l, object, object2, NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(object2, "damagePoison"), true)));
            }
        }

        boolean player(ClientSession clientSession) {
            NativePlayerCommit.Binding binding = this.bindings.get(clientSession);
            return binding != null && binding.matches(clientSession) && String.valueOf(CombatAccess.field(clientSession, "phase")).equals(this.phase) && (this.phase.equals("WORLD") ? (Integer)CombatAccess.field(clientSession, "classicWorldMapId") == this.map && (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId") == this.instance : (Long)CombatAccess.field(clientSession, "currentRoomId") == this.room);
        }

        boolean npc(long l) {
            Npc npc = this.npcs.get(l);
            if (npc == null) {
                return false;
            }
            if (npc.kind == 3) {
                ClientSession clientSession = (ClientSession)npc.owner;
                return this.player(clientSession) && CombatAccess.call(clientSession, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", l) == npc.actor && NativeCombatAttacker.alive(clientSession, (NativeCombatAttacker.Ref)npc.epoch);
            }
            if (npc.kind == 0) {
                return NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) == this.population && NativeCancellationNpcMaterialAction.actor(this.population, l) == npc.actor && NativeCancellationNpcMaterialAction.epoch(npc.actor, false) == npc.epoch && (Boolean)CombatAccess.field(npc.actor, "alive") != false && (Boolean)CombatAccess.field(npc.actor, "present") != false;
            }
            return this.encounter(npc.kind) == npc.owner && this.monster(npc.owner, npc.kind, l) == npc.actor && NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(npc.actor, "damagePoison"), false) == npc.epoch && (Integer)CombatAccess.field(npc.actor, "hp") > 0;
        }

        boolean scene() {
            return this.caster.matches(this.pc) && this.player(this.pc) && (Boolean)CombatAccess.field(this.pc, "sourceItemDisconnected") == false;
        }
    }

    record Npc(int kind, long id, Object owner, Object actor, Object epoch) {
    }
}
