from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro'if '--reproduce'in sys.argv else'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'return-nature-transfer/build/candidate-return-nature-transfer.jar';assert sha(jar)=='54a781b55528a43e037508561b422ca4376f06dee0777493050a0decf9ca158c'
inputs=D/'inputs';src=inputs/'sources';refs=inputs/'source-reference'
for p in [inputs,src,refs]:p.mkdir(exist_ok=True)
for name in ['NativePolymorphSource.java','NativePolymorphWeights.java']:shutil.copyfile(R/name,src/name)
for p in [R/'build.py',R/'PolymorphSourceMethods.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
for rel in ['datatables/WandPolyTable.java','datatables/PolyTable.java','model/L1PolyMorph.java']:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')/rel;shutil.copyfile(p,refs/p.name)
asm=[Path(p['path'])for p in read(R.parent/'build/integration-regression/preparation.json')['asm']];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[jar,S/'lib/*'])),'-d',classes,*src.glob('*.java'))
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'PolymorphSourceMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  run(J/'java.exe','-Xverify:all','-cp',tcp,'PolymorphSourceMethods','splice',jar,classes,classes)
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class'for n in ['CombatDatabase','BattlePolymorphCatalog']},changed
  assert set(fresh)=={'local/lito381/server/'+n+'.class'for n in ['NativePolymorphSource','NativePolymorphSource$Snapshot','NativePolymorphSource$Row','NativePolymorphWeights','NativePolymorphWeights$Row']},fresh
  candidate=D/'candidate-polymorph-source.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'PolymorphSourceMethods','verify',jar,candidate,classes)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()],remaining=['actual shared SQL read/rollback/close and retry validation','actual skill67 random/controlled menu regressions','cause-mismatch no-change response and polymorph projectile metadata are not qualified','final composed regression and native UI publication gates'])
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
