package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Public facts from the SAME immutable battle capture. No inventory object,
 * proc row, source database, private match nonce or loadout is reachable here.
 * A retained event is identified by world/entity/life/revision, NOT by the
 * publication revision: unrelated movement or equipment must not replay it.
 * This candidate model does not grant a wire schema or visual resource. */
final class ModernArmorProtectionEffects {
    record Effect(long graphics,long graphicsMode,boolean appliedProtection,long durationSeconds) {
        Effect {
            if(graphics<0||graphics>65535||graphicsMode<0||graphicsMode>2||durationSeconds<1||durationSeconds>1800)throw bad();
        }
    }
    record State(long entityId,long life,long sequence,long revision,long observedNanos,long untilNanos,List<Effect> effects) {
        State {
            effects=List.copyOf(effects);
            if(entityId<1||life<0||sequence<0||revision<1||observedNanos<0||observedNanos>ModernArmorProcProtection.MAX_TIME
                    ||untilNanos<0||untilNanos!=0&&(untilNanos<=observedNanos||untilNanos>observedNanos+ModernArmorProcProtection.MAX_TIME)
                    ||effects.size()>ModernArmorProcs.MAX_ROWS
                    ||sequence==0&&(observedNanos!=0||untilNanos!=0||!effects.isEmpty())
                    ||untilNanos==0&&!effects.isEmpty())throw bad();
            int grants=0;
            for(var effect:effects)if(effect.appliedProtection()) {
                if(++grants>1||untilNanos!=observedNanos+effect.durationSeconds()*ModernArmorProcProtection.SECOND)throw bad();
            }
        }
        static State from(ModernBattleEngine.ArmorProtectionView view) {
            if(view==null)return null;
            var s=view.state();
            return new State(s.actor().entity(),s.actor().life(),s.sequence(),view.revision(),s.observedNanos(),s.untilNanos(),
                    view.triggers().stream().map(t->new Effect(t.graphics(),t.graphicsMode(),t.appliedProtection(),t.durationSeconds())).toList());
        }
        void validate(long worldRevision,ModernBattleEngine.Phase phase,ModernBattleObservation.Clock clock,
                ModernBattleEngine.Actor actor,ModernVitalBalance.State resources,boolean participating) {
            Objects.requireNonNull(resources);
            if(!actor.member().identity().isHuman()||actor.entityId()!=entityId||resources.life()!=life||revision>worldRevision
                    ||observedNanos>Math.max(0,clock.activeElapsedNanos())
                    ||clock.activeElapsedNanos()<0&&(sequence!=0||untilNanos!=0||!effects.isEmpty())
                    ||untilNanos!=0&&(phase!=ModernBattleEngine.Phase.ACTIVE||!participating||resources.vitals().hp()==0
                        ||untilNanos<=clock.activeElapsedNanos())
                    ||!effects.isEmpty()&&(phase!=ModernBattleEngine.Phase.ACTIVE||!participating||resources.vitals().hp()==0))throw bad();
        }
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid armor protection effects");}
    private ModernArmorProtectionEffects(){}
}
