"""Stage .28 from the shipped .27 frontend, retaining its ownership contract."""
from pathlib import Path
import hashlib, json, re, shutil

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT.parent/'formal-standard-20261011'
SERVER_DIRECTORY = 'server-details-033'
SRC = ROOT/'launcher-src-0.6.33'
TARGET = Path('D:/lineage開發/0-客戶端')
SRC.mkdir(exist_ok=True)
for p in (BASE/'src').iterdir():
    if p.is_file(): shutil.copy2(p, SRC/p.name)
shutil.copytree(BASE/'server-profiles', ROOT/'server-profiles', dirs_exist_ok=True)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def pin(p): return 'new(@"'+str(p)+'", '+str(p.stat().st_size)+', "'+sha(p)+'")'
def replace(text, before, after):
    assert before in text, before
    return text.replace(before, after)

# Pin executable adapter dependencies before spawning either paired process.
modules = ['lito_runtime_probe_shared.py', 'shared_bootstrap.py',
           'native_arrow_interruption.py','native_bounds_guards.py','native_resource_trace.py','native_tbt_hooks.py',
           'native_daynight_extension.py','native_fade_hooks.py','native_furniture_extension.py',
           'native_loader_hooks.py','native_morph_extension.py','native_sound_extension.py',
           'native-layout-patches.json','native_fade_extension.dll',
           'common_catalog.bin','common_catalog_manifest.json',
           'compat-details-033/ddraw.dll',SERVER_DIRECTORY+'/Start-Shared-Formal.py',
           SERVER_DIRECTORY+'/LiTo381-SharedResources-candidate.jar']
pins = {ROOT/p: sha(ROOT/p) for p in modules}
pins[TARGET/'TW13081901.txt'] = '7EBE2B464C8B1EA9C58AC985DD8FF4C823784D8231894262EDDCD7691CA94008'
helper = (BASE/'publish-0.6.27/development_startup.py').read_text(encoding='utf-8')
start = helper.index('DELIVERY = '); end = helper.index('BIN_SHA = ', start)
helper = helper[:start]+f'''DELIVERY = Path({str(ROOT)!r})
SERVER_DELIVERY = DELIVERY / {SERVER_DIRECTORY!r}
CLIENT = Path({str(TARGET)!r})
SELECTED = CLIENT
ENTRY = SERVER_DELIVERY / 'Start-Shared-Formal.py'
PROBE = DELIVERY / 'lito_runtime_probe_shared.py'
DLL = DELIVERY / 'compat-details-033/ddraw.dll'
PINS = {{
'''+''.join(f'    Path({str(p)!r}): {v!r},\n' for p,v in pins.items())+'}\n'+helper[end:]
helper = replace(helper, '本次修復只接受指定 D380 客戶端。', '共用資源版只接受 D:/lineage開發/0-客戶端。')
helper = replace(helper, "'--server-profile', str(run/'server-profile.json')", "'--server-profile', str(run/'server-profile.json'),\n              '--shared-resource-candidate', str(DELIVERY)")
helper = replace(helper, "; module.ROOT=SERVER_DELIVERY", "")
helper = replace(helper, "        ceiling=session_end", '''        deadline=time.monotonic()+90
        resource_receipt=run/'native-probe.shared-ready.json'
        while not resource_receipt.exists():
            pair.check_alive(); before_native()
            if probe.poll() is not None: raise RuntimeError('Native exited before common resources became ready')
            if time.monotonic() >= deadline: raise TimeoutError('Common resources initialization exceeded 90 seconds')
            time.sleep(.1)
        resources=json.loads(resource_receipt.read_text(encoding='utf-8'))
        if resources != {'pid':native_id['pid'],'profile':'tw13081901-v1',
                         'base_count':30000,'total_count':30285,'native_cleanup_complete':True}:
            raise RuntimeError('Common resource readiness identity/count mismatch')
        report['shared_resources_ready']=resources
        emit({'event':'resources_ready', **resources})
        ceiling=session_end''')
(SRC/'development_startup.py').write_text(helper, encoding='utf-8')

cs=(SRC/'DevelopmentStartup.cs').read_text(encoding='utf-8-sig')
cs=re.sub(r'internal const string Root = @"[^"]+";', 'internal const string Root = @"'+str(ROOT).replace('\\','\\\\')+'";', cs)
cs=re.sub(r'private const string ServerRuns = @"[^"]+";', lambda m:'private const string ServerRuns = @"'+str(ROOT/SERVER_DIRECTORY/'runs')+'";', cs)
cs=re.sub(r'private const string EntrySha = "[A-F0-9]+";', 'private const string EntrySha = "'+sha(SRC/'development_startup.py')+'";', cs)
cs=replace(cs, '@"D:\\380\\天堂(Lineage 3.81C) (1)"', '@"'+str(TARGET)+'"')
cs=replace(cs, '本次修復請選擇指定的天堂 3.81C 客戶端目錄。', '共用資源版請選擇 D:\\lineage開發\\0-客戶端。'.replace('\\','\\\\'))
cs=replace(cs, '            var staged = MemorySourcePin.Capture(Path.Combine(Root, "client-runtime", "LiTo.bin"));\n', '')
cs=replace(cs, ' || staged.Sha256 != selected.Sha256', '')
cs=replace(cs, 'selected.Verify(); staged.Verify();', 'selected.Verify();')
cs=replace(cs, '指定客戶端或隔離副本的 LiTo.bin 指紋不符。', '指定客戶端的 LiTo.bin 指紋不符。')
cs=replace(cs, '            await helper.Exit.WaitAsync(cancellation);', '''            var resources = await helper.AwaitEventAsync("resources_ready", TimeSpan.FromSeconds(95), cancellation);
            if (resources.GetProperty("pid").GetInt32() != id.GetProperty("pid").GetInt32()
                || resources.GetProperty("profile").GetString() != "tw13081901-v1"
                || resources.GetProperty("total_count").GetInt32() != 30285)
                throw new InvalidDataException("共用動畫資源尚未就緒。");
            log("[共同來源] TW 30000 筆＋LiTo 專用 285 筆已完成原生載入；請登入測試。");
            await helper.Exit.WaitAsync(cancellation);''')
(SRC/'DevelopmentStartup.cs').write_text(cs, encoding='utf-8')

cs=(SRC/'FormalStandardSources.cs').read_text(encoding='utf-8-sig')
start=cs.index('    internal static readonly'); end=cs.index('    internal string RunsDirectory',start)
paths=[Path(r'C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe'), ROOT/SERVER_DIRECTORY/'Start-Shared-Formal.py',
       Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe'), ROOT/SERVER_DIRECTORY/'LiTo381-SharedResources-candidate.jar']
cs=cs[:start]+'    internal static readonly FormalStandardSources Reviewed = new(\n        '+',\n        '.join(pin(p) for p in paths)+');\n'+cs[end:]
cs=replace(cs, 'Java.Path, "-jar", Jar.Path', 'Java.Path, "-Dlito.resourceProfile=tw13081901-v1", "-Dlito.extendedItemDetails=true", @"-Dlito.commonCatalog='+str(ROOT/'common_catalog.bin')+'", "-jar", Jar.Path')
(SRC/'FormalStandardSources.cs').write_text(cs, encoding='utf-8')
for name in ('MainForm.cs','LiTo381Launcher.csproj'):
    p=SRC/name;p.write_text(p.read_text(encoding='utf-8-sig').replace('0.6.27','0.6.33'),encoding='utf-8')
p=SRC/'LauncherSettings.cs'; text=p.read_text(encoding='utf-8-sig')
text=replace(text, '@"D:\\380\\天堂(Lineage 3.81C) (1)", GameDisplayMode.Windowed, 100', '@"'+str(TARGET)+'", GameDisplayMode.Windowed, 200')
# Retain previous menu scale, but this release has one reviewed client resource root.
text=replace(text, '?? LauncherMenuPreferences.Default).Validate();', '?? LauncherMenuPreferences.Default).Validate() with { ClientDirectory = @"'+str(TARGET)+'" };')
p.write_text(text,encoding='utf-8')
(ROOT/'launcher-build-inputs-0.6.33.json').write_text(json.dumps({str(p):v for p,v in pins.items()},ensure_ascii=False,indent=2),encoding='utf-8')
print('Staged 0.6.33 shared-source launcher and exact-target helper.')
