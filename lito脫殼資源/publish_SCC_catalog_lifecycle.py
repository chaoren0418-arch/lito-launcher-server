"""Publish finite Batch20 evidence; preserve Batch19 and all prior immutable primaries."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    report=load('SCC-catalog-lifecycle-contract.json');pin=record(ROOT/'SCC-catalog-lifecycle-contract.json');review=load('SCC-catalog-lifecycle/independent-review.json');v=report['verification']
    assert review['all_match']and review['primary']==pin
    assert (v['new_code_ranges'],v['new_data_ranges'],v['inherited_raw_ranges'],v['instruction_assertions'],review['aligned_in_slice_branch_targets'])==(11,8,19,65,12)
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'SCC-catalog-lifecycle/READ-ME.txt'
    reader.write_text('''第二十批：S_CC fullrow依賴的catalog UI+68模式、清空／重建與map WORD→BYTE

入口 ../SCC-catalog-lifecycle-contract.json。這是有限靜態資料流及caller補證，runtime物件／virtual／private／provider／joined與ownleave guard仍保留。
Batch19 E376B067F4E8BACD7F0DC01F2A615BE0E2B0B71C2FA3DC59E001EC0330D3F9B5維持原bytes/SHA。21既有primary pins全部保持。
沒有新wire/opcode/correction、新capture、Native執行、GUI input、SQL、provider call或Java修改；不掃全部UI方法，不追WORLD或私有保護解除。
ASM文字xref只导航，固定PE raw是指令依據；未知及已攔截事項不重新解釋。

来源与有限raw
fixedPE 7094272bytes SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
BIN1941568 SHA CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292。
ASM41179755 SHA2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A，导航fingerprint不同不作为runtime权威。
19新PE ranges=11code+8data；19既有raw只核固定byte/pin及本次必要dataflow，不重跑舊矩陣。
65expected指令、12片段內alignedbranch、6算術模型與initializer參數獨立讀回全match。
所有ranges記VA/RVA/fileoffset、完整raw_hex、單檔SHA及逐指令raw；witness逐項指出來源。Complete UI constructor112、base constructor165、scalar destructor33、destructor78、UI+6C setter18、directhelper452C20 120。
46BA10只到第一個catalogclear call，98bytes；creation46BC88..46BCE6 95bytes。没有把46BA10後續其他UI initializer或WORLD列為完整分析。

UI+68的已證來源
465610 complete到46567F ret20h，IDA endp停在basecall後的尾部仍納入fixedraw。
465649讀第7個DWORD參數EBP+20，46565B存UI+68；第8個EBP+24則存+6C。兩個欄位不同。
建立64A958的caller46BC88..46BCE6：allocate70h，非NULL時傳[21h,38h,17Ch,150h,0Ch,0Ch,0,FFFFh]給465610；46BCA5 push0正是arg7。因此這條constructor完成路徑+68=0。
Caller allocation NULL時跳constructor，以NULL存64A958；沒有Native初始化成功或可用物件證據。
Base450090 complete165bytes：count+2C、selection+28初值0，capacity+5C=256，attempt allocate400h bytes，returnpointer存+58。4C2FC0及allocator執行／返回／heapownership未qualify。
465D10只把傳入DWORD存+6C filter mask，再4509A0；不能誤稱它是+68模式setter或清空catalog。沒有擴張filter action。
本次可見class constructor/base/destructor、directsetter、caller與Batch19 rowhelpers中，目的地+68的已證MOV只有46565B。
沒有找到可接回64A958的非零+68 producer。這是有限範圍的缺口，不是全image沒有setter、+68不可變或runtime必為0的斷言。
Disk vtable4F8CB0共23slots，first465680；constructor寫它但沒有actualruntime vtable或虛擬派發資格。共享/inherited其它方法未追。

清空 helper 的精確作用
450230讀count+2C／array+58逐列，NULL略過，非NULL call virtual slot0(1)。每輪再讀count，因此callback重入/修改count/不返回仍unknown。
Callback正常返回及loop完成後450256 count=0、450259 selectedindex=0。
沒有把arraycells全部zero、釋放pointerarray、改capacity+5C、改mode+68或發布64A958；舊cells可能留pointer，activecount=0才令後續dedup掃不到舊列。
也沒有清另一widget64A98C+0C selectedpreviewWORD。selectedindex=0、catalog空和previewWORD不能混同。
Actualvirtualdestructor／heap釋放與所有權未知；靜態clear完成不等於已取得GUI結果。

可證生命週期caller／條件
1. 46A520：ECX=64A958，46A526 call450230；之後attempt C_CE206 c request via4C6AE0。自身不檢64A958 NULL，沒有真正writecommit/network順序證據。
   Direct caller469E9D是469CA0的scene1分支，需62C21C!=0且mode參數1，並且前面UI virtual/private presentation callbacks都正常返回。
   若62C21C==0，469CA0只把參數signextended寫64AF98後return，沒有clear。不能只看phase1就宣稱已清房單。
   Batch16 ownD0→469CA0(1)保持既有靜態引用；selfalias/actualcontext/ownleavecommit仍不是本次新增資格。
   Direct caller452C8C是完整452C20 helper，前面已知其它writer/preview calls返回後才呼叫46A520；沒有+68切換。
   上一層451514..451546：外部EDI comparator返回0，且64AF98==1才45153C call452C20。可證最後predicate/caller，command producer/token、EDI實際函式及user input未資格化。
   Batch14 S_CE failure對46A520的caller只沿用原封存，不重新跑其branches。
2. 既知S_C9 enter-channel result0入口：primary4B3539=71hex→4B33CC、fallback4B377D=2Bhex→4B361C，均4B309F；4B30A1 call4A7850。
   4A7850 header第一c signextends，value0經4A79AC[0]=0→4A7998[0]=4A7897，case0內4A7941 call46BA10。
   本次只封header/result0到lifecycle caller252bytes，不追其它result bodies，也不新增C9 wire schema。前面的4D6970與external cleanup callbacks執行/返回未知。
   46BA10重用條件：64A990、64A98C、64A930三個指標都非NULL→46BA67讀64A958，46BA6D call450230。64A958本身沒有NULLcheck。
   三者任一NULL→46BC88建立新catalog，arg7=0，46BCE2把return存64A958。沒有證舊64A958 dispose、其他UI初始化成功或保留哪個實際物件。
   Stop在98byte firstclear prefix／95byte newcatalog publication；入口的actualbranchstate、dynamicobject、channel/epoch、send/peer順序與後續初始化未知。
3. Destructor：diskvtable first465680→4656B0→450230→450170。Scalar destructor arg低bit1時另外4F103C。
   450170 body／actualvirtualinvocation未追；銷毀路徑不授予一個可用的postdestroy catalog/reset資格。

哪些情況忽略、先清除、允許再次新增
UI+68==0且matching fullDWORD ID仍在activearray：Batch19 465940第一match直接return，CC0所有newrow字段不replace。
上述clear已完成令count=0，或新constructor成功令count=0：下一個CC0掃不到旧activeID，依count/capacity/allocator條件嘗試append。同ID可再次新增，這不是existingrow replacement。
既有Batch18/19 fallback CFdelete完成移除matchingDWORD後亦可令下一CC0缺列；CFprimary私有效果仍unknown，沒有提出Java必須補CF。
UI+68!=0的465940會跳過dedup、count<256且capacity容許時attempt duplicateappend，但本次未接到catalog非零producer。
CC0 body本身沒有clear/listcomplete/resetbit；完整CC handler不call450230、不切+68。Defaultreset可能依賴native前置清空/新物件/缺列，不能從一包完整CC推已有列刷新。
這份證據只解釋可能的前置狀態。沒有預設Java要補發CF、CC partial或其它CC，也沒有宣稱已找出GUI根因。

WORD／表索引／row BYTE精確區分
Row-add 4659A6 load arg7 DWORD EBP+20、4659AD pushEDX，constructor463C47只讀arg7低byte、463C50存row+74。上24bits丟棄。
相鄰4659AA／4659B1傳的是arg6 attributes，不是map；不能混用offset。
Fullrow0 >=signedversion9EA8 ordinary mapWORD(非FFFF/FF9C)：MOVSXi16→ESI，scan64AFD0 count／64AFE0 stride24h records的DWORD+20 key。
EDI初值0；第一matchingindex存EDI，absent/count<=0仍0。Push完整EDI，最後row+74=index&FF，256相隔的index可碰撞。這不是直接保存原版WORD，也不是證真實map resourceID。
新版specialFFFF/FF9C：BYTE=(low8(map)-u8mode-1)&FF。FF9C mode1→9A，FFFF mode1→FD；不要把新版FF9C錯寫成FE-mode。
低於9EA8 fullrow0 ordinary：BYTE=(low8(map)+0C)&FF；special若signedversion>=7794則(FE-u8mode)&FF，否則FF。低版兩個special同投影。
Partial subtype5 dhc：ordinary始終ADD AL,0C，不走新版ordinarylookup；special>=7794仍FE-mode、否則FF。465C50掃所有matchingDWORD IDs→463D70低byte存+74；absent不新增。
所以新版fullrow mapWORD0→lookupindex&FF或default0，而partial5 mapWORD0→0C。WORD0101 olderfull0/partial5→0D只是算術示例，沒有實際packet/runtime驗收。
Maptable內容/currentgeneration、codepage/version真值、selectedpreview/render consumer、resource/WORLD binding都unknown；map WORD、map index、row BYTE各自保留。

Modeflags資料流
Fullrow0 4A7EE0 attrsbit2→low2，否則low1，OR4A7EF0 modeflags；validmode0..5→2000/200/400/1000/800/100hex。
Mode MOVSXi8後unsignedJA>5，負值/>5走未初始化stacklocal返回，不能給穩定flags fallback。
Fullrow已存在且UI+68=0去重時，上述算出的flags/map不存到既有row，仍是duplicate-ignore；這個條件不同於payload cmode=0。
Partial5 465CD0讀第一matchingrow+78（absent回傳inputroomID），ESI&FF只保留低8bits，ORmodeflags，再465C90對所有matching rows寫+78。高24bits全部替換；重複列會採firstrow的lowflags。
Absent沒有matchingsetter效果，不造row；NULL row/actualvirtual/lifetime依舊unknown。
封存LocalLobby attrs0/mode1若真的進constructor，validflags=201hex是條件式staticprojection，不能假設existingrow已寫入或native maplookup已跑。

交付／未知／分類
Newwire0/newopcode0/wirecorrection0/newcapture0。server-packet-delta B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C保持。
Lastprovedcaller：4A7941→46BA10 gate→46BA6D clear或46BCD1 constructor／46BCE2 publish；scene1 469E9D→46A520；commandpredicate45153C→452C20→452C8C→46A520。
缺失input：actual64A958與3gate pointers、+68 mutation/alias、allocator/basecallback、runtimevtable及virtualreturn、62C21C/currentphase、C9result/command實際觸發、maptable/version/generation、ownedchannel/epoch和networkwritecommit。
沒有把catalog變動推為joined/owner/member/source/physical/ownleave authority，没有Modern reset upsert等同Native。Finite段封存即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_SCC_catalog_lifecycle_complete':True,'runtime_object_virtual_private_guard_retained':True,
      'new_code_ranges':11,'new_data_ranges':8,'inherited_raw_ranges':19,'instruction_assertions':65,'aligned_in_slice_branches':12,'map_arithmetic_models':6,
      'constructor_bytes':112,'base_constructor_bytes':165,'prior_primary_pins_preserved':21,'UI68_arg7_zero_on_proved_creation_path':True,
      'catalog_nonzero_UI68_producer_qualified':False,'catalog_clear_conditions_and_last_callers_sealed':True,'map_WORD_index_BYTE_separate':True,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'new_capture_frames':0,'Java_extra_CF_CC_partial_requested':False,
      'Native_GUI_root_cause_or_actual_rowreset_qualified':False,'physical_source_joined_ownexit_authority':False,'packet_modified':False}
    complete=load('completeness-audit.json');complete['SCC_catalog_lifecycle_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text(encoding='utf-8').split('\n第二十批S_CC catalog生命週期：')[0]
    entry.write_text(txt+'\n第二十批S_CC catalog生命週期：SCC-catalog-lifecycle-contract.json + SCC-catalog-lifecycle/READ-ME.txt。UI+68=arg7，64A958新建caller傳0；C9result0三UIpointer重用gate清catalog或新建；scene1與directcaller經46A520 clear後attempt C_CE。Map WORD→signedlookupindex→lowBYTE，partial5 ordinary低BYTE+12不同；modeflags精確OR/mask。65指令/12branch/19新PE/19沿用raw/21priorpins；非零+68 producer與actualobject/private/virtual未知，newwire0，不要求Java補發，不宣稱GUI根因。\n',encoding='utf-8')
    phase='BATCH20_SCC_CATALOG_LIFECYCLE_MAP_PROJECTION_SEALED_RUNTIME_OBJECT_GUARD_RETAINED'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),SCC_catalog_lifecycle_contract=pin,finite_SCC_catalog_lifecycle_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'SCC-catalog-lifecycle').iterdir())if p.is_file()]
    for group in ['modern','launcher']:
        mapped={r['path']:r for r in h[group]};mapped.update({r['path']:r for r in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if r['path']==str(ROOT/'completeness-audit.json')else r for r in h['shared']]
    h['server']['SCC_catalog_preclear_mode_map_projection']=pin
    h['next_steps']=[
      'Batch20 finitecatalogUI68/lifecycle/mapprojection sealed. Proved64A958 constructorarg7zero; catalognonzeroUI68producer/runtimeobject/virtual/private stillunknown. NoNative/provider/SQL/capture orotherUI/WORLD expansion.',
      'C9result0 lastcaller4A7941→46BA10 three-pointergate clear64A958 ornewconstructor; scene1 nonzero62C21C anddirect452C20→46A520 clearthenattemptC_CE. actualtrigger/objects/callbackreturn/networkcommitunknown; phase1alone doesnotguaranteeclear.',
      'NewfullCC0 ordinaryWORD signextendedlookupindex truncateslowBYTE; oldfull/partial5 ordinary lowBYTE+12 differs. NewFF9Cspecial low9C-mode-1 versusold/partial FE-mode; noactualmapresourcebinding.',
      'NativeexistingID ignoredunlesspriorclear/new/absent state; repeatedIDappend/nonzero modecondition separatefromrowreplacement. Do notdemandJavaCF/CCpartial/preclearwire orclaimGUIrootcause.',
      '21priorprimarypins includingBatch19E376B067 unchanged, newwire/opcode/correction/capture0; packetunchanged, joined/source/physical/ownexitguardretained; finitebatchstop.']
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch20 finiteSCC UI68 constructor/preclear lifecycle andWORD-index-BYTE/modeflags sealed; nonzero modeproducer/actualobject/virtual/private inputsunknown.',SCC_catalog_lifecycle_summary=summary)
    progress['handoffs']['modern']='SCC-catalog-lifecycle-contract.json: arg7zero constructor, C9result0 reuse/new gate andscene1/directclearcaller, mapWORD-index-BYTE/partial5difference; Nativeobject/private/virtualguard andJavawireunchanged'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    names=['SCC_catalog_lifecycle_audit.py','review_SCC_catalog_lifecycle.py','publish_SCC_catalog_lifecycle.py','SCC-catalog-lifecycle-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'SCC-catalog-lifecycle').iterdir()if p.is_file()]
    count=seal(names,Path(__file__));print(json.dumps({'primary':pin,'changed_files_checked':count,'prior_pins_preserved':21,'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'runtime_authority_guard_retained':True},ensure_ascii=False))
if __name__=='__main__':main()
