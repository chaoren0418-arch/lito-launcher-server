// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.function.BooleanSupplier;

final class NativeDeWeaponKernel {
    private NativeDeWeaponKernel() {
    }

    static void apply(Target target, int n) {
        target.weapon(n);
        target.range(1);
        target.bow(0);
        switch (n) {
            case 1: {
                target.status(4);
                break;
            }
            case 2: {
                target.status(46);
                break;
            }
            case 3: {
                target.status(50);
                break;
            }
            case 4: 
            case 13: {
                target.status(20);
                target.range(10);
                target.bow(66);
                break;
            }
            case 5: 
            case 14: {
                target.status(24);
                target.range(2);
                break;
            }
            case 6: 
            case 15: {
                target.status(11);
                break;
            }
            case 11: {
                target.status(58);
                break;
            }
            case 12: {
                target.status(54);
                break;
            }
            case 7: 
            case 16: {
                target.status(40);
                break;
            }
            case 17: {
                target.status(58);
                target.range(2);
                break;
            }
            case 18: {
                target.status(24);
                target.range(2);
                break;
            }
            case 8: 
            case 10: {
                target.status(62);
                target.range(2);
                break;
            }
            default: {
                target.status(0);
            }
        }
    }

    static int random(int n, BooleanSupplier booleanSupplier) {
        return switch (n) {
            case 0 -> {
                if (booleanSupplier.getAsBoolean()) {
                    yield 1;
                }
                yield 4;
            }
            case 1 -> {
                if (booleanSupplier.getAsBoolean()) {
                    yield 1;
                }
                yield 3;
            }
            case 2 -> 4;
            case 3 -> 7;
            case 4 -> {
                if (booleanSupplier.getAsBoolean()) {
                    yield 12;
                }
                yield 11;
            }
            case 5 -> 18;
            case 6 -> 17;
            default -> 1;
        };
    }

    static interface Target {
        public void weapon(int var1);

        public void range(int var1);

        public void bow(int var1);

        public void status(int var1);
    }
}
