"""Make a reproducible isolated server from the existing qualified combat JAR.

Two Protocol writers and one ClientSession icon-send seam are changed; a small
packet helper is added. Runtime probes preserve other behavior. Never writes source DB,
server src, formal JAR or another developer's candidate.
"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import tempfile
import zipfile

import catalog

R=Path(__file__).resolve().parent
EXPORTS=["--add-exports","java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
         "--add-exports","java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
         "--add-exports","java.base/jdk.internal.org.objectweb.asm.util=ALL-UNNAMED"]
CLASS="local/lito381/server/Protocol.class"
SESSION="local/lito381/server/ClientSession.class"
HELPER="local/lito381/server/SharedPresentationPackets.class"


def pin(path):
    return {"path":str(path.resolve()),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}


def main():
    contract_path=R/"build/presentation-contract.json"
    contract=json.loads(contract_path.read_text("utf-8"))
    spec=next(r for r in contract["jars"] if r["label"]=="backend-qualified")
    base=Path(spec["jar"]["path"])
    assert pin(base)==spec["jar"]
    qualification=json.loads(Path(spec["receipt"]["path"]).read_text("utf-8"))
    assert qualification["runtime"]==spec["jar"]
    assert qualification["status"]=="backend-regressions-passed-native-validation-deferred-by-user"
    work=R/"build/presentation-probe";work.mkdir(exist_ok=True)
    before,after=work/"Protocol.original.class",work/"Protocol.shared.class"
    session_before,session_after=work/"ClientSession.original.class",work/"ClientSession.shared.class"
    with zipfile.ZipFile(base) as archive:
        assert not any(p.upper().endswith((".SF",".RSA",".DSA")) for p in archive.namelist())
        before.write_bytes(archive.read(CLASS))
        session_before.write_bytes(archive.read(SESSION))
    javac=Path(shutil.which("javac"));java=javac.with_name("java.exe")
    subprocess.run([str(javac),*EXPORTS,"-d",str(work),str(R/"PacketBoxProducerPatch.java")],check=True)
    patched=subprocess.run([str(java),*EXPORTS,"-cp",str(work),"PacketBoxProducerPatch",str(before),str(after)],
                           capture_output=True,text=True,timeout=30)
    assert patched.returncode==0,patched.stderr
    subprocess.run([str(javac),*EXPORTS,"-d",str(work),str(R/"BluePotionPresentationPatch.java")],check=True)
    session_patch=subprocess.run([str(java),*EXPORTS,"-cp",str(work),"BluePotionPresentationPatch",
        str(session_before),str(session_after)],capture_output=True,text=True,timeout=30)
    assert session_patch.returncode==0,session_patch.stderr
    support=work/"packet-support";support.mkdir(exist_ok=True)
    subprocess.run([str(javac),"-d",str(support),str(R/"SharedPresentationPackets.java")],check=True)
    candidate=R/"build/candidate/LiTo381-SharedPresentation-candidate.jar"
    # java -jar resolves manifest Class-Path relative to this isolated JAR.
    # Configuration probes using an explicit classpath do not exercise it.
    dependencies=[]
    (candidate.parent/"lib").mkdir(exist_ok=True)
    for name in ("mysql-connector-j-8.0.33.jar","protobuf-java-3.21.9.jar",
                 "HikariCP-5.1.0.jar","slf4j-api-1.7.36.jar"):
        source=catalog.LAUNCHER/"app/server/lib"/name
        target=candidate.parent/"lib"/name
        shutil.copyfile(source,target)
        assert pin(source)["sha256"]==pin(target)["sha256"]
        dependencies.append(dict(pin(target),source=str(source)))
    (candidate.parent/"runtime-dependencies.json").write_text(
        json.dumps(dependencies,indent=2),"utf-8")
    previous=pin(candidate)["sha256"] if candidate.exists() else None
    with zipfile.ZipFile(base) as src,zipfile.ZipFile(candidate,"w") as dst:
        for entry in src.infolist():
            changed_data=after.read_bytes() if entry.filename==CLASS else (
                session_after.read_bytes() if entry.filename==SESSION else src.read(entry.filename))
            dst.writestr(entry,changed_data)
        new_entry=zipfile.ZipInfo(HELPER,date_time=(1980,1,1,0,0,0))
        new_entry.compress_type=zipfile.ZIP_DEFLATED
        dst.writestr(new_entry,(support/HELPER).read_bytes())
    with zipfile.ZipFile(base) as src,zipfile.ZipFile(candidate) as dst:
        assert dst.namelist()==src.namelist()+[HELPER]
        changed=[name for name in src.namelist() if src.read(name)!=dst.read(name)]
        assert dst.read(HELPER)==(support/HELPER).read_bytes()
    assert set(changed)=={CLASS,SESSION},changed
    subprocess.run([str(javac),"-d",str(work),str(R/"PresentationWireProbe.java"),
                    str(R/"SharedDurationProbe.java")],check=True)
    profile=["-Dlito.resourceProfile=tw13081901-v1",
             "-Dlito.commonCatalog="+str(catalog.LAUNCHER/"app/common_catalog.bin")]
    call=subprocess.run([str(java),"-Xverify:all",*profile,"-cp",str(work)+";"+str(candidate),
                         "PresentationWireProbe"],capture_output=True,text=True,timeout=30)
    assert call.returncode==0,call.stderr
    actual=dict(line.split("=",1) for line in call.stdout.splitlines())
    expected=dict(contract["wire_cases"]["tw13081901-v1"])
    differences=[]
    for key,raw in list(expected.items()):
        if key.startswith(("doll/","dollCustom/")):
            data=bytearray.fromhex(raw);assert data[0]==250;data[0]=123
            if key.startswith("doll/"):data.extend(b"\0\0")
            expected[key]=data.hex();differences.append(key)
    assert actual==expected,[(key,actual.get(key),value) for key,value in expected.items() if actual.get(key)!=value]
    assert len(differences)==36
    duration=subprocess.run([str(java),"-Xverify:all",*profile,"-cp",str(work)+";"+str(candidate),
                             "SharedDurationProbe"],capture_output=True,text=True,timeout=30)
    assert duration.returncode==0,duration.stderr
    session_classes=work/"session-probe";session_classes.mkdir(exist_ok=True)
    subprocess.run([str(javac),"-cp",str(candidate),"-d",str(session_classes),
                    str(R/"BluePotionSessionProbe.java")],check=True)
    with tempfile.TemporaryDirectory(prefix="blue-session-",dir=R/"build") as temp:
        temp_path=Path(temp)
        assert temp_path.resolve().parent==(R/"build").resolve() and not temp_path.is_symlink()
        session_test=subprocess.run([str(java),"-Xverify:all",*profile,"-cp",
            str(session_classes)+";"+str(candidate),"local.lito381.server.BluePotionSessionProbe",temp],
            capture_output=True,text=True,timeout=45)
        (R/"build/blue-session.log").write_text(session_test.stdout+session_test.stderr,"utf-8")
        assert session_test.returncode==0,session_test.stdout+session_test.stderr
    assert pin(base)==spec["jar"]
    result={
        "status":"paired-presentation-server-qualified",
        "candidate":pin(candidate),"parent":spec["jar"],"parent_qualification":pin(Path(spec["receipt"]["path"])),
        "inputs":[pin(contract_path),pin(R/"PacketBoxProducerPatch.java"),pin(R/"PresentationWireProbe.java"),
                  pin(R/"SharedDurationProbe.java"),pin(R/"BluePotionPresentationPatch.java"),
                  pin(R/"SharedPresentationPackets.java"),pin(R/"BluePotionSessionProbe.java")],
        "formal_entry":pin(catalog.LAUNCHER/"app/server/Start-Shared-Formal.py"),
        "changed_entries":changed,
        "added_entries":[HELPER],
        "changed_methods":["Protocol.magicDollIcon(I)[B","Protocol.magicDollCustomIcon(IIZ)[B",
                           "ClientSession.sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V"],
        "unchanged_method_check":patched.stdout.strip(),
        "session_patch_check":session_patch.stdout.strip(),
        "blue_session_test":pin(R/"build/blue-session.log"),
        "packet_cases":len(actual),"changed_packet_cases":differences,"wire_cases":actual,
        "duration_check":duration.stdout.strip(),
        "reproducible":previous==pin(candidate)["sha256"] if previous else None,
        "scope":"Doll opcodeFA->7B; generic56 adds subtype0; blue-potion existing activation also sends matching duration to7B34. ClanFA and existing combat behavior retained.",
        "limits":["Parent full combat/UI qualification is not extended by these presentation probes.",
                  "In-memory GameHub/ClientSession and loopback sockets exercised and closed; no SQL, ServerMain or game process started."],
        "formal_updated":False,"source_database_modified":False,
    }
    (R/"build/presentation-server.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),"utf-8")
    print(f"PASS isolated server: {len(actual)} packet cases, only{len(differences)} doll cases changed; {duration.stdout.strip()}")


if __name__=="__main__":
    main()
