package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
/** Read only: native getter metadata against the startup Source target snapshot. */
public final class MaterialTargetAuditProbe {
 public static void main(String[]a)throws Exception{
  var jar=Path.of(a[2]).toRealPath();if(!Path.of(BattleSpellCatalog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("actual catalog");ResourceDatabase.load(ServerConfig.load(Path.of(a[0]),"standard"));var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var data=(CombatDatabase.Snapshot)f.get(null);
  var getters=Arrays.stream(BattleSpellCatalog.class.getDeclaredMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==1&&m.getParameterTypes()[0]==int.class&&m.getReturnType().isRecord()&&m.getName().endsWith("Spell")).sorted(Comparator.comparing(Method::getName)).toList();var lines=new ArrayList<String>();lines.add("skill\tgetter\trecord\tfield\tnative\tSource\tclassification");int skills=0,records=0,differences=0;
  for(var e:new TreeMap<>(data.skills()).entrySet())if(e.getValue().reagent()>0&&e.getValue().reagentCount()>0){skills++;int id=e.getKey();var source=NativeSkillTargetMetadata.row(id);boolean found=false;
   for(var getter:getters){getter.setAccessible(true);Object value;try{value=getter.invoke(null,id);}catch(InvocationTargetException error){throw new AssertionError("catalog getter failed "+getter.getName()+" id="+id,error.getCause());}if(value==null)continue;Method skill;try{skill=value.getClass().getDeclaredMethod("skillId");}catch(NoSuchMethodException other){continue;}skill.setAccessible(true);if(!skill.invoke(value).equals(id))continue;found=true;records++;
    var expected=Map.of("targetTo",source.targetTo(),"maximumRange",source.ranged(),"area",source.area(),"areaRadius",source.area(),"through",source.through()?1:0,"type",source.type(),"sourceType",source.type(),"attribute",source.attribute(),"nativeId",source.nativeId());
    for(var component:value.getClass().getRecordComponents())if(expected.containsKey(component.getName())){var access=component.getAccessor();access.setAccessible(true);Object nativeValue=access.invoke(value);int number=nativeValue instanceof Boolean b?b?1:0:((Number)nativeValue).intValue();boolean mismatch=number!=expected.get(component.getName());if(mismatch)differences++;lines.add(id+"\t"+getter.getName()+"\t"+value.getClass().getSimpleName()+"\t"+component.getName()+"\t"+number+"\t"+expected.get(component.getName())+"\t"+(mismatch?"REVIEW-native-protocol-or-semantic-difference":"equal"));}
   }if(!found)lines.add(id+"\t-\t-\t-\t-\t-\tno-record-getter-found");
  }
  Files.write(Path.of(a[1]),lines);Files.writeString(Path.of(a[1]).resolveSibling("observations.json"),"{\"material_rows\":"+skills+",\"matched_getter_records\":"+records+",\"field_differences_to_review\":"+differences+",\"created_Hub\":false,\"full_goal_achieved\":false}\n");
 }
}
