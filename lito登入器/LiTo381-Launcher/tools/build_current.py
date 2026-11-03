"""Build the single current release directly from src; no historical release template."""
from pathlib import Path
import hashlib,importlib.util,json,re,subprocess
L=Path(__file__).resolve().parent.parent
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
helper=L/'src/development_startup.py'
spec=importlib.util.spec_from_file_location('build_helper',helper);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
pins={p:sha(p) for p in m.PINS}
t=helper.read_text('utf-8');a=t.index('PINS = {');b=t.index('\nBIN_SHA = ',a)
t=t[:a]+'PINS = {\n'+''.join(f'    Path({str(p)!r}): {v!r},\n' for p,v in pins.items())+'}\n'+t[b:]
helper.write_text(t,encoding='utf-8')
p=L/'src/DevelopmentStartup.cs';t=p.read_text('utf-8')
t=re.sub(r'private const string EntrySha = "[A-F0-9]+";',f'private const string EntrySha = "{sha(helper)}";',t);p.write_text(t,encoding='utf-8')
p=L/'src/FormalStandardSources.cs';t=p.read_text('utf-8')
def refresh(match):
    path=Path(match[1]);return f'new(@"{path}", {path.stat().st_size}, "{sha(path)}")'
t=re.sub(r'new\(@"([^"\n]+)", \d+, "[A-F0-9]+"\)',refresh,t);p.write_text(t,encoding='utf-8')
(L/'app/runtime-pins.json').write_text(json.dumps({str(p):v for p,v in pins.items()},ensure_ascii=False,indent=2),encoding='utf-8')
subprocess.run([r'C:\Program Files\dotnet\dotnet.exe','publish',str(L/'src/LiTo381Launcher.csproj'),'-c','Release','-r','win-x86','--self-contained','true','-o',str(L/'release/current')],check=True)
print('Published release/current (0.6.71).')
