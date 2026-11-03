"""Independent frozen-input byte-identical rebuild and all-method inverse; no DB access."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-readonly-inventory-origin-build-current.json');assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];build=read(Path(index['manifest']['path']));base=Path(build['base']['path']);assert sha(base)==build['base']['sha256'];run=D/'source-readonly-inventory-origin-reproduction'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for ref in build['inputs']:assert sha(Path(ref['path']))==ref['sha256'];shutil.copyfile(ref['path'],inputs/Path(ref['path']).name)
for name in ['SourceReadonlyInventoryOriginDelta.java','SourceLegacyEquipmentSqlLoad.java','SourceReadonlyInventoryModeProbe.java','FreezeSourceTargetDelta.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=index['runtime'],base=build['base'],basis=index['manifest'],Source_or_player_DB_connections=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  for ref in build['asm']:assert sha(Path(ref['path']))==ref['sha256']
  tcp=';'.join(map(str,[run/'tools',*[ref['path']for ref in build['asm']]]));classes=run/'classes';invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[tcp,base,S/'lib/*'])),'-d',run/'tools',*inputs.glob('*.java')]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceReadonlyInventoryOriginMethods','splice',base,classes])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  rebuilt=run/'candidate-rebuilt.jar'
  with zipfile.ZipFile(rebuilt,'w',zipfile.ZIP_DEFLATED)as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  assert sha(rebuilt)==index['runtime']['sha256'];invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceReadonlyInventoryOriginDelta',base,rebuilt]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLegacyEquipmentSqlLoad',rebuilt,S/'lib']);invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'tools',rebuilt,S/'lib/*'])),'local.lito381.server.SourceReadonlyInventoryModeProbe',run/'observations.json'])
 v.update(status='passed-byte-identical-readonly-inventory-origin-and-independent-all-method-inverse',rebuilt=pin(rebuilt),observations=read(run/'observations.json'),inputs=[pin(p)for p in inputs.iterdir()])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-readonly-inventory-origin-reproduction-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=index['runtime'])))
