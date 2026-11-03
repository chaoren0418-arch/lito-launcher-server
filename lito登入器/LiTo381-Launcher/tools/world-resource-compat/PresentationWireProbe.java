import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.HexFormat;

/** Invoke only pure packet writers in an isolated JVM. Never construct a server. */
public final class PresentationWireProbe {
    private static final Class<?> P;
    static {
        try { P = Class.forName("local.lito381.server.Protocol"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static void emit(String label, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method method = P.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            byte[] bytes = (byte[]) method.invoke(null, args);
            System.out.println(label + "=" + HexFormat.of().formatHex(bytes));
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) throw e;
            System.out.println(label + "=REJECT:" + e.getCause().getMessage());
        }
    }
    public static void main(String[] args) throws Exception {
        Class<?> I = int.class, J = long.class;
        for (int seconds : new int[]{0, 1, 600, 32767}) {
            for (int type : new int[]{3, 5, 6, 181, 201, 204, 206, 209, 211, 214, 216, 217, 219}) {
                emit("shield/" + type + "/" + seconds, "mageShield",
                     new Class<?>[]{I,I}, seconds, type);
            }
            emit("strength/" + seconds, "mageStrength", new Class<?>[]{I,I,I,I},
                 seconds, 35, 100, 1);
            emit("dexterity/" + seconds, "mageDexterity", new Class<?>[]{I,I,I},
                 seconds, 35, 1);
            emit("breath/" + seconds, "underwaterBreath", new Class<?>[]{J,I},
                 0x12345678L, seconds);
            emit("doll/" + seconds, "magicDollIcon", new Class<?>[]{I}, seconds);
            for (int graphic : new int[]{512, 513, 514, 515, 516, 5935, 10000, 65535}) {
                emit("dollCustom/" + graphic + "/" + seconds, "magicDollCustomIcon",
                     new Class<?>[]{I,I,boolean.class}, graphic, seconds, seconds == 0);
            }
        }
        emit("breath/permanent", "underwaterBreath", new Class<?>[]{J,I}, 0x12345678L,-1);
        for (int graphic : new int[]{0, 66, 744, 829, 830, 831, 11722, 11723, 11724, 11725, 65535}) {
            emit("effect/" + graphic, "effectObject", new Class<?>[]{J,I}, 0x12345678L,graphic);
            emit("location/" + graphic, "effectLocation", new Class<?>[]{I,I,I}, 32768,32769,graphic);
        }
        for (String enumName : new String[]{"SpeedMode","BraveMode"}) {
            Class<?> type = Class.forName("local.lito381.server.Protocol$" + enumName);
            String method = enumName.equals("SpeedMode") ? "speed" : "braveSpeed";
            for (Object value : type.getEnumConstants()) {
                for (int seconds : new int[]{0,600,32767}) {
                    emit(method + "/" + value + "/" + seconds, method,
                         new Class<?>[]{J,type,I}, 0x12345678L,value,seconds);
                }
            }
        }
    }
}
