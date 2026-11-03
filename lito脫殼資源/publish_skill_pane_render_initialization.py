"""Publish batch7 delta in owned directory; preserve all accepted evidence."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('skill-pane-render-initialization-contract.json')
    verify=load('skill-pane-render-initialization/new-evidence-verification.json')
    assert verify['range_bytes_and_hashes_match'] and verify['all_code_ranges_fully_decoded']
    assert len(verify['instruction_assertions'])==52 and verify['range_count']==18
    assert all(p['match']and record(Path(p['path']))['sha256']==p['expected_sha256']for p in verify['preserved_prior_primary_pins'])
    assert all(record(Path(p['path']))==p for p in report['reusable_outputs'])
    packet=record(ROOT/'server-packet-delta.json');pin=record(ROOT/'skill-pane-render-initialization-contract.json')
    readme=ROOT/'skill-pane-render-initialization/READ-ME.txt'
    readme.write_text('''第七批：技能面板時間分支／選取 LUT 初始化合約

主要入口：../skill-pane-render-initialization-contract.json
固定 PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
18段raw（11段code、7段data），52項instruction斷言。每段都有VA/RVA/raw offset/bytes/SHA；code全部完整decode至instruction邊界。ASM僅作名稱與導航輔助，固定PE bytes為權威。

8300E4 靜態初始化已閉合
482088呼叫4231D0，AL非零後482098呼叫4C18E0。4C18E0依byte641298建立色表；在4C1E9A重讀格式旗標。非零時4C1EF0將8300E4寫為4C1060（RGB565），零時4C2002寫4C1170（RGB555）。兩個target的完整函式body均在固定PE中可見，這個有限分支沒有必需但缺失的private range。

必需色表在7A789C/7A809C/7A889C，各1024 WORD。a,b均0..31，index=32*a+b，q=a+floor(b/2)。blue=q；green=q<<(flag?6:5)；red=q<<(flag?11:10)，store低16bit；這三個store沒有saturation。initializer中的其他FPU/color表不在本批推導輸出，沒有延伸完整程式逆向。

時間分支 caller 為4B9A90，4B9ACC呼叫[8300E4]。第六批已封存gridrow4C8F90在unsigned(time_return)<834878且834870!=0时呼叫4B9A90，傳color0；正常分支仍是4B9980→4BE000。4B9A90第三argument是color WORD，不是普通blitter的mask flag。
color0時對covered source pixels寫通道減半結果；RGB555忽略bit15，RGB565的bit15屬red。RGB565 green輸出為2*floor(G6/4)，捨去原green低bit。不是將destination pixel混合，也不是zero-key；source word0仍寫0，uncovered regions不寫。兩個完整body都沒有coverage-mask寫入。

時間分支同樣加signed i8 dx/dy一次；height是signed i8，<=0早退，header width未用。但skip是直接加原byte、row stride直接加pitch bytes。普通4BE000使用2*floor(skip/2)、2*floor(pitch/2)。因此正常偶數skip/pitch位置相同，奇數skip/pitch會有byte位置差異。code-derived-cases.json的新synthetic RLE明列兩種pitch的write offsets；不是合法archive資源覆蓋測試，也不是native oracle。沒有重跑先前PASS icon/packet。

第二個named directconsumer是488E4F；AH bit2成立時，488E46讀503524作color。有限追蹤到此為止，live color與上游attribute語義保留unknown，未延伸其write來源。

83673C 靜態初始化已閉合
46D1DC呼叫4EC290；4EC2AC傳high callback4EC070、4EC2B1傳low callback4EC040、4EC2B6傳table83673C，4EC2BB呼叫41A2B0。Builder各做256次，寫512 WORD=1024bytes。low半表從83673C開始，high半表从83693C開始。
low[j]：B=floor(2*(j&31)/3)，GL=floor((2*(j>>5)+7)/3)，低表word=B+(GL<<5)。
high[j]：R=floor((2*((j>>(flag?3:2))&31)+31)/3)，GH=floor((2*((j&(flag?7:3))<<3)+(flag?56:24))/3)，word=(R<<(flag?11:10))+(GH<<5)。Builder另有clamp與same-index low[j] green saturation check，兩個callback在j0..255均不觸發saturation；generator保留完整builder行為。
原ASM reciprocal常數AAAAAAAB乘法/shift模型與整數floor除3，全部512項在兩種format一致。RGB555與RGB565的LE16 LUT和三張timed色表均在對應資料夾，pins在主報告reusable_outputs。

套用時沿用第六批consumer4B9DD0：out=(low[v&255]+high[v>>8])&FFFF。grid先畫icon，再對mark!=0的(x-1,y-1)、34x34矩形逐WORD轉換，包含背景與icon未覆蓋處。這是channel-specific色彩轉換；black映射555的2940或565的5280，並非灰階、固定border或alpha。不能等同Modern choice_none dim。row+0C是已閉合的selectionmark，不能從這個LUT推成技能可用性。

仍未驗收
固定PE的8300E4、83673C與三張timed色表snapshot仍為0；這只能描述dump當時內容，不否定其可見初始化程式。重建輸出條件是初始化完成，641298在初始化／繪製期間穩定。實際session是否經過caller、兩個initializer順序與first draw關係、globalbase/pitch、surface virtual+64/+80實作及runtime成功，皆未live觀測。
第六批framewidth/globaltranslation/paneregistry/OS input邊界仍保留。此批關閉靜態target body和LUT來源，沒有原生GUI／首大廳／整體脫殼驗收。沒有native/observer、注入、截圖、新archive讀取／抽取或其他組檔案修改。真正新wire差異0、新opcode0。
第三至第六批主契約和server-packet-delta保留原SHA；第六批報告中的當時unknown保持歷史證據，由本報告補上靜態初始化資料，不回寫已驗收主檔。
''',encoding='utf-8')
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第七批技能面板render initialization：')[0]
    reader.write_text(text+'\n第七批技能面板render initialization：skill-pane-render-initialization-contract.json + skill-pane-render-initialization/READ-ME.txt。18段raw／52項instruction斷言。8300E4的兩個可見write及4C1060/4C1170完整body、83673C的41A2B0 builder與4EC040/4EC070 callbacks已閉合；兩種format的512 WORD LUT與三張1024 WORD timed色表可重建。時間color0為通道減半，exact skip/pitch與普通copy的奇數行為不同；selection整矩形LUT為色彩remap。僅靜態初始化完成，live reachability／旗標epoch／surface／GUI仍未驗收。第三至第六批与packet pins不變，newwire0。\n',encoding='utf-8')
    summary={'evidence':pin,'finite_static_initialization_contract_complete':True,'raw_ranges':18,'instruction_assertions':52,'data_snapshots':7,
      'timed_targets_static_bodies_closed':True,'selection_LUT_static_source_closed':True,'format_outputs':['RGB555','RGB565'],
      'live_initialized_state_confirmed':False,'native_GUI_parity_accepted':False,'new_wire_differences':0,'new_opcodes':0}
    complete=load('completeness-audit.json');complete['skill_pane_render_initialization_batch']=summary;write('completeness-audit.json',complete)
    added=[pin,record(readme)]+[record(ROOT/n)for n in ['skill-pane-render-initialization/ranges.json','skill-pane-render-initialization/new-evidence-verification.json','skill-pane-render-initialization/code-derived-cases.json']]+report['reusable_outputs']
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH7_SKILL_PANE_RENDER_INITIALIZATION_STATIC_CONTRACT_SEALED',entrypoint=record(reader),
      skill_pane_render_initialization_contract=pin,finite_skill_pane_render_initialization_contract_complete=True)
    for group in ['modern','launcher']:
        mapped={p['path']:p for p in handoff[group]};mapped.update({p['path']:p for p in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['next_steps']=[
      'Supervisor/Modern consume batch7 finite static initialization report and LE16 outputs; batch6 layout plus batch7 conditional time/selection pixels are available for review.',
      'Preserve batches3–6 and packet SHA. No new wire/opcode delta; do not replay old PASS icon or packet checks.',
      'Static8300E4 targets and83673C LUT initialization are closed; live initializer reachability/stable format epoch and surface virtual implementations remain unobserved.',
      'NativeGUI/input parity and batch6 framewidth/globaltranslation/registry remain unaccepted; no native/observer work authorized by this finite batch.']
    assert handoff['server']['packet_delta']==packet
    write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Batch7 finite static render initialization sealed; timed targets and selection LUT are reconstructable for555/565. Live initialized state and native GUI remain unaccepted.',skill_pane_render_initialization_summary=summary)
    progress['handoffs']['modern']='skill-pane-render-initialization-contract.json + RGB555/RGB565 LE16 LUTs/tables; static conditional contract only; noModern source modified'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['skill_pane_render_initialization_audit.py','publish_skill_pane_render_initialization.py','skill-pane-render-initialization-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'skill-pane-render-initialization').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'preserved_primary_pins':len(verify['preserved_prior_primary_pins']),
      'packet_unchanged':packet,'native_GUI_accepted':False,'new_wire_differences':0},ensure_ascii=False))

if __name__=='__main__':main()
