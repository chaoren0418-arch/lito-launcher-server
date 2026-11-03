package local.lito381.server;

import java.util.*;

/** Server-issued one-shot list admission. No wire field manufactures this owner.
 * Cutoff is the last admission instant. Once admitted, an atomic durable transfer
 * may finish; its clan pin remains held until the actual transaction finally. */
final class NativeWarehouseAdmission {
    record Owner(Object session,String characterIdentity,String sourceName,String account,
                 long sourceObject,long nativeActor,String phase,long scene,long npc) {
        Owner {Objects.requireNonNull(session);Objects.requireNonNull(characterIdentity);Objects.requireNonNull(sourceName);Objects.requireNonNull(account);Objects.requireNonNull(phase);if(sourceObject<=0||nativeActor<=0||scene==0||npc<=0)throw new IllegalArgumentException("warehouse owner");}
    }
    enum State { ARMED, ADMITTED, RETIRED }
    private final Owner owner;private final Object exactListToken;private final Set<Integer> modes;private final long cutoff;
    private State state=State.ARMED;
    NativeWarehouseAdmission(Owner owner,Object token,Set<Integer> modes,long exclusiveCutoff){
        this.owner=Objects.requireNonNull(owner);exactListToken=Objects.requireNonNull(token);this.modes=Set.copyOf(modes);cutoff=exclusiveCutoff;
        if(modes.isEmpty()||modes.stream().anyMatch(m->!NativeWarehouseCdb.mode(m)))throw new IllegalArgumentException("warehouse modes");
    }
    synchronized boolean matches(Owner current,Object token,int mode,long now){return state==State.ARMED&&owner.equals(current)&&token==exactListToken&&modes.contains(mode)&&now-cutoff<0;}
    synchronized Permit admit(Owner current,Object token,int mode,long now){if(!matches(current,token,mode,now))return null;state=State.ADMITTED;return new Permit(this);}
    synchronized void retire(){if(state==State.ARMED)state=State.RETIRED;}
    synchronized State state(){return state;}
    static final class Permit implements AutoCloseable {
        private final NativeWarehouseAdmission admission;private boolean closed;
        private Permit(NativeWarehouseAdmission owner){admission=owner;}
        @Override public void close(){synchronized(admission){if(!closed){closed=true;admission.state=State.RETIRED;}}}
    }
}
