// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceName;
import local.lito381.server.NativeDeInstancePorts;
import local.lito381.server.NativeDeInstanceState;
import local.lito381.server.NativeDeWorldContext;

final class NativeDeInstanceNames {
    static final String FIELD = "sourceDeInstanceNames";
    private final IdentityHashMap<NativeDeInitializerKernel.Name, NativeDeInstanceName> values = new IdentityHashMap();

    NativeDeInstanceNames() {
    }

    static NativeDeInitializerKernel.Name name(NativeDeInstancePorts.Construction construction, NativeDeInitializerKernel.Name name) {
        if (name == null || name instanceof NativeDeInstanceName) {
            return name;
        }
        NativeDeDeletionPorts.Services services = construction.owner().services;
        if (!(services instanceof NativeDeWorldContext)) {
            throw new IllegalArgumentException("source mutable names require a concrete World");
        }
        NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)services;
        return NativeDeInstanceNames.name(nativeDeWorldContext, name);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeDeInitializerKernel.Name name(NativeDeWorldContext nativeDeWorldContext, NativeDeInitializerKernel.Name name2) {
        if (name2 == null || name2 instanceof NativeDeInstanceName) {
            return name2;
        }
        ReentrantReadWriteLock.ReadLock readLock = nativeDeWorldContext.admission.readLock();
        readLock.lock();
        try {
            if (nativeDeWorldContext.closed) {
                throw new IllegalStateException("source name World closed");
            }
            NativeDeWorldContext nativeDeWorldContext2 = nativeDeWorldContext;
            synchronized (nativeDeWorldContext2) {
                NativeDeInstanceNames nativeDeInstanceNames = (NativeDeInstanceNames)CombatAccess.field(nativeDeWorldContext, FIELD);
                if (nativeDeInstanceNames == null) {
                    nativeDeInstanceNames = new NativeDeInstanceNames();
                    CombatAccess.set(nativeDeWorldContext, FIELD, nativeDeInstanceNames);
                }
                NativeDeInitializerKernel.Name name3 = nativeDeInstanceNames.values.computeIfAbsent(name2, name -> new NativeDeInstanceName(name.get_deobjid(), name.get_name(), name.get_classId(), name.get_sex(), name.get_clanid(), name.get_weaponType(), name.getTitle(), name.getDeShop()));
                return name3;
            }
        }
        finally {
            readLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeWorldContext nativeDeWorldContext2 = nativeDeWorldContext;
        synchronized (nativeDeWorldContext2) {
            NativeDeInstanceNames nativeDeInstanceNames = (NativeDeInstanceNames)CombatAccess.field(nativeDeWorldContext, FIELD);
            if (nativeDeInstanceNames != null) {
                nativeDeInstanceNames.values.clear();
            }
            CombatAccess.set(nativeDeWorldContext, FIELD, null);
        }
    }

    static NativeDeInitializerKernel.Services services(NativeDeInstanceState nativeDeInstanceState) {
        return new Services(nativeDeInstanceState.construction);
    }

    private static final class Services
    implements NativeDeInitializerKernel.Services {
        final NativeDeInstancePorts.Construction boot;
        final NativeDeInitializerKernel.Services source;

        Services(NativeDeInstancePorts.Construction construction) {
            this.boot = construction;
            this.source = construction.initialization();
        }

        @Override
        public NativeDeInitializerKernel.Name[] getDeNameList() {
            NativeDeInitializerKernel.Name[] nameArray = this.source.getDeNameList();
            NativeDeInitializerKernel.Name[] nameArray2 = new NativeDeInitializerKernel.Name[nameArray.length];
            for (int i = 0; i < nameArray.length; ++i) {
                nameArray2[i] = NativeDeInstanceNames.name(this.boot, nameArray[i]);
            }
            return nameArray2;
        }

        @Override
        public NativeDeInitializerKernel.Log log() {
            return this.source.log();
        }

        @Override
        public int nextInt(int n) {
            return this.source.nextInt(n);
        }

        @Override
        public boolean nextBoolean() {
            return this.source.nextBoolean();
        }

        @Override
        public NativeDeInitializerKernel.Setting getSettingById(int n) {
            return this.source.getSettingById(n);
        }

        @Override
        public ArrayList<NativeDeInitializerKernel.Name> liveNames() {
            return this.source.liveNames();
        }

        @Override
        public ReentrantLock namesLock() {
            return this.source.namesLock();
        }

        @Override
        public NativeDeInitializerKernel.Ability getAbility(int n) {
            return this.source.getAbility(n);
        }

        @Override
        public int getRandomPoly(int n, int n2) {
            return this.source.getRandomPoly(n, n2);
        }

        @Override
        public int getRandomDoll(int n, int n2) {
            return this.source.getRandomDoll(n, n2);
        }

        @Override
        public NativeDeInitializerKernel.PetInfo getPetInfo(int n, int n2) {
            return this.source.getPetInfo(n, n2);
        }

        @Override
        public int getAttackSpeed(int n, int n2) {
            return this.source.getAttackSpeed(n, n2);
        }

        @Override
        public int getMoveSpeed(int n, int n2) {
            return this.source.getMoveSpeed(n, n2);
        }

        @Override
        public NativeDeInitializerKernel.Clan clan(int n) {
            return this.source.clan(n);
        }

        @Override
        public NativeDeInitializerKernel.Emblem emblem(int n) {
            return this.source.emblem(n);
        }

        @Override
        public NativeDeInitializerKernel.Template template(int n) {
            return this.source.template(n);
        }

        @Override
        public void doll(NativeDeInitializerKernel.Template template, NativeDeInitializerKernel.Actor actor, int n) {
            this.source.doll(template, actor, n);
        }

        @Override
        public NativeDeInitializerKernel.Pet summon(NativeDeInitializerKernel.Template template, NativeDeInitializerKernel.Actor actor) {
            return this.source.summon(template, actor);
        }

        @Override
        public NativeDeInitializerKernel.Packet visual(int n, int n2) {
            return this.source.visual(n, n2);
        }
    }
}
