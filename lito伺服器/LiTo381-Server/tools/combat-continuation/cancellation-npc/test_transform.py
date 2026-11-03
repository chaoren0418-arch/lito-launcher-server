from pathlib import Path
import hashlib,json,subprocess,os,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/transform-plan';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];F=B/'baseline-PC-ingress-efd4-v2'
cp=';'.join(map(str,[classes,F,F/'fixture-support.zip',jar,S/'lib/*']));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
v=dict(status='running',runtime=b['runtime'],formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
try:
 sources=[R/'CancellationNpcTransformProbe.java']
 reference=Path(r'D:\lineage開發\1-伺服器\src\com\lineage\server\model\skill\skillmode\CANCELLATION.java')
 text=reference.read_text('utf-8');start=text.index('\tprivate static boolean applyNpcTransform(');end=text.index('\n\t@Override',start)
 translated=text[start:end].replace('private static boolean applyNpcTransform(final L1NpcInstance npc, final TransformRule rule)','static boolean apply(final Target npc, final NativeCancellationNpcRules.Rule rule, final Spawn spawn)')
 for old,new in [('rule.transformType()','rule.type()'),('rule.newPassispeed()','rule.passispeed()'),('rule.newNameId()','rule.nameId()'),('CancellationNpcTransformTable.CHANGE_GFX','1'),('CancellationNpcTransformTable.RESPAWN_NPC','2'),('final L1NpcInstance replacement = L1SpawnUtil.spawnT(','final Target replacement = spawn.create('),('new S_ChangeName(','new Name('),('new S_ChangeShape(npc, rule.targetId())','new Shape(npc.getId(), rule.targetId())')]:translated=translated.replace(old,new)
 actual=(R/'NativeCancellationNpcTransform.java').read_text('utf-8');a=actual.index('\tstatic boolean apply(');z=actual.index('\n private NativeCancellationNpcTransform()',a)
 assert actual[a:z].strip()==translated.strip(),'Source transform semantics drifted outside explicit type/port substitutions'
 v['source_reference']=pin(reference);v['source_transform_exact_port']=True

 for p in [*sources,Path(__file__),reference,R/'NativeCancellationNpcTransform.java']:
  q=frozen/(sha(p)+'-'+p.name)
  if not q.exists():shutil.copyfile(p,q)
 v['inputs']=[pin(p)for p in frozen.iterdir()]
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,*sources)
  if '--compile-only'not in sys.argv:run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CancellationNpcTransformProbe',D/'observations.json',jar)
 if '--compile-only'in sys.argv:v['status']='compiled-only-not-qualified'
 else:
  obs=read(D/'observations.json');assert obs['cases']==9;v.update(status='passed-source-transform-port-sequencing',results=obs,observation=pin(D/'observations.json'))
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-6000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
