from pathlib import Path
import shutil,hashlib,re
r=Path(__file__).resolve().parent;w=r.parent;server=w/'server-details-033';server.mkdir(exist_ok=True)
base=w/'server-skills-details'
shutil.copytree(base/'lib',server/'lib',dirs_exist_ok=True)
shutil.copy2(r/'candidate.jar',server/'LiTo381-SharedResources-candidate.jar')
source=(base/'Start-Shared-Formal.py').read_text(encoding='utf-8');pin=hashlib.sha256((r/'candidate.jar').read_bytes()).hexdigest()
source=re.sub(r"(?m)^JAR_SHA256 = .*",f"JAR_SHA256 = '{pin}'",source)
(server/'Start-Shared-Formal.py').write_text(source,encoding='utf-8')
shutil.copy2(base/'test_formal.py',server/'test_formal.py')
builder=(w/'build_launcher_shared.py').read_text(encoding='utf-8')
builder=builder.replace('ROOT = Path(__file__).resolve().parent','ROOT = Path(__file__).resolve().parent.parent').replace("'server-skills-details'","'server-details-033'").replace('compat-gate','compat-details-033').replace("ROOT/'launcher-src'","ROOT/'launcher-src-0.6.33'").replace('0.6.32','0.6.33').replace("ROOT/'launcher-build-inputs.json'","ROOT/'launcher-build-inputs-0.6.33.json'")
(r/'build_launcher.py').write_text(builder,encoding='utf-8')
print('Staged isolated 0.6.33 dependencies; 0.6.32 remains unchanged')
