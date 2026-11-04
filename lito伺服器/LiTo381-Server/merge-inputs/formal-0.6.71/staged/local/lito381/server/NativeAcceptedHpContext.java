// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import local.lito381.server.NativeCastTimeline;

final class NativeAcceptedHpContext
implements AutoCloseable {
    static final List<String> FIELDS = List.of("CombatMonsterCurseLifecycle.poisonDamageTarget", "CombatWorldCommit.effects", "CombatWorldFreeze.visualSeconds", "DragonSkillRequests.REQUEST", "ForcedCombat.CURRENT", "IllusionSkillRequests.REQUEST", "NativeAcceptedState.publication", "NativeCombatAttacker.ingress", "NativeCombatAttacker.reflecting", "NativeCombatOrigin.clock", "NativeCombatOrigin.installing", "NativePlayerCommit.calculation", "NativePlayerCommit.worldCalculation", "NativePlayerLifetime.minimumDeadline", "NativePlayerVitals.minimum", "NativeProcHealingEncounter.direct");
    private static final ThreadLocal<Object>[] locals = NativeAcceptedHpContext.resolve();
    private NativeCastTimeline.Arrival packetBefore;
    private final Object[] previous = new Object[locals.length];
    private boolean closed;

    private static ThreadLocal<Object>[] resolve() {
        ThreadLocal[] threadLocalArray = new ThreadLocal[FIELDS.size()];
        try {
            for (int i = 0; i < FIELDS.size(); ++i) {
                int n;
                String string = FIELDS.get(i);
                Class<?> clazz = Class.forName("local.lito381.server." + string.substring(0, n = string.indexOf(46)), true, NativeAcceptedHpContext.class.getClassLoader());
                Field field = clazz.getDeclaredField(string.substring(n + 1));
                if (field.getType() != ThreadLocal.class || !Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException("invalid native combat context field " + string);
                }
                field.setAccessible(true);
                threadLocalArray[i] = (ThreadLocal)field.get(null);
                if (threadLocalArray[i] != null) continue;
                throw new IllegalStateException("uninitialized native combat context " + string);
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
        return threadLocalArray;
    }

    private NativeAcceptedHpContext() {
        for (int i = 0; i < locals.length; ++i) {
            this.previous[i] = locals[i].get();
        }
        for (ThreadLocal<Object> threadLocal : locals) {
            threadLocal.remove();
        }
        this.packetBefore = NativeCastTimeline.detach();
    }

    static NativeAcceptedHpContext detach() {
        return new NativeAcceptedHpContext();
    }

    @Override
    public void close() {
        if (this.closed) {
            throw new IllegalStateException("native HP context restored twice");
        }
        this.closed = true;
        NativeCastTimeline.restore(this.packetBefore);
        for (int i = locals.length - 1; i >= 0; --i) {
            if (this.previous[i] == null) {
                locals[i].remove();
                continue;
            }
            locals[i].set(this.previous[i]);
        }
    }
}
