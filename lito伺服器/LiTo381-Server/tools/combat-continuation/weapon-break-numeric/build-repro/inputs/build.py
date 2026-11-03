from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro'if '--reproduce'in sys.argv else'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'pet-world-publication/build/candidate-pet-world-publication.jar';assert sha(jar)=='09e3ce8c6f0fea809bdca869f9d71ab3b12c80157c595fe97045fc5e4f490361'
inputs=D/'inputs';orig=inputs/'original';src=inputs/'sources'
for p in [inputs,orig,src]:p.mkdir(exist_ok=True)
shutil.copyfile(S/'src/main/java/local/lito381/server/BattleWeaponBreak.java',orig/'BattleWeaponBreak.java');shutil.copyfile(R/'BattleWeaponBreak.java',src/'BattleWeaponBreak.java')
for p in [R/'build.py',R/'WeaponBreakNumericMethods.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');asm=[Path(p['path'])for p in read(R.parent/'build/integration-regression/preparation.json')['asm']];v=dict(status='building',base=pin(jar),formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'build.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0-normalized';patch=D/'patched';tools=D/'tools';classes=D/'classes'
  for p in [c0,patch,tools,classes]:p.mkdir(exist_ok=True)
  for source,dest in [(orig,c0),(src,patch)]:run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',dest,source/'BattleWeaponBreak.java')
  cp=';'.join(map(str,[tools,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',tools,inputs/'NativeSkillConsumptionMethods.java',inputs/'WeaponBreakNumericMethods.java')
  run(J/'java.exe','-Xverify:all','-cp',cp,'WeaponBreakNumericMethods','splice',jar,classes,c0,patch)
  with zipfile.ZipFile(jar)as z:entries={n:z.read(n)for n in z.namelist()}
  key='local/lito381/server/BattleWeaponBreak.class';entries[key]=(classes/key).read_bytes();candidate=D/'candidate-weapon-break-numeric.jar'
  with zipfile.ZipFile(candidate,'w')as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',cp,'WeaponBreakNumericMethods','verify',jar,candidate,c0,patch)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=[key],semantic_C0_and_single_method_inverse=True,inputs=[pin(p)for p in inputs.rglob('*')if p.is_file()])
except BaseException as error:v.update(status='failed',error=str(error));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
