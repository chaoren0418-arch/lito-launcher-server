from pathlib import Path
import json,hashlib,os,shutil,subprocess
R=Path(__file__).resolve().parent; S=R.parents[2]; B=S/'tools/resource-database/build'; D=R/'build/tests'; D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8')); sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest(); pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');v=dict(status='running',runtime=b['runtime'],base=b['base'],formal_deployment_changed=False)
for name in ['test.log','result.json']:
    p=D/name
    if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in [Path(__file__),R/'ElementalClockProbe.java',R.parent/'elemental-material/ElementalLifecycleProbe.java']:
    q=frozen/(sha(p)+'-'+p.name)
    if not q.exists():shutil.copyfile(p,q)
v['inputs']=[pin(p) for p in frozen.iterdir()]
classes=D/'classes';classes.mkdir(exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[classes,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',
    R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
try:
    with (D/'test.log').open('w',encoding='utf-8') as log:
        def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
        for mode,ref in [('before',b['base']),('after',b['runtime'])]:
            jar=Path(ref['path']);assert sha(jar)==ref['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=D/mode;dest.mkdir(exist_ok=True)
            run(J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,R/'ElementalClockProbe.java')
            run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ElementalClockProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
            row=read(dest/'observations.json');assert row['cases']==8 and row['shortened_lifetimes']==(8 if mode=='before' else 0) and row['locked_writes']==0;v[mode]=row
        assert (D/'before/goldens.txt').read_bytes()==(D/'after/goldens.txt').read_bytes()
        v.update(status='passed',normal_wire_equal=True,outputs=[pin(p) for p in D.glob('*/observations.json')])
except BaseException as e:
    v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace')[-6000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v[k] for k in ['status','before','after','normal_wire_equal']}))
