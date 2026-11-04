// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeClientViewer;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeLiveAiScope;
import local.lito381.server.NativeDeVisibilityCoordinator;
import local.lito381.server.NativeDeVisibilityJobs;
import local.lito381.server.NativeDeVisibilityPc;
import local.lito381.server.NativeDeVisibilityPorts;
import local.lito381.server.NativeDeVisibilityScope;
import local.lito381.server.NativeDeVisibilityTimer;
import local.lito381.server.NativeDeWorldConnection;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativePlayerCommit;

final class NativeDeVisibilityRuntime {
    static final String FIELD = "sourceDeVisibility";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner owner(NativeDeWorldContext nativeDeWorldContext, NativeDeVisibilityPorts.Services services) {
        synchronized (nativeDeWorldContext) {
            nativeDeWorldContext.admit();
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            if (owner == null) {
                owner = new Owner(nativeDeWorldContext, Objects.requireNonNull(services));
                CombatAccess.set(nativeDeWorldContext, FIELD, owner);
            } else if (owner.services != services) {
                throw new IllegalArgumentException("visibility scene producers are immutable");
            }
            return owner;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner owner(NativeDeWorldContext nativeDeWorldContext) {
        synchronized (nativeDeWorldContext) {
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            return owner != null ? owner : NativeDeVisibilityRuntime.owner(nativeDeWorldContext, NativeDeVisibilityPorts.NATIVE);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T> T call(NativeDeWorldContext nativeDeWorldContext, Supplier<T> supplier) {
        Owner owner;
        ReentrantReadWriteLock.ReadLock readLock = nativeDeWorldContext.admission.readLock();
        readLock.lock();
        try {
            nativeDeWorldContext.admit();
            owner = NativeDeVisibilityRuntime.owner(nativeDeWorldContext);
            if (owner.closed) {
                throw new RejectedExecutionException("source visibility closed");
            }
        }
        finally {
            readLock.unlock();
        }
        boolean bl = NativeDeVisibilityScope.present() || NativeDeLiveAiScope.present() && NativeDeLiveAiScope.context() == nativeDeWorldContext;
        NativePlayerCommit.Frame frame = NativePlayerCommit.frame();
        bl |= frame != null && frame.queue.life == nativeDeWorldContext.life;
        NativeDeVisibilityJobs.Credit credit = bl ? null : NativeDeVisibilityJobs.Credit.reserve(owner);
        try {
            return credit == null ? NativeDeVisibilityScope.call(owner, supplier) : NativeDeVisibilityJobs.with(credit, () -> NativeDeVisibilityScope.call(owner, supplier));
        }
        finally {
            if (credit != null) {
                credit.release();
            }
        }
    }

    static void update(NativeDeDeletionPorts.Viewer viewer) {
        NativeDeWorldContext nativeDeWorldContext = NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).context;
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            NativeDeVisibilityPc.updateObject(viewer);
            return null;
        });
    }

    static void updateSource(NativeDeDeletionPorts.Viewer viewer) {
        if (viewer == null) {
            return;
        }
        NativeDeWorldContext.Entry entry = NativeDeWorldContext.entry(viewer);
        NativeDeVisibilityRuntime.call(entry.context, () -> {
            if (viewer instanceof NativeDeClientViewer) {
                NativeDeClientViewer nativeDeClientViewer = (NativeDeClientViewer)viewer;
                ClientSession clientSession = NativeDeVisibilityRuntime.client(nativeDeClientViewer);
                if (clientSession == null) {
                    return null;
                }
                if (entry.context.bindViewer(clientSession, entry.showId) != viewer) {
                    throw new RejectedExecutionException("source PC refresh binding changed");
                }
            }
            NativeDeVisibilityCoordinator.visibilitySource$updateForPc(viewer);
            return null;
        });
    }

    static void tick(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            NativeDeVisibilityCoordinator.tick();
            return null;
        });
    }

    static void start(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            Owner owner = NativeDeVisibilityRuntime.owner(nativeDeWorldContext);
            synchronized (owner) {
                if (owner.timer == null) {
                    owner.timer = new NativeDeVisibilityTimer();
                    owner.timer.start();
                }
            }
            return null;
        });
    }

    static void stop(NativeDeWorldContext nativeDeWorldContext) {
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            Owner owner = NativeDeVisibilityRuntime.owner(nativeDeWorldContext);
            synchronized (owner) {
                if (owner.timer != null) {
                    CombatAccess.call(owner.timer, "cancelTimer", "()V", new Object[0]);
                    owner.timer = null;
                }
            }
            return null;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(NativeDeWorldContext nativeDeWorldContext) {
        synchronized (nativeDeWorldContext) {
            Owner owner = (Owner)CombatAccess.field(nativeDeWorldContext, FIELD);
            if (owner == null) {
                return;
            }
            owner.closed = true;
            for (NativeDeVisibilityJobs.Ticket ticket : (NativeDeVisibilityJobs.Ticket[])owner.tickets.toArray(NativeDeVisibilityJobs.Ticket[]::new)) {
                ticket.cancel(false);
            }
            CombatAccess.set(nativeDeWorldContext, FIELD, null);
        }
    }

    static ClientSession client(NativeDeClientViewer nativeDeClientViewer) {
        return (ClientSession)CombatAccess.call(nativeDeClientViewer, "current", "()Llocal/lito381/server/ClientSession;", new Object[0]);
    }

    static int online(NativeDeDeletionPorts.Viewer viewer) {
        if (viewer instanceof NativeDeClientViewer nativeDeClientViewer && NativeDeVisibilityRuntime.client(nativeDeClientViewer) == null) {
            return 0;
        }
        return NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).onlineStatus;
    }

    static NativeDeWorldConnection connection(NativeDeDeletionPorts.Viewer viewer) {
        if (viewer instanceof NativeDeClientViewer nativeDeClientViewer && NativeDeVisibilityRuntime.client(nativeDeClientViewer) == null) {
            return null;
        }
        return NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).connection;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static int hp(NativeDeDeletionPorts.ObjectRef objectRef, boolean bl) {
        if (objectRef instanceof NativeDeClientViewer) {
            NativeDeClientViewer nativeDeClientViewer = (NativeDeClientViewer)objectRef;
            ClientSession clientSession = NativeDeVisibilityRuntime.client(nativeDeClientViewer);
            if (clientSession == null) {
                throw new RejectedExecutionException("stale source PC HP binding");
            }
            NativePlayerCommit.Queue queue = NativePlayerCommit.queue(clientSession, nativeDeClientViewer.binding);
            if (queue == null) {
                throw new RejectedExecutionException("stale source PC HP queue");
            }
            queue.vitalLock.lock();
            try {
                if (queue.closed || !nativeDeClientViewer.binding.matchesFast(clientSession)) {
                    throw new RejectedExecutionException("retired source PC HP read");
                }
                return NativePlayerCommit.integer(clientSession, bl ? "playerMaxHp" : "playerHp");
            }
            finally {
                queue.vitalLock.unlock();
            }
        }
        NativeDeCharacterFields nativeDeCharacterFields = (NativeDeCharacterFields)objectRef;
        return bl ? nativeDeCharacterFields.getMaxHp() : nativeDeCharacterFields.getCurrentHp();
    }

    static void broadcastPacketHP(NativeDeCharacterFields nativeDeCharacterFields, NativeDeDeletionPorts.Viewer viewer) {
        NativeDeWorldContext nativeDeWorldContext = NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeCharacterFields).context;
        if (NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).context != nativeDeWorldContext) {
            throw new IllegalArgumentException("foreign HP viewer");
        }
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            CombatAccess.call(nativeDeCharacterFields, "visibilitySource$broadcastPacketHP", "(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V", viewer);
            return null;
        });
    }

    static void hideTrackedHpBars(NativeDeCharacterFields nativeDeCharacterFields) {
        NativeDeWorldContext nativeDeWorldContext = NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeCharacterFields).context;
        NativeDeVisibilityRuntime.call(nativeDeWorldContext, () -> {
            CombatAccess.call(nativeDeCharacterFields, "visibilitySource$hideTrackedHpBars", "()V", new Object[0]);
            return null;
        });
    }

    static void requirePublication(NativeDeClientViewer nativeDeClientViewer) {
        if (NativeDeVisibilityScope.present()) {
            Owner owner = NativeDeVisibilityScope.owner();
            if (owner.closed || owner.context.closed || NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)nativeDeClientViewer).context != owner.context) {
                throw new RejectedExecutionException("source visibility publication retired");
            }
        }
    }

    static void send(NativeDeDeletionPorts.Viewer viewer, NativeDeDeletionPorts.Packet packet) {
        Owner owner = NativeDeVisibilityScope.owner();
        if (owner.closed || owner.context.closed || NativeDeWorldContext.entry((NativeDeDeletionPorts.ObjectRef)viewer).context != owner.context) {
            throw new RejectedExecutionException("source visibility scene closed");
        }
        viewer.sendPackets(packet);
    }

    private NativeDeVisibilityRuntime() {
    }

    static final class Owner {
        final NativeDeWorldContext context;
        final NativeDeVisibilityPorts.Services services;
        final Set<NativeDeVisibilityJobs.Ticket> tickets = ConcurrentHashMap.newKeySet();
        volatile boolean closed;
        volatile NativeDeVisibilityTimer timer;

        Owner(NativeDeWorldContext nativeDeWorldContext, NativeDeVisibilityPorts.Services services) {
            this.context = nativeDeWorldContext;
            this.services = services;
        }
    }
}

