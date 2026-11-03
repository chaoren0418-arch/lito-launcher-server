package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Native S_CC add is insert-only. Explicit updates keep an existing row and its selection. */
final class NativeRoomDirectoryRow {
    private static final Method SEND;
    static {
        try {
            SEND = ClientSession.class.getDeclaredMethod("sendEncrypted", byte[].class, String.class);
            SEND.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    private static void emit(ClientSession session, Protocol.Writer packet, String note) throws IOException {
        try { SEND.invoke(session, packet.bytes(), note); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException io) throw io;
            throw new IllegalStateException("native room row", e.getCause());
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    static void send(ClientSession session, GameHub.RoomSnapshot room, String trigger) throws IOException {
        if (NativeRoomAdmission.hidden(session, room.id())) {
            emit(session, new Protocol.Writer().c(Protocol.S_LITO_DELETE_ROOM).d(room.id()),
                    "S_LITO_DELETE_ROOM hidden-running-or-result room=" + room.id());
            return;
        }
        var s = room.settings();
        var hub = (GameHub) NativeRoomRules.get(session, "hub");
        // Match the ArenaBot row advertised by sendRoomUser, including an empty
        // default test room. This is presentation only: admission and settlement
        // continue to count actual combatants.
        int displayedUsers = room.users()
                + (NativeRoomRules.bot(hub.roomRules(room.id()).mode(), hub, room.id())
                   && room.combatants() < 2 ? 1 : 0);
        String note = "S_LITO_ROOM_INFO room=" + room.id() + " name=" + s.name()
                + " users=" + displayedUsers + " capacity=" + s.capacity() + " trigger=" + trigger;
        // Search-mode lists permit duplicate IDs; remove the prior search result first.
        if (trigger.equals("find-user")) {
            emit(session, new Protocol.Writer().c(Protocol.S_LITO_DELETE_ROOM).d(room.id()), note + " search-replace");
        }
        emit(session, new Protocol.Writer().c(0xcc).c(0).d(room.id()).s(s.name())
                .c(s.roomListAttributes(room.battleStarted())).h(displayedUsers).h(s.capacity())
                .h(0).c(s.option0()), note + " add-if-missing");
        emit(session, new Protocol.Writer().c(0xcc).c(1).d(room.id()).s(s.name()), note + " name");
        emit(session, new Protocol.Writer().c(0xcc).c(2).d(room.id())
                .c(s.roomListAttributes(room.battleStarted())), note + " attributes");
        emit(session, new Protocol.Writer().c(0xcc).c(3).d(room.id()).h(displayedUsers), note + " users");
        emit(session, new Protocol.Writer().c(0xcc).c(4).d(room.id()).h(s.capacity()), note + " capacity");
        emit(session, new Protocol.Writer().c(0xcc).c(5).d(room.id()).h(0).c(s.option0()), note + " mode");
    }
}
