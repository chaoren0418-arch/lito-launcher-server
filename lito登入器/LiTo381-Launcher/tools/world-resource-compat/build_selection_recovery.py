"""Stage a narrowly patched child of the currently tested presentation candidate."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent
W=R/"build/selection-recovery"
BASE=R/"build/candidate/LiTo381-SharedPresentation-candidate.jar"
EXPORTS=["--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
         "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
         "--add-exports", "java.base/jdk.internal.org.objectweb.asm.util=ALL-UNNAMED"]
def pin(p):return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def main():
    W.mkdir(exist_ok=True)
    expected="e1496dc6a45cccf331ed439e28b2b4b2fb0eec99df53316d076633b446d90cb9"
    assert pin(BASE)["sha256"]==expected
    javac=Path(shutil.which("javac"));java=javac.with_name("java.exe")
    subprocess.run([str(javac),*EXPORTS,"-d",str(W),str(R/"SelectionRecoveryPatch.java")],check=True)
    subprocess.run([str(javac),"-encoding","UTF-8","-cp",str(BASE),"-d",str(W),
        str(R/"SelectionRecovery.java")],check=True)
    changes={}
    with zipfile.ZipFile(BASE) as z:
        for cls in ("InventoryCatalog$NativeArmorStatus","ClientSession"):
            name="local/lito381/server/"+cls+".class"
            a=W/(cls+".before.class");b=W/(cls+".after.class")
            a.write_bytes(z.read(name))
            subprocess.run([str(java),*EXPORTS,"-cp",str(W),"SelectionRecoveryPatch",str(a),str(b)],check=True)
            changes[name]=b.read_bytes()
        candidate=W/BASE.name
        with zipfile.ZipFile(candidate,"w") as out:
            for entry in z.infolist():out.writestr(entry,changes.get(entry.filename,z.read(entry.filename)))
            helper="local/lito381/server/SelectionRecovery.class"
            entry=zipfile.ZipInfo(helper,date_time=(1980,1,1,0,0,0))
            entry.compress_type=zipfile.ZIP_DEFLATED
            out.writestr(entry,(W/helper).read_bytes())
    with zipfile.ZipFile(BASE) as a,zipfile.ZipFile(candidate) as b:
        assert {n for n in a.namelist() if a.read(n)!=b.read(n)}==set(changes)
    (W/"server-build.json").write_text(json.dumps({
        "status":"built-not-yet-qualified","parent":pin(BASE),"candidate":pin(candidate),
        "changed_entries":list(changes),"formal_updated":False,"source_database_modified":False
    },indent=2),"utf-8")
if __name__=="__main__":main()
