#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstdarg>
#include <vector>
#include <cassert>
bool IsVerifiedTournamentClient(){return true;}
void Log(const char*,...){}
alignas(16) BYTE registry[0x200000]{};
DWORD root[7]{},slots[5]{};BYTE button[0x50]{};
std::vector<int> events;
std::uintptr_t nativeCall(std::uintptr_t address);
#define LOBBY_NATIVE_ADDRESS(address) (reinterpret_cast<std::uintptr_t>(registry)+(address)-0x640000)
#define SELECTOR_CALL(address,type) reinterpret_cast<type>(nativeCall(address))
#include "selector_return.inc"
void __fastcall close(int){events.push_back(1);}
void __cdecl send(const char*,...){events.push_back(2);}
void __fastcall cleanup(int){events.push_back(3);}
void __cdecl reset(){events.push_back(4);SelectorValue<DWORD>(0x64BBD4)=2;}
void __cdecl channel(){events.push_back(5);root[3]=40;root[1]=5;}
void __cdecl characters(){events.push_back(6);root[3]=7;}
void __fastcall reveal(void*,BYTE){events.push_back(9);}
void __fastcall count(void*,void*){events.push_back(7);}
void __cdecl back(){events.push_back(8);SelectorValue<WORD>(0x64AFBC)=256;root[3]=7;}
std::uintptr_t nativeCall(std::uintptr_t address){
 switch(address){
 case 0x4EBEB0:return reinterpret_cast<std::uintptr_t>(&close);
 case 0x4C6AE0:return reinterpret_cast<std::uintptr_t>(&send);
 case 0x469CA0:return reinterpret_cast<std::uintptr_t>(&cleanup);
 case 0x496A00:return reinterpret_cast<std::uintptr_t>(&reset);
 case 0x497020:return reinterpret_cast<std::uintptr_t>(&channel);
 case 0x496EB0:return reinterpret_cast<std::uintptr_t>(&characters);
 case 0x4952F0:return reinterpret_cast<std::uintptr_t>(&reveal);
 case 0x4A7450:return reinterpret_cast<std::uintptr_t>(&count);
 case 0x46B1A0:case 0x455330:return reinterpret_cast<std::uintptr_t>(&back);
 default:std::abort();}
}
int main(){
 root[0]=reinterpret_cast<DWORD>(slots);slots[4]=reinterpret_cast<DWORD>(button);
 *reinterpret_cast<DWORD*>(button)=0x4F7B60;
 *reinterpret_cast<DWORD*>(button+0x20)=0x183D;
 *reinterpret_cast<DWORD*>(button+0x24)=0x46B1A0;
 SelectorValue<std::uintptr_t>(0x75F6E8)=reinterpret_cast<std::uintptr_t>(root);
 for(int repeat=0;repeat<3;++repeat){
  events.clear();root[3]=40;root[1]=5;SelectorValue<DWORD>(0x64AF98)=1;
  ReturnLobbyToCharacters(nullptr,nullptr);
  assert((events==std::vector<int>{1,1,2,3,4,9}));
  assert(SelectorValue<DWORD>(0x64AF98)==0&&IsChannelSelectorRoot());
  assert(SelectorValue<BYTE>(0x64AFC5)==0&&!g_returnCharactersPending);
  assert(*reinterpret_cast<DWORD*>(button+0x24)==reinterpret_cast<DWORD>(&ReturnChannelToCharacters));
  events.clear();
  reinterpret_cast<void (__fastcall*)(void*,void*)>(*reinterpret_cast<DWORD*>(button+0x24))(nullptr,nullptr);
  assert(g_returnCharactersPending&&SelectorValue<WORD>(0x64AFBC)==0);
  assert(SelectorValue<BYTE>(0x64AFC5)==1);
  ReceiveSelectorCharacterCount(nullptr,nullptr);
  assert((events==std::vector<int>{8,7}));
  assert(root[3]==7&&!g_returnCharactersPending);
  assert(SelectorValue<DWORD>(0x64BBD4)==4);
 }
 events.clear();ReceiveSelectorCharacterCount(nullptr,nullptr);assert((events==std::vector<int>{7}));
 root[3]=5;assert(!BindChannelReturnButton());
 for(DWORD phase:{2,3,4,5}){events.clear();SelectorValue<DWORD>(0x64AF98)=phase;ReturnLobbyToCharacters(nullptr,nullptr);assert((events==std::vector<int>{8}));}
 std::puts("PASS: repeated lobby/channel/character sequence, no premature directory request, phase/dispatch reset and pending-count gate.");
}
