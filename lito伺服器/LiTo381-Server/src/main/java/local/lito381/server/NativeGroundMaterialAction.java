// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.BattleGroundEffects;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClassicWorldGroundEffects;
import local.lito381.server.ClassicWorldMapCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativeReuseLog;
import local.lito381.server.Protocol;

final class NativeGroundMaterialAction {
    static void battle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.GroundSpell groundSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialGroundBody(byArray, groundSpell));
    }

    static void world(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.GroundSpell groundSpell) throws IOException {
        NativeLightAction.commitSelf(clientSession, () -> clientSession.nativeMaterialWorldGroundBody(byArray, groundSpell));
    }

    static void admit(ClientSession clientSession, BattleSpellCatalog.GroundSpell groundSpell) {
        if (groundSpell.skillId() != 58 && groundSpell.skillId() != 63) {
            throw new IllegalStateException("unreviewed ground skill");
        }
        if (!NativeLightAction.admitSelf(clientSession, groundSpell.skillId(), groundSpell.actionId(), groundSpell.reuseDelayMillis())) {
            throw new Rejected();
        }
    }

    static boolean prepare(ClientSession clientSession, BattleSpellCatalog.GroundSpell groundSpell) {
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("ground requires player action lock");
        }
        try {
            if (groundSpell.itemConsumeId() != 0L) {
                NativeCastMaterialPolicy.gm(clientSession);
            }
            return true;
        }
        catch (IOException iOException) {
            NativeReuseLog.getLogger(NativeGroundMaterialAction.class).error("ground material policy read failed", iOException);
            NativeLightAction.materialOutput(clientSession, Protocol.messageCode(299, new String[0]), "S_MESSAGE_CODE ground-material-policy=299");
            return false;
        }
    }

    static BattleGroundEffects.CastResult placeBattle(GameHub gameHub, long l, ClientSession clientSession, BattleSpellCatalog.GroundSpell groundSpell, int n, int n2, int n3, int n4, long l2, Runnable runnable) {
        if (!NativeGroundMaterialAction.prepare(clientSession, groundSpell)) {
            return BattleGroundEffects.CastResult.rejected();
        }
        return gameHub.nativeMaterialPlaceGroundSpell(l, clientSession, groundSpell, n, n2, n3, n4, System.nanoTime(), runnable);
    }

    static ClassicWorldGroundEffects.CastResult placeWorld(GameHub gameHub, int n, int n2, ClientSession clientSession, BattleSpellCatalog.GroundSpell groundSpell, int n3, int n4, int n5, int n6, long l, Runnable runnable) {
        if (!NativeGroundMaterialAction.prepare(clientSession, groundSpell)) {
            return ClassicWorldGroundEffects.CastResult.rejected();
        }
        return gameHub.nativeMaterialPlaceWorldGroundSpell(n, n2, clientSession, groundSpell, n3, n4, n5, n6, System.nanoTime(), runnable);
    }

    static List<Object> leases(Object object) {
        return (List)CombatAccess.field(object, "leases");
    }

    static BattleGroundEffects battleCopy(BattleGroundEffects battleGroundEffects) {
        BattleGroundEffects battleGroundEffects2 = new BattleGroundEffects();
        NativeGroundMaterialAction.leases(battleGroundEffects2).addAll(NativeGroundMaterialAction.leases(battleGroundEffects));
        return battleGroundEffects2;
    }

    static ClassicWorldGroundEffects worldCopy(ClassicWorldGroundEffects classicWorldGroundEffects) {
        ClassicWorldGroundEffects classicWorldGroundEffects2 = new ClassicWorldGroundEffects();
        NativeGroundMaterialAction.leases(classicWorldGroundEffects2).addAll(NativeGroundMaterialAction.leases(classicWorldGroundEffects));
        return classicWorldGroundEffects2;
    }

    static void install(Object object, Object object2) {
        List<Object> list = NativeGroundMaterialAction.leases(object);
        list.clear();
        list.addAll(NativeGroundMaterialAction.leases(object2));
    }

    static BattleGroundEffects.CastResult castBattle(BattleGroundEffects battleGroundEffects, BattleSpellCatalog.GroundSpell groundSpell, long l, int n, int n2, int n3, int n4, ArenaMapCatalog.Arena arena, long l2, GameHub gameHub, Runnable runnable) {
        if (!Thread.holdsLock(gameHub)) {
            throw new IllegalStateException("ground placement requires original hub monitor");
        }
        BattleGroundEffects battleGroundEffects2 = NativeGroundMaterialAction.battleCopy(battleGroundEffects);
        BattleGroundEffects.CastResult castResult = battleGroundEffects2.cast(groundSpell, l, n, n2, n3, n4, arena, System.nanoTime());
        if (!castResult.accepted()) {
            return castResult;
        }
        try {
            runnable.run();
        }
        catch (Rejected rejected) {
            return BattleGroundEffects.CastResult.rejected();
        }
        battleGroundEffects2 = NativeGroundMaterialAction.battleCopy(battleGroundEffects);
        BattleGroundEffects.CastResult castResult2 = battleGroundEffects2.cast(groundSpell, l, n, n2, n3, n4, arena, System.nanoTime());
        if (!castResult2.accepted()) {
            throw new IllegalStateException("ground eligibility changed under hub monitor");
        }
        NativeGroundMaterialAction.install(battleGroundEffects, battleGroundEffects2);
        return castResult2;
    }

    static ClassicWorldGroundEffects.CastResult castWorld(ClassicWorldGroundEffects classicWorldGroundEffects, BattleSpellCatalog.GroundSpell groundSpell, long l, int n, int n2, int n3, int n4, ClassicWorldMapCatalog.ClassicMap classicMap, long l2, GameHub gameHub, Object object, Runnable runnable) {
        if (!Thread.holdsLock(gameHub)) {
            throw new IllegalStateException("world ground placement requires original hub monitor");
        }
        ClassicWorldGroundEffects classicWorldGroundEffects2 = NativeGroundMaterialAction.worldCopy(classicWorldGroundEffects);
        ClassicWorldGroundEffects.CastResult castResult = classicWorldGroundEffects2.cast(groundSpell, l, n, n2, n3, n4, classicMap, System.nanoTime());
        if (!castResult.accepted()) {
            NativeGroundMaterialAction.install(classicWorldGroundEffects, classicWorldGroundEffects2);
            NativeGroundMaterialAction.removeEmptyWorld(gameHub, object, classicWorldGroundEffects);
            return castResult;
        }
        try {
            runnable.run();
        }
        catch (Rejected rejected) {
            NativeGroundMaterialAction.removeEmptyWorld(gameHub, object, classicWorldGroundEffects);
            return ClassicWorldGroundEffects.CastResult.rejected();
        }
        classicWorldGroundEffects2 = NativeGroundMaterialAction.worldCopy(classicWorldGroundEffects);
        ClassicWorldGroundEffects.CastResult castResult2 = classicWorldGroundEffects2.cast(groundSpell, l, n, n2, n3, n4, classicMap, System.nanoTime());
        if (!castResult2.accepted()) {
            throw new IllegalStateException("world ground eligibility changed under hub monitor");
        }
        NativeGroundMaterialAction.install(classicWorldGroundEffects, classicWorldGroundEffects2);
        return castResult2;
    }

    static void removeEmptyWorld(GameHub gameHub, Object object, ClassicWorldGroundEffects classicWorldGroundEffects) {
        if (NativeGroundMaterialAction.leases(classicWorldGroundEffects).isEmpty()) {
            ((Map)CombatAccess.field(gameHub, "classicWorldGroundEffects")).remove(object, classicWorldGroundEffects);
        }
    }

    private NativeGroundMaterialAction() {
    }

    static final class Rejected
    extends RuntimeException {
        Rejected() {
            super(null, null, false, false);
        }
    }
}
