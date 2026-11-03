// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeNpcVitalState;

interface NativeDeResurrectionEffects {
    public static final NativeDeResurrectionEffects UNBOUND = new NativeDeResurrectionEffects(){

        private IllegalStateException unbound() {
            return new IllegalStateException("De resurrection scene effects must be bound at construction");
        }

        @Override
        public RemoveObject newRemoveObject(int n) {
            throw this.unbound();
        }

        @Override
        public void undoPoly(NativeDeCharacterFields nativeDeCharacterFields) {
            throw this.unbound();
        }

        @Override
        public List<? extends Viewer> recognizePlayers(NativeDeCharacterFields nativeDeCharacterFields) {
            throw this.unbound();
        }

        @Override
        public void characterFailure(String string, Exception exception) {
            System.getLogger(NativeDeResurrectionEffects.class.getName()).log(System.Logger.Level.ERROR, string, (Throwable)exception);
        }

        @Override
        public void corpseRejected(int n, int n2, long l) {
            System.getLogger(NativeDeResurrectionEffects.class.getName()).log(System.Logger.Level.WARNING, "resurrection rejected after corpse deletion: objectId=" + n + " npcId=" + n2 + " count=" + l);
        }

        @Override
        public int npcId(NativeDeNpcVitalState nativeDeNpcVitalState) {
            throw this.unbound();
        }

        @Override
        public int x(NativeDeNpcVitalState nativeDeNpcVitalState) {
            throw this.unbound();
        }

        @Override
        public int y(NativeDeNpcVitalState nativeDeNpcVitalState) {
            throw this.unbound();
        }

        @Override
        public void broadcastResurrection(NativeDeCharacterFields nativeDeCharacterFields, NativeDeCharacterFields nativeDeCharacterFields2, int n) {
            throw this.unbound();
        }

        @Override
        public LoginSkill newSkillUse() {
            throw this.unbound();
        }

        @Override
        public void untrackCorpse(NativeDeNpcVitalState nativeDeNpcVitalState) {
            throw this.unbound();
        }
    };

    public RemoveObject newRemoveObject(int var1);

    public void undoPoly(NativeDeCharacterFields var1);

    public List<? extends Viewer> recognizePlayers(NativeDeCharacterFields var1);

    public void characterFailure(String var1, Exception var2);

    public void corpseRejected(int var1, int var2, long var3);

    public int npcId(NativeDeNpcVitalState var1);

    public int x(NativeDeNpcVitalState var1);

    public int y(NativeDeNpcVitalState var1);

    public void broadcastResurrection(NativeDeCharacterFields var1, NativeDeCharacterFields var2, int var3);

    public LoginSkill newSkillUse();

    public void untrackCorpse(NativeDeNpcVitalState var1);

    public static interface LoginSkill {
        public boolean handleCommands(Object var1, int var2, int var3, int var4, int var5, int var6, int var7, NativeDeCharacterFields var8);
    }

    public static interface Viewer {
        public void sendRemoveObject(RemoveObject var1);

        public void removeKnownObject(NativeDeCharacterFields var1);

        public void updateObject();
    }

    public record RemoveObject(int objectId) {
    }
}
