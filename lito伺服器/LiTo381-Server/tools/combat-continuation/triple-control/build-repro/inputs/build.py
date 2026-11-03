from pathlib import Path
import json,hashlib,shutil,subprocess,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'invisibility-control/build/candidate-invisibility-control.jar';assert sha(jar)=='c7f2ad7ad7d527e5f2e6f86efbf3f42d48a4828e29543d9216d09ddec9f56729'
asm=[Path(r['path']) for r in read(R.parent/'build/integration-regression/preparation.json')['asm']];inputs=D/'inputs';orig=inputs/'original';src=inputs/'sources';refs=inputs/'source-reference'
for p in [inputs,orig,src,refs]:p.mkdir(exist_ok=True)
for p in [R/'TripleMethods.java',R/'CombatTripleArrow.java',R/'build.py',B/'NativeSkillConsumptionMethods.java',R.parent/'solid-control/SolidMethods.java']:shutil.copyfile(p,inputs/p.name)
shutil.copyfile(R.parent/'invisibility-control/build/inputs/sources/CombatRules.java',orig/'CombatRules.java')
for relative,expected in [('config/ConfigSkill.java','ffc81f59cdd57d18371538b92ab0d6b591009fbb37355be86737adbc414e7d7e'),('server/model/L1AttackPc.java','3fdc94cedc6c3ec803e731be238093bd7fe55c432cf84be6822a079a1627ddfc')]:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage')/relative;assert sha(p)==expected;shutil.copyfile(p,refs/p.name)
def method(text,signature):
 a=text.index(signature);b=text.index('{',a);depth=1;z=b+1
 while depth:
  if text[z]=='{':depth+=1
  elif text[z]=='}':depth-=1
  z+=1
 return text[a:z]
parser=method((refs/'ConfigSkill.java').read_text('utf-8'),'private static double _getDouble(')
attack=(refs/'L1AttackPc.java').read_text('utf-8');a=attack.index('if (_pc.getTripleArrowHitsRemaining() > 0');z=attack.index('\n\t\t}',a)+5;fragment=attack[a:z];fragment=fragment.replace('_pc.getTripleArrowHitsRemaining()','remaining').replace('_pc.setTripleArrowHitsRemaining(remaining - 1);','remaining--;').replace('ConfigSkill.TRIPLE_ARROW_DMG','configured')
(refs/'SourceTripleReference.java').write_text('package local.lito381.server; import java.util.Properties; final class SourceTripleReference {\n'+parser+'\nstatic double configured(Properties p){return _getDouble(p,"Triple_Arrow_Dmg",1.0);}\nstatic double[] multiplier(double _damageMultiplier,int remaining,int _weaponType2,double configured){\n'+fragment+'\nreturn new double[]{_damageMultiplier,remaining};}\n}',encoding='utf-8')
t=(orig/'CombatRules.java').read_text('utf-8');needle='"INVIS_CAST_BREAK",';assert t.count(needle)==1;t=t.replace(needle,'"Triple_Arrow_Dmg",'+needle);needle='if(key.equals("INVIS_CAST_BREAK")) {';assert t.count(needle)==1;t=t.replace(needle,'if(key.equals("Triple_Arrow_Dmg")){CombatClassDamage.parse(values,skill,key,skillFile,defaults,1.0);}\n            else '+needle)
(src/'CombatRules.java').write_text(t,encoding='utf-8');shutil.copyfile(R/'CombatTripleArrow.java',src/'CombatTripleArrow.java')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v={'status':'building','base':pin(jar),'formal_deployment_changed':False,'full_goal_achieved':False}
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',c0,orig/'CombatRules.java')
  with zipfile.ZipFile(jar) as z:assert (c0/'local/lito381/server/CombatRules.class').read_bytes()==z.read('local/lito381/server/CombatRules.class'),'C0 main differs'
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'TripleMethods.java',inputs/'NativeSkillConsumptionMethods.java',inputs/'SolidMethods.java');run(J/'java.exe','-Xverify:all','-cp',tcp,'SolidMethods','nested',jar,c0)
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'TripleMethods','splice',jar,classes);run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(classes)+';'+str(jar),'-d',classes,*src.glob('*.java'));run(J/'java.exe','-Xverify:all','-cp',tcp,'SolidMethods','nested',jar,classes)
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   if p.name in ['CombatRules$1.class','CombatRules$Snapshot.class']:continue
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class' for n in ['ClientSession','CombatRules']},changed
  assert set(fresh)=={'local/lito381/server/CombatTripleArrow'+n+'.class' for n in ['', '$Scope']},fresh
  candidate=D/'candidate-triple-control.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'TripleMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,C0_main_byte_equal=True,inputs=[pin(p) for p in inputs.rglob('*') if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-4500:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
