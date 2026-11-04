// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Random;
import local.lito381.server.NativeDeNpcMobSkill;
import local.lito381.server.NativeDeNpcTemplateScope;
import local.lito381.server.NativeDeNpcTemplateState;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeNpcTemplatePorts {
    NativeDeNpcTemplatePorts() {
    }

    static final class MobSkills {
        MobSkills() {
        }

        static MobSkills getInstance() {
            return new MobSkills();
        }

        NativeDeNpcMobSkill getTemplate(int n) {
            return NativeDeNpcTemplateScope.actor().initialization.mobSkill(n);
        }
    }

    static final class WorkTimer {
        WorkTimer() {
        }

        static void put(NativeDeNpcTemplateState nativeDeNpcTemplateState, Integer n) {
            NativeDeNpcTemplateScope.actor().initialization.work(nativeDeNpcTemplateState, n);
        }
    }

    static interface Services {
        public Random random();

        public void work(NativeDeNpcTemplateState var1, Integer var2);

        public NativeDeNpcMobSkill mobSkill(int var1);

        public NativeDeWorldLog log();
    }
}
