package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/** Restore both players when crossing the native view boundary during an arena move. */
final class NativeBattleVisibility {
    static List<ClientSession> localFirst(List<ClientSession> members,ClientSession local) {
        var ordered=new ArrayList<ClientSession>(members.size());
        if(members.contains(local))ordered.add(local);
        for(var member:members)if(member!=local)ordered.add(member);
        return ordered;
    }
    private static final Map<String,Field> F=new HashMap<>();
    private static final Map<String,Method> M=new HashMap<>();
    static {
        for(String name:List.of("hub","currentRoomId","positionX","positionY","playerHeading","invisible","observer","phase","timedStatus","classicTitle")) {
            try {var f=ClientSession.class.getDeclaredField(name);f.setAccessible(true);F.put(name,f);}
            catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}
        }
        for(var m:ClientSession.class.getDeclaredMethods()) if(Set.of("playerObjectId","litoDisplayName","sourceCharacterIdentity","battleGfx","battleWalkAction","battleLightRange","battleMoveSpeed","battleLevel","characterLawful","battleHpRatio","sendBattleObjectAtHeading","sendEncrypted").contains(m.getName())) {
            int count=m.getName().equals("sendBattleObjectAtHeading") ? 15 : m.getName().equals("sendEncrypted") ? 2 : 0;
            if(m.getParameterCount()!=count)continue;
            m.setAccessible(true);M.put(m.getName(),m);
        }
    }
    private static Object field(ClientSession s,String n) {try{return F.get(n).get(s);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static int number(ClientSession s,String n){return (int)field(s,n);}
    private static Object call(ClientSession s,String n,Object... args)throws IOException {
        try{return M.get(n).invoke(s,args);}
        catch(InvocationTargetException e){if(e.getCause() instanceof IOException io)throw io;throw new IllegalStateException(n,e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(n,e);}
    }
    private static void reveal(ClientSession viewer,ClientSession actor)throws IOException {
        long id=(long)call(actor,"playerObjectId");
        int lawful=((Number)call(actor,"characterLawful")).intValue();lawful=Math.max(-32768,Math.min(32767,lawful));
        call(viewer,"sendBattleObjectAtHeading",id,call(actor,"litoDisplayName"),call(actor,"sourceCharacterIdentity"),
                number(actor,"positionX"),number(actor,"positionY"),call(actor,"battleGfx"),call(actor,"battleWalkAction"),
                number(actor,"playerHeading"),call(actor,"battleLightRange"),call(actor,"battleMoveSpeed"),4,
                call(actor,"battleLevel"),lawful,field(actor,"classicTitle"),call(actor,"battleHpRatio"));
        var brave=((BattleTimedStatus)field(actor,"timedStatus")).brave(System.nanoTime());
        if(brave.mode()!=BattleTimedStatus.BRAVE_NORMAL)call(viewer,"sendEncrypted",
                Protocol.braveSpeed(id,Protocol.BraveMode.fromWire(brave.mode()),0),"S_EMOTION battle-screen-enter object="+id);
    }
    private static void remove(ClientSession viewer,ClientSession actor)throws IOException {
        long id=(long)call(actor,"playerObjectId");call(viewer,"sendEncrypted",Protocol.removeObject(id),"S_REMOVE_OBJECT battle-screen-exit object="+id);
    }
    static void afterMove(ClientSession actor,byte[] request)throws IOException {
        if(!field(actor,"phase").toString().equals("BATTLE")){
            NativeSourceCubeVisibility.refresh(actor);
            return;
        }
        var old=Protocol.moveRequest(request);int x=number(actor,"positionX"),y=number(actor,"positionY");
        if(x!=old.x() || y!=old.y())throw new IllegalStateException("uncommitted visibility move");
        var hub=(GameHub)field(actor,"hub");long room=(long)field(actor,"currentRoomId");
        for(ClientSession peer:hub.battleRecipients(room,hub.forceOf(room,actor))) {
            if(peer==actor || !field(peer,"phase").toString().equals("BATTLE"))continue;
            int px=number(peer,"positionX"),py=number(peer,"positionY");
            boolean before=BattleSpellCatalog.isInNativeScreen(old.sourceX(),old.sourceY(),px,py);
            boolean after=BattleSpellCatalog.isInNativeScreen(x,y,px,py);
            if(before==after)continue;
            if(!(boolean)field(actor,"invisible")){if(after)reveal(peer,actor);else remove(peer,actor);}
            if(!(boolean)field(peer,"invisible") && !(boolean)field(peer,"observer")){if(after)reveal(actor,peer);else remove(actor,peer);}
        }
        NativeSourceCubeVisibility.refresh(actor);
    }
}
