"""Cross-check source frame times, existing JAR resources and real JVM lookup."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

from rebuild_animation import recover, SOURCE
import catalog

R = Path(__file__).resolve().parent


def pin(path):
    return {"path":str(path),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}


def main():
    compiler, provenance = recover()
    from frame_ir import parse_action
    expected = {}
    for path, offset in ((SOURCE,0),(R/"build/inputs/LTlist.spr",30000)):
        for sprite in compiler.parse(path.read_bytes()):
            rate = 24
            for cmd in sprite.commands:
                if cmd.opcode == 110:
                    rate = int(re.findall(r"-?\d+",cmd.body)[0])
                elif cmd.opcode < 100:
                    key = (offset+sprite.id,cmd.opcode)
                    if cmd.alias_sprite is not None:
                        original = (offset+cmd.alias_sprite,cmd.opcode)
                        if original in expected:
                            expected[key] = expected[original]
                    else:
                        _, frames = parse_action(cmd.body)
                        expected[key] = sum(frame.ticks*1000//rate for frame in frames)
    assert len(expected) == 60303
    contract = json.loads((R/"build/presentation-contract.json").read_text("utf-8"))
    javac = Path(shutil.which("javac"))
    target = R/"build/presentation-probe"
    subprocess.run([str(javac),"-d",str(target),str(R/"SharedDurationProbe.java")],check=True)
    results = []
    for spec in contract["jars"]:
        jar = Path(spec["jar"]["path"])
        assert pin(jar) == spec["jar"]
        with zipfile.ZipFile(jar) as z:
            raw = z.read("battle/spr-duration-tw13081901.csv")
            actual = {}
            for line in raw.decode("utf-8").splitlines():
                if not line or line.startswith("#"): continue
                sprite,action,duration = map(int,line.split(","))
                assert (sprite,action) not in actual
                actual[sprite,action] = duration
            assert actual == expected, ("source/server duration mismatch",spec["label"])
        call = subprocess.run([str(javac.with_name("java.exe")),
            "-Dlito.resourceProfile=tw13081901-v1",
            "-Dlito.commonCatalog="+str(catalog.LAUNCHER/"app/common_catalog.bin"),
            "-cp",str(target)+";"+str(jar),"SharedDurationProbe"],
            capture_output=True,text=True,timeout=30)
        assert call.returncode == 0, call.stderr
        assert pin(jar) == spec["jar"]
        results.append({"jar":spec["jar"],"label":spec["label"],
                        "duration_resource_sha256":hashlib.sha256(raw).hexdigest(),
                        "source_rows_compared":len(expected),"actual_lookup":call.stdout.strip()})
    report = {
        "status":"passed",
        "inputs":[pin(SOURCE),pin(R/"build/inputs/LTlist.spr"),pin(R/"SharedDurationProbe.java"),
                  pin(R/"build/presentation-contract.json")],
        "compiler_provenance":provenance,"results":results,
        "scope":"Per-frame integer milliseconds summed per action, including aliases and 285 LiTo UI additions",
        "limits":["No renderer wall-clock measurement, network pacing or live battle tested.",
                  "Does not qualify all damage/skill logic in any combat candidate."],
        "source_database_modified":False,"formal_updated":False,
    }
    (R/"build/server-durations.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),"utf-8")
    print(f"PASS {len(expected)} source/native-frame duration totals and actual JVM lookups x {len(results)} JARs")


if __name__ == "__main__":
    main()
