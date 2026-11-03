package local.lito381.server;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import local.lito381.server.ModernEquipmentOverrideStore.Fingerprint;

/** Selection-owned, ephemeral tournament allowance. Never writes source inventory, Adena or equipment overrides. */
final class ModernShopAuthority implements AutoCloseable {
    static final int MAX_STACKS=256;
    static final long MAX_QUANTITY=1_000_000;
    enum Action { READ, BUY, SELL }
    enum Status { OK, DENIED, CONFLICT, BUSY, UNAVAILABLE, FROZEN }
    enum Reason { NONE, INSUFFICIENT_FUNDS, UNSUPPORTED_ITEM, NOT_OWNED, QUANTITY_LIMIT,
        CAPACITY, STALE_REVISION, CATALOG_CHANGED, ROOM_CHANGED, FROZEN, BACKEND, BUSY, TOO_HEAVY }
    /** An owned match-local catalog stack, deliberately NOT a source inventory object ID. */
    record Stack(ModernShopCatalog.Offer offer,long quantity) {
        Stack { Objects.requireNonNull(offer);if(!purchasable(offer)
                || quantity<1 || quantity>MAX_QUANTITY)throw bad(); }
    }
    record Product(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,
                   long inventoryGraphic,long groundGraphic,long weight,
                   ModernShopCatalog.Support support,ModernShopCatalog.Healing healing,
                   ModernShopCatalog.Timed timed,boolean eligible,ModernAmmo.Facts ammo) {
        Product(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,long inventoryGraphic,long groundGraphic,long weight,
                ModernShopCatalog.Support support,ModernShopCatalog.Healing healing,ModernShopCatalog.Timed timed,boolean eligible) {
            this(itemId,sortOrder,buyPrice,sellPrice,nameToken,inventoryGraphic,groundGraphic,weight,support,healing,timed,eligible,null);
        }
        Product(long itemId,int sortOrder,long buyPrice,long sellPrice,String nameToken,
                long inventoryGraphic,long groundGraphic,long weight,
                ModernShopCatalog.Support support,ModernShopCatalog.Healing healing) {
            this(itemId,sortOrder,buyPrice,sellPrice,nameToken,inventoryGraphic,groundGraphic,weight,support,healing,null,false);
        }
        Product {
            Objects.requireNonNull(support);
            if(itemId<1 || sortOrder<0 || sortOrder>65535 || buyPrice<1 || sellPrice<1
                    || !ModernShopCatalog.validText(nameToken,256) || inventoryGraphic<0 || groundGraphic<0 || weight<0
                    || (support==ModernShopCatalog.Support.HEALING)!=(healing!=null)
                    || (support==ModernShopCatalog.Support.TIMED)!=(timed!=null)
                    || (support==ModernShopCatalog.Support.AMMO)!=(ammo!=null)||ammo!=null&&ammo.itemId()!=itemId
                    || timed!=null&&timed.kind().itemId()!=itemId || timed==null&&eligible
                    || support==ModernShopCatalog.Support.MATERIAL&&(itemId!=ModernSlow.MATERIAL_ID
                        || !nameToken.equals("$508")||inventoryGraphic!=235||groundGraphic!=773))throw bad();
        }
        static Product of(ModernShopCatalog.Offer offer) {return of(offer,false,0);}
        static Product of(ModernShopCatalog.Offer offer,boolean enabled,int characterClass) {
            return of(offer,enabled,false,characterClass);
        }
        static Product of(ModernShopCatalog.Offer offer,boolean enabled,boolean bow,int characterClass) {
            return of(offer,enabled,bow,false,characterClass);
        }
        static Product of(ModernShopCatalog.Offer offer,boolean enabled,boolean bow,boolean materials,int characterClass) {
            var timed=enabled?offer.timed():null;
            var support=!enabled&&offer.support()==ModernShopCatalog.Support.TIMED?ModernShopCatalog.Support.UNIMPLEMENTED:offer.support();
            if(!bow&&support==ModernShopCatalog.Support.AMMO)support=ModernShopCatalog.Support.UNIMPLEMENTED;
            if(!materials&&support==ModernShopCatalog.Support.MATERIAL)support=ModernShopCatalog.Support.UNIMPLEMENTED;
            return new Product(offer.itemId(),offer.sortOrder(),offer.buyPrice(),offer.sellPrice(),offer.nameToken(),
                    offer.inventoryGraphic(),offer.groundGraphic(),offer.weight(),support,offer.healing(),timed,
                    timed!=null&&timed.eligible(characterClass),bow?offer.ammo():null);
        }
    }
    /** Present even for an enabled but empty timed catalog; absent means no grant. */
    record TimedCatalog(int ownerClass) {
        TimedCatalog {if(ownerClass<0 || ownerClass>6)throw bad();}
    }
    record Owned(long itemId,long quantity,long unitPaidPrice) {
        Owned {if(itemId<1 || quantity<1 || quantity>MAX_QUANTITY || unitPaidPrice<1)throw bad();}
    }
    record Snapshot(long roundId,long revision,long allowance,long balance,Fingerprint token,
                    List<Product> offers,List<Owned> stacks,ModernInventoryWeight.Snapshot weight,TimedCatalog timed,boolean ammoEnabled,boolean materialsEnabled) {
        Snapshot(long roundId,long revision,long allowance,long balance,Fingerprint token,List<Product> offers,List<Owned> stacks,ModernInventoryWeight.Snapshot weight,TimedCatalog timed,boolean ammoEnabled) {
            this(roundId,revision,allowance,balance,token,offers,stacks,weight,timed,ammoEnabled,false);
        }
        Snapshot(long roundId,long revision,long allowance,long balance,Fingerprint token,List<Product> offers,List<Owned> stacks,ModernInventoryWeight.Snapshot weight,TimedCatalog timed) {
            this(roundId,revision,allowance,balance,token,offers,stacks,weight,timed,false);
        }
        Snapshot(long roundId,long revision,long allowance,long balance,Fingerprint token,List<Product> offers,List<Owned> stacks) {
            this(roundId,revision,allowance,balance,token,offers,stacks,null,null);
        }
        Snapshot(long roundId,long revision,long allowance,long balance,Fingerprint token,List<Product> offers,List<Owned> stacks,ModernInventoryWeight.Snapshot weight) {
            this(roundId,revision,allowance,balance,token,offers,stacks,weight,null);
        }
        Snapshot {
            Objects.requireNonNull(token);offers=List.copyOf(offers);stacks=List.copyOf(stacks);
            if(roundId<1 || revision<0 || allowance<1 || balance<0 || balance>allowance
                    || offers.size()>256 || stacks.size()>MAX_STACKS)throw bad();
            long previous=0;var sorts=new HashSet<Integer>();
            for(Product p:offers){
                if(p.itemId()<=previous || !sorts.add(p.sortOrder())
                        || p.timed()!=null&&(timed==null || p.eligible()!=p.timed().eligible(timed.ownerClass()))||p.ammo()!=null&&!ammoEnabled
                        ||p.support()==ModernShopCatalog.Support.MATERIAL&&!materialsEnabled)throw bad();
                previous=p.itemId();
            }
            previous=0;for(Owned p:stacks){if(p.itemId()<=previous||p.itemId()==ModernSlow.MATERIAL_ID&&!materialsEnabled)throw bad();previous=p.itemId();}
            if(weight!=null) {
                if(!weight.view().applicable() || weight.units().size()!=stacks.size())throw bad();
                long raw=0;
                for(int i=0;i<stacks.size();i++) {
                    var stock=stacks.get(i);var unit=weight.units().get(i);
                    if(stock.itemId()!=unit.itemId())throw bad();
                    raw=Math.addExact(raw,ModernInventoryWeight.itemWeight(stock.quantity(),unit.unitWeight()));
                }
                if(raw!=weight.view().suppliesWeight())throw bad();
            }
            if((ammoEnabled||materialsEnabled)&&(timed==null||weight==null))throw bad();
        }
    }
    record Outcome(Status status,Reason reason,Snapshot snapshot) {
        Outcome {
            Objects.requireNonNull(status);Objects.requireNonNull(reason);
            boolean present=status==Status.OK || status==Status.DENIED || status==Status.CONFLICT;
            if(present!=(snapshot!=null) || !pair(status,reason))throw bad();
        }
    }
    private final long allowance;
    private final int sourceId;
    private final ModernApplicationProtocol.CharacterRow character;
    private final String account;
    private final boolean timed;
    private final boolean bow;
    private final boolean materials;
    private long round=1, revision, balance;
    private ModernShopCatalog.Snapshot catalog;
    private ModernInventoryWeight.Basis weightBasis;
    private TreeMap<Long,Stack> stacks=new TreeMap<>();
    private boolean closed;
    private long capturedMatch,capturedGeneration,lastTerminalMatch,lastTerminalGeneration;

    ModernShopAuthority(long allowance,int sourceId,ModernApplicationProtocol.CharacterRow character,String account) {
        this(allowance,sourceId,character,account,false);
    }
    ModernShopAuthority(long allowance,int sourceId,ModernApplicationProtocol.CharacterRow character,String account,boolean timed) {
        this(allowance,sourceId,character,account,timed,false);
    }
    ModernShopAuthority(long allowance,int sourceId,ModernApplicationProtocol.CharacterRow character,String account,boolean timed,boolean bow) {
        this(allowance,sourceId,character,account,timed,bow,false);
    }
    /** Explicit opt-in; public material state requires the v47 contract. */
    ModernShopAuthority(long allowance,int sourceId,ModernApplicationProtocol.CharacterRow character,String account,boolean timed,boolean bow,boolean materials) {
        if(allowance<1 || sourceId<1 || character==null || !ModernShopCatalog.validText(account,64))throw bad();
        if(timed&&(character.characterClass()<0 || character.characterClass()>6))throw bad();
        this.allowance=allowance;this.balance=allowance;this.sourceId=sourceId;this.character=character;this.account=account;
        this.timed=timed;
        if((bow||materials)&&!timed)throw bad();this.bow=bow;this.materials=materials;
    }
    synchronized boolean frozen(){return capturedMatch!=0;}
    synchronized Outcome apply(Action action,long expectedRound,long expectedRevision,Fingerprint token,
                               long itemId,long quantity,ModernShopCatalog.Snapshot fresh) {
        return apply(action,expectedRound,expectedRevision,token,itemId,quantity,fresh,null);
    }
    synchronized Outcome apply(Action action,long expectedRound,long expectedRevision,Fingerprint token,
                               long itemId,long quantity,ModernShopCatalog.Snapshot fresh,ModernInventoryWeight.Basis freshWeight) {
        requireOpen();Objects.requireNonNull(action);requireOwner(fresh);
        requireWeight(freshWeight);
        if(capturedMatch!=0)return exceptional(Status.FROZEN,Reason.FROZEN);
        // Validate the entire fresh view BEFORE adopting it; unknown catalog shapes never partially publish.
        var products=products(fresh);long refreshed=revision;
        if(catalog!=null && (!catalog.token().equals(fresh.token())
                || (weightBasis==null)!=(freshWeight==null) || weightBasis!=null&&!weightBasis.sameFacts(freshWeight)))refreshed=Math.incrementExact(revision);
        new Snapshot(round,refreshed,allowance,balance,viewToken(fresh),products,owned(),weight(freshWeight,stacks),timedCatalog(),bow,materials);
        catalog=fresh;revision=refreshed;weightBasis=freshWeight;
        if(action==Action.READ)return result(Status.OK,Reason.NONE);
        if(expectedRound!=round || expectedRevision!=revision)return result(Status.CONFLICT,Reason.STALE_REVISION);
        if(!viewToken(catalog).equals(token))return result(Status.CONFLICT,Reason.CATALOG_CHANGED);
        if(itemId<1 || quantity<1 || quantity>MAX_QUANTITY)throw bad();
        var offer=fresh.offers().stream().filter(p->p.itemId()==itemId).findFirst().orElse(null);
        if(offer==null)return result(Status.DENIED,Reason.UNSUPPORTED_ITEM);
        var existing=stacks.get(itemId);var next=new TreeMap<>(stacks);long nextBalance;
        if(action==Action.BUY) {
            if(!purchasable(offer) || offer.timed()!=null&&!timed||offer.ammo()!=null&&!bow||offer.material()!=null&&!materials)return result(Status.DENIED,Reason.UNSUPPORTED_ITEM);
            if(weightBasis!=null && !ModernInventoryWeight.permitsAdd(weight(weightBasis,stacks).view(),quantity,offer.weight()))
                return result(Status.DENIED,Reason.TOO_HEAVY);
            if(existing!=null && !existing.offer().equals(offer))return result(Status.CONFLICT,Reason.CATALOG_CHANGED);
            if(existing==null && next.size()==MAX_STACKS)return result(Status.DENIED,Reason.CAPACITY);
            long count=existing==null?quantity:Math.addExact(existing.quantity(),quantity);
            if(count>MAX_QUANTITY)return result(Status.DENIED,Reason.QUANTITY_LIMIT);
            long cost=Math.multiplyExact(offer.buyPrice(),quantity);
            if(cost>balance)return result(Status.DENIED,Reason.INSUFFICIENT_FUNDS);
            nextBalance=Math.subtractExact(balance,cost);next.put(itemId,new Stack(offer,count));
        } else {
            if(existing==null)return result(Status.DENIED,Reason.NOT_OWNED);
            if(quantity>existing.quantity())return result(Status.DENIED,Reason.QUANTITY_LIMIT);
            long refund=Math.multiplyExact(Math.min(existing.offer().buyPrice(),offer.sellPrice()),quantity);
            nextBalance=Math.addExact(balance,refund);
            long count=existing.quantity()-quantity;if(count==0)next.remove(itemId);else next.put(itemId,new Stack(existing.offer(),count));
        }
        long nextRevision=Math.incrementExact(revision);
        var candidate=new Snapshot(round,nextRevision,allowance,nextBalance,viewToken(catalog),products,
                next.values().stream().map(s->new Owned(s.offer().itemId(),s.quantity(),s.offer().buyPrice())).toList(),weight(weightBasis,next),timedCatalog(),bow,materials);
        // The single commit: no source SQL, native sidecar, equipment store or external callback.
        stacks=next;balance=nextBalance;revision=nextRevision;
        return new Outcome(Status.OK,Reason.NONE,candidate);
    }
    synchronized Outcome conflict(ModernShopCatalog.Snapshot fresh,Reason reason) {
        return conflict(fresh,reason,null);
    }
    synchronized Outcome conflict(ModernShopCatalog.Snapshot fresh,Reason reason,ModernInventoryWeight.Basis basis) {
        Outcome read=apply(Action.READ,0,0,null,0,0,fresh,basis);
        return read.status()==Status.FROZEN?read:new Outcome(Status.CONFLICT,reason,read.snapshot());
    }
    synchronized List<Stack> capture(long matchId,long generation,long deadline,ModernShopCatalog.Snapshot fresh) {
        return capture(matchId,generation,deadline,fresh,null);
    }
    synchronized List<Stack> capture(long matchId,long generation,long deadline,ModernShopCatalog.Snapshot fresh,ModernInventoryWeight.Basis basis) {
        return capture(matchId,generation,deadline,fresh,basis,null);
    }
    synchronized List<Stack> capture(long matchId,long generation,long deadline,ModernShopCatalog.Snapshot fresh,ModernInventoryWeight.Basis basis,ModernMatchDescriptor descriptor) {
        requireOpen();requireOwner(fresh);
        if(descriptor!=null&&descriptor.has(ModernMatchDescriptor.SLOW)&&!materials)throw bad(); // A capable shop may enter old rooms only without material stock.
        if(descriptor!=null&&(!descriptor.has(ModernMatchDescriptor.ITEMS)||descriptor.has(ModernMatchDescriptor.WEIGHT)&&basis==null))throw bad();
        requireWeight(basis);
        if(matchId<1 || generation<1 || System.nanoTime()-deadline>=0 || matchId<=lastTerminalMatch
                || capturedMatch!=0 && (capturedMatch!=matchId || capturedGeneration!=generation))throw bad();
        products(fresh);
        for(Stack stock:stacks.values()) {
            if(descriptor!=null&&(!descriptor.has(ModernMatchDescriptor.ITEMS)
                    ||stock.offer().timed()!=null&&!descriptor.has(ModernMatchDescriptor.TIMED)
                    ||stock.offer().ammo()!=null&&!descriptor.has(ModernMatchDescriptor.AMMO)
                    ||stock.offer().material()!=null&&!descriptor.has(ModernMatchDescriptor.SLOW)))throw bad();
            var current=fresh.offers().stream().filter(p->p.itemId()==stock.offer().itemId()).findFirst().orElseThrow(ModernShopAuthority::bad);
            if(!purchasable(current) || current.timed()!=null&&!timed || current.ammo()!=null&&!bow||current.material()!=null&&!materials||!current.template().equals(stock.offer().template())
                    || !Objects.equals(current.healing(),stock.offer().healing())
                    || !Objects.equals(current.timed(),stock.offer().timed())||!Objects.equals(current.ammo(),stock.offer().ammo()))throw bad();
        }
        if(stacks.size()>(bow?9:timed?7:3)+(materials?1:0))throw bad();
        var result=List.copyOf(stacks.values());
        weight(basis,stacks); // Validate complete original-price/template stock before freezing.
        if(System.nanoTime()-deadline>=0)throw bad();
        capturedMatch=matchId;capturedGeneration=generation;return result;
    }
    synchronized void terminal(long matchId,long generation) {
        if(closed)return;
        if(matchId<1 || generation<1)throw bad();
        if(matchId<lastTerminalMatch)return;
        if(matchId==lastTerminalMatch){if(generation!=lastTerminalGeneration)throw bad();return;}
        if(capturedMatch>matchId)return;
        if(capturedMatch==matchId && capturedGeneration!=generation)throw bad();
        long nextRound=Math.incrementExact(round),nextRevision=Math.incrementExact(revision);
        lastTerminalMatch=matchId;lastTerminalGeneration=generation;capturedMatch=0;capturedGeneration=0;
        stacks.clear();balance=allowance;round=nextRound;revision=nextRevision;
    }
    @Override public synchronized void close(){closed=true;stacks.clear();catalog=null;capturedMatch=0;capturedGeneration=0;}
    private List<Product> products(ModernShopCatalog.Snapshot fresh){return fresh.offers().stream().map(p->Product.of(p,timed,bow,materials,character.characterClass())).sorted(Comparator.comparingLong(Product::itemId)).toList();}
    private List<Owned> owned(){return stacks.values().stream().map(s->new Owned(s.offer().itemId(),s.quantity(),s.offer().buyPrice())).toList();}
    private Outcome result(Status status,Reason reason){return new Outcome(status,reason,new Snapshot(round,revision,allowance,balance,viewToken(catalog),products(catalog),owned(),weight(weightBasis,stacks),timedCatalog(),bow,materials));}
    private TimedCatalog timedCatalog(){return timed?new TimedCatalog(character.characterClass()):null;}
    private Fingerprint viewToken(ModernShopCatalog.Snapshot fresh) {
        if(materials || fresh.offers().stream().anyMatch(p->p.material()!=null)) {
            try{var hash=MessageDigest.getInstance("SHA-256");hash.update("LITO-SHOP-MATERIAL-PROJECTION\0".getBytes(StandardCharsets.US_ASCII));hash.update((byte)(timed?1:0));hash.update((byte)(bow?1:0));hash.update((byte)(materials?1:0));hash.update(fresh.token().bytes());return new Fingerprint(hash.digest());}
            catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("shop projection fingerprint unavailable");}
        }
        if(!bow&&fresh.offers().stream().anyMatch(p->p.ammo()!=null)) {
            try{var hash=MessageDigest.getInstance("SHA-256");hash.update("LITO-SHOP-AMMO-PROJECTION\0".getBytes(StandardCharsets.US_ASCII));hash.update((byte)(timed?1:0));hash.update(fresh.token().bytes());return new Fingerprint(hash.digest());}
            catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("shop projection fingerprint unavailable");}
        }
        if(timed || fresh.offers().stream().noneMatch(p->p.support()==ModernShopCatalog.Support.TIMED))return fresh.token();
        try {
            var hash=MessageDigest.getInstance("SHA-256");
            hash.update("LITO-SHOP-TIMED-PROJECTION\0".getBytes(StandardCharsets.US_ASCII));
            hash.update((byte)0);hash.update(fresh.token().bytes());return new Fingerprint(hash.digest());
        } catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("shop projection fingerprint unavailable");}
    }
    private static boolean purchasable(ModernShopCatalog.Offer offer) {
        return offer.support()==ModernShopCatalog.Support.HEALING || offer.support()==ModernShopCatalog.Support.TIMED||offer.support()==ModernShopCatalog.Support.AMMO||offer.support()==ModernShopCatalog.Support.MATERIAL;
    }
    private static ModernInventoryWeight.Snapshot weight(ModernInventoryWeight.Basis basis,TreeMap<Long,Stack> stocks) {
        return basis==null?null:ModernInventoryWeight.fromShop(basis,List.copyOf(stocks.values()));
    }
    private void requireWeight(ModernInventoryWeight.Basis basis) {
        if(materials&&basis==null)throw bad();
        if(basis!=null && (basis.source().sourceId()!=sourceId || !basis.source().character().equals(character)
                || basis.validUntilMillis()>=0&&System.currentTimeMillis()>=basis.validUntilMillis()))throw bad();
        if(weightBasis!=null&&basis==null)throw bad();
    }
    static Outcome exceptional(Status status,Reason reason){return new Outcome(status,reason,null);}
    private void requireOwner(ModernShopCatalog.Snapshot fresh){if(fresh==null || fresh.sourceId()!=sourceId || !character.equals(fresh.character()) || !account.equals(fresh.canonicalAccount()))throw new SecurityException("shop source binding mismatch");}
    private void requireOpen(){if(closed)throw new IllegalStateException("shop selection retired");}
    static boolean pair(Status status,Reason reason){return switch(status){case OK->reason==Reason.NONE;case DENIED->reason.ordinal()>=1&&reason.ordinal()<=5||reason==Reason.TOO_HEAVY;case CONFLICT->reason.ordinal()>=6&&reason.ordinal()<=8;case BUSY->reason==Reason.BUSY;case UNAVAILABLE->reason==Reason.BACKEND;case FROZEN->reason==Reason.FROZEN;};}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid modern match-local shop state");}
}
