package local.lito381.server;

import java.util.Map;

/** Retained L1AttackList.SKNPC / L1MagicPc qualification for NEW matches.
 * Classic active quest-effect leases are not imported or cast by Modern's
 * current admitted spell set. This is an explicit absence in that match
 * domain, not a claim that those skills or every native immunity exist.
 * When an effect authority is added, this gate must consume its live proof. */
final class ModernSlowNativeRules {
    private static final Map<Integer,Integer> REQUIRED = Map.ofEntries(
        Map.entry(45912,1013),Map.entry(45913,1013),Map.entry(45914,1013),Map.entry(45915,1013),
        Map.entry(45916,1014),Map.entry(45941,1015),Map.entry(45752,4005),Map.entry(45753,4005),
        Map.entry(45675,4006),Map.entry(81082,4006),Map.entry(45625,4006),Map.entry(45674,4006),
        Map.entry(45685,4006),Map.entry(87000,4007),Map.entry(45020,4008),Map.entry(99019,4025));
    static Map<Integer,Integer> requiredEffects(){return REQUIRED;}
    record MissingEffect(int npcTemplateId,int requiredSkill) {
        MissingEffect {
            if(REQUIRED.getOrDefault(npcTemplateId,0)!=requiredSkill||requiredSkill==0)
                throw new IllegalArgumentException("invalid native Slow prerequisite");
        }
    }
    static MissingEffect freshMatchNpc(ModernNpcCatalog.Npc row,ModernNpcRoster.Actor npc) {
        row.validateActor(npc);
        if(!row.pendingCleanMelee().isEmpty())throw new IllegalArgumentException("unqualified Slow NPC");
        Integer required=REQUIRED.get(row.key().npcId());
        return required==null?null:new MissingEffect(row.key().npcId(),required);
    }
    private ModernSlowNativeRules(){}
}
