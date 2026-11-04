// ported from candidate-50635f7e.jar (T020-B07); original source was area-sleep-slow NativeAreaMaterialAction
package local.lito381.server;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;


final class NativeAreaMaterialAction {
    static final String P = "local/lito381/server/";
    static final ThreadLocal<Context> current = new ThreadLocal();
    private static final ThreadLocal<Object[]> massUnlock = new ThreadLocal();

    static boolean companionSlowEligible(SourceNpcPhysicalStats sourceNpcPhysicalStats) {
        return sourceNpcPhysicalStats != null;
    }

    /*
     */
    static void massTeleport(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MassTeleportSpell massTeleportSpell) throws IOException {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture((ClientSession)clientSession);
        NativeSourceMassTeleportExit.Proof proof = NativeSourceMassTeleportExit.capture((ClientSession)clientSession, (NativePlayerCommit.Binding)binding);
        Object[] objectArray = massUnlock.get();
        Object[] objectArray2 = new Object[]{clientSession, Boolean.FALSE};
        massUnlock.set(objectArray2);
        try {
            NativeAreaMaterialAction.run(clientSession, massTeleportSpell.skillId(), () -> clientSession.nativeMaterialMassTeleportBody(byArray, massTeleportSpell));
        }
        finally {
            try {
                if (!((Boolean)objectArray2[1]).booleanValue() && NativeSourceMassTeleportExit.matches((NativePlayerCommit.Binding)binding, (ClientSession)clientSession, (NativeSourceMassTeleportExit.Proof)proof) && CombatAccess.field(clientSession, "pendingPortal") == null) {
                    NativeSourceMassTeleportExit.completion((ClientSession)clientSession, () -> {
                        if (NativeSourceMassTeleportExit.matches((NativePlayerCommit.Binding)binding, (ClientSession)clientSession, (NativeSourceMassTeleportExit.Proof)proof)) {
                            try {
                                NativeSourceMassTeleportExit.send((ClientSession)clientSession, (byte[])Protocol.paralysis((Protocol.ParalysisMode)Protocol.ParalysisMode.TELEPORT_UNLOCK), (String)"S_PARALYSE mass-teleport admission-unlock", (NativeSourceMassTeleportExit.Proof)proof);
                            }
                            catch (IOException iOException) {
                                throw new UncheckedIOException(iOException);
                            }
                        }
                    }, (NativeSourceMassTeleportExit.Proof)proof).run();
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
        return NativeCompanionTargets.areaSnapshots((ClientSession)clientSession).stream().filter(snapshot -> context.npc(snapshot.objectId())).toList();
    }

    static BattleCompanion companion(ClientSession clientSession, long l) {
        Context context = current.get();
        Npc npc = context.npcs.get(l);
        return npc != null && npc.kind == 3 && npc.owner == clientSession && context.npc(l) ? (BattleCompanion)npc.actor : null;
    }

    static Map<Long, Object> phantasms(ClientSession clientSession, Object object, long l, int n, int n2, int n3, int n4, int n5, long l2) {
        Map map = (Map)CombatAccess.call(clientSession, "nativeCompanionAreaPhantasms", "(Llocal/lito381/server/ClientSession$Phase;JIIIIIJ)Ljava/util/Map;", object, l, n, n2, n3, n4, n5, l2);
        if (map == null) {
            return null;
        }
        Context context = current.get();
        LinkedHashMap<Long, Object> linkedHashMap = new LinkedHashMap<Long, Object>();
        for (Map.Entry<?,?> entry : ((Map<?,?>)map).entrySet()) {
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
     */
    static void nativeCompanionAreaOriginalRun(ClientSession clientSession, int n, NativeSelfCastAction.Task task) throws IOException {
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
        NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter((ClientSession[])((ClientSession[])context.players.toArray(ClientSession[]::new)));
        try {
            if (!context.scene()) {
                return;
            }
            NativeLightAction.commitPair((ClientSession)clientSession, (ClientSession)clientSession, () -> NativeWorldMaterialPolicy.run((ClientSession)clientSession, (boolean)context.phase.equals("WORLD"), (int)n, () -> {
                GameHub gameHub = context.hub;
                synchronized (gameHub) {
                    if (!NativeSourceAreaPlayerProof.scene((Context)context)) {
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
                NativePlayerActionScope.unlocked((ClientSession)clientSession2);
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
        return list.stream().filter(arg_0 -> NativeSourceAreaPlayerProof.player((Context)context, arg_0)).toList();
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
        boolean bl = NativeLightAction.admitPair((ClientSession)clientSession, (ClientSession)clientSession, (int)n, (int)n2, (int)n3);
        if (!bl && n == 69) {
            Object[] objectArray = massUnlock.get();
            if (objectArray != null) {
                objectArray[1] = Boolean.TRUE;
            }
            NativeLightAction.deferScoped(() -> clientSession.nativeSelfOriginalSend(Protocol.paralysis((Protocol.ParalysisMode)Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport material-unlock"));
        }
        return bl;
    }

    static boolean nativeTargetOriginalCaptureOutput(ClientSession clientSession2, byte[] byArray, String string) {
        Context context;
        Object[] objectArray = massUnlock.get();
        if (objectArray != null && objectArray[0] == clientSession2 && Arrays.equals(byArray, Protocol.paralysis((Protocol.ParalysisMode)Protocol.ParalysisMode.TELEPORT_UNLOCK))) {
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
        Runnable runnable = NativeRolePublication.completion((ClientSession)clientSession2, () -> {
            boolean valid;
            GameHub gameHub = context.hub;
            synchronized (gameHub) {
                if (context.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = context.population;
                    synchronized (classicWorldMonsterPopulation) {
                        valid = binding.matches(clientSession2) && context.player(clientSession2)
                                && list.stream().allMatch(arg_0 -> NativeSourceIndirectPlayerProof.player((Context)context, arg_0))
                                && (!bl2 || NativeCompanionTargets.publication((Context)context, (long)l));
                    }
                } else {
                    valid = binding.matches(clientSession2) && context.player(clientSession2)
                            && list.stream().allMatch(arg_0 -> NativeSourceIndirectPlayerProof.player((Context)context, arg_0))
                            && (!bl2 || NativeCompanionTargets.publication((Context)context, (long)l));
                }
            }
            if (!valid) return;
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


    static boolean nativeShockOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeWeaponBreakTargetLifecycle.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    /*
     */
    static void nativeCompanionAreaCapturedRun(ClientSession clientSession, int n, NativeSelfCastAction.Task task, Context context) throws IOException {
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
            if (Thread.currentThread().isInterrupted() || !NativeSourceNegativePairFirst.retry((NativePlayerCommit.Binding)context.caster, (ClientSession)clientSession, (Context)context)) {
                return;
            }
            LockSupport.parkNanos(1000000L);
        }
        if (n2 != list.size()) {
            return;
        }
        NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter((ClientSession[])((ClientSession[])context.players.toArray(ClientSession[]::new)));
        try {
            if (!NativeSourceNegativePairFirst.scene((Context)context)) {
                return;
            }
            NativeLightAction.commitPair((ClientSession)clientSession, (ClientSession)clientSession, () -> NativeWorldMaterialPolicy.run((ClientSession)clientSession, (boolean)context.phase.equals("WORLD"), (int)n, () -> {
                GameHub gameHub = context.hub;
                synchronized (gameHub) {
                    if (!NativeSourceAreaPlayerProof.scene((Context)context)) {
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
                NativePlayerActionScope.unlocked((ClientSession)clientSession2);
            }
            ticket.close();
        }
    }

    static void run(ClientSession clientSession, int n, NativeSelfCastAction.Task task) throws IOException {
        NativeCompanionWindAction.run((ClientSession)clientSession, (int)n, (NativeSelfCastAction.Task)task);
    }

    static boolean nativeShockPlayerOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeShockCompanionAction.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    static boolean nativeAcceptedShockOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeShockPlayerAction.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    static boolean nativeStaticRespawnOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeAcceptedShockMutation.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    static boolean nativeFreezeAcceptedOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeStaticNpcRespawn.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    static boolean nativeBattleControlledOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeAcceptedFreezeMutation.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
    }

    static boolean nativeSourceMassSlowOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeBattleControlledCancellation.active()) {
            return NativeBattleControlledCancellation.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeBattleControlledOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceWindOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceMassSlowTargets.active()) {
            return NativeSourceMassSlowTargets.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceMassSlowOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceNegativeOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceWindTargets.active()) {
            return NativeSourceWindTargets.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceWindOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceLightningOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceNegativeTargets.active()) {
            return NativeSourceNegativeTargets.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceNegativeOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceWeaponBuffOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceLightningLifetime.active()) {
            return NativeSourceLightningLifetime.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceLightningOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceRemainingOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceWeaponBuffTargets.active()) {
            return NativeSourceWeaponBuffTargets.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceWeaponBuffOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceControlledOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceRemainingMaterials.active()) {
            return NativeSourceRemainingMaterials.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string);
        }
        return NativeAreaMaterialAction.nativeSourceRemainingOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeRawOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceControlledDisplay.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string)) {
            return true;
        }
        return NativeAreaMaterialAction.nativeSourceControlledOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) {
        if (NativeSourceRawDamage.captureOutput((ClientSession)clientSession, (byte[])byArray, (String)string)) {
            return true;
        }
        return NativeAreaMaterialAction.nativeRawOriginalCaptureOutput(clientSession, byArray, string);
    }

    static final class Context {
        final ClientSession pc;
        final GameHub hub;
        final NativePlayerCommit.Binding caster;
        final String phase;
        final int map;
        final int instance;
        final long room;
        List<ClientSession> players;
        IdentityHashMap<ClientSession, NativePlayerCommit.Binding> bindings;
        final Map<Long, Npc> npcs;
        ClassicWorldMonsterPopulation population;
        int nativeWeaponBreakTarget;
        long nativeWeaponBreakTargetId;
        NativePlayerVitalCompletions.Receipt nativeWeaponBreakPeerReceipt;
        Object nativeCompanionTargetScope;
        boolean nativeCorpseTargetScope;
        Object nativeShockPlayerScope;
        Object nativeStaticRespawnTarget;
        Object nativeAcceptedFreezeBatch;
        volatile Object nativeBattleControlledCancellationScope;
        volatile Object nativeSourceMassSlowAim;
        volatile Object nativeSourceWindAim;
        volatile Object nativeSourceNegativeAim;
        volatile Object nativeSourceLightningAim;
        volatile Object nativeSourceWeaponBuffAim;
        volatile Object nativeSourceRemainingAim;
        volatile Object nativeSourceControlledAction;
        volatile Object nativeSourceRawAction;
        volatile Object nativeSourceArenaPoisonOutput;
        volatile Object nativeSourceDollNpcScope;
        volatile Object nativeSourceDollAction;
        volatile Object nativeSourceDollOutput;
        volatile Object nativeSourceDollDefenseSnapshot;
        volatile Object nativeSourceCubePlayerOutput;
        volatile Object nativeSourceCubeNpcOutput;
        volatile Object nativeSourcePoisonNpcOutput;
        volatile Object nativeSourcePhysicalNpcOutput;
        volatile Object nativeSourcePhysicalNpcCommit;
        Object nativeSourceMagicSnapshot;
        volatile Object nativeSourceMagicAdmission;
        volatile Object nativeSourceSingleNpcOutput;
        volatile Object nativeSourceSpecialNpc;
        volatile Object nativeSourceGroundProof;
        volatile Object nativeSourceCubeStatusProof;
        volatile Object nativeSourceHealingCast;
        volatile Object nativeSourceFirstPlayers;
        volatile transient Object nativeSourceTargetNegativeProof;
        volatile transient Object nativeSourceWeaponFirstLife;
        transient Object nativeSourceControlledNegativeFinal;

        /*
         */
        Context(ClientSession clientSession) {
            this.bindings = new IdentityHashMap();
            this.npcs = new HashMap<Long, Npc>();
            this.pc = clientSession;
            this.hub = (GameHub)CombatAccess.field(clientSession, "hub");
            this.caster = NativeSourcePlayerFactories.caster((ClientSession)clientSession, (Context)this);
            this.phase = String.valueOf(CombatAccess.field(clientSession, "phase"));
            this.map = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
            this.instance = (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId");
            this.room = (Long)CombatAccess.field(clientSession, "currentRoomId");
            GameHub gameHub = this.hub;
            synchronized (gameHub) {
                ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>(this.phase.equals("WORLD") ? NativeCompanionTargets.owners((List)this.hub.classicWorldMembers(this.map, this.instance), (ClientSession)clientSession) : this.hub.roomMembers(this.room));
                if (!arrayList.contains(clientSession)) {
                    arrayList.add(clientSession);
                }
                this.players = arrayList.stream().distinct().sorted(Comparator.comparingInt(NativeSourcePlayerActionLock::signedId).thenComparingInt(System::identityHashCode)).toList();
                for (ClientSession object : this.players) {
                    this.bindings.put(object, NativeSourceDollAudience.capture((ClientSession)object, (Context)this));
                    for (BattleCompanion.Snapshot object2 : this.phase.equals("WORLD") ? object.classicWorldCompanionSnapshots() : object.controlledCompanionSnapshots()) {
                        BattleCompanion battleCompanion = (BattleCompanion)CombatAccess.call(object, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", object2.objectId());
                        if (battleCompanion == null || !object2.active() || object2.hp() <= 0) continue;
                        this.npcs.put(object2.objectId(), new Npc(3, object2.objectId(), object, battleCompanion, NativeCombatAttacker.companion((ClientSession)object, (BattleCompanion)battleCompanion)));
                    }
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population = this.phase.equals("WORLD") ? NativeCancellationNpcMaterialAction.population((GameHub)this.hub, (int)this.map, (int)this.instance) : null;
                if (this.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation2 = this.population;
                    synchronized (classicWorldMonsterPopulation2) {
                        for (ClassicWorldMonsterPopulation.Snapshot snapshot : this.population.inScreen(this.map, ((Integer)CombatAccess.field(clientSession, "positionX")).intValue(), ((Integer)CombatAccess.field(clientSession, "positionY")).intValue())) {
                            Object object2;
                            object2 = NativeCancellationNpcMaterialAction.actor((ClassicWorldMonsterPopulation)this.population, (long)snapshot.objectId());
                            this.npcs.put(snapshot.objectId(), new Npc(0, snapshot.objectId(), this.population, object2, NativeCancellationNpcMaterialAction.epoch((Object)object2, (boolean)true)));
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
            NativeCompanionTargets.capture((Context)this);
            NativeSourceControlledDisplay.inherit((Context)this);
            NativeSourceRawDamage.inherit((Context)this);
            NativeSourceArenaPoison.inherit((Context)this);
            NativeSourceDollAction.inherit((Context)this);
            NativeSourceDollOutput.inherit((Context)this);
            this.nativeSourceDollDefenseSnapshot = NativeSourceDollDefense.snapshot((Context)this);
            NativeSourceCubePlayer.inherit((Context)this);
            NativeSourceCubeNpc.inherit((Context)this);
            NativeSourcePoisonNpc.inherit((Context)this);
            NativeSourcePhysicalNpc.inherit((Context)this);
            NativeSourceFirstPlayers.seal((Context)this);
            this.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot((Context)this);
            NativeSourceSingleNpc.inherit((Context)this);
            NativeSourceCubeStatus.inherit((Context)this);
            NativeSourceFirstPlayers.seal((Context)this);
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
                this.npcs.put(l, new Npc(n, l, object, object2, NativeCombatAttacker.token((BattleTimedStatus)((BattleTimedStatus)CombatAccess.field(object2, "damagePoison")), (boolean)true)));
            }
        }

        boolean player(ClientSession clientSession) {
            NativePlayerCommit.Binding binding = this.bindings.get(clientSession);
            return binding != null && NativeSourceControlledRoleFrame.current((NativePlayerCommit.Binding)binding, (ClientSession)clientSession) && String.valueOf(CombatAccess.field(clientSession, "phase")).equals(this.phase) && (this.phase.equals("WORLD") ? (Integer)CombatAccess.field(clientSession, "classicWorldMapId") == this.map && (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId") == this.instance : (Long)CombatAccess.field(clientSession, "currentRoomId") == this.room);
        }

        boolean nativeCompanionOriginalNpc(long l) {
            Npc npc = this.npcs.get(l);
            if (npc == null) {
                return false;
            }
            if (npc.kind == 3) {
                ClientSession clientSession = (ClientSession)npc.owner;
                return this.player(clientSession) && CombatAccess.call(clientSession, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", l) == npc.actor && NativeCombatAttacker.alive((ClientSession)clientSession, (NativeCombatAttacker.Ref)((NativeCombatAttacker.Ref)npc.epoch));
            }
            if (npc.kind == 0) {
                return NativeCancellationNpcMaterialAction.population((GameHub)this.hub, (int)this.map, (int)this.instance) == this.population && NativeCancellationNpcMaterialAction.actor((ClassicWorldMonsterPopulation)this.population, (long)l) == npc.actor && NativeCancellationNpcMaterialAction.epoch((Object)npc.actor, (boolean)false) == npc.epoch && (Boolean)CombatAccess.field(npc.actor, "alive") != false && (Boolean)CombatAccess.field(npc.actor, "present") != false;
            }
            return this.encounter(npc.kind) == npc.owner && this.monster(npc.owner, npc.kind, l) == npc.actor && NativeCombatAttacker.token((BattleTimedStatus)((BattleTimedStatus)CombatAccess.field(npc.actor, "damagePoison")), (boolean)false) == npc.epoch && (Integer)CombatAccess.field(npc.actor, "hp") > 0;
        }

        boolean scene() {
            return NativeSourceControlledRoleFrame.current((NativePlayerCommit.Binding)this.caster, (ClientSession)this.pc) && this.player(this.pc) && (Boolean)CombatAccess.field(this.pc, "sourceItemDisconnected") == false;
        }

        boolean npc(long l) {
            return NativeCompanionTargets.npc((Context)this, (long)l);
        }

        Context(Context context) {
            this.pc = context.pc;
            this.hub = context.hub;
            this.caster = context.caster;
            this.phase = context.phase;
            this.map = context.map;
            this.instance = context.instance;
            this.room = context.room;
            this.players = context.players;
            this.bindings = context.bindings;
            this.npcs = context.npcs;
            this.population = context.population;
            this.nativeWeaponBreakTarget = context.nativeWeaponBreakTarget;
            this.nativeWeaponBreakTargetId = context.nativeWeaponBreakTargetId;
            this.nativeWeaponBreakPeerReceipt = context.nativeWeaponBreakPeerReceipt;
            this.nativeCompanionTargetScope = context.nativeCompanionTargetScope;
            this.nativeCorpseTargetScope = context.nativeCorpseTargetScope;
            this.nativeShockPlayerScope = context.nativeShockPlayerScope;
            this.nativeStaticRespawnTarget = context.nativeStaticRespawnTarget;
            this.nativeAcceptedFreezeBatch = null;
            NativeSourceControlledDisplay.inherit((Context)this, (Context)context);
            NativeSourceRawDamage.inherit((Context)this, (Context)context);
            NativeSourceArenaPoison.inherit((Context)this, (Context)context);
            NativeSourceDollAction.inherit((Context)this, (Context)context);
            NativeSourceDollOutput.inherit((Context)this, (Context)context);
            this.nativeSourceDollDefenseSnapshot = NativeSourceDollDefense.snapshot((Context)this);
            NativeSourceCubePlayer.inherit((Context)this, (Context)context);
            NativeSourceCubeNpc.inherit((Context)this, (Context)context);
            NativeSourcePoisonNpc.inherit((Context)this, (Context)context);
            NativeSourcePhysicalNpc.inherit((Context)this, (Context)context);
            NativeSourceFirstPlayers.inherit((Context)this, (Context)context);
            this.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot((Context)this);
            NativeSourceSingleNpc.inherit((Context)this, (Context)context);
            NativeSourceCubeStatus.inherit((Context)this, (Context)context);
            NativeSourceFirstPlayers.inherit((Context)this, (Context)context);
        }

        Context(ClientSession clientSession, NativeSourcePhysicalNpc.Commit commit) {
            this(clientSession);
            this.players = commit.members;
            this.bindings = new IdentityHashMap(commit.viewers);
            NativeSourcePhysicalNpc.bind((Context)this, (NativeSourcePhysicalNpc.Commit)commit);
            NativeSourceFirstPlayers.seal((Context)this);
            this.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot((Context)this);
            NativeSourceSingleNpc.inherit((Context)this);
            NativeSourceCubeStatus.inherit((Context)this);
        }

        Context(ClientSession clientSession, NativeSourcePhysicalNpc.Output output) {
            this.pc = clientSession;
            this.hub = output.commit().origin.scene().hub();
            this.caster = output.binding();
            this.phase = "BATTLE";
            this.room = output.binding().room();
            this.map = output.binding().map();
            this.instance = output.binding().instance();
            this.players = List.of(clientSession);
            this.bindings = new IdentityHashMap();
            this.bindings.put(clientSession, output.binding());
            this.npcs = new HashMap<Long, Npc>();
            Context context = current.get();
            NativeCompanionTargets.capture((Context)this);
            NativeSourceControlledDisplay.inherit((Context)this);
            NativeSourceRawDamage.inherit((Context)this);
            NativeSourceArenaPoison.inherit((Context)this);
            NativeSourceDollAction.inherit((Context)this);
            NativeSourceDollOutput.inherit((Context)this);
            if (NativeSourceDollCommit.enabled((ClientSession)clientSession) && context != null && context.pc == clientSession && context.nativeSourceDollDefenseSnapshot instanceof NativeSourceDollDefense.Snapshot) {
                this.nativeSourceDollDefenseSnapshot = context.nativeSourceDollDefenseSnapshot;
            }
            NativeSourceCubePlayer.inherit((Context)this);
            NativeSourceCubeNpc.inherit((Context)this);
            NativeSourcePoisonNpc.inherit((Context)this);
            NativeSourcePhysicalNpc.inherit((Context)this);
            if (context != null && context.pc == clientSession && context.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot) {
                this.nativeSourceMagicSnapshot = context.nativeSourceMagicSnapshot;
            }
            NativeSourceSingleNpc.inherit((Context)this);
            NativeSourceCubeStatus.inherit((Context)this);
            NativeSourceFirstPlayers.seal((Context)this);
        }

        Context(ClientSession clientSession, NativePlayerCommit.Binding binding, GameHub gameHub) {
            this.pc = clientSession;
            this.hub = gameHub;
            this.caster = binding;
            this.phase = "BATTLE";
            this.room = binding.room();
            this.map = binding.map();
            this.instance = binding.instance();
            this.players = List.of(clientSession);
            this.bindings = new IdentityHashMap();
            this.bindings.put(clientSession, binding);
            this.npcs = new HashMap<Long, Npc>();
            Context context = current.get();
            NativeCompanionTargets.capture((Context)this);
            NativeSourceControlledDisplay.inherit((Context)this);
            NativeSourceRawDamage.inherit((Context)this);
            NativeSourceArenaPoison.inherit((Context)this);
            NativeSourceDollAction.inherit((Context)this);
            NativeSourceDollOutput.inherit((Context)this);
            if (NativeSourceDollCommit.enabled((ClientSession)clientSession) && context != null && context.pc == clientSession && context.nativeSourceDollDefenseSnapshot instanceof NativeSourceDollDefense.Snapshot) {
                this.nativeSourceDollDefenseSnapshot = context.nativeSourceDollDefenseSnapshot;
            }
            NativeSourceCubePlayer.inherit((Context)this);
            NativeSourceCubeNpc.inherit((Context)this);
            NativeSourcePoisonNpc.inherit((Context)this);
            NativeSourcePhysicalNpc.inherit((Context)this);
            if (context != null && context.pc == clientSession && context.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot) {
                this.nativeSourceMagicSnapshot = context.nativeSourceMagicSnapshot;
            }
            NativeSourceSingleNpc.inherit((Context)this);
            NativeSourceCubeStatus.inherit((Context)this);
            NativeSourceFirstPlayers.seal((Context)this);
        }

        Context(ClientSession clientSession, NativePlayerCommit.Binding binding, GameHub gameHub, NativeSourceElementTransport.Inherited inherited) {
            this(clientSession, binding, gameHub);
            this.nativeCompanionTargetScope = inherited.nativeCompanionTargetScope();
            this.nativeSourceControlledAction = inherited.nativeSourceControlledAction();
            this.nativeSourceRawAction = inherited.nativeSourceRawAction();
            this.nativeSourceArenaPoisonOutput = inherited.nativeSourceArenaPoisonOutput();
            this.nativeSourceDollAction = inherited.nativeSourceDollAction();
            this.nativeSourceDollOutput = inherited.nativeSourceDollOutput();
            this.nativeSourceCubePlayerOutput = inherited.nativeSourceCubePlayerOutput();
            this.nativeSourceCubeNpcOutput = inherited.nativeSourceCubeNpcOutput();
            this.nativeSourcePoisonNpcOutput = inherited.nativeSourcePoisonNpcOutput();
            this.nativeSourcePhysicalNpcOutput = inherited.nativeSourcePhysicalNpcOutput();
            this.nativeSourcePhysicalNpcCommit = inherited.nativeSourcePhysicalNpcCommit();
            this.nativeSourceSingleNpcOutput = inherited.nativeSourceSingleNpcOutput();
            this.nativeSourceCubeStatusProof = inherited.nativeSourceCubeStatusProof();
            this.nativeSourceDollDefenseSnapshot = inherited.nativeSourceDollDefenseSnapshot();
            this.nativeSourceMagicSnapshot = inherited.nativeSourceMagicSnapshot();
            this.nativeSourceFirstPlayers = inherited.nativeSourceFirstPlayers();
        }
    }

    record Npc(int kind, long id, Object owner, Object actor, Object epoch) {
    }
}

