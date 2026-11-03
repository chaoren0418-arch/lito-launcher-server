package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--modern-config".equals(args[0])) {
            ModernMain.run(args);
            return;
        }
        Path configPath = Path.of(args.length > 0 ? args[0] : "config/server.properties");
        String serviceMode = "standard";
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (argument.startsWith("--service-mode=")) {
                serviceMode = argument.substring("--service-mode=".length());
            } else {
                throw new IllegalArgumentException("unsupported argument: " + argument);
            }
        }
        ServerConfig config = ServerConfig.load(configPath, serviceMode);
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        animations.requireBattleCapableSprite(61, BattleActionCodes.WALK);
        animations.requireBattleCapableSprite(48, BattleActionCodes.WALK);
        animations.requireBattleCapableSprite(BattleCompanion.GFX, BattleActionCodes.WALK);
        animations.requireWalkAction(460, BattleActionCodes.WALK);
        animations.requireWalkAction(782, BattleActionCodes.WALK);
        animations.requireWalkAction(971, BattleActionCodes.WALK);
        animations.requireWalkAction(1766, BattleActionCodes.WALK);
        animations.requireWalkAction(CastleMercenaryCatalog.COMMANDER_GFX,
                BattleActionCodes.WALK);
        ExecutorService clients = Executors.newCachedThreadPool();
        List<ServerSocket> gameServers = new ArrayList<>();
        GameStateStore state = new GameStateStore(
                config.stateFile(), config.statePersistenceEnabled(),
                config.ephemeralSourceProfiles());
        try (AccountRepository accounts = AccountRepository.open(config);
             LitoMapRepository maps = LitoMapRepository.open(config);
             CaptureLog capture = new CaptureLog(config.captureDirectory());
             GameHub hub = new GameHub(config, state, accounts);
             ServerSocket loginServer = new ServerSocket()) {
            accounts.healthCheck();
            long waterDragonRestoreNanos = System.nanoTime();
            long waterDragonRestoreEpoch = state.currentEpochSeconds();
            for (String sourceName : config.classicServerNames()) {
                RepositoryWaterDragonPassSnapshot durable = accounts
                        .repositoryForClassicSource(sourceName)
                        .waterDragonPassState();
                hub.restoreClassicWorldWaterDragonPasses(
                        sourceName,
                        durable.authoritative()
                                ? durable.passes()
                                : state.activeWaterDragonPasses(sourceName),
                        waterDragonRestoreNanos, waterDragonRestoreEpoch);
            }
            maps.healthCheck();
            loginServer.bind(new InetSocketAddress(InetAddress.getByName(config.bind()), config.port()), 16);
            for (int channelId = 0; channelId < config.channelPorts().size(); channelId++) {
                ServerSocket gameServer = new ServerSocket();
                gameServer.bind(new InetSocketAddress(
                        InetAddress.getByName(config.gameBind()),
                        config.channelPorts().get(channelId)), 16);
                gameServers.add(gameServer);
            }
            System.out.println("LiTo381 Server " + serverVersion());
            System.out.println("[LiTo] login-listen=" + config.bind() + ":" + config.port());
            System.out.println("[LiTo] game-listen=" + config.gameBind() + ":"
                    + config.channelPorts()
                    + (config.dedicatedChannelPortsEnabled()
                            ? " channels=" + GameHub.CHANNEL_NAMES
                            : " shared-channel-endpoint"));
            System.out.println("[LiTo] database=" + accounts.description());
            System.out.println("[LiTo] maps=" + maps.description());
            System.out.println("[LiTo] origin-source=" + config.originSourceId()
                    + " name=" + config.originSourceName());
            System.out.println("[LiTo] state=" + state.description());
            System.out.println("[LiTo] rental-handoff="
                    + (config.rentalBattleHandoffFile() == null
                            ? "process-local"
                            : "shared(" + config.rentalBattleHandoffFile() + ")"));
            System.out.println("[LiTo] battle-ai=" + config.battleAiEnabled()
                    + " tick=" + config.battleAiTickMs() + "ms damage=" + config.battleAiDamage());
            System.out.println("[LiTo] battle-control=" + hub.battleControlDescription());
            System.out.println("[LiTo] battle-animations=" + animations.source()
                    + " sprites=" + animations.spriteCount()
                    + " actions=" + animations.entryCount());
            System.out.println("[LiTo] capture=" + capture.path());
            if (ExtendedCharacterStatsBridge.enabled()) {
                System.out.println("[LiTo] extended-stats-schema="
                        + ExtendedCharacterStatsBridge.SCHEMA_VERSION);
            }
            if (NumericBreakthrough.enabled()) {
                System.out.println("[LiTo] numeric-breakthrough-schema="
                        + NumericBreakthrough.schemaVersion());
            }
            System.out.println("[LiTo] ready");
            for (int listenerIndex = 0; listenerIndex < gameServers.size(); listenerIndex++) {
                ServerSocket gameServer = gameServers.get(listenerIndex);
                int boundChannelId = config.dedicatedChannelPortsEnabled()
                        ? listenerIndex : -1;
                clients.submit(() -> accept(gameServer, ClientSession.Endpoint.GAME,
                        boundChannelId, config, accounts, state, capture, hub, clients));
            }
            while (!loginServer.isClosed()) {
                Socket socket = loginServer.accept();
                clients.submit(new ClientSession(socket, ClientSession.Endpoint.LOGIN,
                        config, accounts, state, capture, hub));
            }
        } finally {
            for (ServerSocket gameServer : gameServers) {
                try {
                    gameServer.close();
                } catch (Exception ignored) {
                    // Preserve the primary startup/runtime failure.
                }
            }
            clients.shutdownNow();
        }
    }

    private static String serverVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    private static void accept(ServerSocket server, ClientSession.Endpoint endpoint,
                               int boundChannelId, ServerConfig config,
                               AccountRepository accounts, GameStateStore state, CaptureLog capture,
                               GameHub hub, ExecutorService clients) {
        try {
            while (!server.isClosed()) {
                Socket socket = server.accept();
                clients.submit(new ClientSession(socket, endpoint, boundChannelId,
                        config, accounts, state, capture, hub));
            }
        } catch (Exception ex) {
            if (!server.isClosed()) System.out.println("[LiTo] " + endpoint + " accept ended: " + ex.getMessage());
        }
    }
}
