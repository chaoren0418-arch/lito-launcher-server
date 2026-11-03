from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;W=R.parent/'weakness-material'
t=(W/'build.py').read_text('utf-8')
t=t.replace("negative-material/build/candidate-negative-material.jar","weakness-material/build/candidate-weakness-material.jar").replace('18fbe46a414e29974bdb4d625b33b6aca9b3271cac0eaf9994a14ed7801f244e','9654d951236b38a68261f2efd6d6e839a52e86c0a3b7e598709cb256c702395e')
t=t.replace('negative-material/build/inputs/sources/NativeLightAction.java','weakness-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=153'","needle='&&skill!=56'").replace("needle+'&&skill!=47&&skill!=56'","needle+'&&skill!=58&&skill!=63'")
start=t.index("needle='note.contains(");end=t.index('(src/light.name)',start)
t=t[:start]+t[end:]
t=t.replace('WeaknessMaterial','GroundMaterial').replace('candidate-weakness-material.jar','candidate-ground-material.jar')
# The base is the already frozen Weakness candidate; only the output name is ground.
t=t.replace("weakness-material/build/candidate-ground-material.jar","weakness-material/build/candidate-weakness-material.jar")
t=t.replace("['ClientSession','NativeLightAction']","['ClientSession','GameHub','NativeLightAction']")
t=t.replace("assert fresh==['local/lito381/server/NativeGroundMaterialAction.class'],fresh","assert set(fresh)=={'local/lito381/server/NativeGroundMaterialAction.class','local/lito381/server/NativeGroundMaterialAction$Rejected.class'},fresh")
(R/'build.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'build-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[W/'build.py',Path(__file__)]],scope='same narrow deterministic builder and C0/inverse gates; adds private ground placement copies in GameHub; original hub methods remain unchanged'),indent=2),encoding='utf-8')
