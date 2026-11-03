"""Child of the qualified selection-recovery DLL, with isolated outputs."""
from pathlib import Path
import json,hashlib,shutil,subprocess
R=Path(__file__).resolve().parent;W=R/'build/class-icons';SRC=W/'native';OUT=W/'candidate'
def once(s,a,b):
    if s.count(a)!=1:raise ValueError('source seam changed: '+a)
    return s.replace(a,b)
def main():
    SRC.mkdir(parents=True,exist_ok=True);OUT.mkdir(exist_ok=True)
    for p in (R/'build/selection-recovery/native').iterdir():
        if p.suffix in ('.cpp','.inc','.h','.def'):shutil.copyfile(p,SRC/p.name)
    for n in ('class_icons.inc','class_icons_fixture.inc'):shutil.copyfile(R/n,SRC/n)
    f=SRC/'class_compat.inc';s=f.read_text('utf-8')
    s=once(s,'#include "profile_location.inc"','#include "profile_location.inc"\n#include "class_icons.inc"')
    s=once(s,'    AddProfileLocationPatch(patches);','    AddProfileLocationPatch(patches);\n    AddExtendedClassIconPatches(patches);');f.write_text(s,'utf-8')
    f=SRC/'status_install_test.cpp';s=f.read_text('utf-8')
    s=once(s,'#include "selection_login_fixture.inc"','#include "selection_login_fixture.inc"\n#include "class_icons_fixture.inc"')
    s=once(s,'        CHECK_INSTALL(RunSelectionLoginFixture()==0);','        CHECK_INSTALL(RunSelectionLoginFixture()==0);\n        CHECK_INSTALL(RunClassIconFixture()==0);');f.write_text(s,'utf-8')
    for n in ('shared_status.bin','native-status-table.bin'):shutil.copyfile(R/'build/selection-recovery/candidate'/n,OUT/n)
    script=W/'compile.cmd';s=(R/'build/compile.cmd').read_text('utf-8').replace('candidate/ddraw.dll','candidate/litoddraw.dll');script.write_text(s,'utf-8')
    result=subprocess.run(['cmd.exe','/d','/c',str(script)],cwd=SRC,capture_output=True)
    (W/'compile.log').write_bytes(result.stdout+result.stderr)
    if result.returncode:print((result.stdout+result.stderr).decode('utf-8','replace'));raise SystemExit(result.returncode)
    native=R.parents[2]/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe'
    for mode in ('accept','reject','reject-option'):
        test=subprocess.run([str(OUT/'status_install_test.exe'),str(native),mode],cwd=OUT,capture_output=True)
        (W/f'native-{mode}.log').write_bytes(test.stdout+test.stderr)
        print(test.stdout.decode('utf-8','replace')[-1600:]);assert test.returncode==0,test.returncode
    def pin(p):return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
    (W/'native-build.json').write_text(json.dumps(dict(status='qualified-native-fixture',candidate=pin(OUT/'litoddraw.dll'),
        parent=pin(R/'build/selection-recovery/candidate/ddraw.dll'),formal_updated=False),indent=2),'utf-8')
if __name__=='__main__':main()
