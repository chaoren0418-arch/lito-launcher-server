// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;

final class NativePolymorphWeights {
    static final int DENOMINATOR = 1000000;

    static int select(List<Row> list, int n) {
        if (n < 1 || n > 1000000) {
            throw new IllegalArgumentException("polymorph roll outside 1..1000000");
        }
        int n2 = 0;
        for (Row row : list) {
            if (n > (n2 += row.chance())) continue;
            return row.polyId();
        }
        return 29;
    }

    private NativePolymorphWeights() {
    }

    record Row(int polyId, int chance) {
        Row {
            if (n2 <= 0) {
                throw new IllegalArgumentException("SQL filters non-positive chance");
            }
        }
    }
}
