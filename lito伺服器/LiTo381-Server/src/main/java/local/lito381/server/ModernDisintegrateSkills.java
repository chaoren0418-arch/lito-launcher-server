package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Joins two SELECT-only facts under the same authenticated selection lease.
 * A disagreement is unavailable, never a guessed learned skill or template.
 * Catalog MP is the qualified base70; the match later supplies effective MP.
 */
final class ModernDisintegrateSkills {
    private ModernDisintegrateSkills() {}
    static List<ModernSkills.Skill> qualify(ModernSkills.Snapshot skills,ModernDisintegrateCatalog.Snapshot catalog)throws IOException {
        if(catalog==null || !catalog.key().equals(new TournamentPresence.CharacterKey(skills.sourceId(),skills.character().objectId())))throw unavailable();
        var row=skills.skills().stream().filter(s->s.skillId()==77).findFirst();
        if(row.isEmpty()!=(catalog.status()==ModernDisintegrateCatalog.Status.UNLEARNED))throw unavailable();
        if(row.isEmpty())return skills.skills();
        var raw=row.orElseThrow();
        boolean qualified=catalog.status()==ModernDisintegrateCatalog.Status.QUALIFIED;
        if(qualified&&(raw.mpCost()!=70||raw.reuseMillis()!=5000))throw unavailable();
        var out=new ArrayList<ModernSkills.Skill>(skills.skills().size());
        for(var skill:skills.skills())out.add(skill.skillId()!=77?skill:new ModernSkills.Skill(77,skill.nameToken(),skill.mpCost(),skill.reuseMillis(),
                qualified?ModernSkills.Support.DISINTEGRATE:ModernSkills.Support.UNSUPPORTED_TEMPLATE));
        return List.copyOf(out);
    }
    private static IOException unavailable(){return new IOException("Disintegrate catalog changed");}
}
