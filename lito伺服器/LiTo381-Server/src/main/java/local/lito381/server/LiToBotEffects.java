// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.GameHub;

final class LiToBotEffects {
    private static final Map<Object, State> states = Collections.synchronizedMap(new WeakHashMap());
    private static final Field ROOMS;

    LiToBotEffects() {
    }

    static Object field(Object object, String string) {
        try {
            Field field = object.getClass().getDeclaredField(string);
            field.setAccessible(true);
            return field.get(object);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static State state(GameHub gameHub, long l) {
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            try {
                Object v = ((Map)ROOMS.get(gameHub)).get(l);
                if (v == null || !((Boolean)LiToBotEffects.field(v, "battleStarted")).booleanValue() || (Integer)LiToBotEffects.field(v, "botHp") <= 0) {
                    return null;
                }
                long l2 = ((Number)LiToBotEffects.field(v, "battleGeneration")).longValue();
                Map<Object, State> map = states;
                synchronized (map) {
                    State state = states.get(v);
                    if (state == null || state.generation != l2) {
                        state = new State(l2);
                        states.put(v, state);
                    }
                    return state;
                }
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new IllegalStateException(illegalAccessException);
            }
        }
    }

    static boolean eligible(GameHub gameHub, long l) {
        BattleRules.Mode mode = gameHub.roomRules(l).mode();
        return gameHub.roomCombatantCount(l) == 1 && mode != BattleRules.Mode.PET_RACE && mode != BattleRules.Mode.QUEST_BATTLE && mode != BattleRules.Mode.TIME_ATTACK && LiToBotEffects.state(gameHub, l) != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean apply(GameHub gameHub, long l, int n, long l2, int n2) {
        State state = LiToBotEffects.state(gameHub, l);
        if (state == null) {
            return false;
        }
        long l3 = Math.addExact(l2, Math.multiplyExact((long)n2, 1000000000L));
        State state2 = state;
        synchronized (state2) {
            if (n == 47) {
                state.weakness = l3;
            } else if (n == 56) {
                state.disease = l3;
            } else {
                state.blind = l3;
            }
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean blind(GameHub gameHub, long l, long l2) {
        State state = LiToBotEffects.state(gameHub, l);
        if (state == null) {
            return false;
        }
        State state2 = state;
        synchronized (state2) {
            return l2 < state.blind;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int damageModifier(GameHub gameHub, long l, long l2) {
        State state = LiToBotEffects.state(gameHub, l);
        if (state == null) {
            return 0;
        }
        State state2 = state;
        synchronized (state2) {
            return (l2 < state.weakness ? BattleSpellCatalog.weaknessSpell(47).meleeDamageModifier() : 0) + (l2 < state.disease ? BattleSpellCatalog.weaknessSpell(56).meleeDamageModifier() : 0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int mp(GameHub gameHub, long l) {
        State state = LiToBotEffects.state(gameHub, l);
        if (state == null) {
            return 0;
        }
        State state2 = state;
        synchronized (state2) {
            return state.mp;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int drain(GameHub gameHub, long l, int n) {
        State state = LiToBotEffects.state(gameHub, l);
        if (state == null) {
            return 0;
        }
        State state2 = state;
        synchronized (state2) {
            int n2 = Math.min(state.mp, Math.max(0, n));
            state.mp -= n2;
            return n2;
        }
    }

    static {
        try {
            ROOMS = GameHub.class.getDeclaredField("rooms");
            ROOMS.setAccessible(true);
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static final class State {
        long generation;
        long blind;
        long weakness;
        long disease;
        int mp = 100;

        State(long l) {
            this.generation = l;
        }
    }
}
