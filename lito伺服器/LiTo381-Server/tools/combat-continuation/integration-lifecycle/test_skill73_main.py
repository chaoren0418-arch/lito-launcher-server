from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, subprocess, shutil, os, zipfile, sys

S=Path(__file__).resolve().parents[3]; B=S/'tools/resource-database/build'
D=Path(__file__).resolve().parent/'build';Q=B/'de-native-magical-weapon-material-development'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
closed='--tamed-close' in sys.argv;weapon='--weapon' in sys.argv;summon='--summon-owner' in sys.argv;zombie='--create-zombie' in sys.argv;retirement='--summon-retirement' in sys.argv;combat='--summon-combat' in sys.argv;terminal='--summon-terminal' in sys.argv;grouped='--summon-group' in sys.argv;prefix='on-summon-retirement-' if retirement else 'on-summon-combat-' if combat else 'on-summon-terminal-' if terminal else 'on-summon-group-' if grouped else 'on-create-zombie-' if zombie else 'on-summon-owner-' if summon else 'on-close-' if closed else 'on-weapon-' if weapon else 'on-static-' if '--static' in sys.argv else '';build=read(D/('summon-retirement-build-current.json' if retirement else 'summon-combat-build-current.json' if combat else 'summon-terminal-build-current.json' if terminal else 'summon-group-build-current.json' if grouped else 'create-zombie-build-current.json' if zombie else 'summon-owner-build-current.json' if summon else 'tamed-close-build-current.json' if closed else 'weapon-target-build-current.json' if weapon else 'static-motion-build-current.json' if prefix else 'pair-build-current.json')); jar=Path(build['runtime']['path'])
parent=read(B/'native-item-weapon-material-progress-current.json'); old=Path(parent['runtime']['path'])
assert sha(jar)==build['runtime']['sha256'] and sha(old)==parent['runtime']['sha256']
manifest=read(Path(build['manifest']['path']))
review=read(D/(prefix+'source-drift-review-current.json')); review_path=Path(review['manifest']['path'])
assert sha(review_path)==review['manifest']['sha256'] and review['runtime']==build['runtime']
reviewed=read(review_path)
assert reviewed['status']=='passed-reviewed-current-Source-material-selector-and-role-policy'
for row in reviewed['Source_inputs']:
    assert sha(Path(row['original']['path']))==row['original']['sha256'] and sha(Path(row['frozen']['path']))==row['frozen']['sha256'], row
run=D/(prefix+'skill73-regression-Main-probes')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
inputs=run/'inputs'; classes=run/'classes'; inputs.mkdir(parents=True); classes.mkdir()
names=['NativeMagicalWeaponMainProbe.java','NativeMagicalWeaponMaterialProbe.java','NativeCastMaterialProbe.java','NativeApplicationMainProbe.java','NativeLightCastProbe.java','NativeLightStoppedProbe.java','NativeLightBoundaryProbe.java','SourceActionPairProbe.java']
for p in [Path(__file__).resolve(),*[B/n for n in names],B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java',B/'NativeSkillConsumptionMethods.java']:
    shutil.copyfile(p,inputs/p.name)
fixture=inputs/'NativeMagicalWeaponMainProbe.java';text=fixture.read_text('utf-8');assert text.count('NativeMagicalWeaponCastAction.class')==1;fixture.write_text(text.replace('NativeMagicalWeaponCastAction.class','NativeMagicalWeaponAction.class'),encoding='utf-8')
verifier=inputs/'VerifyNativeMagicalWeaponMainMemoryFixture.java'
verifier.write_text((B/'VerifyNativeCubeMainMemoryFixture.java').read_text('utf-8').replace('VerifyNativeCubeMainMemoryFixture','VerifyNativeMagicalWeaponMainMemoryFixture'),encoding='utf-8')
p=inputs/'NativeApplicationMainProbe.java'; text=p.read_text('utf-8'); original=sha(p)
needle='new Class[]{AccountRepository.class}'; assert text.count(needle)==1
text=text.replace(needle,'new Class[]{AccountRepository.class,NativeCastMaterialPolicy.Source.class}')
needle='if(method.getName().equals("close"))'; assert text.count(needle)==1
text=text.replace(needle,'if(method.getName().equals("characterAccessLevel"))return NativeMagicalWeaponMainProbe.policy((String)args[0],((Number)args[1]).longValue(),(String)args[2]);\n  '+needle)
p.write_text(text,encoding='utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'); F=B/'baseline-PC-ingress-efd4-v2'
deps=[B/'pc-native-application-development/main-probe-fixtures',B/'pc-native-joint-lifetime-development/fixtures',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
cp=';'.join(map(str,[classes,*deps,jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
log=run/'probe.log'; results=[]; decoded=[]
v=dict(status='running',runtime=build['runtime'],parent=parent['runtime'],build=build['manifest'],Source_drift_review=pin(review_path),frozen_inputs=[pin(p) for p in inputs.iterdir()],fixture_repository_edge=dict(original_sha256=original,adapted=pin(p),only_change='typed memory selected-role policy with bounded latch, role levels0/200, injected read failure or role retirement; no SQL'),production_PC_overlays=0,Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False,full_goal_achieved=False)
try:
    with log.open('w',encoding='utf-8') as out:
        def invoke(args): subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
        invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[inputs/n for n in [*names,'SourceSkillConsumptionReference.java','L1SkillId.java']]])
        prior=read(B/'native-de-item-transaction-current.json')['evidence']['main-probe']; qualified=read(Path(prior['path']))
        overlay=run/'main-memory-overlay'; overlay.mkdir(); overlays=[]
        for row in qualified['overlays']:
            source=Path(row['frozen']['path']); assert sha(source)==row['frozen']['sha256']
            target=overlay/Path(*source.parts[-4:]); target.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,target); overlays.append(pin(target))
        with zipfile.ZipFile(old) as a, zipfile.ZipFile(jar) as b:
            for name in ['Main','SourceLogin']: assert a.read('local/lito381/server/'+name+'.class')==b.read('local/lito381/server/'+name+'.class')
        tools=run/'inverse-tools'; tools.mkdir(); tcp=';'.join(map(str,[tools,*[r['path'] for r in manifest['asm']]]))
        invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',tools,verifier,inputs/'NativeSkillConsumptionMethods.java'])
        invoke([J/'java.exe','-Xverify:all','-cp',tcp,'VerifyNativeMagicalWeaponMainMemoryFixture',jar,overlay])
        for scenario in ['regular','GM','policy-failure','role-retirement']:
            for mode in ['normal','interrupt']:
                branch=run/('after-'+scenario+'-'+mode); branch.mkdir(); observation=branch/'observations.json'
                runtimecp=';'.join(map(str,[classes,overlay,*deps,jar,S/'lib/*']))
                invoke([J/'java.exe','-Xverify:all','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(S.parents[1]/'lito登入器/LiTo381-Launcher/app/common_catalog.bin'),'-cp',runtimecp,'local.lito381.server.NativeMagicalWeaponMainProbe',S/'config/server.properties',branch/'runtime',observation,mode,jar,73,'after',overlay,scenario])
                result=read(observation)
                assert result['repository_close_attempts']==1 and result['repository_calls_after_close']==0 and result['policy_reads']==1
                assert result['expected_commit']==(scenario in ['regular','GM'])
                assert result['actual_frames']>0 if scenario!='role-retirement' else result['actual_frames']==0
                wire=branch/'magical-weapon-Main-wire.txt'; count=0
                for line in wire.read_text('utf-8').splitlines():
                    if not line.strip(): continue
                    seed,plain,frame=line.split(':'); key=int(seed).to_bytes(8,'little'); raw=bytes.fromhex(frame)
                    assert int.from_bytes(raw[:2],'little')==len(raw); cipher=raw[2:]
                    actual=bytes([cipher[0]^key[0]]+[cipher[i]^cipher[i-1]^key[i&7] for i in range(1,len(cipher))])
                    assert actual==bytes.fromhex(plain)
                    decoded.append(dict(stage='after',skill=73,mode=mode,scenario=scenario,plain=plain,frame_sha256=hashlib.sha256(raw).hexdigest())); count+=1
                assert count==result['actual_frames']
                results.append(dict(stage='after',skill=73,mode=mode,scenario=scenario,runtime=pin(jar),observations=pin(observation),results=result,wire=pin(wire)))
                print(json.dumps(dict(result,scenario=scenario,mode=mode)),flush=True)
    v.update(status='passed-real-Main-skill73-normal-interrupt-GM-policy-failure-role-retirement',results=results,independently_decrypted=decoded,overlays=overlays,qualified_overlay=prior,actual_Main_and_SourceLogin_inverse_verified=True)
except BaseException as error:
    v.update(status='failed',error=str(error),results=results); print(log.read_text('utf-8',errors='replace')[-12000:]); raise
finally:
    v.update(log=pin(log),compiled_fixture=[pin(p) for p in classes.rglob('*.class')])
    write(run/'manifest.json',v); write(D/(prefix+'skill73-Main-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
