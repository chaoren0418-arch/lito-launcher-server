// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.lito381.server.BattleAnimationCatalog;
import local.lito381.server.BattlePolymorphCatalog;
import local.lito381.server.BattlePolymorphStatus;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClassicWorldNpcCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatRules;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationNpcRules;
import local.lito381.server.NativeCancellationNpcTransform;
import local.lito381.server.NativeCancellationProbability;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeSkillConsumption;
import local.lito381.server.NativeSkillConsumptionBridges;
import local.lito381.server.NativeWorldMaterialPolicy;
import local.lito381.server.Protocol;
import local.lito381.server.SourceEquipmentMagicStats;

final class NativeCancellationNpcStatic {
    static final String P = "local/lito381/server/";
    static final String FIELD = "nativeCancellationStaticAppearance";

    private static State state(GameHub gameHub, boolean bl) {
        if (!Thread.holdsLock(gameHub)) {
            throw new IllegalStateException("static NPC appearance requires hub ownership");
        }
        State state = (State)CombatAccess.field(gameHub, FIELD);
        if (state == null && bl) {
            state = new State();
            CombatAccess.set(gameHub, FIELD, state);
        }
        return state;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Appearance appearance(GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc) {
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            State state = NativeCancellationNpcStatic.state(gameHub, false);
            return state == null ? new Appearance(staticNpc.nameId(), staticNpc.passispeed()) : state.actors.getOrDefault(staticNpc.objectId(), new Appearance(staticNpc.nameId(), staticNpc.passispeed()));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean restored(boolean bl, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc) {
        if (bl) {
            GameHub gameHub2 = gameHub;
            synchronized (gameHub2) {
                State state = NativeCancellationNpcStatic.state(gameHub, false);
                if (state != null) {
                    state.actors.remove(staticNpc.objectId());
                    if (state.actors.isEmpty()) {
                        CombatAccess.set(gameHub, FIELD, null);
                    }
                }
            }
        }
        return bl;
    }

    static byte[] putObject(ClassicWorldNpcCatalog.StaticNpc staticNpc, int n, int n2, ClientSession clientSession) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        Appearance appearance = NativeCancellationNpcStatic.appearance(gameHub, staticNpc);
        ClassicWorldNpcCatalog.StaticNpc staticNpc2 = new ClassicWorldNpcCatalog.StaticNpc(staticNpc.mapId(), staticNpc.spawnId(), staticNpc.objectId(), staticNpc.npcId(), staticNpc.x(), staticNpc.y(), staticNpc.heading(), staticNpc.movementDistance(), appearance.name(), staticNpc.implementation(), staticNpc.gfxId(), staticNpc.level(), staticNpc.hp(), staticNpc.mp(), staticNpc.experience(), staticNpc.lawful(), staticNpc.lightSize(), appearance.speed(), staticNpc.atkspeed(), staticNpc.changeHead(), staticNpc.normalAction(), staticNpc.chaoticAction());
        return Protocol.classicStaticNpc(staticNpc2, n, n2);
    }

    static boolean route(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell) throws IOException {
        if (!CombatRules.loaded()) {
            return false;
        }
        Protocol.ObjectSpellRequest objectSpellRequest = Protocol.objectSpellRequest(byArray);
        int n = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
        ClassicWorldNpcCatalog.StaticNpc staticNpc = ClassicWorldNpcCatalog.npc(n, objectSpellRequest.targetObjectId());
        if (staticNpc == null || !staticNpc.implementation().equals("L1Quest")) {
            return false;
        }
        NativeCancellationNpcRules.Rule rule = NativeCancellationNpcRules.lookup(staticNpc.npcId());
        if (rule != null && rule.type() != 1) {
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeLightAction.commitPair(clientSession, clientSession, () -> NativeWorldMaterialPolicy.run(clientSession, true, 44, () -> {
            GameHub gameHub2 = gameHub;
            synchronized (gameHub2) {
                if (binding.matches(clientSession) && !((Boolean)CombatAccess.field(clientSession, "sourceItemDisconnected")).booleanValue()) {
                    NativeCancellationNpcStatic.cast(clientSession, gameHub, staticNpc, objectSpellRequest, cancellationSpell, rule);
                }
            }
        }));
        return true;
    }

    private static void cast(ClientSession clientSession, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc, Protocol.ObjectSpellRequest objectSpellRequest, BattleSpellCatalog.CancellationSpell cancellationSpell, NativeCancellationNpcRules.Rule rule) throws IOException {
        if (!String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") || ((Integer)CombatAccess.field(clientSession, "classicWorldMapId")).intValue() != staticNpc.mapId() || objectSpellRequest.skillId() != 44 || cancellationSpell.skillId() != 44) {
            return;
        }
        int n7 = (Integer)CombatAccess.field(clientSession, "positionX");
        int n8 = (Integer)CombatAccess.field(clientSession, "positionY");
        if (Math.max(Math.abs(n7 - staticNpc.x()), Math.abs(n8 - staticNpc.y())) > cancellationSpell.maximumRange() || !BattleSpellCatalog.isInNativeScreen(n7, n8, staticNpc.x(), staticNpc.y()) || !BattleAnimationCatalog.get().supportsTimedAction(CombatAccess.integerCall(clientSession, "battleGfx", "()I", new Object[0]), cancellationSpell.actionId())) {
            return;
        }
        long l = System.nanoTime();
        BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field(clientSession, "timedStatus");
        BattlePolymorphCatalog.Form form = ((BattlePolymorphStatus)CombatAccess.field(clientSession, "polymorphStatus")).active(l);
        if (form != null && !form.skillUsable() || battleTimedStatus.silenceActive(l)) {
            NativeCancellationNpcStatic.send(clientSession, Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE static-Cancellation-rejected");
            NativeCancellationNpcStatic.send(clientSession, Protocol.messageCode(285, new String[0]), "S_MESSAGE_CODE static-Cancellation-rejected");
            return;
        }
        if (CombatAccess.integerCall(clientSession, "currentWeight240", "(J)I", l) >= 197) {
            NativeCancellationNpcStatic.send(clientSession, Protocol.messageCode(316, new String[0]), "S_MESSAGE_CODE static-Cancellation-overweight");
            return;
        }
        if (!((Set)CombatAccess.field(clientSession, "learnedSkills")).contains(44) || ((Boolean)CombatAccess.field(clientSession, "observer")).booleanValue() || (Integer)CombatAccess.field(clientSession, "playerHp") <= 0 || CombatAccess.field(clientSession, "pendingPortal") != null || gameHub.hasPrivateShop(clientSession)) {
            return;
        }
        CharacterSummary characterSummary = (CharacterSummary)CombatAccess.call(clientSession, "selectedCharacterSummary", "()Llocal/lito381/server/CharacterSummary;", new Object[0]);
        SourceEquipmentMagicStats sourceEquipmentMagicStats = (SourceEquipmentMagicStats)CombatAccess.call(clientSession, "requiredEquipmentMagicStats", "()Llocal/lito381/server/SourceEquipmentMagicStats;", new Object[0]);
        int n9 = sourceEquipmentMagicStats.effectiveIntelligence(CombatAccess.integerCall(clientSession, "baseIntelligence", "()I", new Object[0]));
        int n10 = BattleSpellCatalog.originalMagicConsumeReduction(characterSummary == null ? 0 : characterSummary.type(), characterSummary == null ? 0 : Math.max(0, characterSummary.originalIntelligence()));
        int n11 = NativeSkillConsumptionBridges.CancellationSpell_effectiveMpCost_III(cancellationSpell, n9, n10);
        if (l < ((Map)CombatAccess.field(clientSession, "spellReadyNanos")).getOrDefault(44, 0L) || (Integer)CombatAccess.field(clientSession, "playerMp") < n11) {
            return;
        }
        if (rule != null && (rule.targetId() > 65535 || rule.targetId() <= 0)) {
            return;
        }
        if (!NativeLightAction.admitPair(clientSession, clientSession, 44, cancellationSpell.actionId(), cancellationSpell.reuseDelayMillis())) {
            return;
        }
        Target target = new Target(gameHub, staticNpc);
        if (rule != null) {
            NativeCancellationNpcTransform.apply(target, rule, (n, n2, n3, n4, n5, n6) -> null);
        }
        CombatAccess.call(clientSession, "cancelMeditation", "(Ljava/lang/String;)V", "spell-44");
        CombatAccess.set(clientSession, "playerMp", NativeSkillConsumption.boundMp(clientSession, (Integer)CombatAccess.field(clientSession, "playerMp") - n11));
        CombatAccess.call(clientSession, "commitSpellReady", "(IJ)V", 44, System.nanoTime() + (long)cancellationSpell.reuseDelayMillis() * 1000000L);
        CombatAccess.call(clientSession, "cancelAbsoluteBarrier", "(Ljava/lang/String;)V", "spell-44");
        List list = (List)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", staticNpc.mapId());
        for (ClientSession clientSession2 : list) {
            if (!((Boolean)CombatAccess.call(clientSession, "sameClassicWorldMap", "(Llocal/lito381/server/ClientSession;)Z", clientSession2)).booleanValue() || !BattleSpellCatalog.isInNativeScreen((Integer)CombatAccess.field(clientSession2, "positionX"), (Integer)CombatAccess.field(clientSession2, "positionY"), staticNpc.x(), staticNpc.y())) continue;
            if (!((Boolean)CombatAccess.field(clientSession, "invisible")).booleanValue() || clientSession2 == clientSession) {
                NativeCancellationNpcStatic.sendViewer(clientSession2, Protocol.objectAction(clientSession.playerObjectId(), cancellationSpell.actionId()), "S_ACTION static-Cancellation");
            }
            NativeCancellationNpcStatic.sendNpc(clientSession2, gameHub, staticNpc, Protocol.effectObject(staticNpc.objectId(), cancellationSpell.effectGfx()), "S_EFFECT static-Cancellation");
            for (NativeCancellationNpcTransform.Notice notice : target.notices) {
                if (notice instanceof NativeCancellationNpcTransform.Name) {
                    NativeCancellationNpcTransform.Name name = (NativeCancellationNpcTransform.Name)notice;
                    NativeCancellationNpcStatic.sendNpc(clientSession2, gameHub, staticNpc, Protocol.changeDescription(name.objectId(), name.name()), "S_CHANGE_NAME static-Cancellation");
                    continue;
                }
                if (!(notice instanceof NativeCancellationNpcTransform.Shape)) continue;
                NativeCancellationNpcTransform.Shape shape = (NativeCancellationNpcTransform.Shape)notice;
                NativeCancellationNpcStatic.sendNpc(clientSession2, gameHub, staticNpc, Protocol.classicStaticNpcShape(shape.objectId(), shape.gfx(), 0), "S_POLYMORPH static-Cancellation");
            }
        }
        CombatAccess.call(clientSession, "sendInventoryDelta", "(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V", NativeCastMaterial.previous(clientSession), NativeCastMaterial.next(clientSession));
        NativeCancellationNpcStatic.send(clientSession, Protocol.manaPoint((Integer)CombatAccess.field(clientSession, "playerMp"), (Integer)CombatAccess.field(clientSession, "playerMaxMp")), "S_MANA_POINT static-Cancellation");
    }

    private static void sendViewer(ClientSession clientSession, byte[] byArray, String string) {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.deferScoped(() -> {
            if (binding.matches(clientSession)) {
                clientSession.nativeSelfOriginalSend(byArray2, string);
            }
        });
    }

    private static void sendNpc(ClientSession clientSession, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc, byte[] byArray, String string) {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        int n = gameHub.classicWorldNpcGfx(staticNpc);
        Appearance appearance = NativeCancellationNpcStatic.appearance(gameHub, staticNpc);
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.deferScoped(() -> {
            GameHub gameHub2 = gameHub;
            synchronized (gameHub2) {
                if (!binding.matches(clientSession) || ClassicWorldNpcCatalog.npc(staticNpc.mapId(), staticNpc.objectId()) != staticNpc || gameHub.classicWorldNpcGfx(staticNpc) != n || !NativeCancellationNpcStatic.appearance(gameHub, staticNpc).equals(appearance)) {
                    return;
                }
            }
            clientSession.nativeSelfOriginalSend(byArray2, string);
        });
    }

    private static void send(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        NativeCancellationProbability.send(clientSession, byArray, string);
    }

    private NativeCancellationNpcStatic() {
    }

    static final class State {
        final Map<Long, Appearance> actors = new HashMap<Long, Appearance>();

        State() {
        }
    }

    record Appearance(String name, int speed) {
    }

    static final class Target
    implements NativeCancellationNpcTransform.Target {
        final GameHub hub;
        final ClassicWorldNpcCatalog.StaticNpc npc;
        final List<NativeCancellationNpcTransform.Notice> notices = new ArrayList<NativeCancellationNpcTransform.Notice>();

        Target(GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc) {
            this.hub = gameHub;
            this.npc = staticNpc;
        }

        @Override
        public long getId() {
            return this.npc.objectId();
        }

        @Override
        public int getTempCharGfx() {
            return this.hub.classicWorldNpcGfx(this.npc);
        }

        @Override
        public int getGfxId() {
            return this.npc.gfxId();
        }

        @Override
        public int getX() {
            return this.npc.x();
        }

        @Override
        public int getY() {
            return this.npc.y();
        }

        @Override
        public int getMapId() {
            return this.npc.mapId();
        }

        @Override
        public int getHeading() {
            return this.hub.classicWorldNpcHeading(this.npc);
        }

        @Override
        public void setTempCharGfx(int n) {
            if (!this.hub.transformClassicWorldNpcGfx(this.npc, this.npc.gfxId(), n)) {
                throw new IllegalStateException("static NPC appearance changed under owned hub");
            }
        }

        @Override
        public void setPassispeed(int n) {
            Appearance appearance = NativeCancellationNpcStatic.appearance(this.hub, this.npc);
            NativeCancellationNpcStatic.state((GameHub)this.hub, (boolean)true).actors.put(this.npc.objectId(), new Appearance(appearance.name(), n));
        }

        @Override
        public void setNameId(String string) {
            Appearance appearance = NativeCancellationNpcStatic.appearance(this.hub, this.npc);
            NativeCancellationNpcStatic.state((GameHub)this.hub, (boolean)true).actors.put(this.npc.objectId(), new Appearance(string, appearance.speed()));
        }

        @Override
        public void broadcastPacketAll(NativeCancellationNpcTransform.Notice notice) {
            this.notices.add(notice);
        }

        @Override
        public void deleteMe() {
            throw new IllegalStateException("static NPC replacement is not handled by the appearance adapter");
        }
    }
}
