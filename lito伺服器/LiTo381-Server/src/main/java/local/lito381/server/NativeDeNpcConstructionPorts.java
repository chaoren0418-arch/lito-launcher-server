// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeNpcTemplateState;

final class NativeDeNpcConstructionPorts {
    NativeDeNpcConstructionPorts() {
    }

    static abstract class Party {
        Party() {
        }

        public abstract boolean isMember(NativeDeDeletionPorts.Viewer var1);
    }

    static interface Movement {
        public void step(NativeDeNpcTemplateState var1, int var2);

        public int direction(NativeDeNpcTemplateState var1, int var2, int var3);

        public int reverse(NativeDeNpcTemplateState var1, int var2, int var3);

        public int obstacle(NativeDeNpcTemplateState var1, int var2);

        public int door(NativeDeNpcTemplateState var1, int var2);
    }

    static interface Services {
        public Movement movement();

        public void removeInventoryObject(NativeDeDeletionPorts.ObjectRef var1);
    }
}
