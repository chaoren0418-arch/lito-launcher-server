package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernAmmoTest {
    static ModernAmmo.Facts facts(long id){return new ModernAmmo.Facts(id,id==40744?7:10,id==40744?6:9,-1,true,id==40744?0:1,0);}
    static ModernAmmo.State initial(){return ModernAmmo.State.initial(1,List.of(new ModernAmmo.Stock(facts(40744),2,2),new ModernAmmo.Stock(facts(40746),3,3)));}
    @Test void initialSelectionIsLowestOwnedAndEmptyIsCanonical() {
        assertEquals(40744,initial().selectedItemId());assertEquals(0,initial().revision());
        assertEquals(0,ModernAmmo.State.initial(2,List.of()).selectedItemId());
        assertThrows(IllegalArgumentException.class,()->ModernAmmo.State.initial(1,List.of(initial().stocks().get(1),initial().stocks().get(0))));
    }
    @Test void selectAndDeniedSelectionKeepHistoricalShotCoordinatesAndCounter() {
        var shot=initial().shot(2,1,2,32724,32784,32725,32784,false);
        var selected=shot.selected(1,3,40746,true);var denied=selected.selected(2,4,40010,false);
        assertEquals(40746,denied.selectedItemId());assertEquals(3,denied.selectionRevision());assertEquals(1,denied.shotCount());
        assertEquals(2,denied.shotRevision());assertEquals(40744,denied.shotItemId());assertEquals(32725,denied.toX());
        assertEquals(shot.stocks(),denied.stocks());assertEquals(2,denied.requestSequence());
    }
    @Test void hitAndMissDebitExactlyOneAndExhaustionNeverAutoSelects() {
        var miss=initial().shot(1,1,2,1,1,2,1,false);var hit=miss.shot(2,3,2,1,1,2,1,true);
        assertEquals(2,hit.shotCount());assertEquals(0,hit.selected().remaining());assertEquals(40744,hit.selectedItemId());
        assertEquals(3,hit.stocks().get(1).remaining());assertThrows(IllegalArgumentException.class,()->hit.shot(3,4,2,1,1,2,1,true));
        assertEquals(40746,hit.selected(1,3,40746,true).selectedItemId());
    }
    @Test void forgedUnselectedDebitAndSelectionAtShotRevisionAreRejected() {
        var stocks=List.of(new ModernAmmo.Stock(facts(40744),2,2),new ModernAmmo.Stock(facts(40746),3,2));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.State(1,0,2,0,40744,1,2,40746,1,2,1,1,2,1,66,true,stocks));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.State(1,1,2,2,40746,1,2,40746,1,2,1,1,2,1,66,true,stocks));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.State(1,0,0,0,40746,0,0,0,0,0,0,0,0,0,0,false,initial().stocks()));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.State(1,0,1,0,40744,0,0,0,0,0,0,0,0,0,0,false,initial().stocks()));
        var both=List.of(new ModernAmmo.Stock(facts(40744),2,1),new ModernAmmo.Stock(facts(40746),3,2));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.State(1,0,2,0,40744,2,2,40744,2,2,1,1,2,1,66,true,both));
    }
    @Test void wideSourceDamageIsPreservedWhileMalformedFactsFailClosed() {
        assertEquals(Long.MAX_VALUE,new ModernAmmo.Facts(40744,Long.MAX_VALUE,Long.MAX_VALUE,66,true,0,0).small());
        for(long id:new long[]{0,40010,0x1_0000_9f28L})assertThrows(IllegalArgumentException.class,()->facts(id));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.Facts(40744,0,1,-1,true,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.Facts(40744,1,1,-1,false,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.Facts(40744,1,1,-1,true,1,0));
        assertThrows(IllegalArgumentException.class,()->new ModernAmmo.Stock(facts(40744),1_000_001,1));
    }
    @Test void stateWireIs104Plus48PerStockWithReservedBytesStrict()throws Exception {
        var state=initial().shot(3,2,2,32724,32784,32725,32784,true).selected(1,4,40746,true);
        var out=ByteBuffer.allocate(8+104+2*48);ModernAmmoProtocol.putStates(out,List.of(state));assertEquals(out.capacity(),out.position());
        assertEquals(List.of(state),ModernAmmoProtocol.getStates(ByteBuffer.wrap(out.array()),1));
        for(int offset:new int[]{2,7,8+102,8+103,8+104+47}) {
            byte[] broken=out.array().clone();broken[offset]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernAmmoProtocol.getStates(ByteBuffer.wrap(broken),1));
        }
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernAmmoProtocol.getStates(ByteBuffer.wrap(out.array()),2));
    }
    @Test void requestGoldenUsesIndependentSequenceAndRequiresVersionEleven()throws Exception {
        var scope=new ModernRooms.Scope(7,0x1_0000_0001L,-1,3);
        var request=new ModernApplicationProtocol.AmmoRequest(1,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.SELECT,2,3,4,40746));
        byte[] encoded=ModernApplicationProtocol.encode(request,11);assertEquals(72,encoded.length);assertEquals(36,ByteBuffer.wrap(encoded).getShort(12));
        assertEquals(request,ModernApplicationProtocol.decode(encoded,11));
        for(int version=1;version<=10;version++){int old=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request,old));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(encoded,old));}
        for(int offset:new int[]{38,39}){byte[] broken=encoded.clone();broken[offset]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(broken,11));}
    }
}
