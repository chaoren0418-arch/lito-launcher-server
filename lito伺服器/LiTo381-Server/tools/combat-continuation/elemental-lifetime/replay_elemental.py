"""Rerun only the affected elemental probes against the exact final clock candidate."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent; S=R.parents[2]; B=S/'tools/resource-database/build'; E=R.parent/'elemental-material'; D=R/'build/elemental-regression'; D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8')); sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest(); pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';classes=D/'classes';classes.mkdir(exist_ok=True)
deps=[classes,R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*'];cp=';'.join(map(str,deps))
sources=[*E.glob('Elemental*Probe.java'),E/'build/boundaries/DeItemTransactionProbe.java',B/'NativeCastMaterialProbe.java',B/'NativeLightCastProbe.java',B/'NativeLightStoppedProbe.java',B/'SourceActionPairProbe.java',B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java']
frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in [*sources,Path(__file__)]:
    q=frozen/(sha(p)+'-'+p.name)
    if not q.exists():shutil.copyfile(p,q)
inputs=[pin(p) for p in sources]
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
overlay=E/'build/boundaries/memory-observer'
with zipfile.ZipFile(Path(b['base']['path'])) as a,zipfile.ZipFile(jar) as z:
    assert a.read('local/lito381/server/GameStateStore.class')==z.read('local/lito381/server/GameStateStore.class')
observer=pin(overlay/'local/lito381/server/GameStateStore.class')
rows=[];v=dict(status='running',runtime=b['runtime'],inputs=inputs,observer=observer,formal_deployment_changed=False,full_goal_achieved=False)
try:
    with (D/'compile.log').open('w',encoding='utf-8') as log:
        subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sources])),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
    for kind,cases in [('Material',80),('Fault',16),('Stopped',16),('Edges',64),('Lifecycle',44)]:
        branch=D/kind;branch.mkdir(exist_ok=True);receipt=branch/'receipt.json';log=branch/'test.log';output=branch/'observations.json'
        if receipt.exists():
            prior=read(receipt)
            if prior['runtime']==b['runtime'] and prior['inputs']==inputs and prior['status']=='passed' and prior['observer']==observer and all(sha(Path(prior[k]['path']))==prior[k]['sha256'] for k in ['log','output']):
                rows.append(prior);print('REUSE verified',kind,flush=True);continue
        for p in [receipt,log,output]:
            if p.exists():shutil.copyfile(p,branch/('previous-'+sha(p)[:12]+'-'+p.name))
        run_cp=cp if kind!='Fault' else str(classes)+';'+str(overlay)+';'+cp.removeprefix(str(classes)+';')
        print('RUN exact elemental',kind,flush=True)
        with log.open('w',encoding='utf-8') as out:
            rc=subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',run_cp,'local.lito381.server.Elemental'+kind+'Probe',S/'config/server.properties',branch/'runtime',output,jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=240).returncode
        if rc:raise RuntimeError(kind+' failed, see '+str(log))
        observed=read(output);assert observed['cases']==cases
        assert observed.get('locked_write_calls',observed.get('locked_writes'))==0
        if kind=='Material':
            assert observed['Source_window_bypasses']==0
            assert (branch/'self-material-goldens.txt').read_bytes()==(E/'build/tests/before/self-material-goldens.txt').read_bytes()
        if kind=='Fault':assert observed['actual_memory_faults']==16 and observed['failed_material_casts_with_partial_commit']==0
        row=dict(status='passed',kind=kind,runtime=b['runtime'],inputs=inputs,observer=observer,log=pin(log),output=pin(output),observed=observed)
        receipt.write_text(json.dumps(row,ensure_ascii=False,indent=2),encoding='utf-8');rows.append(row)
    v.update(status='passed',results=rows)
except BaseException as e:v.update(status='failed',error=str(e),results=rows);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS exact elemental candidate affected probes')
