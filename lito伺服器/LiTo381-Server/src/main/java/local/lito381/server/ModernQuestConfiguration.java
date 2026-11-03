package local.lito381.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Explicit, bounded startup policy for the implemented single-geometry Quest
 * flow. No native coordinates, NPC defaults, bots, boss skills or teleport
 * policy are inferred. Catalog and geometry qualification still happen before
 * listening. Ordinary npcSpawns retains its original meaning. */
final class ModernQuestConfiguration {
    static final String RULESET="QUEST_FOUR_WAVES_SHARED_GEOMETRY_V1";
    static final String LIVES="CONFIRM_DEATH_AT_ENTRY_SPAWN_V1";
    static final List<String> SUFFIXES;
    static {
        var fields=new ArrayList<>(List.of("questRules","questLives","questObjectives"));
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++)fields.add(waveKey(lane,stage));
        SUFFIXES=List.copyOf(fields);
    }
    record Spec(ModernWorldBinding.Key key,List<List<ModernNpcConfiguration.Spawn>> waves,
                ModernQuestObjectives.Config objectives) {
        Spec { waves=waves.stream().map(List::copyOf).toList(); }
        ModernQuestProgress.Plan qualify(ModernNpcCatalog.Catalog catalog) {
            var result=new ArrayList<ModernQuestProgress.Wave>();
            for(int i=0;i<8;i++)result.add(new ModernQuestProgress.Wave(i/4,i%4+1,
                    waves.get(i).stream().map(s->spawn(catalog,s)).toList()));
            return new ModernQuestProgress.Plan(key,result,objectives,true);
        }
        List<ModernNpcConfiguration.Spawn> firstWaves() {
            var result=new ArrayList<>(waves.get(0));result.addAll(waves.get(4));return List.copyOf(result);
        }
        void writeBinding(DataOutputStream out)throws IOException {
            out.writeUTF(RULESET);out.writeUTF(LIVES);
            for(var wave:waves) {
                out.writeInt(wave.size());
                for(var s:wave){out.writeInt(s.id());out.writeInt(s.lane());out.writeInt(s.stage());cell(out,s.cell());out.writeInt(s.heading());}
            }
            out.writeInt(objectives.targets().size());
            for(var t:objectives.targets()){out.writeInt(t.lane());out.writeInt(t.stage());cell(out,t.cell());out.writeInt(t.graphic());out.writeLong(t.maximumHp());}
        }
        @Override public String toString(){return "ModernQuestSpec[redacted]";}
    }
    static boolean present(Properties p,String prefix){return SUFFIXES.stream().anyMatch(s->p.containsKey(prefix+s));}
    static Spec parse(Properties p,String prefix,ModernWorldBinding.Key key,Set<Integer> ids,
            int capacity,List<ModernWorldMap.Cell> camps) {
        if(!present(p,prefix))return null;
        if(key.mode()!=2||!BattleRules.Mode.QUEST_BATTLE.maps().contains(key.logicalMapId())
                ||!RULESET.equals(required(p,prefix,"questRules"))||!LIVES.equals(required(p,prefix,"questLives"))
                ||p.containsKey(prefix+"npcSpawns"))throw bad();
        var used=new HashSet<Integer>();var waves=new ArrayList<List<ModernNpcConfiguration.Spawn>>();
        int resident=0;
        for(int lane=0;lane<2;lane++) {
            int largest=0;
            for(int stage=1;stage<=4;stage++) {
                var spawns=new ArrayList<ModernNpcConfiguration.Spawn>();var cells=new HashSet<ModernWorldMap.Cell>();
                for(String row:required(p,prefix,waveKey(lane,stage)).split(";",-1)) {
                    var v=row.split(",",-1);if(v.length!=4||spawns.size()==128)throw bad();
                    int id=ModernNpcConfiguration.number(v[0],1,Integer.MAX_VALUE);
                    var cell=new ModernWorldMap.Cell(ModernNpcConfiguration.signed(v[1]),ModernNpcConfiguration.signed(v[2]));
                    if(!ids.contains(id)||!cells.add(cell)||camps.get(lane).equals(cell))throw bad();
                    used.add(id);spawns.add(new ModernNpcConfiguration.Spawn(id,lane,stage,cell,ModernNpcConfiguration.number(v[3],0,7)));
                }
                largest=Math.max(largest,spawns.size());waves.add(List.copyOf(spawns));
            }
            resident+=largest;
        }
        if(resident>capacity||!used.equals(ids))throw bad();
        boolean symbols=key.logicalMapId()==513||key.logicalMapId()==527;
        var targets=new ArrayList<ModernQuestObjectives.Target>();
        for(String row:required(p,prefix,"questObjectives").split(";",-1)) {
            var v=row.split(",",-1);if(v.length!=6||targets.size()==8)throw bad();
            int lane=ModernNpcConfiguration.number(v[0],0,1),stage=ModernNpcConfiguration.number(v[1],1,4);
            var cell=new ModernWorldMap.Cell(ModernNpcConfiguration.signed(v[2]),ModernNpcConfiguration.signed(v[3]));
            if(camps.get(lane).equals(cell)||!v[5].matches("[1-9][0-9]{0,18}"))throw bad();
            var target=new ModernQuestObjectives.Target(lane,stage,cell,ModernNpcConfiguration.number(v[4],1,65535),Long.parseLong(v[5]));
            if(target.slot()!=(symbols?targets.size()+1:(targets.size()+1)*4))throw bad();targets.add(target);
        }
        if(targets.size()!=(symbols?8:2))throw bad();
        return new Spec(key,waves,new ModernQuestObjectives.Config(targets));
    }
    static String waveKey(int lane,int stage){return "questWave"+lane+"_"+stage;}
    static ModernNpcRoster.Spawn spawn(ModernNpcCatalog.Catalog catalog,ModernNpcConfiguration.Spawn s) {
        return new ModernNpcRoster.Spawn(catalog.require(new ModernNpcCatalog.Key(catalog.sourceId(),catalog.database(),s.id())).rosterTemplate(),s.lane(),s.stage(),s.cell(),s.heading());
    }
    static void cell(DataOutputStream out,ModernWorldMap.Cell cell)throws IOException{out.writeInt(cell.x());out.writeInt(cell.y());}
    private static String required(Properties p,String prefix,String suffix){var value=p.getProperty(prefix+suffix);if(value==null||value.isEmpty())throw bad();return value;}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid explicit Quest configuration");}
    private ModernQuestConfiguration(){}
}
