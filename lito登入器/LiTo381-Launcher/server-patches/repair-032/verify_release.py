from pathlib import Path
import ast, hashlib, json, re
import pefile

r=Path(__file__).resolve().parent
w=r.parent
c=w/'compat-gate'
p=w/'publish-0.6.32'
sha=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
baseline=(r/'skill-baseline-final.log').read_text(encoding='utf-8',errors='replace')
candidate=(r/'skill-final.log').read_text(encoding='utf-8',errors='replace')
failures=lambda text:re.findall(r'^  JUnit Jupiter:(.+)$',text,re.M)
assert sorted(failures(baseline))==sorted(failures(candidate))
assert '[      1115 tests successful' in candidate
assert '[         4 tests failed' in candidate
assert len(failures(candidate))==4
assert sha(r/'candidate-details.jar')==sha(w/'server-skills-details/LiTo381-SharedResources-candidate.jar')
source=(w/'server-skills-details/Start-Shared-Formal.py').read_text(encoding='utf-8')
assert "'-Dlito.extendedItemDetails=true'" in source
assert '"-Dlito.extendedItemDetails=true"' in (w/'launcher-src/FormalStandardSources.cs').read_text(encoding='utf-8')
assert sha(r/'candidate-details.jar') in source
assert 'if (IsVerifiedLiToClient() && !EnsureLiToItemDetailExtension()) return false;' in (c/'ddraw_proxy.cpp').read_text(encoding='utf-8')
pins=json.loads((w/'launcher-build-inputs.json').read_text(encoding='utf-8'))
for path,expected in pins.items():assert sha(Path(path)).upper()==expected,(path,'pin mismatch')
helper=(p/'development_startup.py').read_text(encoding='utf-8')
assert sha(p/'development_startup.py')==sha(w/'launcher-src/development_startup.py')
assert "SERVER_DELIVERY = DELIVERY / 'server-skills-details'" in helper
assert "CLIENT = Path('D:" in helper and '0-客戶端' in helper
assert sha(c/'ddraw.dll').upper() in helper
pe=pefile.PE(str(Path(r'D:\lineage開發\3-lito開發\lito登入器\舊版分析與驗收\runtime\LiTo.unpacked-oep-current.exe')))
signatures={0x4B2AF9:'e89216ffff',0x4496B8:'8b481885c9',0x449CC1:'8b481885c9',0x4D9318:'8b41148b4918'}
for va,expected in signatures.items():assert pe.get_data(va-pe.OPTIONAL_HEADER.ImageBase,len(bytes.fromhex(expected))).hex()==expected,hex(va)
formal=json.loads((w/'server-skills-details/formal-timing-test.json').read_text(encoding='utf-8'))
assert formal['accepted'] and formal['sql_connected'] and formal['cleanup_verified']
for name in ['AreaRegression','BotRegression','DetailsRegression','DetailsPublicationRegression']:
    assert 'PASS' in (r/(name+'-final.log')).read_text(encoding='utf-8',errors='replace'),name
report={'version':'0.6.32','client':r'D:\lineage開發\0-客戶端\LiTo.bin',
 'artifacts':{str(path):sha(path) for path in [p/'LiTo381Launcher.exe',p/'development_startup.py',c/'ddraw.dll',r/'candidate-details.jar']},
 'legacy_regression':{'found':1119,'passed':1115,'failed':4,'same_as_0_6_31':True,'failures':failures(candidate),'profile':'legacy fixture, extension disabled'},
 'shared_profile_focused':['area attack own summon and hostile player','ArenaBot blind/weakness/mana drain','wide item values and more than 20 lines','encrypted inventory then detail publication'],
 'formal_startup':formal,'native_patch_signatures':{hex(k):v for k,v in signatures.items()},
 'native_tests':['result modal hit test','Big5 parser and more than 20 rows','native font ABI and 63-row scrolling','three compiled x86 bridges, both branch paths'],
 'manual_gui_verified':False,
 'remaining':['full skill-by-actor matrix is not complete','ArenaBot has no complete source NPC hit/AC model; disease and additional negative spells remain','several companion negative spell paths remain','metadata fields absent from source inventory model are not invented']}
startup=w/'runs/development-f8693d57f1b34156bf7d561323819f66/native-probe.shared-ready.json'
if startup.exists():
    ready=json.loads(startup.read_text(encoding='utf-8'))
    assert ready['pid']==34052 and ready['total_count']==30285 and ready['native_cleanup_complete']
    assert (w/'gui-runs/development-f8693d57f1b34156bf7d561323819f66/native-created.json').exists()
    report['native_startup_verified']={'run_id':'development-f8693d57f1b34156bf7d561323819f66','resources':ready,'item_extension_log':'03:24:22.444 LiTo item detail v1 installed','scale':200,'launch_parameter_gate_corrected':True}
(r/'release-0.6.32.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS: release pins, exact client, four native signatures, formal SQL readiness, baseline-equivalent regression, focused reports. Manual GUI remains pending.')
