// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NumericBreakthrough;

record SourceEquipmentVitalStats(int itemMaximumHp, int setMaximumHp, int itemMaximumMp, int setMaximumMp, int itemCharisma, int setCharisma, int itemPotionRecovery, int setPotionRecovery) {
    static SourceEquipmentVitalStats none() {
        return new SourceEquipmentVitalStats(0, 0, 0, 0, 0, 0, 0, 0);
    }

    int totalMaximumHp() {
        return NumericBreakthrough.clampExtendedValue((long)this.itemMaximumHp + (long)this.setMaximumHp);
    }

    int totalMaximumMp() {
        return NumericBreakthrough.clampExtendedValue((long)this.itemMaximumMp + (long)this.setMaximumMp);
    }

    int effectiveCharisma(int baseCharisma) {
        return NumericBreakthrough.clampAbility((long)baseCharisma + (long)this.itemCharisma + (long)this.setCharisma);
    }

    int totalPotionRecovery() {
        return Math.addExact(this.itemPotionRecovery, this.setPotionRecovery);
    }
}
