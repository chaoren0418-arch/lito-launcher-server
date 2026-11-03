"""Count remaining legacy calls in the exact pair candidate; count is not behavior coverage."""
from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path('D:\\lineage開發\\3-lito開發\\lito伺服器\\LiTo381-Server\\tools\\combat-continuation');S=R.parents[1];B=S/'tools/resource-database/build';D=R/'weakness-material/build/census';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'weakness-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
source=D/'InspectNativeMaterialBodies.java';shutil.copyfile(B/source.name,source);classes=D/'classes';classes.mkdir(exist_ok=True)
asm=read(R/'build/integration-regression/preparation.json')['asm'];cp=';'.join(map(str,[classes,*[r['path'] for r in asm]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2')
with (D/'census.log').open('w',encoding='utf-8') as log:
 subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source)],stdout=log,stderr=subprocess.STDOUT,env=env,check=True,timeout=90)
 with (D/'bodies.tsv').open('w',encoding='utf-8') as out:subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'InspectNativeMaterialBodies',str(jar)],stdout=out,stderr=log,env=env,check=True,timeout=90)
rows=(D/'bodies.tsv').read_text('utf-8').splitlines();assert rows[-1]=='TOTAL\t36\t37'
assert not any(any(name in row for name in ['useElementalSummonSpell','useSummonSpell','useWaterLifeSpell','useDamageProtectionSpell','nativeMaterialElementalSummonBody','nativeMaterialSummonBody','nativeMaterialWaterLifeBody','nativeMaterialProtectionBody','useArmorBreakSpell','nativeMaterialArmorBreakBody','useEntangleSpell','useEraseMagicSpell','nativeMaterialEntangleBody','nativeMaterialEraseBody','useWeaknessSpell','useClassicWorldWeaknessSpell','nativeMaterialWeaknessBody','nativeMaterialWorldWeaknessBody']) for row in rows)
v=dict(status='passed-static-material-census',runtime=b['runtime'],remaining_methods=36,remaining_legacy_calls=37,output=pin(D/'bodies.tsv'),inputs=[pin(source),pin(Path(__file__))],census_is_not_proof_of_missing_behavior=True,formal_deployment_changed=False)
(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(rows[-1])
