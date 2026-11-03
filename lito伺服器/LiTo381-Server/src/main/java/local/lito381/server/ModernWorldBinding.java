package local.lito381.server;

import java.util.Objects;

/** Explicit server configuration, never a guess that logical LiTo IDs name equivalent shared terrain. */
record ModernWorldBinding(int mode,int logicalMapId,ModernWorldMap map) {
    record Key(int mode, int logicalMapId) {
        Key { TournamentRoomPolicy.rules(mode, logicalMapId); }
    }
    ModernWorldBinding {
        TournamentRoomPolicy.rules(mode,logicalMapId);
        Objects.requireNonNull(map);
    }
    boolean matches(ModernRooms.RoomSnapshot room) {
        return room!=null && room.summary().config().mode()==mode && room.summary().config().mapId()==logicalMapId;
    }
    Key key() { return new Key(mode, logicalMapId); }
}
