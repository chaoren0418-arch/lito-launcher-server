package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Immutable packaged catalog only: no hub, scheduler, Source connection or game process. */
public final class StaticQuestMetadata {
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
  var f=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");f.setAccessible(true);var maps=(Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)f.get(null);var noAI=Set.of(71075,70957,81209,80012);var rows=new ArrayList<String>();rows.add("map\tspawn\tnpc\tspeed\tmovement_distance\tgfx\tSource_no_AI");int eligible=0;
  for(var map:new TreeSet<>(maps.keySet()))for(var npc:ClassicWorldNpcCatalog.all(map))if(npc.implementation().equals("L1Quest")){rows.add(map+"\t"+npc.spawnId()+"\t"+npc.npcId()+"\t"+npc.passispeed()+"\t"+npc.movementDistance()+"\t"+npc.gfxId()+"\t"+noAI.contains(npc.npcId()));if(npc.passispeed()>0&&!noAI.contains(npc.npcId()))eligible++;}
  Files.write(Path.of(args[0]),rows);System.out.println("Source quest rows="+(rows.size()-1)+" currently movable non-excluded rows="+eligible);
 }
}
