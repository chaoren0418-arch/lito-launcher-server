package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/** Explicit shared-map policy, not inferred native tower/gate coordinates. */
final class ModernSiegeConfiguration {
    static final String RULESET="NEW_MATCH_SIEGE_V1";
    static final List<String> SUFFIXES=List.of("siegeRules","siegeTowers","siegeGate","siegeAttackCamp","siegeDefenseCamp","siegePortal","siegeRespawnMillis","siegePolicySha256");
    static ModernSiege.Config parse(Properties p,String prefix,ModernWorldBinding.Key key,Integer duration) {
        if(SUFFIXES.stream().noneMatch(s->p.containsKey(prefix+s)))return null;
        if(!RULESET.equals(required(p,prefix+"siegeRules"))||duration==null)throw bad();
        var towers=new ArrayList<ModernSiege.Tower>();var rows=required(p,prefix+"siegeTowers").split(";",-1);if(rows.length!=3)throw bad();
        for(int i=0;i<3;i++){var fields=rows[i].split(",",-1);if(fields.length!=4)throw bad();
            towers.add(new ModernSiege.Tower(ModernSiege.Slot.values()[i],new ModernWorldMap.Cell(integer(fields[0]),integer(fields[1])),integer(fields[2]),Long.parseLong(fields[3])));}
        var gate=cells(required(p,prefix+"siegeGate"),64);var attack=cells(required(p,prefix+"siegeAttackCamp"),50);var defend=cells(required(p,prefix+"siegeDefenseCamp"),50);
        var portal=cells(required(p,prefix+"siegePortal"),2);if(portal.size()!=2||attack.size()<15||defend.size()<15)throw bad();
        var config=new ModernSiege.Config(key,towers,new HashSet<>(gate),attack,defend,new ModernSiege.Portal(portal.get(0),portal.get(1)),duration,Long.parseLong(required(p,prefix+"siegeRespawnMillis")));
        if(!ModernSiegeSettlement.configHash(config).equals(required(p,prefix+"siegePolicySha256")))throw bad();return config;
    }
    private static List<ModernWorldMap.Cell> cells(String value,int max){var rows=value.split(";",-1);if(rows.length<1||rows.length>max)throw bad();
        var result=new ArrayList<ModernWorldMap.Cell>();for(var row:rows){var f=row.split(",",-1);if(f.length!=2)throw bad();result.add(new ModernWorldMap.Cell(integer(f[0]),integer(f[1])));}
        if(new HashSet<>(result).size()!=result.size())throw bad();return List.copyOf(result);}
    private static int integer(String value){if(!value.matches("-?[0-9]{1,10}"))throw bad();return Integer.parseInt(value);}
    private static String required(Properties p,String key){String value=p.getProperty(key);if(value==null||value.isEmpty()||value.length()>8192)throw bad();return value;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid explicit siege policy");}
    private ModernSiegeConfiguration(){}
}
