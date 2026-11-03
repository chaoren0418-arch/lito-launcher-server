package local.lito381.server;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Source-owned learned skills, not a promise that every imported skill is executable. */
final class ModernSkills {
    static final int MAX_SKILLS = 220;
    // Existing wire ordinals remain stable; new peers explicitly negotiate the
    // Light Arrow ruleset before treating the appended support value as usable.
    enum Support { TELEPORT, UNIMPLEMENTED, UNSUPPORTED_TEMPLATE, LIGHT_ARROW, BASIC_HEAL, FIRE_STORM, DISINTEGRATE, GREATER_HASTE, HASTE, SLOW }
    private ModernSkills() {}

    record Skill(int skillId, String nameToken, long mpCost, long reuseMillis, Support support) {
        Skill {
            if (skillId < 1 || skillId > MAX_SKILLS || !ModernCharacterData.validText(nameToken)
                    || mpCost < 0 || reuseMillis < 0 || support == null
                    || support == Support.TELEPORT && (skillId != 5 || mpCost != 10 || reuseMillis != 10)
                    || support == Support.LIGHT_ARROW && (skillId != 4 || mpCost != 3 || reuseMillis != 10)
                    || support == Support.BASIC_HEAL && (skillId != 1 || mpCost != 4 || reuseMillis != 10)
                    || support == Support.FIRE_STORM && (skillId != 70 || mpCost != 48 || reuseMillis != 2000)
                    || support == Support.DISINTEGRATE && (skillId != 77 || mpCost != 70 || reuseMillis != 5000)
                    || support == Support.GREATER_HASTE && (skillId != 54 || mpCost != 60 || reuseMillis != 10)
                    || support == Support.HASTE && (skillId != 43 || mpCost != 40 || reuseMillis != 10)
                    || support == Support.SLOW && (skillId != 29 || mpCost != 20 || reuseMillis != 500)
                    || support == Support.UNSUPPORTED_TEMPLATE && skillId != 5 && skillId != 4 && skillId != 1 && skillId != 70 && skillId != 77 && skillId != 54 && skillId != 43 && skillId != 29
                    || skillId == 5 && support == Support.UNIMPLEMENTED) throw invalid();
        }
        @Override public String toString() { return "ModernSkill[redacted]"; }
    }

    record Snapshot(int sourceId, ModernApplicationProtocol.CharacterRow character, List<Skill> skills) {
        Snapshot {
            Objects.requireNonNull(character);
            if (sourceId <= 0 || character.objectId() <= 0 || !ModernCharacterData.validText(character.name())
                    || character.characterClass() < 0 || character.characterClass() > 65535
                    || character.sex() < 0 || character.sex() > 1 || character.level() < 1
                    || skills == null || skills.size() > MAX_SKILLS) throw invalid();
            skills = List.copyOf(skills);
            int previous = 0;
            for (var skill : skills) {
                if (skill.skillId() <= previous) throw invalid();
                previous = skill.skillId();
            }
        }
        Optional<Skill> teleport() {
            return skills.stream().filter(skill -> skill.skillId() == 5 && skill.support() == Support.TELEPORT).findFirst();
        }
        Optional<Skill> lightArrow() {
            return skills.stream().filter(skill -> skill.skillId() == 4 && skill.support() == Support.LIGHT_ARROW).findFirst();
        }
        @Override public String toString() { return "ModernSkillsSnapshot[redacted]"; }
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern skills"); }
}
