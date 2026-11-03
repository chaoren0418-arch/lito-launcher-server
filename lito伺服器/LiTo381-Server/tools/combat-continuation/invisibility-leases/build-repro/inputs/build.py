from pathlib import Path
import json,hashlib,shutil,subprocess,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'bind-control/build/candidate-bind-control.jar';assert sha(jar)=='29534793beba10c1d1a4d4a922bfaae769cbc0ad9e3500c107f737ca49f38c1f'
asm=[Path(r['path']) for r in read(R.parent/'build/integration-regression/preparation.json')['asm']];inputs=D/'inputs';orig=inputs/'original';src=inputs/'sources';refs=inputs/'source-reference'
for p in [inputs,orig,src,refs]:p.mkdir(exist_ok=True)
for p in [R/'LeaseMethods.java',R/'build.py',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
shutil.copyfile(S/'src/main/java/local/lito381/server/BattleInvisibilityStatus.java',orig/'BattleInvisibilityStatus.java');shutil.copyfile(R/'BattleInvisibilityStatus.java',src/'BattleInvisibilityStatus.java')
for name in ['L1Character.java','Instance/L1PcInstance.java','skill/L1SkillUse.java','skill/L1SkillStop.java','skill/L1SkillTimerTimerImpl.java']:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model')/name;shutil.copyfile(p,refs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v={'status':'building','base':pin(jar),'formal_deployment_changed':False,'full_goal_achieved':False}
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',c0,orig/'BattleInvisibilityStatus.java')
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'LeaseMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  run(J/'java.exe','-Xverify:all','-cp',tcp,'LeaseMethods','c0',jar,c0)
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'LeaseMethods','splice',jar,classes);run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',classes,src/'BattleInvisibilityStatus.java')
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n.startswith('local/lito381/server/NativeAcceptedState$'):continue
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class' for n in ['ClientSession','BattleInvisibilityStatus','NativeAcceptedState']},changed
  assert set(fresh)=={'local/lito381/server/BattleInvisibilityStatus$'+n+'.class' for n in ['Lease','Snapshot']},fresh
  candidate=D/'candidate-invisibility-leases.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'LeaseMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,C0_normalized_equal=True,inputs=[pin(p) for p in inputs.rglob('*') if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-4500:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
