// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class NativeSourceSkillCastAdmission {
    private NativeSourceSkillCastAdmission() {
    }

    static Result admit(BooleanSupplier booleanSupplier, Supplier<? extends Reservation> supplier, BooleanSupplier booleanSupplier2) {
        Objects.requireNonNull(booleanSupplier, "preflight");
        Objects.requireNonNull(supplier, "reservationFactory");
        Objects.requireNonNull(booleanSupplier2, "materialCommit");
        if (!booleanSupplier.getAsBoolean()) {
            return Result.PREFLIGHT_REJECTED;
        }
        Reservation reservation = supplier.get();
        if (reservation == null) {
            return Result.RESERVATION_REJECTED;
        }
        try {
            if (!booleanSupplier2.getAsBoolean()) {
                reservation.cancel();
                return Result.MATERIAL_REJECTED;
            }
            reservation.commit();
            return Result.ACCEPTED;
        }
        catch (Error | RuntimeException throwable) {
            reservation.cancel();
            throw throwable;
        }
    }

    static enum Result {
        PREFLIGHT_REJECTED,
        RESERVATION_REJECTED,
        MATERIAL_REJECTED,
        ACCEPTED;

    }

    static interface Reservation {
        public void commit();

        public void cancel();
    }
}
