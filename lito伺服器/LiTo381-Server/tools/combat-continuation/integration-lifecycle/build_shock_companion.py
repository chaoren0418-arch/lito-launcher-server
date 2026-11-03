from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'controlled-negative-material-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='3e8532141119a0048aefcd38f595813c3c93bd84ba1ac7ced56d285b55c4ab61';seed=read(Path(parent['manifest']['path']));run=D/'shock-companion-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_shock_companion.py','ShockCompanionMethods.java','NativeShockCompanionAction.java']:shutil.copyfile(R/name,inputs/name)
source=[]
for name in ['skill/L1SkillUse.java','L1MagicPc.java']:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model')/name;q=run/'Source'/p.name;q.parent.mkdir(exist_ok=True);shutil.copyfile(p,q);source.append(dict(original=pin(p),frozen=pin(q)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],Source=source,inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  tcp=';'.join(map(str,[run/'tools',*[p['path']for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'ShockCompanionMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ShockCompanionMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeShockCompanionAction.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()};old=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in old if entries[n]!=old[n]);fresh=sorted(set(entries)-set(old));assert changed==['local/lito381/server/CombatCompanionSpells.class','local/lito381/server/NativeAreaMaterialAction.class']and fresh==['local/lito381/server/NativeShockCompanionAction$Death.class','local/lito381/server/NativeShockCompanionAction.class'],(changed,fresh);jar=run/'candidate-shock-companion-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ShockCompanionMethods','verify',base,jar])
 v.update(status='built-controlled-NPC87-admission-pair-lifetime-unqualified',runtime=pin(jar),changed=changed,fresh=fresh)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'shock-companion-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
