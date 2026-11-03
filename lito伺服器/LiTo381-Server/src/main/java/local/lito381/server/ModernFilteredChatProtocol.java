package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/** Explicit v33 recipient-local conversation. Kinds 54/55 never reinterpret
 * v32's shared log. A successful SEND has an independent, exact receipt even
 * when the sender disables their own category. All integers are big endian. */
final class ModernFilteredChatProtocol {
    static final int VERSION=33, REQUEST_KIND=54, RESULT_KIND=55;
    static final int MIN_REQUEST_BYTES=76, MAX_REQUEST_BYTES=76+512;
    static final int MIN_RESULT_BYTES=126, MAX_FRAME_BYTES=72*1024;
    private ModernFilteredChatProtocol(){}
    static boolean negotiated(ModernSessionBoundary.Negotiated protocol) {
        return protocol!=null&&protocol.version()>=VERSION
                &&(protocol.capabilities()&ModernSessionProtocol.CAP_FILTERED_CHAT)!=0;
    }
    static void putRequest(ByteBuffer out,ModernFilteredChat.Request r)throws ModernApplicationProtocol.ProtocolException {
        var t=r.token();var s=t.scope();
        out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId());
        out.put((byte)0).putShort((short)0).putLong(t.roomId()).putLong(t.membership())
                .putLong(r.sequence()).putLong(r.after()).put((byte)r.action().ordinal()).put((byte)r.kind().ordinal());
        ModernApplicationProtocol.putText(out,r.text(),512,r.action()==ModernFilteredChat.Action.READ);
    }
    static ModernFilteredChat.Request getRequest(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException {
        var scope=new ModernRooms.Scope(in.getInt(),in.getLong(),in.getLong(),Byte.toUnsignedInt(in.get()));zero(in,3);
        var token=new ModernFilteredChat.Token(scope,in.getLong(),in.getLong());
        long sequence=in.getLong(),after=in.getLong();int action=Byte.toUnsignedInt(in.get());var kind=kind(in);
        if(action>=ModernFilteredChat.Action.values().length)throw bad();
        return new ModernFilteredChat.Request(token,ModernFilteredChat.Action.values()[action],kind,sequence,after,
                ModernApplicationProtocol.text(in,512,action==0));
    }
    private static void putPeer(ByteBuffer out,ModernDirectChat.Peer peer)throws ModernApplicationProtocol.ProtocolException {
        out.putInt(peer.key().sourceId()).putLong(peer.key().objectId());
        ModernApplicationProtocol.putText(out,peer.characterName(),256,false);
        ModernApplicationProtocol.putText(out,peer.sourceName(),256,false);
    }
    private static ModernDirectChat.Peer peer(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException {
        return new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(in.getInt(),in.getLong()),
                ModernApplicationProtocol.text(in,256,false),ModernApplicationProtocol.text(in,256,false));
    }
    private static void putRow(ByteBuffer out,long id,ModernFilteredChat.Kind kind,ModernDirectChat.Peer sender,String text)
            throws ModernApplicationProtocol.ProtocolException {
        out.putLong(id).put((byte)kind.ordinal()).put((byte)0).putShort((short)0);putPeer(out,sender);
        ModernApplicationProtocol.putText(out,text,512,false);
    }
    static void putResult(ByteBuffer out,ModernFilteredChat.Request echo,ModernFilteredChat.Result result)
            throws ModernApplicationProtocol.ProtocolException {
        validate(echo,result);putRequest(out,echo);var s=result.snapshot();
        out.put((byte)result.status().ordinal()).put((byte)((s.available()?1:0)|(s.truncated()?2:0)))
                .put((byte)(result.receipt()==null?0:1)).put((byte)0).putShort((short)s.messages().size()).putShort((short)0)
                .putLong(s.token().membership()).putLong(s.sequence()).putLong(s.cursor());putPeer(out,s.owner());
        for(var m:s.messages())putRow(out,m.id(),m.kind(),m.sender(),m.text());
        var receipt=result.receipt();if(receipt!=null)putRow(out,receipt.sequence(),receipt.kind(),receipt.sender(),receipt.text());
    }
    static ModernApplicationProtocol.FilteredChatResult getResult(ByteBuffer in,int requestId)
            throws ModernApplicationProtocol.ProtocolException {
        var echo=getRequest(in);int status=Byte.toUnsignedInt(in.get()),flags=Byte.toUnsignedInt(in.get()),hasReceipt=Byte.toUnsignedInt(in.get());
        zero(in,1);int count=Short.toUnsignedInt(in.getShort());zero(in,2);
        if(status>=ModernFilteredChat.Status.values().length||flags>3||hasReceipt>1||count>64)throw bad();
        var token=new ModernFilteredChat.Token(echo.token().scope(),echo.token().roomId(),in.getLong());
        long sequence=in.getLong(),cursor=in.getLong();var owner=peer(in);
        var rows=new ArrayList<ModernFilteredChat.Message>(count);
        for(int i=0;i<count;i++){long id=in.getLong();var kind=kind(in);zero(in,3);
            rows.add(new ModernFilteredChat.Message(id,peer(in),kind,ModernApplicationProtocol.text(in,512,false)));}
        ModernFilteredChat.Receipt receipt=null;
        if(hasReceipt!=0){long seq=in.getLong();var kind=kind(in);zero(in,3);
            receipt=new ModernFilteredChat.Receipt(seq,peer(in),kind,ModernApplicationProtocol.text(in,512,false));}
        var result=new ModernFilteredChat.Result(ModernFilteredChat.Status.values()[status],
                new ModernFilteredChat.Snapshot(token,owner,sequence,cursor,(flags&1)!=0,(flags&2)!=0,rows),receipt);
        validate(echo,result);return new ModernApplicationProtocol.FilteredChatResult(requestId,echo,result);
    }
    static void validate(ModernFilteredChat.Request echo,ModernFilteredChat.Result result)
            throws ModernApplicationProtocol.ProtocolException {
        if(echo==null||result==null)throw new ModernApplicationProtocol.ProtocolException();
        var s=result.snapshot();var t=echo.token();boolean send=echo.action()==ModernFilteredChat.Action.SEND;
        if(!s.token().scope().equals(t.scope())||s.token().roomId()!=t.roomId()
                ||t.membership()!=0&&s.token().membership()!=t.membership()||s.cursor()<echo.after()
                ||send&&s.sequence()!=echo.sequence()
                ||!send&&result.status()!=(s.available()?ModernFilteredChat.Status.OK:ModernFilteredChat.Status.UNAVAILABLE))
            throw new ModernApplicationProtocol.ProtocolException();
        long previous=echo.after();
        for(int i=0;i<s.messages().size();i++) {
            long id=s.messages().get(i).id();
            if(id<=previous||i>0&&id!=previous+1)throw new ModernApplicationProtocol.ProtocolException();previous=id;
        }
        if(previous!=s.cursor()||s.truncated()!=(!s.messages().isEmpty()&&s.messages().get(0).id()-1>echo.after()))
            throw new ModernApplicationProtocol.ProtocolException();
        var receipt=result.receipt();boolean accepted=send&&result.status()==ModernFilteredChat.Status.OK;
        if(accepted!=(receipt!=null)||receipt!=null&&(!receipt.sender().equals(s.owner())
                ||receipt.sequence()!=echo.sequence()||receipt.kind()!=echo.kind()||!receipt.text().equals(echo.text())))
            throw new ModernApplicationProtocol.ProtocolException();
        // No UNAVAILABLE=>!available implication: the legacy NORMAL sink can
        // exhaust without exhausting this recipient's own counters.
    }
    private static ModernFilteredChat.Kind kind(ByteBuffer in){int id=Byte.toUnsignedInt(in.get());if(id>=3)throw bad();return ModernFilteredChat.Kind.values()[id];}
    private static void zero(ByteBuffer in,int count){for(int i=0;i<count;i++)if(in.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid filtered chat frame");}
}
