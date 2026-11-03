import java.lang.reflect.Method;
import java.util.HexFormat;

/** Pure writers for existing non-icon status families, no server construction. */
public final class StatusFamilyProbe {
    private static final Class<?> P;
    static {
        try { P=Class.forName("local.lito381.server.Protocol"); }
        catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private static void emit(String label,String name,Class<?>[] types,Object... args)throws Exception{
        Method m=P.getDeclaredMethod(name,types);m.setAccessible(true);
        System.out.println(label+"="+HexFormat.of().formatHex((byte[])m.invoke(null,args)));
    }
    public static void main(String[] args)throws Exception{
        for(int value=0;value<3;++value){
            emit("poison/"+value,"poison",new Class<?>[]{long.class,int.class},0x12345678L,value);
            emit("blind/"+value,"blind",new Class<?>[]{int.class},value);
        }
        for(int value=0;value<2;++value)
            emit("invisible/"+value,"invisible",new Class<?>[]{long.class,int.class},0x12345678L,value);
        Class<?> mode=Class.forName("local.lito381.server.Protocol$ParalysisMode");
        for(Object value:mode.getEnumConstants())
            emit("paralysis/"+value,"paralysis",new Class<?>[]{mode},value);
    }
}
