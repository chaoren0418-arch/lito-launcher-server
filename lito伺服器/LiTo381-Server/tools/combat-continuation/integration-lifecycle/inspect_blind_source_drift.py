from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,difflib,shutil
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
index=read(D/'on-companion-wind-area-gates-source-drift-review-current.json');old=read(Path(index['manifest']['path']));run=D/'blind-source-drift-inspection'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);changes=[]
for row in old['Source_inputs']:
 original=Path(row['original']['path']);frozen=Path(row['frozen']['path']);assert sha(frozen)==row['frozen']['sha256']
 if sha(original)!=row['original']['sha256']:
  before=run/('prior-'+original.name);after=run/('current-'+original.name);shutil.copyfile(frozen,before);shutil.copyfile(original,after);diff=run/(original.name+'.diff');diff.write_text(''.join(difflib.unified_diff(frozen.read_text('utf-8').splitlines(True),original.read_text('utf-8').splitlines(True))),'utf-8');changes.append(dict(prior=pin(before),current=pin(after),original=pin(original),diff=pin(diff)));print(original.name,sha(frozen),sha(original));print(diff.read_text('utf-8'))
manifest=run/'manifest.json';manifest.write_text(json.dumps(dict(status='readonly-Source-drift-inspection-not-qualified',changes=changes,Source_written=False),ensure_ascii=False,indent=2)+'\n','utf-8');(D/'blind-source-drift-inspection-current.json').write_text(json.dumps(dict(path=str(manifest),sha256=sha(manifest)),ensure_ascii=False,indent=2)+'\n','utf-8')
