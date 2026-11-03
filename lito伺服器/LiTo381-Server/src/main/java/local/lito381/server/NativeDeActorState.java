// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativeDeProfileKernel;
import local.lito381.server.NativePlayerCommit;

final class NativeDeActorState {
    static final String FIELD = "sourceDeActorState";
    private final Object life;
    private final Object state = new Object();
    private Object revision = new Object();
    private int aiType;
    private NativeDeProfileKernel.Combat combat = NativeDeProfileKernel.Combat.NONE;
    private boolean shop;
    private boolean fishing;
    private final Map<NativePlayerCommit.Binding, Integer> hate = new LinkedHashMap<NativePlayerCommit.Binding, Integer>();
    private Object sourceAiTimeline;
    private Object sourceEquipment;

    private NativeDeActorState(Object object) {
        this.life = object;
    }

    private static NativeDeActorState owned(BattleTimedStatus battleTimedStatus) {
        if (!Thread.holdsLock(battleTimedStatus)) {
            throw new IllegalStateException("De actor state requires status monitor");
        }
        Object object = NativeCombatAttacker.token(battleTimedStatus, true);
        NativeDeActorState nativeDeActorState = (NativeDeActorState)CombatAccess.field(battleTimedStatus, FIELD);
        if (nativeDeActorState == null || nativeDeActorState.life != object) {
            nativeDeActorState = new NativeDeActorState(object);
            CombatAccess.set(battleTimedStatus, FIELD, nativeDeActorState);
        }
        return nativeDeActorState;
    }

    private Lease lease() {
        return new Lease(this.life, this.state, this.revision);
    }

    private boolean matches(Lease lease) {
        return lease != null && lease.life() == this.life && lease.state() == this.state && lease.profile() == this.revision;
    }

    private Profile profile() {
        return new Profile(this.aiType, this.combat, this.shop, this.fishing);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease acquire(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            return NativeDeActorState.owned(battleTimedStatus).lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot snapshot(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeActorState nativeDeActorState = NativeDeActorState.owned(battleTimedStatus);
            return new Snapshot(nativeDeActorState.lease(), nativeDeActorState.profile(), nativeDeActorState.hate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease type(BattleTimedStatus battleTimedStatus, Lease lease, int n) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeActorState nativeDeActorState = NativeDeActorState.owned(battleTimedStatus);
            if (!nativeDeActorState.matches(lease)) {
                return null;
            }
            nativeDeActorState.aiType = n;
            nativeDeActorState.revision = new Object();
            return nativeDeActorState.lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease behavior(BattleTimedStatus battleTimedStatus, Lease lease, NativeDeProfileKernel.Combat combat) {
        Objects.requireNonNull(combat);
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeActorState nativeDeActorState = NativeDeActorState.owned(battleTimedStatus);
            if (!nativeDeActorState.matches(lease)) {
                return null;
            }
            nativeDeActorState.combat = combat;
            nativeDeActorState.revision = new Object();
            return nativeDeActorState.lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Transition initialize(BattleTimedStatus battleTimedStatus, Lease lease) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            final NativeDeActorState nativeDeActorState = NativeDeActorState.owned(battleTimedStatus);
            if (!nativeDeActorState.matches(lease)) {
                return null;
            }
            nativeDeActorState.revision = new Object();
            final ArrayList<Effect> arrayList = new ArrayList<Effect>();
            NativeDeProfileKernel.initialize(new NativeDeProfileKernel.Target(){

                @Override
                public int aiType() {
                    return nativeDeActorState.aiType;
                }

                @Override
                public boolean shop() {
                    return nativeDeActorState.shop;
                }

                @Override
                public void createShop() {
                    nativeDeActorState.shop = true;
                }

                @Override
                public void startShop() {
                    arrayList.add(Effect.START_SHOP);
                }

                @Override
                public boolean fishing() {
                    return nativeDeActorState.fishing;
                }

                @Override
                public void createFishing() {
                    nativeDeActorState.fishing = true;
                }

                @Override
                public boolean combat() {
                    return nativeDeActorState.combat != NativeDeProfileKernel.Combat.NONE;
                }

                @Override
                public void createCombat(NativeDeProfileKernel.Combat combat) {
                    nativeDeActorState.combat = combat;
                }

                @Override
                public void startAi() {
                    arrayList.add(Effect.START_AI);
                }

                @Override
                public void error(Exception exception) {
                    arrayList.add(Effect.PROFILE_ERROR);
                }
            });
            return new Transition(nativeDeActorState.lease(), nativeDeActorState.profile(), arrayList);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean firstHate(BattleTimedStatus battleTimedStatus, Lease lease, NativePlayerCommit.Binding binding, int n) {
        if (binding == null) {
            return false;
        }
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            NativeDeActorState nativeDeActorState = NativeDeActorState.owned(battleTimedStatus);
            if (!nativeDeActorState.matches(lease) || nativeDeActorState.hate.containsKey(binding)) {
                return false;
            }
            nativeDeActorState.hate.put(binding, n);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean current(BattleTimedStatus battleTimedStatus, Lease lease) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            return NativeDeActorState.owned(battleTimedStatus).matches(lease);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clear(BattleTimedStatus battleTimedStatus) {
        NativeDeAiTimers.retireStatus(battleTimedStatus);
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            CombatAccess.set(battleTimedStatus, FIELD, null);
        }
    }

    record Lease(Object life, Object state, Object profile) {
    }

    record Profile(int aiType, NativeDeProfileKernel.Combat combat, boolean shop, boolean fishing) {
    }

    record Snapshot(Lease lease, Profile profile, Map<NativePlayerCommit.Binding, Integer> hate) {
        Snapshot {
            map = Map.copyOf(map);
        }
    }

    record Transition(Lease lease, Profile profile, List<Effect> effects) {
        Transition {
            list = List.copyOf(list);
        }
    }

    static enum Effect {
        START_AI,
        START_SHOP,
        PROFILE_ERROR;

    }
}
