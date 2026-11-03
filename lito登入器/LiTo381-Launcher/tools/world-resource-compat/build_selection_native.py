"""Build a separate child DLL; never overwrite the DLL being tested."""
from pathlib import Path
import shutil,subprocess,json,hashlib
R=Path(__file__).resolve().parent
W=R/"build/selection-recovery"
SRC=W/"native";OUT=W/"candidate"
def once(text,a,b):
    assert text.count(a)==1,a
    return text.replace(a,b)
def main():
    SRC.mkdir(parents=True,exist_ok=True);OUT.mkdir(exist_ok=True)
    for p in (R/"build/native").iterdir():
        if p.suffix in (".cpp",".inc",".h",".def"):shutil.copyfile(p,SRC/p.name)
    for name in ("selection_login_recovery.inc","selection_login_fixture.inc"):
        shutil.copyfile(R/name,SRC/name)
    f=SRC/"class_skills.inc";s=f.read_text("utf-8")
    s=once(s,'#include "shared_frame_rate.inc"',
        '#include "shared_frame_rate.inc"\n#include "selection_login_recovery.inc"')
    s=once(s,"    AddSharedFrameRatePatches(patches);",
        "    AddSharedFrameRatePatches(patches);\n    AddSelectionLoginRecoveryPatches(patches);")
    f.write_text(s,"utf-8")
    f=SRC/"status_install_test.cpp";s=f.read_text("utf-8")
    s=once(s,"int wmain(int argc,wchar_t** argv) {",
        '#include "selection_login_fixture.inc"\nint wmain(int argc,wchar_t** argv) {')
    s=once(s,'        std::printf("PASS native world packet-box dispatch:',
        '        CHECK_INSTALL(RunSelectionLoginFixture()==0);\n        std::printf("PASS native world packet-box dispatch:')
    f.write_text(s,"utf-8")
    for name in ("shared_status.bin","native-status-table.bin"):
        shutil.copyfile(R/"build/candidate"/name,OUT/name)
    cmd=R/"build/compile.cmd"
    script=W/"compile.cmd";script.write_bytes(cmd.read_bytes())
    result=subprocess.run(["cmd.exe","/d","/c",str(script)],cwd=SRC,capture_output=True)
    (W/"native-compile.log").write_bytes(result.stdout+result.stderr)
    if result.returncode:
        print((result.stdout+result.stderr).decode("utf-8","replace"));raise SystemExit(result.returncode)
    native=R.parents[2]/"舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe"
    results=[]
    for mode in ("accept","reject","reject-option"):
        call=subprocess.run([str(OUT/"status_install_test.exe"),str(native),mode],cwd=OUT,capture_output=True)
        (W/("native-"+mode+".log")).write_bytes(call.stdout+call.stderr)
        print(call.stdout.decode("utf-8","replace"))
        assert call.returncode==0,call.returncode
        results.append(mode)
    def pin(p):return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
    (W/"native-build.json").write_text(json.dumps(dict(
        status="qualified-native-fixture",candidate=pin(OUT/"ddraw.dll"),
        parent=pin(R/"build/candidate/ddraw.dll"),tests=results,
        formal_updated=False),indent=2),"utf-8")
if __name__=="__main__":main()
