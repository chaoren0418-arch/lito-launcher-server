"""Replay qualified, frozen probes against one pinned later candidate (no rebuild)."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, shutil, subprocess, sys, zipfile

R = Path(__file__).resolve().parent
S = R.parents[1]
B = S / 'tools/resource-database/build'
read = lambda p: json.loads(p.read_text('utf-8'))
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
pin = lambda p: dict(path=str(p.resolve()), sha256=sha(p))
requested_stage = sys.argv[1]
# The polymorph child inherits the same complete predecessor suite as PET.
stage = 'pet-controls' if requested_stage in ('polymorph-material','return-nature-transfer','pet-world-lifetime','npc-physical-size','cancellation-probability','area-sleep-slow','combat-config-origin','call-nature-material','cancellation-battle-runtime') else requested_stage
assert stage in ('negative-material', 'weakness-material', 'ground-material', 'silence-material', 'decay-material', 'world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls')
build = read(R / requested_stage / 'build/build.json')
jar = Path(build['runtime']['path'])
assert sha(jar) == build['runtime']['sha256']
D = R / requested_stage / 'build/composed-replay'
D.mkdir(exist_ok=True)
run = D / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
run.mkdir()
shutil.copyfile(__file__, run / Path(__file__).name)
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
F = B / 'baseline-PC-ingress-efd4-v2'
deps = [R/'elemental-material/build/lifecycle/classes', R/'elemental-material/build/boundaries/classes', R/'build/tests/classes', B/'pc-native-signed-hp-development/signed-probe-fixtures', B/'pc-native-accept-development/fixtures', F, F/'fixture-support.zip', jar, S/'lib/*']
env = dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
specs = [('water-life-material', 'WaterLife', 64, 8, 56), ('protection-material', 'Protection', 104, 8, 56), ('armor-state', 'Armor', 96, 8, 56)]
if stage in ('weakness-material', 'ground-material', 'silence-material', 'decay-material', 'world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('negative-material', 'Negative', 312, 24, 168))
if stage in ('ground-material', 'silence-material', 'decay-material', 'world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('weakness-material', 'Weakness', 208, 16, 112))
if stage in ('silence-material', 'decay-material', 'world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('ground-material', 'Ground', 144, 8, 96))
if stage in ('decay-material', 'world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('silence-material', 'Silence', 104, 8, 56))
if stage in ('world-potion', 'target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('decay-material', 'DecayPotion', 96, 8, 56))
if stage in ('target-negative-material', 'blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('world-potion', 'WorldPotion', 0, 0, 0))
if stage in ('blind-material', 'resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('target-negative-material', 'TargetNegative', 224, 16, 112))
if stage in ('resurrection-ingress', 'poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('blind-material', 'Blind', 208, 16, 112))
if stage in ('poison-publication', 'earth-bind-material', 'pet-controls'):
    specs.append(('resurrection-ingress', 'Resurrection', 256, 8, 48))
if stage in ('earth-bind-material', 'pet-controls'):
    specs.append(('poison-publication', 'PoisonScope', 192, 16, 112))
if stage == 'pet-controls':
    specs.append(('earth-bind-material', 'EarthBind', 112, 8, 56))
if requested_stage in ('return-nature-transfer','pet-world-lifetime','npc-physical-size','cancellation-probability','area-sleep-slow','combat-config-origin','call-nature-material','cancellation-battle-runtime'):
    specs += [('polymorph-material','Polymorph',128,8,56),('return-nature-material','ReturnNature',136,8,36)]
v = dict(status='running', runtime=build['runtime'], driver=pin(run/Path(__file__).name), basis=[], results=[], formal_deployment_changed=False, full_goal_achieved=False)
previous={}
if (D/'current.json').exists():
    current=read(D/'current.json')
    ref=current['receipt']
    assert sha(Path(ref['path']))==ref['sha256']
    old=read(Path(ref['path']))
    if old['runtime']==build['runtime'] and old['driver']['sha256']==v['driver']['sha256']:
        previous={row['probe']:row for row in old['results']}
log = None
try:
    for directory, prefix, material, fault, lifecycle in specs:
        source = R/directory
        probes = [('Material', material, 'tests'), ('Fault', fault, 'boundaries'), ('Lifecycle', lifecycle, 'boundaries')]
        if prefix == 'Resurrection':
            probes += [('Numeric', 48, 'numeric'), ('WorldNpc', 64, 'world-npc'), ('Companion', 80, 'companions'), ('AnswerLifetime', 64, 'answers'), ('AnswerRace', 112, 'answer-races'), ('CompanionAudit', 20, 'companion-audit')]
        if prefix == 'WorldPotion':
            probes = [('', 64, 'tests')]
        if prefix == 'PoisonScope':
            probes += [('WorldNpc', 64, 'world-npcs'), ('Clock', 8, 'clock-audit'), ('Consumer', 8, 'consumer'), ('Timer', 8, 'timers'), ('CurseConsumer', 24, 'curse-consumer')]
        if prefix == 'EarthBind':
            probes += [('WorldNpc', 32, 'world-npcs')]
        if prefix == 'Polymorph':
            probes += [('Menu',80,'menu')]
        if prefix == 'Armor':
            probes += [('StateLock', 12, 'state-locks'), ('Target', 64, 'targets')]
        if prefix == 'Negative':
            probes += [('Companion', 192, 'companions'), ('WorldNpc', 96, 'world-npcs')]
        if prefix in ('Weakness', 'TargetNegative', 'Blind'):
            probes += [('WorldNpc', 64, 'world-npcs')]
        if prefix in ('Silence', 'DecayPotion'):
            probes += [('WorldNpc', 32, 'world-npcs')]
        if prefix == 'Ground':
            probes += [('Clock', 48, 'clock'), ('Placement', 32, 'placement'), ('Numeric', 48, 'numeric')]
        for name, count, folder in probes:
            receipt = source/'build'/folder/'result.json'
            if prefix == 'PoisonScope' and name == 'Consumer':
                receipt = R/'earth-bind-material/build/consumer-completion/result.json'
            original = read(receipt)
            assert original['status'] == 'passed', str(receipt)
            source_jar = Path(original['runtime']['path'])
            assert sha(source_jar) == original['runtime']['sha256']
            v['basis'].append(pin(receipt))
            overlays = [source/'build'/folder/'classes']
            if prefix == 'PoisonScope' and name == 'Consumer':
                overlays = [R/'earth-bind-material/build/consumer-completion/classes']
            if prefix == 'WorldPotion':
                ref = read(source/'build/tests/dependency-freeze.json')['archive']
                assert sha(Path(ref['path'])) == ref['sha256']
                overlays.append(Path(ref['path']))
            if prefix == 'PoisonScope' and name in ('Clock', 'Consumer', 'Timer', 'CurseConsumer'):
                overlays.append(source/'build/boundaries/classes')
                if name == 'CurseConsumer': overlays.insert(1, source/'build/timers/classes')
            if prefix == 'Ground' and name in ('Clock', 'Placement', 'Numeric'):
                overlays.append(source/'build/boundaries/classes')
            if prefix == 'Ground' and name == 'Clock':
                overlays.append(source/'build/boundaries/memory-observer')
                with zipfile.ZipFile(source_jar) as a, zipfile.ZipFile(jar) as b:
                    assert a.read('local/lito381/server/GameStateStore.class') == b.read('local/lito381/server/GameStateStore.class')
            if name == 'Fault':
                overlays.append(source/'build/boundaries/memory-observer')
                with zipfile.ZipFile(source_jar) as a, zipfile.ZipFile(jar) as b:
                    assert a.read('local/lito381/server/GameStateStore.class') == b.read('local/lito381/server/GameStateStore.class')
            if name in ('Target', 'StateLock', 'Companion', 'WorldNpc'):
                overlays.append(source/'build/tests/classes')
            inputs = [pin(p) for d in overlays for p in ([d] if d.is_file() else d.rglob('*.class'))]
            assert inputs
            old=previous.get(prefix+name)
            if old and old['compiled_inputs']==inputs:
                for ref in [old['observations'],old['log']]:
                    assert sha(Path(ref['path']))==ref['sha256']
                v['results'].append(old)
                print('REUSE',prefix+name,'same candidate, driver, compiled inputs and results',flush=True)
                continue
            dest = run/(prefix+name)
            dest.mkdir()
            log = dest/'probe.log'
            cp = ';'.join(map(str, [*overlays, *deps]))
            with log.open('w', encoding='utf-8') as out:
                subprocess.run(list(map(str, [J/'java.exe', '-Xverify:all', '-cp', cp, 'local.lito381.server.'+prefix+name+'Probe', S/'config/server.properties', dest/'runtime', dest/'observations.json', jar, 'after'])), cwd=S, env=env, check=True, stdout=out, stderr=subprocess.STDOUT, timeout=300)
            result = read(dest/'observations.json')
            assert result['cases'] == count
            if prefix == 'WorldPotion':
                assert result['world_heal_gaps'] == 0
            if name == 'StateLock':
                assert result['Source_lock_bypasses'] == 0 and result['old_PC_monitor_blocks'] == 0
            else:
                assert result.get('locked_writes', result.get('locked_write_calls')) == 0
            if name in ('Material', 'Target', 'Companion', 'WorldNpc'):
                assert result['Source_window_bypasses'] == 0
                assert (dest/'goldens.txt').read_bytes() == (source/'build'/folder/'after/goldens.txt').read_bytes()
            if prefix == 'PoisonScope':
                for key in ('early_warning_clocks', 'early_first_ticks'):
                    if key in result: assert result[key] == 0
            if prefix == 'Resurrection':
                for key in ('signed_mp_bound_gaps','stale_native_acceptances','stale_direct_acceptances','stale_companion_acceptances'):
                    if key in result: assert result[key] == 0
            if name == 'Fault':
                assert result['actual_memory_faults'] == fault and result['failed_material_casts_with_partial_commit'] == 0
            v['results'].append(dict(probe=prefix+name, results=result, observations=pin(dest/'observations.json'), log=pin(log), compiled_inputs=inputs))
            print('PASS', prefix+name, count, flush=True)
    v['status'] = 'passed'
except BaseException as error:
    v.update(status='failed', error=str(error))
    if log and log.exists():
        print(log.read_text('utf-8', errors='replace')[-6500:])
    raise
finally:
    (run/'result.json').write_text(json.dumps(v, ensure_ascii=False, indent=2), encoding='utf-8')
    (D/'current.json').write_text(json.dumps(dict(status=v['status'], runtime=build['runtime'], receipt=pin(run/'result.json')), indent=2), encoding='utf-8')
