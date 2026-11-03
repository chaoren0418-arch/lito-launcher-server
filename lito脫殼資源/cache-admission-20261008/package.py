"""Package the bounded derivation; preserve old owner metadata/history."""
import datetime, hashlib, json
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OWNER=ROOT.parent
TASK='bin_resources-modern-cache-admission-input-20261008'
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def put(p,d): p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def pin(p): return dict(path=str(p),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest().upper())
now=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
a=read(ROOT/'arithmetic.json')
sources=read(ROOT/'input-sources.json')
contract=dict(task_id=TASK,owner='bin_resources',state='review',evidence_level='raw_resource_structural_static',updated_at=now,
  conclusion='The exact pinned mode1/map534 requires paging under a 16MiB decoded residence cap: 16777216 < 32601600 < 67108864.',
  arithmetic=a,input_manifest=pin(ROOT/'input-sources.json'),binding_identity=dict(mode=1,map_id=534,namespace='NATIVE_LITO_LTMAP_V1',canonical_sha256_declared=sources['canonical_sha256_declared'],geometry_digest_recomputed=False),
  evidence=[pin(ROOT/name) for name in ['derive.py','raw.stdout.txt','raw.stderr.txt','arithmetic.json','sector-parse.json','members.json','qualified-slots.json','references.csv']],
  reproduction=dict(command='C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B cache-admission-20261008/derive.py',workdir=str(OWNER),exit_code=0,raw_stdout=str(ROOT/'raw.stdout.txt'),raw_stderr=str(ROOT/'raw.stderr.txt')),
  mapping='VA/RVA N/A: external S32/IDX/PAK raw resource bytes and Rust text. Sector/member/block file offsets are reported; no PE or runtime address attribution.',
  qualification=dict(raw_pins_verified=20,complete_sector_parses=16,archive_indexes_validated=2,required_tile_members=96,selected_blocks_validated=14150,source_reference_witnesses='references.csv; first reference for each key in qualified-slots.json',decoded_shape='Each selected supported block satisfies decoder structural guards and fixed 24x24x4 output accounting. No RGBA pixels were generated.',slot_identity='(u16 tile_id,u8 slot); source offset aliasing does not merge keys',tile_count_limit='16384 default used by current CityAssets (hard parser ceiling 65536)',non_floor_coordinates='Raw u8 overlay/object coordinates are retained, including coordinates outside floor extent; city visitor does not reject them.'),
  corrected_analysis_failure=dict(exit_code=1,reason='Initial independent parser incorrectly restricted all drawable x/y to floor extent. Source inspection showed city accepts raw u8 overlays/objects. Guard removed; corrected changed script run exits0.',preserved=[pin(ROOT/name) for name in ['attempt1-failure.json','attempt1.stdout.txt','attempt1.stderr.txt']]),
  consumer_adoption=dict(owner='Modern',source='src/modern_lito/native_city_resources.rs:79',action='Select paging using the effective decoded residence/cache cap when a cache is configured. Preserve complete slot validation, cancellation and eager no-cache behavior; do not substitute decode qualification ceiling for residence capacity.',falsifiable_regression=[
    'At exact pinned mode1/map534, default decode ceiling67108864 and cache16777216: qualified_slots14150; whole_required_decoded_bytes32601600; paged=true; all selected blocks qualified before load completes.',
    'Paged initial storage starts with resident_slots0/resident_decoded_bytes0 and encoded_source_bytes_validated10597587; encoded retention is separate from decoded residence.',
    'Each frame and transient decode stays <=16777216 bytes (<=7281 resident slots), with cache metrics available. A frame demanding >7281 unique keys must reject before eviction.',
    'No-cache eager load at64MiB retains14150 slots/32601600 bytes. Merely lowering decoded ceiling to16MiB is insufficient to test default cache admission.',
    'Independently test demand equal to capacity (eager fits) and capacity plus one2304-byte slot (paging required); retain rejection for invalid cache bounds and selected missing/empty/unsupported blocks.'
  ]),
  unknowns=['Declared canonical geometry SHA is pinned to binding but not recomputed in this task. Sector bytes themselves are independently hash-verified.', 'No pixel hash, historical reference-image parity, current game runtime, GUI responsiveness, timing or native streaming claim.', 'Memory figures account decoded RGBA payload; map/index/encoded resident data, containers and allocator overhead are separate.', 'Modern adoption and supervisor review are pending; current source may change after the recorded SHA snapshot.'],
  next_substantive_task=dict(id='bin_resources-modern-visible-working-set-input-20261008',reason='Derive initial/seam camera unique visible keys from these qualified raw references to make per-frame cache-fit regressions concrete; reuse complete parse rather than replay old preview binaries.'),
  reporting=dict(reported=False,notified=False,adopted=False,reviewed=False),goal_status='active',self_accepted=False,product_complete=False,
  boundaries='No Modern/Launcher/Server source mutation, runtime or GUI/process acquisition, old R483/R486/preview execution, whole archive extraction, stopped backing/next-layer expansion or IDA retry.')
path=OWNER/'cache-admission-20261008-contract.json';put(path,contract)
manifest=dict(task_id=TASK,contract=pin(path),files=[pin(p) for p in sorted(ROOT.iterdir()) if p.is_file() and p.name!='delivery-manifest.json'],state='review',self_accepted=False)
put(ROOT/'delivery-manifest.json',manifest)
entry=dict(task_id=TASK,state='review',updated_at=now,delivery=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json'),goal_status='active',evidence_level=contract['evidence_level'],self_accepted=False,product_complete=False,reporting=contract['reporting'],blockers=[],next_ready_task=contract['next_substantive_task']['id'])
for name in ['監製進度.json','工作佇列.json']:
 p=OWNER/name;d=read(p);key='cache_admission_20261008'
 if key in d: raise RuntimeError('Existing task metadata requires explicit update, not package replay')
 d[key]=entry
 if name=='工作佇列.json':
  assert not any(i.get('id')==TASK for i in d['items'])
  d['items'].append(dict(id=TASK,owner='bin_resources',title='Independent raw resource input for Modern16MiB cache admission',**{k:v for k,v in entry.items() if k!='task_id'}))
 put(p,d)
print(json.dumps(dict(contract=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json')),ensure_ascii=True))
