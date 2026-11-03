from pathlib import Path
import hashlib,json,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'resurrection-lifetime/build/candidate-resurrection-lifetime.jar';assert sha(jar)=='9042d4237407fbdc35a22fb6bd751e1b60ec0d43db3949be3ebc89432dd87a42'
inputs=D/'inputs';inputs.mkdir(exist_ok=True);original=inputs/'original';original.mkdir(exist_ok=True)
for p in [Path(__file__),R/'NativeResurrectionMaterialAction.java']:shutil.copyfile(p,inputs/p.name)
prior=R.parent/'resurrection-material/NativeResurrectionMaterialAction.java';shutil.copyfile(prior,original/prior.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as out:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
  for destination,source in [(c0,original/prior.name),(classes,inputs/prior.name)]:run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',destination,source)
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in c0.rglob('*.class'):assert p.read_bytes()==entries[p.relative_to(c0).as_posix()],'C0 mismatch'
  changed=[];added=[]
  for p in classes.rglob('*.class'):
   name=p.relative_to(classes).as_posix();data=p.read_bytes()
   if name not in entries:added.append(name)
   elif entries[name]!=data:changed.append(name)
   entries[name]=data
  assert changed==['local/lito381/server/NativeResurrectionMaterialAction.class'],changed
  assert added==['local/lito381/server/NativeResurrectionMaterialAction$Target.class'],added
  candidate=D/'candidate-resurrection-companion.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  with zipfile.ZipFile(jar)as a,zipfile.ZipFile(candidate)as z:
   assert all(a.read(name)==z.read(name)for name in a.namelist()if name not in changed),'unrelated entry changed'
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=added,C0_byte_equal=True,all_other_entries_equal=True,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
