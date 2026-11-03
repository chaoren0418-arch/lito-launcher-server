package local.lito381.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Operator-chosen shared geometry/pattern. No native placements or random
 * pattern probabilities are inferred from a logical map number. */
final class ModernTimeAttackConfiguration {
    static final String RULESET="COOPERATIVE_TIME_ATTACK_SHARED_V1";
    static final List<String> SUFFIXES;
    static {
        var keys=new ArrayList<>(List.of("timeAttackRules","timeAttackTower","timeAttackDurationMillis","timeAttackWaves"));
        for(int i=0;i<32;i++)keys.add("timeAttackWave"+i);
        SUFFIXES=List.copyOf(keys);
    }
    record Spec(ModernWorldBinding.Key key,ModernTimeAttack.Tower tower,int durationMillis,
                List<ModernTimeAttack.Kind> kinds,List<List<ModernNpcConfiguration.Spawn>> waves) {
        Spec {kinds=List.copyOf(kinds);waves=waves.stream().map(List::copyOf).toList();}
        ModernTimeAttack.Plan qualify(ModernNpcCatalog.Catalog catalog) {
            var result=new ArrayList<ModernTimeAttack.Wave>();
            for(int i=0;i<waves.size();i++)result.add(new ModernTimeAttack.Wave(kinds.get(i),waves.get(i).stream().map(s->ModernQuestConfiguration.spawn(catalog,s)).toList()));
            return new ModernTimeAttack.Plan(key,tower,result,durationMillis);
        }
        void writeBinding(DataOutputStream out)throws IOException {
            out.writeUTF(RULESET);ModernQuestConfiguration.cell(out,tower.cell());out.writeInt(tower.graphic());out.writeLong(tower.maximumHp());
            out.writeInt(durationMillis);out.writeInt(waves.size());
            for(int i=0;i<waves.size();i++) {out.writeInt(kinds.get(i).ordinal());out.writeInt(waves.get(i).size());
                for(var s:waves.get(i)){out.writeInt(s.id());ModernQuestConfiguration.cell(out,s.cell());out.writeInt(s.heading());}}
        }
        @Override public String toString(){return "ModernTimeAttackSpec[redacted]";}
    }
    static boolean present(Properties p,String prefix){return SUFFIXES.stream().anyMatch(s->p.containsKey(prefix+s));}
    static Spec parse(Properties p,String prefix,ModernWorldBinding.Key key,Set<Integer> ids,int capacity,List<ModernWorldMap.Cell> camps) {
        if(!present(p,prefix))return null;
        if(key.mode()!=4||key.logicalMapId()!=540||camps.size()!=1||!RULESET.equals(required(p,prefix,"timeAttackRules"))
                ||p.containsKey(prefix+"npcSpawns")||p.containsKey(prefix+"npcCamp1")
                ||ModernQuestConfiguration.present(p,prefix))throw bad();
        var t=required(p,prefix,"timeAttackTower").split(",",-1);
        if(t.length!=4||!t[3].matches("[1-9][0-9]{0,18}"))throw bad();
        var tower=new ModernTimeAttack.Tower(cell(t[0],t[1]),ModernNpcConfiguration.number(t[2],1,65535),Long.parseLong(t[3]));
        if(tower.cell().equals(camps.get(0)))throw bad();
        int duration=ModernNpcConfiguration.number(required(p,prefix,"timeAttackDurationMillis"),50,1_800_000);
        var names=required(p,prefix,"timeAttackWaves").split(",",-1);
        if(names.length<2||names.length>32)throw bad();
        var kinds=new ArrayList<ModernTimeAttack.Kind>();var waves=new ArrayList<List<ModernNpcConfiguration.Spawn>>();
        var used=new HashSet<Integer>();int finals=0,dragons=0;
        for(int i=0;i<names.length;i++) {
            var kind=ModernTimeAttack.Kind.valueOf(names[i]);
            if(i==0&&kind!=ModernTimeAttack.Kind.ORDINARY||kind==ModernTimeAttack.Kind.ORDINARY&&(finals!=0||dragons!=0)
                    ||kind==ModernTimeAttack.Kind.FINAL_BOSSES&&(++finals!=1||dragons!=0)
                    ||kind==ModernTimeAttack.Kind.DRAGON&&(finals!=1||++dragons!=1||i!=names.length-1))throw bad();
            var batch=new ArrayList<ModernNpcConfiguration.Spawn>();var cells=new HashSet<ModernWorldMap.Cell>();
            for(String row:required(p,prefix,"timeAttackWave"+i).split(";",-1)) {
                var v=row.split(",",-1);if(v.length!=4||batch.size()==capacity)throw bad();
                int id=ModernNpcConfiguration.number(v[0],1,Integer.MAX_VALUE);var cell=cell(v[1],v[2]);
                if(!ids.contains(id)||!cells.add(cell)||cell.equals(tower.cell())||camps.contains(cell))throw bad();
                used.add(id);batch.add(new ModernNpcConfiguration.Spawn(id,0,kind.ordinal()+1,cell,ModernNpcConfiguration.number(v[3],0,7)));
            }
            kinds.add(kind);waves.add(List.copyOf(batch));
        }
        if(finals!=1||!used.equals(ids))throw bad();
        for(int i=names.length;i<32;i++)if(p.containsKey(prefix+"timeAttackWave"+i))throw bad();
        return new Spec(key,tower,duration,kinds,waves);
    }
    private static ModernWorldMap.Cell cell(String x,String y){return new ModernWorldMap.Cell(ModernNpcConfiguration.signed(x),ModernNpcConfiguration.signed(y));}
    private static String required(Properties p,String prefix,String suffix){var v=p.getProperty(prefix+suffix);if(v==null||v.isEmpty())throw bad();return v;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid explicit Time Attack policy");}
    private ModernTimeAttackConfiguration(){}
}
