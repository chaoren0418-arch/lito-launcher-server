"""Isolated Source17 area geometry layer atop the pinned3636 candidate. No formal changes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'source-lightning-area-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='c24e8294794d4b13a02e5d74cdf04c524a3f557399fdd16e693c5a7a192f18a6';seed=read(Path(read(D/'source-negative-player-build-current.json')['manifest']['path']));run=D/'source-lightning-lifetime-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_source_lightning_lifetime.py','SourceLightningLifetimeMethods.java','SourceLightningLifetimeDelta.java','NativeSourceLightningLifetime.java','FreezeAcceptedMethods.java','FreezeSourceTargetDelta.java','SourceNegativeTargetLoad.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*[p for p in inputs.glob('*.java')if p.name!='NativeSourceLightningLifetime.java']]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLightningLifetimeMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceLightningLifetime.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()};old=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in old if old[n]!=entries[n]);fresh=sorted(set(entries)-set(old));assert changed==['local/lito381/server/ClientSession.class','local/lito381/server/NativeAreaMaterialAction$Context.class','local/lito381/server/NativeAreaMaterialAction.class']and fresh==['local/lito381/server/NativeSourceLightningLifetime$Aim.class','local/lito381/server/NativeSourceLightningLifetime$Output.class','local/lito381/server/NativeSourceLightningLifetime.class'];jar=run/'candidate-source-lightning-lifetime-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLightningLifetimeMethods','verify',base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLightningLifetimeDelta',base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',jar,S/'lib'])
 v.update(status='built-Source17-prelock-lifecycle-unqualified',runtime=pin(jar),changed=changed,fresh=fresh)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-lightning-lifetime-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
