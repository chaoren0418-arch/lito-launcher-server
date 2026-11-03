"""Publish the validated 0.6.50 candidate and refresh source provenance."""
from pathlib import Path
import hashlib,json,re,shutil,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
native=json.loads((L/'docs/illusion-presentation-native.json').read_text('utf-8'))
assert native['nine_buff_icons'] and native['self_name_hover_branch'] and len(native['animation'])==2
baseline=json.loads((R/'baseline.json').read_text('utf-8'))
target=L/'app/server/LiTo381-SharedResources-candidate.jar'
assert sha(target)==baseline['app/server/LiTo381-SharedResources-candidate.jar']
shutil.copyfile(R/'build/candidate.jar',target)
shutil.copyfile(L/'native/ddraw.dll',L/'app/compat/ddraw.dll')
shutil.copyfile(L/'native/ddraw.dll',L.parent.parent.parent/'0-客戶端/ddraw.dll')
jarsha=sha(target)
p=L/'app/server/Start-Shared-Formal.py';t=p.read_text('utf-8');t=re.sub(r"JAR_SHA256\s*=\s*['\"][a-fA-F0-9]+['\"]",f"JAR_SHA256 = '{jarsha}'",t);p.write_text(t,encoding='utf-8')
p=S/'deployment/release-inputs.json';pins=json.loads(p.read_text('utf-8-sig'));pins['server_sha256']=jarsha;p.write_text(json.dumps(pins,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for rel in ('src/MainForm.cs','src/LiTo381Launcher.csproj','tools/build_current.py'):
 p=L/rel;t=p.read_text('utf-8');assert '0.6.49' in t;p.write_text(t.replace('0.6.49','0.6.50'),encoding='utf-8')
changes=['src/main/java/local/lito381/server/'+n+'.java' for n in ('ClientSession','EmptyMelee')]
added='src/main/java/local/lito381/server/IllusionStatusIcons.java'
archive=L/'backups/before-illusion-presentation-0.6.49.zip'
report={'version':'0.6.50','baseline_server_sha256':baseline['app/server/LiTo381-SharedResources-candidate.jar'],
 'published_sha256':jarsha,'native_dll_sha256':sha(L/'app/compat/ddraw.dll'),
 'source_changes':{rel:sha(S/rel) for rel in changes},'previous_source_hashes':{rel:baseline['server-source/'+Path(rel).name] for rel in changes},
 'added_sources':{added:sha(S/added)},'rollback_archive':archive.relative_to(L).as_posix(),'rollback_archive_sha256':sha(archive),
 'native_sources':{rel:sha(L/rel) for rel in ('native/dragon_status.inc','native/self_name.inc','native/class_skills.inc','native/ddraw_proxy.cpp')},
 'server_regression_passed':True,'dragon_presentation_regression_passed':True,'native_test':native,'no_player_sql_writes':True,'manual_test_pending':True,
 'evidence_run':'development-342996ec27224867ae6b613d29dbc26d',
 'changes':['Nine illusion buff icons follow authoritative leases; refresh/expiry/cancel and scene cleanup','S_ATTACK base action 1 and explicit heading; native maps current weapon family','Self-name pass only when native hover actor equals self'],
 'prior_validation_gap':'0.6.49 packet regression passed but player observed facing changes without native attack frames; superseded by 0.6.50 world animation test.'}
(S/'docs/maintenance/illusion-presentation-0.6.50.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=S/'tools/Verify-ProjectLayout.py';t=p.read_text('utf-8')
anchor="assert pins['server_sha256'].lower() == empty['published_sha256']"
assert anchor in t
t=t.replace(anchor,"""illusion = json.loads((S / 'docs/maintenance/illusion-presentation-0.6.50.json').read_text('utf-8'))
assert illusion['baseline_server_sha256'] == empty['published_sha256']
assert pins['server_sha256'].lower() == illusion['published_sha256']
assert sha(L / illusion['rollback_archive']) == illusion['rollback_archive_sha256']
for rel, expected in {**illusion['source_changes'], **illusion['added_sources']}.items(): assert sha(S / rel) == expected, rel
with zipfile.ZipFile(L / illusion['rollback_archive']) as z:
    for rel, expected in illusion['previous_source_hashes'].items():
        assert hashlib.sha256(z.read('server-source/' + Path(rel).name)).hexdigest() == expected
        assert expected == {**empty['source_changes'], **empty['added_sources']}[rel]
def current_source(rel, previous): return illusion['source_changes'].get(rel, previous)
""")
for old,new in [("== expected, rel","== current_source(rel, expected), rel"),
 ("== empty['source_changes'].get(rel, expected), rel","== current_source(rel, empty['source_changes'].get(rel, expected)), rel"),
 ("== empty['source_changes'].get(rel, presentation['source_changes'].get(rel, expected)), rel","== current_source(rel, empty['source_changes'].get(rel, presentation['source_changes'].get(rel, expected))), rel"),
 ("== empty['source_changes'].get(rel, presentation['source_changes'].get(rel, combat['source_changes'].get(rel, latest['source_changes'].get(rel, expected)))), rel","== current_source(rel, empty['source_changes'].get(rel, presentation['source_changes'].get(rel, combat['source_changes'].get(rel, latest['source_changes'].get(rel, expected))))), rel")]:
 t=t.replace(old,new)
# The newest layer is verified before the resolver definition.
t=t.replace("for rel, expected in {**illusion['source_changes'], **illusion['added_sources']}.items(): assert sha(S / rel) == current_source(rel, expected), rel", "for rel, expected in {**illusion['source_changes'], **illusion['added_sources']}.items(): assert sha(S / rel) == expected, rel")
t=t.replace(", *empty['added_sources']}",", *empty['added_sources'], *illusion['added_sources']}")
t=t.replace("assert old == current_source(rel, expected), rel", "assert old == expected, rel")
p.write_text(t,encoding='utf-8')
p=S/'README.zh-TW.md';t=p.read_text('utf-8').replace('現行部署：**登入器 0.6.49','現行部署：**登入器 0.6.50')
t+='''
### 0.6.50 幻術師狀態與原生呈現

補齊鏡像、幻覺：歐吉／巫妖／鑽石高崙／化身、專注、耐力、洞察、恐慌的原生狀態圖示。圖示沿用共用 Tile.pak 資源、原生提示與倒數，以伺服器現有 BUFF 租期同步；不新增戰鬥效果或 SQL 寫入。旗標陣列一起擴充，避免覆蓋相鄰原生資料。

0.6.49 的封包測試未涵蓋原生動畫，實測仍只轉向。此版改正零目標攻擊的基本動作與方向欄位，並驗證幻術師空手及奇古獸動作的動畫幀。自身名字僅在原生游標選中自身時繪製。

證據：[0.6.50 紀錄](docs/maintenance/illusion-presentation-0.6.50.json)。測試：`tools/illusion-presentation/test_icons.py`、`test_empty.py`，以及登入器 `tools/class_compat/verify_lobby.py --presentation`。玩家實測待確認。
''';p.write_text(t,encoding='utf-8')
p=L/'README.zh-TW.md';t=p.read_text('utf-8');t=re.sub(r'^# LiTo .*', '# LiTo 0.6.50',t,count=1);t+='\n目前 0.6.50：幻術師九種 BUFF 的原生狀態图示、空地攻擊的基本動作與方向，以及自身名字的游標判斷。伺服器版本與驗證請見相鄰 LiTo381-Server 的 README.zh-TW.md 與 docs/maintenance/illusion-presentation-0.6.50.json。\n';p.write_text(t,encoding='utf-8')
print('Published candidate inputs',jarsha)
