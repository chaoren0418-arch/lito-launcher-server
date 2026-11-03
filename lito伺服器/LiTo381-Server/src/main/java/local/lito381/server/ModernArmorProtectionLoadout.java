package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Strict internal engine preparation, not resource qualification, negotiation or public admission.
 * Unknown weapon procs, custom siblings and incomplete catalog provenance remain rejected. */
final class ModernArmorProtectionLoadout {
    private final ModernArmorProtectionCatalog.BoundSource bound;
    private final ModernCombatProfile profile;
    private final Set<Long> permitted;
    private final List<ModernArmorProcProtection.Equipped> rules;
    private ModernArmorProtectionLoadout(ModernArmorProtectionCatalog.BoundSource bound,ModernCombatProfile profile,
            Set<Long> permitted,List<ModernArmorProcProtection.Equipped> rules) {
        this.bound=bound;this.profile=profile;this.permitted=Set.copyOf(permitted);this.rules=List.copyOf(rules);
    }
    static ModernArmorProtectionLoadout qualify(ModernArmorProtectionCatalog.BoundSource bound,ModernCombatProfile profile) {
        Objects.requireNonNull(bound);Objects.requireNonNull(profile);var source=bound.physical();
        if(!source.input().equipment().source().equals(profile.source())||source.weaponProcCatalogIds()==null)throw invalid();
        var allowed=new HashSet<Long>();var selected=new ArrayList<ModernArmorProcProtection.Equipped>();
        for(var flag:profile.equipped()) {
            var item=source.input().equipment().items().get(flag.objectId()).source();
            long catalog=item.template().catalogId();
            if(!flag.equipped()&&!item.equipped()||!source.procCatalogIds().contains(catalog))continue;
            if(item.template().kind()!=ModernCharacterData.ItemKind.ARMOR||source.weaponProcCatalogIds().contains(catalog)
                    ||source.armorProcs()==null)throw invalid();
            for(var rule:source.armorProcs().selected(catalog,item.enchantment())) {
                if(!ModernArmorProcProtection.supported(rule))throw invalid();
                if((rule.chance(true)>0||rule.chance(false)>0)&&bound.skill().status()!=ModernArmorProtectionCatalog.Status.QUALIFIED)throw invalid();
                if(flag.equipped())selected.add(new ModernArmorProcProtection.Equipped(item.objectId(),rule));
            }
            allowed.add(item.objectId());
        }
        if(selected.size()>ModernArmorProcs.MAX_ROWS)throw invalid();
        return new ModernArmorProtectionLoadout(bound,profile,allowed,selected);
    }
    void require(ModernPhysicalCombat.Source source,ModernCombatProfile profile) {
        if(bound.physical()!=source||this.profile!=profile)throw invalid();
    }
    boolean permits(long objectId){return permitted.contains(objectId);}
    ModernArmorProtectionLoadout rebindVip(ModernPhysicalCombat.Source source,ModernCombatProfile profile) {
        var prior=bound.physical();
        if(!prior.input().equipment().source().equals(source.input().equipment().source())
                ||!Objects.equals(prior.armorProcs(),source.armorProcs())||!prior.procCatalogIds().equals(source.procCatalogIds())
                ||!Objects.equals(prior.weaponProcCatalogIds(),source.weaponProcCatalogIds()))throw invalid();
        return qualify(new ModernArmorProtectionCatalog.BoundSource(source,bound.skill()),profile);
    }
    /** Requirements for the admitted equipment set, including armor that can
     * be re-equipped after a durable SET. Disabled/exact rows use the same
     * selection as combat. IDs are requirements, never a resource certificate. */
    ModernArmorPresentation.Requirements presentationRequirements() {
        var source=bound.physical();var graphics=new java.util.TreeSet<Integer>();boolean enabled=false;
        for(long objectId:permitted) {
            var item=source.input().equipment().items().get(objectId).source();
            for(var rule:source.armorProcs().selected(item.template().catalogId(),item.enchantment())) {
                if(rule.chance(true)==0)continue;
                enabled=true;
                if(rule.graphics()>0)graphics.add(Math.toIntExact(rule.graphics()));
            }
        }
        if(enabled)graphics.add(ModernArmorProtectionCatalog.GRAPHIC);
        return new ModernArmorPresentation.Requirements(List.copyOf(graphics),enabled?List.of(ModernArmorPresentation.STATUS_ICON):List.of());
    }
    /** Pin ALL observed rules, including unequipped armor and alternate stages.
     * A gear SET must not silently refresh a changed trigger catalog. */
    boolean sameFacts(ModernArmorProtectionLoadout other) {
        if(other==null)return false;
        var a=bound.physical();var b=other.bound.physical();
        return bound.skill().sourceId()==other.bound.skill().sourceId()
                &&bound.skill().database().equals(other.bound.skill().database())
                &&bound.skill().status()==other.bound.skill().status()
                &&Objects.equals(a.armorProcs(),b.armorProcs())
                &&a.procCatalogIds().equals(b.procCatalogIds())
                &&a.weaponProcCatalogIds().equals(b.weaponProcCatalogIds())
                &&permitted.equals(other.permitted)&&rules.equals(other.rules);
    }
    ModernArmorProcProtection.Selection select(ModernArmorProcProtection.Actor actor) {
        if(!actor.owner().equals(new TournamentPresence.CharacterKey(profile.source().sourceId(),profile.source().character().objectId())))throw invalid();
        return new ModernArmorProcProtection.Selection(actor,profile.equipmentRevision(),rules);
    }
    @Override public String toString(){return "ArmorProtectionLoadout[redacted]";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("unsupported armor protection loadout");}
}
