// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.BattleEraseMagicStatus;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClassicWorldMapCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatDatabase;
import local.lito381.server.CombatKiringku;
import local.lito381.server.CombatProbability;
import local.lito381.server.CombatRules;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationSleep;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeReuseActor;
import local.lito381.server.NativeReuseGate;
import local.lito381.server.NativeSkillConsumption;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.Protocol;
import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceSkillControls;

final class NativeCancellationProbability {
    private static final String P = "local/lito381/server/";

    NativeCancellationProbability() {
    }

    static int percentile() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }

    static int guardianMr(long l) {
        return l >= 10000L && l <= 19999L ? 3 : (l >= 20000L && l <= 29999L ? 6 : (l >= 30000L && l <= 39999L ? 9 : 0));
    }

    static boolean evaded(int n, int n2) {
        if (n2 < 1 || n2 > 1000) {
            throw new IllegalArgumentException("evasion roll");
        }
        return n >= n2;
    }

    static boolean succeeds(int n, int n2) {
        if (n < 0 || n > 100 || n2 < 1 || n2 > 100) {
            throw new IllegalArgumentException("probability range");
        }
        return n2 <= n;
    }

    static void prepare(ClientSession clientSession) throws IOException {
        if (!CombatRules.loaded()) {
            return;
        }
        try {
            NativeSourcePlayerActionLock.run(clientSession, () -> {
                try {
                    NativeCastMaterialPolicy.nativeOriginalGm(clientSession);
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            });
        }
        catch (UncheckedIOException uncheckedIOException) {
            throw uncheckedIOException.getCause();
        }
    }

    static boolean allied(ClientSession clientSession, ClientSession clientSession2) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        return clientSession == clientSession2 || gameHub.sameClan(clientSession.sourceCharacterIdentity(), clientSession2.sourceCharacterIdentity()) || gameHub.sameClassicParty(clientSession.sourceCharacterIdentity(), clientSession2.sourceCharacterIdentity());
    }

    static boolean safe(ClientSession clientSession) {
        int n = (Integer)CombatAccess.field(clientSession, "positionX");
        int n2 = (Integer)CombatAccess.field(clientSession, "positionY");
        if (String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD")) {
            return ClassicWorldMapCatalog.forMap((Integer)CombatAccess.field(clientSession, "classicWorldMapId")).isSafetyZone(n, n2);
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        ArenaMapCatalog.forMap(gameHub.battleMapId((Long)CombatAccess.field(clientSession, "currentRoomId"), clientSession));
        return false;
    }

    static int targetMr(ClientSession clientSession, long l) {
        return CombatAccess.integerCall(clientSession, "effectiveBaseMagicResistance", "(J)I", l) + NativeCancellationProbability.guardianMr(clientSession.characterLawful());
    }

    static int legacyChance(int n, int n2, int n3, int n4) {
        int n5 = n + (n2 << 1) + n3 - n4;
        return Math.max(1, Math.min(90, n5));
    }

    private static boolean cachedSourceGm(ClientSession clientSession) {
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(clientSession);
        if (nativeReuseActor == null || nativeReuseActor.materialPolicy == null || nativeReuseActor.materialPolicy.owner() != nativeReuseActor.owner || !nativeReuseActor.owner.matches(clientSession)) {
            throw new IllegalStateException("Source character GM policy must be prepared before probability admission");
        }
        return nativeReuseActor.materialPolicy.gm();
    }

    static int chance(ClientSession clientSession, ClientSession clientSession2, BattleSpellCatalog.CancellationSpell cancellationSpell, long l) {
        if (NativeCancellationProbability.allied(clientSession, clientSession2)) {
            return 100;
        }
        if (NativeCancellationProbability.safe(clientSession) || NativeCancellationProbability.safe(clientSession2)) {
            return 0;
        }
        if (NativeCancellationProbability.cachedSourceGm(clientSession2)) {
            return 1;
        }
        SourceSkillControls.Snapshot snapshot = CombatDatabase.controlsOrNull();
        if (snapshot != null && snapshot.probabilities().containsKey(44)) {
            return CombatProbability.dispatch(cancellationSpell, "unusedConfiguredCancellationFallback", "(I)I", "targetMr", new Object[]{NativeCancellationProbability.targetMr(clientSession2, l)}, clientSession, new Object[]{clientSession2});
        }
        CharacterSummary characterSummary = (CharacterSummary)CombatAccess.call(clientSession, "selectedCharacterSummary", "()Llocal/lito381/server/CharacterSummary;", new Object[0]);
        SourceEquipmentMagicStats sourceEquipmentMagicStats = (SourceEquipmentMagicStats)CombatAccess.call(clientSession, "requiredEquipmentMagicStats", "()Llocal/lito381/server/SourceEquipmentMagicStats;", new Object[0]);
        int n = sourceEquipmentMagicStats.effectiveIntelligence(CombatAccess.integerCall(clientSession, "baseIntelligence", "(J)I", l));
        int n2 = Math.max(CombatKiringku.magicBonus(n) + CombatRules.magicLevel(characterSummary.type(), characterSummary.level()) + (characterSummary.type() == 3 ? 1 : -1), 1);
        int n3 = CombatDatabase.value(3, 44, "CancellationSpell", "probabilityDice");
        int n4 = 0;
        if (n3 > 0) {
            for (int i = 0; i < n2; ++i) {
                n4 += ThreadLocalRandom.current().nextInt(1, n3 + 1);
            }
        }
        Object object = CombatAccess.call(CombatAccess.field(clientSession, "dragonEyeStatus"), "snapshot", "(J)Llocal/lito381/server/BattleDragonEyeStatus$Snapshot;", l);
        return NativeCancellationProbability.legacyChance(n4, CombatAccess.integerCall(clientSession, "originalMagicHit", "()I", new Object[0]), sourceEquipmentMagicStats.totalMagicHit() + CombatAccess.number(object, "magicHit"), NativeCancellationProbability.targetMr(clientSession2, l));
    }

    static boolean allow(ClientSession clientSession, ClientSession clientSession2, BattleSpellCatalog.CancellationSpell cancellationSpell, long l) {
        if (cancellationSpell.skillId() != 44 || clientSession2 == null) {
            throw new IllegalArgumentException("explicit player cancellation only");
        }
        if (!CombatRules.loaded()) {
            return true;
        }
        int n = NativeCancellationProbability.chance(clientSession, clientSession2, cancellationSpell, l);
        int n2 = n == 100 || n == 0 ? 0 : NativeCancellationProbability.percentile();
        boolean bl = n == 100 || n > 0 && NativeCancellationProbability.succeeds(n, n2);
        ((BattleEraseMagicStatus)CombatAccess.field(clientSession2, "eraseMagicStatus")).consume(l);
        NativeCancellationSleep.removeFog(clientSession2);
        System.out.println("[LiTo] Source Cancellation probability target=" + clientSession2.playerObjectId() + " chance=" + n + " roll=" + n2 + " success=" + bl);
        return bl;
    }

    static boolean gate(ClientSession clientSession, ClientSession clientSession3, BattleSpellCatalog.CancellationSpell cancellationSpell, long l, int n) throws IOException {
        long l2;
        if (NativeCancellationProbability.allow(clientSession, clientSession3, cancellationSpell, l)) {
            return true;
        }
        CombatAccess.call(clientSession, "cancelMeditation", "(Ljava/lang/String;)V", "spell-44");
        CombatAccess.set(clientSession, "playerMp", NativeSkillConsumption.boundMp(clientSession, (Integer)CombatAccess.field(clientSession, "playerMp") - n));
        CombatAccess.call(clientSession, "cancelAbsoluteBarrier", "(Ljava/lang/String;)V", "spell-44");
        CombatAccess.call(clientSession, "commitSpellReady", "(IJ)V", 44, l + (long)cancellationSpell.reuseDelayMillis() * 1000000L);
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        List<ClientSession> list = String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") ? ((List)CombatAccess.call(clientSession, "classicWorldMembers", "(I)Ljava/util/List;", (int)((Integer)CombatAccess.field(clientSession, "classicWorldMapId")))).stream().filter(clientSession2 -> (Boolean)CombatAccess.call(clientSession, "sameClassicWorldMap", "(Llocal/lito381/server/ClientSession;)Z", clientSession2)).filter(clientSession2 -> BattleSpellCatalog.isInNativeScreen((Integer)CombatAccess.field(clientSession2, "positionX"), (Integer)CombatAccess.field(clientSession2, "positionY"), (Integer)CombatAccess.field(clientSession, "positionX"), (Integer)CombatAccess.field(clientSession, "positionY"))).toList() : (gameHub.roomRules(l2 = ((Long)CombatAccess.field(clientSession, "currentRoomId")).longValue()).mode() == BattleRules.Mode.QUEST_BATTLE ? gameHub.battleRecipients(l2, gameHub.forceOf(l2, clientSession)) : gameHub.roomMembers(l2));
        for (ClientSession clientSession4 : list) {
            if (((Boolean)CombatAccess.field(clientSession, "invisible")).booleanValue() && clientSession4 != clientSession) continue;
            NativeCancellationProbability.send(clientSession4, Protocol.objectAction(clientSession.playerObjectId(), cancellationSpell.actionId()), "S_ACTION Cancellation probability-failed caster=" + clientSession.playerObjectId());
        }
        CombatAccess.call(clientSession, "sendInventoryDelta", "(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V", NativeCastMaterial.previous(clientSession), NativeCastMaterial.next(clientSession));
        NativeCancellationProbability.send(clientSession, Protocol.manaPoint((Integer)CombatAccess.field(clientSession, "playerMp"), (Integer)CombatAccess.field(clientSession, "playerMaxMp")), "S_MANA_POINT Cancellation probability-failed");
        NativeCancellationProbability.send(clientSession, Protocol.messageCode(cancellationSpell.failureMessage(), new String[0]), "S_MESSAGE_CODE Cancellation probability-failed");
        return false;
    }

    static void send(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        try {
            CombatAccess.call(clientSession, "sendEncrypted", "([BLjava/lang/String;)V", byArray, string);
        }
        catch (UncheckedIOException uncheckedIOException) {
            throw uncheckedIOException.getCause();
        }
    }
}
