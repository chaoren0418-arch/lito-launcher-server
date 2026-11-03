"""Rebuild the self and PC-capture layers independently from their frozen inputs, without replacing selectors."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind='output';index=read(D/'source-kiringku-presentation-build-current.json');assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];build=read(Path(index['manifest']['path']));base=Path(build['base']['path']);assert sha(base)==build['base']['sha256']and sha(Path(index['runtime']['path']))==index['runtime']['sha256'];run=D/('source-kiringku-presentation-reproduction')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for ref in build['inputs']:assert sha(Path(ref['path']))==ref['sha256'];shutil.copyfile(ref['path'],inputs/Path(ref['path']).name)
shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(R/'SourceKiringkuPresentationDelta.java',inputs/'SourceKiringkuPresentationDelta.java');shutil.copyfile(R/'FreezeSourceTargetDelta.java',inputs/'FreezeSourceTargetDelta.java');J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=index['runtime'],base=build['base'],basis=index['manifest'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  for ref in build['asm']:assert sha(Path(ref['path']))==ref['sha256']
  tcp=';'.join(map(str,[run/'tools',*[ref['path']for ref in build['asm']]]));native='NativeSourceKiringkuPresentation.java';invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*[p for p in inputs.glob('*.java')if p.name!=native]]);classes=run/'classes'
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceKiringkuPresentationMethods','splice',base,classes])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/native])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  rebuilt=run/'candidate-rebuilt.jar'
  with zipfile.ZipFile(rebuilt,'w',zipfile.ZIP_DEFLATED)as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  assert sha(rebuilt)==index['runtime']['sha256']
  if kind=='self':invoke([J/'java.exe','-Xverify:all','-cp',tcp,'NegativeSelfMethods','verify',base,rebuilt,S/'lib'])
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceKiringkuPresentationDelta',base,rebuilt]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',rebuilt,S/'lib'])
 v.update(status='passed-byte-identical-Source-kiringku-and-independent-all-method-inverse',rebuilt=pin(rebuilt),inputs=[pin(p)for p in inputs.iterdir()])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source-kiringku-presentation-reproduction-current.json'),dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=index['runtime'])))
