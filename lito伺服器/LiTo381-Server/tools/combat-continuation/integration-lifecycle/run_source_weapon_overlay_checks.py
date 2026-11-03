"""Current-candidate source weapon coverage plus a separate, reversible test-only state-save fault JAR."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,re,shutil,subprocess,sys,zipfile

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref,ref['path'];return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build_mode=len(sys.argv)>1 and sys.argv[1].startswith("build:")
replay=len(sys.argv)>1 and not build_mode
if replay:
 seed=read(sys.argv[1]);S=Path(seed['server_root']);base=checked(seed['runtime']);basis=seed['basis'];build_ref=seed['candidate_build'];run=Path(sys.argv[2]);stage_source=R
else:
 index=read(D/((sys.argv[1][6:]+'-current.json')if build_mode else 'source-program-update-progress-current.json'));checked(index['manifest']);base=checked(index['runtime']);basis=index['manifest'];progress=read(index['manifest']['path']);build_ref=index['manifest']if build_mode else progress['build'];stage_source=R
 run=D/'source-weapon-overlay-checks'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
assert pin(base)['sha256']in {'30e222344d776e5cd0088bed8b6334736c22f403af5912c9f1222ce56d24094e','f6337e684a9fd386e8d4a5d7711a17ce9d463d6a20dedcad70f8317f17cb6fad','40cbc3691c2974af52f7c14cf8c4b81abb9e5de8218495c84480a4511ef456b0','2d05b7bacebbbb95cb65f2fdffa4b78fdff7acb6c4bb9899fafe5300abde1d3f','06f09c15900a31e0f7697f86437236634795783405cfdd234f996087a10b446d','751ebc72c7f17cdaa6e5efc599df4961e619a0eaf39a7f83bae545b345c52589','07a334d7aa8da829af7e4b5543ac12e305b07541706e2b9ae254741766397d9b','c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6','e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a','bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2','50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729','3ccbb6f5957436cc0ba9bd67d373f9dfcbedad16553009842eaf5db6d0e88e9a'}
checked(build_ref);build=read(build_ref['path']);inputs=run/'inputs';inputs.mkdir(parents=True)
names=['SourceWeaponOverlayPlan.java','SourceWeaponRollbackPlan.java','WeaponStateFault.java','SourceWeaponStateFaultMethods.java','run_source_weapon_overlay_checks.py']
for name in names:shutil.copyfile(stage_source/name,inputs/name)
for name in ['SourceWeaponDurabilityOracle.java','SourceWeaponCounterMagicOracle.java']:
 ref=next(x for x in build['inputs']if Path(x['path']).name==name);checked(ref);shutil.copyfile(ref['path'],inputs/name)
for ref in build['asm']:checked(ref)
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
parent_plan=base.parent/'plan';parent_tools=base.parent/'tools';argfiles=[]
v=dict(status='running',server_root=str(S),runtime=pin(base),basis=basis,candidate_build=build_ref,inputs=[pin(p)for p in inputs.iterdir()],asm=build['asm'],new_SQL_connections=0,real_server_started=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with (run/'checks.log').open('w',encoding='utf-8')as log:
  def invoke(args):
   args=list(map(str,args))
   if Path(args[0]).name=='javac.exe':
    p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args[1:])+'\n','utf-8');argfiles.append(p);args=[args[0],'@'+str(p)]
   subprocess.run(args,cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
  cp=';'.join(map(str,[base,parent_plan,S/'lib/*']))
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',run/'plan',inputs/'SourceWeaponOverlayPlan.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',str(run/'plan')+';'+cp,'local.lito381.server.SourceWeaponOverlayPlan'])
  toolcp=';'.join(map(str,[run/'tools',parent_tools,*[x['path']for x in build['asm']]]))
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',toolcp,'-d',run/'tools',inputs/'SourceWeaponStateFaultMethods.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',toolcp,'SourceWeaponStateFaultMethods','splice',base,run/'test-classes',S/'lib'])
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',str(base)+';'+str(S/'lib/*'),'-d',run/'test-classes',inputs/'WeaponStateFault.java'])
  with zipfile.ZipFile(base)as z:
   assert 'local/lito381/server/WeaponStateFault.class'not in z.namelist()
   entries={n:z.read(n)for n in z.namelist()}
  for p in (run/'test-classes').rglob('*.class'):entries[p.relative_to(run/'test-classes').as_posix()]=p.read_bytes()
  test=run/'TEST-ONLY-state-save-fault.jar'
  with zipfile.ZipFile(test,'w')as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  v['test_only_fault_runtime']=pin(test);assert v['runtime']['sha256']!=v['test_only_fault_runtime']['sha256']
  invoke([J/'java.exe','-Xverify:all','-cp',toolcp,'SourceWeaponStateFaultMethods','verify',base,test,S/'lib'])
  testcp=';'.join(map(str,[test,run/'plan',parent_plan,S/'lib/*']))
  invoke([J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',testcp,'-d',run/'fault-plan',inputs/'SourceWeaponRollbackPlan.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',str(run/'fault-plan')+';'+testcp,'local.lito381.server.SourceWeaponRollbackPlan'])
 text=(run/'checks.log').read_text('utf-8')
 v.update(status='passed-current-SHA-source-overlay-and-separate-test-SHA-original-save-rollback-checks',ordinary_same_SHA_checks=int(re.search(r'PASS(\d+) source-equipped weapon',text)[1]),ordinary_native_casts=int(re.search(r'original-casts=(\d+)',text)[1]),ordinary_encrypted_memory_frames=int(re.search(r'encrypted-memory-frames=(\d+)',text)[1]),test_only_rollback_checks=int(re.search(r'PASS(\d+) TEST-JAR original-material',text)[1]),test_only_rollback_attempts=int(re.search(r'original-attempts=(\d+)',text)[1]),test_only_original_state_methods_inverted=int(re.search(r'state fault inverse: (\d+) original methods',text)[1]),candidate_runtime_unchanged=pin(base)==v['runtime'],test_fault_class_absent_from_candidate=True,ordinary_checks_are_candidate_SHA_and_fault_checks_are_separate_SHA=True,remaining='Original physical consumers, complete accepted/final lifecycle, all other materials/Source, backend/Main/memory/MySQL/game and formal composition remain open.')
 assert v['candidate_runtime_unchanged']
except BaseException as error:
 v.update(status='failed',error=str(error));print((run/'checks.log').read_text('utf-8',errors='replace')[-4200:]);raise
finally:
 v['log']=pin(run/'checks.log');v['javac_arguments']=[pin(p)for p in argfiles];write(run/'manifest.json',v)
 if not replay:write(D/'source-weapon-overlay-checks-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),ordinary_same_SHA_checks=v['ordinary_same_SHA_checks'],test_only_rollback_checks=v['test_only_rollback_checks'])))
