import datetime, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parent;OWNER=ROOT.parent
TASK='bin_resources-modern-visible-working-set-input-20261008'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def put(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def pin(p):return dict(path=str(p),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest().upper())
now=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
frames=read(ROOT/'frame-demands.json'); sequences=read(ROOT/'cache-sequence-model.json')
contract=dict(task_id=TASK,state='review',owner='bin_resources',evidence_level='qualified_raw_reference_static_projection_and_cache_model',updated_at=now,
 conclusion='Whole-city demand exceeds16MiB; the three selected historical camera frames fit. Exact keysets plus bounded LRU arithmetic define regressions without replaying previews.',
 inputs=pin(ROOT/'inputs.json'),frames=frames,sequences=sequences,rejection=read(ROOT/'rejection-witness.json'),
 evidence=[pin(p) for p in sorted(ROOT.iterdir()) if p.is_file() and p.name not in ['package.py','delivery-manifest.json']],
 reproduction=dict(command='C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B visible-working-set-20261008/derive.py',workdir=str(OWNER),exit_code=0,raw_stdout=str(ROOT/'raw.stdout.txt'),raw_stderr=str(ROOT/'raw.stderr.txt')),
 projection='World origin=32704+(sector-32767)*64; half_x=2*world_x-32768+x; y=world_y+y. scan_row=2*y-(half_x&~1), scan_column=((half_x+1)&~1)+2*y. Screen x=-camera_x+12*(column-anchor_column), screen y=756-camera_y+6*(row-anchor_row). Visibility uses24px rectangle strict intersection; the conservative row band uses floor division. Includes every raw floor/overlay/object reference, then deduplicates cache keys.',
 keyset_hash='SHA256 of sorted keys serialized as little-endian u16 tile_id then u8 slot; CSV also records visible reference multiplicity.',
 model='Deterministic static model of current PagedTiles::prepare from empty residency: sorted needed keys; missing determined before mutation; evict oldest non-needed stamp/key pairs; insert missing keys in sorted order; touch all needed keys in sorted order. Does not execute consumer code.',
 consumer_adoption=dict(owner='Modern',actions=['At default64MiB decode ceiling plus16MiB cache, load must page; compare actual qualified count/demand to admission contract.', 'Render the initial/repeat/seam-before/seam-after/initial sequence and compare needed_keys and cache hits/misses/evictions/resident payload to the default sequence model. Initial4271 fragments deduplicate to1709 keys; do not budget4271 copies.', 'Run the same sequence with valid capacity5750784 bytes (2496 slots); eviction deltas must be0,0,1605,406,1562 and residence stay within cap, including temporary decode.', 'At valid capacity3935232 bytes, initial frame needs3937536 and must reject before eviction/decode without changing residence/hit/miss/eviction/completed-frame counts. Phase indicators may change before refusal.', 'Use the separately pinned historical initial PNG for consumer pixel regression against existing eager behavior. This task has not generated or compared RGBA pixels.']),
 mapping='VA/RVA N/A: raw resource-derived CSV and Rust projection/cache text; no PE target or native execution claim.',
 unknowns=['Exact keysets are static source-policy projections at the five specified camera coordinates; they do not establish native camera/scene/actor ownership or runtime identity.', 'Cache sequence values are an independent arithmetic model, not observed Modern metrics. Real cancellation/error/transient allocations and concurrency still need consumer checks.', 'No pixel decode/render/hash equality, GUI responsiveness, timing or product acceptance performed.', 'Minimum/maximum cameras have0 visible keys; these bounded witnesses do not establish continuous world/visibility coverage.', 'Historical canonical geometry digest remains declared in preceding contract; next task independently verifies it.'],
 next_substantive_task=dict(id='bin_resources-modern-map534-canonical-qualification-20261008',reason='Close declared-only canonical identity using pinned sector attributes/portals and independent cross-sector direction masks; no original executable or runtime acquisition.'),
 reporting=dict(reported=False,notified=False,adopted=False,reviewed=False),goal_status='active',self_accepted=False,product_complete=False)
path=OWNER/'visible-working-set-20261008-contract.json';put(path,contract)
put(ROOT/'delivery-manifest.json',dict(task_id=TASK,contract=pin(path),files=[pin(p) for p in sorted(ROOT.iterdir()) if p.is_file() and p.name!='delivery-manifest.json'],state='review',self_accepted=False))
entry=dict(task_id=TASK,state='review',updated_at=now,delivery=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json'),goal_status='active',self_accepted=False,product_complete=False,reporting=contract['reporting'],blockers=[],next_ready_task=contract['next_substantive_task']['id'])
for name in ['監製進度.json','工作佇列.json']:
 p=OWNER/name;d=read(p);key='visible_working_set_20261008';assert key not in d;d[key]=entry
 if name=='工作佇列.json':d['items'].append(dict(id=TASK,owner='bin_resources',title='Qualified static camera working sets and protected-frame cache regression inputs',**{k:v for k,v in entry.items() if k!='task_id'}))
 put(p,d)
print(json.dumps(dict(contract=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json')),ensure_ascii=True))
