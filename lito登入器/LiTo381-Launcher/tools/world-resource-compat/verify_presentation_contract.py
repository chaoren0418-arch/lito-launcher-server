"""Compare pure presentation packet writers across fixed existing JARs.

This does not qualify combat logic or launch SQL/a server. Each JAR is verified
against its existing receipt before and after the probe, without copying it.
"""
import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess
import zipfile

import catalog

R = Path(__file__).resolve().parent
S = catalog.LAUNCHER.parent.parent / "lito伺服器/LiTo381-Server"


def pin(path):
    return {"path": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def main():
    shield = json.loads((R / "build/shield-wire.json").read_text("utf-8"))
    specs = [("formal", shield["jar"], None)]
    for label, relative in (
        ("backend-qualified", "cancellation-battle-runtime/build/qualification-current.json"),
        ("incremental-unqualified", "integration-lifecycle/current.json"),
    ):
        receipt = S / "tools/combat-continuation" / relative
        info = json.loads(receipt.read_text("utf-8"))
        specs.append((label, info["runtime"], pin(receipt)))
    javac = Path(shutil.which("javac"))
    target = R / "build/presentation-probe"
    target.mkdir(exist_ok=True)
    subprocess.run([str(javac), "-d", str(target), str(R / "PresentationWireProbe.java")], check=True)
    startup = catalog.LAUNCHER / "app/server/Start-Shared-Formal.py"
    startup_text = startup.read_text("utf-8")
    assert "'-Dlito.resourceProfile=tw13081901-v1'" in startup_text
    assert "'-Dlito.commonCatalog='+str(CATALOG)" in startup_text
    common = catalog.LAUNCHER / "app/common_catalog.bin"
    results, baselines = [], {}
    for label, expected, receipt in specs:
        jar = Path(expected["path"])
        assert pin(jar) == expected, ("JAR changed before probe", label)
        counts = {}
        for mode in ("lito-legacy", "tw13081901-v1"):
            call = subprocess.run(
                [str(javac.with_name("java.exe")), "-Dlito.resourceProfile="+mode,
                 "-Dlito.commonCatalog="+str(common), "-cp", str(target)+";"+str(jar),
                 "PresentationWireProbe"], capture_output=True, text=True, timeout=30)
            if call.returncode:
                raise RuntimeError(call.stderr)
            rows = dict(line.split("=", 1) for line in call.stdout.splitlines())
            assert len(rows) == len(call.stdout.splitlines()), "duplicate probe case"
            for label_key, raw in rows.items():
                parts = label_key.split("/")
                if raw.startswith("REJECT:"):
                    if parts[0] in ("speed","braveSpeed"):
                        assert parts[1] == "NORMAL" and int(parts[2]) > 0
                    else:
                        assert parts[0] in ("effect","location")
                        assert int(parts[1]) == 0 or (mode == "tw13081901-v1" and int(parts[1]) == 65535)
                    continue
                wire = bytes.fromhex(raw)
                if parts[0] == "doll":
                    assert wire[1:] == bytes([56])+struct.pack("<H", int(parts[1]))
                elif parts[0] == "dollCustom":
                    graphic, seconds = map(int, parts[1:])
                    assert wire[1:] == bytes([154])+struct.pack("<HHHH", seconds, graphic, 0, seconds == 0)
                elif parts[0] == "effect":
                    requested = int(parts[1])
                    actual = requested if mode == "tw13081901-v1" else {
                        11722:744,11723:829,11724:830,11725:831}.get(requested,requested)
                    assert wire == bytes([55])+struct.pack("<IH", 0x12345678, actual)
                elif parts[0] == "shield":
                    selector, seconds = map(int, parts[1:])
                    assert wire == bytes([109])+struct.pack("<HB", seconds,selector)
            if mode not in baselines:
                baselines[mode] = rows
            assert rows == baselines[mode], ("presentation wire drift", label, mode)
            counts[mode] = len(rows)
        classes = {}
        with zipfile.ZipFile(jar) as z:
            for name in ("Protocol", "BattleSpellCatalog", "SharedClientResources"):
                classes[name] = hashlib.sha256(z.read("local/lito381/server/"+name+".class")).hexdigest()
        assert pin(jar) == expected, ("JAR changed during probe", label)
        results.append({"label":label,"jar":expected,"receipt":receipt,"class_sha256":classes,
                        "cases":counts,"matches_formal":True})
    result = {
        "status":"passed-wire-contract-only",
        "inputs":[pin(R / "PresentationWireProbe.java"),pin(startup),pin(common)],
        "jars":results,"wire_cases":baselines,
        "scope":"Actual pure packet writers; byte equality across formal and two existing candidates",
        "limits":[
            "Not a full combat candidate qualification or merge approval.",
            "No client packet dispatch, UI, SQL, server start or shutdown exercised here.",
            "Magic-doll packet-box56/154 requires a separate LiTo receiver adapter.",
            "Shared profile preserves heal gfx11722..11725; legacy fallback is intentional. No producer change needed.",
        ],
        "source_database_modified":False,"formal_updated":False,
    }
    (R / "build/presentation-contract.json").write_text(json.dumps(result,indent=2,ensure_ascii=False),"utf-8")
    print(f"PASS {sum(map(len,baselines.values()))} presentation cases per JAR across {len(results)} fixed JARs; both profiles; no server/SQL")


if __name__ == "__main__":
    main()
