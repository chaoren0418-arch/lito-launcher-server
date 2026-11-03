from pathlib import Path
import subprocess,hashlib,json,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher'
B=R/'build';B.mkdir(exist_ok=True);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
base=R/'inputs/base-0.6.70.jar'
receipt=json.loads((L/'docs/maintenance/release-0.6.70.json').read_text('utf-8'))
assert sha(base)==next(x['sha256'] for x in receipt['runtime'] if x['path'].endswith('LiTo381-SharedResources-candidate.jar'))
C=B/'classes';C.mkdir(exist_ok=True)
asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=';'.join(str(asm/f'{x}/9.7/{x}-9.7.jar') for x in ['asm','asm-tree'])
run=lambda *a:subprocess.run(list(map(str,a)),check=True)
source=S/'src/main/java/local/lito381/server/GmPolyMe.java'
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',base,'-d',C,source)
run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',B,R/'Patch.java')
entry='local/lito381/server/GmTravel.class'
with zipfile.ZipFile(base) as z:(B/'GmTravel.class').write_bytes(z.read(entry))
target=C/entry;target.parent.mkdir(parents=True,exist_ok=True)
run(J/'java.exe','-cp',str(B)+';'+str(base)+';'+cp,'Patch',B/'GmTravel.class',target)
client_entry='local/lito381/server/ClientSession.class'
with zipfile.ZipFile(base) as z:(B/'ClientSession.class').write_bytes(z.read(client_entry))
run(J/'java.exe','-cp',str(B)+';'+str(base)+';'+cp,'Patch',B/'ClientSession.class',C/client_entry)
changed={str(p.relative_to(C)).replace('\\','/'):p.read_bytes() for p in C.rglob('*.class')}
with zipfile.ZipFile(base) as old,zipfile.ZipFile(B/'candidate.jar','w',zipfile.ZIP_DEFLATED) as new:
 for info in old.infolist():new.writestr(info,changed.pop(info.filename,old.read(info.filename)))
 for name,content in changed.items():new.writestr(name,content)
with zipfile.ZipFile(base) as old,zipfile.ZipFile(B/'candidate.jar') as new:
 diff=[n for n in new.namelist() if n not in old.namelist() or new.read(n)!=old.read(n)]
 assert sorted(diff)==sorted([entry,client_entry,'local/lito381/server/GmPolyMe.class','local/lito381/server/GmPolyMe$Forms.class']),diff
manifest={'base_sha256':sha(base),'candidate_sha256':sha(B/'candidate.jar'),'changed_entries':diff,'source_sha256':sha(source)}
(B/'build-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8');print(json.dumps(manifest,indent=2))
