package local.lito381.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/** Same server artifact, explicitly selected modern mode before any native startup/migration. */
final class ModernMain {
    private ModernMain() {}

    static void run(String[] arguments) throws IOException {
        run(arguments, null, System.out);
    }

    static void run(String[] arguments, java.io.InputStream console, java.io.PrintStream output) throws IOException {
        if (arguments.length != 2 || !"--modern-config".equals(arguments[0])) {
            throw new IOException("usage: Main --modern-config <absolute modern properties path>");
        }
        try (var config = ModernServerConfig.load(Path.of(arguments[1]), System::getenv)) {
            ModernServer server = start(config);
            Thread shutdown = new Thread(() -> stopAndJoin(server), "modern-shutdown");
            boolean registered = false;
            try {
                Runtime.getRuntime().addShutdownHook(shutdown);
                registered = true;
                output.println("[Modern LiTo] authentication/roster listener ready at " + server.address());
                output.flush();
                if (console != null) {
                    Thread operator = new Thread(() -> ModernServerConsole.run(console, output, server::close,
                            () -> ModernServerConsole.status(server, "MODERN_STATUS")), "modern-local-console");
                    operator.setDaemon(true);
                    operator.start();
                }
                while (!server.awaitTermination(1000)) {
                    // Server owns the accept/session/backend threads. This wait is
                    // not a UI loop, and a closed listener alone is not termination.
                }
                if (server.listenerFailed()) throw new IOException("modern listener stopped unexpectedly");
            } finally {
                stopAndJoin(server);
                if (console != null) {
                    output.println(ModernServerConsole.status(server, "MODERN_STOPPED"));
                    output.flush();
                }
                if (registered) {
                    try { Runtime.getRuntime().removeShutdownHook(shutdown); }
                    catch (IllegalStateException shuttingDown) { /* Hook is already running. */ }
                }
            }
        } catch (Exception failure) {
            // Configuration/JDBC/provider exceptions can contain secret values.
            // This entry point must not forward messages, causes or SQL URLs.
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IOException("modern server startup or termination failed; check its explicit configuration");
        }
    }

    /** Testable production wiring; caller must keep config alive until every owned worker exits. */
    static ModernServer start(ModernServerConfig config) throws Exception {
        if(config.matchmakingEnabled() && (config.settlement()==null || !config.worlds().enabled()
                || config.equipmentStoreDirectory()==null))throw new IOException("matchmaking requires explicit central records, equipment and shared worlds");
        if (config.settlement() != null && (!config.worlds().enabled() || !config.worlds().combatEnabled()
                || config.equipmentStoreDirectory() == null
                || config.sources().stream().anyMatch(source -> source.catalogDatabase() == null)))
            throw new IOException("settlement requires explicit combat, equipment and source catalogs");
        var tls = ModernTlsIdentity.load(config.tls().keyStore(), config.tls().password());
        var repositories = new TreeMap<Integer, ModernJdbcBackend>();
        var bindings = new TreeMap<Integer, ModernAccountAuthentication.SourceBinding>();
        var dataSources = new TreeMap<Integer, ModernCharacterDataRepository.Source>();
        var skillsSources = new TreeMap<Integer, ModernSkillsRepository.Source>();
        var disintegrateSources = new TreeMap<Integer, ModernDisintegrateRepository.Source>();
        var greaterHasteSources = new TreeMap<Integer, ModernGreaterHasteRepository.Source>();
        var hasteSources = new TreeMap<Integer, ModernHasteRepository.Source>();
        var slowSources = new TreeMap<Integer, ModernSlowRepository.Source>();
        var gmSources = new TreeMap<Integer, ModernGmCommandRepository.Source>();
        var shopSources = new TreeMap<Integer, ModernShopCatalogRepository.Source>();
        var equipmentSources = new TreeMap<Integer, ModernEquipmentRepository.Source>();
        for (var source : config.sources()) {
            var repository = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), config.dbNetworkMillis());
            repositories.put(source.id(), repository);
            bindings.put(source.id(), ModernAccountAuthentication.SourceBinding.direct(source.name(), repository::authenticate));
            if (source.catalogDatabase() != null) dataSources.put(source.id(),
                    new ModernCharacterDataRepository.Source(repository, source.catalogDatabase()));
            if (source.catalogDatabase() != null) equipmentSources.put(source.id(), new ModernEquipmentRepository.Source(source, repository));
            if (source.catalogDatabase() != null) skillsSources.put(source.id(), new ModernSkillsRepository.Source(repository,source.catalogDatabase(),config.worlds().fireStormEnabled()||config.worlds().npcFireStormEnabled()));
            if(source.catalogDatabase()!=null)disintegrateSources.put(source.id(),new ModernDisintegrateRepository.Source(repository,source.catalogDatabase()));
            if(source.catalogDatabase()!=null)greaterHasteSources.put(source.id(),new ModernGreaterHasteRepository.Source(repository,source.catalogDatabase()));
            if(source.catalogDatabase()!=null)hasteSources.put(source.id(),new ModernHasteRepository.Source(repository,source.catalogDatabase()));
            if(source.catalogDatabase()!=null)slowSources.put(source.id(),new ModernSlowRepository.Source(repository,source.catalogDatabase()));
            if (source.catalogDatabase() != null) gmSources.put(source.id(),new ModernGmCommandRepository.Source(source,repository));
            if(config.shopAllowance()>0 && source.catalogDatabase()!=null)shopSources.put(source.id(),new ModernShopCatalogRepository.Source(source,repository));
        }
        // Source accounts' own banned field is enforced by the JDBC auth query.
        // GameHub/state-store global moderation is not silently imported here;
        // it belongs to the forthcoming modern gameplay-session integration.
        var verifier = ModernAccountAuthentication.repositories(Map.copyOf(bindings), account -> false);
        var characters = ModernJdbcBackend.characters(Map.copyOf(repositories));
        var limits = new ModernServer.Limits(config.maxSessions(), config.maxPerIp(),
                config.acceptPerMinute(), config.acceptBurst(), config.backendWorkers(), config.backendQueue(),
                config.backendDeadlineMillis(), config.handshakeMillis(), config.frameMillis(), config.applicationReadMillis());
        var data = dataSources.isEmpty() ? ModernCharacterDataRepository.unavailable() : ModernCharacterDataRepository.jdbc(dataSources);
        var skills = skillsSources.isEmpty() ? ModernSkillsRepository.unavailable() : ModernSkillsRepository.jdbc(skillsSources);
        if((config.worlds().disintegrateEnabled()||config.worlds().npcDisintegrateEnabled())&&disintegrateSources.size()!=config.sources().size())throw new IOException("Disintegrate requires every explicit source catalog");
        var disintegrate=(config.worlds().disintegrateEnabled()||config.worlds().npcDisintegrateEnabled())?ModernDisintegrateRepository.jdbc(disintegrateSources):null;
        if(config.worlds().greaterHasteEnabled()&&greaterHasteSources.size()!=config.sources().size())throw new IOException("greater haste requires every explicit source catalog");
        if(config.worlds().hasteEnabled()&&hasteSources.size()!=config.sources().size())throw new IOException("haste requires every explicit source catalog");
        var greaterHaste=config.worlds().greaterHasteEnabled()?ModernGreaterHasteRepository.jdbc(greaterHasteSources):null;
        var haste=config.worlds().hasteEnabled()?ModernHasteRepository.jdbc(hasteSources):null;
        if(config.worlds().slowEnabled()&&slowSources.size()!=config.sources().size())throw new IOException("slow requires every explicit source catalog");
        var slow=config.worlds().slowEnabled()?ModernSlowRepository.jdbc(slowSources):null;
        var gm = gmSources.isEmpty() ? ModernGmCommandRepository.unavailable() : ModernGmCommandRepository.jdbc(gmSources);
        var shop = shopSources.isEmpty()?ModernShopCatalogRepository.unavailable():ModernShopCatalogRepository.jdbc(shopSources,config.worlds().bowEnabled());
        ModernEquipmentOverrideStore store = null;
        ModernWorldService worlds = null;
        ModernSettlementConnections settlementConnections = null;
        ModernSettlementConnections recordConnections = null;
        ModernCharacterRecordRepository records = ModernCharacterRecordRepository.unavailable();
        ModernMatchOutbox outbox = null;
        ModernMatchSettlement.JournaledStore coordinator = null;
        ModernSettlementConnections raceConnections=null;
        ModernPetRaceOutbox raceOutbox=null;
        ModernPetRaceSettlement.JournaledStore raceCoordinator=null;
        boolean transferred = false;
        try {
            if (config.worlds().combatEnabled() && (config.equipmentStoreDirectory() == null
                    || equipmentSources.size() != config.sources().size()))
                throw new IOException("combat requires explicit equipment storage and every source catalog");
            var mappings = loadMappings(config);
            if(config.shopAllowance()>0 && (shopSources.size()!=config.sources().size() || mappings.isEmpty()
                    || mappings.values().stream().noneMatch(ModernWorldService.Mapping::matchItems)))
                throw new IOException("shop requires every explicit source catalog and an item-enabled mapping");
            if (config.settlement() != null && (mappings.isEmpty()
                    || mappings.values().stream().noneMatch(m -> m.combatEnabled()&&(m.binding().key().mode()==1||m.honor()!=null||m.timeAttack()!=null||m.siege()!=null
                        ||m.npc()!=null&&m.npc().questPlan()!=null&&m.npc().questPlan().objectives()!=null))))
                throw new IOException("settlement requires an explicit elimination, qualified Honor, Siege, Quest or Time Attack mapping");
            var rooms = new ModernRooms(mappings.keySet(),mappings.entrySet().stream().filter(entry -> entry.getValue().serverBots())
                    .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()),mappings.entrySet().stream().filter(e->e.getValue().playerCapacity()!=100)
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().playerCapacity())),
                    new ModernChat(),new ModernSpeechAuthority(ModernSpeechAuthority.sources(config)));
            if (config.settlement() != null) {
                var settlement = config.settlement();
                settlementConnections = new ModernSettlementConnections(settlement.database(), config.dbConnectMillis(), config.dbNetworkMillis(), 2);
                byte[] binding = ModernSettlementCoordinator.binding(config);
                try { outbox = new ModernMatchOutbox(settlement.outboxDirectory(), binding); }
                finally { Arrays.fill(binding, (byte) 0); }
                var sql = new ModernMatchSettlementJdbc(settlementConnections, settlement.database().database());
                coordinator = new ModernSettlementCoordinator(outbox, sql, config.backendDeadlineMillis(), settlementConnections);
                // Independent read admission: menu refreshes must not occupy the
                // writer's two commit/recovery slots. No connection opens here.
                recordConnections = new ModernSettlementConnections(settlement.database(), config.dbConnectMillis(), config.dbNetworkMillis(),
                        Math.min(4, config.backendWorkers()));
                records = ModernCharacterRecordRepository.jdbc(recordConnections, settlement.database().database());
            }
            if(config.raceSettlement()!=null) {
                var settlement=config.raceSettlement();
                if(mappings.values().stream().noneMatch(m->m.race()!=null))throw new IOException("race settlement requires an explicit qualified course");
                raceConnections=new ModernSettlementConnections(settlement.database(),config.dbConnectMillis(),config.dbNetworkMillis(),2);
                byte[] route=ModernPetRaceSettlement.binding(config);
                try{raceOutbox=new ModernPetRaceOutbox(settlement.outboxDirectory(),route);}finally{Arrays.fill(route,(byte)0);}
                raceCoordinator=new ModernPetRaceSettlementCoordinator(raceOutbox,new ModernPetRaceSettlementJdbc(raceConnections,settlement.database().database()),
                        config.backendDeadlineMillis(),raceConnections);
            }
            if (!mappings.isEmpty()) {
                worlds = new ModernWorldService(rooms, mappings,
                        new ModernWorldService.Limits(100, 2, 16, 30_000, 50,
                                new ModernBattleEngine.Limits(30_000, 3_000_000_000L, 300_000)),
                        bound -> java.util.concurrent.ThreadLocalRandom.current().nextLong(bound), coordinator,
                        config.worlds().weightEnabled()?config.sources().stream().collect(java.util.stream.Collectors.toMap(
                                ModernServerConfig.SourceConfig::id,ModernServerConfig.SourceConfig::weightRate)):Map.of(),raceCoordinator);
                // Only a successfully constructed world owns the coordinator.
                // Prior constructor failures still unwind the local owners below.
                coordinator = null; outbox = null; settlementConnections = null;
                raceCoordinator=null;raceOutbox=null;raceConnections=null;
            }
            ModernEquipmentAuthority equipment = null;
            ModernVipEquipmentAuthority vipEquipment = null;
            if (config.equipmentStoreDirectory() != null) {
                store = new ModernEquipmentOverrideStore(config.equipmentStoreDirectory());
                equipment = config.worlds().combatEnabled()
                        ? new ModernEquipmentAuthority(ModernEquipmentRepository.jdbc(equipmentSources),
                            ModernEquipmentRepository.combatJdbc(equipmentSources), ModernEquipmentRepository.physicalJdbc(equipmentSources, config.worlds().magicQualificationEnabled(),config.worlds().bowEnabled(),config.worlds().regenEnabled(),config.worlds().honorEnabled(),config.worlds().slowEnabled(),config.worlds().playerHealingEnabled(),config.worlds().armorProtectionEnabled()), store)
                        : new ModernEquipmentAuthority(ModernEquipmentRepository.jdbc(equipmentSources), store);
                if(config.worlds().fullVipEnabled()) {
                    var selected=equipmentSources.entrySet().stream().filter(e->e.getValue().config().fullVip())
                            .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,Map.Entry::getValue));
                    var provider=ModernEquipmentRepository.fullVipJdbc(selected,config.worlds().magicQualificationEnabled(),
                            config.worlds().bowEnabled(),config.worlds().regenEnabled(),config.worlds().honorEnabled(),
                            config.worlds().slowEnabled(),config.worlds().playerHealingEnabled(),config.worlds().armorProtectionEnabled());
                    vipEquipment=new ModernVipEquipmentAuthority(provider,store,System::currentTimeMillis);
                }
            }
            var server = new ModernServer(config.bind(), tls, limits, verifier, characters, new TournamentPresence(), data, equipment, store, rooms, worlds,
                    ModernSessionProtocol.MAX_VERSION,skills,records,recordConnections,gm,shop,config.shopAllowance(),disintegrate,config.matchmakingEnabled(),greaterHaste,haste,slow,vipEquipment);
            transferred = true;
            return server;
        } finally {
            if (!transferred) {
                try {
                    if (worlds != null) {
                        worlds.close();
                        boolean interrupted = false;
                        for (;;) {
                            try { if (worlds.awaitTermination(1000)) break; }
                            catch (InterruptedException waiting) { interrupted = true; }
                        }
                        if (interrupted) Thread.currentThread().interrupt();
                    }
                    if (coordinator != null) stopAndJoin(coordinator);
                    else try {
                        if (outbox != null) {
                            outbox.close();
                            // No coordinator took ownership, so no asynchronous outbox
                            // work was admitted. Close must still release its OS lock.
                            if (!outbox.isQuiescent()) throw new IOException("settlement outbox did not retire");
                        }
                    } finally { if (settlementConnections != null) stopAndJoin(settlementConnections); }
                } finally {
                    try {
                        if(raceCoordinator!=null)stopAndJoin(raceCoordinator);
                        else try{if(raceOutbox!=null){raceOutbox.close();if(!raceOutbox.isQuiescent())throw new IOException("race outbox did not retire");}}
                        finally{if(raceConnections!=null)stopAndJoin(raceConnections);}
                    }finally{
                        try { if (store != null) store.close(); }
                        finally { if (recordConnections != null) stopAndJoin(recordConnections); }
                    }
                }
            }
        }
    }

    /** Same startup-only path for the listener and the explicit read-only qualification probe. */
    static Map<ModernWorldBinding.Key,ModernWorldService.Mapping> loadMappings(ModernServerConfig config)throws Exception {
        if(config.worldPolicy()==null)return config.worlds().load(null,()->Thread.currentThread().isInterrupted(),ModernNpcConfiguration.load(config));
        return loadWorldPolicy(config,false).mappings();
    }
    record QualifiedWorldPolicy(ModernWorldPolicy.Snapshot snapshot,Map<ModernWorldBinding.Key,ModernWorldService.Mapping> mappings) {
        QualifiedWorldPolicy{mappings=Map.copyOf(mappings);}
    }
    /** Candidate inspection has no call path to server construction; strict loadMappings always passes false. */
    static QualifiedWorldPolicy loadWorldPolicy(ModernServerConfig config,boolean candidate)throws Exception {
        var policy=config.worldPolicy();
        if(policy==null)throw new IOException("explicit world policy required");
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(policy.deadlineMillis());
        ModernWorldPolicy.Snapshot snapshot;
        var connections=new ModernSettlementConnections(policy.database(),config.dbConnectMillis(),config.dbNetworkMillis(),1);
        try {var reader=new ModernWorldPolicyJdbc(connections,policy.database().database());snapshot=candidate?reader.readCandidate(policy.pin(),deadline):reader.read(policy.pin(),deadline);}
        finally{stopAndJoin(connections);}
        var npcs=ModernNpcConfiguration.load(config,deadline);
        var mappings=config.worlds().load(snapshot,()->Thread.currentThread().isInterrupted()||deadline-System.nanoTime()<=0,npcs);
        return new QualifiedWorldPolicy(snapshot,mappings);
    }

    private static void stopAndJoin(ModernMatchSettlement.JournaledStore store) {
        store.close(); boolean interrupted = false;
        try {
            for (;;) try { if (store.awaitTermination(1000)) return; }
            catch (InterruptedException waiting) { interrupted = true; }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static void stopAndJoin(ModernPetRaceSettlement.JournaledStore store) {
        store.close();boolean interrupted=false;
        try{for(;;)try{if(store.awaitTermination(1000))return;}catch(InterruptedException waiting){interrupted=true;}}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void stopAndJoin(ModernSettlementConnections connections) {
        connections.close(); boolean interrupted = false;
        try {
            for (;;) try { if (connections.awaitTermination(1000)) return; }
            catch (InterruptedException waiting) { interrupted = true; }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }

    private static void stopAndJoin(ModernServer server) {
        server.close();
        boolean interrupted = false;
        try {
            for (;;) {
                try { if (server.awaitTermination(1000)) return; }
                catch (InterruptedException waitInterrupted) { interrupted = true; }
                // Do not destroy config secrets or report shutdown while a JDBC
                // worker still owns them. Its socket/deadline must actually retire.
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
}
