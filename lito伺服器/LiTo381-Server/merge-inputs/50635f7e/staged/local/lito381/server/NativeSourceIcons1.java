package local.lito381.server;
import java.util.*;
/** Complete Source ICONS1 encoder from an immutable scalar snapshot; confers no actor/output authority. */
final class NativeSourceIcons1 {
 record Snapshot(Set<Integer> active,Map<Integer,Integer> seconds,boolean coexist,int overrideSkill,int overrideSeconds,long wallMillis){Snapshot{active=Set.copyOf(active);seconds=Map.copyOf(seconds);}int seconds(int skill){return seconds.getOrDefault(skill,0);}}
 record Rule(int skill,int slot,int mode){}
 private static final List<Rule> RULES=List.of(
  new Rule(32,0,0),new Rule(31,1,0),new Rule(14,3,0),new Rule(71,4,0),new Rule(78,5,0),new Rule(104,7,0),new Rule(47,8,2),new Rule(56,9,2),
  new Rule(111,17,0),new Rule(55,18,0),new Rule(158,19,0),new Rule(167,20,0),new Rule(153,21,0),new Rule(134,22,0),new Rule(176,23,3),new Rule(133,24,4),
  new Rule(171,26,0),new Rule(155,27,0),new Rule(174,30,0),new Rule(175,31,0),new Rule(173,32,0),new Rule(206,52,1),new Rule(216,53,1),new Rule(217,54,1),
  new Rule(191,55,0),new Rule(193,56,0),new Rule(188,57,0),new Rule(211,58,0),new Rule(183,59,0),new Rule(181,60,1),new Rule(1017,61,0));
 private static final List<Integer> WEAPONS=List.of(148,155,163,149,156,166);
 private static final Set<Integer> PROXIES=Set.of(3,8,12,26,42,48,99,109,110,114,115,148,149,151,155,156,159,163,166,168);
 static boolean independent(Snapshot snapshot,int skill){return snapshot.coexist()&&PROXIES.contains(skill);}
 static byte duration(int seconds,int mode){return (byte)switch(mode){case 0->seconds/4;case 1->seconds/16;case 2->seconds<=0?0:seconds<32?Math.max(1,seconds/2):seconds/4;case 3->Math.min(seconds,4080)/16;case 4->Math.min(seconds,1020)/4;default->throw new AssertionError(mode);};}
 static void put(byte[] data,Rule rule,int seconds){data[rule.slot()]=duration(seconds,rule.mode());if(rule.slot()==24)data[25]=3;}
 static byte[] packet(Snapshot snapshot){Objects.requireNonNull(snapshot);byte[] data=new byte[204];
  for(var rule:RULES)if(snapshot.active().contains(rule.skill())&&(rule.slot()!=27||!independent(snapshot,rule.skill())))put(data,rule,snapshot.seconds(rule.skill()));
  for(int skill:WEAPONS)if(snapshot.active().contains(skill)&&!independent(snapshot,skill)){data[10]=duration(snapshot.seconds(skill),0);data[11]=(byte)(skill-1);break;}
  int epoch=(int)(snapshot.wallMillis()/1000);for(int i=0;i<4;i++)data[72+i]=(byte)(epoch>>>(8*i));
  int override=snapshot.overrideSkill(),seconds=snapshot.overrideSeconds();if(override>0&&seconds>0&&!independent(snapshot,override)){
   for(var rule:RULES)if(rule.skill()==override)put(data,rule,seconds);
   if(WEAPONS.contains(override)){data[10]=duration(seconds,0);data[11]=(byte)(override-1);}
  }
  var wire=new Protocol.Writer().c(Protocol.S_PACKET_BOX).c(20).d(data.length);for(byte value:data)wire.c(value);return wire.bytes();
 }
 private NativeSourceIcons1(){}
}
