"""Verify and regenerate every new fixture from its pinned original and explicit change list. No game initialization."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
run=D/'negative-fixture-version-verification'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);rows=[]
for folder in ['source-negative-failure-fixtures','source-negative-range-fixtures','source-negative-player-fixtures','competition-pair-fixtures']:
 basis=read(R/folder/'manifest.json');out=run/folder;out.mkdir();shutil.copyfile(R/folder/'manifest.json',out/'manifest.json')
 for row in basis['rows']:
  for key in ['original','versioned']:assert sha(Path(row[key]['path']))==row[key]['sha256']
  original=Path(row['original']['path']).read_text('utf-8');actual=Path(row['versioned']['path']).read_text('utf-8');regenerated=original
  for old,new in row['changes']:assert old in regenerated;regenerated=regenerated.replace(old,new)
  assert regenerated==actual
  for old,new in reversed(row['changes']):assert new in actual;actual=actual.replace(new,old)
  assert actual==original
  p=out/Path(row['versioned']['path']).name;p.write_text(regenerated,'utf-8');assert sha(p)==row['versioned']['sha256'];rows.append(dict(basis=row,regenerated=pin(p)))
shutil.copyfile(__file__,run/Path(__file__).name);v=dict(status='passed-whole-file-inverse-and-byte-identical-regeneration',rows=rows,fixture_count=len(rows),native_cases=0,created_Main=False,created_Hub=False,created_SQL_connection=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);(run/'manifest.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8');(D/'negative-fixture-version-verification-current.json').write_text(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False,indent=2)+'\n','utf-8');print('PASS regenerated inverse fixtures='+str(len(rows)))
