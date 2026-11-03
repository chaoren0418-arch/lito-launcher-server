"""Rebuild the frozen BATTLE companion increment without changing candidate pointers."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
name='create-zombie-reproduction';index=read(D/'create-zombie-build-current.json');assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];build=read(Path(index['manifest']['path']));assert build['status'].startswith('built-Source-create-zombie-transaction');base=Path(build['base']['path']);assert sha(base)==build['base']['sha256'] and sha(Path(index['runtime']['path']))==index['runtime']['sha256']
sources=[p for p in build['frozen_inputs'] if p['path'].endswith('.java')]
for p in sources:assert sha(Path(p['path']))==p['sha256']
run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
for p in sources:shutil.copyfile(p['path'],inputs/Path(p['path']).name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='running',runtime=index['runtime'],basis=index['manifest'],base=build['base'],frozen_inputs=sources,full_chain_rebuilt=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False)
try:
 classes=run/'classes'
 with log.open('w',encoding='utf-8') as output:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=output,stderr=subprocess.STDOUT,check=True,timeout=180)
  for ref in build['asm']:assert sha(Path(ref['path']))==ref['sha256']
  tools=run/'tools';tcp=';'.join(map(str,[tools,*[ref['path'] for ref in build['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'CreateZombieMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'CreateZombieMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeCreateZombieLifecycle.java',inputs/'NativeSummonOwnerLifecycle.java'])
 with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()}
 for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
 rebuilt=run/'candidate-rebuilt.jar'
 with zipfile.ZipFile(rebuilt,'w',zipfile.ZIP_DEFLATED) as z:
  for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
 assert sha(rebuilt)==index['runtime']['sha256'],'frozen increment differs'
 v.update(status='passed-byte-identical-frozen-create-zombie-transaction-increment-rebuild',rebuilt=pin(rebuilt))
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],sha256=sha(rebuilt))))
