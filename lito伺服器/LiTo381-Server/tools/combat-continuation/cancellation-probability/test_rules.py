from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build/source-rules';D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model')
paths=[source/'L1MagicPc.java',source/'L1MagicMode.java',source/'Instance/L1PcInstance.java']
for p in paths:shutil.copyfile(p,D/(sha(p)+'-'+p.name))
magic=paths[0].read_text('utf-8');pc=paths[2].read_text('utf-8')
start=magic.index('\t\tdefault:',magic.index('private int calcProbability('));end=magic.index('\t\t\tif (skillId == TAMING_MONSTER)',start)
body=magic[start:end].removeprefix('\t\tdefault:').replace('l1skills.getProbabilityDice()','dice')
def method(text,signature):
 start=text.index(signature);brace=text.index('{',start);depth=1;i=brace+1
 while depth:
  if text[i]=='{':depth+=1
  elif text[i]=='}':depth-=1
  i+=1
 return text[start:i]
guardian=method(pc,'public int guardianEncounter()')
oracle='''package local.lito381.server;
final class SourceCancellationOracle {
 int bonus,level,mr,roll;long lawful;P _pc=new P(); R _random=new R();
 final class P{boolean wizard,elf;int original,hit;boolean isWizard(){return wizard;}boolean isElf(){return elf;}int getOriginalMagicHit(){return original;}int getMagicHit(){return hit;}}
 final class R{int nextInt(int bound){if(roll<1||roll>bound)throw new AssertionError();return roll-1;}}
 int getMagicBonus(){return bonus;}int getMagicLevel(){return level;}int getTargetMr(){return mr;}int getLeverage(){return 10;}int getLawful(){return (int)lawful;}
 int legacy(int dice){int probability=0;
'''+body+'''return Math.max(Math.min(probability,90),1);}
'''+guardian+'\n}\n'
(D/'SourceCancellationOracle.java').write_text(oracle,'utf-8')
probe='''package local.lito381.server;
import java.nio.file.*;
public final class CancellationRulesProbe {
 public static void main(String[]args)throws Exception{
  long checks=0;var s=new SourceCancellationOracle();
  for(boolean wizard:new boolean[]{false,true})for(int bonus:new int[]{-1,0,1,10,30})for(int level:new int[]{0,1,5,10})for(int dice:new int[]{0,1,3,10})for(int roll:new int[]{1,3,10})for(int original:new int[]{0,5,25})for(int hit:new int[]{-10,0,30})for(int mr:new int[]{0,30,100,300}){
   if(dice>0&&roll>dice)continue;s._pc.wizard=wizard;s.bonus=bonus;s.level=level;s.roll=roll;s._pc.original=original;s._pc.hit=hit;s.mr=mr;
   int sum=dice==0?0:Math.max(bonus+level+(wizard?1:-1),1)*roll;
   if(s.legacy(dice)!=NativeCancellationProbability.legacyChance(sum,original,hit,mr))throw new AssertionError("source default probability");checks++;
  }
  for(int lawful=-40001;lawful<=40001;lawful++){s.lawful=lawful;int index=s.guardianEncounter();int expected=index==0?3:index==1?6:index==2?9:0;if(expected!=NativeCancellationProbability.guardianMr(lawful))throw new AssertionError("source lawful MR");checks++;}
  for(int chance=0;chance<=100;chance++){int passed=0;for(int roll=1;roll<=100;roll++){if(NativeCancellationProbability.succeeds(chance,roll))passed++;checks++;}if(passed!=chance)throw new AssertionError("inclusive random threshold");}
  Files.writeString(Path.of(args[0]),"{\\"checks\\":"+checks+",\\"source_legacy_guardian_and_percentile\\":true,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
(D/'CancellationRulesProbe.java').write_text(probe,'utf-8')
b=json.loads((R/'build/build.json').read_text('utf-8'));jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
classes=D/'classes';classes.mkdir(exist_ok=True);cp=';'.join(map(str,[classes,jar,S/'lib/*']));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (D/'test.log').open('w',encoding='utf-8') as log:
 for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,D/'SourceCancellationOracle.java',D/'CancellationRulesProbe.java'],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CancellationRulesProbe',D/'observations.json']]:
  subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
result={'status':'passed-source-extracted-legacy-and-guardian','runtime':b['runtime'],'sources':[{'path':str(p),'sha256':sha(p)} for p in paths],'observations':json.loads((D/'observations.json').read_text()),'source_database_mutated':False,'formal_deployment_changed':False}
(D/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),'utf-8');print(json.dumps(result['observations']))
