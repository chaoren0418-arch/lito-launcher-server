package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;

/** Optional v32, kinds 50/51. All integers are big endian. No claimed sender
 * name or arbitrary recipient is accepted. READ bootstraps a membership token;
 * SEND consumes the connection's chat sequence, including semantic denials. */
final class ModernChatProtocol {
    static final int VERSION = 32, REQUEST_KIND = 50, RESULT_KIND = 51;
    static final int REQUEST_PREFIX = 60, MAX_REQUEST_BYTES = 16 + REQUEST_PREFIX + ModernChat.MAX_TEXT_BYTES;
    static final int MAX_FRAME_BYTES = 72 * 1024;
    enum Action { READ, SEND }
    record Request(ModernRooms.Scope scope, long roomId, long membership, long sequence,
                   long after, Action action, String text) {
        Request {
            Objects.requireNonNull(scope); Objects.requireNonNull(action); Objects.requireNonNull(text);
            if (roomId < 0 || membership < 0 || sequence < 0 || after < 0) throw bad();
            if (action == Action.READ) {
                if (sequence != 0 || !text.isEmpty() || membership == 0 && after != 0) throw bad();
            } else {
                if (membership == 0 || sequence == 0) throw bad();
                ModernChat.validateText(text);
            }
        }
        @Override public String toString() { return "ChatRequest[redacted]"; }
    }
    private ModernChatProtocol() {}
    static boolean negotiated(ModernSessionBoundary.Negotiated protocol) {
        return protocol != null && protocol.version() >= VERSION
                && (protocol.capabilities() & ModernSessionProtocol.CAP_CHAT) != 0;
    }
    static void putRequest(ByteBuffer out, Request r) throws ModernApplicationProtocol.ProtocolException {
        var s = r.scope();
        out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId());
        out.put((byte)0).putShort((short)0).putLong(r.roomId()).putLong(r.membership())
                .putLong(r.sequence()).putLong(r.after()).put((byte)r.action().ordinal()).put((byte)0);
        ModernApplicationProtocol.putText(out,r.text(),ModernChat.MAX_TEXT_BYTES,r.action()==Action.READ);
    }
    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        int source=in.getInt(); long object=in.getLong(),epoch=in.getLong(); int channel=Byte.toUnsignedInt(in.get());
        zero(in,3); long room=in.getLong(),membership=in.getLong(),sequence=in.getLong(),after=in.getLong();
        int action=Byte.toUnsignedInt(in.get()); zero(in,1);
        if(action>=Action.values().length)throw bad();
        return new Request(new ModernRooms.Scope(source,object,epoch,channel),room,membership,sequence,after,
                Action.values()[action],ModernApplicationProtocol.text(in,ModernChat.MAX_TEXT_BYTES,action==0));
    }
    static void putResult(ByteBuffer out, Request echo, ModernChat.Result result) throws ModernApplicationProtocol.ProtocolException {
        validate(echo,result); putRequest(out,echo); var s=result.snapshot();
        out.put((byte)result.status().ordinal()).put((byte)((s.available()?1:0)|(s.truncated()?2:0)))
                .putShort((short)s.messages().size()).putLong(s.membership()).putLong(s.requestSequence()).putLong(s.cursor());
        for(var m:s.messages()) {
            out.putLong(m.id()).putInt(m.sender().sourceId()).putLong(m.sender().objectId());
            ModernApplicationProtocol.putText(out,m.characterName(),256,false);
            ModernApplicationProtocol.putText(out,m.sourceName(),256,false);
            ModernApplicationProtocol.putText(out,m.text(),ModernChat.MAX_TEXT_BYTES,false);
        }
    }
    static ModernApplicationProtocol.ChatResult getResult(ByteBuffer in,int requestId) throws ModernApplicationProtocol.ProtocolException {
        var echo=getRequest(in); int status=Byte.toUnsignedInt(in.get()),flags=Byte.toUnsignedInt(in.get());
        int count=Short.toUnsignedInt(in.getShort());
        if(status>=ModernChat.Status.values().length||flags>3||count>ModernChat.MAX_MESSAGES)throw bad();
        long membership=in.getLong(),sequence=in.getLong(),cursor=in.getLong();
        var rows=new ArrayList<ModernChat.Message>(count);
        for(int i=0;i<count;i++) rows.add(new ModernChat.Message(in.getLong(),
                new TournamentPresence.CharacterKey(in.getInt(),in.getLong()),
                ModernApplicationProtocol.text(in,256,false),ModernApplicationProtocol.text(in,256,false),
                ModernApplicationProtocol.text(in,ModernChat.MAX_TEXT_BYTES,false)));
        var result=new ModernChat.Result(ModernChat.Status.values()[status],new ModernChat.Snapshot(
                new ModernChat.Audience(echo.scope().channelId(),echo.roomId()),membership,sequence,cursor,
                (flags&1)!=0,(flags&2)!=0,rows));
        validate(echo,result); return new ModernApplicationProtocol.ChatResult(requestId,echo,result);
    }
    static void validate(Request echo,ModernChat.Result result) throws ModernApplicationProtocol.ProtocolException {
        if(echo==null||result==null)throw new ModernApplicationProtocol.ProtocolException();
        var s=result.snapshot();
        if(!s.audience().equals(new ModernChat.Audience(echo.scope().channelId(),echo.roomId()))
                ||echo.membership()!=0&&s.membership()!=echo.membership()||s.cursor()<echo.after()
                ||echo.action()==Action.SEND&&s.requestSequence()!=echo.sequence()
                ||echo.action()==Action.READ&&result.status()!=ModernChat.Status.OK&&result.status()!=ModernChat.Status.UNAVAILABLE
                ||result.status()==ModernChat.Status.UNAVAILABLE&&s.available())throw new ModernApplicationProtocol.ProtocolException();
        for(var m:s.messages())if(m.id()<=echo.after())throw new ModernApplicationProtocol.ProtocolException();
        if(!s.messages().isEmpty()&&s.messages().get(s.messages().size()-1).id()!=s.cursor())throw new ModernApplicationProtocol.ProtocolException();
        if(echo.action()==Action.SEND&&result.status()==ModernChat.Status.OK) {
            if(s.messages().isEmpty())throw new ModernApplicationProtocol.ProtocolException();
            var last=s.messages().get(s.messages().size()-1);
            if(!last.sender().equals(new TournamentPresence.CharacterKey(echo.scope().sourceId(),echo.scope().objectId()))
                    ||!last.text().equals(echo.text()))throw new ModernApplicationProtocol.ProtocolException();
        }
    }
    private static void zero(ByteBuffer in,int n) {for(int i=0;i<n;i++)if(in.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern chat frame");}
}
