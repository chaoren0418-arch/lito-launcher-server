"""Capture negative PC primary before existing all-player lock; retain numerical and material code byte-exact."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'source-lightning-hp-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='4a2a5b3d4299e47d566c2f70be885bd82ccd5055f6c58a23dd72fcc609023d96';seed=read(Path(parent['manifest']['path']));projection=read(R/'source-lightning-output-src/manifest.json')
for key in ['original','versioned']:assert sha(Path(projection[key]['path']))==projection[key]['sha256']
inverse=Path(projection['versioned']['path']).read_text('utf-8')
for old,new in reversed(projection['changes']):assert inverse.count(new)==1;inverse=inverse.replace(new,old)
assert inverse==Path(projection['original']['path']).read_text('utf-8')
run=D/'source-lightning-output-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_source_lightning_output.py','FreezeAcceptedMethods.java','FreezeSourceTargetDelta.java','SourceLightningOutputDelta.java','SourceNegativeTargetLoad.java']:shutil.copyfile(R/name,inputs/name)
shutil.copyfile(projection['versioned']['path'],inputs/'NativeSourceLightningLifetime.java');original=run/'inverse-source/NativeSourceLightningLifetime.java';original.parent.mkdir();original.write_text(inverse,'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],projection=projection,inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*[p for p in inputs.glob('*.java')if p.name!='NativeSourceLightningLifetime.java']])
  for directory,source in [(run/'inverse-classes',original),(classes,inputs/'NativeSourceLightningLifetime.java')]:invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[base,S/'lib/*'])),'-d',directory,source])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()};old=dict(entries)
  for p in (run/'inverse-classes').rglob('*.class'):assert entries[p.relative_to(run/'inverse-classes').as_posix()]==p.read_bytes(),'whole-file inverse reproduces every base helper class byte'
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  assert len(entries)==len(old)and[n for n in old if entries[n]!=old[n]]==['local/lito381/server/NativeSourceLightningLifetime.class']
  jar=run/'candidate-source-lightning-output-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLightningOutputDelta',base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',jar,S/'lib'])
 v.update(status='built-Source17-output-capture-life-unqualified',runtime=pin(jar),whole_source_inverse_reproduced_base_classes=True)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-lightning-output-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
