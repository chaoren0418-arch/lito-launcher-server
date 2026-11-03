// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeNpcVitalState;

interface NativeDeCorpseClock {
    public static final NativeDeCorpseClock UNBOUND = new NativeDeCorpseClock(){

        @Override
        public void track(NativeDeNpcVitalState nativeDeNpcVitalState) {
            throw new IllegalStateException("De corpse timer application owner must be bound");
        }

        @Override
        public void invalidStart(int n, int n2, int n3, long l) {
            System.getLogger(NativeDeCorpseClock.class.getName()).log(System.Logger.Level.WARNING, "corpse timer start rejected: objectId=" + n + " npcId=" + n2 + " seconds=" + n3 + " count=" + l);
        }
    };

    public void track(NativeDeNpcVitalState var1);

    public void invalidStart(int var1, int var2, int var3, long var4);
}
