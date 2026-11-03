from pathlib import Path
import json,hashlib,shutil,subprocess,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'build/candidate-combat-buff.jar';assert sha(jar)=='97b1318aa4ab310c367ace9c3a5e5f197a899ce5875caf451e2bfa3010f50dfe'
manifest=read(R.parent/'build/integration-regression/preparation.json');asm=[Path(x['path']) for x in manifest['asm']]
inputs=D/'inputs';inputs.mkdir(exist_ok=True);orig=inputs/'original';orig.mkdir(exist_ok=True);src=inputs/'sources';src.mkdir(exist_ok=True)
source_inputs=inputs/'source-reference';source_inputs.mkdir(exist_ok=True)
for relative,expected in [('config/ConfigSkill.java','ffc81f59cdd57d18371538b92ab0d6b591009fbb37355be86737adbc414e7d7e'),('server/model/skill/skillmode/SOLID_CARRIAGE.java','56675b9adaa7e3f72df8df9331d353551b5b98c433dfc46fea820cb30bbd5d89')]:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage')/relative;assert sha(p)==expected;shutil.copyfile(p,source_inputs/p.name)
config=(source_inputs/'ConfigSkill.java').read_text('utf-8');a=config.index('private static int _getInt(');z=config.index('private static double _getDouble(',a);parser=config[a:z]
mode=(source_inputs/'SOLID_CARRIAGE.java').read_text('utf-8');a=mode.index('final boolean canUse = ')+len('final boolean canUse = ');z=mode.index(';',a);condition=mode[a:z].replace('ConfigSkill.SOLID_CARRIAGE_MODE','mode')
(source_inputs/'SourceSolidReference.java').write_text('package local.lito381.server; import java.util.Properties; final class SourceSolidReference {\n'+parser+'\n static int mode(Properties p){return _getInt(p,"SOLID_CARRIAGE_MODE",1);}\n static boolean eligible(boolean hasShield,boolean hasArm,int mode){return '+condition+';}\n}',encoding='utf-8')
for p in [R/'SolidMethods.java',R/'SolidCarriageRules.java',R/'build.py',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
shutil.copyfile(S/'src/main/java/local/lito381/server/CombatRules.java',orig/'CombatRules.java')
t=(orig/'CombatRules.java').read_text('utf-8');needle='"NATURES_BLESSING_HEAL","ILLUSION_AVATAR_DAMAGE_RATE"';assert t.count(needle)==1;t=t.replace(needle,'"SOLID_CARRIAGE_MODE",'+needle)
needle='if(key.equals("NATURES_BLESSING_HEAL")) {';assert t.count(needle)==1
t=t.replace(needle,'''if(key.equals("SOLID_CARRIAGE_MODE")) {
                int mode=1;String raw=skill.getProperty(key);
                try {if(raw==null||raw.isBlank())throw new NumberFormatException();mode=Integer.parseInt(raw.trim());}
                catch(NumberFormatException absentOrMalformed){defaults.add(key);}
                values.put(key,(double)mode);
            }
            else '''+needle)
(src/'CombatRules.java').write_text(t,encoding='utf-8');shutil.copyfile(R/'SolidCarriageRules.java',src/'SolidCarriageRules.java')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v={'status':'building','base':pin(jar),'formal_deployment_changed':False,'full_goal_achieved':False}
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True)
  run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',jar,'-d',c0,orig/'CombatRules.java')
  with zipfile.ZipFile(jar) as z:
   for p in c0.rglob('*.class'):assert p.read_bytes()==z.read(p.relative_to(c0).as_posix()),'C0 differs '+p.name
  tools=D/'tools';tools.mkdir(exist_ok=True);tcp=';'.join(map(str,[tools,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'SolidMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'SolidMethods','splice',jar,classes)
  run(J/'javac.exe','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(classes)+';'+str(jar),'-d',classes,*src.glob('*.java'))
  run(J/'java.exe','-Xverify:all','-cp',tcp,'SolidMethods','nested',jar,classes)
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   if p.name in ['CombatRules$1.class','CombatRules$Snapshot.class']:continue
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class' for n in ['ClientSession','SourceEquipmentPhysicalStats','CombatRules']},changed
  assert fresh==['local/lito381/server/SolidCarriageRules.class'],fresh
  candidate=D/'candidate-solid-control.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'SolidMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,C0_byte_equal=True,inputs=[pin(p) for p in inputs.rglob('*') if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-4000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
