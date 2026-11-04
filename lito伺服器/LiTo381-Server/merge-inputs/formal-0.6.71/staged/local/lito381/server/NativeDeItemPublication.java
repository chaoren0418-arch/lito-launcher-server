// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeAcceptedState;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemTransaction;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeServerLifetime;
import local.lito381.server.RuntimeInventory;

final class NativeDeItemPublication {
    static final String FIELD = "sourceItemPublicationLock";
    private static final ThreadLocal<Ticket> current = new ThreadLocal();

    static void requireCurrent(ClientSession clientSession) throws IOException {
        Ticket ticket = current.get();
        if (ticket != null) {
            ticket.require(clientSession);
        }
    }

    static void committed(ClientSession clientSession, NativeDeItemTransaction.Change change) {
        Ticket ticket = current.get();
        if (ticket != null) {
            if (ticket.pc != clientSession) {
                throw new IllegalStateException("item publication context differs");
            }
            ticket.output = change;
        }
    }

    static boolean allowPublication(ClientSession clientSession) {
        if (!NativeRolePublication.allowPublication(clientSession) || !NativeAcceptedState.allowPublication(clientSession)) {
            return false;
        }
        Ticket ticket = current.get();
        if (ticket == null) {
            return true;
        }
        try {
            ticket.require(clientSession);
            return true;
        }
        catch (IOException iOException) {
            return false;
        }
    }

    static RuntimeInventory inventory(ClientSession clientSession) {
        Ticket ticket = current.get();
        return ticket != null && ticket.pc == clientSession && ticket.output != null ? ticket.output.next() : clientSession.nativeDeInventory(0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean retain(ClientSession clientSession) throws IOException {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(clientSession);
        if (nativeServerLifetime == null) {
            return false;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            ArrayDeque<Object> arrayDeque = NativeServerLifetime.packets.get();
            if (nativeServerLifetime.closing && !arrayDeque.contains(nativeServerLifetime)) {
                if (arrayDeque.isEmpty()) {
                    NativeServerLifetime.packets.remove();
                }
                throw new IOException("application item publication admission closed");
            }
            ++nativeServerLifetime.handlers;
            arrayDeque.push(nativeServerLifetime);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void grant(ClientSession clientSession, int n, int n2) throws IOException {
        if (current.get() != null) {
            throw new IOException("nested item publication requires deferred output handoff");
        }
        ReentrantLock reentrantLock = (ReentrantLock)CombatAccess.field(clientSession, FIELD);
        boolean bl = NativeDeItemPublication.retain(clientSession);
        boolean bl2 = false;
        try {
            Ticket ticket;
            Object object;
            Object object2 = object = CombatAccess.field(clientSession, "sourceItemAdmissionGate");
            synchronized (object2) {
                ticket = new Ticket(clientSession);
                ticket.require(clientSession);
            }
            try {
                reentrantLock.lockInterruptibly();
                bl2 = true;
            }
            catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IOException("item publication wait interrupted", interruptedException);
            }
            current.set(ticket);
            clientSession.nativeDeGrantAndPublishPeriodicItem(n, n2);
        }
        finally {
            current.remove();
            if (bl2) {
                reentrantLock.unlock();
            }
            if (bl) {
                NativeServerLifetime.leavePacket();
            }
        }
    }

    private NativeDeItemPublication() {
    }

    static Runnable completion(Runnable runnable) {
        Ticket ticket = current.get();
        return () -> {
            Ticket ticket2 = current.get();
            if (ticket == null) {
                current.remove();
            } else {
                current.set(ticket);
            }
            try {
                runnable.run();
            }
            finally {
                if (ticket2 == null) {
                    current.remove();
                } else {
                    current.set(ticket2);
                }
            }
        };
    }

    private static final class Ticket {
        final ClientSession pc;
        final NativeDeItemApplication app;
        final NativeDeItemApplication.Owner owner;
        final long objectId;
        final String selected;
        final String source;
        final String account;
        NativeDeItemTransaction.Change output;

        Ticket(ClientSession clientSession) {
            this.pc = clientSession;
            GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
            this.app = (NativeDeItemApplication)CombatAccess.field(gameHub, "sourceItemApplication");
            this.owner = NativeDeItemApplication.transactionOwner(clientSession);
            this.objectId = clientSession.playerObjectId();
            this.selected = clientSession.selectedCharacterName();
            this.source = clientSession.sourceCharacterIdentity();
            this.account = clientSession.sourceAccountIdentity();
        }

        void require(ClientSession clientSession) throws IOException {
            if (!(clientSession == this.pc && this.objectId == this.pc.playerObjectId() && this.selected.equals(this.pc.selectedCharacterName()) && this.source.equals(this.pc.sourceCharacterIdentity()) && this.account.equals(this.pc.sourceAccountIdentity()) && NativePlayerLifetime.of(this.pc).admits())) {
                throw new IOException("item publication character admission retired");
            }
            GameHub gameHub = (GameHub)CombatAccess.field(this.pc, "hub");
            if (CombatAccess.field(gameHub, "sourceItemApplication") != this.app || this.app != null && (this.app.closed || this.owner == null || !this.owner.active || !this.owner.matches(this.pc) || CombatAccess.field(this.pc, "sourceItemOwner") != this.owner)) {
                throw new IOException("item publication owner retired");
            }
        }
    }
}
