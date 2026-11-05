// DECOMPILED from base-0.6.71.jar, TODO review
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
        NativeDeActorState s = (NativeDeActorState)CombatAccess.field(battleTimedStatus, FIELD);
        if (s == null || s.life != object) {
            s = new NativeDeActorState(object);
            CombatAccess.set(battleTimedStatus, FIELD, s);
        }
        return s;
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
        synchronized (battleTimedStatus) {
            return NativeDeActorState.owned(battleTimedStatus).lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot snapshot(BattleTimedStatus battleTimedStatus) {
        synchronized (battleTimedStatus) {
            NativeDeActorState s = NativeDeActorState.owned(battleTimedStatus);
            return new Snapshot(s.lease(), s.profile(), s.hate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease type(BattleTimedStatus battleTimedStatus, Lease lease, int n) {
        synchronized (battleTimedStatus) {
            NativeDeActorState s = NativeDeActorState.owned(battleTimedStatus);
            if (!s.matches(lease)) {
                return null;
            }
            s.aiType = n;
            s.revision = new Object();
            return s.lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease behavior(BattleTimedStatus battleTimedStatus, Lease lease, NativeDeProfileKernel.Combat combat) {
        Objects.requireNonNull(combat);
        synchronized (battleTimedStatus) {
            NativeDeActorState s = NativeDeActorState.owned(battleTimedStatus);
            if (!s.matches(lease)) {
                return null;
            }
            s.combat = combat;
            s.revision = new Object();
            return s.lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Transition initialize(BattleTimedStatus battleTimedStatus, Lease lease) {
        synchronized (battleTimedStatus) {
            final NativeDeActorState s = NativeDeActorState.owned(battleTimedStatus);
            if (!s.matches(lease)) {
                return null;
            }
            s.revision = new Object();
            final ArrayList<Effect> effects = new ArrayList<Effect>();
            NativeDeProfileKernel.initialize(new NativeDeProfileKernel.Target(){

                @Override
                public int aiType() {
                    return s.aiType;
                }

                @Override
                public boolean shop() {
                    return s.shop;
                }

                @Override
                public void createShop() {
                    s.shop = true;
                }

                @Override
                public void startShop() {
                    effects.add(Effect.START_SHOP);
                }

                @Override
                public boolean fishing() {
                    return s.fishing;
                }

                @Override
                public void createFishing() {
                    s.fishing = true;
                }

                @Override
                public boolean combat() {
                    return s.combat != NativeDeProfileKernel.Combat.NONE;
                }

                @Override
                public void createCombat(NativeDeProfileKernel.Combat combat) {
                    s.combat = combat;
                }

                @Override
                public void startAi() {
                    effects.add(Effect.START_AI);
                }

                @Override
                public void error(Exception exception) {
                    effects.add(Effect.PROFILE_ERROR);
                }
            });
            return new Transition(s.lease(), s.profile(), effects);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean firstHate(BattleTimedStatus battleTimedStatus, Lease lease, NativePlayerCommit.Binding binding, int n) {
        if (binding == null) {
            return false;
        }
        synchronized (battleTimedStatus) {
            NativeDeActorState s = NativeDeActorState.owned(battleTimedStatus);
            if (!s.matches(lease) || s.hate.containsKey(binding)) {
                return false;
            }
            s.hate.put(binding, n);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean current(BattleTimedStatus battleTimedStatus, Lease lease) {
        synchronized (battleTimedStatus) {
            return NativeDeActorState.owned(battleTimedStatus).matches(lease);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clear(BattleTimedStatus battleTimedStatus) {
        NativeDeAiTimers.retireStatus(battleTimedStatus);
        synchronized (battleTimedStatus) {
            CombatAccess.set(battleTimedStatus, FIELD, null);
        }
    }

    record Lease(Object life, Object state, Object profile) {
    }

    record Profile(int aiType, NativeDeProfileKernel.Combat combat, boolean shop, boolean fishing) {
    }

    record Snapshot(Lease lease, Profile profile, Map<NativePlayerCommit.Binding, Integer> hate) {
        Snapshot {
            hate = Map.copyOf(hate);
        }
    }

    record Transition(Lease lease, Profile profile, List<Effect> effects) {Transition {
            effects = List.copyOf(effects);
        }
    }

    static enum Effect {
        START_AI,
        START_SHOP,
        PROFILE_ERROR;

    }
}
