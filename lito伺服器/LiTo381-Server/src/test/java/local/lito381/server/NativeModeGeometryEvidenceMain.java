package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export existing Native course facts; never registers a Modern physical alias. */
public final class NativeModeGeometryEvidenceMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Owned evidence file required");
        var result=new LinkedHashMap<String,Object>();
        result.put("scope","Compiled Native runtime metadata only; local source declarations, not re-verified external historical evidence or Modern map qualification");
        result.put("native_pet_race_courses",List.of(565,566,569,570).stream().map(BattlePetRaceCourseCatalog::forMap).toList());
        result.put("native_pet_race_laps",BattlePetRaceCourseCatalog.LAPS);
        result.put("native_pet_race_capacity",BattlePetRaceCourseCatalog.MAXIMUM_PLAYERS);
        result.put("native_pet_race_geometry_scope","Native LTmap Layer8 direction markers and collision-derived corridor gates; sequential server-owned course progression");
        result.put("quest_source_declarations",strings(BattleQuestEncounter.class));
        result.put("quest_source_limitations",List.of("Selected species and same-family actors; original event-server coordinates/counts/tuned stats are not recovered",
                "Photo-derived counts are visible lower bounds; species distribution and connected-floor deployment are provisional",
                "Generated lito_battle_* tables are this project's LiTo extensions, not recovered original Tournament server SQL"));
        result.put("quarantined_boundary_source_declarations",strings(BattleQuestBoundary.class));
        result.put("quarantined_boundary_scope","573/574 are proved Native Siege resources. Their resource-derived min-cut diagnostic must never be used as Quest layout qualification.");
        result.put("siege_local_policy_declarations",strings(NativeLitoSiegePolicy.class));
        result.put("modern_new_qualified_bindings",0);
        Files.writeString(Path.of(args[0]),json(result),StandardCharsets.UTF_8);
        System.out.println("[native-mode-evidence] four courses exported; provisional/experimental source bounds retained");
    }
    private static Map<String,String> strings(Class<?> type)throws Exception {
        var result=new LinkedHashMap<String,String>();
        for(var field:type.getDeclaredFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getType()==String.class) {
            field.setAccessible(true);result.put(field.getName(),(String)field.get(null));
        }
        return result;
    }
    private static String json(Object value)throws Exception {
        if(value==null)return "null";
        if(value instanceof String text)return "\""+text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";
        if(value instanceof Number || value instanceof Boolean)return value.toString();
        if(value instanceof Enum<?> e)return json(e.name());
        if(value instanceof Iterable<?> values){var rows=new java.util.ArrayList<String>();for(var item:values)rows.add(json(item));return "["+String.join(",",rows)+"]";}
        if(value instanceof Map<?,?> values){var rows=new java.util.ArrayList<String>();for(var item:values.entrySet())rows.add(json(item.getKey().toString())+":"+json(item.getValue()));return "{"+String.join(",",rows)+"}";}
        if(value.getClass().isRecord()){var fields=new LinkedHashMap<String,Object>();for(var component:value.getClass().getRecordComponents()){var accessor=component.getAccessor();accessor.setAccessible(true);fields.put(component.getName(),accessor.invoke(value));}return json(fields);}
        throw new IllegalArgumentException("Unsupported evidence type");
    }
}
