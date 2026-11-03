package local.lito381.server;

import java.util.*;

/** Exact selected-BIN warehouse C_DB envelope, independent of LTshop 0/1 parser. */
final class NativeWarehouseCdb {
    static boolean mode(int value){return value==2||value==3||value==4||value==5||value==8||value==9;}
    static boolean isWarehouse(byte[] bytes){
        if(bytes==null||bytes.length<4||Byte.toUnsignedInt(bytes[0])!=Protocol.C_LITO_BUY_SELL)return false;
        int at=(bytes.length&7)==4?1:(bytes.length&7)==0&&bytes.length>=8?5:-1;
        return at>=0&&mode(Byte.toUnsignedInt(bytes[at]));
    }
    record Request(boolean hasContext,long npcContext,int mode,List<Protocol.ShopLine> lines,byte[] opaqueTrailer) {
        Request {lines=List.copyOf(lines);opaqueTrailer=opaqueTrailer.clone();}
        @Override public byte[] opaqueTrailer(){return opaqueTrailer.clone();}
        Protocol.ClassicShopTransaction forOwnedNpc(long npc){
            if(npc<=0||hasContext&&npcContext!=npc)throw new IllegalArgumentException("warehouse NPC context differs");
            return new Protocol.ClassicShopTransaction(npc,mode,lines);
        }
    }
    static Request parse(byte[] bytes){
        if(!isWarehouse(bytes))throw new IllegalArgumentException("not an exact warehouse C_DB envelope");
        var in=new Protocol.Reader(bytes);in.c();boolean context=(bytes.length&7)==0;
        long npc=context?in.d():0;int operation=in.c(),count=in.h();
        // Preserve selected producer's bounded 0x1000 buffer, including its
        // contextual count boundary. Opaque bytes are not a token or source id.
        if(context&&npc==0||count>(context?509:510)||in.remaining()!=count*8+8)
            throw new IllegalArgumentException("invalid exact warehouse C_DB layout");
        var rows=new ArrayList<Protocol.ShopLine>(count);
        for(int i=0;i<count;i++){long id=in.d(),quantity=in.d();if(id==0||quantity==0||quantity>Integer.MAX_VALUE)throw new IllegalArgumentException("invalid warehouse C_DB row");rows.add(new Protocol.ShopLine(id,quantity));}
        var tail=new byte[8];for(int i=0;i<8;i++)tail[i]=(byte)in.c();
        return new Request(context,npc,operation,rows,tail);
    }
}
