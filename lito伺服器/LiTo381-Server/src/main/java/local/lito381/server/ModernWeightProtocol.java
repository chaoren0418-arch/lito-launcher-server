package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** v9 append-only carrying facts. Capacity is source-qualified binary64, never client input. */
final class ModernWeightProtocol {
    static final int SNAPSHOT_BYTES = 40, UNIT_BYTES = 16, PREFIX_BYTES = 8;
    record State(long entityId, ModernInventoryWeight.Snapshot snapshot) {
        State { if (entityId < 1) throw bad(); Objects.requireNonNull(snapshot); }
    }
    private ModernWeightProtocol() {}

    static void putOptional(ByteBuffer out, ModernInventoryWeight.Snapshot snapshot) {
        out.put((byte) (snapshot == null ? 0 : 1)); zeroes(out, 7);
        if (snapshot != null) put(out, snapshot);
    }
    static ModernInventoryWeight.Snapshot getOptional(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        int present = Byte.toUnsignedInt(in.get()); if (present > 1) fail(); zero(in, 7);
        return present == 0 ? null : get(in);
    }
    static void put(ByteBuffer out, ModernInventoryWeight.Snapshot snapshot) {
        var v = snapshot.view();
        out.putLong(v.sourceGearWeight()).putLong(v.suppliesWeight()).putLong(v.totalWeight())
                .putLong(Double.doubleToRawLongBits(v.maximumWeight())).putShort((short) v.stage())
                .put((byte) v.hitPenalty()).put((byte) 0).putShort((short) snapshot.units().size()).putShort((short) 0);
        for (var unit : snapshot.units()) out.putLong(unit.itemId()).putLong(unit.unitWeight());
    }
    static ModernInventoryWeight.Snapshot get(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        long gear = in.getLong(), supplies = in.getLong(), total = in.getLong();
        double maximum = Double.longBitsToDouble(in.getLong()); int stage = Short.toUnsignedInt(in.getShort());
        int penalty = Byte.toUnsignedInt(in.get()); zero(in, 1); int count = Short.toUnsignedInt(in.getShort()); zero(in, 2);
        if (count > ModernInventoryWeight.MAX_UNITS || in.remaining() < count * UNIT_BYTES) fail();
        var view = new ModernInventoryWeight.View(gear, supplies, total, maximum, stage);
        if (penalty != view.hitPenalty()) fail();
        var units = new ArrayList<ModernInventoryWeight.Unit>(count);
        for (int i = 0; i < count; i++) units.add(new ModernInventoryWeight.Unit(in.getLong(), in.getLong()));
        return new ModernInventoryWeight.Snapshot(view, units);
    }
    static void putStates(ByteBuffer out, List<State> states) {
        out.putShort((short) states.size()); zeroes(out, 6);
        for (var state : states) { out.putLong(state.entityId()); put(out, state.snapshot()); }
    }
    static List<State> getStates(ByteBuffer in, int actorCount) throws ModernApplicationProtocol.ProtocolException {
        int count = Short.toUnsignedInt(in.getShort()); zero(in, 6);
        if (count > 100 || count != 0 && count != actorCount || in.remaining() < count * (8 + SNAPSHOT_BYTES)) fail();
        var states = new ArrayList<State>(count);
        for (int i = 0; i < count; i++) states.add(new State(in.getLong(), get(in)));
        return List.copyOf(states);
    }
    static void validateStates(ModernWorldProtocol.Frame world, List<ModernItemProtocol.State> items, List<State> weights) {
        validateStates(world,items,weights,List.of());
    }
    static void validateStates(ModernWorldProtocol.Frame world,List<ModernItemProtocol.State> items,List<State> weights,List<ModernAmmo.State> ammo) {
        if(!ammo.isEmpty()&&ammo.size()!=world.actors().size())throw bad();
        if (weights.isEmpty()) return;
        if (weights.size() != world.actors().size() || weights.size() != items.size()) throw bad();
        long previous = 0;
        for (int i = 0; i < weights.size(); i++) {
            var state = weights.get(i); var actor = world.actors().get(i);
            if (state.entityId() <= previous || state.entityId() != actor.entityId()
                    || state.entityId() != items.get(i).entityId()
                    || state.snapshot().view().applicable() != actor.member().identity().isHuman()) throw bad();
            previous = state.entityId(); validateStocks(state.snapshot(),items.get(i).stocks(),ammo.isEmpty()?List.of():ammo.get(i).stocks());
        }
    }
    static void validateStocks(ModernInventoryWeight.Snapshot snapshot, List<ModernItemProtocol.Stock> stocks) {
        validateStocks(snapshot,stocks,List.of());
    }
    static void validateStocks(ModernInventoryWeight.Snapshot snapshot,List<ModernItemProtocol.Stock> stocks,List<ModernAmmo.Stock> ammo) {
        if(ammo.isEmpty()){validateItemStocks(snapshot,stocks);return;}
        var quantities=new java.util.TreeMap<Long,Long>();for(var s:stocks)if(quantities.put(s.itemId(),s.remaining())!=null)throw bad();
        for(var s:ammo)if(quantities.put(s.itemId(),s.remaining())!=null)throw bad();
        if(quantities.size()>9||snapshot.units().size()!=quantities.size())throw bad();long raw=0;
        for(var unit:snapshot.units()){var q=quantities.remove(unit.itemId());if(q==null)throw bad();raw=Math.addExact(raw,ModernInventoryWeight.itemWeight(q,unit.unitWeight()));}
        if(!quantities.isEmpty()||raw!=snapshot.view().suppliesWeight())throw bad();
    }
    private static void validateItemStocks(ModernInventoryWeight.Snapshot snapshot,List<ModernItemProtocol.Stock> stocks) {
        if (snapshot.units().size() != stocks.size()) throw bad();
        long raw = 0;
        for (int i = 0; i < stocks.size(); i++) {
            var unit = snapshot.units().get(i); var stock = stocks.get(i);
            if (unit.itemId() != stock.itemId()) throw bad();
            raw = Math.addExact(raw, ModernInventoryWeight.itemWeight(stock.remaining(), unit.unitWeight()));
        }
        if (raw != snapshot.view().suppliesWeight()) throw bad();
    }
    static void validateShop(ModernInventoryWeight.Snapshot snapshot, List<ModernShopAuthority.Owned> stocks) {
        if (snapshot == null) return;
        if (!snapshot.view().applicable() || snapshot.units().size() != stocks.size()) throw bad();
        long raw = 0;
        for (int i = 0; i < stocks.size(); i++) {
            var unit = snapshot.units().get(i); var stock = stocks.get(i);
            if (unit.itemId() != stock.itemId()) throw bad();
            raw = Math.addExact(raw, ModernInventoryWeight.itemWeight(stock.quantity(), unit.unitWeight()));
        }
        if (raw != snapshot.view().suppliesWeight()) throw bad();
    }
    static void validateEquipment(ModernEquipmentAuthority.Snapshot equipment) {
        var snapshot = equipment.weight(); if (snapshot == null) return;
        if (!snapshot.view().applicable() || snapshot.view().suppliesWeight() != 0 || !snapshot.units().isEmpty()
                || equipment.lifetimes().size() != equipment.source().inventory().size()) throw bad();
        long raw = 0;
        for (int i = 0; i < equipment.source().inventory().size(); i++) {
            var item = equipment.source().inventory().get(i); var life = equipment.lifetimes().get(i);
            if (item.objectId() != life.itemObjectId()) throw bad();
            if (item.template().kind() == ModernCharacterData.ItemKind.ETC || life.state() == ModernItemLifetime.State.EXPIRED) continue;
            if (!life.usable()) throw bad();
            raw = Math.addExact(raw, ModernInventoryWeight.itemWeight(item.quantity(), item.template().unitWeight()));
        }
        if (raw != snapshot.view().sourceGearWeight()) throw bad();
    }
    private static void zeroes(ByteBuffer out, int count) { for (int i = 0; i < count; i++) out.put((byte) 0); }
    private static void zero(ByteBuffer in, int count) throws ModernApplicationProtocol.ProtocolException {
        for (int i = 0; i < count; i++) if (in.get() != 0) fail();
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern weight frame"); }
    private static void fail() throws ModernApplicationProtocol.ProtocolException { throw new ModernApplicationProtocol.ProtocolException(); }
}
