package local.lito381.server;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Explicit opt-in to NEW siege placement on unchanged shared Lineage Kent terrain.
 * Not native LiTo Rainbird reconstruction, automatic map fallback, or resource copying. */
final class ModernSharedSiegePreset {
    static final String KEY="world.3.547.sharedPreset", NAME="SHARED_KENT_SIEGE_V1", PREFIX="world.3.547.";
    static final String MANIFEST="1072ad94a0642960a05eddea1ae6bcb8e5f633c05caf8b7934e046375cf84c23";
    static final String ATTACK="33105,32770;33088,32770;33086,32770;33088,32768;33088,32772;33090,32770;33084,32770;33086,32768;33086,32772;33088,32766;33088,32774;33090,32768;33090,32772;33092,32770;33082,32770;33084,32768;33084,32772;33086,32766;33086,32774;33088,32764;33088,32776;33090,32766;33090,32774;33092,32768;33092,32772;33080,32770;33082,32768;33082,32772;33084,32766;33084,32774;33086,32764;33086,32776;33088,32762;33088,32778;33090,32764;33090,32776;33092,32766;33092,32774;33094,32768;33094,32772;33078,32770;33080,32768;33080,32772;33082,32766;33084,32764;33084,32776;33086,32762;33086,32778;33088,32760;33088,32780";
    static final String DEFENSE="33120,32770;33136,32788;33134,32788;33136,32786;33136,32790;33138,32788;33132,32788;33134,32786;33134,32790;33136,32784;33136,32792;33138,32786;33138,32790;33140,32788;33130,32788;33132,32786;33132,32790;33134,32784;33134,32792;33136,32782;33136,32794;33138,32784;33138,32792;33140,32786;33140,32790;33142,32788;33128,32788;33130,32786;33130,32790;33132,32784;33132,32792;33134,32782;33136,32780;33136,32796;33138,32782;33138,32794;33140,32784;33140,32792;33142,32786;33142,32790;33144,32788;33126,32788;33128,32786;33128,32790;33130,32784;33130,32792;33132,32782;33134,32780;33136,32778;33136,32798";
    static final String GATE="33111,32769;33111,32770;33111,32771;33111,32772;33150,32805;33151,32805;33152,32805;33153,32805";
    static final String SECTORS="80047ffe,80047fff,80048000,80057ffe,80057fff,80058000,80067ffe,80067fff,80068000";
    static final String PINS="80047ffe:159053:749834e9af4320172c0fbc3e46191a41f78acbb227216cd111c2ea8caae6e106,80047fff:138689:468bb376c195439c2694b2f997d638e70afca73485c5a643abc0c02b243e1cad,80048000:153627:fbe1211f4008013a90dd1b0aba6639f658b4847bf47729301df8b51458ec8425,80057ffe:152458:b706f2ac1fb1699aa1d65b4773889a1c1b57ddd21cbf27e417081afef31b3858,80057fff:123400:8953d92230bd607525ab6bcb98d9c7fd1a004811594e6424130b0043079f0ad1,80058000:130123:a4a1c82357ba17bf005d87c0ba2fe4e045a981c47d63abafa2a7857dd39afc84,80067ffe:159879:4181909abdee062ce3b85eb8a8181107eed93b985f7a46ea59652fccbc9c0137,80067fff:149584:9bcc7cee2e4428e2ed06c52a9f63bf68240c8cef34c46763bdbee7a997f5e489,80068000:147305:a2273af6ec3d57e618e3507b7b922a80be0ba66750fb354e20f3578e9a6b916a";
    static ModernSiege.Config policy() {
        return new ModernSiege.Config(new ModernWorldBinding.Key(3,547),List.of(
                new ModernSiege.Tower(ModernSiege.Slot.OUTER_LEFT,new ModernWorldMap.Cell(33104,32755),2665,10_000),
                new ModernSiege.Tower(ModernSiege.Slot.OUTER_RIGHT,new ModernWorldMap.Cell(33104,32785),2667,10_000),
                new ModernSiege.Tower(ModernSiege.Slot.INNER,new ModernWorldMap.Cell(33142,32770),2671,10_000)),
                Set.copyOf(cells(GATE)),cells(ATTACK),cells(DEFENSE),new ModernSiege.Portal(new ModernWorldMap.Cell(33107,32770),new ModernWorldMap.Cell(33120,32774)),1_200_000,5_000);
    }
    static Map<String,String> fields() {
        var c=policy();var result=new LinkedHashMap<String,String>();
        result.put("physicalMap","4");result.put("sectors",SECTORS);result.put("manifestSha256",MANIFEST);result.put("sectorPins",PINS);
        result.put("spawnX","33105");result.put("spawnY","32770");result.put("activeTimeoutMillis","1200000");
        result.put("siegeRules",ModernSiegeConfiguration.RULESET);result.put("siegeTowers",c.towers().stream().map(t->t.cell().x()+","+t.cell().y()+","+t.gfx()+","+t.maximumHp()).collect(Collectors.joining(";")));
        result.put("siegeGate",GATE);result.put("siegeAttackCamp",ATTACK);result.put("siegeDefenseCamp",DEFENSE);result.put("siegePortal","33107,32770;33120,32774");
        result.put("siegeRespawnMillis","5000");result.put("siegePolicySha256",ModernSiegeSettlement.configHash(c));return Map.copyOf(result);
    }
    /** No I/O or secret lookup; never changes caller-owned properties. Mixed policy is ambiguous. */
    static Properties expand(Properties input) {
        if(!input.containsKey(KEY))return input;
        if(!NAME.equals(input.getProperty(KEY)))throw new IllegalArgumentException("unknown shared siege preset");
        var fields=fields();if(fields.keySet().stream().anyMatch(k->input.containsKey(PREFIX+k)))throw new IllegalArgumentException("mixed shared siege preset");
        var result=(Properties)input.clone();result.remove(KEY);fields.forEach((k,v)->result.setProperty(PREFIX+k,v));return result;
    }
    private static List<ModernWorldMap.Cell> cells(String value) {return Arrays.stream(value.split(";")).map(row->{var a=row.split(",");return new ModernWorldMap.Cell(Integer.parseInt(a[0]),Integer.parseInt(a[1]));}).toList();}
    private ModernSharedSiegePreset(){}
}
