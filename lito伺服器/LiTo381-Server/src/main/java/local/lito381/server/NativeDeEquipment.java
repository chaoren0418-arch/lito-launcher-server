// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeDeActorState;
import local.lito381.server.NativeDeAiState;
import local.lito381.server.NativeDeAiTimers;
import local.lito381.server.NativeDeSpriteCatalog;
import local.lito381.server.NativeDeSprites;
import local.lito381.server.NativeDeWeaponKernel;

final class NativeDeEquipment {
    static final String FIELD = "sourceEquipment";

    private NativeDeEquipment() {
    }

    private static Object root(BattleTimedStatus battleTimedStatus) {
        if (!Thread.holdsLock(battleTimedStatus)) {
            throw new IllegalStateException("equipment requires native status monitor");
        }
        NativeDeActorState.acquire(battleTimedStatus);
        return CombatAccess.field(battleTimedStatus, "sourceDeActorState");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Lease bind(BattleTimedStatus battleTimedStatus, NativeDeActorState.Lease lease, Values values) {
        Objects.requireNonNull(values);
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            if (!NativeDeActorState.current(battleTimedStatus, lease)) {
                return null;
            }
            Holder holder = new Holder(lease, values);
            CombatAccess.set(NativeDeEquipment.root(battleTimedStatus), FIELD, holder);
            return holder.lease();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static View snapshot(BattleTimedStatus battleTimedStatus) {
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            Holder holder = (Holder)CombatAccess.field(NativeDeEquipment.root(battleTimedStatus), FIELD);
            return holder == null ? null : new View(holder.lease(), holder.values());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static View perform(BattleTimedStatus battleTimedStatus, Lease lease, Request request) {
        Objects.requireNonNull(request);
        BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
        synchronized (battleTimedStatus2) {
            Holder holder = (Holder)CombatAccess.field(NativeDeEquipment.root(battleTimedStatus), FIELD);
            if (holder == null || !holder.matches(lease)) {
                return null;
            }
            switch (request.op()) {
                case APPLY_WEAPON: {
                    NativeDeWeaponKernel.apply(holder, request.value());
                    break;
                }
                case REFRESH_OWN_SPEED: {
                    NativeDeSpriteCatalog.Catalog catalog = NativeDeSprites.catalog();
                    holder.set(Op.ATTACK, catalog.attack(holder.tempGfx, holder.weapon + 1));
                    holder.set(Op.MOVE, catalog.move(holder.tempGfx, holder.weapon));
                    break;
                }
                default: {
                    holder.set(request.op(), request.value());
                }
            }
            return new View(holder.lease(), holder.values());
        }
    }

    static View perform(GameHub gameHub, NativeCombatAttacker.Ref ref, Object object, NativeDeAiState.Lease lease, Lease lease2, Request request) {
        return NativeDeAiTimers.withTarget(gameHub, ref, object, lease, live -> NativeDeEquipment.perform(live.status(), lease2, request));
    }

    private static final class Holder
    implements NativeDeWeaponKernel.Target {
        final Object life;
        final Object root;
        final Object equipment = new Object();
        final Template template;
        int tempGfx;
        int gfx;
        int weapon;
        int status;
        int rawRange;
        int rawBow;
        int attack;
        int move;

        Holder(NativeDeActorState.Lease lease, Values values) {
            this.life = lease.life();
            this.root = lease.state();
            this.template = values.template();
            this.tempGfx = values.tempGfx();
            this.gfx = values.gfx();
            this.weapon = values.weapon();
            this.status = values.status();
            this.rawRange = values.rawRange();
            this.rawBow = values.rawBow();
            this.attack = values.attack();
            this.move = values.move();
        }

        Lease lease() {
            return new Lease(this.life, this.root, this.equipment);
        }

        boolean matches(Lease lease) {
            return lease != null && this.life == lease.life() && this.root == lease.root() && this.equipment == lease.equipment();
        }

        Values values() {
            return new Values(this.tempGfx, this.gfx, this.weapon, this.status, this.rawRange, this.rawBow, this.attack, this.move, this.template);
        }

        void set(Op op, int n) {
            switch (op) {
                case TEMP_GFX: {
                    this.tempGfx = n;
                    break;
                }
                case GFX: {
                    this.gfx = n;
                    break;
                }
                case WEAPON: {
                    this.weapon = n;
                    break;
                }
                case STATUS: {
                    this.status = n;
                    break;
                }
                case RANGE: {
                    this.rawRange = n;
                    break;
                }
                case BOW: {
                    this.rawBow = n;
                    break;
                }
                case ATTACK: {
                    this.attack = n;
                    break;
                }
                case MOVE: {
                    this.move = n;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("primitive equipment field required");
                }
            }
        }

        @Override
        public void weapon(int n) {
            this.set(Op.WEAPON, n);
        }

        @Override
        public void range(int n) {
            this.set(Op.RANGE, n);
        }

        @Override
        public void bow(int n) {
            this.set(Op.BOW, n);
        }

        @Override
        public void status(int n) {
            this.set(Op.STATUS, n);
        }
    }

    record Values(int tempGfx, int gfx, int weapon, int status, int rawRange, int rawBow, int attack, int move, Template template) {
        Values {
            Objects.requireNonNull(template);
        }

        int range() {
            return this.rawRange == -1 ? this.template.range() : this.rawRange;
        }

        int bow() {
            return this.rawBow == -1 ? this.template.bow() : this.rawBow;
        }

        int attackRange() {
            int n = this.range();
            return n <= 0 ? 1 : n;
        }
    }

    record Lease(Object life, Object root, Object equipment) {
    }

    record View(Lease lease, Values values) {
    }

    record Request(Op op, int value) {
        Request {
            Objects.requireNonNull(op);
        }
    }

    static enum Op {
        TEMP_GFX,
        GFX,
        WEAPON,
        STATUS,
        RANGE,
        BOW,
        ATTACK,
        MOVE,
        APPLY_WEAPON,
        REFRESH_OWN_SPEED;

    }

    record Template(int range, int bow) {
    }
}
