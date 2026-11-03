"""Derived LPAC SID and security on NEW private objects only.

No CreateAppContainerProfile/registration, no existing ACL changes, no firewall
or global configuration. The accepted watchdog remains byte-for-byte reused.
"""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json,os,uuid,winreg
from pathlib import Path
from owned_process import HERE,SECURITY_ATTRIBUTES,api,check,digest,save,owned,Close
ADV=C.WinDLL('advapi32',use_last_error=True)
UE=C.WinDLL('userenv',use_last_error=True)
def bind(lib,name,args,result=W.BOOL):
    f=getattr(lib,name);f.argtypes=args;f.restype=result;return f
OpenToken=bind(ADV,'OpenProcessToken',[W.HANDLE,W.DWORD,C.POINTER(C.c_void_p)])
TokenInfo=bind(ADV,'GetTokenInformation',[W.HANDLE,C.c_int,C.c_void_p,W.DWORD,C.POINTER(W.DWORD)])
SidString=bind(ADV,'ConvertSidToStringSidW',[C.c_void_p,C.POINTER(C.c_void_p)])
Sddl=bind(ADV,'ConvertStringSecurityDescriptorToSecurityDescriptorW',[W.LPCWSTR,W.DWORD,C.POINTER(C.c_void_p),C.c_void_p])
LocalFree=api('LocalFree',[C.c_void_p],C.c_void_p)
MakeDir=api('CreateDirectoryW',[W.LPCWSTR,C.POINTER(SECURITY_ATTRIBUTES)])
Derive=bind(UE,'DeriveAppContainerSidFromAppContainerName',[W.LPCWSTR,C.POINTER(C.c_void_p)],C.c_long)
FreeSid=bind(ADV,'FreeSid',[C.c_void_p],C.c_void_p)
def sid_text(ptr):
    text=C.c_void_p();check(SidString(ptr,C.byref(text)),'ConvertSidToStringSidW')
    try:return C.wstring_at(text)
    finally:LocalFree(text)
def token_value(handle,kind):
    length=W.DWORD();TokenInfo(handle,kind,None,0,C.byref(length))
    if not length.value:raise OSError(C.get_last_error(),'GetTokenInformation size '+str(kind))
    b=C.create_string_buffer(length.value)
    check(TokenInfo(handle,kind,b,len(b),C.byref(length)),'GetTokenInformation '+str(kind))
    return b
def current_user_sid():
    h=C.c_void_p();check(OpenToken(-1,8,C.byref(h)),'OpenCurrentToken')
    try:
        buffer=token_value(h,1)
        return sid_text(C.c_void_p.from_buffer(buffer).value)
    finally:Close(h)
def derive(moniker):
    sid=C.c_void_p();result=Derive(moniker,C.byref(sid))
    if result:raise OSError(result,'Derive SID failed; no profile registration fallback')
    return sid
def registration_state(sid,moniker):
    rows=[]
    for suffix in ['Mappings','Storage']:
        key=r'Software\Classes\Local Settings\Software\Microsoft\Windows\CurrentVersion\AppContainer'+'\\'+suffix+'\\'+sid
        try:
            h=winreg.OpenKey(winreg.HKEY_CURRENT_USER,key,0,winreg.KEY_READ);winreg.CloseKey(h);exists=True
        except FileNotFoundError:exists=False
        rows.append({'HKCU_key':key,'exists':exists})
    package=Path(os.environ['LOCALAPPDATA'])/'Packages'/moniker
    return {'selected_sid_registry_keys':rows,'outside_package_directory':str(package),'outside_package_directory_exists':package.exists()}
def create_private_directory(path,user,sid,writable=False):
    p=owned(path)
    if p.exists():raise ValueError('Only new private directory creation allowed')
    rights='FA' if writable else 'FRFX'
    descriptor='D:P(A;OICI;FA;;;SY)(A;OICI;FA;;;'+user+')(A;OICI;'+rights+';;;'+sid+')S:(ML;OICI;NW;;;LW)'
    sd=C.c_void_p();check(Sddl(descriptor,1,C.byref(sd),None),'Convert private object SDDL')
    try:
        sa=SECURITY_ATTRIBUTES(C.sizeof(SECURITY_ATTRIBUTES),sd,False)
        check(MakeDir(str(p),C.byref(sa)),'Create NEW private directory with descriptor')
    finally:LocalFree(sd)
    return {'path':str(p),'creation_sddl':descriptor,'app_sid_writable':writable,'existing_security_descriptor_modified':False}
def build_probe_profile():
    moniker='lito.ida77.fixture.'+uuid.uuid4().hex[:20];sid=derive(moniker)
    try:sidstr=sid_text(sid)
    finally:FreeSid(sid)
    user=current_user_sid();state=registration_state(sidstr,moniker)
    if any(r['exists'] for r in state['selected_sid_registry_keys']) or state['outside_package_directory_exists']:
        raise ValueError('Unexpected preexisting profile; stop')
    root=HERE/'private-runtime';acl=create_private_directory(root,user,sidstr)
    source=Path(r'D:\IDA_Pro_7.7\python38')
    names=['python.exe','python38.dll','python3.dll','python38.zip','vcruntime140.dll','vcruntime140_1.dll',
           '_ctypes.pyd','libffi-7.dll','_socket.pyd','select.pyd','_hashlib.pyd','libcrypto-1_1.dll']
    copies=[]
    for name in names:
        raw=(source/name).read_bytes();dest=root/name
        with dest.open('xb') as f:f.write(raw)
        copies.append({'source':str(source/name),'destination':str(dest),'sha256':digest(dest),'bytes':len(raw)})
    with (root/'python38._pth').open('xb') as f:f.write(b'python38.zip\n.\npython\npython/3\n# no import site\n')
    profile={'schema':'private-derived-LPAC-candidate-v1','moniker':moniker,'app_sid':sidstr,'owner_sid':user,
       'runtime_root':str(root),'root_acl_at_creation':acl,'python_dependencies':copies,'_pth':str(root/'python38._pth'),
       'network_capability_count':0,'LPAC_all_application_packages_opt_out_requested':True,
       'AppContainerProfile_created_or_registered':False,'before_registration_state':state,
       'original_installation_global_profile_existing_ACLs_or_firewall_changed':False,'qualification':'PENDING_REAL_PROBE'}
    save(HERE/'private-profile-candidate.json',profile)
    print(json.dumps({'private_profile_created':True,'dependencies':len(copies),'registration_created':False}))
if __name__=='__main__':build_probe_profile()
