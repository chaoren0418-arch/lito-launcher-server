"""Classify the reviewed Source delta without applying its SQL-writing code to LiTo."""
from pathlib import Path
import hashlib,json,re
R=Path(__file__).resolve().parent;D=R/'build/source-drift'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
selection=read(D/'selection-requalification.json');source=Path(selection['current_inventory']['path']);assert sha(source)==selection['current_inventory']['sha256']
patch=R/'inventory-source-delta.patch';text=patch.read_text('utf-8');rows=[]
groups={
 'private-shop-uncertain-transfer':{1107,1134},
 'item-main-row-and-deadline-publication':{1405,1432,1462,1482,1492,1512,1535,1545},
 'durable-count-rejection-and-identity':{1581,1623,1630,1641},
 'durable-inventory-and-ground-transfer':{1796,1820,1835,1854},
 'exact-item-uncertain-database-committer':{2207},
 'durable-delete-identity':{2961},
 'timed-armor-prepare-before-equip':{3053}}
for match in re.finditer(r'^@@ -(\d+),\d+ \+(\d+),\d+ @@',text,re.M):
 line=int(match[1]);found=[key for key,value in groups.items() if line in value];assert len(found)==1,line
 rows.append(dict(qualified_line=line,current_line=int(match[2]),category=found[0]))
assert {r['qualified_line'] for r in rows}==set.union(*groups.values())
v=dict(status='reviewed-current-Source-delta-scope-not-full-inventory-qualification',source=pin(source),delta=pin(patch),selector_equality=pin(D/'selection-requalification.json'),hunks=rows,
 conclusions=[
 'The four extracted material-selection blocks remain byte-for-byte equal. Selection validation is reusable only with its pinned Source and candidate receipts.',
 'Private shops, main-row insert/count/delete, trade/drop durable compensation and character_items_time confirmation are Source SQL paths. LiTo intentionally uses its independently tested memory transaction and does not import these database writers.',
 'Timed-item rollback and timed-armor prepare are behavior-relevant beyond material selection. Their LiTo import/equipment/expiry consumers still need bounded qualification; this review does not mark them complete.',
 'Source exact-item uncertain committer now distinguishes ambiguous outcomes. LiTo material IOException rollback is tested; this does not qualify every fatal/error/quarantine path of all other inventory operations.'],
 formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
(D/'inventory-drift-review.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print('Recorded '+str(len(rows))+' reviewed Source hunks; no Source database or formal mutation')
