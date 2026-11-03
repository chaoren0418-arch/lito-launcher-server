from pathlib import Path
import hashlib,json,shutil,subprocess
R=Path(__file__).resolve().parent;W=R/"build/selection-recovery"
def pin(p):return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def main():
    old=R/"build/candidate/LiTo381-SharedPresentation-candidate.jar"
    new=W/old.name
    javac=Path(shutil.which("javac"));java=javac.with_name("java.exe")
    classes=W/"tests";classes.mkdir(exist_ok=True)
    files=["SourceSelectionReadProbe.java","SelectionCleanupProbe.java","ArmorBoundaryProbe.java",
           "BluePotionSessionProbe.java","PresentationWireProbe.java","SharedDurationProbe.java"]
    subprocess.run([str(javac),"-encoding","UTF-8","-cp",str(new),"-d",str(classes),
                    *[str(R/f) for f in files]],check=True)
    profile=["-Dfile.encoding=UTF-8","-Dlito.resourceProfile=tw13081901-v1",
             "-Dlito.commonCatalog="+str(R.parents[1]/"app/common_catalog.bin")]
    dependencies=json.loads((R/"build/candidate/runtime-dependencies.json").read_text("utf-8"))
    (W/"lib").mkdir(exist_ok=True)
    copied=[]
    for row in dependencies:
        source=Path(row["path"]);assert pin(source)["sha256"]==row["sha256"]
        target=W/"lib"/source.name;shutil.copyfile(source,target);copied.append(pin(target))
    tests={}
    def run(name,cls,args,jar=new,expected=0):
        cp=str(classes)+";"+str(jar)+";"+str(W/"lib/*")
        p=subprocess.run([str(java),"-Xverify:all",*profile,"-cp",cp,cls,*map(str,args)],capture_output=True,timeout=60)
        log=W/(name+".log");log.write_bytes(p.stdout+p.stderr)
        assert (p.returncode==0)==(expected==0),(name,p.returncode,p.stdout[-2500:],p.stderr[-2500:])
        tests[name]=pin(log);print("PASS",name)
    run("armor-boundaries","ArmorBoundaryProbe",[old,new])
    run("normal-disconnect","local.lito381.server.SelectionCleanupProbe",[W/"normal-fixture"])
    run("fault-parent-reproduced","local.lito381.server.SelectionCleanupProbe",[W/"parent-fault-fixture","fault"],old,1)
    run("fault-disconnect","local.lito381.server.SelectionCleanupProbe",[W/"fault-fixture","fault"])
    run("source-read","local.lito381.server.SourceSelectionReadProbe",
        [R/"build/candidate/runs/world-shared-test-20261020-02/config/server.dev.standard.properties",
         "3333",W/"read-fixture-unused.properties"])
    run("blue-potion","local.lito381.server.BluePotionSessionProbe",[W/"blue-fixture"])
    run("wire","PresentationWireProbe",[])
    run("duration","SharedDurationProbe",[])
    original=json.loads((R/"build/presentation-server.json").read_text("utf-8"))["wire_cases"]
    wire=dict(line.split("=",1) for line in (W/"wire.log").read_text("utf-8").splitlines() if "=" in line)
    assert wire==original,"presentation wire drift"
    native=json.loads((W/"native-build.json").read_text("utf-8"))
    assert native["status"]=="qualified-native-fixture"
    tests["native"]=pin(W/"native-build.json")
    assert pin(old)["sha256"]=="e1496dc6a45cccf331ed439e28b2b4b2fb0eec99df53316d076633b446d90cb9"
    receipt=dict(status="selection-recovery-candidate-qualified-native-ui-pending",
        parent_jar=pin(old),parent_dll=pin(R/"build/candidate/ddraw.dll"),
        jar=pin(new),dll=native["candidate"],dependencies=copied,tests=tests,
        sources=[pin(R/name) for name in files+["SelectionRecovery.java","SelectionRecoveryPatch.java",
            "selection_login_recovery.inc","selection_login_fixture.inc","build_selection_recovery.py",
            "build_selection_native.py","test_selection_recovery.py"]],
        formal_updated=False,source_database_modified=False,
        limitations=["User who-is-who selection has no matching successful selection log in the failed run; retest distinct names.",
                     "The original disconnect failure cause was not logged; injected cleanup exception proves final ownership release.",
                     "Actual blank-login UI awaits user test; original receiver and callback were executed in native fixture."])
    (W/"qualification.json").write_text(json.dumps(receipt,ensure_ascii=False,indent=2),"utf-8")
if __name__=="__main__":main()
