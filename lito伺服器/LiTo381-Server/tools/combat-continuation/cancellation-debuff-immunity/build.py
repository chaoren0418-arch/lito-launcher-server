from pathlib import Path
import json, hashlib, subprocess, os, zipfile, shutil, sys
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent
D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=C/'cancellation-speed-immunity/build/candidate-cancellation-speed-immunity.jar'
assert sha(jar)=='345640f9126e8db6ed152eaaeb41262d3edf564a5c07b6f514206ac8ffaf4a41'
inputs=D/'inputs';inputs.mkdir(exist_ok=True)
for p in [Path(__file__),R/'NativeCancellationSpeedImmunity.java',R/'source-contract.json']:
 shutil.copyfile(p,inputs/p.name)
original=inputs/'original';original.mkdir(exist_ok=True)
shutil.copyfile(C/'cancellation-speed-immunity/NativeCancellationSpeedImmunity.java',original/'NativeCancellationSpeedImmunity.java')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
key='local/lito381/server/NativeCancellationSpeedImmunity.class'
v=dict(status='building',base=pin(jar),formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def compile(source,dest):
   dest.mkdir(exist_ok=True)
   subprocess.run(list(map(str,[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',dest,source])),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  compile(original/'NativeCancellationSpeedImmunity.java',D/'c0')
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n)for n in z.namelist()}
  assert (D/'c0'/key).read_bytes()==entries[key], 'C0 changed'
  compile(inputs/'NativeCancellationSpeedImmunity.java',D/'classes')
  changed=list((D/'classes').rglob('*.class'));assert [p.relative_to(D/'classes').as_posix()for p in changed]==[key]
  entries[key]=changed[0].read_bytes()
  candidate=D/'candidate-cancellation-debuff-immunity.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  with zipfile.ZipFile(jar)as old,zipfile.ZipFile(candidate)as new:
   assert set(old.namelist())==set(new.namelist())
   assert [n for n in old.namelist()if old.read(n)!=new.read(n)]==[key]
 v.update(status='built-unqualified',runtime=pin(candidate),C0_byte_equal=True,changed_entries=[key],inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
