from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(R/'current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='7fcbc7741654f59f0a3e8c47f586595fe9ad8301defc09378d774d160482f2b1'
checkpoint=read(Path(parent['manifest']['path']));index=read(Path(checkpoint['receipts']['weapon-target-build-current']['path']));prior=read(Path(index['manifest']['path']));assert index['runtime']==parent['runtime'];old=next(Path(p['path']) for p in prior['frozen_inputs'] if p['path'].endswith('NativeWeaponBreakTargetLifecycle.java'))
run=D/'weapon-output-identity-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_weapon_output_identity.py','NativeWeaponBreakTargetLifecycle.java','WeaponOutputDelta.java']:shutil.copyfile(R/name,inputs/name)
c0=run/'original';c0.mkdir();shutil.copyfile(old,c0/old.name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',parent=pin(R/'current.json'),base=parent['runtime'],frozen_inputs=[pin(p) for p in inputs.iterdir()],original_helper=pin(c0/old.name),asm=prior['asm'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  compiler=[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[jar,S/'lib/*']))]
  invoke([*compiler,'-d',run/'C0',c0/old.name]);classes=run/'classes';invoke([*compiler,'-d',classes,inputs/'NativeWeaponBreakTargetLifecycle.java'])
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  helper='local/lito381/server/NativeWeaponBreakTargetLifecycle.class';assert entries[helper]==(run/'C0'/helper).read_bytes(),'parent helper C0 differs'
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if entries[n]!=before[n]);fresh=sorted(set(entries)-set(before));assert changed==[helper] and fresh==[],(changed,fresh)
  candidate=run/'candidate-integrated-battle-weapon-companion-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[p['path'] for p in prior['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'WeaponOutputDelta.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'WeaponOutputDelta',jar,candidate])
 v.update(status='built-BATTLE-weapon-companion-output-identity-targeted-proof-pending',runtime=pin(candidate),changed=changed,fresh=fresh,all_other_archive_entries_byte_identical=True,production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'weapon-target-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
