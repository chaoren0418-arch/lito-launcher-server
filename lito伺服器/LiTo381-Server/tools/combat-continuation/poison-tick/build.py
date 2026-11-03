from pathlib import Path
import hashlib,json,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro'if '--reproduce'in sys.argv else'build');D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));read=lambda p:json.loads(p.read_text('utf-8'))
jar=R.parent/'poison-owner/build/candidate-poison-owner.jar';assert sha(jar)=='6780344aae6cc593436870c1eb93e95ab0238ff85d20d3d7edf8519f648776be'
inputs=D/'inputs';original=inputs/'original';src=inputs/'sources'
for p in [inputs,original,src]:p.mkdir(exist_ok=True)
for name in ['NativePoisonMaterialAction.java']:
 shutil.copyfile(R/name,src/name);shutil.copyfile(R.parent/'poison-owner/build/inputs/sources'/name,original/name)
for p in [Path(__file__),R/'PoisonTickMethods.java',B/'NativeSkillConsumptionMethods.java',Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/poison/L1DamagePoison.java')]:shutil.copyfile(p,inputs/p.name)
asm=[Path(p['path'])for p in read(R.parent/'build/integration-regression/preparation.json')['asm']];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as out:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True);tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]))
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',c0,*original.glob('*.java'))
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in c0.rglob('*.class'):assert p.read_bytes()==entries[p.relative_to(c0).as_posix()],'C0 mismatch '+p.name
  run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'PoisonTickMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  run(J/'java.exe','-Xverify:all','-cp',tcp,'PoisonTickMethods','splice',jar,classes)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,*src.glob('*.java'))
  prior=dict(entries);changed=[];added=[]
  for p in classes.rglob('*.class'):
   name=p.relative_to(classes).as_posix();data=p.read_bytes()
   if name not in entries:added.append(name)
   elif entries[name]!=data:changed.append(name)
   entries[name]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class'for n in ['ClientSession','NativePoisonMaterialAction']},changed
  assert not added,added
  candidate=D/'candidate-poison-tick.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'PoisonTickMethods','verify',jar,candidate)
  assert all(entries[name]==data for name,data in prior.items()if name not in changed)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=added,C0_byte_equal=True,all_other_entries_equal=True,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
