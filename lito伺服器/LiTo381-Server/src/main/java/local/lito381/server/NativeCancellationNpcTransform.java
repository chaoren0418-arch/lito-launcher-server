// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeCancellationNpcRules;

final class NativeCancellationNpcTransform {
    static boolean apply(Target target, NativeCancellationNpcRules.Rule rule, Spawn spawn) {
        if (rule.type() == 1) {
            if (target.getTempCharGfx() != target.getGfxId() || target.getTempCharGfx() == rule.targetId()) {
                return false;
            }
            target.setTempCharGfx(rule.targetId());
            if (rule.passispeed() > 0) {
                target.setPassispeed(rule.passispeed());
            }
            if (!rule.nameId().isEmpty()) {
                target.setNameId(rule.nameId());
                target.broadcastPacketAll(new Name(target.getId(), rule.nameId()));
            }
            target.broadcastPacketAll(new Shape(target.getId(), rule.targetId()));
            return true;
        }
        if (rule.type() == 2) {
            Target target2 = spawn.create(rule.targetId(), target.getX(), target.getY(), target.getMapId(), target.getHeading(), rule.durationSeconds());
            if (target2 == null) {
                return false;
            }
            target.deleteMe();
            return true;
        }
        return false;
    }

    private NativeCancellationNpcTransform() {
    }

    static interface Target {
        public long getId();

        public int getTempCharGfx();

        public int getGfxId();

        public int getX();

        public int getY();

        public int getMapId();

        public int getHeading();

        public void setTempCharGfx(int var1);

        public void setPassispeed(int var1);

        public void setNameId(String var1);

        public void broadcastPacketAll(Notice var1);

        public void deleteMe();
    }

    record Name(long objectId, String name) implements Notice
    {
    }

    /*
     * Uses 'sealed' constructs - enablewith --sealed true
     */
    static interface Notice {
    }

    record Shape(long objectId, int gfx) implements Notice
    {
    }

    @FunctionalInterface
    static interface Spawn {
        public Target create(int var1, int var2, int var3, int var4, int var5, int var6);
    }
}
