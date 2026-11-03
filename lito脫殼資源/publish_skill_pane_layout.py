"""Seal finite upstream layout evidence; never modify accepted prior batches or packet contract."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    report=load('skill-pane-layout-contract.json');verify=load('skill-pane-layout/new-evidence-verification.json')
    assert verify['range_bytes_and_hashes_match'] and all(x['match']for x in verify['preserved_prior_primary_pins'])
    assert len(verify['instruction_assertions'])==54 and len(verify['data_assertions'])==10
    packet=record(ROOT/'server-packet-delta.json');prior=record(ROOT/'native-icon-draw-contract.json')
    readme=ROOT/'skill-pane-layout/READ-ME.txt'
    readme.write_text('''第六批：技能面板位置／可見列的有限靜態合約

主要入口：../skill-pane-layout-contract.json
來源：固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
46段有VA/RVA/raw offset/SHA的有限raw範圍，54項instruction斷言、10項data斷言。每項合約結論附PE SHA、instruction VA/RVA/raw bytes及caller/vtable證據。ASM僅輔助，recovered input fingerprint與固定PE不同。

主要結論
指定47E4B0→4CC7B0/4CC7E0→4EB430→4C91E0→450230/450170鏈是釋放路徑：47E57F經pane virtual0刪除834884；pane destructor經owner virtual0刪除技能list；450230刪除rows後清count/start。不是產生row位置的render chain。

實際render chain：WORLD488860的48A9F4由836B54..836B60四個pane registry slots反向讀nonnull指標，呼叫virtual+8；若當時slot是834884且vtable與已知constructor相符，即4CCB20。該函式先畫pane frame／child controls，再於4CCCB1呼叫owner virtual+24=4CBDB0→4CD290。四個registry slots是面板登錄數，不是技能欄數；registry當時內容未動態觀測。

座標與步距
492D3A caller建owner rectangle=(left37,top43,right174,bottom248)。4C9190先傳step196x34給common list；pane建好後492DDF呼叫pane virtual+4=4CC870，將owner mode+54設1，step+20/+24均設34。因此196不是這個啟用路徑的最終cell寬。
Pane child container +14/+18由41B310初始化為(0,0)。4CC757從frame provider834BD4+E98之payload讀signedWORD width，B=640-width；4CC764/76F/77A把B加到container x以及owner left/right。4CC330拖曳callback將(dx,dy)加到container origin及owner四個rectangle edges。owner draw在4CD2C2/2CA再加globals75A998/75A99C。
用F表示目前frame origin（已含右對齊與拖曳），G表示畫面translation，格起點O=F+G+(37,43)。實際frame width、G、drag位置和fullOS輸入正規化鏈未觀測；此合約使用來源證明的symbolic值，不從Modern FRAME_WIDTH193猜原版值。沒有另抽archive。

Visible rows／columns
對正常step34、nonnegative start S、count N與迴圈中穩定fields，k=i-S：x=Ox+34*(k mod4)，y=Oy+34*floor(k/4)。4CD290在row底y+34>Oy+6*34時退出；下一格底x+34>Ox+4*34時換列。因此4欄、6列、最多24個slots，i範圍[S,min(N,S+24))。沒有bitmap/header offset或5/4 scale參與格位置公式。
初始slot count class byte75F694==2為99、==0為56、==4為40、其餘80；capacity256。這些是placeholder-inclusive list邏輯slots，不是本次learned skill數。ID=-1的placeholder不呼叫row virtual+8，但仍佔格／推進index。count<=0或start>=count不畫rows。negative start沒有lower guard（4CD300在table[start]讀取），null row也沒有guard；本組未執行這些不安全分支。

Scroll
owner+28是start slot，不是pixel displacement。4CC890/8F0為-4/+4，候選非負／小於count才更新。4CC950/9D0 page movement是-36/+36，依0/count-1 clamp後向零除4再乘4。這不是24格visible page大小。
4CC310 scrollbar row callback直接把row*4傳4CD270寫start，沒有count clamp。4CCA60位置p clamp到36..231，正常count>1時start=4*trunc(trunc((N-1)/4)*(p-36)/195)。start不必把最後24格填滿；N80/S76只有4個剩餘slots。直接callback row21可寫S84，N80時renderer早退；這是setter程式行為，不是已觀測控制項真的提供row21。

繪製、mark與明暗
此grid使用row vtable+8=4C8F90，與第五批獨立追過的text-row vtable+4=4C8EA0不同。4C8F90仍用同一shared TBT cache；time-return<834878且834870!=0時呼叫4B9A90，否則呼叫4B9980。4B9A90轉到8300E4間接目標；固定PE的指標是00000000，live target/body未知。不能宣稱所有技能格frame均走第五批closed normal-copy path，也不能把這個alternate直接命名為gray/disabled。
Normal path 4C8F90→4B9980→4BE000沿用第五批指令合約：row caller位置之外，blitter另加bitmap signed i8 dx/dy一次。grid loop沒先加該offset。實際shared91 offset(3,5)已在第五批raw vector證明，此批只讀其已封存metadata；假設caller(37,43)時normal有效起點(40,48)，不是原版畫面oracle。8300E4的offset/clip/alpha未知。
選取mark row+0C由4CD6D0設／清，4509A0可清全部mark。4CD290先画row，再於mark非零時用4B9DD0對(x-1,y-1)、width34、height34的矩形套16bit LUT。每個word由LUT[lowbyte]+LUT[256+highbyte]相加寫回。LUT83673C固定snapshot512words全0，但live LUT值未知；未冒認它是固定色的dim/border或不可用技能狀態。
Hover index owner+4C另外決定tooltip。可見matching row時，text width=6*chars（row+14或strlen），x=min(gridX,Gx+630-width)，y=gridY+32；若gridY+44>Gy+400則y=gridY-12。這些是text placement，與bitmap cell clip分開。4C8EA0 text-row tail則是caller(x+42,y+3)，可能第二行下移14px，並非grid row renderer。

Clip與邊界
這段grid的上界是迴圈停止呼叫，不是逐cell bitmap裁切；normal blitter的no-explicit-clip證據沿用第五批。外部surface virtual方法與IMG blits的完整clip仍unknown；不能擴成『原版全程沒有clip』。
輸入上游4CD730先檢owner rectangle half-open邊界。4CD480再計算S+4*trunc((y-top)/stepY)+trunc((x-left)/stepX)，只拒row>8、col>=4、index>=N，helper單獨沒有lower checks。案例的point是owner/input座標、translation明確設0；不是OS螢幕點擊結果。
原始rect寬137／高205；draw只有4*34=136、6*34=204。右邊界內1px x=left+136算col4而被拒；下边界內1px y=top+204算row6／indexS+24，可通過helper但該row沒有被draw loop畫出。4個placement案例與closed-form一致；5個hit boundary案例包含這兩個細節，未新啟原生去點擊驗證。

Modern差異
modern-comparison.json保存當前world_skill_panel.rs SHA與行號：它使用inset(27,34)、35x35 step、5/4縮放、8個supported IDs形成2列及每格42x42 clip。原版34px／4欄部分已閉合，但6列、inset(37,43)、slot order與scroll、selection LUT／timer alternate不同。5/4與42clip是Modern呈現policy，不能從此有限原版caller直接視為parity；choice.is_none dim與selected border也不能等同原版row+0C mark或time branch。

查核與邊界
placement-cases.json有4個loop/formula案例、5個hit邊界、7個scroll轉移、1個第五批actual offset metadata計算。皆code-derived，不用Modern輸出作原版oracle。
未閉合地址：8300E4 live alternate target；83673C live LUT；4CC757實際frame width；4B9997外部surface虛擬實作；48A9F4當時registry/globaltranslation/framecallstack；完整OS input normalization。精確unknown保留在主合約blocked_addresses。
本批寫入僅自有目錄；沒有native執行／GUI截圖／新抽archive／Modern修改／舊PASS資源或packet檢查重跑。第三、四、五批主契約與server-packet-delta原封存SHA保留。真正新wire差異0、新opcode0，整體脫殼、首大廳與GUI均未驗收。
''',encoding='utf-8')
    pin=record(ROOT/'skill-pane-layout-contract.json');added=[pin,record(readme)]+[record(ROOT/n)for n in ['skill-pane-layout/ranges.json','skill-pane-layout/placement-cases.json','skill-pane-layout/modern-comparison.json','skill-pane-layout/new-evidence-verification.json']]
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第六批技能面板layout：')[0]
    reader.write_text(text+'\n第六批技能面板layout：skill-pane-layout-contract.json + skill-pane-layout/READ-ME.txt。46段raw、54項instruction／10項data斷言。指定47E4B0鏈是destructor；WORLD pane render另由registry virtual+8→4CCB20→owner virtual+24→4CBDB0→4CD290。啟用後34px、4欄6列、最多24slots；正常grid row用4C8F90，含未知8300E4 time branch。輸入bottom1px可映射到未draw的row6，code-derived尚非實點結果。Modern5/4、35px、42clip與8個supported slots仍是自身policy。第三四五批與packet SHA保留，原生GUI未驗收。\n',encoding='utf-8')
    complete=load('completeness-audit.json');complete['skill_pane_layout_batch']={'evidence':pin,'finite_static_contract_complete':True,'raw_ranges':46,'instruction_assertions':54,'data_assertions':10,'step_pixels':[34,34],'columns':4,'visible_rows':6,'visible_slots_max':24,'timed_alternate_blitter_target_closed':False,'live_mark_LUT_closed':False,'native_GUI_parity_accepted':False,'new_wire_differences':0};write('completeness-audit.json',complete)
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH6_SKILL_PANE_LAYOUT_STATIC_CONTRACT_SEALED',entrypoint=record(reader),skill_pane_layout_contract=pin,finite_skill_pane_layout_contract_complete=True)
    for group in ['modern','launcher']:
        mapped={p['path']:p for p in handoff[group]};mapped.update({p['path']:p for p in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['next_steps']=['Supervisor and Modern consume skill-pane-layout-contract:34px,4columns/6rows,slot-index scroll and row+8 branch; distinguishModern scaling/clip/filtered slots',
      'Only if further parity work is authorized: qualify live8300E4 target/body and live83673C LUT; currentfixedPEvalues do not identifyreal timed/marked pixels',
      'Native GUI/input parity needsobservedframewidth/globaltranslation/registry/callstack; bottom1px case iscode-derived only',
      'Preserve acceptedbatch3/4/5; existingserverS1E timing comparison remains independent; no new wire delta']
    assert handoff['server']['packet_delta']==packet
    write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Finite skill-pane layout static contract sealed.34px/4columns/6visible rows closed;8300E4 livebody,mark LUT andnative GUI/input parity remain unaccepted.',skill_pane_layout_summary=complete['skill_pane_layout_batch'])
    progress['handoffs']['modern']='skill-pane-layout-contract.json + skill-pane-layout/*; actualgridrow4C8F90 anddynamic targetunknown explicit; noModern source modified'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet and record(ROOT/'native-icon-draw-contract.json')==prior
    names=['skill_pane_layout_audit.py','publish_skill_pane_layout.py','skill-pane-layout-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'skill-pane-layout').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'batch5_unchanged':prior,'packet_unchanged':packet,'native_GUI_accepted':False},ensure_ascii=False))

if __name__=='__main__':main()
