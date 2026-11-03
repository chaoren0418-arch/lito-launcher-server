// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

final class NativeProcHealingKernel {
    private NativeProcHealingKernel() {
    }

    static boolean isHealSkill(int n, IntUnaryOperator intUnaryOperator) {
        return switch (n) {
            case 1, 19, 35, 49, 57, 158, 164 -> true;
            default -> intUnaryOperator.applyAsInt(n) == 16;
        };
    }

    static int calculate(int n, int n2, int n3, int n4, int n5, IntUnaryOperator intUnaryOperator) {
        int n6 = 0;
        int n7 = n2 + n3;
        for (int i = 0; i < n7; ++i) {
            n6 += intUnaryOperator.applyAsInt(n) + 1;
        }
        double d = 1.0;
        if (n4 > 0) {
            d += (double)n4 / 32768.0;
        }
        n6 = (int)((double)n6 * d);
        return (int)((double)n6 * ((double)n5 / 10.0));
    }

    static void apply(Target target, IntSupplier intSupplier, int n, Runnable runnable) {
        if (target == null || target.dead()) {
            return;
        }
        int n2 = intSupplier.getAsInt();
        if (target.kind() == Kind.MONSTER) {
            int n3 = target.undead();
            if (n3 == 3) {
                NativeProcHealingKernel.graphics(target, runnable);
                return;
            }
            if (n3 == 1) {
                target.commit(n2);
                NativeProcHealingKernel.graphics(target, runnable);
                return;
            }
        }
        if (target.hasEffect(170)) {
            n2 <<= 1;
        }
        if (target.hasEffect(173)) {
            n2 >>= 1;
        }
        if (target.hasEffect(4012)) {
            n2 >>= 1;
        }
        if (target.hasEffect(164)) {
            n2 += n;
        }
        if (target.kind() == Kind.PLAYER) {
            target.removeEffect(170);
        }
        if (target.kind() != Kind.TOWER && target.kind() != Kind.DOOR && n2 > 0) {
            target.commit(-n2);
        }
        NativeProcHealingKernel.graphics(target, runnable);
    }

    private static void graphics(Target target, Runnable runnable) {
        if (!target.dead()) {
            runnable.run();
        }
    }

    static interface Target {
        public boolean dead();

        public Kind kind();

        public int undead();

        public boolean hasEffect(int var1);

        public void removeEffect(int var1);

        public void commit(int var1);
    }

    static enum Kind {
        PLAYER,
        MONSTER,
        NPC,
        PET,
        SUMMON,
        TOWER,
        DOOR;

    }
}
