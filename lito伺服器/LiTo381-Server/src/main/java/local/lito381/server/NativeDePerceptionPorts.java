// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDePerceptionPacket;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDePerceptionState;

final class NativeDePerceptionPorts {
    NativeDePerceptionPorts() {
    }

    static final class Emblems {
        Emblems() {
        }

        static Emblems get() {
            return new Emblems();
        }

        NativeDePerceptionEmblemData get(int n) {
            return NativeDePerceptionScope.actor().perception.emblem(n);
        }
    }

    static abstract class PoisonTransition
    extends Poison {
        PoisonTransition() {
        }
    }

    static abstract class Poison {
        Poison() {
        }

        abstract int getEffectId();
    }

    static interface Fishing {
        public boolean isFishing();

        public int getFishX();

        public int getFishY();
    }

    static interface Shop {
        public boolean isShop();

        public String getShopChat();

        public void start();
    }

    static interface Services {
        public void startAI(NativeDePerceptionState var1);

        public void wakeup(NativeDePerceptionState var1);

        public NativeDePerceptionEmblemData emblem(int var1);

        public void debug(NativeDePerceptionPacket var1);
    }
}
