from pathlib import Path
import shutil,hashlib,re
r=Path(__file__).resolve().parent;w=r.parent;server=w/'server-gm-034';server.mkdir(exist_ok=True)
base=w/'server-details-033'
shutil.copytree(base/'lib',server/'lib',dirs_exist_ok=True)
shutil.copy2(r/'candidate.jar',server/'LiTo381-SharedResources-candidate.jar')
source=(base/'Start-Shared-Formal.py').read_text(encoding='utf-8');pin=hashlib.sha256((r/'candidate.jar').read_bytes()).hexdigest()
source=re.sub(r"(?m)^JAR_SHA256 = .*",f"JAR_SHA256 = '{pin}'",source)
(server/'Start-Shared-Formal.py').write_text(source,encoding='utf-8')
shutil.copy2(base/'test_formal.py',server/'test_formal.py')
builder=(w/'repair-033/build_launcher.py').read_text(encoding='utf-8').replace('server-details-033','server-gm-034').replace('0.6.33','0.6.34')
(r/'build_launcher.py').write_text(builder,encoding='utf-8')
print('Staged isolated 0.6.34. Existing game and 0.6.33 unchanged.')
