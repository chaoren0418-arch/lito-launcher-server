import datetime,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parent;OWNER=ROOT.parent;TASK='bin_resources-map534-catalog-route-20261008'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def put(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def pin(p):return dict(path=str(p),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest().upper())
now=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat();result=read(ROOT/'route-witness.json')
contract=dict(task_id=TASK,owner='bin_resources',state='review',evidence_level='independent_raw_archive_decode_and_static_catalog_route',updated_at=now,
 product_gap='Canonical geometry SHA binds map534 geometry but contains no mode. Cache/frame regression identity also requires the raw catalog/category route and complete plan authority.',
 conclusion='Exact pinned LTtext member LTmapstr-k.tbl contains unique raw row22 for534#deathmatch#Lodun City (Death). Current pinned Modern category policy maps deathmatch to1, and the pinned authority row links exactly16 sectors to the regression plan.',
 inputs=pin(ROOT/'inputs.json'),result=result,scope=read(ROOT/'scope.json'),
 evidence=[pin(p) for p in sorted(ROOT.iterdir()) if p.is_file() and p.name not in ['package.py','delivery-manifest.json']],
 reproduction=dict(command='C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B map534-catalog-route-20261008/derive.py',workdir=str(OWNER),exit_code=0,raw_stdout=str(ROOT/'raw.stdout.txt'),raw_stderr=str(ROOT/'raw.stderr.txt')),
 raw_member_mapping=dict(index_row=245,decoded_index_range=[6864,6892],encrypted_index_covering_blocks=[6860,6892],pak_member_range=[190888,193472],decoded_table_row_range=[649,722],encoded_member_blocks_covering_row=[648,728],absolute_pak_blocks_covering_row=[191536,191616]),
 source_mapping='VA/RVA explicitly N/A for raw IDX/PAK/catalog and Rust policy. Cipher port is tied to recorded resource_cipher.rs SHA/constants; no new original fixed-PE cipher/mode function semantics or address mapping is asserted.',
 same_bytes_identity='Each input is read once into a bounded byte snapshot; SHA is computed over that snapshot and the parser consumes it. The member is sliced from the hashed PAK snapshot. No second pathname read is used for parsed byte identity.',
 cipher_boundary='IDX count4bytes is plaintext; a continuous8byte-block stream covers following old28 rows, so row245 begins4bytes inside an encrypted block. PAK member restarts cipher at member offset0. Final short block is preserved. Independent bitwise Python decrypts only bounded LTtext index and this one member; no old CLI/Modern executable/test suite runs.',
 consumer_adoption=dict(owner='Modern',actions=['For the new map534 cache/frame regression, assert exact mode1,map534,source_row22,nameLodun City (Death) with catalog member index245/offset190888 and decodedSHAE7A5D41C7CC3678E5038121DF5AF98F5544DC418AD0C4DF2EEC88F06981114FD.', 'Compare complete mode-qualified plan sector names/length/SHA against the pinned16-sector authority row before load; a geometry digest alone cannot grant another mode.', 'Reject absent/duplicate target catalog members, wrong decoded pin or wrong route/authority identity. Use the actual full qualified catalog, without manufacturing partial authentic tables or alternate map fixtures.', 'Use same parsed byte snapshot for any resource hash exposed downstream; a synthetic mutation reproduction is a unit witness and must be labeled separately from authentic resource bytes.']),
 falsifiable_acceptance=['Independent decrypt must produce exact2584bytes and decodedSHAE7A5D41C7CC3678E5038121DF5AF98F5544DC418AD0C4DF2EEC88F06981114FD from the pinned encoded memberSHA18BECECC446D1220CC54E47AB31DED3F58FEB96DD8F2056A5831CD73883ED44F.', 'Unique target row has73raw bytes[649,722), followed byCRLF; source_row22 excludes the declared67-row header. Raw fields in route-witness.json must agree exactly with catalog40 and pinned original authority manifest.', 'All16 authority sector names/byte lengths/SHA match mode1/map534 plan; no mode inference from numeric map ID or canonical SHA alone.'],
 unknowns=read(ROOT/'scope.json')['unknowns']+['No consumer adoption run, pixel parity, original process/GUI, current human interval or product acceptance proof. Supervisor review is pending.'],
 reporting=dict(reported=False,notified=False,adopted=False,reviewed=False),goal_status='active',self_accepted=False,product_complete=False,
 next_required_action='Supervisor validate and forward this mode identity supplement to Modern. Review new consumer adoption receipts or concrete consumer BIN questions; do not infer native match/clock/owner authority from catalog labels.')
path=OWNER/'map534-catalog-route-20261008-contract.json';put(path,contract)
put(ROOT/'delivery-manifest.json',dict(task_id=TASK,contract=pin(path),files=[pin(p) for p in sorted(ROOT.iterdir()) if p.is_file() and p.name!='delivery-manifest.json'],state='review',self_accepted=False))
entry=dict(task_id=TASK,state='review',updated_at=now,delivery=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json'),goal_status='active',self_accepted=False,product_complete=False,reporting=contract['reporting'],blockers=[],next_required_action=contract['next_required_action'])
assessment_path=OWNER.parent/'監製工作流/bin_resources-modern-cache-admission-input-20261008-assessment.json';assessment=read(assessment_path)
assert assessment['decision']=='accepted' and any(e['sha256']==pin(OWNER/'cache-admission-20261008-contract.json')['sha256'] for e in assessment['evidence'])
notification=OWNER.parent/'監製工作流/bin-cache-admission-to-modern-20261008-receipt.json'
for name in ['監製進度.json','工作佇列.json']:
 p=OWNER/name;d=read(p);key='map534_catalog_route_20261008';assert key not in d;d[key]=entry
 accepted=d['cache_admission_20261008'];accepted['supervisor_review']=dict(decision='accepted',evidence_scope='raw_resource_structural_static',assessment=pin(assessment_path),product_accepted=False)
 accepted['reporting']['reviewed']=True;accepted['reporting']['notified']=True;accepted['reporting']['consumer_notification_receipt']=pin(notification)
 if name=='工作佇列.json':
  prior=next(i for i in d['items'] if i['id']==accepted['task_id']);prior['supervisor_review']=accepted['supervisor_review'];prior['reporting']=accepted['reporting']
  d['items'].append(dict(id=TASK,owner='bin_resources',title='Raw LTtext mode/map route witness absent from canonical geometry hash',**{k:v for k,v in entry.items() if k!='task_id'}))
 d['continuous_product_goal_20261008']['completed_analyses'].append(pin(path));d['continuous_product_goal_20261008']['updated_at']=now
 put(p,d)
print(json.dumps(dict(contract=pin(path),delivery_manifest=pin(ROOT/'delivery-manifest.json'),accepted_prior_assessment=pin(assessment_path)),ensure_ascii=True))
