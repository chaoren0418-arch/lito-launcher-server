package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

/** Independently negotiated v32 kinds 52/53. Existing v32 frames are unchanged.
 * Raw recipient spelling is echoed exactly; only the authority normalizes it.
 * Clients must not guess Java's Unicode case mapping to correlate a response. */
final class ModernDirectChatProtocol {
    static final int VERSION=32, REQUEST_KIND=52, RESULT_KIND=53;
    static final int MIN_REQUEST_BYTES=84, MAX_REQUEST_BYTES=84+256+512;
    static final int MIN_RESULT_BYTES=126, MAX_FRAME_BYTES=128*1024;
    record Target(int sourceId,String name) {
        Target {
            Objects.requireNonNull(name);
            if(name.getBytes(StandardCharsets.UTF_8).length>256)throw bad();
            // Validate both the exact wire value and the authority's canonical address.
            ModernChat.validateText(name); new ModernDirectChat.Address(sourceId,name);
        }
        ModernDirectChat.Address address(){return new ModernDirectChat.Address(sourceId,name);}
        @Override public String toString(){return "DirectTarget[redacted]";}
    }
    record Request(ModernRooms.Scope scope,long roomId,long membership,long sequence,long after,
                   ModernDirectChat.Action action,Target target,ModernDirectChat.Preferences preferences,String text) {
        Request {
            Objects.requireNonNull(scope);
            if(roomId<0||membership<=0)throw bad();
            new ModernDirectChat.Request(action,sequence,after,target==null?null:target.address(),preferences,text);
        }
        ModernDirectChat.Request command(){return new ModernDirectChat.Request(action,sequence,after,target==null?null:target.address(),preferences,text);}
        @Override public String toString(){return "DirectWireRequest[redacted]";}
    }
    private ModernDirectChatProtocol(){}
    static boolean negotiated(ModernSessionBoundary.Negotiated protocol){
        return protocol!=null&&protocol.version()>=VERSION&&(protocol.capabilities()&ModernSessionProtocol.CAP_DIRECT_CHAT)!=0;
    }
    static int flags(ModernDirectChat.Preferences p){return (p.whispers()?1:0)|(p.world()?2:0)|(p.trade()?4:0);}
    static ModernDirectChat.Preferences preferences(int bits){
        if(bits>7)throw bad();return new ModernDirectChat.Preferences((bits&1)!=0,(bits&2)!=0,(bits&4)!=0);
    }
    static void putRequest(ByteBuffer out,Request r)throws ModernApplicationProtocol.ProtocolException{
        var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId());
        out.put((byte)0).putShort((short)0).putLong(r.roomId()).putLong(r.membership()).putLong(r.sequence()).putLong(r.after());
        out.put((byte)r.action().ordinal()).put((byte)(r.preferences()==null?0:flags(r.preferences()))).putShort((short)0);
        out.putInt(r.target()==null?0:r.target().sourceId());
        ModernApplicationProtocol.putText(out,r.target()==null?"":r.target().name(),256,true);
        ModernApplicationProtocol.putText(out,r.text(),512,r.action()!=ModernDirectChat.Action.SEND);
    }
    static Request getRequest(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException{
        int source=in.getInt();long object=in.getLong(),epoch=in.getLong();int channel=Byte.toUnsignedInt(in.get());zero(in,3);
        long room=in.getLong(),membership=in.getLong(),sequence=in.getLong(),after=in.getLong();
        int action=Byte.toUnsignedInt(in.get()),bits=Byte.toUnsignedInt(in.get());zero(in,2);
        if(action>=ModernDirectChat.Action.values().length||bits>7||action!=2&&bits!=0)throw bad();
        int targetSource=in.getInt();String targetName=ModernApplicationProtocol.text(in,256,true);
        if(targetSource==0&&!targetName.isEmpty())throw bad();
        var target=targetSource==0?null:new Target(targetSource,targetName);
        return new Request(new ModernRooms.Scope(source,object,epoch,channel),room,membership,sequence,after,
                ModernDirectChat.Action.values()[action],target,action==2?preferences(bits):null,
                ModernApplicationProtocol.text(in,512,action!=1));
    }
    private static void putPeer(ByteBuffer out,ModernDirectChat.Peer p)throws ModernApplicationProtocol.ProtocolException{
        out.putInt(p.key().sourceId()).putLong(p.key().objectId());
        ModernApplicationProtocol.putText(out,p.characterName(),256,false);
        ModernApplicationProtocol.putText(out,p.sourceName(),256,false);
    }
    private static ModernDirectChat.Peer getPeer(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException{
        return new ModernDirectChat.Peer(new TournamentPresence.CharacterKey(in.getInt(),in.getLong()),
                ModernApplicationProtocol.text(in,256,false),ModernApplicationProtocol.text(in,256,false));
    }
    static void putResult(ByteBuffer out,Request echo,ModernDirectChat.Result result)throws ModernApplicationProtocol.ProtocolException{
        putResult(out,echo,result,false);
    }
    static void putResult(ByteBuffer out,Request echo,ModernDirectChat.Result result,boolean moderated)throws ModernApplicationProtocol.ProtocolException{
        validate(echo,result,moderated);putRequest(out,echo);var s=result.snapshot();
        out.put((byte)result.status().ordinal()).put((byte)((s.available()?1:0)|(s.truncated()?2:0)))
                .put((byte)flags(s.preferences())).put((byte)s.blocked().size()).putShort((short)s.messages().size()).putShort((short)0)
                .putLong(s.requestSequence()).putLong(s.cursor());putPeer(out,s.owner());
        for(var a:s.blocked()) {out.putInt(a.sourceId());ModernApplicationProtocol.putText(out,a.name(),256,false);}
        for(var m:s.messages()) {
            out.putLong(m.id()).putLong(m.senderSequence());putPeer(out,m.sender());putPeer(out,m.recipient());
            ModernApplicationProtocol.putText(out,m.text(),512,false);
        }
    }
    static ModernApplicationProtocol.DirectChatResult getResult(ByteBuffer in,int requestId)throws ModernApplicationProtocol.ProtocolException{
        return getResult(in,requestId,false);
    }
    static ModernApplicationProtocol.DirectChatResult getResult(ByteBuffer in,int requestId,boolean moderated)throws ModernApplicationProtocol.ProtocolException{
        var echo=getRequest(in);int status=Byte.toUnsignedInt(in.get()),state=Byte.toUnsignedInt(in.get());
        var prefs=preferences(Byte.toUnsignedInt(in.get()));int blocked=Byte.toUnsignedInt(in.get()),count=Short.toUnsignedInt(in.getShort());zero(in,2);
        if(status>=ModernDirectChat.Status.values().length||state>3||blocked>16||count>64)throw bad();
        long sequence=in.getLong(),cursor=in.getLong();var owner=getPeer(in);
        var addresses=new ArrayList<ModernDirectChat.Address>(blocked);
        for(int i=0;i<blocked;i++) {
            int source=in.getInt();String name=ModernApplicationProtocol.text(in,256,false);
            var address=new ModernDirectChat.Address(source,name);
            if(!address.name().equals(name))throw bad(); // Never silently rewrite a received canonical result.
            addresses.add(address);
        }
        var messages=new ArrayList<ModernDirectChat.Message>(count);
        for(int i=0;i<count;i++)messages.add(new ModernDirectChat.Message(in.getLong(),in.getLong(),getPeer(in),getPeer(in),ModernApplicationProtocol.text(in,512,false)));
        var result=new ModernDirectChat.Result(ModernDirectChat.Status.values()[status],new ModernDirectChat.Snapshot(owner,sequence,cursor,
                (state&1)!=0,(state&2)!=0,prefs,addresses,messages));
        validate(echo,result,moderated);return new ModernApplicationProtocol.DirectChatResult(requestId,echo,result);
    }
    static void validate(Request echo,ModernDirectChat.Result result)throws ModernApplicationProtocol.ProtocolException{
        validate(echo,result,false);
    }
    static void validate(Request echo,ModernDirectChat.Result result,boolean moderated)throws ModernApplicationProtocol.ProtocolException{
        if(echo==null||result==null)throw new ModernApplicationProtocol.ProtocolException();
        var s=result.snapshot();var action=echo.action();var status=result.status();
        if(status==ModernDirectChat.Status.SILENCED&&(!moderated||action!=ModernDirectChat.Action.SEND))
            throw new ModernApplicationProtocol.ProtocolException();
        if(!s.owner().key().equals(new TournamentPresence.CharacterKey(echo.scope().sourceId(),echo.scope().objectId()))
                ||s.cursor()<echo.after()||action!=ModernDirectChat.Action.READ&&s.requestSequence()!=echo.sequence()
                ||status==ModernDirectChat.Status.UNAVAILABLE&&s.available()
                ||(status==ModernDirectChat.Status.RATE_LIMITED||status==ModernDirectChat.Status.UNREACHABLE)&&action!=ModernDirectChat.Action.SEND
                ||status==ModernDirectChat.Status.BLOCK_LIST_FULL&&action!=ModernDirectChat.Action.BLOCK)throw new ModernApplicationProtocol.ProtocolException();
        long previous=0;
        for(var m:s.messages()) {
            if(m.id()<=echo.after()||previous!=0&&m.id()!=previous+1)throw new ModernApplicationProtocol.ProtocolException();
            previous=m.id();
        }
        if(s.messages().isEmpty()?s.cursor()!=echo.after()||s.truncated():s.truncated()!=(s.messages().get(0).id()-1>echo.after()))
            throw new ModernApplicationProtocol.ProtocolException();
        if(status==ModernDirectChat.Status.OK) {
            if(action==ModernDirectChat.Action.SEND) {
                if(s.messages().isEmpty())throw new ModernApplicationProtocol.ProtocolException();
                var last=s.messages().get(s.messages().size()-1);
                if(!last.sender().equals(s.owner())||last.senderSequence()!=echo.sequence()||!last.text().equals(echo.text())
                        ||!echo.target().address().matches(last.recipient()))throw new ModernApplicationProtocol.ProtocolException();
            }
            if(action==ModernDirectChat.Action.PREFERENCES&&!s.preferences().equals(echo.preferences())
                    ||action==ModernDirectChat.Action.BLOCK&&!s.blocked().contains(echo.target().address())
                    ||action==ModernDirectChat.Action.UNBLOCK&&s.blocked().contains(echo.target().address())
                    ||action==ModernDirectChat.Action.CLEAR_BLOCKS&&!s.blocked().isEmpty())throw new ModernApplicationProtocol.ProtocolException();
        }
    }
    private static void zero(ByteBuffer in,int n){for(int i=0;i<n;i++)if(in.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern direct chat frame");}
}
