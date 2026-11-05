// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-npc-static/NativeCancellationNpcStatic.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
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
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSkillConsumption;
import local.lito381.server.NativeSkillConsumptionBridges;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.NativeStaticCancellationReplacement;
import local.lito381.server.NativeStaticNpcMotion;
import local.lito381.server.NativeStaticNpcRespawn;
import local.lito381.server.NativeStaticNpcScene;
import local.lito381.server.NativeWorldMaterialPolicy;
import local.lito381.server.Protocol;
import local.lito381.server.SourceEquipmentMagicStats;
import java.io.IOException;import java.util.*;

/** Scene-owned L1Quest appearance state. Source SQL remains a startup-only input. */
final class NativeCancellationNpcStatic {
 static final String P="local/lito381/server/",FIELD="nativeCancellationStaticAppearance";
 record Appearance(String name,int speed){}
 static final class State {final Map<Long,Appearance> actors=new HashMap<>();}
 private static State state(GameHub hub,boolean create){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("static NPC appearance requires hub ownership");
  State s=(State)CombatAccess.field(hub,FIELD);if(s==null&&create){s=new State();CombatAccess.set(hub,FIELD,s);}return s;
 }
 static Appearance appearance(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc){
  synchronized(hub){State s=state(hub,false);return s==null?new Appearance(npc.nameId(),npc.passispeed()):s.actors.getOrDefault(npc.objectId(),new Appearance(npc.nameId(),npc.passispeed()));}
 }
    static boolean restored(boolean bl, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc) {
        if (bl) {
            synchronized (gameHub) {
                State state = NativeCancellationNpcStatic.state(gameHub, false);
                if (state != null) {
                    state.actors.remove(staticNpc.objectId());
                    if (state.actors.isEmpty()) {
                        CombatAccess.set(gameHub, FIELD, null);
                    }
                }
            }
        }
        NativeStaticNpcMotion.changed(gameHub);
        return bl;
    }
    static byte[] putObject(ClassicWorldNpcCatalog.StaticNpc staticNpc, int n, int n2, ClientSession clientSession) {
        NativeStaticNpcMotion.touch(staticNpc, clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        Appearance appearance = NativeStaticNpcScene.appearance(gameHub, staticNpc, NativeStaticNpcScene.instance(clientSession));
        ClassicWorldNpcCatalog.StaticNpc staticNpc2 = new ClassicWorldNpcCatalog.StaticNpc(staticNpc.mapId(), staticNpc.spawnId(), staticNpc.objectId(), staticNpc.npcId(), NativeStaticNpcMotion.x(staticNpc, clientSession), NativeStaticNpcMotion.y(staticNpc, clientSession), staticNpc.heading(), staticNpc.movementDistance(), appearance.name(), staticNpc.implementation(), staticNpc.gfxId(), staticNpc.level(), staticNpc.hp(), staticNpc.mp(), staticNpc.experience(), staticNpc.lawful(), staticNpc.lightSize(), appearance.speed(), staticNpc.atkspeed(), staticNpc.changeHead(), staticNpc.normalAction(), staticNpc.chaoticAction());
        return Protocol.classicStaticNpc(staticNpc2, n, n2);
    }
    static final class Target
    implements NativeCancellationNpcTransform.Target {
        final GameHub hub;
        final ClientSession pc;
        final int instance;
        final ClassicWorldNpcCatalog.StaticNpc npc;
        final List<NativeCancellationNpcTransform.Notice> notices = new ArrayList<NativeCancellationNpcTransform.Notice>();

        Target(GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc, ClientSession clientSession) {
            this.hub = gameHub;
            this.npc = staticNpc;
            this.pc = clientSession;
            this.instance = NativeStaticNpcScene.instance(clientSession);
        }

        @Override
        public long getId() {
            return this.npc.objectId();
        }

        @Override
        public int getTempCharGfx() {
            return NativeStaticNpcScene.gfx(this.hub, this.npc, this.pc);
        }

        @Override
        public int getGfxId() {
            return this.npc.gfxId();
        }

        @Override
        public int getX() {
            return NativeStaticNpcMotion.x(this.npc, this.pc);
        }

        @Override
        public int getY() {
            return NativeStaticNpcMotion.y(this.npc, this.pc);
        }

        @Override
        public int getMapId() {
            return this.npc.mapId();
        }

        @Override
        public int getHeading() {
            return NativeStaticNpcScene.heading(this.hub, this.npc, this.pc);
        }

        @Override
        public void setTempCharGfx(int n) {
            if (!NativeStaticNpcScene.transform(this.hub, this.npc, this.npc.gfxId(), n, this.pc)) {
                throw new IllegalStateException("static NPC appearance changed under owned hub");
            }
        }

        @Override
        public void setPassispeed(int n) {
            Appearance appearance = NativeStaticNpcScene.appearance(this.hub, this.npc, this.instance);
            this.store(new Appearance(appearance.name(), n));
        }

        @Override
        public void setNameId(String string) {
            Appearance appearance = NativeStaticNpcScene.appearance(this.hub, this.npc, this.instance);
            this.store(new Appearance(string, appearance.speed()));
        }

        private void store(Appearance appearance) {
            if (this.instance <= 0) {
                NativeCancellationNpcStatic.state((GameHub)this.hub, (boolean)true).actors.put(this.npc.objectId(), appearance);
            } else {
                NativeStaticNpcScene.appearance(this.hub, this.npc, this.instance, appearance);
            }
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
    static boolean route(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell) throws IOException {
        return NativeStaticCancellationReplacement.route(clientSession, byArray, cancellationSpell);
    }
    private static void cast(ClientSession clientSession, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc, Protocol.ObjectSpellRequest objectSpellRequest, BattleSpellCatalog.CancellationSpell cancellationSpell, NativeCancellationNpcRules.Rule rule) throws IOException {
        if (!NativeStaticNpcRespawn.castAllowed(clientSession, staticNpc)) {
            return;
        }
        if (!String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") || ((Integer)CombatAccess.field(clientSession, "classicWorldMapId")).intValue() != staticNpc.mapId() || objectSpellRequest.skillId() != 44 || cancellationSpell.skillId() != 44) {
            return;
        }
        int n7 = (Integer)CombatAccess.field(clientSession, "positionX");
        int n8 = (Integer)CombatAccess.field(clientSession, "positionY");
        if (Math.max(Math.abs(n7 - NativeStaticNpcMotion.x(staticNpc, clientSession)), Math.abs(n8 - NativeStaticNpcMotion.y(staticNpc, clientSession))) > cancellationSpell.maximumRange() || !BattleSpellCatalog.isInNativeScreen(n7, n8, NativeStaticNpcMotion.x(staticNpc, clientSession), NativeStaticNpcMotion.y(staticNpc, clientSession)) || !BattleAnimationCatalog.get().supportsTimedAction(CombatAccess.integerCall(clientSession, "battleGfx", "()I", new Object[0]), cancellationSpell.actionId())) {
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
        if (l < (Long)((Map)CombatAccess.field(clientSession, "spellReadyNanos")).getOrDefault(44, 0L) || (Integer)CombatAccess.field(clientSession, "playerMp") < n11) {
            return;
        }
        if (rule != null && (NativeStaticCancellationReplacement.admissionTarget(rule) > 65535 || NativeStaticCancellationReplacement.admissionTarget(rule) <= 0)) {
            return;
        }
        if (!NativeLightAction.admitPair(clientSession, clientSession, 44, cancellationSpell.actionId(), cancellationSpell.reuseDelayMillis())) {
            return;
        }
        Target target = new Target(gameHub, staticNpc, clientSession);
        if (rule != null) {
            NativeStaticCancellationReplacement.apply(target, rule, (n, n2, n3, n4, n5, n6) -> null);
        }
        CombatAccess.call(clientSession, "cancelMeditation", "(Ljava/lang/String;)V", "spell-44");
        CombatAccess.set(clientSession, "playerMp", NativeSkillConsumption.boundMp(clientSession, (Integer)CombatAccess.field(clientSession, "playerMp") - n11));
        CombatAccess.call(clientSession, "commitSpellReady", "(IJ)V", 44, System.nanoTime() + (long)cancellationSpell.reuseDelayMillis() * 1000000L);
        CombatAccess.call(clientSession, "cancelAbsoluteBarrier", "(Ljava/lang/String;)V", "spell-44");
        List<ClientSession> list = gameHub.classicWorldMembers(staticNpc.mapId(), NativeStaticNpcScene.instance(clientSession));
        for (ClientSession clientSession2 : list) {
            if (!(!((Boolean)CombatAccess.call(clientSession, "sameClassicWorldMap", "(Llocal/lito381/server/ClientSession;)Z", clientSession2)).booleanValue() || !BattleSpellCatalog.isInNativeScreen((Integer)CombatAccess.field(clientSession2, "positionX"), (Integer)CombatAccess.field(clientSession2, "positionY"), NativeStaticNpcMotion.x(staticNpc, clientSession), NativeStaticNpcMotion.y(staticNpc, clientSession)))) {
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
                if (!(!(notice instanceof NativeCancellationNpcTransform.Shape))) {
                NativeCancellationNpcTransform.Shape shape = (NativeCancellationNpcTransform.Shape)notice;
                NativeCancellationNpcStatic.sendNpc(clientSession2, gameHub, staticNpc, Protocol.classicStaticNpcShape(shape.objectId(), shape.gfx(), 0), "S_POLYMORPH static-Cancellation");

                }
}

            }
}
        CombatAccess.call(clientSession, "sendInventoryDelta", "(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V", NativeCastMaterial.previous(clientSession), NativeCastMaterial.next(clientSession));
        NativeCancellationNpcStatic.send(clientSession, Protocol.manaPoint((Integer)CombatAccess.field(clientSession, "playerMp"), (Integer)CombatAccess.field(clientSession, "playerMaxMp")), "S_MANA_POINT static-Cancellation");
    }
    private static void sendViewer(ClientSession clientSession, byte[] byArray, String string) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        int n = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
        int n2 = NativeStaticNpcScene.instance(clientSession);
        NativeCancellationNpcStatic.publish(clientSession, gameHub, n, n2, NativePlayerCommit.Binding.capture(clientSession), byArray, string, () -> true);
    }
    private static void sendNpc(ClientSession clientSession, GameHub gameHub, ClassicWorldNpcCatalog.StaticNpc staticNpc, byte[] byArray, String string) {
        Object actor;
        int n = NativeStaticNpcScene.instance(clientSession);
        int n2 = NativeStaticNpcScene.gfx(gameHub, staticNpc, clientSession);
        Appearance appearance = NativeStaticNpcScene.appearance(gameHub, staticNpc, n);
        synchronized (gameHub) {
            actor = n > 0 ? NativeStaticNpcScene.actor(gameHub, staticNpc, n, false) : null;
        }
        NativeStaticNpcRespawn.publishNpc(clientSession, gameHub, staticNpc.mapId(), n, NativePlayerCommit.Binding.capture(clientSession), byArray, string, () -> ClassicWorldNpcCatalog.npc(staticNpc.mapId(), staticNpc.objectId()) == staticNpc && (n <= 0 || NativeStaticNpcScene.actor(gameHub, staticNpc, n, false) == actor) && NativeStaticNpcScene.gfx(gameHub, staticNpc, clientSession) == n2 && NativeStaticNpcScene.appearance(gameHub, staticNpc, n).equals(appearance));
    }
 private static void send(ClientSession pc,byte[] packet,String note)throws IOException{NativeCancellationProbability.send(pc,packet,note);}
 private NativeCancellationNpcStatic(){}
    static boolean nativeStaticReplacementOriginalRoute(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell) throws IOException {
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
        if (rule != null && NativeStaticCancellationReplacement.routeType(rule) != 1) {
            return false;
        }
        int n2 = NativeStaticNpcScene.instance(clientSession);
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        Object object = NativeStaticNpcScene.location(gameHub, clientSession);
        NativeLightAction.commitPair(clientSession, clientSession, () -> NativeWorldMaterialPolicy.run(clientSession, true, 44, () -> {
            synchronized (gameHub) {
                if (binding.matches(clientSession) && object != null && NativeStaticNpcScene.location(gameHub, clientSession) == object && NativeStaticNpcScene.inScene(clientSession, gameHub, n, n2) && ClassicWorldNpcCatalog.npc(n, staticNpc.objectId()) == staticNpc) {
                    NativeCancellationNpcStatic.cast(clientSession, gameHub, staticNpc, objectSpellRequest, cancellationSpell, rule);
                }
            }
        }));
        return true;
    }
    static void nativeStaticRespawnPublish(ClientSession clientSession, GameHub gameHub, int n, int n2, NativePlayerCommit.Binding binding, byte[] byArray, String string, BooleanSupplier booleanSupplier) {
        NativeCancellationNpcStatic.publish(clientSession, gameHub, n, n2, binding, byArray, string, booleanSupplier);
    }
    private static void publish(ClientSession clientSession, GameHub gameHub, int n, int n2, NativePlayerCommit.Binding binding, byte[] byArray, String string, BooleanSupplier booleanSupplier) {
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.deferScoped(() -> {
            Runnable[] runnableArray = new Runnable[1];
            NativeSourcePlayerActionLock.run(clientSession, () -> {
                if (binding.matches(clientSession)) {
                    runnableArray[0] = NativeRolePublication.completion(clientSession, () -> {
                        boolean bl;
                        synchronized (gameHub) {
                            bl = binding.matches(clientSession) && NativeStaticNpcScene.inScene(clientSession, gameHub, n, n2) && booleanSupplier.getAsBoolean();
                        }
                        if (bl) {
                            try {
                                clientSession.nativeSelfOriginalSend(byArray2, string);
                            }
                            catch (IOException iOException) {
                                throw new UncheckedIOException(iOException);
                            }
                        }
                    });
                }
            });
            if (runnableArray[0] != null) {
                runnableArray[0].run();
            }
        });
    }
}
