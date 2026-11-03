package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Joins SELECT-only learned and template facts under one selected-source lease.
 * Neither a raw skill row nor a battle history lane grants this permission. */
final class ModernGreaterHasteSkills {
    static List<ModernSkills.Skill> qualify(ModernSkills.Snapshot skills, ModernGreaterHasteCatalog.Snapshot catalog) throws IOException {
        if(catalog==null || !catalog.key().equals(new TournamentPresence.CharacterKey(skills.sourceId(),skills.character().objectId())))throw unavailable();
        var row=skills.skills().stream().filter(s->s.skillId()==54).findFirst();
        if(row.isEmpty()!=(catalog.status()==ModernGreaterHasteCatalog.Status.UNLEARNED))throw unavailable();
        if(row.isEmpty())return skills.skills();
        var raw=row.orElseThrow();
        boolean qualified=catalog.status()==ModernGreaterHasteCatalog.Status.QUALIFIED;
        if(qualified&&(raw.mpCost()!=60||raw.reuseMillis()!=10))throw unavailable();
        var out=new ArrayList<ModernSkills.Skill>(skills.skills().size());
        for(var skill:skills.skills())out.add(skill.skillId()!=54?skill:new ModernSkills.Skill(54,skill.nameToken(),skill.mpCost(),skill.reuseMillis(),
                qualified?ModernSkills.Support.GREATER_HASTE:ModernSkills.Support.UNSUPPORTED_TEMPLATE));
        return List.copyOf(out);
    }
    private static IOException unavailable(){return new IOException("Greater Haste catalog changed");}
    private ModernGreaterHasteSkills() {}
}
