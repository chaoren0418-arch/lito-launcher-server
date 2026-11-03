package local.lito381.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Format-30 reversible sidecar codec for non-catalog native item templates. */
final class DurableItemDefinitionCodec {
    private static final int VERSION = 2;
    private static final int MAX_RAW_BYTES = 128 * 1024;
    private static final int MAX_TEXT_BYTES = 32 * 1024;

    private DurableItemDefinitionCodec() {}

    static String encode(InventoryCatalog.Definition definition) {
        if (definition == null) return "";
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                out.writeByte(VERSION);
                out.writeLong(definition.catalogId());
                out.writeInt(definition.useType());
                out.writeInt(definition.icon());
                out.writeInt(definition.groundGfx());
                out.writeInt(definition.blessing());
                out.writeBoolean(definition.identified());
                writeText(out, definition.name());
                writeText(out, definition.detail());
                out.writeLong(definition.buyPrice());
                out.writeLong(definition.sellPrice());
                out.writeBoolean(definition.stackable());
                out.writeBoolean(definition.tradable());
                out.writeInt(definition.weaponWalkAction());
                out.writeInt(definition.lightRange());
                writeText(out, definition.effect().name());
                out.writeInt(definition.itemDescriptionId());
                out.writeInt(definition.unitWeight());
                out.writeInt(definition.fireResistance());
                writeWeapon(out, definition.sourceWeaponStatus());
                writeArmor(out, definition.sourceArmorStatus());
                out.writeBoolean(definition.cantDelete());
            }
            byte[] raw = buffer.toByteArray();
            if (raw.length > MAX_RAW_BYTES) {
                throw new IllegalArgumentException("durable item definition is too large");
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (IOException impossible) {
            throw new IllegalStateException("in-memory item definition encoding failed", impossible);
        }
    }

    static InventoryCatalog.Definition decode(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            byte[] raw = Base64.getUrlDecoder().decode(token);
            if (raw.length == 0 || raw.length > MAX_RAW_BYTES) {
                throw new IllegalArgumentException("invalid durable item definition size");
            }
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
                int version = in.readUnsignedByte();
                if (version != 1 && version != VERSION) {
                    throw new IllegalArgumentException("unsupported durable item definition version");
                }
                long catalogId = in.readLong();
                int useType = in.readInt();
                int icon = in.readInt();
                int groundGfx = in.readInt();
                int blessing = in.readInt();
                boolean identified = in.readBoolean();
                String name = readText(in);
                String detail = readText(in);
                long buyPrice = in.readLong();
                long sellPrice = in.readLong();
                boolean stackable = in.readBoolean();
                boolean tradable = in.readBoolean();
                int weaponWalkAction = in.readInt();
                int lightRange = in.readInt();
                InventoryCatalog.Effect effect = InventoryCatalog.Effect.valueOf(readText(in));
                int itemDescriptionId = in.readInt();
                int unitWeight = in.readInt();
                int fireResistance = in.readInt();
                InventoryCatalog.NativeWeaponStatus weapon = readWeapon(in);
                InventoryCatalog.NativeArmorStatus armor = readArmor(in);
                boolean cantDelete = version >= 2 && in.readBoolean();
                if (in.available() != 0) {
                    throw new IllegalArgumentException(
                            "durable item definition contains trailing bytes");
                }
                return new InventoryCatalog.Definition(
                        catalogId, useType, icon, groundGfx, blessing, identified,
                        name, detail, buyPrice, sellPrice, stackable, tradable,
                        weaponWalkAction, lightRange, effect, itemDescriptionId,
                        unitWeight, fireResistance, weapon, armor, cantDelete);
            }
        } catch (EOFException damaged) {
            throw new IllegalArgumentException("truncated durable item definition", damaged);
        } catch (IOException | RuntimeException damaged) {
            if (damaged instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalArgumentException("invalid durable item definition", damaged);
        }
    }

    private static void writeWeapon(DataOutputStream out,
                                    InventoryCatalog.NativeWeaponStatus status)
            throws IOException {
        out.writeBoolean(status != null);
        if (status == null) return;
        out.writeInt(status.damageSmall());
        out.writeInt(status.damageLarge());
        out.writeInt(status.material());
        out.writeInt(status.unitWeight());
        out.writeInt(status.classMask());
        out.writeInt(status.safeEnchant());
    }

    private static InventoryCatalog.NativeWeaponStatus readWeapon(DataInputStream in)
            throws IOException {
        if (!in.readBoolean()) return null;
        return new InventoryCatalog.NativeWeaponStatus(
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }

    private static void writeArmor(DataOutputStream out,
                                   InventoryCatalog.NativeArmorStatus status)
            throws IOException {
        out.writeBoolean(status != null);
        if (status == null) return;
        out.writeInt(status.displayArmorClass());
        out.writeInt(status.material());
        out.writeInt(status.unitWeight());
        out.writeInt(status.classMask());
        out.writeInt(status.safeEnchant());
        out.writeInt(status.strength());
        out.writeInt(status.dexterity());
        out.writeInt(status.constitution());
        out.writeInt(status.wisdom());
        out.writeInt(status.intelligence());
        out.writeInt(status.charisma());
        out.writeInt(status.hitPoints());
        out.writeInt(status.manaPoints());
        out.writeInt(status.magicResistance());
        out.writeInt(status.spellPower());
        out.writeBoolean(status.haste());
        out.writeInt(status.fireResistance());
        out.writeInt(status.waterResistance());
        out.writeInt(status.windResistance());
        out.writeInt(status.earthResistance());
        out.writeInt(status.blindResistance());
        out.writeInt(status.sleepResistance());
        out.writeInt(status.freezeResistance());
        out.writeInt(status.stoneResistance());
        out.writeInt(status.sustainResistance());
        out.writeInt(status.stunResistance());
        out.writeInt(status.armorType());
    }

    private static InventoryCatalog.NativeArmorStatus readArmor(DataInputStream in)
            throws IOException {
        if (!in.readBoolean()) return null;
        return new InventoryCatalog.NativeArmorStatus(
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readBoolean(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt());
    }

    private static void writeText(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("durable item definition text is too large");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readText(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("invalid durable item definition text size");
        }
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException("truncated durable item text");
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
