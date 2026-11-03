#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#define CHECK(c) do {if(!(c)){std::printf("FAIL line=%d\n",__LINE__);std::exit(1);}} while(0)
bool verified{},sourceMode{};DWORD requested=99;
bool IsVerifiedLiToClient(){return verified;}
bool ReadToolbarBytes(std::uintptr_t p,void* out,SIZE_T n){if(!p)return false;std::memcpy(out,reinterpret_cast<void*>(p),n);return true;}
void Log(const char*,...){}
bool LoadLoginSources(){return sourceMode;}
bool RequestNativeSourceSession(DWORD id){requested=id;return true;}
bool RequestNativeAccountLogin(){requested=10;return true;}
#include "character_source_return.inc"
volatile BYTE role{}; int calls{};
void __cdecl nativeReturn(){CHECK(role==0);++calls;}
int main(){
 sourceMode=true;ReturnCharacterToSources();CHECK(requested==99);
 verified=true;ReturnCharacterToSources();CHECK(requested==0);verified=false;sourceMode=false;
 for(BYTE v=0;v<2;v++){role=v;RouteCharacterSourceReturn(role,&nativeReturn);CHECK(role==v);}
 CHECK(calls==2);
 alignas(4) BYTE button[0x40]{};
 *reinterpret_cast<DWORD*>(button)=0x4f7b60;
 *reinterpret_cast<DWORD*>(button+0x20)=0x18a8;
 *reinterpret_cast<DWORD*>(button+0x24)=0x492fe0;
 DWORD slots[8]{0,0,0,0,reinterpret_cast<DWORD>(button),0x1234,0,0};
 DWORD root[]{reinterpret_cast<DWORD>(slots),8,0,7};
 CHECK(BindCharacterSourceReturn(reinterpret_cast<DWORD>(root)));
 CHECK(BindCharacterSourceReturn(reinterpret_cast<DWORD>(root)));
 CHECK(slots[5]==0x1234);
 root[3]=8;CHECK(!BindCharacterSourceReturn(reinterpret_cast<DWORD>(root)));root[3]=7;
 *reinterpret_cast<DWORD*>(button+0x24)=0x1234;CHECK(!BindCharacterSourceReturn(reinterpret_cast<DWORD>(root)));
 CHECK(!BindCharacterSourceReturn(0));
 alignas(4) BYTE back[0x40]{};
 *reinterpret_cast<DWORD*>(back)=0x4f7b60;
 *reinterpret_cast<DWORD*>(back+0x20)=0x183d;
 *reinterpret_cast<DWORD*>(back+0x24)=0x494d50;
 slots[6]=reinterpret_cast<DWORD>(back);
 CHECK(!BindCharacterLoginReturn(reinterpret_cast<DWORD>(root)));
 sourceMode=true;CHECK(BindCharacterLoginReturn(reinterpret_cast<DWORD>(root)));
 CHECK(BindCharacterLoginReturn(reinterpret_cast<DWORD>(root)));
 CHECK(slots[5]==0x1234);
 verified=true;ReturnCharacterToLogin();CHECK(requested==10);
 root[3]=40;CHECK(!BindCharacterLoginReturn(reinterpret_cast<DWORD>(root)));root[3]=7;
 *reinterpret_cast<DWORD*>(back+0x24)=0x1234;CHECK(!BindCharacterLoginReturn(reinterpret_cast<DWORD>(root)));
 std::puts("PASS: manual source callback uses ordinary role once, restores launcher role, guards page/control and preserves Confirm.");
}
