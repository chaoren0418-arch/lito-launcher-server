package local.lito381.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Modern-only listener. Owns every accepted socket/session/backend worker; no native GameHub startup. */
final class ModernServer implements AutoCloseable {
    static ModernSessionExtensions.Offer extensionsFor(ModernWorldService worlds) {
        int bits=worlds==null?0:(worlds.playerHealingEnabled()?ModernSessionExtensions.HEALING_MASK:0)
                |(worlds.armorProtectionEnabled()?ModernSessionExtensions.ARMOR_PROTECTION_DATA|ModernSessionExtensions.ARMOR_PRESENTATION_DATA:0);
        if(worlds!=null&&worlds.fullVipEnabled())bits|=ModernSessionExtensions.FULL_VIP_DATA|ModernSessionExtensions.FULL_VIP_EQUIPMENT|ModernSessionExtensions.FULL_VIP_WORLD|ModernSessionExtensions.FULL_VIP_APPEARANCE;
        if(worlds!=null&&worlds.nativeResourcesEnabled())bits|=ModernSessionExtensions.NATIVE_WORLD_RESOURCES_V1;
        if(worlds!=null&&worlds.nativeCatalogEnabled())bits|=ModernSessionExtensions.NATIVE_CATALOG_RESOURCES_V1;
        if(worlds!=null&&worlds.nativeActorEnabled())bits|=ModernSessionExtensions.NATIVE_ACTOR_DEPENDENCIES_V1;
        return new ModernSessionExtensions.Offer(bits,0);
    }
    private final int maximumVersion;
    private final boolean matchmakingEnabled;
    record Limits(int maxSessions, int maxPerIp, int acceptPerMinute, int acceptBurst,
                  int backendWorkers, int backendQueue, int backendMillis,
                  int handshakeMillis, int frameMillis, int applicationReadMillis) {
        Limits {
            if (maxSessions < 1 || maxSessions > 256 || maxPerIp < 1 || maxPerIp > maxSessions
                    || acceptPerMinute < 1 || acceptPerMinute > 3600 || acceptBurst < 1 || acceptBurst > maxSessions
                    || backendWorkers < 1 || backendWorkers > 32 || backendQueue < 0 || backendQueue > 256
                    || backendMillis < 100 || backendMillis > 30_000
                    || applicationReadMillis < 100 || applicationReadMillis > 30_000) {
                throw new IllegalArgumentException("invalid bounded modern listener limits");
            }
            new ModernTlsSessionTransport.Limits(handshakeMillis, frameMillis);
        }
    }
    record Snapshot(int sessions, int sourceAddressEntries, int backendActive, int backendQueued,
                    long accepted, long rejected, long failed, boolean closing) {}

    private final Limits limits;
    private final ModernAccountAuthentication.CredentialVerifier verifier;
    private final ModernCharacterRepository characters;
    private final TournamentPresence tournamentPresence;
    private final ModernCharacterDataRepository characterData;
    private final ModernSkillsRepository skills;
    private final ModernDisintegrateRepository disintegrateRepository;
    private final ModernGreaterHasteRepository greaterHasteRepository;
    private final ModernHasteRepository hasteRepository;
    private final ModernSlowRepository slowRepository;
    private final ModernCharacterRecordRepository records;
    private final ModernGmCommandRepository gmRepository;
    private final ModernShopCatalogRepository shopRepository;
    private final long shopAllowance;
    private final ModernSettlementConnections ownedRecordConnections;
    private final ModernEquipmentAuthority equipment;
    private final ModernVipEquipmentAuthority vipEquipment;
    private final ModernEquipmentOverrideStore ownedEquipmentStore;
    private final ModernRooms rooms;
    private final ModernWorldService worlds;
    private final SSLServerSocket listener;
    private final ModernBackendExecutor backend;
    private final ThreadPoolExecutor sessions;
    private final ConcurrentHashMap<SessionTask, Boolean> owned = new ConcurrentHashMap<>();
    private final Admission admission;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean listenerFailed = new AtomicBoolean();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final ModernSessionFailures failureDiagnostics = new ModernSessionFailures();
    private final CountDownLatch acceptEnded = new CountDownLatch(1);
    private final Thread acceptThread;

    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters)
            throws IOException {
        this(bind, tls, limits, verifier, characters, new TournamentPresence());
    }

    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence) throws IOException {
        this(bind, tls, limits, verifier, characters, tournamentPresence, ModernCharacterDataRepository.unavailable());
    }

    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData) throws IOException {
        this(bind, tls, limits, verifier, characters, tournamentPresence, characterData, null);
    }

    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment) throws IOException {
        this(bind, tls, limits, verifier, characters, tournamentPresence, characterData, equipment, null);
    }

    /** Store ownership transfers only after successful construction. */
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,null);
    }

    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,null);
    }

    /** World service ownership also transfers only on successful construction. */
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds) throws IOException {
        this(bind, tls, limits, verifier, characters, tournamentPresence, characterData, equipment, ownedEquipmentStore,
                rooms, worlds, ModernSessionProtocol.MAX_VERSION);
    }
    /** Explicit rollout/test ceiling; cannot enable capabilities not supported by the wired services. */
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds, int maximumVersion) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,ModernSkillsRepository.unavailable());
    }
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds, int maximumVersion, ModernSkillsRepository skills) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,skills,ModernCharacterRecordRepository.unavailable(),null);
    }
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds, int maximumVersion, ModernSkillsRepository skills,
                 ModernCharacterRecordRepository records, ModernSettlementConnections ownedRecordConnections) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,skills,records,ownedRecordConnections,ModernGmCommandRepository.unavailable());
    }
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds, int maximumVersion, ModernSkillsRepository skills,
                 ModernCharacterRecordRepository records, ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,skills,records,ownedRecordConnections,gmRepository,ModernShopCatalogRepository.unavailable(),0);
    }
    ModernServer(InetSocketAddress bind, SSLContext tls, Limits limits,
                 ModernAccountAuthentication.CredentialVerifier verifier, ModernCharacterRepository characters,
                 TournamentPresence tournamentPresence, ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment, ModernEquipmentOverrideStore ownedEquipmentStore, ModernRooms rooms,
                 ModernWorldService worlds, int maximumVersion, ModernSkillsRepository skills,
                 ModernCharacterRecordRepository records, ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance) throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,maximumVersion,
                skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,null);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository)throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,disintegrateRepository,false);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                 boolean matchmakingEnabled)throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,
                maximumVersion,skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,disintegrateRepository,matchmakingEnabled,null);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                 boolean matchmakingEnabled,ModernGreaterHasteRepository greaterHasteRepository)throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,maximumVersion,skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,disintegrateRepository,matchmakingEnabled,greaterHasteRepository,null);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                 boolean matchmakingEnabled,ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository)throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,maximumVersion,skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,disintegrateRepository,matchmakingEnabled,greaterHasteRepository,hasteRepository,null);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                 boolean matchmakingEnabled,ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository)throws IOException {
        this(bind,tls,limits,verifier,characters,tournamentPresence,characterData,equipment,ownedEquipmentStore,rooms,worlds,maximumVersion,skills,records,ownedRecordConnections,gmRepository,shopRepository,shopAllowance,disintegrateRepository,matchmakingEnabled,greaterHasteRepository,hasteRepository,slowRepository,null);
    }
    ModernServer(InetSocketAddress bind,SSLContext tls,Limits limits,ModernAccountAuthentication.CredentialVerifier verifier,
                 ModernCharacterRepository characters,TournamentPresence tournamentPresence,ModernCharacterDataRepository characterData,
                 ModernEquipmentAuthority equipment,ModernEquipmentOverrideStore ownedEquipmentStore,ModernRooms rooms,ModernWorldService worlds,
                 int maximumVersion,ModernSkillsRepository skills,ModernCharacterRecordRepository records,ModernSettlementConnections ownedRecordConnections,
                 ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                 boolean matchmakingEnabled,ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository,
                 ModernVipEquipmentAuthority vipEquipment)throws IOException {
        if (maximumVersion < 1 || maximumVersion > ModernSessionProtocol.MAX_VERSION) throw new IllegalArgumentException("invalid protocol ceiling");
        if(vipEquipment!=null&&(worlds==null||!worlds.fullVipEnabled())
                ||worlds!=null&&worlds.fullVipEnabled()&&(vipEquipment==null||maximumVersion!=47||equipment==null||rooms==null||shopAllowance<=0
                    ||!worlds.combatEnabled()||!worlds.weightEnabled()||!worlds.timedEnabled()||!worlds.regenEnabled()))
            throw new IllegalArgumentException("complete VIP requires explicit source and v47 world inventory authorities");
        this.vipEquipment=vipEquipment;
        if(worlds!=null&&(worlds.playerHealingEnabled()||worlds.armorProtectionEnabled())&&(maximumVersion!=47||equipment==null||rooms==null||shopAllowance<=0
                ||!worlds.combatEnabled()||!worlds.spellsEnabled()||!worlds.weightEnabled()||!worlds.timedEnabled()||!worlds.regenEnabled()))
            throw new IllegalArgumentException("extended battle data requires complete v47 world and inventory authorities");
        if(matchmakingEnabled && (maximumVersion<34 || worlds==null || rooms==null || equipment==null || records==null))
            throw new IllegalArgumentException("matchmaking requires v34 and explicit world, room, equipment and record authorities");
        this.matchmakingEnabled=matchmakingEnabled;
        this.maximumVersion = maximumVersion;
        this.limits = Objects.requireNonNull(limits);
        this.verifier = Objects.requireNonNull(verifier);
        this.characters = Objects.requireNonNull(characters);
        this.tournamentPresence = Objects.requireNonNull(tournamentPresence);
        this.characterData = Objects.requireNonNull(characterData);
        this.skills = Objects.requireNonNull(skills);
        this.disintegrateRepository=disintegrateRepository;
        this.greaterHasteRepository=greaterHasteRepository;
        this.hasteRepository=hasteRepository;
        this.slowRepository=slowRepository;
        if(worlds!=null&&worlds.slowEnabled()&&(slowRepository==null||maximumVersion<ModernSlowProtocol.VERSION))
            throw new IllegalArgumentException("slow requires explicit source authority and v47");
        if(worlds!=null&&worlds.greaterHasteEnabled()&&(greaterHasteRepository==null||maximumVersion<42))
            throw new IllegalArgumentException("greater haste requires explicit source authority and v42");
        if(worlds!=null&&worlds.hasteEnabled()&&(hasteRepository==null||maximumVersion<44))
            throw new IllegalArgumentException("haste requires explicit source authority and v44");
        if(worlds!=null&&worlds.disintegrateEnabled()&&(disintegrateRepository==null||maximumVersion<27))
            throw new IllegalArgumentException("Disintegrate requires explicit source authority and v27");
        if(worlds!=null&&worlds.npcDisintegrateEnabled()&&(disintegrateRepository==null||maximumVersion<29))
            throw new IllegalArgumentException("NPC77 requires explicit source authority and v29");
        if(worlds!=null&&worlds.npcFireStormEnabled()&&maximumVersion<31)
            throw new IllegalArgumentException("NPC70 requires explicit source authority and v31");
        this.records = Objects.requireNonNull(records);
        this.gmRepository = Objects.requireNonNull(gmRepository);
        this.shopRepository=Objects.requireNonNull(shopRepository);
        if(shopAllowance<0 || shopAllowance>0 && (worlds==null || !worlds.combatEnabled() || equipment==null))
            throw new IllegalArgumentException("shop requires combat world services");
        if(worlds!=null && worlds.weightEnabled() && shopAllowance==0)
            throw new IllegalArgumentException("weight requires explicit shop allowance");
        this.shopAllowance=shopAllowance;
        this.ownedRecordConnections = ownedRecordConnections;
        if (ownedEquipmentStore != null && equipment == null) throw new IllegalArgumentException("store needs authority");
        this.equipment = equipment;
        this.ownedEquipmentStore = ownedEquipmentStore;
        this.rooms = rooms;
        this.worlds=worlds;
        if(worlds!=null && rooms==null)throw new IllegalArgumentException("world service needs rooms");
        if (bind == null || bind.isUnresolved() || bind.getAddress().isMulticastAddress()) {
            throw new IllegalArgumentException("resolved unicast/listen address required");
        }
        admission = new Admission(limits, System::nanoTime);
        backend = new ModernBackendExecutor(limits.backendWorkers(), limits.backendQueue());
        var threadNumber = new AtomicLong();
        sessions = new ThreadPoolExecutor(limits.maxSessions(), limits.maxSessions(), 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(limits.maxSessions()), task -> {
                    Thread thread = new Thread(task, "modern-session-" + threadNumber.incrementAndGet());
                    thread.setDaemon(false);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        SSLServerSocket opened = null;
        try {
            opened = (SSLServerSocket) Objects.requireNonNull(tls).getServerSocketFactory().createServerSocket();
            opened.setUseClientMode(false);
            opened.setNeedClientAuth(false);
            opened.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            opened.setReuseAddress(false);
            opened.bind(bind, Math.min(128, limits.maxSessions()));
            listener = opened;
            acceptThread = new Thread(this::accept, "modern-accept");
            acceptThread.setDaemon(false);
            acceptThread.start();
        } catch (IOException | RuntimeException | Error failure) {
            if (opened != null) try { opened.close(); } catch (IOException ignored) {}
            sessions.shutdown();
            backend.close();
            throw failure;
        }
    }

    InetSocketAddress address() { return (InetSocketAddress) listener.getLocalSocketAddress(); }
    boolean listenerFailed() { return listenerFailed.get(); }
    java.util.List<ModernSessionFailures.Count> failureCounts() { return failureDiagnostics.snapshot(); }
    Snapshot snapshot() {
        return new Snapshot(owned.size(), admission.addressCount(), backend.activeCount(), backend.queuedCount(),
                accepted.get(), rejected.get(), failed.get(), closing.get());
    }

    private void accept() {
        try {
            while (!closing.get()) {
                SSLSocket socket = (SSLSocket) listener.accept();
                if (closing.get() || !admission.reserve(socket.getInetAddress())) {
                    rejected.incrementAndGet();
                    abortSocket(socket);
                    continue;
                }
                SessionTask task = new SessionTask(socket);
                owned.put(task, Boolean.TRUE);
                if (closing.get()) {
                    task.requestClose();
                    task.release();
                    continue;
                }
                try {
                    socket.setTcpNoDelay(true);
                    sessions.execute(task);
                    accepted.incrementAndGet();
                } catch (IOException | RuntimeException failure) {
                    rejected.incrementAndGet();
                    task.requestClose();
                    task.release();
                }
            }
        } catch (IOException | RuntimeException failure) {
            if (!closing.get()) listenerFailed.set(true);
        } finally {
            close();
            acceptEnded.countDown();
        }
    }

    private final class SessionTask implements Runnable {
        private final SSLSocket socket;
        private final AtomicBoolean stop = new AtomicBoolean();
        private final AtomicBoolean released = new AtomicBoolean();
        private volatile ModernTlsSessionTransport transport;
        SessionTask(SSLSocket socket) { this.socket = socket; }
        @Override public void run() {
            try {
                if (stop.get() || closing.get()) return;
                var policy = worlds!=null ? ModernSessionBoundary.Policy.world(equipment!=null,worlds.combatEnabled(),shopAllowance>0,worlds.weightEnabled(),worlds.timedEnabled(),worlds.bowEnabled(),worlds.regenEnabled(),worlds.honorEnabled(),worlds.fireStormEnabled(),worlds.raceEnabled(),worlds.npcEnabled()) : rooms!=null ? ModernSessionBoundary.Policy.rooms(equipment!=null) : equipment == null
                        ? ModernSessionBoundary.Policy.current() : ModernSessionBoundary.Policy.equipment();
                if(worlds!=null&&worlds.disintegrateEnabled())policy=new ModernSessionBoundary.Policy(policy.supportedCapabilities()|ModernSessionProtocol.CAP_DISINTEGRATE,policy.requiredCapabilities(),policy.maxVersion());
                if(worlds!=null&&worlds.npcDisintegrateEnabled())policy=new ModernSessionBoundary.Policy(policy.supportedCapabilities()|ModernSessionProtocol.CAP_NPC_DISINTEGRATE,policy.requiredCapabilities(),policy.maxVersion());
                if(worlds!=null&&worlds.npcFireStormEnabled())policy=new ModernSessionBoundary.Policy(policy.supportedCapabilities()|ModernSessionProtocol.CAP_NPC_FIRE_STORM,policy.requiredCapabilities(),policy.maxVersion());
                // Chat routing is available with room ownership, independently
                // of world/SQL providers. Only an explicit v32 client offer opts in.
            if(rooms!=null)policy=policy.withChat().withDirectChat();
            if(rooms!=null&&rooms.supportsFilteredChat())policy=policy.withFilteredChat();
                if(matchmakingEnabled)policy=policy.withMatchmaking();
                if(worlds!=null&&worlds.largeMapsEnabled())policy=policy.withLargeMaps();
                // Available only to an explicit v35 client capability offer.
                // This read channel never enables new mutations or SET lanes.
                if(worlds!=null&&maximumVersion>=35)policy=policy.withBattleData();
                if(worlds!=null&&maximumVersion>=37)policy=policy.withBattleActions();
                if(worlds!=null&&worlds.greaterHasteEnabled())policy=policy.withGreaterHaste();
                if(worlds!=null&&worlds.hasteEnabled())policy=policy.withHasteData();
                if(worlds!=null&&worlds.slowEnabled())policy=policy.withSlowCommands();
                // Known bits are not enabled bits. Adding a codec must never
                // silently grant that feature to an existing listener.
                var extensions=extensionsFor(worlds);
                var opened = new ModernTlsSessionTransport(socket, policy.withMaxVersion(maximumVersion),extensions,
                        new ModernTlsSessionTransport.Limits(limits.handshakeMillis(), limits.frameMillis()), verifier);
                transport = opened;
                if (stop.get() || closing.get()) opened.failClosed();
                else new ModernApplicationSession(opened, characters, backend,
                        limits.applicationReadMillis(), limits.backendMillis(), tournamentPresence, characterData, equipment, rooms, worlds,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,slowRepository,vipEquipment).run();
            } catch (IOException | RuntimeException failure) {
                if (!stop.get() && !closing.get()) {
                    var current = transport;
                    var point = current == null ? ModernSessionFailures.point(ModernSessionFailures.Stage.SETUP, failure)
                            : current.failurePoint().orElseGet(() -> ModernSessionFailures.point(ModernSessionFailures.Stage.DISPATCH, failure));
                    failureDiagnostics.record(point);
                    failed.incrementAndGet();
                }
                // No raw backend/TLS exception text: it can include sensitive input.
            } finally {
                requestClose();
                release();
            }
        }
        void requestClose() {
            stop.set(true);
            var current = transport;
            // The transport owns TLS teardown and notifies the backend scope.
            // Before that owner exists, no handshake/credentials have started.
            if (current != null) current.failClosed();
            else abortSocket(socket);
        }
        void release() {
            if (released.compareAndSet(false, true)) {
                owned.remove(this);
                admission.release(socket.getInetAddress());
            }
        }
    }

    static void abortSocket(Socket socket) {
        try { socket.setSoLinger(true, 0); } catch (IOException ignored) {}
        try { socket.shutdownInput(); } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    /** Requests shutdown only; awaitTermination is the proof that workers really exited. */
    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        try { listener.close(); } catch (IOException ignored) {}
        for (SessionTask task : new ArrayList<>(owned.keySet())) task.requestClose();
        sessions.shutdown();
        backend.close();
        if(worlds!=null)worlds.close();
        if(ownedRecordConnections!=null)ownedRecordConnections.close();
    }

    boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0 || timeoutMillis > 60_000) throw new IllegalArgumentException("bounded wait required");
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        if (!acceptEnded.await(timeoutMillis, TimeUnit.MILLISECONDS)) return false;
        long remaining = Math.max(0, end - System.nanoTime());
        if (!sessions.awaitTermination(remaining, TimeUnit.NANOSECONDS)) return false;
        remaining = Math.max(0, end - System.nanoTime());
        if (!backend.awaitTermination(TimeUnit.NANOSECONDS.toMillis(remaining)) || !owned.isEmpty()) return false;
        remaining=Math.max(0,end-System.nanoTime());
        if(worlds!=null && !worlds.awaitTermination(TimeUnit.NANOSECONDS.toMillis(remaining)))return false;
        remaining=Math.max(0,end-System.nanoTime());
        if(ownedRecordConnections!=null && !ownedRecordConnections.awaitTermination(TimeUnit.NANOSECONDS.toMillis(remaining)))return false;
        // Cancellation is not worker retirement. Keep the OS writer identity
        // until every possible provider/store operation has actually returned.
        if (ownedEquipmentStore != null) {
            ownedEquipmentStore.close();
            return ownedEquipmentStore.isQuiescent();
        }
        return true;
    }

    /** Bounded global + per-IP connection rate and concurrency admission, before TLS/credentials. */
    static final class Admission {
        private final Limits limits;
        private final LongSupplier clock;
        private final int maximumAddresses;
        private final Map<InetAddress, Bucket> addresses = new LinkedHashMap<>();
        private final Bucket global;
        private int active;
        Admission(Limits limits, LongSupplier clock) {
            this.limits = limits;
            this.clock = clock;
            maximumAddresses = Math.min(1024, Math.max(16, limits.maxSessions() * 4));
            global = new Bucket(clock.getAsLong());
        }
        synchronized boolean reserve(InetAddress address) {
            long now = clock.getAsLong();
            global.refill(now);
            if (active >= limits.maxSessions() || global.tokens < 1) return false;
            Bucket bucket = addresses.get(address);
            if (bucket == null) {
                if (addresses.size() >= maximumAddresses) {
                    addresses.entrySet().removeIf(entry -> {
                        entry.getValue().refill(now);
                        return entry.getValue().active == 0 && entry.getValue().tokens >= limits.acceptBurst();
                    });
                }
                if (addresses.size() >= maximumAddresses) return false;
                bucket = new Bucket(now);
                addresses.put(address, bucket);
            }
            bucket.refill(now);
            if (bucket.active >= limits.maxPerIp() || bucket.tokens < 1) return false;
            global.tokens -= 1;
            bucket.tokens -= 1;
            bucket.active++;
            active++;
            return true;
        }
        synchronized void release(InetAddress address) {
            Bucket bucket = addresses.get(address);
            if (bucket == null || bucket.active <= 0 || active <= 0) throw new IllegalStateException("unbalanced admission release");
            bucket.active--;
            active--;
            // Retain spent tokens after disconnect; otherwise reconnect bypasses rate limiting.
        }
        synchronized int addressCount() { return addresses.size(); }
        private final class Bucket {
            private double tokens = limits.acceptBurst();
            private long last;
            private int active;
            Bucket(long now) { last = now; }
            void refill(long now) {
                long elapsed = now - last;
                if (elapsed > 0) {
                    tokens = Math.min(limits.acceptBurst(), tokens + elapsed * (limits.acceptPerMinute() / 60_000_000_000.0));
                    last = now;
                }
            }
        }
    }
}
