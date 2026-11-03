"""IDAPython 3.8 script: allowlisted export only; never follows xref targets.

Not qualified for execution in the currently installed auto-plugin environment.
"""
import sys
sys.dont_write_bytecode=True
import os
import json
import time
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from ida77_request import validate_request, decompiler_probe, Refusal, sha
ROOT=Path(__file__).resolve().parent
FIXTURE_SHA='5E8BD11307A3F9F6E323062E9C7FC7F9FF536471196613A931D3F94A2F8FF1E3'


def export(request, job):
    import idaapi, ida_auto, ida_funcs, ida_nalt, ida_bytes, ida_lines, idautils, idc
    validated=validate_request(request)
    if validated['source_sha256']!=FIXTURE_SHA or ROOT not in Path(validated['source_path']).resolve().parents:
        raise Refusal('SCOPE_NOT_AUTHORIZED','This draft is qualified for synthetic fixture input only')
    deadline=time.monotonic()+validated['limits']['timeout_seconds']-2
    def tick():
        if time.monotonic()>deadline:
            raise Refusal('TIME_LIMIT','Export time budget reached')
    # Native calls need an external hard watchdog. That execution profile has
    # NOT been implemented or qualified; the shipped runner never starts IDA.
    ida_auto.auto_wait()
    tick()
    if ida_nalt.get_imagebase()!=validated['image_base']:
        raise Refusal('IDA_IMAGE_BASE_MISMATCH','Loaded base differs from input header')
    if Path(ida_nalt.get_input_file_path()).resolve()!=Path(validated['source_path']).resolve():
        raise Refusal('IDA_INPUT_MISMATCH','Loaded file differs from validated staged copy')
    version=idaapi.get_kernel_version()
    if not version.startswith('7.7'):
        raise Refusal('IDA_VERSION_MISMATCH',version)
    provenance={'source_sha256':validated['source_sha256'],'ida_version':version,
                'evidence_level':'synthetic_static_IDA_analysis_not_execution_or_GUI'}
    def addr(ea):
        return {'va':ea,'rva':ea-validated['image_base'],**provenance}
    hr,decompiler=decompiler_probe()
    output={'schema':'ida77-bounded-result-v1','source':validated,'ida_version':version,
            'python_version':sys.version,'decompiler':decompiler,'functions':[],
            'xref_policy':'Only immediate incoming/start and outgoing/instruction references; targets are metadata, never traversed',
            'input_executed':False,'followed_target_functions':False}
    for entry in validated['entries']:
        tick();fn=ida_funcs.get_func(entry['va'])
        if fn is None or fn.start_ea!=entry['va'] or fn.end_ea>entry['end_va']:
            raise Refusal('FUNCTION_BOUNDARY_MISMATCH','Explicit function start and maximum end required')
        for chunk_count,(a,b) in enumerate(idautils.Chunks(fn.start_ea),1):
            tick()
            if chunk_count>validated['limits']['max_instructions_per_function']:
                raise Refusal('CHUNK_LIMIT','Bounded function chunks')
            if a<entry['va'] or b>entry['end_va']:
                raise Refusal('FUNCTION_TAIL_OUT_OF_ALLOWLIST','No external chunks')
        loaded=ida_bytes.get_bytes(entry['va'],entry['end_va']-entry['va'])
        if loaded is None or sha(loaded)!=entry['source_range_sha256']:
            raise Refusal('IDA_LOADED_BYTES_MISMATCH','Loaded/possibly patched bytes differ from pinned source')
        row={'address':addr(fn.start_ea),'end_address':addr(fn.end_ea),
             'name':idc.get_func_name(fn.start_ea),'instructions':[],
             'direct_xrefs':[],'pseudocode':None,'pseudocode_status':decompiler['status'],**provenance}
        def add_xref(x,direction):
            tick()
            if len(row['direct_xrefs'])>=validated['limits']['max_xrefs_per_function']:
                raise Refusal('XREF_LIMIT','Not silently truncate direct references')
            row['direct_xrefs'].append({'direction':direction,'from':addr(x.frm),
                                       'to':addr(x.to),'type':int(x.type),**provenance})
        for x in idautils.XrefsTo(fn.start_ea,0):
            add_xref(x,'incoming_to_start')
        for ea in idautils.FuncItems(fn.start_ea):
            tick()
            if not entry['va']<=ea<entry['end_va']:
                raise Refusal('ITEM_OUT_OF_ALLOWLIST','No implicit function expansion')
            if len(row['instructions'])>=validated['limits']['max_instructions_per_function']:
                raise Refusal('INSTRUCTION_LIMIT','Instruction/item cap')
            size=idc.get_item_size(ea)
            if size<=0 or ea+size>entry['end_va']:
                raise Refusal('ITEM_OUT_OF_ALLOWLIST','Item end')
            raw=ida_bytes.get_bytes(ea,size)
            if raw is None:
                raise Refusal('UNREADABLE_IDA_ITEM','Raw bytes unavailable')
            row['instructions'].append({'address':addr(ea),'bytes':raw.hex(),
                                       'text':ida_lines.tag_remove(idc.generate_disasm_line(ea,0) or ''),**provenance})
            for x in idautils.XrefsFrom(ea,0):
                if int(x.type)!=21:  # fl_F fallthrough is not an explicit xref
                    add_xref(x,'outgoing_from_item')
        if hr:
            tick()
            try:
                code=str(hr.decompile(fn.start_ea))
                if len(code)>validated['limits']['max_pseudocode_chars']:
                    raise Refusal('PSEUDOCODE_LIMIT','Refuse over-budget output')
                row['pseudocode']={'address':addr(fn.start_ea),'text':code,**provenance}
                row['pseudocode_status']='DECOMPILED'
            except Refusal:
                raise
            except Exception as e:
                row['pseudocode_status']='DECOMPILATION_FAILED'
                row['decompilation_error_type']=type(e).__name__
        output['functions'].append(row)
        if len(json.dumps(output,ensure_ascii=False).encode('utf-8'))>validated['limits']['max_output_bytes']:
            raise Refusal('OUTPUT_LIMIT','Accumulated export cap')
        tick()
    output['status']='EXPORTED' if hr else 'DISASSEMBLY_ONLY_DECOMPILER_UNAVAILABLE'
    return output


def main():
    job=Path(os.environ['LITO_IDA_JOB']).resolve()
    if ROOT not in job.parents or not job.is_dir() or (job/'result.json').exists():
        raise Refusal('OUTPUT_PATH_REFUSED','Fresh existing owned job only; never overwrite result')
    request=json.loads((job/'request.json').read_text('utf-8'))
    try:
        result=export(request,job)
        maximum=request.get('limits',{}).get('max_output_bytes',262144)
        blob=json.dumps(result,ensure_ascii=False,indent=2).encode('utf-8')
        if len(blob)>maximum:
            raise Refusal('OUTPUT_LIMIT','Serialized export cap')
        exit_code=0
    except Refusal as e:
        result={'schema':'ida77-bounded-result-v1','status':'REFUSED','code':e.code,
                'detail':e.detail,'input_executed':False}
        blob=json.dumps(result).encode('utf-8');exit_code=2
    except Exception as e:
        result={'schema':'ida77-bounded-result-v1','status':'EXPORT_ERROR',
                'error_type':type(e).__name__,'input_executed':False}
        blob=json.dumps(result).encode('utf-8');exit_code=3
    with (job/'result.json').open('xb') as stream:
        stream.write(blob)
    import ida_pro
    ida_pro.qexit(exit_code)


if __name__=='__main__':
    main()
