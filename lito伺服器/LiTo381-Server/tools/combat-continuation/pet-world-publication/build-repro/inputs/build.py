"""Only the already-qualified WORLD pet publication guard changes."""
from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];P=R.parent/'pet-world-lifetime';D=R/('build-repro'if '--reproduce'in sys.argv else'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=P/'build/candidate-pet-world-lifetime.jar';assert sha(jar)=='18738d9d5728bfeef5f2b4cd2466f8fc87b2104352000e6b196cf17b9d216de3'
inputs=D/'inputs';orig=inputs/'original';src=inputs/'sources'
for p in [inputs,orig,src]:p.mkdir(exist_ok=True)
shutil.copyfile(P/'build/inputs/sources/NativePetWorldLifetime.java',orig/'NativePetWorldLifetime.java');shutil.copyfile(R/'NativePetWorldLifetime.java',src/'NativePetWorldLifetime.java');shutil.copyfile(__file__,inputs/'build.py')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);cp=str(jar)+';'+str(S/'lib/*')
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',c0,orig/'NativePetWorldLifetime.java')
  with zipfile.ZipFile(jar)as z:
   for p in c0.rglob('*.class'):assert p.read_bytes()==z.read(p.relative_to(c0).as_posix()),'C0 mismatch '+p.name
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,src/'NativePetWorldLifetime.java')
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  changed=[]
  for p in classes.rglob('*.class'):
   n=p.relative_to(classes).as_posix();data=p.read_bytes();assert n in entries
   if entries[n]!=data:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/NativePetWorldLifetime.class','local/lito381/server/NativePetWorldLifetime$Target.class'}
  candidate=D/'candidate-pet-world-publication.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,C0_byte_equal=True,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as error:v.update(status='failed',error=str(error));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
