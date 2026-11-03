// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import local.lito381.server.ClientSession;
import local.lito381.server.GameStateStore;
import local.lito381.server.Protocol;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;

public final class LiveBattleStats {
    static final Map<GameStateStore, Boolean> enabled = Collections.synchronizedMap(new WeakHashMap());

    static Field field(Class<?> clazz, String string) {
        try {
            Field field = clazz.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static Object get(Object object, String string) {
        try {
            return LiveBattleStats.field(object.getClass(), string).get(object);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static Object call(Object object, String string, Class<?>[] classArray, Object ... objectArray) throws Exception {
        Method method = object.getClass().getDeclaredMethod(string, classArray);
        method.setAccessible(true);
        try {
            return method.invoke(object, objectArray);
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable throwable = invocationTargetException.getCause();
            if (throwable instanceof Exception) {
                Exception exception = (Exception)throwable;
                throw exception;
            }
            throw invocationTargetException;
        }
    }

    public static void enable(ClientSession clientSession) {
        try {
            if (SharedDatabase.enabled((ServerConfig)LiveBattleStats.get(clientSession, "config"))) {
                enabled.put((GameStateStore)LiveBattleStats.get(clientSession, "state"), true);
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("Cannot enable live results", exception);
        }
    }

    public static void beforeSnapshot(GameStateStore gameStateStore) {
        if (!enabled.containsKey(gameStateStore)) {
            return;
        }
        try {
            boolean bl = false;
            for (Object object : ((Map)LiveBattleStats.get(gameStateStore, "profiles")).values()) {
                for (String string : List.of("wins", "losses", "rankingPoints", "classicServerName", "channel", "sourceLevel", "race", "monthlyWins", "monthlyLosses", "monthlyRankingPoints", "monthlyRankingPeriod")) {
                    String string2 = "snapshot" + Character.toUpperCase(string.charAt(0)) + string.substring(1);
                    if (Objects.equals(LiveBattleStats.get(object, string), LiveBattleStats.get(object, string2))) continue;
                    bl = true;
                    break;
                }
                if (!bl) continue;
                break;
            }
            if (bl) {
                for (String string : List.of("rankingSnapshotBoundaryEpochSeconds", "monthlyRankingSnapshotBoundaryEpochSeconds")) {
                    Field field = LiveBattleStats.field(GameStateStore.class, string);
                    long l = field.getLong(gameStateStore);
                    if (l <= 0L) continue;
                    field.setLong(gameStateStore, l - 1L);
                }
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("Cannot refresh live rankings", exception);
        }
    }

    public static boolean baselineEquals(Object object, Object object2, ClientSession clientSession) {
        if (enabled.containsKey((GameStateStore)LiveBattleStats.get(clientSession, "state"))) {
            GameStateStore.TournamentProfileState tournamentProfileState = (GameStateStore.TournamentProfileState)object;
            GameStateStore.TournamentProfileState tournamentProfileState2 = (GameStateStore.TournamentProfileState)object2;
            return tournamentProfileState.wins() == tournamentProfileState2.wins() && tournamentProfileState.losses() == tournamentProfileState2.losses() && tournamentProfileState.classicServerName().equals(tournamentProfileState2.classicServerName());
        }
        return object.equals(object2);
    }

    public static void persistRankingRoster(ClientSession clientSession) throws IOException {
        if (enabled.containsKey((GameStateStore)LiveBattleStats.get(clientSession, "state"))) {
            return;
        }
        try {
            LiveBattleStats.call(clientSession, "persistSqlTournamentRoster", new Class[0], new Object[0]);
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }

    static String hex(String string) {
        return HexFormat.of().formatHex(string.getBytes(Charset.forName("MS950")));
    }

    public static byte[] packet(String string, String string2, String string3, GameStateStore.TournamentStatistics tournamentStatistics, Protocol.LitoCharacterRankings litoCharacterRankings) {
        String string4 = "LITOSTAT1:" + LiveBattleStats.hex(string) + ":" + LiveBattleStats.hex(string2) + ":" + LiveBattleStats.hex(string3) + ":" + tournamentStatistics.winRateDenominator() + "," + tournamentStatistics.wins() + "," + tournamentStatistics.losses() + "," + tournamentStatistics.disconnects() + "," + litoCharacterRankings.overall() + "," + litoCharacterRankings.classicServer() + "," + litoCharacterRankings.channel() + "," + litoCharacterRankings.level() + "," + litoCharacterRankings.race() + "," + litoCharacterRankings.overallMovement() + "," + litoCharacterRankings.classicServerMovement() + "," + litoCharacterRankings.channelMovement() + "," + litoCharacterRankings.levelMovement() + "," + litoCharacterRankings.raceMovement();
        return new Protocol.Writer().c(15).c(254).s(string4).bytes();
    }

    public static void lobby(ClientSession clientSession) throws IOException {
        LiveBattleStats.enable(clientSession);
        if (!enabled.containsKey((GameStateStore)LiveBattleStats.get(clientSession, "state"))) {
            return;
        }
        if (!LiveBattleStats.get(clientSession, "phase").toString().equals("CHANNEL")) {
            return;
        }
        String string = (String)LiveBattleStats.get(clientSession, "selectedCharacter");
        if (string.isBlank()) {
            return;
        }
        try {
            GameStateStore gameStateStore = (GameStateStore)LiveBattleStats.get(clientSession, "state");
            String string2 = (String)LiveBattleStats.call(clientSession, "stateCharacter", new Class[0], new Object[0]);
            GameStateStore.TournamentStatistics tournamentStatistics = gameStateStore.tournamentStatistics(string2);
            Protocol.LitoCharacterRankings litoCharacterRankings = gameStateStore.characterRankings(string2);
            String string3 = (String)LiveBattleStats.call(clientSession, "litoDisplayName", new Class[0], new Object[0]);
            LiveBattleStats.call(clientSession, "sendEncrypted", new Class[]{byte[].class, String.class}, LiveBattleStats.packet((String)LiveBattleStats.get(clientSession, "selectedClassicServerName"), string, string3, tournamentStatistics, litoCharacterRankings), "S_LITO_LIVE_STATS player=" + string + " battles=" + tournamentStatistics.winRateDenominator() + " wins=" + tournamentStatistics.wins() + " losses=" + tournamentStatistics.losses());
        }
        catch (Exception exception) {
            throw new IOException("Cannot publish live lobby statistics", exception);
        }
    }
}
