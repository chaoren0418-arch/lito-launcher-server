package local.lito381.server;

import java.io.IOException;

/** TEST JAR ONLY: fail before private save can inspect persistence or touch a filesystem. */
final class WeaponStateFault {
    static GameStateStore armed;
    static int failAt,calls,failures,failedOrdinal;
    static void arm(GameStateStore state,int at){
        if(armed!=null||at<1)throw new AssertionError("invalid fault arm");
        if((boolean)CombatAccess.field(state,"persistent"))throw new AssertionError("persistent store forbidden");
        armed=state;failAt=at;calls=0;failures=0;failedOrdinal=0;
    }
    static void before(GameStateStore state)throws IOException{
        if(state==armed&&++calls==failAt){failures++;failedOrdinal=calls;throw new IOException("detached-weapon-state-save-failure");}
    }
    static void clear(){armed=null;failAt=0;}
    private WeaponStateFault(){}
}
