// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceEquipmentVitalStats;

record SourceEquipmentEvaluation(SourceEquipmentMagicStats magic, SourceEquipmentVitalStats vital) {
    SourceEquipmentEvaluation {
        if (magic == null || vital == null) {
            throw new IllegalArgumentException("incomplete source equipment evaluation");
        }
    }

    static SourceEquipmentEvaluation none() {
        return new SourceEquipmentEvaluation(SourceEquipmentMagicStats.none(), SourceEquipmentVitalStats.none());
    }
}
