"""Seal scoped evidence only. Keep the last fully qualified baseline and formal release unchanged."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, zipfile

R = Path(__file__).resolve().parent
ROOT = R.parent
S = R.parents[2]
D = R / "build"
read = lambda p: json.loads(p.read_text("utf-8"))
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
pin = lambda p: dict(path=str(p.resolve()), sha256=sha(p))
def write(p, value):
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

b = read(D / "build.json")
runtime = b["runtime"]
assert pin(Path(runtime["path"])) == runtime
assert b["base"]["sha256"] == "8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8"
repro = read(R / "build-repro/build.json")
assert sha(Path(repro["runtime"]["path"])) == runtime["sha256"] == repro["runtime"]["sha256"]
rows, counts = [], dict(cases=0, checks=0, socket_writes_under_locks=0, material_faults=0, partial_material_commits=0)
for folder, expected in [
    ("tests", 120), ("boundaries", 12), ("lifecycle", 84),
    ("scenes", 108), ("scene-boundaries", 12), ("scene-lifecycle", 60),
    ("retained-battle/tests", 276), ("retained-battle/boundaries", 12)
]:
    p = D / folder / "result.json"
    v = read(p)
    assert v["status"].startswith("passed") and v["runtime"] == runtime, folder
    values = [v["after"]] if "after" in v else [x["results"] for x in v["results"] if x["mode"] == "after"]
    assert sum(x["cases"] for x in values) == expected, folder
    for x in values:
        counts["cases"] += x["cases"]
        counts["checks"] += x["checks"]
        assert x.get("locked_writes", x.get("locked_write_calls")) == 0
        assert x.get("failed_material_casts_with_partial_commit", 0) == 0
        assert x.get("Source_window_bypasses", 0) == 0
        counts["material_faults"] += x.get("actual_memory_faults", 0)
    rows.append(dict(evidence=pin(p), cases=expected))
assert counts["cases"] == 684 and counts["material_faults"] == 36
formal = S.parent.parent / "lito登入器/LiTo381-Launcher"
protected = [
    (formal / "app/server/LiTo381-SharedResources-candidate.jar", "75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371"),
    (formal / "app/compat/ddraw.dll", "0135e6e76594eb98695b85e9b451669adaedccfa6a13fafc49bf934d9c761de4"),
    (Path("D:/lineage開發/0-客戶端/LiTo.bin"), "ce6ee68ada8fd37e6c096b1a27e46cb6888f1179f85defb5007ac60287360292"),
]
for p, expected in protected:
    assert sha(p) == expected, str(p)
with zipfile.ZipFile(b["base"]["path"]) as old, zipfile.ZipFile(runtime["path"]) as new:
    changed = sorted(n for n in old.namelist() if n in new.namelist() and old.read(n) != new.read(n))
    added = sorted(set(new.namelist()) - set(old.namelist()))
    assert not set(old.namelist()) - set(new.namelist())
    assert set(changed) == set(b["changed"]) and set(added) == set(b["added"])
calls = (D / "audit/candidate-calls.tsv").read_text("utf-8").splitlines()
assert len(calls) == 5 and all(x.startswith("local/lito381/server/NativeStaticNpcScene\t") for x in calls)
source = Path("D:/lineage開發/1-伺服器/src/com/lineage/server/model/Instance")
audit = dict(
    status="reviewed-source-and-existing-candidate-no-reimplementation",
    source_files=[pin(source / name) for name in ["L1QuestInstance.java", "L1NpcInstance.java", "L1SummonInstance.java"]],
    existing_candidate_method_evidence=[pin(D / "audit/release-companion.txt"), pin(D / "audit/population-release.txt")],
    findings=[
        "L1QuestInstance intentionally disables movement AI for 71075,70957,81209,80012; do not introduce wandering for these NPCs.",
        "Type1 passispeed/name/gfx projection exists; dynamic static-NPC coordinates, AI schedule, collision and perception consumers still need a coherent implementation.",
        "TAMED releaseCompanionToNature already has WORLD and BATTLE restoration branches; reuse these entry points instead of implementing duplicate routes.",
        "Source liberateClaimed creates a new monster ID, transfers carried inventory to the monster, preserves current HP/MP, sets EXP=0 and marks drops initialized.",
        "Candidate WORLD releaseTamed reactivates the original hidden actor; caller transfers companion items to owner. This differs from Source liberation and requires targeted transaction/identity/reward repair.",
    ],
    source_database_mutated=False, formal_deployment_changed=False,
)
write(D / "remaining-source-audit.json", audit)
q = dict(
    updated_utc=datetime.now(timezone.utc).isoformat(),
    status="passed-scoped-static-scene-regressions-full-objective-open",
    runtime=runtime, base=b["base"], build=pin(D / "build.json"),
    reproduction=pin(R / "build-repro/build.json"), tests=rows, counts=counts,
    exact_inverse=b["exact_static_scene_calls_and_lifecycle_inverse"],
    callsite_audit=pin(D / "audit/candidate-calls.tsv"),
    source_audit=pin(D / "remaining-source-audit.json"),
    protected=[pin(p) for p, _ in protected],
    fixture_limit="Instance tests place the three read-only Source quest templates into map1011 in fixture memory only. A catalog-initialization overlay makes the fixture catalog writable; production catalog, DB and map admission checks are unchanged.",
    completed=[
        "Map+instance+NPC presentation state, native perception and quest restore; 26 ClientSession calls redirected; five public-world fallback calls remain.",
        "Pending scene admission rejects a caster who left and returned to the same map/instance.",
        "Deferred publication rejects moved viewers and stale/recreated NPC presentation identity; no network writes under owned locks.",
        "Private state released when the final member leaves/transitions; hub close releases public/private presentation state.",
    ],
    remaining=["Static NPC AI/movement consumers", "TAMED Source liberation parity and other open paths in main report",
               "Final composed/system/SQL/native batch validation after the remaining implementation"],
    same_sha_backend_regression_passed=False,
    formal_deployment_changed=False, source_database_mutated=False,
    full_goal_achieved=False, native_launched=False, publication_allowed=False,
    sealing_driver=pin(Path(__file__)),
)
write(D / "qualification-current.json", q)
index = ROOT / "continuation-current.json"
i = read(index)
assert i["latest_candidate"]["sha256"] == b["base"]["sha256"]
i.update(time_utc=q["updated_utc"], status="scoped-static-scene-candidate-full-objective-open",
         latest_targeted_candidate=runtime, candidate_under_validation=runtime,
         targeted_candidate_full_regression_passed=False,
         cancellation_static_scene_candidate=pin(D / "qualification-current.json"))
i["active_work"].update(updated_utc=q["updated_utc"], static_scene=pin(D / "qualification-current.json"),
                        remaining_source_audit=pin(D / "remaining-source-audit.json"),
                        next="Reuse 743b35d1 scoped candidate (684 cases); 8d26c9d7 remains the fully backend-qualified parent. Complete coherent static NPC AI/movement and TAMED liberation parity, then other open coverage. No formal update or intermediate native testing.")
i["active_work"]["final_publication_allowed"] = False
write(index, i)
controls = ROOT / "control-gaps.json"
c = read(controls)
def repin(value):
    if isinstance(value, dict):
        if value.get("path") == str(index.resolve()) and "sha256" in value:
            value["sha256"] = sha(index)
        for v in value.values(): repin(v)
    elif isinstance(value, list):
        for v in value: repin(v)
repin(c)
write(controls, c)
print(json.dumps(dict(status=q["status"], runtime=runtime, counts=counts, formal_deployment_changed=False), ensure_ascii=False))
