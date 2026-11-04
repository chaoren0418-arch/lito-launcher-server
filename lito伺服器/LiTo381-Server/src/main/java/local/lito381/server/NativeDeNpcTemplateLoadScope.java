// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import local.lito381.server.NativeDeNpcMobSkill;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateMobLoader;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeNpcTemplateLoadScope {
    private static final ThreadLocal<Context> current = new ThreadLocal();

    NativeDeNpcTemplateLoadScope() {
    }

    static Context context() {
        Context context = current.get();
        if (context == null) {
            throw new IllegalStateException("NPC table load scope absent");
        }
        return context;
    }

    static <T> T call(Context context, Supplier<T> supplier) {
        if (current.get() != null) {
            throw new IllegalStateException("nested NPC table publication");
        }
        current.set(context);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            current.remove();
        }
    }

    static boolean present() {
        return current.get() != null;
    }

    static Map<Integer, NativeDeNpcTemplate> npcs() {
        return NativeDeNpcTemplateLoadScope.context().npcs;
    }

    static Map<Integer, NativeDeNpcMobSkill> mobSkills() {
        return NativeDeNpcTemplateLoadScope.context().mobSkills;
    }

    static Map<String, Integer> families() {
        return NativeDeNpcTemplateLoadScope.context().families;
    }

    static NativeDeWorldLog log() {
        return NativeDeNpcTemplateLoadScope.context().log;
    }

    static void orc(int n) {
        NativeDeNpcTemplateLoadScope.context().orc = n;
    }

    static Class<?> resolveClass(String string) throws ClassNotFoundException {
        try {
            return NativeDeNpcTemplateLoadScope.context().resolver.resolve(string);
        }
        catch (ClassNotFoundException classNotFoundException) {
            NativeDeNpcTemplateLoadScope.context().unresolvedExecutors.add(string);
            throw classNotFoundException;
        }
    }

    static void suppressedBlankNames(NativeDeNpcTemplateMobLoader nativeDeNpcTemplateMobLoader) {
        ++NativeDeNpcTemplateLoadScope.context().suppressedBlankNameWrites;
    }

    static final class Context {
        final Sql sql;
        final Resolver resolver;
        final NativeDeWorldLog log;
        final Map<Integer, String> skillNames;
        final Map<Integer, NativeDeNpcTemplate> npcs = new HashMap<Integer, NativeDeNpcTemplate>();
        final Map<Integer, NativeDeNpcMobSkill> mobSkills = new HashMap<Integer, NativeDeNpcMobSkill>();
        Map<String, Integer> families = new HashMap<String, Integer>();
        final Set<String> requiredImplementations = new TreeSet<String>();
        final Set<String> unresolvedExecutors = new TreeSet<String>();
        int orc = -1;
        int suppressedBlankNameWrites;

        Context(Sql sql, Resolver resolver, NativeDeWorldLog nativeDeWorldLog, Map<Integer, String> map) {
            this.sql = Objects.requireNonNull(sql);
            this.resolver = Objects.requireNonNull(resolver);
            this.log = Objects.requireNonNull(nativeDeWorldLog);
            this.skillNames = Collections.unmodifiableMap(new HashMap<Integer, String>(map));
        }
    }

    static interface Resolver {
        public Class<?> resolve(String var1) throws ClassNotFoundException;
    }

    static final class Timer {
        private final long started = System.nanoTime();

        Timer() {
        }

        public long get() {
            return (System.nanoTime() - this.started) / 1000000L;
        }
    }

    static final class Skills {
        Skills() {
        }

        static Skills get() {
            return new Skills();
        }

        Skill getTemplate(int n) {
            String string = NativeDeNpcTemplateLoadScope.context().skillNames.get(n);
            return NativeDeNpcTemplateLoadScope.context().skillNames.containsKey(n) ? new Skill(string) : null;
        }
    }

    record Skill(String name) {
        public String getName() {
            return this.name;
        }
    }

    static final class Npcs {
        Npcs() {
        }

        static Npcs get() {
            return new Npcs();
        }

        NativeDeNpcTemplate getTemplate(int n) {
            return NativeDeNpcTemplateLoadScope.npcs().get(n);
        }
    }

    static final class Database {
        Database() {
        }

        static Database get() {
            return new Database();
        }

        Connection getConnection() throws SQLException {
            return NativeDeNpcTemplateLoadScope.context().sql.open();
        }
    }

    static interface Sql {
        public Connection open() throws SQLException;
    }
}
