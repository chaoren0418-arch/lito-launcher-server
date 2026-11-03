package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Match-local arrows: not source item object IDs, and not potion USE events. */
final class ModernAmmo {
    static final int MAX_STOCKS=2, SPRITE=66;
    static final long MAX_QUANTITY=1_000_000;
    enum Kind {
        SILVER(40744,0,"silver"), MITHRIL(40746,1,"mithril");
        private final long itemId;private final int material;private final String sourceMaterial;
        Kind(long id,int material,String sourceMaterial){this.itemId=id;this.material=material;this.sourceMaterial=sourceMaterial;}
        long itemId(){return itemId;}int material(){return material;}String sourceMaterial(){return sourceMaterial;}
        static Kind find(long id){for(var k:values())if(k.itemId==id)return k;return null;}
    }
    record Facts(long itemId,long small,long large,int rawArrowSprite,boolean consume,int material,int blessing) {
        Facts {
            var kind=Kind.find(itemId);
            if(kind==null || small<1 || large<1 || rawArrowSprite!=-1&&rawArrowSprite!=SPRITE
                    || !consume || material!=kind.material() || blessing!=0)throw bad();
        }
    }
    record Stock(Facts facts,long initial,long remaining) {
        Stock {Objects.requireNonNull(facts);if(initial<1||initial>MAX_QUANTITY||remaining<0||remaining>initial)throw bad();}
        long itemId(){return facts.itemId();}
        Stock debit(){if(remaining==0)throw bad();return new Stock(facts,initial,remaining-1);}
    }
    record State(long entityId,long requestSequence,long revision,long selectionRevision,long selectedItemId,
                 long shotCount,long shotRevision,long shotItemId,long attackSequence,long lastTargetEntityId,
                 int fromX,int fromY,int toX,int toY,int projectileSprite,boolean hit,List<Stock> stocks) {
        State {
            stocks=List.copyOf(stocks);
            if(entityId<1||requestSequence<0||revision<0||selectionRevision<0||selectionRevision>revision
                    || shotCount<0||shotRevision<0||shotRevision>revision||attackSequence<0||stocks.size()>MAX_STOCKS)throw bad();
            long previous=0,consumed=0;
            for(var stock:stocks){if(stock.itemId()<=previous)throw bad();previous=stock.itemId();consumed=Math.addExact(consumed,stock.initial()-stock.remaining());}
            if(consumed!=shotCount || stocks.isEmpty()!=(selectedItemId==0)
                    || selectedItemId!=0&&stocks.stream().noneMatch(s->s.itemId()==selectedItemId)
                    || selectionRevision>0&&requestSequence==0 || requestSequence>0&&revision==0)throw bad();
            if((revision==0)!=(requestSequence==0&&shotCount==0)
                    ||selectionRevision==0&&selectedItemId!=(stocks.isEmpty()?0:stocks.get(0).itemId())
                    ||selectionRevision==0&&stocks.stream().anyMatch(s->s.itemId()!=selectedItemId&&s.remaining()!=s.initial())
                    ||shotCount>0&&(selectionRevision==shotRevision||shotRevision>selectionRevision&&shotItemId!=selectedItemId)
                    ||shotCount>attackSequence)throw bad();
            if(shotCount==0) {
                if(shotRevision!=0||shotItemId!=0||attackSequence!=0||lastTargetEntityId!=0||fromX!=0||fromY!=0||toX!=0||toY!=0||projectileSprite!=0||hit)throw bad();
            } else if(shotRevision==0||attackSequence==0||lastTargetEntityId<1||lastTargetEntityId==entityId||projectileSprite!=SPRITE
                    || fromX==toX&&fromY==toY||stocks.stream().noneMatch(s->s.itemId()==shotItemId&&s.remaining()<s.initial()))throw bad();
        }
        static State initial(long entity,List<Stock> stocks) {
            long selected=stocks.stream().filter(s->s.remaining()>0).mapToLong(Stock::itemId).min().orElse(0);
            return new State(entity,0,0,0,selected,0,0,0,0,0,0,0,0,0,0,false,stocks);
        }
        Stock selected(){return stocks.stream().filter(s->s.itemId()==selectedItemId).findFirst().orElse(null);}
        State selected(long sequence,long nextRevision,long itemId,boolean success) {
            if(sequence!=Math.incrementExact(requestSequence)||nextRevision<=revision)throw bad();
            if(success&&stocks.stream().noneMatch(s->s.itemId()==itemId&&s.remaining()>0))throw bad();
            return new State(entityId,sequence,nextRevision,success?nextRevision:selectionRevision,success?itemId:selectedItemId,
                    shotCount,shotRevision,shotItemId,attackSequence,lastTargetEntityId,fromX,fromY,toX,toY,projectileSprite,hit,stocks);
        }
        State shot(long nextRevision,long sequence,long target,int x,int y,int tx,int ty,boolean didHit) {
            var stock=selected();if(stock==null||stock.remaining()==0||nextRevision<=revision||sequence<=attackSequence)throw bad();
            var next=stocks.stream().map(s->s.itemId()==selectedItemId?s.debit():s).toList();
            return new State(entityId,requestSequence,nextRevision,selectionRevision,selectedItemId,Math.incrementExact(shotCount),
                    nextRevision,selectedItemId,sequence,target,x,y,tx,ty,SPRITE,didHit,next);
        }
        State wireRevision() {
            return new State(entityId,requestSequence,wire(revision),wire(selectionRevision),selectedItemId,shotCount,wire(shotRevision),
                    shotItemId,attackSequence,lastTargetEntityId,fromX,fromY,toX,toY,projectileSprite,hit,stocks);
        }
        private static long wire(long value){return value==0?0:Math.incrementExact(value);}
    }
    private ModernAmmo(){}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid qualified match arrow state");}
}
