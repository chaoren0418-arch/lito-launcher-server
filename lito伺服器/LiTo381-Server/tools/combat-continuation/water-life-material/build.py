from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
jar=R.parent/'summon-ring/build/candidate-summon-ring.jar';assert sha(jar)=='68f0f3381731e076d8d726938cf869d9e59f2c6f98347380611188f583aaeaa3'
inputs=D/'inputs';orig=inputs/'original';src=inputs/'sources';refs=inputs/'source-reference'
for p in [inputs,orig,src,refs]:p.mkdir(exist_ok=True)
light=R.parent/'summon-material/build/inputs/sources/NativeLightAction.java';shutil.copyfile(light,orig/light.name)
t=light.read_text('utf-8');needle='&&skill!=73';assert t.count(needle)==1;t=t.replace(needle,needle+'&&skill!=170')
t=t.replace('boolean captureSelf;NativeCastMaterial.Cast material;', 'boolean captureSelf;ClientSession target;NativePlayerCommit.Binding targetBinding;NativeCastMaterial.Cast material;')
needle='byte[] snapshot=plain.clone();batch.outputs.add(()->receiver.nativeSelfOriginalSend(snapshot,note));return true;'
assert t.count(needle)==1
t=t.replace(needle, 'byte[] snapshot=plain.clone();batch.outputs.add(()->{if(batch.target!=null&&(receiver==batch.target||note.startsWith("S_EFFECT Water-Life target="))&&!batch.targetBinding.matches(batch.target))return;receiver.nativeSelfOriginalSend(snapshot,note);});return true;')
position=t.rfind('}');t=t[:position]+(R/'PairMethods.txt').read_text('utf-8')+'\n'+t[position:];(src/light.name).write_text(t,encoding='utf-8')
shutil.copyfile(R/'PairMethods.txt',inputs/'PairMethods.txt')
shutil.copyfile(R/'NativeWaterLifeCastAction.java',src/'NativeWaterLifeCastAction.java')
for p in [R/'build.py',R/'WaterLifeMethods.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java');shutil.copyfile(p,refs/p.name)
asm=[Path(p['path']) for p in read(R.parent/'build/integration-regression/preparation.json')['asm']];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v={'status':'building','base':pin(jar),'formal_deployment_changed':False,'full_goal_achieved':False}
try:
 with (D/'build.log').open('w',encoding='utf-8') as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  c0=D/'c0';c0.mkdir(exist_ok=True);run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',c0,orig/light.name)
  with zipfile.ZipFile(jar) as z:
   for p in c0.rglob('*.class'):assert p.read_bytes()==z.read(p.relative_to(c0).as_posix()),'C0 mismatch '+p.name
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'WaterLifeMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'WaterLifeMethods','splice',jar,classes)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,*src.glob('*.java'))
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in classes.rglob('*.class'):
   n=p.relative_to(classes).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif data!=entries[n]:changed.append(n)
   entries[n]=data
  assert set(changed)=={'local/lito381/server/'+n+'.class' for n in ['ClientSession','NativeLightAction','NativeLightAction$Batch']},changed
  assert fresh==['local/lito381/server/NativeWaterLifeCastAction.class'],fresh
  candidate=D/'candidate-water-life-material.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'WaterLifeMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,added=fresh,C0_byte_equal=True,inputs=[pin(p) for p in inputs.rglob('*') if p.is_file()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-4500:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
