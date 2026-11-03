from pathlib import Path
import hashlib,json,os,shutil,subprocess,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
parent=read(B/'native-item-weapon-material-progress-current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='df9464f06c41821f3abf7c29ccc5876abe1ead66e570a40308f1274e90521a37'
manifest=read(Path(read(B/'de-native-item-weapon-clock-development/build-current.json')['manifest']['path']))
inputs=D/'inputs';inputs.mkdir(exist_ok=True);sources=inputs/'sources';sources.mkdir(exist_ok=True);original=inputs/'original';original.mkdir(exist_ok=True)
light=next(Path(r['path']) for r in manifest['frozen_inputs'] if r['path'].endswith('sources\\NativeLightAction.java') and 'original-sources' not in r['path'])
cube=read(Path(read(B/'de-native-cube-material-development/build-current.json')['manifest']['path']))
material=next(Path(r['path']) for r in cube['frozen_inputs'] if r['path'].endswith('sources\\NativeCastMaterial.java') and 'original-sources' not in r['path'])
for p in [material,light]:shutil.copyfile(p,original/p.name)
for p in [R/'build.py',R/'MagicalWeaponMethods.java',R/'NativeMagicalWeaponAction.java',B/'NativeSkillConsumptionMethods.java']:shutil.copyfile(p,inputs/p.name)
t=(original/'NativeLightAction.java').read_text('utf-8');needle='&&!NativeItemWeaponCastAction.supported(skill)';assert t.count(needle)==1;t=t.replace(needle,needle+'&&skill!=73');(sources/'NativeLightAction.java').write_text(t,encoding='utf-8')
t=(original/'NativeCastMaterial.java').read_text('utf-8')
t=t.replace('static final class Cast {final int skill;','interface Effect {boolean apply(RuntimeInventory staged)throws IOException;}\n static final class Cast {Effect effect;boolean effectResult;final int skill;',1)
needle='cast.resolved=true;cast.bypass=true;var wire=NativeDeItemTransaction.wireCopy(pc.nativeDeInventory(0));cast.before=wire;cast.after=wire;return true;'
assert t.count(needle)==1;t=t.replace(needle,'cast.bypass=true;if(cast.effect==null){cast.resolved=true;var wire=NativeDeItemTransaction.wireCopy(pc.nativeDeInventory(0));cast.before=wire;cast.after=wire;return true;}')
t=t.replace('if(count<=0)return reject(pc);','if(!cast.bypass&&count<=0)return reject(pc);',1)
a=t.index('     var plan=NativeSourceMaterialPlan.plan(');z=t.index('\n     if(next.nativeDeItems()',a)
t=t[:a]+'''     var next=live.copy();cast.staged=next;
     if(!cast.bypass){var plan=NativeSourceMaterialPlan.plan(views,(int)pc.playerObjectId(),item,count);if(!plan.allowed())return false;
      for(var debit:plan.debits()){var value=next.item(debit.objectId());if(value==null||value.quantity()!=debit.oldCount()||!next.remove(debit.objectId(),debit.count()))throw new IOException("material canonical plan changed");}}
     if(cast.effect!=null)cast.effectResult=cast.effect.apply(next);'''+t[z:]
(sources/'NativeCastMaterial.java').write_text(t,encoding='utf-8');shutil.copyfile(R/'NativeMagicalWeaponAction.java',sources/'NativeMagicalWeaponAction.java')
asm=[Path(x['path']) for x in manifest['asm']];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');log=D/'build.log';v={'status':'building','base':pin(jar),'formal_deployment_changed':False,'full_goal_achieved':False}
try:
 with log.open('w',encoding='utf-8') as out:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=120)
  tool=D/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,*asm]));run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,inputs/'MagicalWeaponMethods.java',inputs/'NativeSkillConsumptionMethods.java')
  c0=D/'c0';c0.mkdir(exist_ok=True);run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(jar)+';'+str(S/'lib/*'),'-d',c0,*original.glob('*.java'))
  with zipfile.ZipFile(jar) as z:
   for p in c0.rglob('*.class'):assert p.read_bytes()==z.read(p.relative_to(c0).as_posix()),'C0 differs: '+p.name
  output=D/'classes';output.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',tcp,'MagicalWeaponMethods','splice',jar,output)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',';'.join(map(str,[output,jar,S/'lib/*'])),'-d',output,*sources.glob('*.java'))
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()}
  changed=[];fresh=[]
  for p in output.rglob('*.class'):
   n=p.relative_to(output).as_posix();data=p.read_bytes()
   if n not in entries:fresh.append(n)
   elif entries[n]!=data:changed.append(n)
   entries[n]=data
  allowed={'ClientSession','NativeLightAction','NativeCastMaterial','NativeCastMaterial$Cast'}
  assert set(changed)=={'local/lito381/server/'+n+'.class' for n in allowed},changed
  assert set(fresh)=={'local/lito381/server/'+n+'.class' for n in ['NativeMagicalWeaponAction','NativeCastMaterial$Effect']},fresh
  candidate=D/'candidate-magical-weapon.jar'
  with zipfile.ZipFile(candidate,'w') as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  run(J/'java.exe','-Xverify:all','-cp',tcp,'MagicalWeaponMethods','verify',jar,candidate)
 v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,fresh=fresh,C0_byte_equal=True,inputs=[pin(p) for p in sorted(inputs.rglob('*')) if p.is_file()])
except BaseException as e:
 v.update(status='failed',error=str(e));print(log.read_text('utf-8',errors='replace')[-5500:]);raise
finally:
 (D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
