"""Compile-only continuation: non-null encounter generations and controlled39 cost HUD; no game/DB fixture."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'source-remaining-material-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='bd462f9e6171e02ae934b6e5fb9926faf5efa73e926f996a7ac213795f942d7e';seed=read(Path(parent['manifest']['path']));run=D/'source-program-safety-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in [Path(__file__).name,'NativeSourceMonsterHp.java','NativeSourceRemainingMaterials.java','SourceDamageDisplayLoad.java','SourceMonsterHpLoad.java','SourceRemainingMaterialLoad.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],inputs=[pin(p)for p in inputs.iterdir()],native_runtime_cases=0,actual_player_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[base,S/'lib/*'])),'-d',classes,*[inputs/name for name in ['NativeSourceMonsterHp.java','NativeSourceRemainingMaterials.java']]])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()};original=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed={n for n in entries if original.get(n)!=entries[n]};expected={'local/lito381/server/'+n+'.class'for n in ['NativeSourceMonsterHp','NativeSourceRemainingMaterials','NativeSourceRemainingMaterials$Aim']};assert changed==expected,(changed,expected)
  jar=run/'candidate-source-program-safety-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'tools',*[inputs/name for name in ['SourceDamageDisplayLoad.java','SourceMonsterHpLoad.java','SourceRemainingMaterialLoad.java']]])
  for load in ['SourceDamageDisplayLoad','SourceMonsterHpLoad','SourceRemainingMaterialLoad']:invoke([J/'java.exe','-Xverify:all','-cp',run/'tools',load,jar,S/'lib'])
 v.update(status='built-Source-program-update-compile-only-unqualified',runtime=pin(jar),changed_entries=sorted(changed))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-program-safety-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
