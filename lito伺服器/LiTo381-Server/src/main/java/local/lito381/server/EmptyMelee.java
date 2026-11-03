package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.List;

/** Native target-zero melee presentation. No damage, resource debit or global player cache. */
final class EmptyMelee {
    static Protocol.AttackRequest parse(byte[] payload, int opcode) {
        if (opcode != Protocol.C_ATTACK && opcode != Protocol.C_FAR_ATTACK
                && opcode != Protocol.C_ALT_ATTACK) throw new IllegalArgumentException("unsupported native attack opcode");
        var in = new Protocol.Reader(payload);
        if (in.c() != opcode) throw new IllegalArgumentException("wrong native attack opcode");
        var request = new Protocol.AttackRequest(in.d(), in.h(), in.h());
        if (in.remaining() != 0) throw new IllegalArgumentException("invalid native attack layout");
        return request;
    }

    // The retained production session is patched at this narrow dispatch boundary.
    // Reflective access keeps all existing target combat methods byte-for-byte intact.
    static boolean handle(ClientSession s, Protocol.AttackRequest r, String envelope) throws IOException {
        if (r.targetObjectId() != 0) return false;
        synchronized (s) {
            String phase = get(s,"phase").toString();
            boolean battle = phase.equals("BATTLE");
            if (!battle && !phase.equals("WORLD") || (boolean)get(s,"observer")
                    || (int)get(s,"playerHp") <= 0 || (boolean)get(s,"invisible")) return true;
            if (battle && (boolean)call(s,"isPetRaceBattle")) return true;
            if (!battle && !ClassicWorldMapCatalog.supports((int)get(s,"classicWorldMapId"))) return true;
            if ((boolean)call(s,"rejectWhileParalyzed",new Class[]{String.class},"C_EMPTY_MELEE")) return true;
            int walk = (int)call(s,"battleWalkAction");
            if (envelope.equals("FAR") || walk == BattleActionCodes.BOW_WALK
                    || walk == BattleActionCodes.THROWING_KNIFE_WALK) return true;
            int x=(int)get(s,"positionX"), y=(int)get(s,"positionY");
            if (r.x()==0 || r.y()==0 || r.x()==x && r.y()==y
                    || !BattleSpellCatalog.isInNativeScreen(x,y,r.x(),r.y())) return true;
            long now=System.nanoTime();
            if ((int)call(s,"currentWeight240",new Class[]{long.class},now) >= BattleInventoryWeight.BUFFED_REGEN_LIMIT) {
                send(s,Protocol.messageCode(110),"S_MESSAGE_CODE empty-melee-overweight=110");return true;
            }
            var animation=BattleAnimationCatalog.get().requireTimedAction((int)call(s,"battleGfx"),BattleActionCodes.attackForWalk(walk));
            var status=(BattleTimedStatus)get(s,"timedStatus");
            int interval=interval(Math.toIntExact(animation.durationMillis()),status.speed(now).type(),status.brave(now).mode());
            long ready=(long)get(s,"emptyMeleeReadyNanos");
            if (ready!=0 && now<ready) return true;
            set(s,"emptyMeleeReadyNanos",now+interval*1_000_000L);
            call(s,"closePersonalShopIfOpen",new Class[]{String.class},"empty-melee");
            call(s,"cancelAbsoluteBarrier",new Class[]{String.class},"empty-melee");
            call(s,"cancelMeditation",new Class[]{String.class},"empty-melee");
            call(get(s,"naturalHpRegen"),"noteAttack");call(get(s,"naturalMpRegen"),"noteAttack");
            call(s,"markBattleCombat");
            int heading=(int)call(s,"heading",new Class[]{int.class,int.class,int.class,int.class},x,y,r.x(),r.y());
            set(s,"playerHeading",heading);
            var hub=(GameHub)get(s,"hub");
            @SuppressWarnings("unchecked")
            List<ClientSession> recipients=battle ? hub.battleRecipients((long)get(s,"currentRoomId"),hub.forceOf((long)get(s,"currentRoomId"),s))
                    : (List<ClientSession>)call(s,"classicWorldMembers",new Class[]{int.class},get(s,"classicWorldMapId"));
            byte[] turn=new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION).d(s.playerObjectId()).c(heading).bytes();
            // Native 0x4E45F0 maps base action 1 to current weapon family + 1.
            // The second byte after target is heading, including when target is zero.
            byte[] swing=new Protocol.Writer().c(Protocol.S_ATTACK).c(BattleActionCodes.ATTACK)
                    .d(s.playerObjectId()).d(0).c(0).c(heading).d(0).bytes();
            for (ClientSession peer:recipients) {
                if (peer!=s && (!BattleSpellCatalog.isInNativeScreen((int)get(peer,"positionX"),(int)get(peer,"positionY"),x,y)
                        || !battle && !(boolean)call(s,"sameClassicWorldMap",new Class[]{ClientSession.class},peer))) continue;
                send(peer,turn,"S_CHANGE_DIRECTION empty-melee heading="+heading);
                send(peer,swing,"S_ATTACK empty-melee target=0 damage=0 action="+animation.actionId()+" interval-ms="+interval);
            }
            return true;
        }
    }

    // Reference ActionIntervalCalculator's ATTACK multipliers; movement-only modes do not accelerate attacks.
    static int interval(int base,int speed,int brave) {
        int ms=speed==2 ? base*2 : speed==1 ? (int)(base*.755) : base;
        if(brave==6) ms=(int)(ms*.775);
        if(brave==1 || brave==5) ms=(int)(ms*.755);
        if(brave==3) ms=(int)((int)(ms*.855)*.9);
        return Math.max(1,ms);
    }
    private static Object get(Object o,String n) {try {var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new IllegalStateException(n,e);}}
    private static void set(Object o,String n,Object v) {try {var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}catch(Exception e){throw new IllegalStateException(n,e);}}
    private static Object call(Object o,String n) throws IOException {return call(o,n,new Class[0]);}
    private static Object call(Object o,String n,Class<?>[] types,Object...args) throws IOException {
        try {var m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
        catch(InvocationTargetException e){if(e.getCause() instanceof IOException io)throw io;throw new IllegalStateException(n,e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(n,e);}
    }
    private static void send(ClientSession s,byte[] p,String note) throws IOException {call(s,"sendEncrypted",new Class[]{byte[].class,String.class},p,note);}
}
