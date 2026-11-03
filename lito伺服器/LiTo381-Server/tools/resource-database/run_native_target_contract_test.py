"""Verify the pinned release DLL native picker and explicit-target server contract; never starts or edits the client."""
from pathlib import Path
import ast,hashlib,json,os,re,struct,subprocess,sys
import pefile,capstone
from capstone.x86_const import X86_OP_REG,X86_OP_IMM,X86_OP_MEM
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();bind=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
EXPECTED={skill:3 for skill in [183,188,193,202,204,207,208,209,211,212,213,214,216,217,218,219]};EXPECTED.update({203:1,184:10,194:10,187:2,192:8})
def inspect_binary():
 dll=L/'app/compat/ddraw.dll';raw=dll.read_bytes();pe=pefile.PE(data=raw);base=pe.OPTIONAL_HEADER.ImageBase;cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);cs.detail=True
 def read(va,n):off=pe.get_offset_from_rva(va-base);return raw[off:off+n]
 def code(va,n=96):return list(cs.disasm(read(va,n),va))
 def value(ins,index):return ins.operands[index].imm
 def reg(ins,index):return ins.reg_name(ins.operands[index].reg)
 bridges=[]
 pattern=re.compile(re.escape(bytes.fromhex('56e8'))+b'.{4}'+re.escape(bytes.fromhex('83c40485c0')),re.S)
 for match in pattern.finditer(raw):
  va=base+pe.get_rva_from_offset(match.start());ins=code(va,49)
  if any(i.mnemonic=='mov' and i.op_str=='eax, 0x4ca6e0' for i in ins):bridges.append((va,ins))
 assert len(bridges)==1
 bridge,route=bridges[0];assert [(i.mnemonic,i.op_str) for i in route]==[('push','esi'),('call',hex(value(route[1],0))),('add','esp, 4'),('test','eax, eax'),('je',hex(bridge+37)),('mov','edx, eax'),('push','dword ptr [ebp + 0xc]'),('mov','ecx, esi'),('mov','dword ptr [edi + 0x68], esi'),('mov','eax, 0x4ca6e0'),('call','eax'),('mov','eax, 0x4cbc76'),('jmp','eax'),('mov','edx, esi'),('mov','dword ptr [edi + 0x68], esi'),('mov','eax, 0x4cbc2c'),('jmp','eax')]
 lookup=value(route[1],0);ops=code(lookup,32)[:6];assert [x.mnemonic for x in ops]==['mov','add','cmp','ja','movzx','jmp']
 assert reg(ops[0],0)=='eax' and ops[0].operands[1].type==X86_OP_MEM and ops[0].reg_name(ops[0].operands[1].mem.base)=='esp' and ops[0].operands[1].mem.disp==4
 assert reg(ops[1],0)==reg(ops[2],0)==reg(ops[4],0)=='eax'
 delta=value(ops[1],1);limit=value(ops[2],1);default=value(ops[3],0);table=ops[4].operands[1].mem;jumps=ops[5].operands[0].mem
 assert ops[4].operands[1].size==1 and ops[4].reg_name(table.base)=='eax' and table.index==0 and table.scale==1
 assert jumps.base==0 and ops[5].reg_name(jumps.index)=='eax' and jumps.scale==4
 def returns(address):
  instructions=code(address,8)[:2];assert instructions[1].mnemonic=='ret' and not instructions[1].op_str
  first=instructions[0]
  if first.mnemonic=='xor':assert first.op_str=='eax, eax';return 0
  assert first.mnemonic=='mov' and reg(first,0)=='eax' and first.operands[1].type==X86_OP_IMM
  return value(first,1)
 assert returns(default)==0
 def evaluate(argument):
  index=(argument+delta)&0xffffffff
  if index>limit:return returns(default)
  selector=read(table.disp+index,1)[0];destination=struct.unpack('<I',read(jumps.disp+4*selector,4))[0]
  return returns(destination)
 cases={str(n):evaluate(n) for n in [*range(256),-1,-2147483648,2147483647]}
 assert {n+1:cases[str(n)] for n in range(256) if cases[str(n)]}==EXPECTED
 assert all(cases[str(n)]==0 for n in [-1,-2147483648,2147483647])
 assert all(cases[str(n-1)]==0 for n in [201,205,206,210,215,220])
 references=[]
 for match in re.finditer(re.escape(b'\x68'+struct.pack('<I',bridge)),raw):
  va=base+pe.get_rva_from_offset(match.start()-5);ins=code(va,48)
  if len(ins)<12 or ins[0].mnemonic!='push':continue
  label=value(ins[0],0)
  try:name=read(label,64).split(b'\0')[0]
  except Exception:continue
  if name==b'dragon-skill-target-picker':references.append(ins)
 assert len(references)==1;install=references[0]
 assert install[1].op_str==hex(bridge) and install[2].mnemonic=='push' and value(install[2],0)==0xe9
 assert any(x.mnemonic=='push' and x.op_str=='0x4cbc27' for x in install)
 assert any(x.mnemonic=='mov' and x.op_str=='dword ptr [ebp - 0x38], 0x7789d68b' for x in install)
 assert any(x.mnemonic=='mov' and x.op_str=='byte ptr [ebp - 0x34], 0x68' for x in install)
 startup=L/'release/current/development_startup.py';tree=ast.parse(startup.read_text('utf-8'));pins=next(node.value for node in tree.body if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='PINS' for t in node.targets));declared=next(v.value for k,v in zip(pins.keys,pins.values) if isinstance(k,ast.Call) and Path(k.args[0].value).resolve()==dll.resolve());assert sha(dll)==declared.lower()
 image=L.parent/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';unpacked=image.read_bytes();client=pefile.PE(data=unpacked);client_base=client.OPTIONAL_HEADER.ImageBase
 def oldread(va,n):off=client.get_offset_from_rva(va-client_base);return unpacked[off:off+n]
 assert oldread(0x4cbc27,5)==bytes.fromhex('8bd6897768')
 callback=list(cs.disasm(oldread(0x4ca845,0x4ca885-0x4ca845),0x4ca845));assert any(x.mnemonic=='mov' and x.op_str=='dword ptr [0x75a82c], 0x4c9e40' for x in callback)
 producer=list(cs.disasm(oldread(0x4ca021,0x36),0x4ca021));formats=[value(x,0) for x in producer if x.mnemonic=='push' and x.operands[0].type==X86_OP_IMM and 0x500000<value(x,0)<0x700000];assert len(formats)==1 and oldread(formats[0],7)==b'cccdhh\0'
 assert any(x.mnemonic=='and' and x.op_str=='ecx, 7' for x in producer) and any(x.mnemonic=='sar' and x.op_str=='eax, 3' for x in producer) and any(x.mnemonic=='push' and x.op_str=='0x14' for x in producer) and any(x.mnemonic=='call' and x.op_str=='0x4c6ae0' for x in producer)
 inputs=[bind(p) for p in [dll,L/'native/class_skills.inc',L/'native/class_compat.inc',L/'native/ddraw_proxy.cpp',startup,image]]
 result=dict(status='verified-offline-pinned-release-protocol',pinned_release_DLL_sha256=sha(dll),bridge_address=hex(bridge),lookup_address=hex(lookup),actual_DLL_lookup_evaluated_cases=len(cases),actual_DLL_targeted_ranges=EXPECTED,actual_compiled_picker_hook_site='0x4cbc27',actual_native_picker='0x4ca6e0',actual_native_callback='0x4c9e40',actual_native_producer='0x4ca021',actual_native_packet_schema='cccdhh',actual_native_packet_bytes=11,original_callback_and_producer_instructions_verified=True,source_and_cube_ids_remain_legacy=[201,205,206,210,215,220],release_DLL_pin_matches_formal_startup=True,raw_unpatched_default_arm_is_not_release_targeted_protocol=True,live_UI_or_full_formal_startup_executed=False,formal_startup_preflight_limitation='existing selected-client TW13081901.txt missing; no client assets created or changed',client_inputs=inputs,lookup_case_results=cases,tool_versions=dict(capstone=capstone.__version__,pefile=pefile.__version__))
 return result
binary=inspect_binary();(B/'native-target-client-binary-contract.json').write_text(json.dumps(binary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
if '--binary-only' in sys.argv:print(json.dumps({k:v for k,v in binary.items() if k not in ['lookup_case_results','client_inputs']},ensure_ascii=False));raise SystemExit()
sources=[R/(n+'.java') for n in ['NativeTargetContractRegression','WorldFoeGapProbe','WorldMagicCommitRegression','NpcMagicRegression','NpcDamageQueueRegression','MonsterDamageCallbackRegression','RangedDamageRegression','KiringkuRegression','CombatRegression','CounterDetectionRegression','PlayerMagicRegression','PlayerMirrorRegression','PlayerDamageRegression','PatienceRegression','NpcPhysicalRegression','NpcPoisonRegression','PoisonAtomicRegression','PoisonParalysisRegression','PoisonCallbackRegression','SilenceCallbackRegression','ParalysisCallbackRegression','CurseCallbackRegression','MonsterParalysisRegression','MonsterCurseCallbackRegression','MonsterSilenceCallbackRegression','WorldCompanionPhysicalRegression','CompanionDamageRegression','ClassMagicRegression','ClassDamageRegression','WeaponBuffRegression','AwakenCombatRegression','PolluteHealingRegression','NpcDebuffRegression','ReturnNatureRegression']]
sources.extend(S/n for n in ['tools/durable-settlement/FlowRegression.java','tools/dragon-combat/Regression.java','tools/native-room-state/NativeDirectoryRegression.java','tools/shared-database/SourceLoginRegression.java'])
inputs=[bind(p) for p in [Path(__file__),*sources]];candidate=bind(B/'candidate.jar');formal=bind(L/'app/server/LiTo381-SharedResources-candidate.jar');receipt_path=B/'native-target-contract-oracle-current.json';cp=';'.join(map(str,[B/'candidate.jar',S/'lib/*']));env=dict(os.environ);env.setdefault('JAVA_TOOL_OPTIONS','-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m')
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=[],generated_executable_files=[],verification_inputs=inputs,native_client_protocol_inputs=binary['client_inputs'],binary_contract_receipt=bind(B/'native-target-client-binary-contract.json'),native_client_protocol_contract=binary,full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');save()
try:
 subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],cwd=S,env=env,check=True)
 with (B/'NativeTargetContractRegression.log').open('w',encoding='utf-8') as log:subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Dlineage.damage.commitWorkers=2','-Dlineage.damage.commitDispatchCapacity=256','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.NativeTargetContractRegression',str(S/'config/server.properties'),str(S/'runtime/combat-native-target-contract-regression'),str(B/'native-target-contract-observations.json')],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
 observed=json.loads((B/'native-target-contract-observations.json').read_text('utf-8'));assert observed['cases']==84 and observed['formal_targeted_skill_count']==21 and not observed['full_skill_effect_or_eligibility_parity_verified']
 for row in [candidate,formal,*inputs,*binary['client_inputs']]:assert sha(Path(row['path']))==row['sha256'],row['path']
 receipt.update(status='passed',native_formal_explicit_request_contract_verified=observed['unexpected_selector_acceptances']==0,native_observation_contract=observed,native_observation_receipt=bind(B/'native-target-contract-observations.json'),native_fixture_classes=[bind(p) for p in sorted((B/'tests/local/lito381/server').glob('NativeTargetContractRegression*.class'))],native_regression_checks=observed['checks'])
 print((B/'NativeTargetContractRegression.log').read_text('utf-8')[-1900:])
except BaseException as error:receipt.update(status='failed',failure=str(error));raise
finally:save()
