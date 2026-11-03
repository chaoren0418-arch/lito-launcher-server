"""Same formal SQL startup on unused test ports while the user keeps .28 open."""
import types, json, time
from pathlib import Path
ROOT=Path(__file__).resolve().parent
source=(ROOT/'Start-Shared-Formal.py').read_text(encoding='utf-8').replace('32000','32020')
marker=source.index('                    required=')
source=source[:marker]+source[marker:].replace('lito_play_20261011','lito_roomtest_20261011')
module=types.ModuleType('timing_formal');module.__file__=str(ROOT/'Start-Shared-Formal.py')
exec(compile(source,module.__file__,'exec'),module.__dict__)
original_prepare=module.prepare
def prepare(run_id):
    run,configs=original_prepare(run_id)
    for config in configs:
        path=Path(config['config']['path'])
        values=module.props(path.read_bytes())
        for key,value in list(values.items()):
            values[key]=value.replace('32000','32020').replace('lito_play_20261011','lito_roomtest_20261011')
        sources=run/'config/source-servers.properties'
        sources.write_text(Path(values['source.servers.file']).read_text(encoding='utf-8').replace(
            'lito_play_20261011','lito_roomtest_20261011'),encoding='utf-8')
        values['source.servers.file']=str(sources).replace('\\','/')
        values['state.file']=str(run/'outputs/server.state').replace('\\','/')
        path.write_text('\n'.join(k+'='+v for k,v in values.items())+'\n',encoding='utf-8')
        config['config']=module.pin(path);config['outputs']['state.file']=values['state.file']
    return run,configs
module.prepare=prepare
pair=module.Pair('shared-timing-ready-'+str(time.time_ns()))
try:
    pair.start_pair();pair.check_alive()
    assert pair.report['sql_connected']
    log=Path(pair.report['instances'][0]['stdout']).read_bytes()
    assert b'battle-animations=/battle/spr-action-tw13081901.csv sprites=7394 actions=60303' in log
finally:pair.cleanup()
assert pair.report['cleanup_verified']
(ROOT/'formal-timing-test.json').write_text(json.dumps({'accepted':True,'run':str(pair.run),
    'port':32020,'sql_connected':True,'shared_action_count':60303,'cleanup_verified':True},indent=2),encoding='utf-8')
print('PASS: formal shared timing + SQL + owned endpoints and cleanup on 32020.')
