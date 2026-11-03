package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static local.lito381.server.ModernBattleWire.*;

/** Schema9 preserves every schema8 section byte for byte and adds one typed
 * owner-only section. Explicit negotiation is independent of resource loading.
 * No graphics substitution, event deduplication or private catalog is encoded. */
final class ModernBattleArmorPresentationWire {
    static final int SCHEMA=9,SECTION=17;
    static final int GRAPHICS_LIMIT=ModernArmorProcs.MAX_ROWS+1;
    static final int BATCH_LIMIT=3*8+8+2*4+4+ModernArmorPresentation.MAX_VISUALS*9;
    static final int LIMIT=7*8+4+GRAPHICS_LIMIT*4+4+4+1+BATCH_LIMIT;
    static final int MAX_BYTES=ModernBattleArmorWire.MAX_BYTES+8+LIMIT;
    static byte[] encode(ModernBattleAudience.View view) {
        if(view==null||view.armorPresentation()==null)throw new IllegalArgumentException("missing armor presentation");
        var candidate=view.armorPresentation();
        var count=new Writer(null,LIMIT);put(count,candidate);
        var base=ModernBattleWire.encodeArmorProtection(view);
        int length=Math.addExact(base.length,Math.addExact(8,count.position));
        if(length>MAX_BYTES)throw new IllegalArgumentException("armor presentation too large");
        var bytes=Arrays.copyOf(base,length);var buffer=ByteBuffer.wrap(bytes);
        buffer.position(base.length);buffer.putShort((short)SECTION).putShort((short)0).putInt(count.position);
        var out=new Writer(buffer,LIMIT);put(out,candidate);
        if(out.position!=count.position||buffer.position()!=length)throw new IllegalArgumentException("armor presentation size mismatch");
        buffer.putShort(4,(short)SCHEMA);buffer.putInt(8,length);buffer.putShort(12,(short)SECTION);
        return bytes;
    }
    static void put(Writer w,ModernArmorPresentation.Candidate c) {
        world(w,c.world());w.l(c.worldRevision());w.l(c.entityId());w.l(c.life());w.l(c.equipmentRevision());
        w.list(c.required().graphics(),GRAPHICS_LIMIT,Writer::i);w.list(c.required().icons(),1,Writer::i);
        w.optional(c.batch(),(b,v)->{
            b.l(v.sequence());b.l(v.revision());b.l(v.observedNanos());b.l(v.anchor().moveSequence());cell(b,v.anchor().cell());
            b.list(v.visuals(),ModernArmorPresentation.MAX_VISUALS,(d,e)->{d.i(e.ordinal());d.i(e.graphic());d.e(e.role());});
        });
    }
    private ModernBattleArmorPresentationWire(){}
}
