"""Build isolated sources; never overwrite native/, app/, release/ or databases."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys
import catalog

R = Path(__file__).resolve().parent
L = R.parents[1]
B = R / "build"
C = B / "candidate"
SRC = B / "native"


def pin(p):
    return {"path": str(p.resolve()), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}


def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError("source seam drift: " + before[:70])
    return text.replace(before, after)


def main():
    paired=json.loads((B / "presentation-server.json").read_text("utf-8"))
    if paired["status"]!="paired-presentation-server-qualified" or pin(Path(paired["candidate"]["path"]))!=paired["candidate"]:
        raise ValueError("qualify paired doll producer before native build")
    wire = json.loads((B / "shield-wire.json").read_text("utf-8"))
    if wire["status"] != "qualified" or pin(Path(wire["jar"]["path"])) != wire["jar"]:
        raise ValueError("qualify current JAR shield writer before compiling selector adapter")
    C.mkdir(parents=True, exist_ok=True)
    SRC.mkdir(parents=True, exist_ok=True)
    wire_cases=[]
    for key,raw in paired["wire_cases"].items():
        fields=key.split("/")
        if fields[0] not in ("doll","dollCustom"):continue
        packet=bytes.fromhex(raw)
        seconds=int(fields[-1]);slot=87 if fields[0]=="doll" else (
            89+int(fields[1])-512 if 512<=int(fields[1])<=516 else -1)
        wire_cases.append("{"+str(len(packet))+","+str(slot)+","+str(seconds)+",{"+
                          ",".join(map(str,packet))+"}}")
    if len(wire_cases)!=36:raise ValueError("paired doll fixture coverage drift")
    (SRC / "shared_doll_wire_fixture.h").write_text(
        "struct SharedDollWireCase {unsigned size;int slot;unsigned seconds;BYTE packet[10];};\n"+
        "static const SharedDollWireCase kSharedDollWireCases[]={\n"+",\n".join(wire_cases)+"\n};\n","ascii")
    state_cases=[]
    for key,raw in paired["wire_cases"].items():
        family=key.split("/")[0]
        if family not in ("strength","dexterity","breath","speed","braveSpeed"):continue
        if raw.startswith("REJECT:"):continue
        packet=bytes.fromhex(raw)
        seconds=-1 if key.endswith("/permanent") else int(key.split("/")[-1])
        if family=="strength":slot=9
        elif family=="dexterity":slot=10
        elif family=="breath":slot=11
        elif family=="speed":slot={0:-1,1:0,2:1}[packet[5]]
        else:slot={0:-1,1:2,3:2,4:37,5:2,6:95}[packet[5]]
        state_cases.append("{"+str(slot)+","+str(seconds)+",{"+",".join(map(str,packet))+"}}")
    if len(state_cases)!=36:raise ValueError("paired state fixture coverage drift")
    with (SRC/"shared_doll_wire_fixture.h").open("a",encoding="ascii") as stream:
        stream.write("struct SharedStateWireCase {int slot,seconds;BYTE packet[10];};\n"+
            "static const SharedStateWireCase kSharedStateWireCases[]={\n"+",\n".join(state_cases)+"\n};\n")
    originals = []
    for p in sorted((L / "native").iterdir()):
        if p.suffix in (".inc", ".h", ".def") or p.name == "ddraw_proxy.cpp":
            originals.append(pin(p))
            shutil.copy2(p, SRC / p.name)
    shutil.copy2(R / "shared_status_catalog.h", SRC / "shared_status_catalog.h")
    shutil.copy2(R / "status_catalog_test.cpp", SRC / "status_catalog_test.cpp")
    shutil.copy2(R / "status_install_test.cpp", SRC / "status_install_test.cpp")
    for name in ("shared_status_lifecycle.h", "shared_status_lifecycle.inc", "status_lifecycle_test.cpp", "shared_status_order.inc", "shared_packetbox.inc", "shared_frame_rate.inc"):
        shutil.copy2(R / name, SRC / name)
    frame_oracle=json.loads((B/"frame-rate.json").read_text("utf-8"))
    if frame_oracle["status"]!="native-tw-oracle-qualified" or pin(B/"shared_frame_fixture.h")["sha256"]!=frame_oracle["fixture_sha256"]:
        raise ValueError("qualify native frame-rate oracle before build")
    shutil.copy2(B/"shared_frame_fixture.h",SRC/"shared_frame_fixture.h")
    raw, rows = catalog.native_rows()
    data, audit = catalog.compile_catalog((B / "inputs/effectlist2.xml").read_bytes(), raw, rows)
    graphics = json.loads((B / "graphic-dispatch.json").read_text("utf-8"))
    if graphics["xml_sha256"] != audit["xml_sha256"]:
        raise ValueError("qualify graphic dispatch against current XML before build")
    graphic_rows = [r for r in graphics["rows"] if r["qualified_generic"]]
    if len({r["graphic"] for r in graphic_rows}) != len(graphic_rows):
        raise ValueError("ambiguous shared graphic identity")
    if any(not 0 <= r["xml_duration"] <= 32767 for r in graphic_rows):
        raise ValueError("unsupported default graphic duration")
    priority = json.loads((B / "status-priority.json").read_text("utf-8"))
    if priority["status"] != "passed" or priority["xml_sha256"] != audit["xml_sha256"]:
        raise ValueError("qualify native priority against current XML before build")
    if {row["slot"] for row in priority["keys"]}!={row["slot"] for row in audit["bindings"]}:
        raise ValueError("priority evidence does not cover current catalog slots; rerun oracle-only")
    priority_keys = [100]*catalog.SLOTS
    for row in priority["keys"]:
        priority_keys[row["slot"]] = row["key"]
    (C / "shared_status.bin").write_bytes(data)
    (C / "native-status-table.bin").write_bytes(raw)
    behavior = {key:[0]*catalog.SLOTS for key in ("OffIcons","Groups","Durations","StartMessages","EndMessages")}
    for binding in audit["bindings"]:
        slot, effect = binding["slot"], binding["effect"]
        behavior["OffIcons"][slot] = int(effect.get("offIcon", "0"))
        behavior["Groups"][slot] = int(effect.get("group", "0"))
        behavior["Durations"][slot] = int(effect.get("duration", "0"))
        behavior["StartMessages"][slot] = int(effect.get("startMsg", "0"))
        behavior["EndMessages"][slot] = int(effect.get("endMsg", "0"))
    effects=catalog.parse_effects((B / "inputs/effectlist2.xml").read_bytes())
    first_graphic={}
    for key,effect in effects.items():
        graphic=int(effect.get("graphic",0))
        if graphic>0:first_graphic.setdefault(graphic,key)
    packet_graphics=[]
    for binding in audit["bindings"]:
        effect=binding["effect"];graphic=int(effect.get("graphic",0))
        if graphic and first_graphic[graphic]==int(effect["id"]):
            packet_graphics.append({"graphic":graphic,"slot":binding["slot"]})
    (SRC / "shared_status_behavior.h").write_text(
        "#pragma once\nnamespace SharedStatusBehavior {\n" +
        "".join(f"constexpr unsigned {key}[{catalog.SLOTS}]={{" + ",".join(str(v & 0xffffffff) for v in value) + "};\n"
                for key,value in behavior.items()) +
        f"constexpr int PriorityKeys[{catalog.SLOTS}]={{" + ",".join(map(str,priority_keys)) + "};\n"
        "struct Graphic {unsigned graphic,slot;int seconds;};\nconstexpr Graphic Graphics[]={\n" +
        "".join(f'{{{r["graphic"]},{r["slot"]},{r["xml_duration"]}}},\n' for r in graphic_rows) +
        "};\nstruct PacketGraphic {unsigned graphic,slot;};\nconstexpr PacketGraphic PacketGraphics[]={\n"+
        "".join(f'{{{r["graphic"]},{r["slot"]}}},\n' for r in packet_graphics)+
        "};\n}\n", "ascii")
    source = SRC / "ddraw_proxy.cpp"
    text = source.read_text("utf-8")
    text = replace_once(text, "#include <vector>",
                        '#include <vector>\n#include "shared_status_catalog.h"\n#include "shared_status_behavior.h"\n'
                        'static std::uint32_t g_sharedStatusIcons[SharedStatusCatalog::Slots]{};')
    source.write_text(text, "utf-8")
    # Class installation owns the status consumer and runs BEFORE numeric
    # installation. Validate here, before constructing any status patch.
    classes = SRC / "class_compat.inc"
    text = classes.read_text("utf-8")
    text = replace_once(text, "bool EnsureClassCompatibilityPatch() {",
        "static INIT_ONCE g_sharedStatusOnce=INIT_ONCE_STATIC_INIT;\n"
        "static bool g_sharedStatusValid=false;\n"
        "BOOL CALLBACK InitializeSharedStatus(PINIT_ONCE, PVOID, PVOID*) {\n"
        "    char setting[3]{};\n"
        '    DWORD count=GetEnvironmentVariableA("LITO_SHARED_EXPIRED_ICONS",setting,3);\n'
        "    if(count && (count!=1 || (setting[0]!='0' && setting[0]!='1')))return TRUE;\n"
        "    g_sharedExpiredIconsEnabled=!count || setting[0]=='1';\n"
        "    g_sharedStatusValid=SharedStatusCatalog::Load(g_self, reinterpret_cast<void*>(0x6328D8), g_sharedStatusIcons);\n"
        "    return TRUE; // memoize success or failure; no partially retried catalog\n"
        "}\n"
        "bool EnsureClassCompatibilityPatch() {")
    text = replace_once(text, "    AddSevenClassSkillPatches(patches);",
        "    if(!InitOnceExecuteOnce(&g_sharedStatusOnce, InitializeSharedStatus, nullptr, nullptr) || !g_sharedStatusValid) {\n"
        '        Log("shared status catalog rejected before class/status patch commit");\n'
        "        return false;\n    }\n"
        '    Log("shared status catalog validated slots=95 preserved=1");\n'
        "    AddSevenClassSkillPatches(patches);")
    classes.write_text(text, "utf-8")
    dragon = SRC / "dragon_status.inc"
    text = dragon.read_text("utf-8")
    text = replace_once(text,"g_dragonStatusRows[87][5]","g_dragonStatusRows[SharedStatusCatalog::Slots][5]")
    text = replace_once(text,"g_statusVisible[88]{},g_statusActive[88]{}",
                        "g_statusVisible[96]{},g_statusActive[96]{}")
    text = replace_once(text,'DwordBytes(22),"dragon-status-clear-capacity"',
                        'DwordBytes(24),"dragon-status-clear-capacity"')
    text = replace_once(text,'    if(slot<0 || slot>=77) return "";',
                        '    if(slot<0 || slot>=static_cast<int>(SharedStatusCatalog::Slots) || (slot>=77 && slot<87)) return "";')
    # The pinned formal JAR emits shared Shield=5 / EarthSkin=6, while
    # unmodified LiTo dispatches those to EarthBless=7 / Shield=4.
    # Do not reinterpret other legacy selectors without producer evidence.
    text = replace_once(text,
        "    int slot=ExtendedStatusSlot(packet[3]);",
        "    int slot=ExtendedStatusSlot(packet[3]);\n"
        "    if(packet[3]==5)slot=4; // shared SHIELD; verified actual JAR writer\n"
        "    else if(packet[3]==6)slot=6; // shared EARTH_SKIN\n")
    before = "    for(int i=0;i<9;++i)g_dragonStatusRows[78+i][0]=kIllusionIcons[i];"
    text = replace_once(text, before, before +
        "\n    for(unsigned i=0;i<SharedStatusCatalog::Slots;++i){g_dragonStatusRows[i][0]=g_sharedStatusIcons[i];"
        "g_dragonStatusRows[i][2]=SharedStatusBehavior::Groups[i];"
        "if(i>=87){g_dragonStatusRows[i][1]=SharedStatusBehavior::Durations[i];"
        "g_dragonStatusRows[i][3]=SharedStatusBehavior::StartMessages[i];"
        "g_dragonStatusRows[i][4]=SharedStatusBehavior::EndMessages[i];}}")
    dragon.write_text(text, "utf-8")
    skills = SRC / "class_skills.inc"
    text = skills.read_text("utf-8")
    text = replace_once(text, '#include "dragon_status.inc"',
                        '#include "dragon_status.inc"\n#include "shared_status_lifecycle.inc"\n#include "shared_status_order.inc"\n#include "shared_packetbox.inc"\n#include "shared_frame_rate.inc"')
    text = replace_once(text, "    AddDragonStatusPatches(patches);",
                        "    AddDragonStatusPatches(patches);\n    AddSharedStatusLifecyclePatches(patches);\n    AddSharedStatusOrderPatches(patches);\n    AddSharedPacketBoxPatches(patches);\n    AddSharedFrameRatePatches(patches);")
    skills.write_text(text, "utf-8")
    script = B / "compile.cmd"
    script.write_text(
        '@echo off\ncall "C:\\Program Files\\Microsoft Visual Studio\\18\\Insiders\\VC\\Auxiliary\\Build\\vcvars32.bat" >nul\n'
        'if errorlevel 1 exit /b 1\n'
        'cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro '
        'ddraw_proxy.cpp /Fo../candidate/ddraw.obj /Fe../candidate/ddraw.dll '
        '/link /Brepro /MAP:../candidate/ddraw.map /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib\n'
        'if errorlevel 1 exit /b 1\n'
        'cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro '
        'status_catalog_test.cpp /Fo../candidate/status_catalog_test.obj /Fe../candidate/status_catalog_test.exe\n'
        'if errorlevel 1 exit /b 1\n'
        'cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro '
        'status_lifecycle_test.cpp /Fo../candidate/status_lifecycle_test.obj /Fe../candidate/status_lifecycle_test.exe\n'
        'if errorlevel 1 exit /b 1\n'
        'cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro '
        'status_install_test.cpp /Fo../candidate/status_install_test.obj /Fe../candidate/status_install_test.exe '
        '/link /BASE:0x10000 /FIXED /MANIFEST:EMBED /MANIFESTUAC:"level=\'asInvoker\' uiAccess=\'false\'" '
        'user32.lib gdi32.lib dxguid.lib\n'
        'if errorlevel 1 exit /b 1\n', "ascii")
    result = subprocess.run(["cmd.exe", "/d", "/c", str(script)], cwd=SRC,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (B / "compile.log").write_bytes(result.stdout)
    if result.returncode:
        print(result.stdout.decode("utf-8", errors="replace"))
        raise SystemExit(result.returncode)
    test = subprocess.run([str(C / "status_catalog_test.exe"), str(C / "shared_status.bin"),
                           str(C / "native-status-table.bin")], cwd=C,
                          capture_output=True, text=True)
    (B / "native-test.log").write_text(test.stdout + test.stderr, "utf-8")
    if test.returncode:
        raise RuntimeError(test.stdout + test.stderr)
    lifecycle = subprocess.run([str(C / "status_lifecycle_test.exe")],
                              cwd=C, capture_output=True, text=True, timeout=30)
    (B / "lifecycle-test.log").write_text(lifecycle.stdout+lifecycle.stderr, "utf-8")
    if lifecycle.returncode:
        raise RuntimeError(lifecycle.stdout+lifecycle.stderr)
    installer_results = []
    for mode in ("accept", "reject", "reject-option"):
        check = subprocess.run([str(C / "status_install_test.exe"), str(catalog.NATIVE), mode],
                               cwd=C, capture_output=True, text=True, timeout=30)
        (B / f"installer-{mode}.log").write_text(check.stdout+check.stderr, "utf-8")
        if check.returncode:
            raise RuntimeError(f"installer {mode} {check.returncode}: {check.stdout} {check.stderr}")
        installer_results.append(check.stdout.strip())
    audit.update(candidate=pin(C / "ddraw.dll"), catalog=pin(C / "shared_status.bin"),
                 baseline_inputs=originals, source_changes=["ddraw_proxy.cpp", "class_compat.inc", "dragon_status.inc", "class_skills.inc"],
                 lifecycle_test=lifecycle.stdout.strip(),
                 behavior_header=pin(SRC / "shared_status_behavior.h"),
                 paired_server=paired["candidate"], paired_server_receipt=pin(B / "presentation-server.json"),
                 paired_wire_fixture=pin(SRC / "shared_doll_wire_fixture.h"),
                 test_output=test.stdout.strip(), installer_results=installer_results,
                 native_game_started=False, formal_updated=False,
                 full_goal_complete=False)
    audit["behavior"] = "shared icons/offIcons/groups/own-graphic defaults; explicit server durations take precedence; original messages; full group cleanup"
    audit["behavior_tables"] = behavior
    audit["graphic_dispatch_rows"] = len(graphic_rows)
    audit["not_yet_consumed"] = []
    audit["priority"] = "TW creation-order mode with XML priority/kind; original LiTo spacing and draw"
    (B / "status-candidate.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2), "utf-8")
    print(test.stdout.strip())
    print(lifecycle.stdout.strip())
    print("\n".join(installer_results))
    print(json.dumps(audit["candidate"], ensure_ascii=False))


if __name__ == "__main__":
    main()
