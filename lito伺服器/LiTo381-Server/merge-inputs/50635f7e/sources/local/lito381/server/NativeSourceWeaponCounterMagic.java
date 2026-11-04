package local.lito381.server;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Source27 Counter Magic at the existing successful controlled-NPC mutation. */
final class NativeSourceWeaponCounterMagic {
    static int stack(BattleCompanion pet, ClientSession caster,
                     List<ClientSession> recipients, long targetId) throws IOException {
        var context = NativeAreaMaterialAction.current.get();
        if (!NativeSourceWeaponTargetLife.marked(context)) return pet.addWeaponBreakStack();
        if (!NativeSourceWeaponTargetLife.current(context)) return -1;
        var proof = NativeSourceTargetNegativeLife.proof(context);
        if (proof == null || proof.npc() == null || proof.npc().actor() != pet
                || proof.npc().id() != targetId) return -1;
        var exceptions = (Set<?>) CombatAccess.field(caster, "sourceCounterMagicBypassSkills");
        if (!exceptions.contains(27)
                && ((BattleTimedStatus) CombatAccess.field(pet, "timedStatus"))
                    .consumeCounterMagic(System.nanoTime())) {
            var ward = BattleSpellCatalog.counterMagicSpell(31);
            if (ward.consumeEffectGfx() > 0) for (var receiver : recipients) {
                NativeWeaponBreakBattleCompanion.send(receiver,
                    Protocol.effectObject(targetId, ward.consumeEffectGfx()),
                    "S_EFFECT Counter Magic Weapon Break target=" + targetId);
            }
            return -1;
        }
        return pet.addWeaponBreakStack();
    }

    private NativeSourceWeaponCounterMagic() {}
}
