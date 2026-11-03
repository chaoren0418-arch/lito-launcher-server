"""Bounded fixed-PE inspection; no game or DLL code is executed."""
from audit import *
import sys

def main():
    pe=pefile.PE(str(PE)); cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    if sys.argv[1]=='asm':
        s=ASM.read_text(encoding='utf-8').splitlines()
        a,b=map(int,sys.argv[2:]);print('\n'.join(f'{i+1}: {s[i]}' for i in range(a-1,b)))
    elif sys.argv[1]=='raw':
        va,n=(int(x,16) for x in sys.argv[2:]); raw=pe.get_data(va-0x400000,n)
        for i in cs.disasm(raw,va): print(f'{i.address:08X} {i.bytes.hex():24} {i.mnemonic} {i.op_str}')
    elif sys.argv[1]=='calls':
        va,n=(int(x,16) for x in sys.argv[2:]);raw=pe.get_data(va-0x400000,n)
        for i in cs.disasm(raw,va):
            if i.mnemonic=='call' or '62c9b' in i.op_str:print(f'{i.address:08X} {i.mnemonic} {i.op_str}')
    elif sys.argv[1]=='data':
        va,n=(int(x,16) for x in sys.argv[2:]);raw=pe.get_data(va-0x400000,n)
        for off in range(0,len(raw),4):print(f'{va+off:08X} {raw[off:off+4].hex()} {int.from_bytes(raw[off:off+4],"little"):08X}')
    elif sys.argv[1]=='exports':
        for dll in sys.argv[2:]:
            p=Path('C:/Windows/SysWOW64')/dll; d=pefile.PE(str(p))
            wanted={'WSAAsyncSelect','recv','send','WSAGetLastError','MessageBoxA','PeekMessageA','TranslateMessage','DispatchMessageA'}
            exports=[{'name':e.name.decode(),'rva':f'{e.address:08X}','forwarder':e.forwarder.decode() if e.forwarder else None} for e in d.DIRECTORY_ENTRY_EXPORT.symbols if e.name and e.name.decode() in wanted]
            print(json.dumps({'source':record(p),'exports':exports},ensure_ascii=False))

if __name__=='__main__':main()
