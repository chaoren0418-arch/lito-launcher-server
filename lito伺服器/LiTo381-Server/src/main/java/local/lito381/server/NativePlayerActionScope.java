// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Objects;
import local.lito381.server.ClientSession;
import local.lito381.server.NativePlayerVitalCompletions;

final class NativePlayerActionScope {
    private static final ThreadLocal<Root> current = new ThreadLocal();

    static Ticket enter(ClientSession ... clientSessionArray) {
        int n;
        for (ClientSession clientSessionArray2 : clientSessionArray) {
            Objects.requireNonNull(clientSessionArray2, "pc");
        }
        Object object = current.get();
        int n2 = n = object == null ? 1 : 0;
        if (n != 0) {
            object = new Root();
        }
        Ticket ticket = new Ticket((Root)object);
        for (ClientSession clientSession : clientSessionArray) {
            ((Root)object).register(clientSession, false);
        }
        ++((Root)object).depth;
        if (n != 0) {
            current.set((Root)object);
        }
        return ticket;
    }

    static void unlocked(ClientSession clientSession) {
        Root root = current.get();
        if (root == null) {
            NativePlayerVitalCompletions.drain(clientSession);
        } else {
            root.register(clientSession, true);
        }
    }

    static boolean deferDrain(ClientSession clientSession) {
        Root root = current.get();
        if (root == null) {
            return false;
        }
        root.register(clientSession, false);
        return true;
    }

    static Runnable completion(ClientSession clientSession, Runnable runnable) {
        Root root = current.get();
        if (root == null) {
            return runnable;
        }
        root.register(clientSession, false);
        return new Gated(root, runnable);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    static Root blocked(Runnable runnable) {
        if (!(runnable instanceof Gated)) return null;
        Gated gated = (Gated)runnable;
        if (gated.root.closed) return null;
        Root root = gated.root;
        return root;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean resumeAfterDrain(ClientSession clientSession, Root root) {
        Object object = root.fence;
        synchronized (object) {
            if (root.closed) {
                return true;
            }
            root.register(clientSession, false);
            return false;
        }
    }

    private NativePlayerActionScope() {
    }

    static final class Root {
        final Thread owner = Thread.currentThread();
        final Object fence = new Object();
        final IdentityHashMap<ClientSession, Boolean> members = new IdentityHashMap();
        final IdentityHashMap<ClientSession, Boolean> unlocked = new IdentityHashMap();
        final ArrayList<ClientSession> order = new ArrayList();
        final ArrayList<ClientSession> releaseOrder = new ArrayList();
        volatile boolean closed;
        int depth;

        Root() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void register(ClientSession clientSession, boolean bl) {
            Object object = this.fence;
            synchronized (object) {
                if (this.closed) {
                    throw new IllegalStateException("closed player action scope");
                }
                if (this.members.put(clientSession, Boolean.TRUE) == null) {
                    this.order.add(clientSession);
                }
                if (bl && this.unlocked.put(clientSession, Boolean.TRUE) == null) {
                    this.releaseOrder.add(clientSession);
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void finish() {
            if (Thread.currentThread() != this.owner || current.get() != this || this.depth <= 0) {
                throw new IllegalStateException("player action scope ownership");
            }
            if (--this.depth != 0) {
                return;
            }
            current.remove();
            Object object = this.fence;
            synchronized (object) {
                this.closed = true;
            }
            try {
                ClientSession clientSession;
                int n;
                for (n = 0; n < this.releaseOrder.size(); ++n) {
                    clientSession = this.releaseOrder.get(n);
                    if (this.members.remove(clientSession) == null) continue;
                    this.drain(clientSession);
                }
                for (n = 0; n < this.order.size(); ++n) {
                    clientSession = this.order.get(n);
                    if (this.members.remove(clientSession) == null) continue;
                    this.drain(clientSession);
                }
            }
            finally {
                this.members.clear();
                this.unlocked.clear();
                this.order.clear();
                this.releaseOrder.clear();
            }
        }

        private void drain(ClientSession clientSession) {
            try {
                NativePlayerVitalCompletions.drain(clientSession);
            }
            catch (Throwable throwable) {
                try {
                    NativePlayerVitalCompletions.logCompletionFailure(clientSession, throwable);
                }
                catch (Throwable throwable2) {
                    // empty catch block
                }
            }
        }
    }

    static final class Ticket
    implements AutoCloseable {
        final Root root;
        boolean released;

        Ticket(Root root) {
            this.root = root;
        }

        @Override
        public void close() {
            if (this.released) {
                throw new IllegalStateException("player action scope closed twice");
            }
            this.released = true;
            this.root.finish();
        }
    }

    private record Gated(Root root, Runnable action) implements Runnable
    {
        @Override
        public void run() {
            if (!this.root.closed) {
                throw new IllegalStateException("player action completion before outer unlock");
            }
            this.action.run();
        }
    }
}
