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
        animations.requireBattleCapableSprite(61, 0);
        animations.requireBattleCapableSprite(48, 0);
        animations.requireBattleCapableSprite(936, 0);
        animations.requireWalkAction(460, 0);
        animations.requireWalkAction(782, 0);
        animations.requireWalkAction(971, 0);
        animations.requireWalkAction(1766, 0);
        animations.requireWalkAction(148, 0);
        ExecutorService clients = Executors.newCachedThreadPool();
        List<ServerSocket> gameServers = new ArrayList<>();
        GameStateStore state = new GameStateStore(
                config.stateFile(), config.statePersistenceEnabled(),
                config.ephemeralSourceProfiles());
        try {
            AccountRepository accounts = AccountRepository.open(config);
            try {
                LitoMapRepository maps = LitoMapRepository.open(config);
                try {
                    CaptureLog capture = new CaptureLog(config.captureDirectory());
                    try {
                        GameHub hub = new GameHub(config, state, accounts);
                        NativeServerLifetime.install(hub, clients, gameServers, capture, maps, accounts);
                        try {
                            NativeDeItemApplication.bootstrap(hub, config);
                            ServerSocket loginServer = new ServerSocket();
                            NativeServerLifetime.login(hub, loginServer);
                            try {
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
                                NativeServerLifetime.bindMain(loginServer,
                                        new InetSocketAddress(InetAddress.getByName(config.bind()), config.port()),
                                        16);
                                for (int channelId = 0; channelId < config.channelPorts().size(); channelId++) {
                                    ServerSocket gameServer = new ServerSocket();
                                    NativeServerLifetime.bindMain(gameServer,
                                            new InetSocketAddress(
                                                    InetAddress.getByName(config.gameBind()),
                                                    config.channelPorts().get(channelId)),
                                            16);
                                    // Keep the formal bytecode CHECKCAST after loading the listener.
                                    NativeServerLifetime.addListener(gameServers, (ServerSocket) (Object) gameServer);
                                }
                                System.out.println("LiTo381 Server " + serverVersion());
                                System.out.println("[LiTo] login-listen=" + config.bind() + ":" + config.port());
                                System.out.println("[LiTo] game-listen=" + config.gameBind() + ":"
                                        + String.valueOf(config.channelPorts())
                                        + (config.dedicatedChannelPortsEnabled()
                                                ? " channels=" + String.valueOf(GameHub.CHANNEL_NAMES)
                                                : " shared-channel-endpoint"));
                                System.out.println("[LiTo] database=" + accounts.description());
                                System.out.println("[LiTo] maps=" + maps.description());
                                System.out.println("[LiTo] origin-source=" + config.originSourceId()
                                        + " name=" + config.originSourceName());
                                System.out.println("[LiTo] state=" + state.description());
                                System.out.println("[LiTo] rental-handoff="
                                        + (config.rentalBattleHandoffFile() == null
                                                ? "process-local"
                                                : "shared(" + String.valueOf(config.rentalBattleHandoffFile()) + ")"));
                                System.out.println("[LiTo] battle-ai=" + config.battleAiEnabled()
                                        + " tick=" + config.battleAiTickMs() + "ms damage=" + config.battleAiDamage());
                                System.out.println("[LiTo] battle-control=" + hub.battleControlDescription());
                                System.out.println("[LiTo] battle-animations=" + animations.source()
                                        + " sprites=" + animations.spriteCount()
                                        + " actions=" + animations.entryCount());
                                System.out.println("[LiTo] capture=" + String.valueOf(capture.path()));
                                if (ExtendedCharacterStatsBridge.enabled()) {
                                    System.out.println("[LiTo] extended-stats-schema=2");
                                }
                                if (NumericBreakthrough.enabled()) {
                                    System.out.println("[LiTo] numeric-breakthrough-schema="
                                            + NumericBreakthrough.schemaVersion());
                                }
                                {
                                    java.io.PrintStream readyOutput = System.out;
                                    SourceLogin.start(config, accounts, state, capture, hub, clients);
                                    readyOutput.println("[LiTo] ready");
                                }
                                for (int listenerIndex = 0; listenerIndex < gameServers.size(); listenerIndex++) {
                                    ServerSocket gameServer = gameServers.get(listenerIndex);
                                    int boundChannelId = config.dedicatedChannelPortsEnabled()
                                            ? listenerIndex : -1;
                                    clients.submit(() -> accept(gameServer, ClientSession.Endpoint.GAME,
                                            boundChannelId, config, accounts, state, capture, hub, clients));
                                }
                                while (!loginServer.isClosed()) {
                                    Socket socket = NativeServerLifetime.acceptSocket(loginServer, hub);
                                    NativeServerLifetime.submitAccepted(clients,
                                            new ClientSession(socket, ClientSession.Endpoint.LOGIN,
                                                    config, accounts, state, capture, hub));
                                }
                            } catch (Throwable thrown) {
                                try {
                                    loginServer.close();
                                } catch (Throwable cleanupFailure) {
                                    thrown.addSuppressed(cleanupFailure);
                                }
                                throw thrown;
                            }
                            loginServer.close();
                        } catch (Throwable thrown) {
                            try {
                                NativeServerLifetime.closeHub(hub);
                            } catch (Throwable cleanupFailure) {
                                thrown.addSuppressed(cleanupFailure);
                            }
                            throw thrown;
                        }
                        NativeServerLifetime.closeHub(hub);
                    } catch (Throwable thrown) {
                        try {
                            NativeServerLifetime.closeResource(capture);
                        } catch (Throwable cleanupFailure) {
                            thrown.addSuppressed(cleanupFailure);
                        }
                        throw thrown;
                    }
                    NativeServerLifetime.closeResource(capture);
                } catch (Throwable thrown) {
                    if (maps != null) {
                        try {
                            NativeServerLifetime.closeResource(maps);
                        } catch (Throwable cleanupFailure) {
                            thrown.addSuppressed(cleanupFailure);
                        }
                    }
                    throw thrown;
                }
                if (maps != null) {
                    NativeServerLifetime.closeResource(maps);
                }
            } catch (Throwable thrown) {
                if (accounts != null) {
                    try {
                        NativeServerLifetime.closeResource(accounts);
                    } catch (Throwable cleanupFailure) {
                        thrown.addSuppressed(cleanupFailure);
                    }
                }
                throw thrown;
            }
            if (accounts != null) {
                NativeServerLifetime.closeResource(accounts);
            }
        } finally {
            for (ServerSocket gameServer : gameServers) {
                try {
                    gameServer.close();
                } catch (Exception ignored) {
                }
            }
            NativeServerLifetime.finishClients(clients);
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
            try {
                while (!server.isClosed()) {
                    Socket socket = NativeServerLifetime.acceptSocket(server, hub);
                    NativeServerLifetime.submitAccepted(clients,
                            new ClientSession(socket, endpoint, boundChannelId,
                                    config, accounts, state, capture, hub));
                }
            } catch (Exception ex) {
                if (!server.isClosed()) {
                    System.out.println("[LiTo] " + String.valueOf(endpoint)
                            + " accept ended: " + ex.getMessage());
                }
            }
            NativeServerLifetime.abortAccept();
            return;
        } catch (Throwable thrown) {
            NativeServerLifetime.abortAccept();
            throw thrown;
        }
    }
}
