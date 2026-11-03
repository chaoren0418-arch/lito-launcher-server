from pathlib import Path
import hashlib,json,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'weapon-break-lifetime/build/candidate-weapon-break-lifetime.jar';assert sha(jar)=='221518da47ac7aca5ba7c134628d3de09e2073c6c63870ab67bcace7748db9e7'
inputs=D/'inputs';inputs.mkdir(exist_ok=True);original=inputs/'original';original.mkdir(exist_ok=True)
for p in [Path(__file__),R/'NativeNpcPhysicalSource.java']:shutil.copyfile(p,inputs/p.name)
for p in [R.parent/'npc-physical-source/NativeNpcPhysicalSource.java']:shutil.copyfile(p,original/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as out:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
  for dest,src in [(c0,original),(classes,inputs)]:run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',dest,*src.glob('*.java'))
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in c0.rglob('*.class'):assert p.read_bytes()==entries[p.relative_to(c0).as_posix()],'C0 mismatch '+p.name
  changed=[];added=[]
  for p in classes.rglob('*.class'):
   name=p.relative_to(classes).as_posix();data=p.read_bytes()
   if name not in entries:added.append(name)
   elif entries[name]!=data:changed.append(name)
   entries[name]=data
  assert set(changed)=={'local/lito381/server/NativeNpcPhysicalSource.class'},changed
  assert not added,added
  candidate=D/'candidate-npc-physical-size.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  with zipfile.ZipFile(jar)as a,zipfile.ZipFile(candidate)as z:assert all(a.read(name)==z.read(name)for name in a.namelist()if name not in changed)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=added,C0_byte_equal=True,all_other_entries_equal=True,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
