from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(R/'current.json');assert parent['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df';landing=read(D/'companion-landing-build-current.json');base=Path(landing['runtime']['path']);assert sha(base)==landing['runtime']['sha256']=='b81dbbb52b6f9f9be58ded041b6f876615d495890beb457021f36e54dcf4cfcd';seed=read(Path(read(D/'summon-retirement-build-current.json')['manifest']['path']));run=D/'companion-placement-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_companion_placement.py','CompanionPlacementMethods.java','NativeCompanionPlacement.java']:shutil.copyfile(R/name,inputs/name)
variants=[]
for name in ['NativeSummonGroundTransfer.java','NativeSummonRetirement.java']:
 ref=next(ref for ref in seed['inputs'] if Path(ref['path']).name==name);assert sha(Path(ref['path']))==ref['sha256'];text=Path(ref['path']).read_text('utf-8');old=text
 if name=='NativeSummonGroundTransfer.java':
  key=' static Stage capture(ClientSession owner,BattleCompanion pet){';assert text.count(key)==1;text=text.replace(key,key+'\n  if(owner!=null&&pet!=null&&NativeCompanionPlacement.state(pet)!=null)return NativeCompanionPlacement.ground(owner,pet,null);')
  key=' static Stage captureFallback(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp original,boolean atOwner){';assert text.count(key)==1;text=text.replace(key,key+'\n  if(owner!=null&&pet!=null&&NativeCompanionPlacement.state(pet)!=null)return NativeCompanionPlacement.ground(owner,pet,original);')
 else:
  key='if(ownerReturn&&(original==null||original.master.matches(owner)))';assert text.count(key)==1;text=text.replace(key,'if(ownerReturn&&NativeCompanionPlacement.ownerReturn(owner,pet)&&(original==null||original.master.matches(owner)))')
 assert text!=old;p=inputs/name;p.write_text(text,'utf-8');variants.append(dict(original=ref,variant=pin(p)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=landing['runtime'],integrated_parent=parent['runtime'],asm=seed['asm'],inputs=[pin(p) for p in inputs.iterdir()],variant_sources=variants,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,all_scene_consumers_wired=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  tcp=';'.join(map(str,[run/'tools',*[p['path'] for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'CompanionPlacementMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'CompanionPlacementMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,*[inputs/n for n in ['NativeCompanionPlacement.java','NativeSummonGroundTransfer.java','NativeSummonRetirement.java']]])
  with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()};old=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in old if entries[n]!=old[n]);fresh=sorted(set(entries)-set(old));assert changed==['local/lito381/server/'+n+'.class' for n in ['BattleCompanion','ClientSession','NativeReturnNatureTransfer','NativeSummonGroundTransfer','NativeSummonRetirement']] and fresh==['local/lito381/server/NativeCompanionPlacement'+n+'.class' for n in ['$Removal','$Scene','$State','$Transition','$Viewer','']],(changed,fresh);jar=run/'candidate-companion-placement-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'CompanionPlacementMethods','verify',base,jar])
 v.update(status='built-wired-portal-actor-scene-ownership-all-consumers-unqualified',runtime=pin(jar),changed=changed,fresh=fresh,new_player_fields_workers_schedulers_ThreadLocals_global_registries=0)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'companion-placement-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],changed=v['changed'],fresh=v['fresh'])))
