package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Notify waiting native clients across source databases after room mutations.
 * Snapshot membership before sending; no GameHub monitor is held during I/O.
 * The room snapshot and recipients come from the shared GameHub channel.
 */
final class LobbyRoomDirectory {
    private static final Field HUB = field("hub"), CHANNEL = field("currentChannelId"), PHASE = field("phase");
    private static final Method SEND = method("sendRoomList");
    private static Field field(String name) {
        try { var f = ClientSession.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    private static Method method(String name) {
        try { var m = ClientSession.class.getDeclaredMethod(name); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    static void peerFailure(IOException error) {
        System.out.println("[LiTo] room directory peer notification failed after notifying remaining recipients: " + error.getClass().getSimpleName());
    }
    static void refresh(ClientSession actor, String trigger) {
        try {
            int channel = CHANNEL.getInt(actor);
            if (channel < 0) return;
            GameHub hub = (GameHub) HUB.get(actor);
            for (ClientSession peer : hub.channelMembers(channel)) {
                if (peer == actor || CHANNEL.getInt(peer) != channel || !PHASE.get(peer).toString().equals("CHANNEL")) continue;
                try {
                    SEND.invoke(peer);
                    System.out.println("[LiTo] lobby room directory refreshed channel=" + channel + " trigger=" + trigger);
                } catch (InvocationTargetException e) {
                    if (!(e.getCause() instanceof IOException)) throw new IllegalStateException("room directory refresh", e.getCause());
                    // A disconnected peer must not abort the creator's room transition
                    // or prevent later lobby recipients receiving the directory.
                    System.out.println("[LiTo] lobby room directory peer unavailable channel=" + channel + " trigger=" + trigger);
                }
            }
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
