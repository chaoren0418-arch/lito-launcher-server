"""Publish finite graphics evidence, preserving packet and accepted prior primary files."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    contract=load('native-icon-draw-contract.json');checks=load('graphics-parity/new-evidence-verification.json')
    packet_pin=record(ROOT/'server-packet-delta.json')
    assert len(checks['instruction_assertions'])==29 and all(r['match']for r in checks['raw_range_checks'])
    assert checks['selected_existing_samples']==4 and checks['selected_total_actual_samples']==5 and checks['new_archive_members_extracted']==1
    assert checks['new_fallback91_cases_checked']==2 and not checks['fallback_91_vector_missing']
    assert all(r['match']for r in checks['accepted_batch3_batch4_primary_pins_unchanged'])
    readme=ROOT/'graphics-parity'/'READ-ME.txt'
    readme.write_text('''第五批：原版 icon 解碼／繪製合約（有限靜態證據與5個實際來源樣本）

主要入口：../native-icon-draw-contract.json
固定 PE：4271D92F…AB6E9；raw bytes 優先於 recovered fingerprint 不同的 ASM。
本批保留12段對齊 VA/RVA/raw offset/原始bytes，29項指令斷言；使用4個既有抽取樣本，加上監製明確補充授權的單筆shared91抽取。總共5個實際來源樣本，沒有原版執行、注入、GUI截圖或 Modern decoder/test 執行。

可供 Modern 採用的合約
1. LTicon 使用%d.lbt、64A964 cache；shared 使用%d.tbt、648EC8 cache。未知spell fallback91經技能列4C8EA0取shared cache，不是LTicon/91.lbt。兩個namespace不可用數字91互相替代。
2. Header[0]/[1]在4BE006/12以i8有號位移；Header[3]在4BE020為有號height；Header[2]未用於本段繪製。正常既有樣本height皆1..127。每row是span_count u8，每span是skip_bytes u8、word_count u8、LE16 words。skip使用>>1；原版這段沒有odd skip reject。
3. 4231F0：回傳欄位tuple7C00/03E0/001F時641298=0；F800/07E0/001F時641298=1。shared43F9E2與LT43FAE8在pointer非零且flag非零時呼叫43DA40，將編碼word就地轉為(v+(v&7FE0))&FFFF。有效RGB555會成為RGB565，green6的低位為0，不是bit replication。raw來源、原始555與轉換後cache必須分開，避免重複轉換。
4. 4B9980成功取得表面(EAX=0)後，以63485C + y*634848 + x*2建立caller origin，向4BE000傳pitch、mask flag0、base。4BE000再加2*(dy*(pitch>>1)+dx)，每row加2*(pitch>>1)。正偶數pitch時就是目的表面pitch；壓縮row長度不等於固定width stride。
5. 4BE070/7A以REP MOVSD/MOVSB複製全部word，包括0000。未編碼的空白才保留目的值；本路徑沒有zero color-key、alpha計算或混合。可用coverage表示寫入範圍，但不是已證明的原生RGBA alpha。
6. wrapper傳入835B3C的第三個argument在本wrapper未使用，不能直接冒認為clip rectangle。本段wrapper/blitter没有顯式clip/scale；上游和外部virtual surface行為仍unknown。mask非零的另一分支是寫01到6476C4，不是alpha，而這個wrapper傳0不走它。

5個實際來源樣本
shared Sprite row51633 383.tbt：32x32，covered1021、zero54、未覆蓋3，用來區分covered black與skip。
LTicon row9 106.lbt：32x31，covered991、zero291、未覆蓋1。
LTicon row27 122.lbt：header29x26、位移(2,3)，covered497、zero0、未覆蓋257。
LTicon row217 91.lbt：32x32，covered993、zero111、未覆蓋31，只是namespace反例，不能冒認為未知spell fallback圖。
shared Sprite row90223 91.tbt：26x21、位移(3,5)，covered168、zero0、未覆蓋378；這才是未知spell fallback的實際shared namespace資源。source payload403bytes SHA A88333B37CF5D6F51960375509A0EB34EA856361A4BF906593737D04201699FB。

vectors/<namespace>-<member>/vector.json記錄來源SHA、index row、source pak/index歷史pin、header、每row/span/payload offset、word值、目的byte offset及case preconditions。對應binary：
raw.bin：原始既有抽取payload；converted-565.bin：程式碼推導的就地轉換結果。
source-words.le16 / converted-words.le16：dense vector，以width_for_vector和height解析；空洞填0，必須併用coverage.u8，1=原始span覆蓋、0=未覆蓋。
destination-flag-0.le16 / destination-flag-1.le16：64x64、pitch128、caller origin(8,8)、全表面預填5AA5h、DF=0、mask flag0、正常in-bounds。先依header位移，再複製span；covered0000覆蓋5AA5，空洞保留5AA5。
span model與獨立stream model的10個case一致。4個既有樣本的8個case與來源layout hash查核結果沿用，最後只新增shared91的2個case；已有向量本批增補時只核對pins，沒有重跑既有decoder。兩個model都是raw-byte code-derived models，沒有任何原版runtime畫面或Modern輸出作獨立oracle。

Modern差異見modern-comparison.json的當前source SHA與行號：reference_rgba沿用PakViewer零值透明與RGB888 bit replication；world_skill_panel用u8 offset、5/4比例與42x42 clip；world_inventory_panel::blit有RGBA blend。這些是Modern現行呈現policy，不能當原版4B9980→4BE000 parity已驗收。保留raw555/coverage的現行設計可提供另一路原版word-copy presentation，實作與採用交由Modern組。

fallback來源與單筆補抽
filename ASCII sum%16=Sprite12，該shard沒有member；base Sprite row90223、offset903342224、length403有91.tbt。監製補充授權必要單筆抽取後，extract_fallback91.py核對原始Sprite.pak、Sprite.idx及既有decoded index整檔SHA，只seek/read這一筆403bytes；未重抽其他member。fallback91-extraction.json記錄archive/index/member/payload指紋與授權範圍。raw與解碼payload相同，沒有payload加密codec層。fallback-91-namespace.json保留route與實際向量入口；沒有任何LTicon替代或補造pixel。

精確未完成事項
外部virtual+64/+80實作、live pixel format旗標、表面pitch、實際native callstack、上游clip/最終RGBA channel mapping與原版畫面仍未閉合。DF=0是本批case前提，wrapper/blitter没有CLD；負height、odd/negative pitch、highbit、odd skip與malformed資源沒有新增行為驗證。

驗收邊界
本批完成有限靜態追蹤與5個實際樣本的10組code-derived case，含真正shared fallback91及兩個非零位移樣本。原生graphics runtime parity、整體脫殼、首大廳與GUI均未驗收。保留第三批3個primary SHA與snapshot、第四批receive report原樣。server-packet-delta不修改，F3/F4既有採用維持，新增opcode0、wire更正0。
''',encoding='utf-8')
    pin=record(ROOT/'native-icon-draw-contract.json');added=[pin,record(readme),record(ROOT/'graphics-parity/ranges.json'),record(ROOT/'graphics-parity/fallback-91-namespace.json'),record(ROOT/'graphics-parity/modern-comparison.json'),record(ROOT/'graphics-parity/new-evidence-verification.json')]
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第五批icon繪製合約：')[0]
    reader.write_text(text+'\n第五批icon繪製合約：native-icon-draw-contract.json + graphics-parity/READ-ME.txt。12段raw／29項指令斷言支持conditional RGB555→565、signed offset、外部pitch與coverage word-copy。5個實際來源樣本／10組code-derived case；包含監製補充授權單筆403byte Sprite/91.tbt fallback資源(位移3,5)，LTicon/91不能替代。未使用Modern作原版oracle，原生runtime/GUI parity尚未驗收。\n',encoding='utf-8')
    complete=load('completeness-audit.json');complete['native_icon_draw_contract_batch']={'evidence':pin,'finite_static_trace_complete':True,'existing_samples':4,'authorized_new_members':1,'actual_samples':5,'raw_ranges':12,'instruction_assertions':29,'code_derived_cases':10,'new_fallback91_cases':2,'fallback91_actual_vector_available':True,'all_requested_samples_complete':True,'native_runtime_parity_accepted':False};write('completeness-audit.json',complete)
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH5_GRAPHICS_STATIC_CONTRACT_AND_FIVE_SAMPLE_VECTORS_SEALED',entrypoint=record(reader),native_icon_draw_contract=pin,finite_graphics_static_trace_complete=True,graphics_requested_actual_samples_complete=True)
    modern={x['path']:x for x in handoff['modern']};modern.update({x['path']:x for x in added});handoff['modern']=[modern[k]for k in sorted(modern)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['next_steps']=['Supervisor/Modern consume native-icon-draw-contract and raw-byte vectors; assess zero transparency, signed offset and conditional565 presentation policy',
      'Modern can consume actual shared91 fallback vector from Sprite row90223 and keep it distinct from LTicon91; supplemental single-member extraction complete',
      'Native display parity needs separately authorized observed surface format/pitch/draw/output evidence; current models are code-derived only',
      'Existing server independent S1E timing comparison remains pending; preserve accepted batch3/4 and packet contracts']
    assert handoff['server']['packet_delta']==packet_pin
    write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Finite native icon static draw contract sealed;5 actual samples/10code-derived cases including authorized single-member shared fallback91. Native runtime display parity remains unaccepted.',graphics_summary=complete['native_icon_draw_contract_batch'])
    progress['handoffs']['modern']='native-icon-draw-contract.json + graphics-parity/*; actual shared fallback91 vector included; no Modern source modified'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet_pin
    names=['graphics_contract_audit.py','graphics_probe.py','extract_fallback91.py','publish_graphics_contract.py','native-icon-draw-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'graphics-parity').rglob('*')if p.is_file()]+[str(p.relative_to(ROOT))for p in(ROOT/'resources/graphics-fallback91').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'contract':pin,'changed_files_sealed':count,'packet_delta_unchanged':packet_pin,'delivery':record(ROOT/'delivery-verification.json'),'fallback91_vector_available':True,'native_runtime_parity_accepted':False},ensure_ascii=False))

if __name__=='__main__':main()
