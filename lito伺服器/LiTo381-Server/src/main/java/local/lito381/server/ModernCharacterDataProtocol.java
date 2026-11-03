package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/** Typed character-data payload. Only kind 15 gets the larger inventory bound. */
final class ModernCharacterDataProtocol {
    static final int MAX_FRAME_BYTES = 256 * 1024;
    private ModernCharacterDataProtocol() {}
    static void put(ByteBuffer out, ModernCharacterData.Snapshot snapshot)
            throws ModernApplicationProtocol.ProtocolException {
        ModernApplicationProtocol.putCharacter(out, snapshot.character());
        var values = snapshot.persisted();
        for (long value : new long[] {values.maxHp(), values.maxMp(), values.savedHp(), values.savedMp(),
                values.armorClass(), values.strength(), values.dexterity(), values.constitution(),
                values.intelligence(), values.wisdom(), values.charisma(), values.experience(), values.lawful(),
                values.originalStrength(), values.originalDexterity(), values.originalConstitution(),
                values.originalIntelligence(), values.originalWisdom(), values.food225()}) out.putLong(value);
        out.putShort((short) snapshot.inventory().size()).putShort((short) 0);
        for (var item : snapshot.inventory()) {
            var template = item.template();
            out.putLong(item.objectId()).putLong(template.catalogId()).putLong(item.quantity())
                    .putLong(item.enchantment()).putLong(item.durability()).putLong(item.blessing()).putLong(item.chargeCount())
                    .putLong(template.inventoryGraphic()).putLong(template.groundGraphic()).putLong(template.unitWeight());
            out.put((byte) template.kind().ordinal())
                    .put((byte) ((item.equipped() ? 1 : 0) | (item.identified() ? 2 : 0) | (item.sealed() ? 4 : 0)))
                    .putShort((short) 0);
            ModernApplicationProtocol.putText(out, template.nameToken(), ModernCharacterData.MAX_TEXT_BYTES, false);
        }
    }
    static ModernCharacterData.Snapshot get(ByteBuffer in, int sourceId)
            throws ModernApplicationProtocol.ProtocolException {
        var character = ModernApplicationProtocol.character(in);
        var values = new ModernCharacterData.PersistedValues(in.getLong(),in.getLong(),in.getLong(),in.getLong(),
                in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),
                in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong(),in.getLong());
        int count = Short.toUnsignedInt(in.getShort());
        if (count > ModernCharacterData.MAX_ITEMS || in.getShort() != 0) throw new ModernApplicationProtocol.ProtocolException();
        var items = new ArrayList<ModernCharacterData.InventoryItem>(count);
        for (int index = 0; index < count; index++) {
            long objectId=in.getLong(),catalogId=in.getLong(),quantity=in.getLong(),enchantment=in.getLong(),
                    durability=in.getLong(),blessing=in.getLong(),charges=in.getLong(),
                    inventoryGraphic=in.getLong(),groundGraphic=in.getLong(),weight=in.getLong();
            int kind=Byte.toUnsignedInt(in.get()),flags=Byte.toUnsignedInt(in.get());
            if (kind > 2 || (flags & ~7) != 0 || in.getShort() != 0) throw new ModernApplicationProtocol.ProtocolException();
            var template = new ModernCharacterData.ItemTemplate(catalogId, ModernCharacterData.ItemKind.values()[kind],
                    ModernApplicationProtocol.text(in, ModernCharacterData.MAX_TEXT_BYTES, false), inventoryGraphic,groundGraphic,weight);
            items.add(new ModernCharacterData.InventoryItem(objectId,template,quantity,(flags&1)!=0,
                    enchantment,durability,(flags&2)!=0,blessing,(flags&4)!=0,charges));
        }
        return new ModernCharacterData.Snapshot(sourceId,character,values,items);
    }
}
