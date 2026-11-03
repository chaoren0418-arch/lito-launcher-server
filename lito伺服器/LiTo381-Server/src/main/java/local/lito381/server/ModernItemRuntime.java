package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Immutable live request/stock authority. Historical use and current equipment
 * bonus belong to ModernUseHistory.Items, never to a fixed-profile wire DTO.
 * Admission, owner, class, cooldown, resources and RNG stay engine-owned.
 * This is not authorization to enable advanced SET or a new wire capability. */
final class ModernItemRuntime {
    record State(long requestSequence,long requestRevision,List<ModernItemProtocol.Stock> stocks) {
        State {
            stocks=List.copyOf(stocks);
            if(requestSequence<0||requestRevision<0||(requestSequence==0)!=(requestRevision==0)
                    ||requestSequence>0&&requestSequence>=requestRevision||consumed(stocks)>requestSequence)throw bad();
        }
        static State initial(List<ModernItemProtocol.Stock> stocks){return new State(0,0,stocks);}
        void validate(ModernUseHistory.Items history) {
            Objects.requireNonNull(history);
            if(history.uses()!=consumed(stocks))throw bad();
            var last=history.last();
            if(last!=null&&(last.sequence()>requestSequence||last.revision()>requestRevision
                    ||(last.sequence()==requestSequence)!=(last.revision()==requestRevision)
                    ||stocks.stream().noneMatch(s->s.itemId()==last.itemId()&&s.remaining()<s.initial())))throw bad();
        }
        void validate(long worldRevision,ModernUseHistory.Items history) {
            validate(history);
            if(worldRevision<1||requestRevision>worldRevision||history.profileRevision()>worldRevision)throw bad();
        }
        private State next(long revision,long sequence,List<ModernItemProtocol.Stock> remaining) {
            if(sequence!=Math.incrementExact(requestSequence)||revision<=requestRevision)throw bad();
            return new State(sequence,revision,remaining);
        }
        State requested(long revision,long sequence){return next(revision,sequence,stocks);}
        State used(long revision,long sequence,ModernUseHistory.Items before,ModernUseHistory.Items after) {
            validate(revision-1,before);Objects.requireNonNull(after);
            var use=after.last();
            if(use==null||use.revision()!=revision||use.sequence()!=sequence||after.uses()!=Math.incrementExact(before.uses())
                    ||!ModernUseHistory.follows(before,after)
                    ||stocks.stream().noneMatch(s->s.itemId()==use.itemId()&&s.remaining()>0))throw bad();
            var remaining=stocks.stream().map(s->s.itemId()==use.itemId()
                    ?new ModernItemProtocol.Stock(s.itemId(),s.initial(),s.remaining()-1):s).toList();
            var result=next(revision,sequence,remaining);result.validate(revision,after);return result;
        }
    }
    private static long consumed(List<ModernItemProtocol.Stock> stocks) {
        if(stocks.size()>ModernItemProtocol.MAX_TIMED_STOCKS)throw bad();
        long previous=0,total=0;
        for(var s:stocks){if(s.itemId()<=previous)throw bad();previous=s.itemId();total=Math.addExact(total,s.initial()-s.remaining());}
        return total;
    }
    static int effect(ModernUseHistory.Items history) {
        var use=history.last();if(use==null)return 0;
        var potion=ModernItemProtocol.HealingKind.find(use.itemId());
        return potion==null?ModernTimedEffects.Kind.find(use.itemId()).effect():potion.effect;
    }
    static int delayMillis(ModernUseHistory.Items history) {
        var use=history.last();if(use==null)return 0;
        var potion=ModernItemProtocol.HealingKind.find(use.itemId());return potion==null?0:potion.delayMillis;
    }
    private ModernItemRuntime(){}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical match supplies");}
}
