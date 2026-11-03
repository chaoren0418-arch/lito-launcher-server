package local.lito381.server;

import java.lang.reflect.*;

/** Shared-source Dragon Knight compatibility. No persistent or global player state. */
final class DragonCombat {
    private static final Class<?> TARGET = nested("FoeSlayerTarget");
    private static final Constructor<?> CONSTRUCTOR;
    private static final Method COMPANION = method(ClientSession.class,"resolveBattleSpellCompanion",long.class);
    private static final Method SEND = method(ClientSession.class,"sendEncrypted",byte[].class,String.class);
    static {
        CONSTRUCTOR = TARGET.getDeclaredConstructors()[0]; CONSTRUCTOR.setAccessible(true);
    }
    private static Class<?> nested(String name) {
        try { return Class.forName(ClientSession.class.getName()+"$"+name); }
        catch(Exception e) {throw new ExceptionInInitializerError(e);}
    }
    private static Method method(Class<?> type,String name,Class<?>...args) {
        try {var m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
        catch(Exception e) {throw new ExceptionInInitializerError(e);}
    }
    private static Object get(Object object,String name) throws ReflectiveOperationException {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static Object record(Object value,String name) throws ReflectiveOperationException {
        return method(value.getClass(),name).invoke(value);
    }
    // Resolve authoritative actor coordinates; never use packet coordinates for validation.
    private static Object companion(ClientSession s,long id,int maximumRange) throws ReflectiveOperationException {
        var value=COMPANION.invoke(s,id); if(value==null) return null;
        var owner=(ClientSession)record(value,"owner");
        var actor=(BattleCompanion.Snapshot)record(value,"snapshot");
        var hub=(GameHub)get(s,"hub");long room=(long)get(s,"currentRoomId");
        if((int)get(s,"playerHp")<=0 || owner!=s && !hub.canAttack(room,s,owner))return null;
        if(Math.max(Math.abs((int)get(s,"positionX")-actor.x()),Math.abs((int)get(s,"positionY")-actor.y()))>maximumRange)return null;
        return value;
    }
    static Object foeCompanion(ClientSession s) {
        var request=DragonSkillRequests.current();if(request==null || request.skillId()!=187)return null;
        try {
            var value=companion(s,request.targetObjectId(),2);if(value==null)return null;
            var actor=(BattleCompanion.Snapshot)record(value,"snapshot");
            return CONSTRUCTOR.newInstance(actor.objectId(),actor.x(),actor.y(),null,null,null,null,null,false);
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("Foe Slayer companion resolution",e);}
    }
    static boolean foeCompanionStillEligible(ClientSession s,long id,Object phase,long room,int map,int instance,int x,int y,int range) {
        try {
            if(!phase.toString().equals("BATTLE") || get(s,"phase")!=phase || (long)get(s,"currentRoomId")!=room
                || (int)get(s,"positionX")!=x || (int)get(s,"positionY")!=y
                || ((GameHub)get(s,"hub")).battleMapId(room,s)!=map) return false;
            return companion(s,id,range)!=null;
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("Foe Slayer companion revalidation",e);}
    }
    static void sendSkinIcon(ClientSession s,int seconds) {
        try {SEND.invoke(s,Protocol.mageShield(seconds,181),"S_DRAGON_SKIN_ICON seconds="+seconds);}
        catch(InvocationTargetException e) {
            if(e.getCause() instanceof java.io.IOException) {
                System.out.println("[LiTo] Dragon Skin icon transport closed");return;
            }
            throw new IllegalStateException(e.getCause());
        } catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
    }
}
