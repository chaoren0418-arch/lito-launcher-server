package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Objects;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;
import static local.lito381.server.ModernShopAuthority.*;

/** Version-eight match-local shop. Source inventory/object IDs never enter this namespace. */
final class ModernShopProtocol {
    static final int REQUEST_BYTES=108,MAX_FRAME_BYTES=256*1024;
    record Request(ModernRooms.Scope scope,Action action,long roomId,long expectedRoomRevision,
                   long roundId,long expectedRevision,Fingerprint token,long itemId,long quantity) {
        Request {
            Objects.requireNonNull(scope);Objects.requireNonNull(action);Objects.requireNonNull(token);
            if(roomId<0 || expectedRoomRevision<0 || (roomId==0)!=(expectedRoomRevision==0))throw bad();
            if(action==Action.READ) {
                if(roundId!=0 || expectedRevision!=0 || itemId!=0 || quantity!=0 || !zeroToken(token))throw bad();
            } else if(roundId<1 || expectedRevision<0 || itemId<1 || quantity<1 || quantity>MAX_QUANTITY)throw bad();
        }
    }
    record Reply(Outcome outcome,ModernRooms.Result rooms) {
        Reply {Objects.requireNonNull(outcome);if((outcome.snapshot()!=null)!=(rooms!=null))throw bad();}
        static Reply exceptional(Status status,Reason reason){return new Reply(ModernShopAuthority.exceptional(status,reason),null);}
    }
    static void putRequest(ByteBuffer out,Request r) {
        var s=r.scope();out.putInt(s.sourceId()).putLong(s.objectId()).putLong(s.epoch()).put((byte)s.channelId())
                .put((byte)0).putShort((short)0).put((byte)r.action().ordinal()).put((byte)0).putShort((short)0)
                .putLong(r.roomId()).putLong(r.expectedRoomRevision()).putLong(r.roundId()).putLong(r.expectedRevision())
                .put(r.token().bytes()).putLong(r.itemId()).putLong(r.quantity());
    }
    static Request getRequest(ByteBuffer in) {
        int source=in.getInt();long object=in.getLong(),epoch=in.getLong();int channel=u8(in);zero(in,3);
        Action action=at(Action.values(),u8(in));zero(in,3);long room=in.getLong(),roomRev=in.getLong(),round=in.getLong(),rev=in.getLong();
        return new Request(new ModernRooms.Scope(source,object,epoch,channel),action,room,roomRev,round,rev,token(in),in.getLong(),in.getLong());
    }
    static void putResult(ByteBuffer out,ModernApplicationProtocol.ShopResult message,int version) throws ModernApplicationProtocol.ProtocolException {
        validate(message,version);var reply=message.reply();var result=reply.outcome();putRequest(out,message.echo());
        out.put((byte)result.status().ordinal()).put((byte)result.reason().ordinal()).put((byte)(result.snapshot()==null?0:1)).put((byte)0);
        if(result.snapshot()==null)return;
        Snapshot s=result.snapshot();out.putLong(s.roundId()).putLong(s.revision()).putLong(s.allowance()).putLong(s.balance()).put(s.token().bytes())
                .putShort((short)s.offers().size()).putShort((short)s.stacks().size());
        for(Product p:s.offers()) {
            out.putLong(p.itemId()).putShort((short)p.sortOrder()).put((byte)p.support().ordinal()).put((byte)0)
                    .putLong(p.buyPrice()).putLong(p.sellPrice()).putLong(p.inventoryGraphic()).putLong(p.groundGraphic()).putLong(p.weight());
            text(out,p.nameToken());out.put((byte)(p.healing()==null?0:1)).put((byte)0).putShort((short)0);
            if(p.healing()!=null){var h=p.healing();out.putLong(h.min()).putLong(h.max()).putLong(h.effect()).putLong(h.delayGroup()).putLong(h.delayMillis());}
        }
        for(Owned stock:s.stacks())out.putLong(stock.itemId()).putLong(stock.quantity()).putLong(stock.unitPaidPrice());
        int lengthAt=out.position();out.putInt(0);int start=out.position();
        ModernRoomProtocol.putResult(out,new ModernApplicationProtocol.RoomResult(message.requestId(),roomEcho(message.echo().scope()),reply.rooms()),version);
        out.putInt(lengthAt,out.position()-start);
        if(version>=9)ModernWeightProtocol.putOptional(out,s.weight());
        if(version>=10)putTimed(out,s);
        if(version>=11)putAmmo(out,s);
        if(version>=47)out.put((byte)(s.materialsEnabled()?1:0)).put(new byte[7]);
    }
    static ModernApplicationProtocol.ShopResult getResult(ByteBuffer in,int requestId,int version) throws ModernApplicationProtocol.ProtocolException {
        Request echo=getRequest(in);Status status=at(Status.values(),u8(in));Reason reason=at(Reason.values(),u8(in));boolean present=bool(in);zero(in,1);
        Snapshot snapshot=null;ModernRooms.Result rooms=null;
        if(present) {
            long round=in.getLong(),revision=in.getLong(),allowance=in.getLong(),balance=in.getLong();Fingerprint token=token(in);
            int offers=count(in),stacks=count(in);var products=new ArrayList<Product>(offers);var owned=new ArrayList<Owned>(stacks);var ammoIds=new java.util.ArrayList<Long>();
            for(int i=0;i<offers;i++) {
                long id=in.getLong();int sort=Short.toUnsignedInt(in.getShort());var support=at(ModernShopCatalog.Support.values(),u8(in));zero(in,1);
                if(version<47&&support==ModernShopCatalog.Support.MATERIAL)throw bad();
                if(version<10 && support==ModernShopCatalog.Support.TIMED)throw bad();
                if(version<11 && support==ModernShopCatalog.Support.AMMO)throw bad();
                if(support==ModernShopCatalog.Support.AMMO){ammoIds.add(id);support=ModernShopCatalog.Support.UNIMPLEMENTED;}
                long buy=in.getLong(),sell=in.getLong(),inventory=in.getLong(),ground=in.getLong(),weight=in.getLong();String name=text(in);
                boolean healing=bool(in);zero(in,3);ModernShopCatalog.Healing h=null;
                if(healing){long min=in.getLong(),max=in.getLong(),effect=in.getLong(),group=in.getLong(),delay=in.getLong();
                    if(effect<1 || effect>Integer.MAX_VALUE)throw bad();h=new ModernShopCatalog.Healing(min,max,(int)effect,group,delay);}
                var timed=support==ModernShopCatalog.Support.TIMED?new ModernShopCatalog.Timed(kindFor(id)):null;
                // The appendix supplies the verified owner eligibility later.
                products.add(new Product(id,sort,buy,sell,name,inventory,ground,weight,support,h,timed,false));
            }
            for(int i=0;i<stacks;i++)owned.add(new Owned(in.getLong(),in.getLong(),in.getLong()));
            int length=in.getInt();if(length<1 || length>ModernRoomProtocol.MAX_FRAME_BYTES-16 || length>in.remaining())throw bad();
            var nested=in.slice().order(in.order());nested.limit(length);
            var room=ModernRoomProtocol.getResult(nested,requestId,version);
            if(nested.hasRemaining() || !room.echo().equals(roomEcho(echo.scope())))throw bad();
            ModernRoomProtocol.validate(room);rooms=room.result();in.position(in.position()+length);
            var weight=version>=9?ModernWeightProtocol.getOptional(in):null;
            var timed=version>=10?getTimed(in,products):null;
            boolean ammo=version>=11&&getAmmo(in,products,ammoIds);
            boolean materials=false;if(version>=47){materials=bool(in);zero(in,7);}
            snapshot=new Snapshot(round,revision,allowance,balance,token,products,owned,weight,timed,ammo,materials);
        }
        var result=new ModernApplicationProtocol.ShopResult(requestId,echo,new Reply(new Outcome(status,reason,snapshot),rooms));
        validate(result,version);return result;
    }
    static void validate(ModernApplicationProtocol.ShopResult result,int version) throws ModernApplicationProtocol.ProtocolException {
        if(version<8 || result.echo()==null || result.reply()==null)throw new ModernApplicationProtocol.ProtocolException();
        var echo=result.echo();var reply=result.reply();var outcome=reply.outcome();
        if(version<9 && outcome.reason()==Reason.TOO_HEAVY)throw bad();
        if(outcome.snapshot()!=null) {
            if(version<47&&(outcome.snapshot().materialsEnabled()||outcome.snapshot().offers().stream()
                    .anyMatch(p->p.support()==ModernShopCatalog.Support.MATERIAL)))throw bad();
            if(version<9 && outcome.snapshot().weight()!=null)throw bad();
            if(version<10 && (outcome.snapshot().timed()!=null || outcome.snapshot().offers().stream()
                    .anyMatch(p->p.support()==ModernShopCatalog.Support.TIMED)))throw bad();
            if(version<11&&(outcome.snapshot().ammoEnabled()||outcome.snapshot().offers().stream().anyMatch(p->p.ammo()!=null)))throw bad();
            ModernWeightProtocol.validateShop(outcome.snapshot().weight(),outcome.snapshot().stacks());
        }
        if(outcome.reason()==Reason.TOO_HEAVY) {
            var snapshot=outcome.snapshot();
            if(outcome.status()!=Status.DENIED || echo.action()!=Action.BUY || snapshot==null || snapshot.weight()==null
                    || !snapshot.weight().view().applicable() || snapshot.roundId()!=echo.roundId()
                    || snapshot.revision()!=echo.expectedRevision() || !snapshot.token().equals(echo.token()))throw bad();
            var offer=snapshot.offers().stream().filter(product->product.itemId()==echo.itemId()).findFirst();
            // Existing stock retains its old unit weight, but admission prices the
            // proposed additional quantity using the fresh catalog's unit weight.
            if(offer.isEmpty() || (offer.get().support()!=ModernShopCatalog.Support.HEALING
                    && offer.get().support()!=ModernShopCatalog.Support.TIMED&&offer.get().support()!=ModernShopCatalog.Support.AMMO&&offer.get().support()!=ModernShopCatalog.Support.MATERIAL)
                    || ModernInventoryWeight.permitsAdd(snapshot.weight().view(),echo.quantity(),offer.get().weight()))throw bad();
        }
        if(reply.rooms()!=null) {
            var room=new ModernApplicationProtocol.RoomResult(result.requestId(),roomEcho(echo.scope()),reply.rooms());ModernRoomProtocol.validate(room);
            if(reply.rooms().status()!=ModernRooms.Status.OK)throw bad();
            var joined=reply.rooms().view().joined();
            if(outcome.status()==Status.OK && (joined==null?0:joined.summary().id())!=echo.roomId())throw bad();
            if(outcome.status()==Status.OK && echo.action()!=Action.READ) {
                if(outcome.snapshot().roundId()!=echo.roundId() || outcome.snapshot().revision()!=Math.incrementExact(echo.expectedRevision())
                        || !outcome.snapshot().token().equals(echo.token()))throw bad();
            }
        }
    }
    private static void putAmmo(ByteBuffer out,Snapshot snapshot) {
        var products=snapshot.offers().stream().filter(p->p.ammo()!=null).toList();
        if(products.size()>2||!snapshot.ammoEnabled()&&!products.isEmpty())throw bad();
        out.put((byte)(snapshot.ammoEnabled()?1:0)).put((byte)0).putShort((short)0).putShort((short)products.size()).putShort((short)0);
        for(var p:products)ModernAmmoProtocol.putFacts(out,p.ammo());
    }
    private static boolean getAmmo(ByteBuffer in,ArrayList<Product> products,java.util.List<Long> expected)throws ModernApplicationProtocol.ProtocolException {
        boolean enabled=bool(in);zero(in,3);int count=Short.toUnsignedInt(in.getShort());zero(in,2);
        if(count>2||count!=expected.size()||!enabled&&count!=0||in.remaining()<count*32)throw bad();
        for(int n=0;n<count;n++) {
            var facts=ModernAmmoProtocol.getFacts(in);if(facts.itemId()!=expected.get(n))throw bad();
            int index=-1;for(int i=0;i<products.size();i++)if(products.get(i).itemId()==facts.itemId())index=i;if(index<0)throw bad();var p=products.get(index);
            products.set(index,new Product(p.itemId(),p.sortOrder(),p.buyPrice(),p.sellPrice(),p.nameToken(),p.inventoryGraphic(),p.groundGraphic(),p.weight(),
                    ModernShopCatalog.Support.AMMO,p.healing(),p.timed(),p.eligible(),facts));
        }
        return enabled;
    }
    private static void putTimed(ByteBuffer out,Snapshot snapshot) {
        var timed=snapshot.timed();
        var products=snapshot.offers().stream().filter(p->p.timed()!=null).toList();
        if(products.size()>4 || timed==null&&!products.isEmpty())throw bad();
        out.put((byte)(timed==null?0:1)).put((byte)(timed==null?0:timed.ownerClass())).putShort((short)0)
                .putShort((short)products.size()).putShort((short)0);
        for(var product:products) {
            var kind=product.timed().kind();
            out.putLong(product.itemId()).put((byte)kind.ordinal()).put((byte)(product.eligible()?1:0))
                    .put((byte)kind.classMask()).put((byte)0).putLong(kind.durationMillis()).putInt(kind.effect());
        }
    }
    private static TimedCatalog getTimed(ByteBuffer in,ArrayList<Product> products) {
        boolean enabled=bool(in);int ownerClass=u8(in);zero(in,2);int count=Short.toUnsignedInt(in.getShort());zero(in,2);
        if(count>4 || !enabled&&(ownerClass!=0 || count!=0))throw bad();
        var timed=enabled?new TimedCatalog(ownerClass):null;
        int expected=0;for(var p:products)if(p.timed()!=null)expected++;
        if(count!=expected)throw bad();
        int index=0;
        for(int n=0;n<count;n++) {
            while(index<products.size() && products.get(index).timed()==null)index++;
            if(index==products.size())throw bad();
            var product=products.get(index);long itemId=in.getLong();var kind=at(ModernTimedEffects.Kind.values(),u8(in));
            boolean eligible=bool(in);int mask=u8(in);zero(in,1);long duration=in.getLong();int effect=in.getInt();
            if(itemId!=product.itemId() || kind!=product.timed().kind() || mask!=kind.classMask()
                    || duration!=kind.durationMillis() || effect!=kind.effect()
                    || eligible!=product.timed().eligible(ownerClass))throw bad();
            products.set(index++,new Product(product.itemId(),product.sortOrder(),product.buyPrice(),product.sellPrice(),product.nameToken(),
                    product.inventoryGraphic(),product.groundGraphic(),product.weight(),product.support(),product.healing(),product.timed(),eligible));
        }
        return timed;
    }
    private static ModernTimedEffects.Kind kindFor(long id) {
        for(var kind:ModernTimedEffects.Kind.values())if(kind.itemId()==id)return kind;
        throw bad();
    }
    static ModernRoomProtocol.Echo roomEcho(ModernRooms.Scope scope){return new ModernRoomProtocol.Echo(scope,ModernRooms.Action.READ,0,0,0);}
    private static boolean zeroToken(Fingerprint token){for(byte b:token.bytes())if(b!=0)return false;return true;}
    private static Fingerprint token(ByteBuffer in){byte[] bytes=new byte[32];in.get(bytes);return new Fingerprint(bytes);}
    private static int u8(ByteBuffer in){return Byte.toUnsignedInt(in.get());}
    private static boolean bool(ByteBuffer in){int n=u8(in);if(n>1)throw bad();return n==1;}
    private static int count(ByteBuffer in){int n=Short.toUnsignedInt(in.getShort());if(n>256)throw bad();return n;}
    private static <T>T at(T[] values,int n){if(n>=values.length)throw bad();return values[n];}
    private static void zero(ByteBuffer in,int n){for(int i=0;i<n;i++)if(in.get()!=0)throw bad();}
    private static void text(ByteBuffer out,String s){byte[] b=s.getBytes(StandardCharsets.UTF_8);out.putShort((short)b.length).put(b);}
    private static String text(ByteBuffer in){int n=Short.toUnsignedInt(in.getShort());if(n>256 || n>in.remaining())throw bad();byte[] b=new byte[n];in.get(b);
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();}
        catch(java.nio.charset.CharacterCodingException error){throw bad();}}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern shop frame");}
}
