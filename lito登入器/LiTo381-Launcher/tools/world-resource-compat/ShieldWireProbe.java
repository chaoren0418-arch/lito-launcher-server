import java.lang.reflect.Method;
import java.util.HexFormat;

/** Inspect the existing JAR's real catalog and packet writer; no server/SQL. */
public final class ShieldWireProbe {
    public static void main(String[] args) throws Exception {
        Class<?> catalog = Class.forName("local.lito381.server.BattleSpellCatalog");
        Method spell = catalog.getDeclaredMethod("shieldSpell", int.class);
        spell.setAccessible(true);
        Class<?> protocol = Class.forName("local.lito381.server.Protocol");
        Method writer = protocol.getDeclaredMethod("mageShield", int.class, int.class);
        writer.setAccessible(true);
        for (int skill : new int[]{3, 99, 151}) {
            Object value = spell.invoke(null, skill);
            Method selector = value.getClass().getDeclaredMethod("iconType");
            selector.setAccessible(true);
            int type = (Integer) selector.invoke(value);
            byte[] packet = (byte[]) writer.invoke(null, 600, type);
            System.out.println(skill + "," + type + "," + HexFormat.of().formatHex(packet));
        }
    }
}
