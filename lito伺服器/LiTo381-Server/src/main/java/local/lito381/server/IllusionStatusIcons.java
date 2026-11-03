package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Map;

/** Presentation of authoritative leases; no new buffs, database writes or global session cache. */
final class IllusionStatusIcons {
    static final int[] SKILLS={201,204,206,209,211,214,216,217,219};
    private static final String[] FIELDS={"mirrorImageExpiresAtNanos","illusionOgreExpiresAtNanos",
            "concentrationExpiresAtNanos","illusionLichExpiresAtNanos",null,"diamondGolemExpiresAtNanos",
            "insightExpiresAtNanos","panicExpiresAtNanos","illusionAvatarExpiresAtNanos"};
    static void sync(ClientSession s) throws IOException {
        String phase=get(s,"phase").toString();
        if (!phase.equals("BATTLE") && !phase.equals("WORLD")) {set(s,"illusionIconLeases",null);return;}
        long now=System.nanoTime();
        long[] previous;
        synchronized(s) {
            previous=(long[])get(s,"illusionIconLeases");
            if(previous==null){previous=new long[SKILLS.length];set(s,"illusionIconLeases",previous);}
        }
        Object ability=get(s,"abilityStatus"),timed=get(s,"timedStatus");
        synchronized(previous) {
        for(int i=0;i<SKILLS.length;i++) {
            long expires;
            if(i==4) expires=patience(s,phase);
            else {Object owner=i==2?timed:ability;synchronized(owner){expires=(long)get(owner,FIELDS[i]);}}
            if(expires<=now)expires=0;
            if(previous[i]==expires)continue;
            int seconds=expires==0?0:(int)Math.min(32767,(expires-now+999_999_999L)/1_000_000_000L);
            send(s,Protocol.mageShield(seconds,SKILLS[i]),"S_ILLUSION_STATUS skill="+SKILLS[i]+" seconds="+seconds);
            previous[i]=expires;
        }
        }
    }
    private static long patience(ClientSession s,String phase) {
        Object hub=get(s,"hub");
        synchronized(hub) {
            Object status=null;
            if(phase.equals("BATTLE")) {
                Object room=((Map<?,?>)get(hub,"rooms")).get(get(s,"currentRoomId"));
                if(room!=null)status=get(room,"patienceStatus");
            } else {
                for(var row:((Map<?,?>)get(hub,"classicWorldPatienceStatuses")).entrySet()) {
                    Object key=row.getKey();
                    if(get(key,"mapId").equals(get(s,"classicWorldMapId")) && get(key,"instanceId").equals(get(s,"classicWorldInstanceId"))) {status=row.getValue();break;}
                }
            }
            if(status==null)return 0;
            synchronized(status){return lease(status,s.playerObjectId());}
        }
    }
    private static long lease(Object status,long id){Object value=((Map<?,?>)get(status,"expiresAtByObjectId")).get(id);return value==null?0:(long)value;}
    private static Object get(Object o,String n){try{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new IllegalStateException(n,e);}}
    private static void set(Object o,String n,Object v){try{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}catch(Exception e){throw new IllegalStateException(n,e);}}
    private static void send(ClientSession s,byte[] packet,String note)throws IOException {
        try{var m=ClientSession.class.getDeclaredMethod("sendEncrypted",byte[].class,String.class);m.setAccessible(true);m.invoke(s,packet,note);}
        catch(InvocationTargetException e){if(e.getCause() instanceof IOException io)throw io;throw new IllegalStateException(e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
}
