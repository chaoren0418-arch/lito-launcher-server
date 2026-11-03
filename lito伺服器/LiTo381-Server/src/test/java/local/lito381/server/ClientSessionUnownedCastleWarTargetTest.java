package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionUnownedCastleWarTargetTest {
    @TempDir Path temporary;

    @Test
    void exactSingleSpaceTargetResolvesCurrentTempNpcOnlyWhileCastleIsUnowned()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("unowned-target.properties"), true);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("capture")), hub);
            String source = config.classicServerNames().get(0);
            long start = System.currentTimeMillis() - 60_000L;
            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(
                            8, "狄亞得要塞", start, 60, 10_080)));
            ClassicWorldCastleWarField.WarObject objective =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(candidate -> candidate.kind()
                                    == ClassicWorldCastleWarField.Kind.TOWER)
                            .filter(candidate -> candidate.castleId() == 8)
                            .findFirst().orElseThrow();
            setEnumField(session, "phase", "WORLD");
            setField(session, "selectedClassicServerName", source);
            setField(session, "classicWorldMapId", 320);
            setField(session, "playerHp", 100);
            handleObjectiveAction(session, objective.objectId());
            assertEquals(objective.objectId(),
                    field(session, "lastClassicNpcActionObjectId"));

            CastleSummary unowned = unownedTarget(session);
            assertEquals(8, unowned.castleId());
            assertEquals("NPC", unowned.clanName());
            assertEquals("狄亞得要塞", unowned.castleName());

            state.transferCastleOwnership(
                    source, 8, "NewDiad", "NewLeader", 27);
            assertNull(unownedTarget(session),
                    "the same temp NPC can no longer target a newly owned castle");

            setField(session, "lastClassicNpcActionObjectId", 0L);
            assertNull(unownedTarget(session));
        }
    }

    private static CastleSummary unownedTarget(ClientSession session)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "unownedClassicCastleWarTarget");
        method.setAccessible(true);
        return (CastleSummary) method.invoke(session);
    }

    private static void handleObjectiveAction(ClientSession session, long objectId)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "handleClassicWorldNpcAction",
                Protocol.HypertextActionRequest.class);
        method.setAccessible(true);
        method.invoke(session, new Protocol.HypertextActionRequest(
                objectId, "", "", Protocol.HypertextActionKind.GENERIC));
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
    }
}
