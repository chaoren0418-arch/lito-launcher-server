"""Frozen-input compile/rebuild/inverse only. Actual game actors, MySQL and cursor validation remain deferred."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
layers=[('display','source-damage-display','SourceDamageDisplayMethods','NativeSourceDamageDisplay','SourceDamageDisplayLoad'),('hp','source-monster-hp','SourceMonsterHpMethods','NativeSourceMonsterHp','SourceMonsterHpLoad'),('remaining','source-remaining-material','SourceRemainingMaterialMethods','NativeSourceRemainingMaterials','SourceRemainingMaterialLoad')]
run=D/'source-program-update-reproduction'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',layers=[],created_Hub=False,created_SQL_connection=False,actual_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  for kind,stem,patch,helper,loader in layers:
   index=read(D/(stem+'-build-current.json'));assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];b=read(Path(index['manifest']['path']));base=Path(b['base']['path']);assert sha(base)==b['base']['sha256'];assert sha(Path(index['runtime']['path']))==index['runtime']['sha256'];folder=run/kind;inputs=folder/'inputs';inputs.mkdir(parents=True)
   for ref in b['inputs']:assert sha(Path(ref['path']))==ref['sha256'];shutil.copyfile(ref['path'],inputs/Path(ref['path']).name)
   for name in ['SourceProgramUpdateDelta.java','FreezeSourceTargetDelta.java']:shutil.copyfile(R/name,inputs/name)
   tcp=';'.join(map(str,[folder/'tools',*[ref['path']for ref in b['asm']]]));classes=folder/'classes'
   invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',folder/'tools',*[p for p in inputs.glob('*.java')if p.name!=helper+'.java']]);args=[J/'java.exe','-Xverify:all','-cp',tcp,patch,'splice',base,classes]
   if kind!='hp':args+=[S/'lib']
   invoke(args);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/(helper+'.java')])
   with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
   for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
   jar=folder/'rebuilt.jar'
   with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
    for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
   assert sha(jar)==index['runtime']['sha256'];invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceProgramUpdateDelta',kind,base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,loader,jar,S/'lib']);v['layers'].append(dict(kind=kind,basis=index['manifest'],runtime=index['runtime'],rebuilt=pin(jar),inputs=[pin(p)for p in inputs.iterdir()]))
  # The last layer only recompiles the three explicitly changed helper classes.
  index=read(D/'source-program-safety-build-current.json');b=read(Path(index['manifest']['path']));base=Path(b['base']['path']);assert sha(base)==b['base']['sha256'];folder=run/'safety';inputs=folder/'inputs';inputs.mkdir(parents=True)
  for ref in b['inputs']:assert sha(Path(ref['path']))==ref['sha256'];shutil.copyfile(ref['path'],inputs/Path(ref['path']).name)
  classes=folder/'classes';invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[base,S/'lib/*'])),'-d',classes,*[inputs/(name+'.java')for name in ['NativeSourceMonsterHp','NativeSourceRemainingMaterials']]])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()};original=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  assert sorted(n for n in entries if original.get(n)!=entries[n])==b['changed_entries'];jar=folder/'rebuilt.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  assert sha(jar)==index['runtime']['sha256'];v['runtime']=index['runtime'];v['layers'].append(dict(kind='safety',basis=index['manifest'],runtime=index['runtime'],rebuilt=pin(jar)));v['status']='passed-four-byte-identical-program-builds-and-three-independent-all-original-method-inverses-no-game-validation'
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-program-update-reproduction-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],layers=len(v['layers']),runtime=v.get('runtime'))))
