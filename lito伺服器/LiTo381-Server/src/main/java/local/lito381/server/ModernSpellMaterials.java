package local.lito381.server;

import java.util.List;
import java.util.Objects;

/** Source-qualified, match-local spell reagents. No source inventory imports,
 * SQL writes, item-USE events, or authority to cast. Immutable debit candidates
 * must be committed with MP/effects by the owning engine after its final gates. */
final class ModernSpellMaterials {
    static final int MAX_STOCKS = 1;
    record Facts(long itemId, long unitWeight) {
        Facts { if (itemId != ModernSlow.MATERIAL_ID || unitWeight < 0) throw bad(); }
    }
    static Facts qualify(long id, ModernShopCatalog.Template t) {
        if (id != ModernSlow.MATERIAL_ID || t.kind() != ModernCharacterData.ItemKind.ETC
                || t.arrow() != null || !t.nameToken().equals("$508")
                || t.inventoryGraphic() != 235 || t.groundGraphic() != 773) return null;
        var e = t.etc();
        if (!"0".equals(e.executor()) || !e.itemType().equals("material")
                || !e.useType().equals("none") || !e.material().equals("gemstone")
                || e.stackable() != 1 || e.maxChargeCount() != 0 || e.minLevel() != 0 || e.maxLevel() != 0
                || e.blessing() != 0 || e.trade() != 0 || e.cantDelete() != 0 || e.delayGroup() != 0
                || e.delayMillis() != 0 || e.delayEffect() != 0 || e.foodVolume() != 0
                || e.saveAtOnce() != 0 || e.limitClass() != 127) return null;
        return new Facts(id, t.weight());
    }
    record Stock(Facts facts, long initial, long remaining) {
        Stock {
            Objects.requireNonNull(facts);
            if (initial < 1 || initial > ModernShopAuthority.MAX_QUANTITY || remaining < 0 || remaining > initial) throw bad();
        }
        long itemId() { return facts.itemId(); }
        Stock debit() {
            if (remaining < ModernSlow.MATERIAL_COUNT) throw bad();
            return new Stock(facts, initial, remaining - ModernSlow.MATERIAL_COUNT);
        }
    }
    record State(ModernTargetHistory.World world, ModernRooms.Scope scope,
            ModernTargetHistory.Participant caster, long revision, long uses, long lastSequence,
            long lastLife, long lastEquipmentRevision, List<Stock> stocks) {
        State {
            Objects.requireNonNull(world); Objects.requireNonNull(scope); Objects.requireNonNull(caster);
            stocks = List.copyOf(stocks);
            if (!caster.identity().isHuman() || caster.identity().sourceId() != scope.sourceId()
                    || caster.identity().id() != scope.objectId() || stocks.size() > MAX_STOCKS
                    || revision < 0 || uses < 0 || lastSequence < uses || lastLife < 0 || lastEquipmentRevision < 0) throw bad();
            long consumed = 0;
            for (var stock : stocks) consumed = Math.addExact(consumed, stock.initial() - stock.remaining());
            if (consumed != Math.multiplyExact(uses, ModernSlow.MATERIAL_COUNT)) throw bad();
            if (uses == 0) {
                if (revision != 0 || lastSequence != 0 || lastLife != 0 || lastEquipmentRevision != 0) throw bad();
            } else if (revision < uses || lastLife < 1 || stocks.isEmpty()) throw bad();
        }
        static State initial(ModernTargetHistory.World world, ModernRooms.Scope scope,
                ModernTargetHistory.Participant caster, List<ModernShopAuthority.Stack> captured) {
            Objects.requireNonNull(captured);
            // Full capture must be sorted/unique, even the stocks owned by other lanes.
            long previous = 0;
            for (var stack : captured) {
                if (stack.offer().itemId() <= previous) throw bad();
                previous = stack.offer().itemId();
            }
            return new State(world, scope, caster, 0, 0, 0, 0, 0,
                    captured.stream().filter(s -> s.offer().material() != null)
                    .map(s -> new Stock(s.offer().material(), s.quantity(), s.quantity())).toList());
        }
        boolean hasSlowMaterial() { return !stocks.isEmpty() && stocks.get(0).remaining() >= ModernSlow.MATERIAL_COUNT; }
        /** Called for an admitted spell attempt, whether its probability hits
         * or misses. Rejected/stale/cancelled attempts must not call/commit it. */
        State slow(long nextRevision, long sequence, long life, ModernSlow.Profile profile,
                ModernSlowCatalog.Snapshot learned) {
            Objects.requireNonNull(profile); Objects.requireNonNull(learned);
            if (nextRevision <= revision || sequence <= lastSequence || life < 1 || life < lastLife
                    || profile.equipmentRevision() < lastEquipmentRevision
                    || profile.key().sourceId() != scope.sourceId() || profile.key().objectId() != scope.objectId()
                    || !learned.accepts(profile) || !hasSlowMaterial()) throw bad();
            return new State(world, scope, caster, nextRevision, Math.incrementExact(uses), sequence,
                    life, profile.equipmentRevision(), List.of(stocks.get(0).debit()));
        }
        ModernInventoryWeight.Snapshot reweight(ModernInventoryWeight.Basis basis,
                List<ModernItemProtocol.Stock> items, List<ModernAmmo.Stock> ammo,
                List<ModernInventoryWeight.Unit> units) {
            if (basis.source().sourceId() != scope.sourceId() || basis.source().character().objectId() != scope.objectId()
                    || basis.equipmentRevision() < lastEquipmentRevision) throw bad();
            return ModernInventoryWeight.world(basis, items, ammo, stocks, units);
        }
        @Override public String toString() { return "ModernSpellMaterials[redacted]"; }
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid match spell material"); }
    private ModernSpellMaterials() {}
}
