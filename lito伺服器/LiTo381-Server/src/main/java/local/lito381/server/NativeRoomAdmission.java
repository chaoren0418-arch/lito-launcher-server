package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Map;

/** Waiting-room directory policy; match/result objects survive independently of listing. */
final class NativeRoomAdmission {
    private static final Field HUB=field(ClientSession.class,"hub"), CHANNEL=field(ClientSession.class,"currentChannelId"),
            ROOM_ID=field(ClientSession.class,"currentRoomId"), PHASE=field(ClientSession.class,"phase"),
            ROOMS=field(GameHub.class,"rooms");
    private static final Field STARTED, RESULT;
    static {
        try { Class<?> type=Class.forName("local.lito381.server.GameHub$Room");
            STARTED=field(type,"battleStarted");RESULT=field(type,"resultPending");
        } catch(ClassNotFoundException e){throw new ExceptionInInitializerError(e);}
    }
    private static Field field(Class<?> type,String name){
        try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
        catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}
    }
    static boolean hidden(ClientSession viewer,long id){
        try {
            // /argo is the separate native observation directory.
            if(CHANNEL.getInt(viewer)==99)return false;
            GameHub hub=(GameHub)HUB.get(viewer);
            synchronized(hub){
                Object room=((Map<?,?>)ROOMS.get(hub)).get(id);
                return room==null || STARTED.getBoolean(room) || RESULT.getBoolean(room);
            }
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    static void hideStartedRoom(ClientSession actor){
        try {
            GameHub hub=(GameHub)HUB.get(actor);int channel=CHANNEL.getInt(actor);long room=ROOM_ID.getLong(actor);
            // Copy recipients before network I/O. Failure of one lobby socket must not abort battle start.
            for(ClientSession peer:hub.channelMembers(channel)){
                if(CHANNEL.getInt(peer)!=channel || !PHASE.get(peer).toString().equals("CHANNEL"))continue;
                try {peer.serverRoomDeleted(room);}
                catch(IOException e){LobbyRoomDirectory.peerFailure(e);}
            }
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
}
