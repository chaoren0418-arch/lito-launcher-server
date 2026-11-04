// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.LongSupplier;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMapCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.GameHub;
import local.lito381.server.NativeDeChatJobs;
import local.lito381.server.NativeDeChatRuntime;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeInstanceNames;
import local.lito381.server.NativeDeInventoryIdentity;
import local.lito381.server.NativeDeLiveAiJobs;
import local.lito381.server.NativeDeNpcKnowledge;
import local.lito381.server.NativeDeObjectBindings;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeTerrainResources;
import local.lito381.server.NativeDeTerrainWorld;
import local.lito381.server.NativeDeVisibilityRuntime;
import local.lito381.server.NativeDeWorldConnection;
import local.lito381.server.NativeDeWorldControls;
import local.lito381.server.NativeDeWorldLocation;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.NativeDeWorldMap;
import local.lito381.server.NativeDeWorldMaps;
import local.lito381.server.NativeDeWorldScope;
import local.lito381.server.NativeLitoMapTableCatalog;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeProcHealingEncounter;
import local.lito381.server.ResourceDatabase;

final class NativeDeWorldContext
implements NativeDeDeletionPorts.Services {
    static final String FIELD = "sourceDeWorldContext";
    static final String ENTRY = "sourceDeWorldEntry";
    final NativePlayerLifetime life;
    final NativeDeDeletionPorts.Services remaining;
    final NativeDeWorldMaps maps;
    final NativeDeWorldLog log;
    final int recognizeRange;
    final Map<String, Object> registries = new LinkedHashMap<String, Object>();
    final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap();
    final ConcurrentHashMap<Integer, NativeDeWorldMap> mapValues = new ConcurrentHashMap();
    final Object core;
    final NativeDeDeletionOwner deletion;
    volatile boolean closed;
    final ReentrantReadWriteLock admission = new ReentrantReadWriteLock();
    final NativeDeDeletionPorts.World world = new NativeDeDeletionPorts.World(){

        @Override
        ArrayList<NativeDeDeletionPorts.ObjectRef> getVisibleObjects(NativeDeDeletionPorts.ObjectRef objectRef) {
            NativeDeWorldContext.this.require(objectRef);
            return (ArrayList)NativeDeWorldContext.this.call("getVisibleObjects", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/ArrayList;", objectRef);
        }

        @Override
        ArrayList<NativeDeDeletionPorts.Viewer> getRecognizePlayer(NativeDeDeletionPorts.ObjectRef objectRef) {
            NativeDeWorldContext.this.require(objectRef);
            return (ArrayList)NativeDeWorldContext.this.call("getRecognizePlayer", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/ArrayList;", objectRef);
        }

        @Override
        boolean isObjectRegistered(NativeDeDeletionPorts.ObjectRef objectRef) {
            NativeDeWorldContext.this.require(objectRef);
            return (Boolean)NativeDeWorldContext.this.call("isObjectRegistered", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z", objectRef);
        }

        @Override
        void removeObject(NativeDeDeletionPorts.ObjectRef objectRef) {
            NativeDeWorldContext.this.require(objectRef);
            NativeDeWorldContext.this.call("removeObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V", objectRef);
        }

        @Override
        void removeVisibleObject(NativeDeDeletionPorts.ObjectRef objectRef) {
            NativeDeWorldContext.this.require(objectRef);
            NativeDeWorldContext.this.call("removeVisibleObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V", objectRef);
        }
    };
    final NativeDeDeletionPorts.DeWorld deWorld = new NativeDeDeletionPorts.DeWorld(){

        @Override
        void remove(String string) {
            NativeDeWorldContext.this.invoke(NativeDeWorldContext.this.registries.get("De"), "remove", "(Ljava/lang/String;)V", string);
        }
    };
    private volatile Object sourceDeInstanceNames;
    private volatile Object sourceDeLiveAiJobs;
    private volatile Object sourceDeTerrain;
    private volatile Object sourceDeVisibility;
    private volatile Object sourceNpcChatJobs;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeDeWorldContext of(GameHub gameHub, NativeDeDeletionPorts.Services services) {
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(gameHub);
            if (!nativePlayerLifetime.admits()) {
                throw new RejectedExecutionException("World application closed");
            }
            NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)CombatAccess.field(gameHub, FIELD);
            if (nativeDeWorldContext != null) {
                if (nativeDeWorldContext.closed) {
                    throw new RejectedExecutionException("World application closed");
                }
                if (nativeDeWorldContext.remaining != services) {
                    throw new IllegalArgumentException("World service binding is immutable");
                }
                return nativeDeWorldContext;
            }
            HashSet<Integer> hashSet = new HashSet<Integer>(ClassicWorldMapCatalog.supportedMapIds());
            NativeDeTerrainResources.addMaps(hashSet);
            hashSet.addAll(ArenaMapCatalog.mapIds());
            for (NativeLitoMapTableCatalog.Entry entry : ResourceDatabase.maps(List.of())) {
                hashSet.add(entry.mapId());
            }
            NativeDeWorldContext nativeDeWorldContext2 = new NativeDeWorldContext(nativePlayerLifetime, services, hashSet, (string, string2, objectArray) -> System.getLogger(NativeDeWorldContext.class.getName()).log(string.equals("warn") ? System.Logger.Level.WARNING : (string.equals("error") ? System.Logger.Level.ERROR : System.Logger.Level.INFO), string2 + " " + Arrays.toString(objectArray)), NativeDeWorldControls.load(), System::nanoTime);
            CombatAccess.set(gameHub, FIELD, nativeDeWorldContext2);
            return nativeDeWorldContext2;
        }
    }

    static void closeHub(GameHub gameHub) {
        NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)CombatAccess.field(gameHub, FIELD);
        if (nativeDeWorldContext != null) {
            nativeDeWorldContext.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    NativeDeClientViewer bindViewer(ClientSession clientSession, int n) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        if (NativePlayerLifetime.of(gameHub) != this.life) {
            throw new IllegalArgumentException("foreign PC World binding");
        }
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            synchronized (battleTimedStatus) {
                int n2;
                this.admit();
                NativeDeClientViewer nativeDeClientViewer = NativeDeClientViewer.of(clientSession);
                if (nativeDeClientViewer.binding.phase().equals("WORLD")) {
                    n2 = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
                } else {
                    Object object = NativeProcHealingEncounter.room(gameHub, nativeDeClientViewer.binding.room());
                    if (object == null) {
                        throw new IllegalStateException("PC battle room absent");
                    }
                    n2 = ((BattleRules.MatchState)CombatAccess.field(object, "match")).rules().mapId();
                }
                int n3 = (Integer)CombatAccess.field(clientSession, "positionX");
                int n4 = (Integer)CombatAccess.field(clientSession, "positionY");
                Object object = CombatAccess.call(clientSession, "selectedCharacterSummary", "()Llocal/lito381/server/CharacterSummary;", new Object[0]);
                if (object == null) {
                    throw new IllegalStateException("PC selected character absent");
                }
                int n5 = (Integer)CombatAccess.call(object, "type", "()I", new Object[0]);
                Entry entry = this.attach(nativeDeClientViewer, n3, n4, n2, n, clientSession.playerName(), n5, 1, true);
                if (entry.showId != n) {
                    throw new IllegalArgumentException("PC show-ID binding requires owner retirement");
                }
                entry.name = clientSession.playerName();
                entry.classType = n5;
                if (!this.publish(nativeDeClientViewer)) {
                    throw new IllegalStateException("native PC World publication rejected");
                }
                if (!((Boolean)this.call("relocateVisibleObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;III)Z", nativeDeClientViewer, n3, n4, n2)).booleanValue()) {
                    throw new IllegalStateException("native PC World pose rejected");
                }
                return nativeDeClientViewer;
            }
        }
    }

    NativeDeWorldContext(NativePlayerLifetime nativePlayerLifetime, NativeDeDeletionPorts.Services services, Set<Integer> set, NativeDeWorldLog nativeDeWorldLog, int n, LongSupplier longSupplier) {
        this(nativePlayerLifetime, services, set, nativeDeWorldLog, n, longSupplier, "NativeDeWorldCore");
    }

    NativeDeWorldContext(NativePlayerLifetime nativePlayerLifetime, NativeDeDeletionPorts.Services services, Set<Integer> set, NativeDeWorldLog nativeDeWorldLog, int n, LongSupplier longSupplier, String string) {
        this.life = Objects.requireNonNull(nativePlayerLifetime);
        this.remaining = Objects.requireNonNull(services);
        this.maps = new NativeDeWorldMaps(Set.copyOf(set));
        this.log = Objects.requireNonNull(nativeDeWorldLog);
        this.recognizeRange = n;
        this.core = NativeDeWorldScope.call(this, () -> {
            for (String string2 : List.of("Bow", "Crown", "Darkelf", "De", "Doll", "DragonKnight", "Effect", "Elf", "Illusionist", "Item", "Knight", "Mob", "Npc", "Pet", "Quest", "Summons", "Trap", "Wizard")) {
                this.registries.put(string2, NativeDeWorldContext.construct("NativeDeWorldRegistry" + string2));
            }
            return NativeDeWorldContext.construct(string);
        });
        this.deletion = new NativeDeDeletionOwner(nativePlayerLifetime, this, longSupplier);
    }

    private static Object construct(String string) {
        try {
            Constructor<?> constructor = Class.forName("local.lito381.server." + string).getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            return constructor.newInstance(new Object[0]);
        }
        catch (InvocationTargetException invocationTargetException) {
            throw NativeDeWorldContext.failure(invocationTargetException.getCause());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(reflectiveOperationException);
        }
    }

    static RuntimeException failure(Throwable throwable) {
        if (throwable instanceof Error) {
            Error error = (Error)throwable;
            throw error;
        }
        if (throwable instanceof RuntimeException) {
            RuntimeException runtimeException = (RuntimeException)throwable;
            return runtimeException;
        }
        return new IllegalStateException(throwable);
    }

    AtomicLong count(String string2) {
        return this.counters.computeIfAbsent(string2, string -> new AtomicLong());
    }

    Object invoke(Object object, String string, String string2, Object ... objectArray) {
        return NativeDeWorldScope.call(this, () -> {
            try {
                MethodType methodType = MethodType.fromMethodDescriptorString(string2, this.getClass().getClassLoader());
                Method method = object.getClass().getDeclaredMethod(string, methodType.parameterArray());
                method.setAccessible(true);
                return method.invoke(object, objectArray);
            }
            catch (InvocationTargetException invocationTargetException) {
                throw NativeDeWorldContext.failure(invocationTargetException.getCause());
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                throw new IllegalStateException(reflectiveOperationException);
            }
        });
    }

    Object call(String string, String string2, Object ... objectArray) {
        return this.invoke(this.core, string, string2, objectArray);
    }

    NativeDeWorldMap map(int n) {
        return this.mapValues.computeIfAbsent(n, NativeDeWorldMap::new);
    }

    void admit() {
        if (this.closed || !this.life.admits()) {
            throw new RejectedExecutionException("World application closed");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    Entry attach(NativeDeDeletionPorts.ObjectRef objectRef, int n, int n2, int n3, int n4, String string, int n5, int n6, boolean bl) {
        Object object;
        Entry entry;
        ReentrantReadWriteLock.ReadLock readLock = this.admission.readLock();
        readLock.lock();
        try {
            NativeDeDeletionPorts.ObjectRef objectRef2;
            this.admit();
            NativeDeInventoryIdentity.requireWorldIdentity(objectRef);
            if (!(objectRef instanceof NativeDeObjectFields)) {
                throw new IllegalArgumentException("World actor requires native object identity");
            }
            if (objectRef instanceof NativeDeDeletionState) {
                NativeDeDeletionState deletionState = (NativeDeDeletionState)objectRef;
                if (deletionState.deletionOwner != this.deletion) {
                    throw new IllegalArgumentException("foreign NPC World owner");
                }
            }
            if (objectRef instanceof NativeDeClientViewer) {
                objectRef2 = (NativeDeClientViewer)objectRef;
                if (((NativeDeClientViewer)objectRef2).life != this.life || ((NativeDeClientViewer)objectRef2).retired) {
                    throw new IllegalArgumentException("foreign or retired PC World owner");
                }
            }
            objectRef2 = objectRef;
            synchronized (objectRef2) {
                Entry entry2 = (Entry)CombatAccess.field(objectRef, ENTRY);
                if (entry2 != null) {
                    if (entry2.context != this) {
                        throw new IllegalArgumentException("World binding is application immutable");
                    }
                    Entry entry3 = entry2;
                    // MONITOREXIT @DISABLED, blocks:[0, 4, 8] lbl23 : MonitorExitStatement: MONITOREXIT : var11_11 /* !! */ 
                    readLock.unlock();
                    return entry3;
                }
                entry = new Entry(this, n, n2, n3, n4, string, n5, n6, bl, objectRef);
            }
        }
        catch (Throwable throwable) {
            readLock.unlock();
            throw throwable;
        }
        {
            CombatAccess.set(objectRef, ENTRY, entry);
            if (objectRef instanceof NativeDeDeletionState) {
                object = (NativeDeDeletionState)objectRef;
                ((NativeDeDeletionState)object).showId = n4;
            }
            object = entry;
        }
        readLock.unlock();
        return (Entry)object;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean publish(NativeDeDeletionPorts.ObjectRef objectRef) {
        ReentrantReadWriteLock.ReadLock readLock = this.admission.readLock();
        readLock.lock();
        try {
            this.admit();
            this.require(objectRef);
            this.call("storeObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V", objectRef);
            boolean bl = (Boolean)this.call("addVisibleObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z", objectRef);
            return bl;
        }
        finally {
            readLock.unlock();
        }
    }

    Entry require(NativeDeDeletionPorts.ObjectRef objectRef) {
        Entry entry = NativeDeWorldContext.entry(objectRef);
        if (entry.context != this) {
            throw new IllegalArgumentException("foreign application World actor");
        }
        return entry;
    }

    static Entry entry(NativeDeDeletionPorts.ObjectRef objectRef) {
        Objects.requireNonNull(objectRef);
        Entry entry = (Entry)CombatAccess.field(objectRef, ENTRY);
        if (entry == null) {
            throw new IllegalStateException("native World actor is not attached");
        }
        if (NativeDeWorldScope.present() && NativeDeWorldScope.owner() != entry.context) {
            throw new IllegalArgumentException("foreign scoped World actor");
        }
        return entry;
    }

    static void detach(NativeDeDeletionPorts.ObjectRef objectRef) {
        Entry entry = (Entry)CombatAccess.field(objectRef, ENTRY);
        if (entry != null) {
            entry.context.call("removeObject", "(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V", objectRef);
        }
    }

    void close() {
        ReentrantReadWriteLock.WriteLock writeLock = this.admission.writeLock();
        writeLock.lock();
        try {
            this.closed = true;
            NativeDeChatJobs.close(this);
            NativeDeVisibilityRuntime.close(this);
            NativeDeTerrainWorld.close(this);
            NativeDeLiveAiJobs.close(this);
            NativeDeInstanceNames.close(this);
            NativeDeNpcKnowledge.retireWorld(this);
            NativeDeWorldScope.call(this, () -> {
                NativeDeWorldContext.clearMaps(this.core);
                for (Object object : this.registries.values()) {
                    NativeDeWorldContext.clearMaps(object);
                }
                return null;
            });
        }
        finally {
            writeLock.unlock();
        }
    }

    private static void clearMaps(Object object) {
        try {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object object2 = field.get(object);
                if (object2 instanceof Map) {
                    Map map = (Map)object2;
                    map.clear();
                    continue;
                }
                if (!(object2 instanceof CopyOnWriteArrayList)) continue;
                CopyOnWriteArrayList copyOnWriteArrayList = (CopyOnWriteArrayList)object2;
                copyOnWriteArrayList.clear();
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(reflectiveOperationException);
        }
    }

    @Override
    public NativeDeDeletionPorts.World world() {
        return this.world;
    }

    @Override
    public NativeDeDeletionPorts.DeWorld deWorld() {
        return this.deWorld;
    }

    @Override
    public short mapId(NativeDeDeletionPorts.ObjectRef objectRef) {
        return (short)this.require((NativeDeDeletionPorts.ObjectRef)objectRef).location.getMapId();
    }

    @Override
    public NativeDeDeletionPorts.Quest quest() {
        return this.remaining.quest();
    }

    @Override
    public NativeDeDeletionPorts.Boss boss() {
        return this.remaining.boss();
    }

    @Override
    public NativeDeDeletionPorts.Log log() {
        return this.remaining.log();
    }

    @Override
    public ArrayList<NativeDeInitializerKernel.Name> names() {
        return this.remaining.names();
    }

    @Override
    public ReentrantLock namesLock() {
        return this.remaining.namesLock();
    }

    @Override
    public boolean bossAudit() {
        return this.remaining.bossAudit();
    }

    @Override
    public void timer(String string, NativeDeDeletionPorts.ObjectRef objectRef) {
        this.remaining.timer(string, objectRef);
    }

    @Override
    public void stopAI(NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeLiveAiJobs.stop(objectRef);
        this.remaining.stopAI(objectRef);
    }

    @Override
    public void cancelDelayedMove(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.remaining.cancelDelayedMove(objectRef);
    }

    @Override
    public void cancelChat(NativeDeDeletionPorts.ObjectRef objectRef) {
        NativeDeChatRuntime.cancelObject(objectRef);
        this.remaining.cancelChat(objectRef);
    }

    @Override
    public void onDeleteClaimed(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.remaining.onDeleteClaimed(objectRef);
    }

    @Override
    public void allTargetClear(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.remaining.allTargetClear(objectRef);
    }

    @Override
    public void removeAllKnown(NativeDeDeletionPorts.ObjectRef objectRef) {
        this.remaining.removeAllKnown(objectRef);
    }

    static final class Entry {
        final NativeDeWorldContext context;
        final NativeDeWorldLocation location;
        volatile int showId;
        volatile int onlineStatus;
        volatile int classType;
        volatile String name;
        volatile NativeDeWorldConnection connection;

        Entry(NativeDeWorldContext nativeDeWorldContext, int n, int n2, int n3, int n4, String string, int n5, int n6, boolean bl, NativeDeDeletionPorts.ObjectRef objectRef) {
            this.context = nativeDeWorldContext;
            this.location = NativeDeObjectBindings.attachLocation(objectRef, nativeDeWorldContext, n, n2, n3, n4);
            this.showId = n4;
            this.name = Objects.requireNonNull(string);
            this.classType = n5;
            this.onlineStatus = n6;
            this.connection = bl ? new NativeDeWorldConnection() : null;
        }
    }
}
