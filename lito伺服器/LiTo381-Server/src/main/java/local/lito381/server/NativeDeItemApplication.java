// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeDeItemDefinitionsResources;
import local.lito381.server.NativeDeItemInventory;
import local.lito381.server.NativeDeItemTemplatesResources;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.ServerConfig;

final class NativeDeItemApplication {
    static final String FIELD = "sourceItemApplication";
    static final String OWNER = "sourceItemOwner";
    static final String DISCONNECTED = "sourceItemDisconnected";
    static final String GATE = "sourceItemAdmissionGate";
    static final int INVENTORY = 0;
    static final int RENTAL_TOWN = 1;
    static final int RENTAL_BACKUP = 2;
    static final int WAREHOUSE = 3;
    final GameHub hub;
    final NativePlayerLifetime life;
    final NativeDeItemTemplatesResources.Snapshot templates;
    final NativeDeItemDefinitionsResources.Snapshot definitions;
    volatile boolean closed;

    private NativeDeItemApplication(GameHub gameHub, NativeDeItemTemplatesResources.Snapshot snapshot, NativeDeItemDefinitionsResources.Snapshot snapshot2) {
        if (!snapshot.database().equals(snapshot2.database())) {
            throw new IllegalArgumentException("source item generations belong to different databases");
        }
        this.hub = gameHub;
        this.life = NativePlayerLifetime.of(gameHub);
        this.templates = snapshot;
        this.definitions = snapshot2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static NativeDeItemApplication install(GameHub gameHub, NativeDeItemTemplatesResources.Snapshot snapshot, NativeDeItemDefinitionsResources.Snapshot snapshot2) {
        NativeDeItemApplication nativeDeItemApplication;
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot2);
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            if (!NativePlayerLifetime.of(gameHub).admits()) {
                throw new IllegalStateException("item application is closed");
            }
            NativeDeItemApplication object = (NativeDeItemApplication)CombatAccess.field(gameHub, FIELD);
            if (object != null) {
                if (object.closed || object.templates != snapshot || object.definitions != snapshot2) {
                    throw new IllegalStateException("item application generation is immutable");
                }
                return object;
            }
            nativeDeItemApplication = new NativeDeItemApplication(gameHub, snapshot, snapshot2);
            CombatAccess.set(gameHub, FIELD, nativeDeItemApplication);
        }
        try {
            for (ClientSession clientSession : gameHub.connectedSessionsSnapshot()) {
                Owner owner = nativeDeItemApplication.owner(clientSession);
                if (owner == null) continue;
                Owner owner2 = owner;
                synchronized (owner2) {
                    for (int i = 0; i < 4; ++i) {
                        owner.adopt(clientSession.nativeDeInventory(i));
                    }
                    owner.refreshFields();
                }
            }
            return nativeDeItemApplication;
        }
        catch (Throwable throwable) {
            nativeDeItemApplication.close();
            throw throwable;
        }
    }

    static void bootstrap(GameHub gameHub, ServerConfig serverConfig) throws Exception {
        NativeDeItemTemplatesResources.Snapshot snapshot = new NativeDeItemTemplatesResources.Owner().reload(serverConfig);
        NativeDeItemDefinitionsResources.Snapshot snapshot2 = new NativeDeItemDefinitionsResources.Owner().reload(serverConfig);
        NativeDeItemApplication.install(gameHub, snapshot, snapshot2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Owner owner(ClientSession clientSession) {
        if (this.closed || !NativeCastMaterial.ownerLifetime(this.life, this, clientSession) || ((Boolean)CombatAccess.field(clientSession, DISCONNECTED)).booleanValue()) {
            return null;
        }
        Object object = CombatAccess.field(clientSession, GATE);
        synchronized (object) {
            if (((Boolean)CombatAccess.field(clientSession, DISCONNECTED)).booleanValue()) {
                return null;
            }
            Owner owner = (Owner)CombatAccess.field(clientSession, OWNER);
            if (owner != null && owner.active && owner.application.get() == this && owner.matches(clientSession)) {
                return owner;
            }
            if (owner != null) {
                owner.retire();
            }
            if (clientSession.selectedCharacterName().isBlank()) {
                return null;
            }
            Owner owner2 = new Owner(this, clientSession);
            CombatAccess.set(clientSession, OWNER, owner2);
            return owner2;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void replace(ClientSession clientSession, RuntimeInventory runtimeInventory, int n) {
        Owner owner;
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = gameHub == null ? null : (NativeDeItemApplication)CombatAccess.field(gameHub, FIELD);
        Owner owner2 = owner = nativeDeItemApplication == null ? null : nativeDeItemApplication.owner(clientSession);
        if (owner == null) {
            clientSession.nativeDeSetInventory(n, runtimeInventory);
            return;
        }
        Owner owner3 = owner;
        synchronized (owner3) {
            if (!owner.active || !owner.matches(clientSession)) {
                throw new IllegalStateException("stale item inventory assignment");
            }
            owner.adopt(runtimeInventory);
            RuntimeInventory runtimeInventory2 = clientSession.nativeDeInventory(n);
            if (runtimeInventory2 == null) {
                clientSession.nativeDeSetInventory(n, runtimeInventory);
                owner.refreshFields();
                return;
            }
            RuntimeInventory runtimeInventory3 = runtimeInventory2;
            synchronized (runtimeInventory3) {
                if (runtimeInventory != null) {
                    RuntimeInventory runtimeInventory4 = runtimeInventory;
                    synchronized (runtimeInventory4) {
                        Object nativeItems = runtimeInventory.nativeDeItems();
                        if (nativeItems instanceof NativeDeItemInventory inventory) {
                            inventory.validateReplacement(runtimeInventory2);
                        }
                        clientSession.nativeDeSetInventory(n, runtimeInventory);
                    }
                } else {
                    clientSession.nativeDeSetInventory(n, null);
                }
            }
            owner.refreshFields();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void retireClient(ClientSession clientSession) {
        if (clientSession == null) {
            return;
        }
        Object object = CombatAccess.field(clientSession, GATE);
        synchronized (object) {
            Owner owner = (Owner)CombatAccess.field(clientSession, OWNER);
            if (owner != null) {
                owner.retire();
                CombatAccess.set(clientSession, OWNER, null);
            }
        }
    }

    static void disconnect(ClientSession clientSession) {
        CombatAccess.set(clientSession, DISCONNECTED, true);
        NativeDeItemApplication.retireClient(clientSession);
    }

    static void closeHub(GameHub gameHub) {
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, FIELD);
        if (nativeDeItemApplication != null) {
            nativeDeItemApplication.close();
        }
    }

    private void close() {
        this.closed = true;
        for (ClientSession clientSession : this.hub.connectedSessionsSnapshot()) {
            NativeDeItemApplication.retireClient(clientSession);
        }
    }

    static Owner transactionOwner(ClientSession clientSession) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = gameHub == null ? null : (NativeDeItemApplication)CombatAccess.field(gameHub, FIELD);
        return nativeDeItemApplication == null ? null : nativeDeItemApplication.owner(clientSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Owner publicationOwner(ClientSession clientSession) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)CombatAccess.field(gameHub, FIELD);
        Object object = CombatAccess.field(clientSession, GATE);
        synchronized (object) {
            long l;
            if (((Boolean)CombatAccess.field(clientSession, DISCONNECTED)).booleanValue() || clientSession.selectedCharacterName().isBlank()) {
                return null;
            }
            Owner owner = (Owner)CombatAccess.field(clientSession, OWNER);
            if (owner != null && owner.active && owner.application.get() == nativeDeItemApplication && owner.matches(clientSession)) {
                return owner;
            }
            if (nativeDeItemApplication != null) {
                if (nativeDeItemApplication.closed) {
                    return null;
                }
                Owner owner2 = nativeDeItemApplication.owner(clientSession);
                if (owner2 != null) {
                    return owner2;
                }
                if (nativeDeItemApplication.closed || ((Boolean)CombatAccess.field(clientSession, DISCONNECTED)).booleanValue() || clientSession.selectedCharacterName().isBlank()) {
                    return null;
                }
            }
            if ((l = clientSession.playerObjectId()) <= 0L || l > 0xFFFFFFFFL) {
                return null;
            }
            if (owner != null) {
                owner.retire();
            }
            Owner owner3 = new Owner(nativeDeItemApplication, clientSession);
            CombatAccess.set(clientSession, OWNER, owner3);
            return owner3;
        }
    }

    static final class Owner {
        final WeakReference<NativeDeItemApplication> application;
        final WeakReference<ClientSession> session;
        final long objectId;
        final String selected;
        final String source;
        final String account;
        volatile boolean active = true;

        Owner(NativeDeItemApplication nativeDeItemApplication, ClientSession clientSession) {
            this.application = new WeakReference<NativeDeItemApplication>(nativeDeItemApplication);
            this.session = new WeakReference<ClientSession>(clientSession);
            this.objectId = clientSession.playerObjectId();
            this.selected = clientSession.selectedCharacterName();
            this.source = clientSession.sourceCharacterIdentity();
            this.account = clientSession.sourceAccountIdentity();
            if (this.objectId <= 0L || this.objectId > 0xFFFFFFFFL) {
                throw new IllegalArgumentException("native item owner requires unsigned dword player identity");
            }
        }

        boolean matches(ClientSession clientSession) {
            return this.session.get() == clientSession && (Boolean)CombatAccess.field(clientSession, NativeDeItemApplication.DISCONNECTED) == false && this.objectId == clientSession.playerObjectId() && this.selected.equals(clientSession.selectedCharacterName()) && this.source.equals(clientSession.sourceCharacterIdentity()) && this.account.equals(clientSession.sourceAccountIdentity());
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void adopt(RuntimeInventory runtimeInventory) {
            if (runtimeInventory == null) {
                return;
            }
            NativeDeItemApplication nativeDeItemApplication = Objects.requireNonNull((NativeDeItemApplication)this.application.get(), "item application released");
            if (!this.active || nativeDeItemApplication.closed || !NativeCastMaterial.adoptLifetime(nativeDeItemApplication.life, this, runtimeInventory)) {
                throw new IllegalStateException("item owner retired");
            }
            RuntimeInventory runtimeInventory2 = runtimeInventory;
            synchronized (runtimeInventory2) {
                NativeDeItemInventory nativeDeItemInventory = NativeDeItemInventory.bind(runtimeInventory, nativeDeItemApplication.templates, nativeDeItemApplication.definitions);
                if (nativeDeItemInventory.rentalCharacter != null) {
                    if (!nativeDeItemInventory.rentalCharacter.equals(this.source.toLowerCase(Locale.ROOT)) || nativeDeItemInventory.rentalAccount != null && !nativeDeItemInventory.rentalAccount.equals(this.account.toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("rental capsule identity differs from authenticated owner");
                    }
                    nativeDeItemInventory.rentalCharacter = null;
                    nativeDeItemInventory.rentalAccount = null;
                }
                if (nativeDeItemInventory.owner != null && nativeDeItemInventory.owner != this) {
                    throw new IllegalArgumentException("inventory belongs to another character admission");
                }
                nativeDeItemInventory.owner = this;
            }
        }

        void requireActive(RuntimeInventory runtimeInventory) {
            int n;
            NativeDeItemApplication nativeDeItemApplication = (NativeDeItemApplication)this.application.get();
            ClientSession clientSession = (ClientSession)this.session.get();
            if (!this.active || nativeDeItemApplication == null || nativeDeItemApplication.closed || !nativeDeItemApplication.life.admits() || clientSession == null || !this.matches(clientSession) || CombatAccess.field(clientSession, NativeDeItemApplication.OWNER) != this) {
                throw new IllegalStateException("source item owner is stale");
            }
            boolean bl = (Boolean)CombatAccess.field(clientSession, "rentalTownRoomState") != false && String.valueOf(CombatAccess.field(clientSession, "phase")).equals("BATTLE");
            int n2 = n = bl ? 1 : 0;
            if (clientSession.nativeDeInventory(n) != runtimeInventory) {
                throw new IllegalStateException("source item inventory is staged or inactive");
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void refreshFields() {
            ClientSession clientSession = (ClientSession)this.session.get();
            if (clientSession == null) {
                return;
            }
            boolean bl = (Boolean)CombatAccess.field(clientSession, "rentalTownRoomState") != false && String.valueOf(CombatAccess.field(clientSession, "phase")).equals("BATTLE");
            RuntimeInventory runtimeInventory = clientSession.nativeDeInventory(bl ? 1 : 0);
            for (int i = 0; i < 4; ++i) {
                RuntimeInventory runtimeInventory2 = clientSession.nativeDeInventory(i);
                if (runtimeInventory2 == null) continue;
                RuntimeInventory runtimeInventory3 = runtimeInventory2;
                synchronized (runtimeInventory3) {
                    Map map = runtimeInventory2.nativeDeItems();
                    if (map instanceof NativeDeItemInventory) {
                        NativeDeItemInventory nativeDeItemInventory = (NativeDeItemInventory)map;
                        if (nativeDeItemInventory.owner == this) {
                            nativeDeItemInventory.ownerFields(runtimeInventory2 == runtimeInventory ? (int)this.objectId : 0);
                        }
                    }
                    continue;
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        synchronized void retire() {
            this.active = false;
            ClientSession clientSession = (ClientSession)this.session.get();
            if (clientSession != null) {
                for (int i = 0; i < 4; ++i) {
                    RuntimeInventory runtimeInventory = clientSession.nativeDeInventory(i);
                    if (runtimeInventory == null) continue;
                    RuntimeInventory runtimeInventory2 = runtimeInventory;
                    synchronized (runtimeInventory2) {
                        continue;
                    }
                }
            }
        }
    }
}
