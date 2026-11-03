"""Record development separately from historical runtime qualification."""
from pathlib import Path
from datetime import datetime, timezone
import csv
import json
import zipfile
from build_development_closeout import ROOT, SERVER, pin, checked, write


def main():
    build = ROOT / 'build'
    previous = json.loads((build / 'source-program-update-progress-current.json').read_text('utf-8'))
    old = json.loads(checked(previous['manifest']).read_text('utf-8'))
    checked(previous['runtime'])
    names = ['source-area-pair-first-build', 'source-area-pair-first-common-reproduction',
             'source-area-pair-first-verification', 'source-completion-inventory',
             'source-next-producer-contracts', 'source-invocation-ABI-census',
             'source-weapon-next-reference', 'source-weapon-overlay-checks']
    evidence = {}
    for name in names:
        value = json.loads((build / (name + '-current.json')).read_text('utf-8'))
        checked(value['manifest'])
        checked(value['runtime'])
        evidence[name] = value
    area = evidence[names[0]]
    if any(v['runtime']['sha256'] != area['runtime']['sha256'] for v in evidence.values()):
        raise ValueError('Historical evidence has divergent candidate hashes')
    release_candidate = json.loads((build / 'development-release-compat-current.json').read_text('utf-8'))
    candidate = json.loads((build / 'development-skill-effects-current.json').read_text('utf-8'))
    manifest = json.loads(checked(candidate['manifest']).read_text('utf-8'))
    checked(candidate['runtime'])
    npc_review = json.loads((build / 'development-npc-numeric-review-current.json').read_text('utf-8'))
    npc_manifest = json.loads(checked(npc_review['manifest']).read_text('utf-8'))
    if npc_review['runtime']['sha256'] != candidate['runtime']['sha256']: raise ValueError('NPC review must refer to current candidate')
    sql = old['readonly_SQL_consumers_byte_retained']
    with zipfile.ZipFile(checked(sql['base'])) as source, zipfile.ZipFile(candidate['runtime']['path']) as target:
        for entry in sql['entries']:
            if source.read(entry) != target.read(entry):
                raise ValueError('Readonly SQL mapping changed: ' + entry)
    inv = json.loads(checked(evidence['source-completion-inventory']['manifest']).read_text('utf-8'))
    artifacts = {Path(ref['path']).name: ref for ref in inv['artifacts']}
    run = build / 'development-closeout-progress' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    run.mkdir(parents=True)
    with checked(artifacts['skills.tsv']).open(encoding='utf-8') as stream:
        skills = list(csv.DictReader(stream, delimiter='\t'))
    control_inventory = json.loads((build / 'development-control-inventory-current.json').read_text('utf-8'))
    control_manifest = json.loads(checked(control_inventory['manifest']).read_text('utf-8'))
    if control_manifest['runtime']['sha256'] != candidate['runtime']['sha256']:
        raise ValueError('Control inventory must refer to the current development candidate')
    with checked(control_inventory['matrix']).open(encoding='utf-8') as stream:
        controls = list(csv.DictReader(stream, delimiter='\t'))
    skill_inventory = json.loads((build / 'development-skill-inventory-current.json').read_text('utf-8'))
    skill_manifest = json.loads(checked(skill_inventory['manifest']).read_text('utf-8'))
    if skill_manifest['runtime']['sha256'] != candidate['runtime']['sha256']:
        raise ValueError('Skill routing inventory must refer to the current candidate')
    with checked(skill_inventory['matrix']).open(encoding='utf-8') as stream:
        routing = {r['skill_id']: r for r in csv.DictReader(stream, delimiter='\t')}
    for row in skills:
        route = routing[row['skill_id']]
        for key in ['category', 'source_type', 'target', 'target_to', 'range', 'area', 'through', 'current_entry_locators', 'static_path_state']:
            row[key] = route[key]
        row['development_status'] = 'inherited-implementation-needs-per-skill-review'
        if row['skill_id'] in ['76', '167']:
            row['development_status'] = 'PC-natural-controlled-first-independent-output-early-release-natural-and-PC-commit-guards-implemented-tests-deferred'
        row['final_candidate_tests'] = 'deferred-by-user'
    for row in controls:
        row['final_candidate_tests'] = 'deferred-by-user'
    for name, rows in [('skills.tsv', skills), ('controls.tsv', controls)]:
        with (run / name).open('w', encoding='utf-8', newline='') as stream:
            writer = csv.DictWriter(stream, fieldnames=list(rows[0]), delimiter='\t')
            writer.writeheader()
            writer.writerows(rows)
    groups = old['remaining_work_groups']
    for group in groups:
        group['verification_status'] = 'deferred-by-user'
        if group['id'] == 'materials':
            group['routing_review'] = '807 rows retained: 181 player-selector skills have current entries and symbolic packet-dispatch paths, including independent teleport5; 38 placeholders and 588 non-player data rows remain in consumer review. Per-skill effects/semantics are not inferred from routing.'
        if group['id'] == 'math-and-status':
            group['development_delta'] = 'Quest/Time Attack magic uses one immutable skills row per readiness/payment and per original target dice loop. HP cost leaves one HP; original record getters, scheduling and RNG loops retained. Birth-captured mobskill reuse gates all typed row selection and records cooldown after eight original schedule sites. Two original target resistance reads now use Source metadata attribute; coefficient and resistance arithmetic are retained. Visual/Ground costs now share Source HP/MP snapshots. Natural L1Monster recovery is wired to the existing Hub executor with birth-captured Source data. WORLD ordinary magic, Ckew magic/status costs and dice now use readonly skills snapshots. Four Quest/Time normal spell lanes now check ordinary silence and HP/MP after selection, reject into original physical fallback, and pass one cost snapshot to the retained schedule body. Birth-frozen positive width/height areaMagicAttack routes now bypass normal NPC cost/silence admission and supply zero HP/MP to the retained scheduling bodies; The actual encounter now supplies the force-indexed Quest or owning Time Attack arena for area-only map93 rejection before schedule/reuse. Two damage ingress target lists now use first-member actual-map/screen/static-LOS checks before RNG; The proven TYPE_GMBUFF empty-self-target branch now records successful casts without NPC damage/RNG; empty-self-target NPC action output is now success-bound; the empty-self-target Definition branch now completes synchronously under the original Hub/encounter locks before existing zero-cost sleep/reuse scheduling, with zero successes taking the same-attempt physical fallback. Nonempty self execution, visual/ground ingress and natural use-count/finish-AI semantics remain open. Shared BATTLE LOS now uses three exact Source ray methods plus the existing readonly terrain/copy-map assets instead of walking-only Bresenham; room doors are now bound for the two area damage target paths; other dynamic-door consumers, map-specific arrow overrides, explicit GM-invisibility/ghost adapters and exact Source spatial iteration order remain open. Controlled companion mobskill/SP consumer coverage and other NPC classes remain open; no immediate self-cost HPBAR is introduced.'
        if group['id'] == 'npc-factory-ai':
            group['development_delta'] = 'Birth mobskill routes capture Source reuseDelay. Two original row gates and eight accepted schedule sites share existing actor cooldown maps; cooling probability/summon draws are skipped. Natural L1Monster HP/MP recovery now runs at Source modulo cadence on the existing Hub executor with frozen birth data and close cleanup. Pet/summon BATTLE and WORLD recovery now consume birth data under original owner/action/lifetime/Hub/bag/pet/status locks; owner HP display uses original complete encryption routes after mutation locks release. WORLD checks original map/instance registration and placement role/scene. Accepted pet evolution now updates actual factory proof with the replacement profile, preserving new-template recovery and invalidating old first-participant receipts. Controlled birth now freezes complete mobskill facts and recovery together; a compiled Source selection core preserves trigger/retarget/RNG order, but live actor/effect adapters and tick binding remain open. Actual companion state now owns counts/reuse deadlines; construction/evolution, terminal writes and both rest ticks are bound, and old leases expire on AI finish. BATTLE accepted drain completion now resolves the actual first companion/master proof and applies bounded HP with guarded owner output; spell selection/payment/dispatch, WORLD effects and remaining no-target cases are open. Natural empty-self-target area casts now complete before original sleep/reuse scheduling; zero successes reach same-attempt physical fallback, while completed receipts avoid later recasting. Birth-frozen triggerCount now gates after original chance predicates, summon readiness uses the original cap semantics, and proven completed empty-self casts increment the original actor map once. The current no-target discovery resets counts only for a live original natural monster whose AI can run and has no registered same-map PC within radius20; reuse remains intact. Other cast-success increments, full Source selection, other noTarget finish causes, other NPC classes and broader lifecycle remain open.'
        if group['id'] == 'paired-lifecycle':
            group['development_delta'] = '76/167 PC/natural/controlled first participants, seven pre-calculation iterator filters, independent PC/NPC final outputs, early receipt cleanup, ten natural NPC and six status mutation guards, and first factory/profile identity implemented. Existing controlled class-failure rules retained. Other producer coverage open; no new runtime tests.'
        if group['id'] == 'external-controls':
            group['development_delta'] = '252 rows traced; 52 literal and 31 dynamic keys located, 141 source-reference applicability reviews and 28 unresolved-reference reviews remain. Optional effect_skill_id now enters the original readonly target-row snapshot, preserving skills.id and old-schema compatibility; effect/status consumers remain open. Inventory is bound to current candidate SHA; no runtime qualification.'
        if group['id'] in ['formal', 'formal-compatibility', 'formal-0.6.71'] or 'Compose formal0.6.71' in group['work']:
            group['status'] = 'implemented-static-compared-pending-tests'
            group['work'] = '0.6.71 四類別、11方法項目已隔離整合；正式 DLL／圖示／狀態資源依賴 SHA 已核對。未部署，執行期與畫面驗收另行安排。'
    state = {
        'status': 'active-development-closeout-in-progress-tests-deferred',
        'goal': {'created': True, 'status': 'active', 'thread_id': '01a1837e-b07b-71c0-9bf9-07297a4c820f'},
        'runtime': candidate['runtime'], 'build': candidate['manifest'],
        'release_compatibility_build': release_candidate,
        'control_inventory': control_inventory,
        'skill_routing_inventory': skill_inventory,
        'npc_numeric_review': npc_review, 'npc_area_routes': npc_manifest['offline_area_routes'],
        'skill_effect_references': npc_manifest['offline_area_routes']['effect_references'],
        'historical_common_progress': previous,
        'newer_area_source_reconciliation': {
            'status': 'hashes-checked-and-compile-only-reproduced',
            'runtime': area['runtime'], 'receipts': evidence,
            'historical_current_aliases_rewritten': False,
            'historical_alias_divergence_retained': True,
            'verification_inherited_only_for_exact_historical_SHA': True,
        },
        'actual_changes': [
            '補齊Source效果身分對照：53個模式登記與807技能逐列解析已凍結；7個效果參照中4個有模式、3個走無模式狀態路徑。效果參照不是最終寫入狀態：49014→CONFUSION202的PC模式寫SILENCE64並送1339訊息，NPC模式無效果；49017→CURSE_PARALYZE33使用外控準備／麻痺時間及兩階段狀態。裝備proc自訂秒數大於0時直接寫原技能ID，不使用effect_skill_id；0秒才走原技能TYPE_GMBUFF。這是來源語意與盤點工具更新，候選JAR未變，消費端仍待實作。',
            '補上Source可選effect_skill_id讀取：保留原skills.id用途與前九欄位順序，在原唯讀交易中由同一ResultSet將效果參照附到原目標Row，所有資料驗證成功後才沿原Map發布；缺欄依Source採0，不新增連線／查詢或schema修改。一般施法的無模式狀態ID採正值參照，模式本體與自訂秒數proc另依Source規則；模式解析保留原技能優先、缺少時只查一次效果參照，不改寫成本／時長／圖像／目標資料。原record ABI與讀取／關閉／發布本體靜態保留，雙重建SHA一致。離線效果參照另有逐列矩陣；實際模式／狀態消費端仍待接入，沒有宣告別名技能已可完整施放。',
            '自然NPC出生時以同一唯讀mobskill快照保存triggerCount，兩個原機率判定後接入次數上限，保留既有RNG。召喚沿用原計次表與增量，readiness改用出生上限；非正值不限制次數，未知資料保留原本體。已完成的空目標自施結果在原遭遇鎖內成功排程後、reuse前僅計一次；不把其他尚未接妥的排程當成施法成功。原無目標探索只有在目前L1Monster、實際同地圖20格內無已登記PC、原活動階段／動作時間及七種睡眠／定身檢查通過時清次數，保留冷卻；已死／觀戰／隱身PC仍算可見者。首次NPC／房間／生命期均重查，1531個原方法靜態逆向保留、雙重建SHA一致。其餘施法成功計次、完整選列與其他AI結束原因仍待接線。',
            '已證明無玩家效果的空目標TYPE_GMBUFF改在原Hub／遭遇鎖內完成：逐一核對首次NPC與玩家、實際地圖、畫面與動態門LOS後記錄成功。至少一次成功才進入既有零成本sleep／reuse排程；全失敗在排程前走原同次普攻，不重選或補造actor。原tick攜帶已完成結果，在原主目標失效門檻之前獨立送出；主目標離場不撤銷其他玩家已完成的自施，仍須原NPC、房間及生命期成立，後續傷害入口不再重施；一次性領取與輸出拒絕保護重送，結果於輸出前清除，當次Context在全部正常／異常出口還原，沒有待完成actor狀態或新計時器。3530個原方法靜態逆向保留、雙重建SHA一致。使用次數與無可見PC的AI結束重設後續已有部分接入；非空自施與視覺／地面分支仍缺。',
            '空目標TYPE_GMBUFF分支的NPC動作改接實際施法結果：兩個傷害迴圈在原攻擊序號／圖像建立前，零成功或缺首次Selection即不輸出；成功才使用mobskill原始ActId（非正值採Source輔助動作19），發給同地圖10格內首次接收者。NPC首次owner／generation／factory／life／座標與接收者role／life／房間／地圖在原八條完整加密路徑重查；輸出不持有Selection／Scope／目標名冊，施法紀錄在送出前清除。2511個原方法靜態逆向保留，雙重建SHA一致。非空命中計畫保留原本體；此項僅記錄輸出改動；sleep／reuse與同次物理備援後由同步完成端接入，使用次數語意仍未完成。',
            'TYPE_GMBUFF補入已證明的空目標自施分支：TYPE_ATTACK且target非none時，Source拒絕攻擊自己，目標清單為空；不計算傷害／RNG或發送玩家效果，但handleCommands仍回傳成功。兩個NPC範圍傷害迴圈在首次身分檢查後記錄個別施法成功／拒絕，並於數值讀取前跳過原NPC傷害；結果只保存在當次Selection，與HP接受結果分開。光亮／火牆／生命之泉及四個立方體有迴圈前效果，明確排除。2490個原ClientSession／Selection方法靜態保留，雙重建SHA一致；離線SQL的33筆範圍魔法列中11筆符合此分支，非實際DB驗收。空目標成功後NPC動作已另接結果；非空自施與預先提交的排程仍待修正。',
            '兩個NPC範圍傷害入口補上動態房門／攻城門LOS：沿用首次Selection取得當前房間門快照，每次判斷建立獨立door map的短期地形層，只共用唯讀tile／箭線陣列與界限；呼叫原Source setPassable套用關門方向的中心與兩側格，再執行完全相同的Source射線。開門／未啟用不覆蓋原始地形，不把開門當成強制箭線放行；門只屬於原房間base map，射線後重查房間身分、開始狀態、地圖與門值。共用arena快取及原角色資料未寫入，四個原目標helper方法靜態逆向保留，其餘既有JAR項目逐位元相同，雙重建SHA一致。其他LOS消費端的門接線與地圖專屬箭線覆蓋仍待完成。',
            'Quest／Time Attack兩個NPC魔法傷害入口改為先依出生時固定的正寬高判定Source範圍分支，再從同房首次成員取實際同地圖、畫面內且靜態LOS可見玩家，不再受原方向／半徑／主目標距離限制；一般隱身不排除。每個原傷害迴圈在數值讀取與RNG前重查首次玩家role／life／座標及實際NPC登記／owner／generation／factory／life／座標；已知範圍列缺首次證明則拒絕。2486個原ClientSession方法靜態逆向保留，其餘既有JAR項目逐位元相同，雙重建SHA一致。這只完成兩個傷害入口的目標准入；TYPE_GMBUFF、自施成功回傳／排程、視覺／地面分支、其他LOS消費端的動態門、明確GM隱身／ghost轉接及Source空間遍歷順序仍未完成。',
            '共用BATTLE LOS入口已接Source逐格邊界射線與完整箭線資料，修正原本僅以移動阻擋邊做Bresenham階梯取樣。L1Character三個方法只替換地圖型別，其餘文字逐字核對；沿用既有唯讀terrain snapshot、原始雜湊驗證資源及copy-map定義。arena只保留依snapshot身分替換的純地形快取，不持有角色／房間／Context或計時器；Source未載入時保留原路徑，已載入卻缺定義則拒絕。154個原方法及所有既有資源／其他JAR項目靜態保留，雙重建SHA一致；兩個NPC傷害入口已接可見目標、Source LOS與房門／攻城門覆蓋；其他消費端動態門、地圖專屬箭線覆蓋及視覺／地面範圍入口仍待接線。',
            '範圍魔法補上實際NPC地圖93拒絕：四條選中施法接點在原遭遇鎖內傳入實際encounter；Quest依原怪物force索引取得arena，Time Attack取所屬arena。正寬高範圍列在地圖93或缺少權威地圖時走原物理備援，發生於魔法准入／扣款／排程／冷卻／攻擊物件建立之前；一般與舊路徑保留。332個原遭遇方法靜態逆向保留，其餘既有JAR項目逐位元相同，雙重建SHA一致。可見目標與靜態LOS已接兩個傷害入口；獨立TYPE_GMBUFF、視覺／地面分支及成功後排程仍待接線。',
            'Source範圍魔法的NPC免成本與沉默分支已接入三個既有准入邊界：以出生時固定的mobskill正寬高辨認，選取保留冷卻，選中後核對技能metadata，四條原排程接收零NPC HP／MP；傷害值仍由原輸出端獨立取得。八個原准入方法靜態逆向比對保留，其餘既有JAR項目逐位元相同，雙重建SHA一致。各可見玩家自身執行TYPE_GMBUFF與成功後動作／排程仍未接線；兩個傷害入口已接靜態LOS，地圖93拒絕另已接入，不宣告完整範圍語意完成。',
            'Quest／Time Attack四個一般魔法／視覺／地面分支調整為先選列，再處理一般沉默與HP／MP。依出生時固定的mobskill寬高區分一般與areaMagicAttack：一般沉默只取消並解除當次施法；成本不足或沉默拒絕走原物理備援，不重選、不扣款、不寫冷卻。四個原排程完整副本共用一次成本快照；靜態逆向保留502個原方法，雙重建SHA一致。正寬高範圍魔法另接免NPC成本分支；其施法者／效果／成功後排程與一般區域沉默政策仍待核對，未宣告完整NPC語意完成。',
            'BATTLE受控NPC回吸完成端已接原NPC魔法傷害接受流程：用首次寵物View／Master證明解析實際actor，缺證明即拒絕，不代用主人或新造怪物快照。沿用成功接受後只執行一次的規則，在主人行為鎖及lifetime／Hub／名冊／pet／status鎖內重查後補HP；離開寫入鎖才送主人血條，八條完整加密路徑於加密前重查身分與比例。自然怪物本體保留、雙重建SHA一致；實際寵物技能選取／扣款／施法與WORLD回吸尚未完成。',
            '實際寵物／召喚物新增專屬mobskill次數與冷卻狀態；出生／進化從固定工廠資料建立，四個原HP／active終止接點及BATTLE／WORLD休息tick清次數、保留冷卻，普通換目標不重設。狀態租約核對actor／factory／life／generation並要求原寵物與狀態鎖；AI結束或進化使舊租約失效。靜態逆向保留2625個原方法，雙重建SHA一致；效果轉接與其餘noTarget分支仍未完成。',
            '受控NPC出生與進化工廠現在從同一次唯讀快照捕捉回復、INT／WIS／family、施法速度與完整mobskill列；保留列順序、類型、各觸發條件、換目標、範圍、倍率、技能／動作／特效、召喚、變身與冷卻，資料脫離可變模板。Source選取核心已編譯並保留亂數與條件順序，但實際actor／效果轉接與tick啟用仍未完成。',
            '修正寵物進化後實際factory stamp仍指向舊NPC的缺口：在原evolve鎖內、原profile寫入前捕捉新模板證明，並在同一寫入接點更新為可見的新stamp。進化後回復採新NPC資料，舊profile／factory延後效果失效；原進化驗證、重設、物品與持久化流程保留。',
            'WORLD普通魔法、Ckew魔法與Ckew狀態三個既有成本分支接入唯讀skills首次快照；同次HP／MP檢查與扣款共用數值，HP至少留1，原Population鎖、選取、RNG、冷卻與輸出保留。原WORLD骰值迴圈四個讀取接點共用一次快照；靜態逆向比對保留269個原方法。',
            '新增純編譯／打包入口，重建既有範圍候選得到相同 SHA；不呼叫既有測試流程。',
            '依正式來源修補戰鬥候選的 Protocol、ClientSession、GmTravel、NativeArmorStatus；補入四個正式 helper。',
            '靜態比對11個正式方法項目全數相同，其餘戰鬥方法保留；第二次打包得到相同候選 SHA。',
            '沿用20個唯讀 SQL reader 與裝備映射類別，與既有固定來源逐位元相同；這不是新候選 DB 驗收。',
            '76／167 PC、受控與自然NPC主目標沿用首次Context／status／life／binding；七個既有計算迴圈在RNG前過濾已失效的首次參與者，保留原類型篩選、順序及重複項。',
            '76／167 PC／NPC指向效果與接收者狀態封包攜帶個別首次身分至既有完整加密輸出邊界；不以無關主目標退休取消有效次目標。排隊值不持有Aim／Context／完整名冊，Aim及受控receipt在延後IO前釋放。',
            '自然NPC的緩速、風之枷鎖、兩種魔消耗用入口及解除睡眠共十個寫入接點，在原遭遇鎖內比對實際解析出的actor與首次身分；原查找、條件、RNG及狀態寫入本體保留。',
            '新增六個PC狀態寫入保護：緩速、風之枷鎖、反制魔法、兩種魔消耗用及解除睡眠。在原狀態鎖內檢查首次物件、所屬PC與生命期；共用魔消狀態保留自然NPC的首次證明。沒有增加鎖或工作執行緒。',
            'NPC首次證明補入原模板／profile、factory stamp及NPC編號，選取、寫入與最終輸出均須維持一致；受控NPC仍遵循Source的isTargetFailure規則。',
            '靜態逆向移除新增接點後比對原方法，保留八個輸出相關類別、兩個遭遇類別及三個狀態類別的原方法；非指定JAR項目逐位元相同。這是靜態檢查，不是執行期驗收。',
            '252外控列補Source欄位與引用位置；另外辨認31個動態鍵（21職業傷害、10傷害數字），候選位元組的字串組合模板已核對。這31鍵與既有31個技能預設不是同一批。',
            '807技能列重新分類並綁定目前候選：181個玩家技能均定位到入口及靜態分派路徑，補回獨立傳送5；38個none列與588個非玩家資料列仍保留消費端審查。納入lambda及精確比對的Dragon反射橋接，未把方法存在當作效果完成。',
            'Quest／Time Attack NPC魔法HP／MP成本及damage_value／damage_dice_count／damage_dice接入既有唯讀skills快照。同一次扣款共用首次數值，HP至少保留1；原骰數迴圈、record身分及延遲排程本體保留。未新增SQL查詢。',
            'NPC出生時保留Source mobskill的reuseDelay；魔法、物理、召喚、視覺與地面技能共用原冷卻表。兩個選取入口在原機率判斷前拒絕冷卻列，八個成功排程接點記錄延遲，冷卻列不抽取其原機率／召喚亂數；不混用skills.reuseDelay，也未新增計時器。',
            'Quest／Time Attack兩個NPC傷害入口的屬性讀取改接既有Source技能metadata；原抗性、INT係數、倍率及MR算式保留，record定義未改。SP來源、其餘數值邊界與其他NPC類別仍在審查。',
            'Time Attack視覺／地面技能補上Source HP／MP成本；兩個完整readiness副本讓同次排程共用首次成本。原MP扣款、冷卻與排程保留，新增HP檢查至少留1。',
            '核對Source NPC有效INT限1..254；自付HP完成後刷新NpcHprTimer，並無立即HPBAR廣播。保留SP來源與其他NPC類別的真實缺口，不新增缺乏Source依據的顯示行為。',
            '自然L1Monster依出生模板的HP／MP回復參數，在既有Hub排程器每秒固定節拍運作；未指定時採Source的10秒／2HP、30秒／1MP。原生命期、Hub及遭遇鎖內掃描目前存活物件，死亡／移除／關閉後不更新，關閉時取消並清除任務；無新增執行緒池、SQL或輸出封包。',
            '寵物與召喚／馴服物的原工廠stamp補上出生模板回復參數；未指定時採Source的20秒／1HP與20秒／1MP，缺少資料則不補造。BATTLE與WORLD回復排程已接既有Hub排程器；依原主人行為鎖、生命期、Hub、名冊、寵物與狀態鎖重新核對首次物件後更新HP／MP，離開寫入鎖才送主人血條。最終加密前重查生命期、profile、factory、轉場與血量比例，未製造玩家攻擊來源；WORLD額外核對原地圖／副本登記與轉場role／scene；其他NPC類別及更完整生命期覆蓋繼續核對。',
        ],
        'groups': groups,
        'skill_matrix': pin(run / 'skills.tsv'), 'control_matrix': pin(run / 'controls.tsv'),
        'counts': {'skill_inventory_rows': len(skills), 'material_rows': sum(r['material'] == 'True' for r in skills), 'property_inventory_rows': len(controls)},
        'controlled_area_source_rule': {
            'source': pin(Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java')),
            'method': 'isTargetFailure', 'line': 4117,
            'review': 'PC returns through its own relation branch. PET/SUMMON do not acquire Monster isErase; Source76/167 reject them after admitted cost. Candidate failedControlledTargets and WindTargets.apply retain those gates. This is static review, not runtime validation.'},
        'dependency_manifest': release_candidate['manifest'],
        'readonly_SQL_classes_byte_retained': sql['entries'],
        'unknowns': ['真角色裝備／INT／成長與加成重載需日後唯讀 DB 驗證；此階段不連 DB。', '原生游標與Ctrl送包實證仍缺，通暢氣脈術自施尚不能宣告修復。', '252屬性列尚須逐列判定別名、消費端與適用性，不能當成252缺失。'],
        'next_development': ['依已凍結53項模式登記與逐列身分契約接TYPE_GMBUFF及一般施法：4個別名模式與3個無模式狀態分別實作；模式本體可能寫不同狀態或用外控時間。proc須先分治癒／正自訂秒數原ID／0秒原技能GMBUFF；不得把effect_skill_id套到正自訂秒數分支。成本／時長／圖像／目標仍取原技能資料', '自然NPC出生triggerCount、召喚上限、空目標成功計次與無可見PC歸零已接；續接其他實際施法成功計次、完整選列、其他AI結束原因、非空自施與視覺／地面分支', '把mobskill選取核心與已接入的實際actor狀態連至可見目標、成本／效果及攻擊tick；BATTLE回吸完成端已接實際受控actor，仍須接施法入口與WORLD回吸，並補其餘noTarget結束分支，補裝備SP消費端；NPC目標適用性與De實際actor／數值綁定、其他NPC回復及AI／RNG語意', '181玩家技能按實際模型逐項核對效果、正常／Ctrl及自友敵關係，588非玩家資料追查NPC／特殊消費端', 'NPC areaMagicAttack兩個傷害入口已接可見目標與靜態LOS；續補視覺／地面入口、明確GM隱身／ghost轉接與Source空間遍歷順序、其他LOS消費端的動態門與地圖專屬箭線覆蓋、各玩家自施TYPE_GMBUFF與成功後排程規則（成功依handleCommands回傳，不以造成傷害替代；地圖93拒絕已接入）及一般區域沉默適用性；其餘生產端、狀態、NPC、娃娃、地面／立方體與外控消費端收尾', '252外控中尚待適用性審查的消費端與資料讀取接線'],
        'deferred_tests': ['效果身分分流：原模式優先、單次別名、mode鍵與實際狀態不同；49014的PC模式SILENCE64／訊息1339及NPC模式無效果；49017的CURSE_READY_TIME／CURSE_TIME、雙階段狀態與凍結排除；proc治癒優先、正自訂秒數原ID不轉別名、0秒原技能GMBUFF；既有效果不刷新與原技能圖像／時長', '效果參照讀取：新／舊schema、缺欄／NULL／0／負值、自指／循環／不存在mode、原模式優先、原skills.id不變、資料重載快照一致、失敗不得部分發布；別名不得重取被參照技能成本／時長／圖像', 'NPC次數：出生上限與重載隔離、0／負值不限次數、上限拒絕仍保留原機率抽取、召喚不重複計次、空目標零成功不計次／成功僅一次；無敵方但有同圖20格PC不得歸零，已死／觀戰／隱身PC、不同force地圖、睡眠／定身／動作尚未到期、重啟／重生與停止拒絕；歸零保留reuse', '空目標同步施法：全失敗保持魔法冷卻不變並走同次普攻，成功僅一次sleep／reuse；首次玩家／NPC退役、停止／重啟、門變更、主次目標、metadata重載、重送與巢狀／異常出口；後續入口不得重施，結果須於輸出前清除', '空目標自施NPC動作：零成功不得發送；成功採原ActId或19、10格同地圖名冊、觀戰／已死接收者與普通隱身策略、首次NPC移動／移除／重生／換工廠及接收者換房／換force／生命期於八條加密前拒絕；結果清理、重送及異常還原', 'TYPE_GMBUFF空目標：TYPE_ATTACK自施且target非none應成功但零傷害／零RNG／零玩家效果；失效首次參與者拒絕；target=none、非攻擊型、光亮／火牆／生命之泉與四立方體不得誤入；成功結果與HP接受互不代替', 'NPC範圍動態門：關門中心／兩側格、正45度與非45度、目標格是門、開門還原原tile、兩房同arena隔離、門在射線期間開關／重設、不同Quest force地圖、停止／重啟房間及既有靜態LOS退路', 'NPC範圍入口：方向／小半徑外但可見玩家、主目標距離不代替可見名單、一般隱身、不同實際地圖、無首次證明、玩家移動／換身／離房及NPC移動／移除／重生／遭遇重設於RNG前拒絕；原一般分支保留', 'Source LOS直線／1:3與1:5斜率／正45度單側開放／兩側封閉／中間格箭線位元／目標tile3、地圖複製與快照重載、動態門拒絕及所有原共用LOS消費端', '最終同SHA後端／Main／STOPPED／記憶體與生命週期矩陣（含受控10／28回吸接受／拒絕／致死／重送與換主／進化／延遲輸出、寵物mobskill狀態隔離／AI重啟／冷卻保留／舊租約拒絕、回復、進化模板更新、主死亡、換主／移除、輸出延遲與關閉）', '全技能合法與拒絕、成本／RNG／回滾／重送與主次目標；一般NPC選中沉默／HP不足／MP恰足與不足時的解除、備援、零扣款／零冷卻及areaMagicAttack零NPC成本／保留NPC沉默、無可見成功目標／地圖93／LOS拒絕與成功後排程分流', '真角色唯讀載入／加成重載', '原生游標、正常／Ctrl自施友方敵方、前景多視窗與大廳', '0.6.71選服、七職業圖示、BUFF、GM7200與結算清理'],
        'tests_executed': False, 'database_connected': False, 'database_written': False,
        'deployed': False, 'new_baseline_created': False,
        'development_closeout_complete': False, 'full_goal_achieved': False,
        'all_material_skills_complete': False, 'all_doll_paths_complete': False,
        'all_Source_consumers_wired': False, 'same_SHA_full_regression_passed': False,
        'qualified_for_release': False,
    }
    write(run / 'manifest.json', state)
    state['checkpoint'] = pin(run / 'manifest.json')
    write(build / 'development-closeout-current.json', state)
    doc = SERVER / 'docs/maintenance/combat-closeout-current.zh-TW.md'
    lines = ['# 戰鬥開發收尾進度（2026-10-29）', '',
             '目標已由 create_goal 建立，目前 active。**開發仍進行中；本次已整合項目為「已實作、待測」；不是產品驗收或發布。**', '',
             '## 候選與沿用成果', '',
             f"目前候選：`{candidate['runtime']['sha256']}`。", '',
             f"[隔離 JAR]({Path(candidate['runtime']['path']).as_posix()}) · [建置與依賴 SHA]({Path(candidate['manifest']['path']).as_posix()}) · [結構化交接]({(build / 'development-closeout-current.json').as_posix()})", '',
             '沿用共同 50635… 的29／152／153初次身分、最終輸出與清理，以及73自身與69退出成果；核對較新3ccbb…的76／167主目標與76 PC次目標修補。新純編譯入口重建3ccbb…相同SHA；既有測試收據只適用各自SHA，未重跑、未跨版本當通過。歷史 current／共同進度不覆寫。', '',
             '## 實際改動', '']
    lines += ['- ' + state['actual_changes'][0], '', f"[完整改動與證據紀錄]({(run / 'manifest.json').as_posix()})；以下矩陣保留全部12項範圍，未完成項目不因本輪修補而縮減。"]
    lines += ['', '重建順序（只編譯／打包及靜態檢查）：`build_development_closeout.py` → `build_closeout_release_compat.py` → `build_closeout_area_primary.py` → `build_closeout_area_output.py` → `build_closeout_area_commit.py` → `build_closeout_area_player_commit.py` → `build_closeout_npc_skill_values.py` → `build_closeout_npc_reuse.py` → `build_closeout_npc_attribute.py` → `build_closeout_npc_aux_cost.py` → `build_closeout_npc_recovery.py` → `build_closeout_companion_recovery_data.py` → `build_closeout_companion_recovery.py` → `build_closeout_world_magic_values.py` → `build_closeout_companion_evolution.py` → `build_closeout_controlled_npc_data.py` → `build_closeout_companion_skills.py` → `build_closeout_companion_magic_heal.py` → `build_closeout_npc_spell_admission.py` → `build_closeout_npc_area_cost.py` → `build_closeout_npc_area_scene.py` → `build_closeout_battle_line_of_sight.py` → `build_closeout_npc_area_targets.py` → `build_closeout_npc_area_doors.py` → `build_closeout_npc_area_cast.py` → `build_closeout_npc_area_output.py` → `build_closeout_npc_area_attempt.py` → `build_closeout_npc_counts.py` → `build_closeout_skill_effects.py` → `audit_closeout_controls.py` → `audit_closeout_skills.py` → `audit_closeout_npc_numeric.py` → `checkpoint_development_closeout.py`；均位於 tools/combat-continuation/integration-lifecycle。不要以舊 build_source_area_pair_first.py 代替，該入口包含執行期測試。', '',
              '## 逐項狀態', '', '| 群組 | 開發狀態 | 驗證 |', '| --- | --- | --- |']
    concise = {
        'paired-lifecycle': ('首次身分與生命期', '29／152／153、76／167等已接首次身分及輸出保護；其餘生產／接收路徑仍缺'),
        'materials': ('材料與施法准入', '58材料列持續核對；完整成本、回滾、關係、範圍與各目標種類仍缺'),
        'proc-dispatch': ('Proc分派', '部分接受後效果與自訂時長已接；一般SkillUse及娃娃／魔法石／NPC等仍缺'),
        'hp-producers': ('HP與完成事件', '部分首次狀態、生命期、完成輸出已接；其餘生產端及重入／延後路徑仍缺'),
        'math-and-status': ('數值、狀態與LOS', 'NPC成本／骰值／屬性、Source靜態LOS與範圍門已接；完整公式、狀態及其他門消費端仍缺'),
        'npc-factory-ai': ('NPC工廠與AI', '出生資料／回復、空目標自施及部分計次／AI結束已接；其他成功計次、非空自施、完整選列及受控施法仍缺'),
        'presentation': ('封包與顯示', '部分獨立目標輸出與加密邊界已接；其餘圖像、OwnCharStatus及實際畫面仍缺'),
        'external-controls': ('外控與資料讀取', '252外控已盤點；可選效果參照已讀取，消費端及141列適用性／28列來源仍待審查'),
        'formal-composition': ('正式0.6.71相容', '隔離整合4類別／11方法；正式資源SHA保留，功能與畫面待驗收'),
        'same-sha-backend': ('最終同SHA後端驗證', '依指示待程式完成後安排；包含Main、STOPPED、生命期與記憶體矩陣'),
        'chinese-mysql': ('真實中文MySQL', '依指示另行安排唯讀角色／裝備／加成重載驗證'),
        'client-cursor': ('客戶端與游標', '依指示另行安排；技能26正常自施、Ctrl、自友敵及多視窗仍待實證'),
    }
    if set(concise) != {g['id'] for g in groups}: raise ValueError('Full closeout matrix scope changed')
    labels = {'partial-program-wiring': '部分已接／待補', 'inventoried-not-complete': '盤點完成／接線未完',
              'implemented-static-compared-pending-tests': '已整合／待測', 'deferred-by-user-until-program-complete': '另行安排'}
    lines[-2:] = ['| 項目 | 狀態 | 已接與剩餘範圍 |', '| --- | --- | --- |']
    lines += [f"| {concise[g['id']][0]} | {labels.get(g['status'], g['status'])} | {concise[g['id']][1]} |" for g in groups]
    lines += ['', f"[NPC範圍列分類]({Path(state['npc_area_routes']['matrix']['path']).as_posix()})：離線SQL {state['npc_area_routes']['area_rows']}列，其中{state['npc_area_routes']['empty_self_target_rows']}列為已接的空目標自施分支；不是實際DB驗收。"]
    lines += ['', f"[效果參照逐列矩陣]({Path(state['skill_effect_references']['matrix']['path']).as_posix()})：離線{state['skill_effect_references']['positive_reference_rows']}列、{state['skill_effect_references']['distinct_effect_ids']}個效果ID；讀取已接，執行消費端未完成。"]

    contracts = state['skill_effect_references']['Source_execution_contracts']
    lines += [f"[Source模式與逐技能身分]({Path(contracts['all_skill_resolution_matrix']['path']).as_posix()})：{contracts['mode_registrations']}個模式登記、807列；7個參照為4個模式及3個無模式狀態。模式鍵不等於實際狀態ID；正自訂秒數proc保留原技能ID，0秒才走TYPE_GMBUFF。僅來源契約與盤點，執行接線仍缺。"]
    lines += ['', f"[逐技能矩陣]({(run / 'skills.tsv').as_posix()})：{len(skills)}列，其中58材料列；181玩家技能均有目前入口與靜態分派路徑，38占位列及588非玩家資料列繼續消費端審查。仍需逐項審查效果，不以入口結果代替。", '',
              f"[逐外控矩陣]({(run / 'controls.tsv').as_posix()})：252盤點列，52列已有精確字面定位，另31列動態鍵已定位，141列有Source引用仍待適用性審查，28列未找到合格Source引用仍待審查；不等同252缺失。既有31項技能預設處理沿用，與31個動態鍵分開計數。", '', '## 下一步與未知資料', '']
    lines += ['- ' + x for x in state['next_development'] + state['unknowns']]
    lines += ['', '## 後續待測', ''] + ['- ' + x for x in state['deferred_tests']]
    lines += ['', '本次未執行單元／整合／回歸／Main／客戶端或UI測試，未連DB、未寫Source／玩家資料／schema／WAL，未更動正式JAR／DLL／啟動雜湊／捷徑、未部署或建立新基準。', '']
    doc.write_text('\n'.join(lines), encoding='utf-8')
    print(json.dumps({'status': state['status'], 'document': str(doc), 'checkpoint': state['checkpoint']}, ensure_ascii=False))


if __name__ == '__main__':
    main()
