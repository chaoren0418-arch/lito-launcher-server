"""Seal finite Batch22 candidate index; inherit earlier PASS without replay."""
from audit import ROOT, record, write, stamp
from seal_delta import seal
from pathlib import Path
import json

def load(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
    report=load('map-candidate-index-contract.json')
    pin=record(ROOT/'map-candidate-index-contract.json')
    review=load('map-candidate-index/independent-review.json')
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],review['total_physical_rows'],review['total_source_bytes'])==(109,268,8666)
    for r in report['prior_primary_pins']:
        current=record(Path(r['path']))
        assert current['bytes']==r['bytes'] and current['sha256']==r['expected_sha256']
    for r in report['source_pins']+report['optional_existing_display_pins']+report['outputs']+list(report['input_evidence'].values())+[report['parser']]:
        assert record(Path(r['path']))==r
    packet=record(ROOT/'server-packet-delta.json')
    assert packet==report['verification']['server_packet_unchanged']
    readme=ROOT/'map-candidate-index/READ-ME.txt'
    readme.write_text('''第二十二批：五個既有 LTmapstr 候選的完整 offline byte index

主入口 ../map-candidate-index-contract.json；catalog.json 登記五份獨立 parsed JSON。
map_candidate_index.py 是 pure-standard-library parser/query library；query 不依賴 audit.py、PE、archive、provider 或 runtime。
既有 map-table-source/existing-map-resource-candidates.json 僅前六個 nonempty lines，resources/archive-manifest.json 僅 archive/bytes/view metadata，extract_resources.py 沒有完整 # parser；equivalence-preflight.json 引用差額，沒有重抽資源。

來源與可還原保證
僅讀 Batch21 已固定五份 decoded bytes；raw/decoded 共10 pins 保持，archive manifest pin保持。
每份 raw 與 decoded 仍在既有 resources 路徑，未複寫、重抽或重解碼。parsed JSON 保留 source SHA、archive row/offset、index/pak SHA、原 raw/decoded pin。
每個 physical line 都有 1-based line、byte offset/length、content hex/SHA、包含 EOL 的 raw_bytes_hex、EOL hex/type、# delimiter/semicolon 絕對 offsets。
每個 # token 都保留原 bytes、位置與長度；空 token、末尾 token、SP/HT、comment、placeholder、未終止末列不丟棄。
CRLF/CR/LF 才分列；非ASCII 85/NUL不當換行。末尾 EOL 不增加 phantom row。
printable_ascii 只在所有 bytes 為20..7E時輸出，非ASCII維持 hex，不做 replacement/codec guessing。
L1 是沿用 resource encoding marker，未授予文字 codec。c/j/k 的既有 UTF8 view 僅 metadata 引用 assumed_code_page950，未讀作權威 label、未新增或重寫；h.tbl/h.txt 原 decode_error metadata 沿用。

分類規則與 count definitions
Header只辨認第一個非 blank physical line 的 standalone unsigned ASCII decimal（只trim SP/HT）；是 file_declared_count，不是 runtime64AFD0count。
Full-line comment 是第一個非SP/HT byte為 ;；保留原tokens與marker。不把其它位置 ; truncate成inlinecomment。
File_declared_key 只在 noncomment/nonheader # row 的第一token為 signed ASCII decimal，trim SP/HT，可保留 leadingzero與sign lexeme，integer不截斷成WORD/DWORD。
Commented_file_key 是 ;後第一token獨立 inactive numeric declaration，預設key查詢不納入。全部實際檔案無重複 active keys；介面仍保留所有重複命中與 source order。
Placeholder只標記精確 lowercase ASCII token no exist / no exsist（SP/HT trim）；两種拼字都保留，不移除、不補 key、不猜 zero/default語義。
counts 同時列 physical rows、header後physical/nonblank rows、comments、noncomment nonblank/hash/plain rows、numerickeys、placeholders、#與token數；count_minus_header 分別計算，不挑一個當runtime有效rows。

Candidate                 header  physical-after-header  comments  noncomment  numeric-key  placeholder
LTmapstr-c.tbl               67             67              12         55          47             8
LTmapstr-h.tbl               42             42               4         38           0             6
LTmapstr-h.txt               32             32               0         32           0             0
LTmapstr-j.tbl               57             55               7         48           0            14
LTmapstr-k.tbl               67             67              12         55          47             8
j.tbl header57 vs physical55差-2；noncomment48差-9；最後 physical line56 是 no exist#no exsist，沒有 EOL。
h/j.tbl 是類別token開頭，h.txt是plain opaque label bytes，沒有明示numerickey。不得把line ordinal改造成key，也不得對齊不同檔案建立跨locale/runtime映射。
c/k header相同與numeric keys相同不授予兩份label/codec/current runtime table等價資格。

Modern 可以使用的查詢介面
from map_candidate_index import CandidateIndex
index = CandidateIndex()                       # 預設本檔相鄰 map-candidate-index/catalog.json
index.list_candidates()                        # 五個 exact filenames
index.metadata('LTmapstr-j.tbl')                # header/counts/provenance/qualification
index.line('LTmapstr-h.tbl', 2)                 # 原physical row/token bytes；不是runtime row1
index.key('LTmapstr-c.tbl', 500)                # 明示key，回傳所有matches；不fallback ordinal
index.key('LTmapstr-c.tbl', 571, True)          # 明確 opt-in inactive comments
index.label_metadata_for_key('LTmapstr-c.tbl', 500) # 回傳全部tokens，label_column_qualified=False
index.token('LTmapstr-h.tbl', bytes.fromhex('d6f2cca8bebabcbcb3a120')) # exact opaque token

CLI（在本組目錄）
python map_candidate_index.py list
python map_candidate_index.py --candidate LTmapstr-c.tbl label-metadata 500
python map_candidate_index.py --candidate LTmapstr-j.tbl metadata
python map_candidate_index.py --candidate LTmapstr-h.tbl line 2
python map_candidate_index.py --candidate LTmapstr-c.tbl --include-comments key 571
python map_candidate_index.py --candidate LTmapstr-c.tbl token-ascii town
python map_candidate_index.py --candidate LTmapstr-h.tbl token-hex d6f2cca8bebabcbcb3a120
可用 --catalog 指定搬移後catalog，parsed JSON須同目錄；查詢verify parsed size/SHA及candidate/sourceprovenance。catalog本身信任需沿primary/catalog pin，不接受suffix別名或默認locale。
token-ascii僅strict ASCII；其它label用token-hex，query不解碼非ASCII、不猜canonicaltitle欄位。完整 query-examples.json提供命中/無命中、inactivecomment、opaque label、j最後列例。
key('LTmapstr-j.tbl',500)及key(...,0)皆空matches，不從h/j的類別或physical位置合成數字。

獨立檢查／交接邊界
independent-review.json：109 checks全match；268 physical rows、8666 bytes，regex獨立partition/offset/token intervals、每byte還原、fresh parser deterministic equality、10來源pins及23 priorprimarypins保持。
有限 synthetic fixture 驗證 mixedEOL、blank、leadingzero/signed/duplicate keys、empty/trailing #、comment、inlinesemicolon、85/NUL/FF opaquebytes、兩placeholder拼字、unterminatedlastline、exactquery與非法fallback拒絕。
只新增完整 offline metadata。newPE/newwire/newopcode/wirecorrection/newcapture=0；server-packet-delta SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。
Modern可展示來源候選metadata/ASCII token或opaque hex，但不能宣稱native label codec、canonical map title、actuallocale/provider selection、64AFD0/64AFE0 currentgeneration、recordcount/order、runtime lookupindex、lowBYTE ormap/resourceID綁定已合格。
沒有 Native/GUI/provider/SQL/WORLD/privatebody/Java/Modern/BIN 修改，不要求新wire，不重跑Batch20/21 PASS。有限本批完成即停。
''',encoding='utf-8')
    summary={
        'evidence':pin,'finite_offline_map_candidate_index_complete':True,
        'runtime_resource_binding_guard_retained':True,'candidate_files':5,
        'physical_rows':268,'source_bytes':8666,'independent_checks':109,
        'raw_decoded_pins_preserved':10,'prior_primary_pins_preserved':23,
        'explicit_numeric_key_candidates':['LTmapstr-c.tbl','LTmapstr-k.tbl'],
        'ordinal_key_synthesis_or_cross_candidate_alignment':False,
        'codec_inference_or_redecode':False,'archive_reextraction':False,
        'new_PE_ranges':0,'new_wire_differences':0,'new_opcodes':0,
        'wire_corrections':0,'new_capture_frames':0,'packet_modified':False,
        'Native_GUI_provider_SQL_Java_Modern_BIN_modified':False,
        'Batch20_Batch21_PASS_replayed':False,
    }
    complete=load('completeness-audit.json')
    complete['map_candidate_index_batch']=summary
    write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md'
    text=entry.read_text(encoding='utf-8').split('\n第二十二批map candidate index：')[0]
    entry.write_text(text+'\n第二十二批map candidate index：map-candidate-index-contract.json + map-candidate-index/READ-ME.txt + map_candidate_index.py。五既有LTmapstr分檔完整byte rows/tokens/comments/placeholders/counts；c/k明示47numeric keys，h/j/h.txt沒有numerickey；j header57 vs55 physicalafterheader且末列未終止。268physicalrows/8666bytes/109checks/10sourcepins/23priorpins全match。Modern可查exactfilename/explicitkey/physicalrow/exacttoken metadata，無ordinal/locale/index/mapID映射或codec猜測。Runtime/provider/resourceguard保持，newwire0，不重跑Batch20/21PASS。\n',encoding='utf-8')
    phase='BATCH22_OFFLINE_MAP_CANDIDATE_BYTE_INDEX_SEALED_RUNTIME_RESOURCE_BINDING_OPEN'
    h=load('handoff.json')
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),map_candidate_index_contract=pin,
             finite_offline_map_candidate_index_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'map-candidate-index').iterdir())if p.is_file()]+[record(ROOT/'map_candidate_index.py')]
    for group in ('modern','launcher'):
        mapped={r['path']:r for r in h[group]}
        mapped.update({r['path']:r for r in added})
        h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if r['path']==str(ROOT/'completeness-audit.json')else r for r in h['shared']]
    h['server']['known_offline_map_candidate_index_boundary']=pin
    h['next_steps']=[
        'Batch22 finite offline candidate index complete: catalog + five parsed JSON + pure stdlib map_candidate_index.py. Exact filename/explicit numeric key/physical row/exact byte token query only; no ordinal fallback or cross-candidate alignment.',
        '268 physical rows/8666 source bytes reconstructed exactly; all # tokens, whitespace, empty/trailing token, comment, placeholder, EOL retained. j header57 vs55 after-header rows and unterminated last line remain visible.',
        'c/k each47 explicit active numeric keys; h/j/h.txt none. Full comments/inactive571 separate; no canonical title column, locale codec, default file selection or runtime key qualification.',
        'Modern may query/display candidate metadata and ASCII/opaque hex; actual636584/provider/parsing/64AFD0/AFE0 count/order/generation/runtimeindex/lowBYTE/mapresourcebinding stay unknown. No guessed nonASCII decoding; existing assumed950 display refs only.',
        '23 prior primary pins and10 raw/decoded asset pins unchanged; newPE/wire/opcode/correction/capture0, packet unchanged. Native/GUI/provider/SQL/WORLD/privatebody/Java/Modern/BIN0; Batch20/21 PASS not replayed. Finite batch stop.',
    ]
    assert h['server']['packet_delta']==packet
    write('handoff.json',h)
    p=load('監製進度.json')
    p.update(updated_at=stamp(),phase=phase,
             current_task='Batch22 five fixed LTmapstr byte-preserving offline indices and Modern metadata query interface sealed; runtime/resource binding remains unknown.',
             map_candidate_index_summary=summary)
    p['handoffs']['modern']='map-candidate-index-contract.json / map_candidate_index.py: exact candidate/filekey/physicalrow/opaque-token metadata query; preserve all bytes and counts, no runtime index/locale/codec/mapresource binding.'
    p['next_steps']=h['next_steps']
    write('監製進度.json',p)
    names=['map_candidate_index.py','map_candidate_index_audit.py','review_map_candidate_index.py',
           'publish_map_candidate_index.py','map-candidate-index-contract.json','handoff.json',
           'completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'map-candidate-index').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,
                      'delivery':record(ROOT/'delivery-verification.json'),
                      'prior_primary_pins_preserved':23,'source_pins_preserved':10,
                      'packet_unchanged':packet,'runtime_resource_binding_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
