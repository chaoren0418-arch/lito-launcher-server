"""Compare seven controls with exact Source getter/capacity method bodies; no live SQL."""
from pathlib import Path
import re,json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;D=R/'build/plan';D.mkdir(parents=True,exist_ok=True)
base=Path('D:/lineage開發/1-伺服器');source=base/'src/com/lineage/server/model/Instance/L1PcInstance.java';config=base/'src/com/lineage/config/ConfigCharSetting.java';settings=base/'config/charsettings.properties'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p),sha256=sha(p))
runtime=json.loads((R.parent/'poison-publication/build/build.json').read_text('utf-8'))['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256']
text=source.read_text('utf-8');methods=[]
for name in ['getChaForPet','getRemainingControlledPetSlots','canTakePet']:
 m=re.search(r'\tpublic (?:int|boolean) '+name+r'\(',text);assert m,name
 start=m.start();opening=text.index('{',start);end=opening+1;depth=1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 methods.append(text[start:end])
c=config.read_text('utf-8');fields=[];assign=[]
for line in c.splitlines():
 if re.match(r'\s*public static int \w+_CHA_FOR_PET;',line):fields.append(line.strip())
 if re.match(r'\s*\w+_CHA_FOR_PET\s*=',line):assign.append('ConfigCharSetting.'+line.strip())
assert len(fields)==len(assign)==7
reference='''package local.lito381.server;import java.util.*;
final class SourcePetReference {
 final int type,cha;final Map<Integer,com.lineage.server.model.Instance.L1NpcInstance> pets=new LinkedHashMap<>();
 SourcePetReference(int type,int cha,int count,int used){this.type=type;this.cha=cha;for(int i=0;i<count;i++)pets.put(i,new com.lineage.server.model.Instance.L1NpcInstance(i==0?used:0));}
 int getCha(){return cha;}Map<Integer,com.lineage.server.model.Instance.L1NpcInstance> getPetList(){return pets;}
 boolean isCrown(){return type==0;}boolean isKnight(){return type==1;}boolean isElf(){return type==2;}boolean isWizard(){return type==3;}boolean isDarkelf(){return type==4;}boolean isDragonKnight(){return type==5;}boolean isIllusionist(){return type==6;}
 static final int MAX_CONTROLLED_PET_COUNT=8;
 static final class ConfigCharSetting {FIELDS}
 static void configure(Properties set){ASSIGN}
 METHODS
}
'''.replace('FIELDS','\n'.join(fields)).replace('ASSIGN','\n'.join(assign)).replace('METHODS','\n'.join(methods))
ref=D/'SourcePetReference.java';ref.write_text(reference,encoding='utf-8');npc=D/'L1NpcInstance.java';npc.write_text('package com.lineage.server.model.Instance; public class L1NpcInstance {final int cost;public L1NpcInstance(int c){cost=c;}public int getPetcost(){return cost;}}',encoding='utf-8');classes=D/'classes';classes.mkdir(exist_ok=True)
inputs=[source,config,settings,R/'NativePetControls.java',R/'PetControlPlanProbe.java',Path(__file__),ref,npc];frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in inputs:shutil.copyfile(p,frozen/(sha(p)+'-'+p.name))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='running',runtime=runtime,inputs=[pin(p)for p in inputs],scope='pure source parser and capacity decision only; input charisma is already effective and clamped; no native consumer, ownership, factory, or publication qualification',formal_deployment_changed=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  for args in [[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',classes,ref,npc,R/'NativePetControls.java',R/'PetControlPlanProbe.java'],[J/'java.exe','-Xverify:all','-cp',str(classes)+';'+str(jar),'local.lito381.server.PetControlPlanProbe',settings,D/'observations.json']]:subprocess.run(list(map(str,args)),env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=45)
 v.update(status='passed-pure-planning-only',observations=pin(D/'observations.json'))
except BaseException as error:v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace'));raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print((D/'observations.json').read_text('utf-8'))
