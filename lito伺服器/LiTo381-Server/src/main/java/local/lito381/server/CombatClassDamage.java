package local.lito381.server;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Source class identity order is crown, knight, elf, wizard, darkelf, DK, illusionist. */
final class CombatClassDamage {
    private static final List<String> CLASSES=List.of("Crown","Knight","Elf","Wizard","Darkelf","DragonKnight","Illusionist");
    private static final List<String> PC_KEYS=CLASSES.stream().map(type->"Other_To_is"+type+"_pc").toList();
    private static final List<String> NPC_KEYS=CLASSES.stream().map(type->"Other_To_is"+type+"_npc").toList();
    private static final Set<String> KEYS;
    static {var keys=new HashSet<String>(PC_KEYS);keys.addAll(NPC_KEYS);KEYS=Set.copyOf(keys);}
    static Set<String> keys(){return KEYS;}
    static String key(int type,boolean player){return type<0||type>=CLASSES.size()?null:(player?PC_KEYS:NPC_KEYS).get(type);}
    static void parse(Map<String,Double> out,Properties source,String key,Path file,Set<String> defaults)throws IOException {
        parse(out,source,key,file,defaults,1.0);
    }
    static void parse(Map<String,Double> out,Properties source,String key,Path file,Set<String> defaults,double fallback)throws IOException {
        String text=source.getProperty(key);double value=fallback;boolean defaulted=text==null||text.isEmpty();
        if(!defaulted)try{value=Double.parseDouble(text.trim());}catch(NumberFormatException invalid){defaulted=true;}
        if(defaulted){defaults.add(key);value=fallback;}
        if(!Double.isFinite(value))throw new IOException(file+": non-finite class damage multiplier "+key);
        out.put(key,value);
    }
    static int scale(int damage,int type,boolean player) {
        if(damage<=0)return 0;String key=key(type,player);double multiplier=key==null?1.0:CombatRules.number(key,1.0);
        return Math.max(0,(int)(damage*multiplier));
    }
    static int physical(int damage,ClientSession caster,ClientSession victim){return scale(damage,caster.battleCharacterType(),victim!=null);}
    static boolean hit(boolean previous,int damage){return CombatRules.loaded()?previous&&damage>0:previous;}
}
