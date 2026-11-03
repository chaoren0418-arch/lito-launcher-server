from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/choices';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
source=R/'build/inputs/SUMMON_MONSTER.java';text=source.read_text('utf-8');columns=[]
for name in ['summonstr_list','summonid_list','summonlvl_list','summoncha_list']:
    block=re.findall(r'\b'+name+r'\s*=\s*new\s+(?:String|int)\[\]\s*\{([^}]+)\}',text)
    assert len(block)==1
    column=[int(x) for x in re.findall(r'\d+',block[0])];assert len(column)==31;columns.append(column)
table=D/'source-choices.tsv';table.write_text(''.join('\t'.join(map(str,row))+'\n' for row in zip(*columns)),encoding='utf-8')
frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in [Path(__file__),R/'SummonChoicesProbe.java',R/'SummonMaterialProbe.java',source,table]:
    q=frozen/(sha(p)+'-'+p.name)
    if not q.exists():shutil.copyfile(p,q)
for name in ['test.log','result.json']:
    p=D/name
    if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
classes=D/'classes';classes.mkdir(exist_ok=True);F=B/'baseline-PC-ingress-efd4-v2';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=';'.join(map(str,[classes,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],inputs=[pin(p) for p in frozen.iterdir()],formal_deployment_changed=False)
try:
    with (D/'test.log').open('w',encoding='utf-8') as log:
        def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
        run(J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,R/'SummonMaterialProbe.java',R/'SummonChoicesProbe.java')
        if '--compile-only' in sys.argv:v['status']='compiled-only-not-qualified'
        else:
            run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SummonChoicesProbe',S/'config/server.properties',D/'runtime',D/'observations.json',jar,table)
            row=read(D/'observations.json');assert row['cases']==62 and row['locked_writes']==0
            v.update(status='passed',observed=row,output=pin(D/'observations.json'))
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace')[-6000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v[k] for k in ['status','runtime']}))
